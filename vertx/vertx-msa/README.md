```sh
#mvn clean compile
#mvn exec:java -f ./vertx-msa-sensor/pom.xml
#curl http://127.0.0.1:8080/data

mvn clean package

./run.sh sensor -p 8080
./run.sh sensor -p 8081
./run.sh sensor -p 8082
./run.sh sensor -p 8083
curl http://127.0.0.1:8080/data | jq

./run.sh dashboard -p 5000
open http://127.0.0.1:5000

./run.sh initdb
./run.sh store -p 7000
curl http://127.0.0.1:7000/all | jq

./run.sh edge -p 6000 -sh 127.0.0.1 -sp 7000
curl http://127.0.0.1:6000/latest | jq
curl http://127.0.0.1:6000/five-minutes | jq
hey http://127.0.0.1:6000/five-minutes
```
