package io.cloudsoft.marklogic.forests;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;

import static brooklyn.entity.basic.ConfigKeys.*;
import static brooklyn.event.basic.Sensors.newLongSensor;
import static brooklyn.event.basic.Sensors.newStringSensor;

/**
 * A forest is a repository for documents. Forests are managed by a single host - in Brooklyn a
 * {@link io.cloudsoft.marklogic.nodes.MarkLogicNode MarkLogicNode}. Hosts may have many forests.
 */
@ImplementedBy(ForestImpl.class)
public interface Forest extends Entity, Startable {

    ConfigKey<String> NAME = newStringConfigKey(
            "marklogic.forest.name",
            "The name of the forest");

    //todo: needed for the time being since we don't suck in all state from marklogic.
    ConfigKey<Boolean> CREATED_BY_BROOKLYN = newBooleanConfigKey(
            "marklogic.forest.createdByBrooklyn",
            "If the forest is created by brooklyn", false);

    ConfigKey<String> MASTER = newStringConfigKey(
            "marklogic.forest.master", "" +
            "The name of the master forest (null if there is no master)");

    ConfigKey<String> HOST = newStringConfigKey(
            "marklogic.forest.host",
            "Specifies the host on which the forest resides.");

    ConfigKey<String> DATA_DIR = newStringConfigKey(
            "marklogic.forest.data-dir",
            "Specifies a public directory in which the forest is located. If the data directory is not specified, the " +
                    "forest will be created in the default forest directory on the host machine and the specified host " +
                    "machine cannot be changed once the forest is created.");

    ConfigKey<String> LARGE_DATA_DIR = newStringConfigKey(
            "marklogic.forest.large-data-dir",
            "Specifies a directory in which large objects are stored. If the directory is not specified, large objects will be stored under the data directory.");

    ConfigKey<String> FAST_DATA_DIR = newStringConfigKey(
            "marklogic.forest.fast-data-dir",
            "Specifies a directory that is smaller but faster than the data directory. The directory should be on a different storage device than the data directory.");

    ConfigKey<UpdatesAllowed> UPDATES_ALLOWED = newConfigKey(
            UpdatesAllowed.class, "marklogic.forest.updates-allowed",
            "Specifies which operations are allowed on this forest.",
            UpdatesAllowed.ALL);

    ConfigKey<Boolean> REBALANCER_ENABLED = newBooleanConfigKey(
            "marklogic.forest.rebalancer-enabled",
            "Enable automatic rebalancing after configuration changes.",
            true);

    ConfigKey<Boolean> FAILOVER_ENABLED = newBooleanConfigKey(
            "marklogic.forest.failover-enabled",
            "Enable assignment to a failover host if the primary host is down.",
            false);

    ConfigKey<MarkLogicGroup> GROUP = newConfigKey(
            MarkLogicGroup.class,
            "marklogic.forests.group", "The group");

    BasicAttributeSensorAndConfigKey<VolumeInfo> DATA_DIR_VOLUME_INFO = new BasicAttributeSensorAndConfigKey<VolumeInfo>(
            VolumeInfo.class, "marklogic.forest.data-dir.volumeId",
            "Specifies a volume id in which the forest's regular data is located. If null, indicates that no volume " +
                    "has been created yet for this forest. If DATA_DIR is null then the volumeId should also be null", null);

    BasicAttributeSensorAndConfigKey<VolumeInfo> FAST_DATA_DIR_VOLUME_INFO = new BasicAttributeSensorAndConfigKey<VolumeInfo>(
            VolumeInfo.class, "marklogic.forest.fast-data-dir.volumeId",
            "Specifies a volume id in which the forest's fast data is located. If null, indicates that no volume " +
                    "has been created yet. If FAST_DATA_DIR is null then the volumeId should also be null", null);

    AttributeSensor<String> STATUS = newStringSensor(
            "forest.status",
            "The status of the forest");

    AttributeSensor<Long> FOREST_ID = newLongSensor(
            "forest.id",
            "The id of the forest");


    /**
     * Exposed for use by {@link io.cloudsoft.marklogic.nodes.MarkLogicNodeSshDriver#mountForest} and
     * {@link io.cloudsoft.marklogic.nodes.MarkLogicNodeSshDriver#unmountForest unmountForest}.
     */
    <T> T setAttribute(AttributeSensor<T> attribute, T value);

    /** Exposed for use by {@link ForestsImpl#attachReplicaForest} and {@link ForestsImpl#setForestHost(String, String)}. */
    <T> T setConfig(ConfigKey<T> key, T val);

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

    void awaitStatus(String... expectedState);

    boolean createdByBrooklyn();
}
