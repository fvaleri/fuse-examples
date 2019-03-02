```sh
#mvn clean package
#java -jar vertx-openapi-0.0.1-SNAPSHOT-fat.jar
mvn clean compile exec:java

curl -k https://localhost:8443/pets
curl -k https://localhost:8443/pets/3
curl -k https://localhost:8443/pets/5
curl -k -X POST -H "Content-type: application/json" -d '{"id":4, "name":"Alan"}' https://localhost:8443/pets
curl -k -X POST -H "Content-type: application/json" -d '{"id":4}' https://localhost:8443/pets
curl -k https://localhost:8443/pets
```
