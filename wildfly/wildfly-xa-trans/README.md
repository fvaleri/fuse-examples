```sh
EAP_HOME="/path/to/jboss-eap-7.2"
EAP_BASE="/path/to/hosts/host0"
FUSE_JAR="/path/to/fuse-eap-installer-7.7.0.jar"

# start PostgreSQL and create the database
psql template1
CREATE DATABASE testdb WITH TEMPLATE template0 ENCODING UTF8 LC_CTYPE en_US;
\c testdb;
CREATE USER admin WITH ENCRYPTED PASSWORD 'admin';
GRANT ALL PRIVILEGES ON DATABASE testdb to admin;
\q
psql testdb -U admin
CREATE TABLE testtb (id SERIAL PRIMARY KEY, text VARCHAR);
\q

# install Fuse/Camel subsystem
cd $EAP_HOME && java -jar $FUSE_JAR

# start EAP/Wildfly standalone-full.xml (using the embedded broker)
$EAP_HOME/bin/add-user.sh -u admin -p admin
$EAP_HOME/bin/add-user.sh -a -u admin -p admin -g guest
mkdir -p $EAP_BASE && cp -rp $EAP_HOME/standalone/* $EAP_BASE
$EAP_HOME/bin/standalone.sh -Djboss.server.base.dir=$EAP_BASE -c standalone-full.xml

# install JDBC driver and configure EAP/Wildfly
curl https://jdbc.postgresql.org/download/postgresql-42.2.18.jar -o /tmp/postgresql.jar
$EAP_HOME/bin/jboss-cli.sh -c <<\EOF
module add --name=org.postgresql --resources=/tmp/postgresql.jar --dependencies=javax.api,javax.transaction.api
/subsystem=datasources/jdbc-driver=postgres:add(driver-name="postgres",driver-module-name="org.postgresql",driver-class-name=org.postgresql.Driver)
data-source add --jndi-name=java:jboss/datasources/testdbDS --name=testdbDS --connection-url=jdbc:postgresql://localhost:5432/testdb --driver-name=postgres --user-name=admin --password=admin
/subsystem=transactions:write-attribute(name=node-identifier,value=host0)
/subsystem=messaging-activemq/server=default/jms-queue=TestQueue:add(entries=["java:/jms/queue/TestQueue", "java:jboss/exported/jms/queue/TestQueue"])
reload
EOF

# deploy the application and send some messages
mvn install -Pwildfly
#mvn clean -Pwildfly
```
