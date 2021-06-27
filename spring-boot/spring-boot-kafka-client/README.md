```sh
# cluster deploy
oc apply -f my-cluster.yml

# get bootstrap hostname for external access (port 9094)
oc get routes | grep bootstrap

# create client truststore
oc extract secret/my-cluster-cluster-ca-cert --keys=ca.crt --to=- > /tmp/ca.crt
keytool -import -alias root -file /tmp/ca.crt -keystore /tmp/truststore.jks -storepass secret -noprompt

mvn clean spring-boot:run #-Djavax.net.debug=ssl
```
