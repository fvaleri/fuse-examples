```sh
bin/standalone.sh
mvn install -Pwildfly
#mvn clean -Pwildfly

curl -X GET -H "Content-Type: application/json" http://localhost:8080/rest/api/swagger
curl -X POST -H "Content-Type: application/json" -d '{"firstName":"Mario","lastName":"Rossi"}' http://localhost:8080/rest/api/customers
curl -X GET -H "Content-Type: application/json" http://localhost:8080/rest/api/customers
curl -X GET -H "Content-Type: application/json" http://localhost:8080/rest/api/customers/1
```
