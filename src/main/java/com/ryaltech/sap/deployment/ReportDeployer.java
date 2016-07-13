package com.ryaltech.sap.deployment;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.FileUtils;

import com.beust.jcommander.JCommander;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.CePropertyID;
import com.crystaldecisions.sdk.occa.infostore.IInfoObject;
import com.crystaldecisions.sdk.occa.infostore.IInfoObjects;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
import com.crystaldecisions.sdk.occa.managedreports.IReportAppFactory;
import com.crystaldecisions.sdk.occa.report.application.DBOptions;
import com.crystaldecisions.sdk.occa.report.application.DatabaseController;
import com.crystaldecisions.sdk.occa.report.application.OpenReportOptions;
import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.data.IConnectionInfo;
import com.crystaldecisions.sdk.plugin.desktop.report.IReport;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ReportDeployer {

	private ReportDeployerParams params;

	public ReportDeployer(ReportDeployerParams params) {
		this.params = params;
	}

	public static void main(String[] args) throws Exception {
		ReportDeployerParams params = new ReportDeployerParams();
		JCommander jc = new JCommander(params);
		try {
			jc.parse(args);
		} catch (Exception ex) {
			// due to intelligible nature of jcommander parse messages
			System.out.println(ex.getMessage());
			params.help = true;
		}
		if (params.help) {
			jc.usage();
			System.out
					.println(String
							.format("For example: java -classpath <BOE_SDK_JAVA_LIB>/cereports.jar:ReportDeployment.jar %s -user SCOTT  -server server:6400 -dbproperties /opt/data/db.properties -sourcepath c:/import -password",
									ReportDeployer.class
											.getCanonicalName()));
			System.out.println("You will be prompted for password");
			return;
		}

		new ReportDeployer(params).upload();

	}

	/**
	 * Returns a list found at the path
	 *
	 * @param sourcePath
	 *            directory or single report
	 * @return
	 */
	private Collection<File> getReportFiles(String sourcePath) {
		File sourcePathFile = new File(sourcePath);
		if (sourcePathFile.exists()) {
			if (sourcePathFile.isDirectory()) {
				return FileUtils.listFiles(sourcePathFile,
						new String[] { "rpt" }, true);
			} else {
				return Arrays.asList(sourcePathFile);

			}
		} else {

			return Collections.EMPTY_LIST;
		}
	}

	private void upload() throws Exception {
		System.out.println("Connecting...");
		IEnterpriseSession session = CrystalEnterprise.getSessionMgr().logon(
				params.boeUser, params.boePassword, params.boeServer,
				params.authType);
		System.out.println("Connected");
		try {
			IInfoStore infoStore = (IInfoStore) session.getService("",
					"InfoStore");
			for (File report : getReportFiles(params.sourcePath)) {
				//upload of some of the files is possible
				String destinationPath = null;
				try {
					System.out.print(String.format("Uploading %s",
							report.getCanonicalPath()));

					destinationPath = createDestinationPath(report,
							params.sourcePath, params.destinationPath);
					System.out
							.println(String.format(" to %s", destinationPath));
					uploadReport(infoStore, destinationPath, report);
					update(infoStore, session, destinationPath);
					adjustLoginSettings(infoStore, destinationPath);
					System.out.println(String.format("Finished uploading %s",
							report.getCanonicalPath()));
				} catch (Exception ex) {
					new Exception(String.format("Failed uploading %s",
							report.getCanonicalPath()), ex).printStackTrace(System.out);
					//if we managed to calculate the path before failing
					if(destinationPath!=null && !params.keepReportOnFailure){
						System.out.println(String.format("Removing %s from BOE", destinationPath));
						deleteReport(infoStore, destinationPath);
					}
				}
			}

		} finally {
			System.out.println("Disconnecting");
			session.logoff();
			System.out.println("Disconnected");
		}
	}

	private String createDestinationPath(File report, String sourcePath,
			String destinationPath) throws Exception {
		if (new File(sourcePath).isDirectory()) {
			return destinationPath
					+ "/"
					+ new File(sourcePath).getCanonicalFile().toURI()
							.relativize(report.getCanonicalFile().toURI())
							.getPath();

		} else {
			return destinationPath + "/" + report.getName();
		}
	}

	// don't call directly
	private int getObjectId(IInfoStore infoStore, int parentId,
			int currentDepth, String[] subFolders) throws Exception {

		IInfoObjects boFolderObjects = infoStore
				.query(String
						.format("Select * From CI_INFOOBJECTS Where SI_NAME ='%s' AND SI_PARENTID = %s",
								subFolders[currentDepth], parentId));
		if (boFolderObjects.size() != 1) {
			throw new RuntimeException("issue with the object location");
		}
		;
		int id = ((IInfoObject) boFolderObjects.get(0)).getID();
		if (subFolders.length == currentDepth + 1)
			return id;
		return getObjectId(infoStore, id, currentDepth + 1, subFolders);

	}

	private int getObjectId(IInfoStore infoStore, String folder)
			throws Exception {

		try{
		String sanitizedFolder = folder;
		do {
			folder = sanitizedFolder;
			sanitizedFolder = folder.replace("//", "/");
		} while (!folder.equals(sanitizedFolder));
		if (sanitizedFolder.startsWith("/"))
			sanitizedFolder = sanitizedFolder.substring(1);
		return getObjectId(infoStore, 0, 0, sanitizedFolder.split("/"));
		}catch(Exception ex){
			throw new RuntimeException(String.format("cannot locate %s",folder ));
		}

	}

	private void uploadReport(IInfoStore infoStore, String destination,
			File sourceFile) throws Exception {
		String name = sourceFile.getName();

		try {
			deleteReport(infoStore, destination);
		} catch (Exception ex) {
			// if delete fails, it either did not exist or we will see
			// downstream implications
		}

		IInfoObjects infoObjects = infoStore.newInfoObjectCollection();


		IReport reportObject = (IReport)infoObjects.add(infoStore.getPluginMgr().getPluginInfo("CrystalEnterprise.Report"));


		reportObject.setTitle(name);
		reportObject.getFiles().addFile(sourceFile);

		reportObject.properties().setProperty(CePropertyID.SI_PARENTID,
				getObjectId(infoStore, new File(destination).getParent().replace('\\', '/')));


		reportObject.refreshProperties();

		infoStore.commit(infoObjects);

	}

	private void deleteReport(IInfoStore infoStore, String path)
			throws Exception {
		IInfoObjects infoObjects = infoStore.query(String.format(
				"Select * From CI_INFOOBJECTS Where SI_ID = %s",
				getObjectId(infoStore, path)));
		infoObjects.delete((IInfoObject) infoObjects.get(0));
		infoStore.commit(infoObjects);

	}

	private void update(IInfoStore infoStore, IEnterpriseSession session,
			String reportFullPath) throws Exception {
		IInfoObjects infoObjects = infoStore.query(String.format(
				"select *  from ci_infoobjects where si_id=%s",
				getObjectId(infoStore, reportFullPath)));
		IReport report = (IReport) infoObjects.get(0);

		IReportAppFactory raf = (IReportAppFactory) session.getService("",
				"RASReportService");

		ReportClientDocument document = raf.openDocument(report,
				OpenReportOptions._refreshRepositoryObjects, null);

		DatabaseController controller = document.getDatabaseController();

		IConnectionInfo newConnectionInfo = createConnectionInfo(params.dbPropertyFile);
		for (IConnectionInfo oldConnectionInfo : controller
				.getConnectionInfos(null)) {

			controller.replaceConnection(oldConnectionInfo, newConnectionInfo,
					null, DBOptions._useDefault);
		}



		document.save();
		document.close();

		report.refreshProperties();
		infoStore.commit(infoObjects);
	}

	private void adjustLoginSettings(IInfoStore infoStore, String reportFullPath)
			throws Exception {
		IInfoObjects infoObjects = infoStore.query(String.format(
				"select *  from ci_infoobjects where si_id=%s",
				getObjectId(infoStore, reportFullPath)));
		IReport report = (IReport) infoObjects.get(0);


		try {
			report.getProcessingInfo().properties()
					.getProperty("SI_DBLOGONCONFIGURABLE").setValue(false);
		} catch (NullPointerException nex) {
			// property has not been added yet
			report.getProcessingInfo().properties()
					.add("SI_DBLOGONCONFIGURABLE", false, 0);
		}


		report.getProcessingInfo().properties()
				.getProperties("SI_LOGON_INFO", true)
				.getProperties("SI_LOGON1", true).getProperty("SI_LOGON_MODE")
				.setValue(3);
		infoStore.commit(infoObjects);

	}

	/**
	 * only small files
	 *
	 * @param path
	 * @return
	 */
	private String readFileFromCalsspath(String path) throws Exception {
		InputStream stream = getClass().getResourceAsStream(path);
		byte[] buffer = new byte[stream.available()];
		stream.read(buffer);
		return new String(buffer, "UTF-8");

	}



	private IConnectionInfo createConnectionInfo(String dbPropertyFileLocation)
			throws Exception {
		return (IConnectionInfo) new XStream(new DomDriver())
				.fromXML(readFileFromCalsspath("/ConnectionInfoTemplate.xml")
						.replace("${DB_PROPERTIES_LOCATION}",
								dbPropertyFileLocation));
	}

}
