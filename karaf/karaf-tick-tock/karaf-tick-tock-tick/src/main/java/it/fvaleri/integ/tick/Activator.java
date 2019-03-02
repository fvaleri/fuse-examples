package it.fvaleri.integ.tick;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
    private static final String SERVICE_PID = "it.fvaleri.integ.tick";

    private boolean stop = false;
    private MyTickService tick = new MyTickService();

    public void start(BundleContext bundleContext) throws Exception {
        stop = false;

        Hashtable <String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, SERVICE_PID);
        bundleContext.registerService(ManagedService.class.getName(), tick, properties);
        bundleContext.registerService(TickService.class.getName(), tick, null);
        LOG.info("Tick bundle started");

        new Thread(new Runnable() {
            public void run() {
                while (!stop) {
                    try {
                        LOG.info("Tick!");
                        for (TickListener listener : tick.getListeners()) {
                            listener.tick();
                        }
                        Thread.sleep(tick.getDelay());
                    } catch (Exception e) {
                    }
                }
            }
        }).start();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        stop = true;
        LOG.info("Tick bundle stopped");
    }
}

