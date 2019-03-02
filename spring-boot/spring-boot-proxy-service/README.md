```sh
mvn clean spring-boot:run

SOCKET="localhost:8080"
curl http://$SOCKET/api/test/proxy
curl http://$SOCKET/actuator
curl http://$SOCKET/actuator/health
curl http://$SOCKET/actuator/jolokia/read/org.apache.camel:context=*,type=routes,name=*

mvn clean oc:deploy -Pcloud
#mvn oc:undeploy -Pcloud
SOCKET="$(oc get route spring-boot-proxy-service -o jsonpath={.spec.host})"
```
