```sh
# using deferred constraint to cause an Exception at commit time
CREATE TABLE snowflakes (i INT UNIQUE DEFERRABLE INITIALLY DEFERRED)
TRUNCATE snowflakes;

# build and run
mvn clean install

log:set DEBUG it.fvaleri.integ
#log:set DEBUG org.springframework.transaction
features:addurl mvn:it.fvaleri.integ/karaf-jdbc-trans/0.0.1-SNAPSHOT/xml/features
features:install karaf-jdbc-trans

log:tail
```
