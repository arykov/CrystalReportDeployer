---
layout: post
title: "Identity Propogation from Crystal Reports Enterprise to Oracle DB"
description: ""
category:
tags: [reporting, business objects, crystal reports, SAP, java, jdbc]
---
Crystal Report  Deployer
====================

For background on this check the blog post [here](http://blog.ryaltech.com/2016/05/06/crystal-identitypropagation).

This utility is used to deploy reports to SAP Business Objects Enterprise and change DB connection to use [Oracle Identity JDBC Driver](https://github.com/arykov/identityoraclejdbc). 

Before building don't forget to update boe.lib.home in pom.xml

Usage:

<pre>
The following options are required: -password, -p -user, -u -dbproperties, -dbp -sourcepath, -sp -server, -s

Usage: `java com.ryaltech.sap.deployment.ReportDeployer [options]`
  Options:
    -authtype, -at
       Authentication type: secEnterprise, secLDAP, etc. Defaults to
       secEnterprise
       Default: secEnterprise
  * -dbproperties, -dbp
       Location of DB properties fo JDBC file Has to be accessible to BOE server
    -destinationpath, -dp
       Destination path in BOE hierarchy Default is root
       Default: <empty string>
    -help, -h, -?
       Help
       Default: false
    -keepReportOnFailure, -kr
       Specify if you want report not to be deleted even if its configuration
       was only partially completed
       Default: false
  * -password, -p
       BOE password(entered interactively)
  * -server, -s
       BOE server:port For example boeserver:6400
  * -sourcepath, -sp
       Directory with reports or individual report
  * -user, -u
       BOE user
</pre>
For example: `java -classpath <BOE_SDK_JAVA_LIB>/cereports.jar:ReportDeployment.jar com.ryaltech.sap.deployment.ReportDeployer -user SCOTT  -server server:6400 -dbproperties /opt/data/db.properties -sourcepath /home/arykov/reports -password'

You will be prompted for password.


