```sh
# start PostreSQL 11 with max_prepared_transactions=100 and AMQ 7.8
mvn clean spring-boot:run

SOCKET="localhost:8080"
curl http://$SOCKET/api/messages
curl -X POST http://$SOCKET/api/message?content=hello
curl -X POST http://$SOCKET/api/message?content=fail

# OpenShift (install AMQ and PostgresSQL from the catalog)
oc set env dc/postgresql POSTGRESQL_MAX_PREPARED_TRANSACTIONS=100
oc create -f pvc.yml

mvn clean oc:deploy -Pcloud
#mvn oc:undeploy -Pcloud

oc scale statefulset spring-boot-xa-trans --replicas 3
SOCKET="$(oc get route spring-boot-xa-trans -o jsonpath={.spec.host})"
```
