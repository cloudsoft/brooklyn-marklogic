package io.cloudsoft.marklogic.forests;

import brooklyn.entity.basic.AbstractEntity;

public class ForestImpl extends AbstractEntity implements Forest {

    public String getName() {
        return getConfig(NAME);
    }

    public String getHost(){
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
}
