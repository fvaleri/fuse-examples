```sh
bin/jboss-cli.sh -c <<\EOF
/subsystem=transactions:write-attribute(name=node-identifier,value=host0)
/subsystem=messaging-activemq/server=default/jms-queue=TestQueue:add(entries=["java:/jms/queue/TestQueue", "java:jboss/exported/jms/queue/TestQueue"])
reload
EOF

mvn install -Pwildfly
#mvn clean -Pwildfly

open http://localhost:8080/services/hello?wsdl

curl -H "Content-Type: text/xml;charset=UTF-8" -d '
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:fv="http://it.fvaleri.integ/hello">
    <soapenv:Header />
    <soapenv:Body>
        <fv:writeText>
            <arg0>test</arg0>
        </fv:writeText>
    </soapenv:Body>
</soapenv:Envelope>' http://localhost:8080/services/hello
```
