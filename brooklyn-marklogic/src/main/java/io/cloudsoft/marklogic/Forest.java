package io.cloudsoft.marklogic;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(ForestImpl.class)
public interface Forest extends Entity {

    @SetFromFlag("name")
    ConfigKey<String> NAME = new BasicConfigKey<String>(
            String.class, "marklogic.forest.name",
            "The name of the forest", null);

    @SetFromFlag("host")
    ConfigKey<String> HOST = new BasicConfigKey<String>(
            String.class, "marklogic.forest.host",
            "Specifies the host on which the forest resides. This is not the dnsname/ip, but an internal nummeric id.", null);

    @SetFromFlag("data_dir")
    ConfigKey<String> DATA_DIR = new BasicConfigKey<String>(
            String.class, "marklogic.forest.data-dir",
            "Specifies a public directory in which the forest is located. If the data directory is not specified, the " +
                    "forest will be created in the default forest directory on the host machine and the specified host " +
                    "machine cannot be changed once the forest is created.", null);

    @SetFromFlag("large_dataDir")
    ConfigKey<String> LARGE_DATA_DIR = new BasicConfigKey<String>(
            String.class, "marklogic.forest.large-data-dir",
            "Specifies a directory in which large objects are stored. If the directory is not specified, large objects will be stored under the data directory.", null);

    @SetFromFlag("large_dataDir")
    ConfigKey<String> FAST_DATA_DIR = new BasicConfigKey<String>(
            String.class, "marklogic.forest.fast-data-dir",
            "Specifies a directory that is smaller but faster than the data directory. The directory should be on a different storage device than the data directory.", null);

    @SetFromFlag("updates_allowed")
    ConfigKey<UpdatesAllowed> UPDATES_ALLOWED = new BasicConfigKey<UpdatesAllowed>(
            UpdatesAllowed.class, "marklogic.forest.updates-allowed",
            "Specifies which operations are allowed on this forest.", UpdatesAllowed.ALL);

    @SetFromFlag("rebalancer_enable")
    ConfigKey<Boolean> REBALANCER_ENABLED = new BasicConfigKey<Boolean>(
            Boolean.class, "marklogic.rebalancer-enabled",
            "Enable automatic rebalancing after configuration changes.", true);

    @SetFromFlag("failover_enable")
    ConfigKey<Boolean> FAILOVER_ENABLED = new BasicConfigKey<Boolean>(
            Boolean.class, "marklogic.failover-enabled",
            "Enable assignment to a failover host if the primary host is down.", false);

    String getName();

    String getHost();

    String getDataDir();

    String getLargeDataDir();

    String getFastDataDir();

    UpdatesAllowed getUpdatesAllowed();

    boolean isRebalancerEnabled();

    boolean isFailoverEnabled();

    //todo: failoverhosts
    //todo: forest replicas
}
