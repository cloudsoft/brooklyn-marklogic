package io.cloudsoft.marklogic.pumper;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.util.collections.MutableMap;

public class ContentPumperImpl extends SoftwareProcessImpl implements ContentPumper {

    private static final Logger LOG = LoggerFactory.getLogger(ContentPumperImpl.class);
    Multimap<String, AtomicBoolean> shutdownFlags = ArrayListMultimap.create();

    private AtomicBoolean submitPumpTask(final String host, final int port, final String username, final String password) {
        final AtomicBoolean shutdownFlag = new AtomicBoolean();
        getManagementSupport().getExecutionContext().submit(MutableMap.of(), new Runnable() {
                int iterationCount;
                @Override
                public void run() {
                    while (!shutdownFlag.get()) {
                        LOG.debug("Pump task iteration #{}", ++iterationCount);
                        getDriver().pumpTo(host, port, username, password);
                    }
                    LOG.debug("Pumper to {}:{} terminating after {} iterations", new Object[]{host, port, iterationCount});
                }
            });
        return shutdownFlag;
    }

    @Override
    public void pumpTo(String host, int port, String username, String password) {
        LOG.info("Pumping data once to {}:{}", host, port);
        getDriver().pumpTo(host, port, username, password);
    }

    @Override
    public void startPumping(String host, int port, String username, String password) {
        LOG.info("Pumping data continually to {}:{}", host, port);
        AtomicBoolean shutdownFlag = submitPumpTask(host, port, username, password);
        shutdownFlags.put(host+port, shutdownFlag);
    }

    @Override
    public void stopAllPumping() {
        LOG.info("Notifying all pumpers of termination");
        for (AtomicBoolean flag : shutdownFlags.values()) {
            flag.set(true);
        }
    }

    @Override
    public void stopPumping(String host, int port) {
        Collection<AtomicBoolean> flags = shutdownFlags.removeAll(host+port);
        if (!flags.isEmpty()) {
            for (AtomicBoolean flag : flags) {
                LOG.info("Notifying pumper to {}:{} of termination", host, port);
                flag.set(false);
            }
        } else {
            LOG.debug("No data was being sent to {}:{}; stopPumping has nothing to do.", host, port);
        }
    }

    @Override
    public Class getDriverInterface() {
        return ContentPumpDriver.class;
    }

    @Override
    public ContentPumpDriver getDriver() {
        return (ContentPumpDriver) super.getDriver();
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        setAttribute(SERVICE_UP, Boolean.TRUE);
    }

}
