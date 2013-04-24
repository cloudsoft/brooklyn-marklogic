package io.cloudsoft.marklogic;

public class CreateForestRequest {

    private String name;
    private String dataDir;
    private String largeDataDir;
    private String fastDataDir;
    private UpdatesAllowed updatesAllowed;
    private boolean rebalancerEnabled;
    private boolean failoverEnabled;

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public boolean isFailoverEnabled() {
        return failoverEnabled;
    }

    public void setFailoverEnabled(boolean failoverEnabled) {
        this.failoverEnabled = failoverEnabled;
    }

    public String getFastDataDir() {
        return fastDataDir;
    }

    public void setFastDataDir(String fastDataDir) {
        this.fastDataDir = fastDataDir;
    }

    public String getLargeDataDir() {
        return largeDataDir;
    }

    public void setLargeDataDir(String largeDataDir) {
        this.largeDataDir = largeDataDir;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRebalancerEnabled() {
        return rebalancerEnabled;
    }

    public void setRebalancerEnabled(boolean rebalancerEnabled) {
        this.rebalancerEnabled = rebalancerEnabled;
    }

    public UpdatesAllowed getUpdatesAllowed() {
        return updatesAllowed;
    }

    public void setUpdatesAllowed(UpdatesAllowed updatesAllowed) {
        this.updatesAllowed = updatesAllowed;
    }
}
