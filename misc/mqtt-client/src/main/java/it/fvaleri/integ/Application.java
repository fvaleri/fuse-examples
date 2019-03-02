package it.fvaleri.integ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                LOG.info("Application shutdown");
                ApplicationUtil.sleep(2_000);
                Runtime.getRuntime().halt(0);
            }
        });
        String clientType = args[0];
        if (clientType == null || clientType.isEmpty()) {
            LOG.error("Empty client type");
            Runtime.getRuntime().halt(1);
        }
        switch (clientType) {
            case "producer":
                new Producer().run();
                break;
            case "consumer":
                new Consumer().run();
                break;
            default:
                LOG.error("Unknown client type");
                break;
        }
    }
}

