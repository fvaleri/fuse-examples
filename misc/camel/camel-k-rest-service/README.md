```sh
kamel run src/customer-api.xml \
    --open-api src/customer-api.json \
    --name customers \
    --dependency camel-undertow \
    --dependency camel-rest \
    --property camel.rest.port=8080 \
    --dev

kamel run src/customer-api.xml \
    --open-api src/customer-api.json \
    --name customers \
    --dependency camel-undertow \
    --dependency camel-rest \
    --property camel.rest.port=8080 \
    --logging-level org.apache.camel.k=DEBUG \
    -t jolokia.enabled=true \
    -t prometheus.enabled=true \
    -t prometheus.service-monitor=false \
    --dev

kubectl get it
curl http://customers-demo.crc.testing/camel/customer
```
