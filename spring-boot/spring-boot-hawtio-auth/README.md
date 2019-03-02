```sh
mvn clean spring-boot:run
open http://localhost:13500/management/hawtio
curl -u admin:admin http://localhost:13000
curl -u admin:admin http://localhost:13500/management/hawtio/jolokia/list
```
