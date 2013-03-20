package io.cloudsoft.marklogic;

import java.util.Collection;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.collect.ImmutableList;

/**
 * A node in a MarkLogic cluster, where it will be the master if {@code getConfig(IS_MASTER)}.
 */
@ImplementedBy(MarkLogicNodeImpl.class)
public interface MarkLogicNode extends SoftwareProcess {

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = new BasicConfigKey<String>(
    		SoftwareProcess.SUGGESTED_VERSION, "6.0-2.3");

    // FIXME This doesn't work because gives 403 unless you include username/password in curl
    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "http://developer.marklogic.com/download/binaries/6.0/${driver.downloadFilename}");
    
    @SetFromFlag("isMaster")
    ConfigKey<Boolean> IS_MASTER = new BasicConfigKey<Boolean>(
            Boolean.class, "marklogic.node.ismaster", "Whether this node in the cluster is the master", false);
    
    @SetFromFlag("masterAddress")
    ConfigKey<String> MASTER_ADDRESS = new BasicConfigKey<String>(
            String.class, "marklogic.node.masterAddress", "If this is not the master, specifies the master address to use", null);
    
    @SetFromFlag("isStorageEbs")
    ConfigKey<Boolean> IS_STORAGE_EBS = new BasicConfigKey<Boolean>(
            Boolean.class, "marklogic.node.isStorageEbs", "Whether the storage should use EBS Volumes", true);

    @SetFromFlag("isBackupEbs")
    ConfigKey<Boolean> IS_BACKUP_EBS = new BasicConfigKey<Boolean>(
            Boolean.class, "marklogic.node.isBackupEbs", "Whether the backup should use an EBS Volume", true);

    @SetFromFlag("isReplicaEbs")
    ConfigKey<Boolean> IS_REPLICA_EBS = new BasicConfigKey<Boolean>(
            Boolean.class, "marklogic.node.isReplicaEbs", "Whether the replica should use an EBS Volume", true);

    @SetFromFlag("isFastdirEbs")
    ConfigKey<Boolean> IS_FASTDIR_EBS = new BasicConfigKey<Boolean>(
            Boolean.class, "marklogic.node.isFastdirEbs", "Whether the fastdir should use an EBS Volume", true);

    @SetFromFlag("autoScaleGroup")
    BasicAttributeSensorAndConfigKey<String> MARKLOGIC_AUTO_SCALE_GROUP = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "marklogic.node.autoScaleGroup", "<description goes here>", null);
    
    @SetFromFlag("sdbBucketName")
    ConfigKey<String> SDB_BUCKET_NAME = new BasicConfigKey<String>(
            String.class, "marklogic.node.sdbBucketName", "<description goes here>", null);
    
    @SetFromFlag("sdbClusterName")
    ConfigKey<String> SDB_CLUSTER_NAME = new BasicConfigKey<String>(
            String.class, "marklogic.node.sdbClusterName", "<description goes here>", null);
    
    @SetFromFlag("numMountPoints")
    ConfigKey<Integer> NUM_MOUNT_POINTS = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.node.volumes.numMountPoints", "Number of regular EBS Volumes", 2);
    
    @SetFromFlag("varOptVolume")
    BasicAttributeSensorAndConfigKey<String> VAR_OPT_VOLUME = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "marklogic.node.volumes.varOpt", "EBS Volume ID for /var/opt (or null if does not already exist)", null);
    
    @SetFromFlag("backupVolume")
    BasicAttributeSensorAndConfigKey<String> BACKUP_VOLUME = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "marklogic.node.volumes.backup", "EBS Volume ID for the backup volume (or null if does not already exist)", null);
    
    @SetFromFlag("regularVolumes")
    BasicAttributeSensorAndConfigKey<Collection<String>> REGULAR_VOLUMES = new BasicAttributeSensorAndConfigKey(
    		Collection.class, "marklogic.node.volumes.regulars", "EBS Volume IDs for the regular volumes (or empty if does not already exist)", ImmutableList.<String>of());

    @SetFromFlag("fastdirVolumes")
    BasicAttributeSensorAndConfigKey<Collection<String>> FASTDIR_VOLUMES = new BasicAttributeSensorAndConfigKey(
    		Collection.class, "marklogic.node.volumes.fastdirs", "EBS Volume IDs for the fastdir volumes (or empty if does not already exist)", ImmutableList.<String>of());

    @SetFromFlag("replicaVolumes")
    BasicAttributeSensorAndConfigKey<Collection<String>> REPLICA_VOLUMES = new BasicAttributeSensorAndConfigKey(
    		Collection.class, "marklogic.node.volumes.replicas", "EBS Volume IDs for the replica volumes (or empty if does not already exist)", ImmutableList.<String>of());

    // FIXME Should be 100GB, but set to 5GB for now
    @SetFromFlag("volumeSize")
    ConfigKey<Integer> VOLUME_SIZE = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.node.volumes.size", "The size of each EBS Volume for /var/opt, regular, fastdir and replica (if being created from scratch)", 5);

    // FIXME Should be 200GB, but set to 5GB for now
    @SetFromFlag("backupVolumeSize")
    ConfigKey<Integer> BACKUP_VOLUME_SIZE = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.node.volumes.backupSize", "The size of backup EBS Volume (if being created from scratch)", 5);

	AttributeSensor<String> URL = new BasicAttributeSensor<String>(
			String.class, "marklogic.node.url", "Base URL for MarkLogic node");
}
