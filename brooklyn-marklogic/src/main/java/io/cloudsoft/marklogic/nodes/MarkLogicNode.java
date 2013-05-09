package io.cloudsoft.marklogic.nodes;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.NamedParameter;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.webapp.JavaWebAppService;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.*;
import brooklyn.location.PortRange;
import brooklyn.location.basic.PortRanges;
import brooklyn.util.flags.SetFromFlag;
import com.google.common.collect.ImmutableList;
import io.cloudsoft.marklogic.forests.Forest;

/**
 * A node in a MarkLogic cluster, where it will be the master if {@code getConfig(IS_MASTER)}.
 */
@ImplementedBy(MarkLogicNodeImpl.class)
public interface MarkLogicNode extends SoftwareProcess {

    @SetFromFlag("version")
    ConfigKey<NodeType> NODE_TYPE = new BasicConfigKey<NodeType>(
            NodeType.class, "marklogic.node-type",
            "The type of the marklogic node; d-type only has forests, e-type only has appservers, d+e-type can have both", NodeType.E_D_NODE);

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = new BasicConfigKey<String>(
            SoftwareProcess.SUGGESTED_VERSION, "7.0-ea1_20130315");

    @SetFromFlag("websiteUsername")
    ConfigKey<String> WEBSITE_USERNAME = new BasicConfigKey<String>(
            String.class, "marklogic.website-username",
            "The username to access MarkLogic Server website", null);

    @SetFromFlag("websitePassword")
    ConfigKey<String> WEBSITE_PASSWORD = new BasicConfigKey<String>(
            String.class, "marklogic.website-password",
            "The password to access MarkLogic website", null);

    @SetFromFlag("user")
    ConfigKey<String> USER = new BasicConfigKey<String>(
            String.class, "marklogic.user",
            "The user to access MarkLogic Server Web UI", "admin");

    @SetFromFlag("password")
    ConfigKey<String> PASSWORD = new BasicConfigKey<String>(
            String.class, "marklogic.password",
            "The password to access MarkLogic Server Web UI", "hap00p");

    @SetFromFlag("awsAccessKey")
    ConfigKey<String> AWS_ACCESS_KEY = new BasicConfigKey<String>(
            String.class, "marklogic.aws-access-key",
            "The AWS Access Key", null);

    @SetFromFlag("awsSecretKey")
    ConfigKey<String> AWS_SECRET_KEY = new BasicConfigKey<String>(
            String.class, "marklogic.aws-secret-key",
            "The AWS Access Key", null);

    @SetFromFlag("licenseKey")
    ConfigKey<String> LICENSE_KEY = new BasicConfigKey<String>(
            String.class, "marklogic.licenseKey", "The license key to register the MarkLogic Server", null);

    @SetFromFlag("licensee")
    ConfigKey<String> LICENSEE = new BasicConfigKey<String>(
            String.class, "marklogic.licensee", "The licensee to register the MarkLogic Server", null);

    @SetFromFlag("fCount")
    ConfigKey<Integer> FCOUNT = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.fcount", "FCount", 4);

    @SetFromFlag("host")
    ConfigKey<String> HOST = new BasicConfigKey<String>(
            String.class, "marklogic.host",
            "The internal identifier of this marklogic node", null);

    @SetFromFlag("group")
    ConfigKey<String> GROUP = new BasicConfigKey<String>(
            String.class, "marklogic.group",
            "The MarkLogic group this node belongs to", "Default");


    @SetFromFlag("cluster")
    ConfigKey<String> CLUSTER = new BasicConfigKey<String>(
            String.class, "marklogic.cluster", "The cluster name", null);

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


    @SetFromFlag("availabilityZone")
    ConfigKey<String> AVAILABILITY_ZONE = new BasicConfigKey<String>(
            String.class, "marklogic.node.availabilityZone", "Availability zone to use (appended to the region name - e.g. could be \"c\")", "c");

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

    @SetFromFlag("numMountPoints")
    ConfigKey<Integer> NUM_MOUNT_POINTS = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.node.volumes.numMountPoints", "Number of regular EBS Volumes", 2);

    @SetFromFlag("varOptVolume")
    BasicAttributeSensorAndConfigKey<String> VAR_OPT_VOLUME = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "marklogic.node.volumes.varOpt", "EBS Volume ID for /var/opt (or null if does not already exist)", null);

    @SetFromFlag("backupVolume")
    BasicAttributeSensorAndConfigKey<String> BACKUP_VOLUME = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "marklogic.node.volumes.backup", "EBS Volume ID for the backup volume (or null if does not already exist)", null);

    //@SetFromFlag("regularVolumes")
    // BasicAttributeSensorAndConfigKey<Collection<String>> REGULAR_VOLUMES = new BasicAttributeSensorAndConfigKey(
    // 		Collection.class, "marklogic.node.volumes.regulars", "EBS Volume IDs for the regular volumes (or empty if does not already exist)", ImmutableList.<String>of());

    //@SetFromFlag("fastdirVolumes")
    //BasicAttributeSensorAndConfigKey<Collection<String>> FASTDIR_VOLUMES = new BasicAttributeSensorAndConfigKey(
    //  		Collection.class, "marklogic.node.volumes.fastdirs", "EBS Volume IDs for the fastdir volumes (or empty if does not already exist)", ImmutableList.<String>of());

    //@SetFromFlag("replicaVolumes")
    //BasicAttributeSensorAndConfigKey<Collection<String>> REPLICA_VOLUMES = new BasicAttributeSensorAndConfigKey(
    // 		Collection.class, "marklogic.node.volumes.replicas", "EBS Volume IDs for the replica volumes (or empty if does not already exist)", ImmutableList.<String>of());

    // FIXME Should be 100GB, but set to 5GB for now, for cheaper testing!
    @SetFromFlag("volumeSize")
    ConfigKey<Integer> VOLUME_SIZE = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.node.volumes.size", "The size of each EBS Volume for /var/opt, regular, fastdir and replica (if being created from scratch)", 5);

    // FIXME Should be 200GB, but set to 5GB for now, for cheaper testing!
    @SetFromFlag("backupVolumeSize")
    ConfigKey<Integer> BACKUP_VOLUME_SIZE = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.node.volumes.backupSize", "The size of backup EBS Volume (if being created from scratch)", 5);

    AttributeSensor<String> URL = new BasicAttributeSensor<String>(
            String.class, "marklogic.node.url", "Base URL for MarkLogic node");

    ConfigKey<Integer> BIND_PORT = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.bindPort", "The distributed protocol server socket bind internet port number.", 7999);

    ConfigKey<Integer> FOREIGN_BIND_PORT = new BasicConfigKey<Integer>(
            Integer.class, "marklogic.foreignBindPort", "The distributed protocol server socket bind internet port number.", 7998);

    //TODO: This should not be here, it is a temporary hack to let the nginx loadbalancer read out the port.
    PortAttributeSensorAndConfigKey APP_SERVICE_PORT = new PortAttributeSensorAndConfigKey(
            "http.port", "HTTP port", ImmutableList.of(8011));

    void createDatabaseWithForest(String name);

    void createForest(Forest forest);

    String getMasterAddress();

    String getHostName();

    void createRestAppServer(String name,String database,String groupName,String port);

    void createGroup(String groupName);

    void assignHostToGroup(String hostAddress, String groupName);

    String getGroupName();

    void createDatabase(String name);
}
