```sh
# dev mode run
mvn compile quarkus:dev -Ddebug=false
curl http://localhost:8080/greet
curl http://localhost:8080/greet/fede

# jar build
mvn package
java -jar target/quarkus-app/quarkus-run.jar

# native build
mvn clean package -Pnative
./target/quarkus-rest-service-0.0.1-SNAPSHOT-runner
# or you can run the native executable build for Linux in a container 
mvn package -Pnative -Dquarkus.native.container-build=true
mvn package -Pnative -Dquarkus.native.remote-container-build=true

# container image build
docker build -f src/main/docker/Dockerfile.native-distroless -t quay.io/fvaleri/quarkus-rest-service:latest .
docker push quay.io/fvaleri/quarkus-rest-service:latest
```
