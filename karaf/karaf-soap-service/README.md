```sh
# requires jdk8
mvn clean install

# create fabric
create --clean --wait-for-provisioning --zookeeper-password admin
container-remove-profile root jboss-fuse-full
container-create-child root child0
container-create-child root child1
container-list

# deploy broker
container-change-profile child0 mq-amq
container-connect child0
activemq:dstat

# deploy app
profile-create --parents feature-cxf --parents feature-camel-jms my-app
profile-edit -b file:///Users/fvaleri/code/integ-examples/karaf/karaf-soap-service/target/karaf-soap-service-0.0.1-SNAPSHOT.jar my-app
profile-edit -p org.ops4j.pax.web/org.osgi.service.http.port=9090 my-app
profile-edit -f camel-cxf my-app

container-change-profile child1 my-app
container-connect child1
list | grep -i karaf-soap-service
log:tail

open http://localhost:9090/cxf

curl -H "Content-Type: text/xml;charset=UTF-8" \
    -H "SOAPAction: http://integ.fvaleri.it/ReportIncident" \
    -d@src/main/resources/test-request.xml http://localhost:9090/cxf/ws/incident
```
