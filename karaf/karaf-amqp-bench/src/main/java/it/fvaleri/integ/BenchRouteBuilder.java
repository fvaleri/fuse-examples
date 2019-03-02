package it.fvaleri.integ;

import static org.apache.camel.LoggingLevel.INFO;

import java.text.DecimalFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BenchRouteBuilder extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(BenchRouteBuilder.class);
    private static DecimalFormat FORMATTER = new DecimalFormat("#0.000");

    private static final String MESSAGE_GENERATOR_ID = "message-generator";
    private static final String MESSAGE_PRODUCER_ID = "message-producer";
    private static final String MESSAGE_CONSUMER_ID = "message-consumer";
    private static final String BENCH_QUEUE = "TestQueue";

    public static final String NM_HEADER = "nMessages";
    public static final String PT_HEADER = "prodThreads";
    private static final Integer NUMBER_OF_MSGS = 500_000;
    private static final Integer PRODUCER_THREADS = 10;

    @Override
    public void configure() throws Exception {
        from("timer:foo?repeatCount=1&period=1")
            .routeId(MESSAGE_GENERATOR_ID)
            .autoStartup(false)
            .setExchangePattern(ExchangePattern.InOnly)
            .process(exch -> {
                if (exch.getIn().getHeader(NM_HEADER) == null) {
                    exch.getIn().setHeader(NM_HEADER, NUMBER_OF_MSGS);
                }
                if (exch.getIn().getHeader(PT_HEADER) == null) {
                    exch.getIn().setHeader(PT_HEADER, PRODUCER_THREADS);
                }
            })
            .log(INFO, "Generating ${header.nMessages} messages using ${header.prodThreads} producer threads")
            .bean(Producer.class)
            .bean(MyTimer.class, "waitAndStop")
            .process(stopRoute(MESSAGE_GENERATOR_ID));

        from("direct:messageProducer")
            .routeId(MESSAGE_PRODUCER_ID)
            .to("amqp-writer:queue:" + BENCH_QUEUE)
            .bean(MyTimer.class, "countDown");

        from("amqp-reader:queue:" + BENCH_QUEUE)
            .routeId(MESSAGE_CONSUMER_ID)
            .autoStartup(false)
            .bean(MyTimer.class, "start")
            .aggregate(constant("true"), (oldE, newE) -> newE)
                .completionSize(NUMBER_OF_MSGS)
            .log(INFO, getClass().getName(), "Queue " + BENCH_QUEUE + " emptied by the route " + MESSAGE_CONSUMER_ID)
            .bean(MyTimer.class, "stop")
            .process(stopRoute(MESSAGE_CONSUMER_ID));
    }

    private Processor stopRoute(String id) {
        return exchange -> new Thread(() -> {
            try {
                exchange.getContext().stopRoute(id);
            } catch (Exception e) {
                log.error("Failed to stop route {}", id);
            }
        }).start();
    }

    public static class MyTimer {
        private static Long START_NS = 0L;
        private static CountDownLatch LATCH = new CountDownLatch(NUMBER_OF_MSGS);

        public static void start() {
            synchronized (START_NS) {
                if (START_NS == 0) {
                    START_NS = System.nanoTime();
                    LOG.info("Start time taken");
                }
            }
        }

        public static void stop() {
            double durationNs = (System.nanoTime()-START_NS);
            double durationMs = durationNs/1_000_000f;
            double durationSec = (System.nanoTime()-START_NS)/1_000_000_000f;
            long throughput = durationSec>0 ? (int)(NUMBER_OF_MSGS/durationSec) : NUMBER_OF_MSGS;
            LOG.info("Took {} ms ({} msgs/s)", FORMATTER.format(durationMs), throughput);
            synchronized (START_NS) {
                START_NS = 0L;
            }
        }

        public static void countDown() {
            start();
            LATCH.countDown();
        }

        public static void waitAndStop() {
            try {
                LATCH.await(Integer.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LATCH = new CountDownLatch(NUMBER_OF_MSGS);
            stop();
        }
    }
}

