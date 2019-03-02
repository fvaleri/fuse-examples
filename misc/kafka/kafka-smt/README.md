```sh
mvn clean package
cp target/kafka-smt-*.jar $PLUGINS

# add to the connectors SMT chain
"transforms": "JsonWriter",
"transforms.JsonWriter.type": "it.fvaleri.integ.JsonWriter"
```
