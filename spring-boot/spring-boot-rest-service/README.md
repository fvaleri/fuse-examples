```sh
mvn clean compile
mvn spring-boot:run -f ./spring-boot-rest-service-server/pom.xml
mvn spring-boot:run -f ./spring-boot-rest-service-client/pom.xml

SOCKET="localhost:8080"
curl -H "Content-Type: application/json" http://$SOCKET/actuator/health
curl -H "Content-Type: application/json" http://$SOCKET/api/doc
curl -H "Content-Type: application/json" http://$SOCKET/api/greet/fede

mvn clean oc:deploy -Pcloud
#mvn oc:undeploy -Pcloud
SOCKET="$(oc get route spring-boot-rest-service-server -o jsonpath={.spec.host})"
```
