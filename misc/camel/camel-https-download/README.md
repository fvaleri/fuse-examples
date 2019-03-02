```sh
echo | openssl s_client -servername raw.githubusercontent.com -connect raw.githubusercontent.com:443 |\
  sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > /tmp/server.pem
keytool -import -noprompt -alias server -file /tmp/server.pem -keystore client.ts -storepass secret
mvn clean test
```
