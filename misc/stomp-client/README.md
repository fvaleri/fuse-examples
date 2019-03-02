```sh
# activemq setup
log4j.logger.org.apache.activemq.transport = DEBUG

<transportConnectors>
    <transportConnector name="websocket" uri="ws://localhost:61614?maximumConnections=1000&amp;wireFormat.maxFrameSize=104857600"/>
</transportConnectors>

<destinations>
    <queue physicalName="TestQueue"/>
</destinations>

# build and run
mvn clean compile exec:java \
    -DbrokerURL="ws://localhost:61614" \
    -DqueueName="TestQueue" \
    -DbrokerUserLogin="admin" \
    -DbrokerUserPasscode="admin"
```
