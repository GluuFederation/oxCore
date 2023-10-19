# DO NOT EDIT - See: https://www.eclipse.org/jetty/documentation/current/startup-modules.html

[description]
Enables extended JDBC persistent/distributed session storage.

[tags]
session

[provides]
session-store-jdbc-extended

[depend]
jdbc
sessions
sessions/jdbc-extended/${db-connection-type}

[lib]
lib/session-store-jdbc-extended-${jetty.version}.jar

[xml]
etc/sessions/jdbc-extended/session-store-extended.xml

[ini]
db-connection-type?=datasource

[ini-template]
##
##Extended JDBC Session properties
##

#jetty.session.gracePeriod.seconds=3600
#jetty.session.savePeriod.seconds=0
#jetty.session.lockTime.millis=0
#jetty.session.delayTime.millis=0
#jetty.session.serialization.compress.data=false

jetty.session.jdbc.blobType=mediumblob
#jetty.session.jdbc.longType=
#jetty.session.jdbc.stringType=

## Connection type:Datasource
#db-connection-type=datasource
#jetty.session.jdbc.datasourceName=/jdbc/sessions

## Connection type:driver
db-connection-type=driver
#jetty.session.jdbc.driverClass=
#jetty.session.jdbc.driverUrl=

## Session table schema
#jetty.session.jdbc.schema.accessTimeColumn=accessTime
#jetty.session.jdbc.schema.contextPathColumn=contextPath
#jetty.session.jdbc.schema.cookieTimeColumn=cookieTime
#jetty.session.jdbc.schema.createTimeColumn=createTime
#jetty.session.jdbc.schema.expiryTimeColumn=expiryTime
#jetty.session.jdbc.schema.lastAccessTimeColumn=lastAccessTime
#jetty.session.jdbc.schema.lastSavedTimeColumn=lastSavedTime
#jetty.session.jdbc.schema.lockTimeColumn=lockTime
#jetty.session.jdbc.schema.idColumn=sessionId
#jetty.session.jdbc.schema.lastNodeColumn=lastNode
#jetty.session.jdbc.schema.virtualHostColumn=virtualHost
#jetty.session.jdbc.schema.maxIntervalColumn=maxInterval
#jetty.session.jdbc.schema.mapColumn=map
#jetty.session.jdbc.schema.table=JettySessions
# Optional name of the schema used to identify where the session table is defined in the database: 
#  "" - empty string, no schema name 
#  "INFERRED" - special string meaning infer from the current db connection
#  name - a string defined by the user
#jetty.session.jdbc.schema.schemaName
# Optional name of the catalog used to identify where the session table is defined in the database: 
#  "" - empty string, no catalog name
#  "INFERRED" - special string meaning infer from the current db connection
#  name - a string defined by the user
#jetty.session.jdbc.schema.catalogName

