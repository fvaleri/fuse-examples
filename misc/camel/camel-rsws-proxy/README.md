```sh
mvn clean compile exec:java
http_proxy=http://localhost:8080 curl \
    -d '{"subtract": {"intA":3, "intB":1}}' http://www.dneonline.com/calculator.asmx
```
