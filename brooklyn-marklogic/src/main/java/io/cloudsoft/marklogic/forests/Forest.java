package io.cloudsoft.marklogic.forests;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;

@ImplementedBy(ForestImpl.class)
public interface Forest extends Entity, Startable {

    @SetFromFlag("name")
    ConfigKey<String> NAME = new BasicConfigKey<String>(
            String.class, "marklogic.forest.name",
            "The name of the forest", null);

    @SetFromFlag("master")
    ConfigKey<String> MASTER = new BasicConfigKey<String>(
            String.class, "marklogic.forest.master",
            "The name of the master forest (null if there is no master)", null);

    @SetFromFlag("host")
    ConfigKey<String> HOST = new BasicConfigKey<String>(
            String.class, "marklogic.forest.host",
            "Specifies the host on which the forest resides.", null);

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

    @SetFromFlag("fast_dataDir")
    ConfigKey<String> FAST_DATA_DIR = new BasicConfigKey<String>(
            String.class, "marklogic.forest.fast-data-dir",
            "Specifies a directory that is smaller but faster than the data directory. The directory should be on a different storage device than the data directory.", null);

    @SetFromFlag("updates_allowed")
    ConfigKey<UpdatesAllowed> UPDATES_ALLOWED = new BasicConfigKey<UpdatesAllowed>(
            UpdatesAllowed.class, "marklogic.forest.updates-allowed",
            "Specifies which operations are allowed on this forest.", UpdatesAllowed.ALL);

    @SetFromFlag("rebalancer_enable")
    ConfigKey<Boolean> REBALANCER_ENABLED = new BasicConfigKey<Boolean>(
            Boolean.class, "marklogic.forest.rebalancer-enabled",
            "Enable automatic rebalancing after configuration changes.", true);

    @SetFromFlag("failover_enable")
    ConfigKey<Boolean> FAILOVER_ENABLED = new BasicConfigKey<Boolean>(
            Boolean.class, "marklogic.forest.failover-enabled",
            "Enable assignment to a failover host if the primary host is down.", false);

    @SetFromFlag("cluster")
    BasicConfigKey<MarkLogicGroup> GROUP = new BasicConfigKey<MarkLogicGroup>(
            MarkLogicGroup.class, "marklogic.forests.group", "The group");

    AttributeSensor<String> STATUS = new BasicAttributeSensor<String>(String.class, "forest.status", "The status of the forest");

    AttributeSensor<Long> FOREST_ID = new BasicAttributeSensor<Long>(Long.class, "forest.id", "The id of the forest");

    @SetFromFlag("data_dir_volumeId")
    BasicAttributeSensorAndConfigKey<VolumeInfo> DATA_DIR_VOLUME_INFO = new BasicAttributeSensorAndConfigKey<VolumeInfo>(
            VolumeInfo.class, "marklogic.forest.data-dir.volumeId",
            "Specifies a volume id in which the forest's regular data is located. If null, indicates that no volume " +
                    "has been created yet for this forest. If DATA_DIR is null then the volumeId should also be null", null);

    @SetFromFlag("fast_dataDir_volumeId")
    BasicAttributeSensorAndConfigKey<VolumeInfo> FAST_DATA_DIR_VOLUME_INFO = new BasicAttributeSensorAndConfigKey<VolumeInfo>(
            VolumeInfo.class, "marklogic.forest.fast-data-dir.volumeId",
            "Specifies a volume id in which the forest's fast data is located. If null, indicates that no volume " +
                    "has been created yet. If FAST_DATA_DIR is null then the volumeId should also be null", null);

    <T> T setAttribute(AttributeSensor<T> attribute, T value);

    Long getForestId();

    String getName();

    String getHostname();

    String getMaster();

    String getDataDir();

    String getLargeDataDir();

    String getFastDataDir();

    UpdatesAllowed getUpdatesAllowed();

    boolean isRebalancerEnabled();

    boolean isFailoverEnabled();

    String getStatus();

    void awaitStatus(String expectedState);

    <T> T setConfig(ConfigKey<T> key, T val);
}
