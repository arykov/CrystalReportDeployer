package com.ryaltech.sap.deployment;

import com.beust.jcommander.Parameter;

public class ReportDeployerParams {
	@Parameter(names = { "-authtype", "-at"},  description = "Authentication type: secEnterprise, secLDAP, etc. Defaults to secEnterprise", required=false)
	String authType = "secEnterprise";
	@Parameter(names = { "-user", "-u"}, description = "BOE user", required=true)
	String boeUser;
	@Parameter(names = { "-password", "-p"}, description = "BOE password(entered interactively)", required=true, password=true)
	String boePassword;
	@Parameter(names = { "-server", "-s"}, description = "BOE server:port For example boeserver:6400", required=true)
	String boeServer;
	@Parameter(names = { "-dbproperties", "-dbp"}, description = "Location of DB properties fo JDBC file Has to be accessible to BOE server", required=true)
	String dbPropertyFile;
	@Parameter(names = { "-sourcepath", "-sp"}, description = "Directory with reports or individual report", required=true)
	String sourcePath;
	@Parameter(names = { "-destinationpath", "-dp"}, description = "Destination path in BOE hierarchy Default is root", required=false)
	String destinationPath = "";
	@Parameter(names = { "-keepReportOnFailure", "-kr"}, description = "Specify if you want report not to be deleted even if its configuration was only partially completed", required=false )
	boolean keepReportOnFailure = false;
	@Parameter(names = { "-help", "-h", "-?"}, description = "Help", help=true)
	boolean  help;


}
