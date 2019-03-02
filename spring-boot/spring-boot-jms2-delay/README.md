```sh
mvn clean spring-boot:run

# install AMQ from the catalog
mvn clean oc:deploy -Pcloud
#mvn oc:undeploy -Pcloud
POD_NAME=$(oc get pods | grep spring-boot-jms2-delay | grep Running | cut -d " " -f1)
oc logs $POD_NAME
```
