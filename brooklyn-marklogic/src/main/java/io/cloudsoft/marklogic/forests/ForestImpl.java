package io.cloudsoft.marklogic.forests;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.AbstractEntity;

public class ForestImpl extends AbstractEntity implements Forest {

    private static final Logger LOG = LoggerFactory.getLogger(ForestImpl.class);


    public String getName() {
        return getConfig(NAME);
    }

    public String getHost() {
        return getConfig(HOST);
    }

    public String getDataDir() {
        return getConfig(DATA_DIR);
    }

    public String getLargeDataDir() {
        return getConfig(LARGE_DATA_DIR);
    }

    public String getFastDataDir() {
        return getConfig(FAST_DATA_DIR);
    }

    public UpdatesAllowed getUpdatesAllowed() {
        return getConfig(UPDATES_ALLOWED);
    }

    public boolean isRebalancerEnabled() {
        return getConfig(REBALANCER_ENABLED);
    }

    public boolean isFailoverEnabled() {
        return getConfig(FAILOVER_ENABLED);
    }
    
    public void setDataDirVolumeId(String volumeId) {
        setAttribute(DATA_DIR_VOLUME_ID, checkNotNull(volumeId, "volumeId"));
    }

    public void setFastDirVolumeId(String volumeId) {
        setAttribute(FAST_DATA_DIR_VOLUME_ID, checkNotNull(volumeId, "volumeId"));
    }
}
