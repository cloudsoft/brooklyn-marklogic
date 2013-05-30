package io.cloudsoft.marklogic.forests;

import brooklyn.entity.basic.AbstractEntity;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;
import brooklyn.location.Location;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

public class ForestImpl extends AbstractEntity implements Forest {

    private static final Logger LOG = LoggerFactory.getLogger(ForestImpl.class);

    private FunctionFeed statusFeed;

    @Override
    public String getName() {
        return getConfig(NAME);
    }

    @Override
    public String getHost() {
        return getConfig(HOST);
    }

    @Override
    public String getDataDir() {
        return getConfig(DATA_DIR);
    }

    @Override
    public String getLargeDataDir() {
        return getConfig(LARGE_DATA_DIR);
    }

    @Override
    public String getFastDataDir() {
        return getConfig(FAST_DATA_DIR);
    }

    @Override
    public UpdatesAllowed getUpdatesAllowed() {
        return getConfig(UPDATES_ALLOWED);
    }

    @Override
    public boolean isRebalancerEnabled() {
        return getConfig(REBALANCER_ENABLED);
    }

    @Override
    public boolean isFailoverEnabled() {
        return getConfig(FAILOVER_ENABLED);
    }

    @Override
    public Long getForestId(){
        return getAttribute(FOREST_ID);
    }

    @Override
    public String getStatus() {
        return getAttribute(STATUS);
    }

    public MarkLogicNode getAnyUpNode() {
        final MarkLogicGroup group = getConfig(GROUP);
        if (group == null) throw new NullPointerException("Group was not configured");
        return group.getAnyStartedMember();
    }

    @Override
    public void awaitStatus(String expectedState) {
        for (int k = 0; k < 120; k++) {
            String status = getStatus();
            if (expectedState.equals(status)) {
                return;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        throw new RuntimeException(format("Status of forest %s didn't change in time to %s, currently is %s", getName(), expectedState, getStatus()));
    }

    public void connectSensors() {
        statusFeed = FunctionFeed.builder()
                .entity(this)
                .poll(new FunctionPollConfig<Object, String>(STATUS)
                        .period(5, TimeUnit.SECONDS)
                        .callable(new Callable<String>() {
                            public String call() throws Exception {
                                MarkLogicNode node = getAnyUpNode();
                                if (node == null) {
                                    LOG.info("No node found to check forest: " + getName());
                                    return null;
                                }
                                String status = node.getForestStatus(getName());
                                if (status == null) {
                                    return null;
                                }
                                int beginIndex = status.indexOf("<state>") + "<state>".length();
                                int endIndex = status.indexOf("</state>");
                                if (beginIndex == -1 || endIndex == -1) {
                                    LOG.error("bad status information:" + status);
                                    return null;
                                }

                                return status.substring(beginIndex, endIndex);
                            }
                        })
                )
                .build();
    }

    @Override
    public void restart() {
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        connectSensors();
    }

    @Override
    public void stop() {
        if (statusFeed != null) {
            statusFeed.stop();
            statusFeed = null;
        }
    }

    public void setDataDirVolumeId(String volumeId) {
        setAttribute(DATA_DIR_VOLUME_ID, checkNotNull(volumeId, "volumeId"));
    }

    public void setFastDirVolumeId(String volumeId) {
        setAttribute(FAST_DATA_DIR_VOLUME_ID, checkNotNull(volumeId, "volumeId"));
    }
}
