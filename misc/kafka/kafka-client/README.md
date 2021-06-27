```sh
# overwrite defaults using env variables or config file
#export CONFIG_FILE_PATH="./src/main/resources/configs/custom.json"
mvn clean compile exec:java -Ppro
mvn clean compile exec:java -Pcon

# schema upload (set artifactId == topicName)
curl -v -X POST -H "Content-Type: application/json" \
    -H "X-Registry-ArtifactId: my-topic" -H "X-Registry-ArtifactType: AVRO" \
    -d @src/main/resources/schemas/test.avsc $REGISTRY_URL/artifacts
```
