```sh
# overwrite defaults using env variables or config file
#export CONFIG_FILE_PATH="./src/main/resources/configs/custom.json"
mvn clean compile exec:java -Ppro
mvn clean compile exec:java -Pcon
```
