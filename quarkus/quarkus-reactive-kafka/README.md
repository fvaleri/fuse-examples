```sh
# run Kafka cluster and create the truststore
CLUSTER_NAME="my-cluster"
oc extract secret/$CLUSTER_NAME-cluster-ca-cert --keys=ca.crt --to=- > /tmp/ca.crt
keytool -import -noprompt -trustcacerts -alias root -file /tmp/ca.crt -keystore /tmp/client.ts -storepass changeit

# dev mode run
export BOOTSTRAP_URL="localhost:9092"
mvn compile quarkus:dev -Ddebug=false
open http://localhost:8080

# jar build
mvn package
java -jar target/quarkus-app/quarkus-run.jar

# native build
mvn clean package -Pnative -DskipTests
./target/quarkus-reactive-kafka-0.0.1-SNAPSHOT-runner
# or you can run the native executable build for Linux in a container 
mvn package -Pnative -Dquarkus.native.container-build=true
mvn package -Pnative -Dquarkus.native.remote-container-build=true

# container image build
docker build -f src/main/docker/Dockerfile.native-distroless -t quay.io/fvaleri/quarkus-reactive-kafka:latest .
docker push quay.io/fvaleri/quarkus-reactive-kafka:latest
```
