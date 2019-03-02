package it.fvaleri.integ;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.apache.camel.processor.aggregate.jdbc.JdbcAggregationRepositoryNew; // 2.x
//import org.apache.camel.processor.aggregate.jdbc.JdbcAggregationRepository; // 3.x
import org.apache.camel.spi.AggregationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    protected static final int THREADS = 20;
    protected static final int END = 100;

    private static final String CID_HEADER = "corrId";
    private static final String DB_URL = "jdbc:derby:target/testdb;create=true";
    //private static final String DB_URL = "jdbc:mysql://localhost:3306/testdb";
    //private static final String DB_URL = "jdbc:postgresql://localhost:5432/testdb";
    private static final String DB_USER = "admin";
    private static final String DB_PASS = "admin";

    private static String CORRELATION_ID, EXPECTED_RESULT;
    private static Queue<Integer> INPUT_QUEUE;
    private static CountDownLatch LATCH;

    public static void main(String[] args) throws Exception {
        // init
        CORRELATION_ID = UUID.randomUUID().toString();
        EXPECTED_RESULT = IntStream.rangeClosed(1, END)
            .mapToObj(Integer::toString).collect(Collectors.joining("."));
        INPUT_QUEUE = new ConcurrentLinkedQueue<>();
        IntStream.rangeClosed(1, END).forEach(INPUT_QUEUE::add);
        LATCH = new CountDownLatch(THREADS);

        // test
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        for (int i = 0; i < THREADS; i++) {
            executor.execute(Application::startCamel);
        }

        // wait
        LATCH.await();
        stop(executor);
    }

    private static void startCamel() {
        try {
            Main camel = new Main();
            camel.addRouteBuilder(new RouteBuilder() { // 2.x
            //camel.configure().addRoutesBuilder(new RouteBuilder() { // 3.x
                @Override
                public void configure() {
                    from("timer:foo?repeatCount=1&period=1")
                        .setExchangePattern(ExchangePattern.InOnly)
                        .bean(new MyProducerBean());

                    from("direct:aggregator")
                        .filter(body().isNotNull())
                        .aggregate().header(CID_HEADER)
                            .aggregationStrategy(Application::aggregationStrategy)
                            .completionPredicate(Application::completionPredicate)
                            .aggregationRepository(getAggregationRepository())
                            .optimisticLocking()
                        .log(LoggingLevel.INFO, "Result: ${body}");
                }
            });

            camel.start();
            LOG.debug("Camel started");
            LATCH.await();
            camel.stop();
            LOG.debug("Camel stopped");
        } catch (Exception e) {
            LOG.error("Failed to start Camel: {}", e.getMessage());
        }
    }

    // clustered idempotent repository that the nodes use to coordinate and grant locks
    private static AggregationRepository getAggregationRepository() {
        SingleConnectionDataSource ds = new SingleConnectionDataSource(DB_URL, DB_USER, DB_PASS, true);
        ds.setAutoCommit(false);
		try {
            Connection conn = ds.getConnection();
			conn.createStatement().execute(
			    "create table aggregation("
			        + "id varchar(255) not null primary key,"
				    + "exchange blob not null,"
			        + "version bigint not null"
			    + ")");
            conn.createStatement().execute(
                "create table aggregation_completed("
                    + "id varchar(255) not null primary key,"
                    + "exchange blob not null,"
                    + "version bigint not null"
                + ")");
        } catch (SQLException e) {
            if (!e.getMessage().contains("already exists")) {
                LOG.error("Database initialization failure", e);
            }
        }
        DataSourceTransactionManager txManager = new DataSourceTransactionManager(ds);
        // repositoryName (aggregation) must match tableName (aggregation, aggregation_completed)
        JdbcAggregationRepositoryNew repo = new JdbcAggregationRepositoryNew(txManager, "aggregation", ds); // 2.x
        //JdbcAggregationRepository repo = new JdbcAggregationRepository(txManager, "aggregation", ds); // 3.x
        repo.setUseRecovery(false);
        repo.setStoreBodyAsText(false);
        return (AggregationRepository) repo;
    }

    private static Exchange aggregationStrategy(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            return newExchange;
        }
        String body = oldExchange.getIn().getBody(String.class) + "."
            + newExchange.getIn().getBody(String.class);
        oldExchange.getIn().setBody(body);
        LOG.trace("Queue: {}", INPUT_QUEUE);
        LOG.trace("Aggregation: {}", oldExchange.getIn().getBody());
        return oldExchange;
    }

    private static boolean completionPredicate(Exchange exchange) {
        boolean isComplete = false;
        final String body = exchange.getIn().getBody(String.class);
        if (body != null && !body.isEmpty()) {
            String[] a1 = body.split("\\.");
            String[] a2 = EXPECTED_RESULT.split("\\.");
            if (a1.length == a2.length) {
                Arrays.sort(a1);
                Arrays.sort(a2);
                isComplete = Arrays.equals(a1, a2);
            }
        }
        LOG.debug("Complete? {}", isComplete);
        return isComplete;
    }

    private static void stop(ExecutorService executor) {
        try {
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.error("Termination interrupted");
        } finally {
            if (executor.isTerminated()) {
                LOG.debug("All tasks completed");
            } else {
                executor.shutdownNow();
            }
        }
    }

    static class MyProducerBean {
        public void run(Exchange exchange) throws Exception {
            CamelContext context = exchange.getContext();
            ProducerTemplate template = context.createProducerTemplate();
            template.setThreadedAsyncMode(false);
            Endpoint endpoint = context.getEndpoint("direct:aggregator");
            Integer item = null;
            while ((item = INPUT_QUEUE.poll()) != null) {
                template.sendBodyAndHeader(endpoint, item, CID_HEADER, CORRELATION_ID);
            }
            template.stop();
            LATCH.countDown();
        }
    }
}

