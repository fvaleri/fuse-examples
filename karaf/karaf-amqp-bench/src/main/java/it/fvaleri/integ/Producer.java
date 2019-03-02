package it.fvaleri.integ;

import static it.fvaleri.integ.BenchRouteBuilder.NM_HEADER;
import static it.fvaleri.integ.BenchRouteBuilder.PT_HEADER;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.apache.camel.BeanInject;
import org.apache.camel.Body;
import org.apache.camel.Endpoint;
import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.apache.camel.impl.DefaultProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer {
    private static final Logger LOG = LoggerFactory.getLogger(Producer.class);

    @BeanInject("producerTemplate")
    private DefaultProducerTemplate producerTemplate;
    private ExecutorService producerTemplateExecutor;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Handler
    public void produceMsgs(@Body String body, @Header(NM_HEADER) int nMessages,
            @Header(PT_HEADER) int nThreads) throws Exception {
        Endpoint endpoint = producerTemplate.getCamelContext().getEndpoint("direct:messageProducer");
        producerTemplate.setExecutorService(createProducerExecutor(nThreads));
        //producerTemplate.setMaximumCacheSize(1000);
        //LOG.info("Producer max cache: {}", producerTemplate.getMaximumCacheSize());

        IntStream.range(0, nMessages).forEach(i -> executor
                .execute(() -> producerTemplate.asyncSendBody(endpoint, body)));

        LOG.info("Sent {} async messages", nMessages);
    }

    private ExecutorService createProducerExecutor(int threads) {
        if (producerTemplateExecutor != null) {
            producerTemplateExecutor.shutdown();
        }
        producerTemplateExecutor = Executors.newFixedThreadPool(threads);
        return producerTemplateExecutor;
    }
}

