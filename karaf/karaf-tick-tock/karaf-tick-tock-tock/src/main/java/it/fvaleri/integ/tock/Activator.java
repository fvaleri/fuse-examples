package it.fvaleri.integ.tock;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.fvaleri.integ.tick.TickListener;
import it.fvaleri.integ.tick.TickService;

public class Activator implements BundleActivator, TickListener {
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    @Override
    public void tick() {
        LOG.info("Tock!");
    }

    public void start(BundleContext bundleContext) throws Exception {
        ServiceTracker tracker = new ServiceTracker(bundleContext, TickService.class.getName(), null);
        tracker.open();
        TickService tick = (TickService) tracker.getService();
        tracker.close();

        if (tick != null) {
            tick.addListener(this);
        } else {
            throw new Exception("Can't start tock bundle, as tick service is not running");
        }

        LOG.info("Tock bundle started");
    }

    public void stop(BundleContext bundleContext) throws Exception {
        ServiceTracker tracker = new ServiceTracker(bundleContext, TickService.class.getName(), null);

        tracker.open();
        TickService tick = (TickService) tracker.getService();
        tracker.close();

        if (tick != null) {
            tick.removeListener(this);
        }

        LOG.info("Tock bundle stopped");
    }
}

