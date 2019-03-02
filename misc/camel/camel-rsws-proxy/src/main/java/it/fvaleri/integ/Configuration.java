package it.fvaleri.integ;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {
    @BindToRegistry
    public Service svc() {
        return new Service();
    }

    public static final class Service {
        private static final Logger LOG = LoggerFactory.getLogger(Service.class);

        public String hostname(final Exchange exchange) {
            final String envHostname = System.getenv("SERVICE_HOSTNAME");
            if (envHostname != null) {
                LOG.debug("Using hostname from environment: {}", envHostname);
                return envHostname;
            }

            final Message message = exchange.getMessage();
            final String headerHostname = message.getHeader("Host", String.class);
            if (headerHostname != null) {
                LOG.debug("Using hostname from the Host HTTP header: {}", headerHostname);
                return headerHostname;
            }

            final String proxyHostname = message.getHeader(Exchange.HTTP_HOST, String.class);
            LOG.debug("Using hostname from the proxy request: {}", proxyHostname);
            return proxyHostname;
        }
    }
}

