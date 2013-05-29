package io.cloudsoft.marklogic.nodes;

import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;
import brooklyn.location.MachineProvisioningLocation;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsLocationCustomizer;
import brooklyn.util.MutableMap;
import com.google.common.base.Functions;
import com.google.common.collect.*;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.forests.Forest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public class MarkLogicNodeImpl extends SoftwareProcessImpl implements MarkLogicNode {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicNodeImpl.class);

    private final AtomicInteger deviceNameSuffix = new AtomicInteger('h');
    private static final String NODE_NAME = "mynodename";

    private FunctionFeed serviceUp;

    @Override
    public void init() {
        //we give it a bit longer timeout for starting up
        setConfig(ConfigKeys.START_TIMEOUT, 240);

        //todo: ugly.. we don't want to get the properties  this way, but for the time being it works.
        setConfig(WEBSITE_USERNAME, getManagementContext().getConfig().getFirst("brooklyn.marklogic.website-username"));
        setConfig(WEBSITE_PASSWORD, getManagementContext().getConfig().getFirst("brooklyn.marklogic.website-password"));
        setConfig(LICENSE_KEY, getManagementContext().getConfig().getFirst("brooklyn.marklogic.license-key"));
        setConfig(LICENSEE, getManagementContext().getConfig().getFirst("brooklyn.marklogic.licensee"));
        setConfig(AWS_ACCESS_KEY, getManagementContext().getConfig().getFirst("brooklyn.marklogic.aws-access-key"));
        setConfig(AWS_SECRET_KEY, getManagementContext().getConfig().getFirst("brooklyn.marklogic.aws-secret-key"));
        setConfig(FCOUNT, Integer.parseInt(getManagementContext().getConfig().getFirst("brooklyn.marklogic.fcount")));
        setConfig(CLUSTER_NAME, getManagementContext().getConfig().getFirst("brooklyn.marklogic.cluster"));
        String configuredVersion = getManagementContext().getConfig().getFirst("brooklyn.marklogic.version");
        if (configuredVersion != null && !configuredVersion.isEmpty()) {
            setConfig(SoftwareProcess.SUGGESTED_VERSION, configuredVersion);
        }
    }

    public Class getDriverInterface() {
        return MarkLogicNodeDriver.class;
    }

    /**
     * Sets up the polling of sensors.
     */
    @Override
    protected void connectSensors() {
        super.connectSensors();

        serviceUp = FunctionFeed.builder()
                .entity(this)
                .period(5000)
                .poll(new FunctionPollConfig<Boolean, Boolean>(SERVICE_UP)
                        .onError(Functions.constant(Boolean.FALSE))
                        .callable(new Callable<Boolean>() {
                            public Boolean call() {
                                return getDriver().isRunning();
                            }
                        }))
                .build();
    }

    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();

        if (serviceUp != null) serviceUp.stop();
    }

    /**
     * The ports to be opened in the VM (e.g. in the aws-ec2 security group created by jclouds).
     */
    @Override
    protected Collection<Integer> getRequiredOpenPorts() {
        // TODO What ports need to be open?
        // I got these from `sudo netstat -antp` for the MarkLogic daemon
        // TODO If want to use a pre-existing security group instead, can add to
        //      obtainProvisioningFlags() something like:
        //      .put("securityGroups", groupName)
        //TODO: the 8011 port has been added so we can register an application on that port. In the future this needs to come
        //from the application, but for the time being it is hard coded.
        int bindPort = getConfig(BIND_PORT);
        int foreignBindPort = getConfig(FOREIGN_BIND_PORT);
        return ImmutableSet.copyOf(Iterables.concat(super.getRequiredOpenPorts(), ImmutableList.of(bindPort, foreignBindPort, 8000, 8001, 8002, 8011)));
    }

    @Override
    protected Map<String, Object> obtainProvisioningFlags(MachineProvisioningLocation location) {
        if (location instanceof JcloudsLocation) {
            return MutableMap.<String, Object>builder()
                    .putAll(super.obtainProvisioningFlags(location))
         //           .put("customizer", getEbsVolumeCustomizer((JcloudsLocation) location))
                    .build();
        } else {
            LOG.warn("Location {} is not a jclouds-location, so cannot setup EBS volumes for {}", location, this);
            return super.obtainProvisioningFlags(location);
        }
    }

    /**
     * Configures the location so that any instances will have the required EBS volumes attached.
     * Also sets up the datadir for the given instance, to use that EBS volume.
     * <p/>
     * Algorithm
     * Lock the ASG meta Lock
     * <p/>
     * Do I exist in the ASG List
     * Yes                                No
     * Reattach                       Any free slots in the ASG List
     * Yes                    No
     * Create Node             Any Dead nodes in the ASG List
     * Create Volumes       No                        yes
     * Attach             Start Standalone      Takeover Node
     * Attach volumes
     * <p/>
     * Unlock the ASG meta lock
     * <p/>
     * Lock Cluster Lock
     * Check Cluster Master
     * 1. none?   Become Master - Database Created?  Create DB
     * 2. Dead?   Did I take over his node?  Yes I am Master, No wait for master
     * 3  Alive?  Nothing to do
     * UnLock Cluster Lock
     * <p/>
     * Same node? Nothing to do
     * New node?  Join cluster
     * Takeover node?  Fix host, become master if I am master
     */
    protected JcloudsLocationCustomizer getEbsVolumeCustomizer(JcloudsLocation location) {
        // TODO Currently only new node, and semi-handling replacing a node if configured with appropriate volume ids

        List<JcloudsLocationCustomizer> customizers = Lists.newArrayList();

        // TODO Use hex_val to get proper mount points
        String varOptVolumeId = getConfig(VAR_OPT_VOLUME);
        customizers.add(createOrAttachVolumeCustomizer(location, varOptVolumeId, "/var/opt", getConfig(VOLUME_SIZE)));

        if (getConfig(IS_BACKUP_EBS)) {
            // TODO Get the volumeId for the volume created here, so can set the attribute accordingly; same for other volume attributes
            String backupVolumeId = getConfig(BACKUP_VOLUME);
            customizers.add(createOrAttachVolumeCustomizer(location, backupVolumeId, "/var/opt/backup", getConfig(BACKUP_VOLUME_SIZE)));
        }

        if (getConfig(IS_STORAGE_EBS)) {
            // TODO In startup_script, mount points are:
            //   /var/opt/mldata/$sdb_bucket_name-$node_name-fastdir-$vol_count
            //   /var/opt/mldata/$sdb_bucket_name-$node_name-replica-$vol_count
            //   /var/opt/mldata/$sdb_bucket_name-$node_name-$vol_count

            int numMountPoints = getConfig(NUM_MOUNT_POINTS);

            Map<String, String> regularVolumes = toVolumeMountsMap(getConfig(REGULAR_VOLUMES), "/var/opt/mldata/" + NODE_NAME + "-", numMountPoints);

            customizers.addAll(createOrAttachVolumeCustomizers(location, regularVolumes, getConfig(VOLUME_SIZE)));

            if (getConfig(IS_FASTDIR_EBS)) {
                Map<String, String> fastdirVolumes = toVolumeMountsMap(getConfig(FASTDIR_VOLUMES), "/var/opt/mldata/" + NODE_NAME + "-fastdir-", numMountPoints);
                customizers.addAll(createOrAttachVolumeCustomizers(location, fastdirVolumes, getConfig(VOLUME_SIZE)));
            }

            if (getConfig(IS_REPLICA_EBS)) {
                Map<String, String> replicaVolumes = toVolumeMountsMap(getConfig(REPLICA_VOLUMES), "/var/opt/mldata/" + NODE_NAME + "-fastdir-", numMountPoints);
                customizers.addAll(createOrAttachVolumeCustomizers(location, replicaVolumes, getConfig(VOLUME_SIZE)));
            }
        }

        return new CompoundJcloudsLocationCustomizer(customizers);
    }
    private String nextDeviceSuffix() {
        return Character.toString((char) deviceNameSuffix.getAndIncrement()).toLowerCase();
    }

    private Map<String, String> toVolumeMountsMap(Collection <String> volumeIds, String mountPointPrefix, int numVolumes) {
        Map<String, String> result = Maps.newLinkedHashMap();
        int count = 0;
        for (String volumeId : volumeIds) {
            result.put(volumeId, mountPointPrefix + (count++));
        }
        for (int i = result.size(); i < numVolumes; i++) {
            result.put(null, mountPointPrefix + (count++));
        }
        return result;
    }

    /**
     * Returns a customizer that will either attach an existing volume (if volumeId is non-null) or create a
     * new volume of the given size.
     */
    private JcloudsLocationCustomizer createOrAttachVolumeCustomizer(JcloudsLocation location, String volumeId, String mountPoint, int sizeInGib) {
        // TODO Need to get correct ec2DeviceName and osDeviceName, I presume?
        // TODO Don't hard-code availability zone suffix
        String region = location.getRegion();
        String availabilityZone = region + getConfig(AVAILABILITY_ZONE);
        String deviceSuffix = nextDeviceSuffix();
        String ec2DeviceName = "/dev/sd" + deviceSuffix;
        String osDeviceName = "/dev/xvd" + deviceSuffix;
        String filesystemType = "ext3";

        LOG.info("volumeId: "+volumeId+" mountPoint: "+mountPoint +" ec2DeviceName: "+ec2DeviceName);

        return (volumeId != null) ?
                EbsVolumeCustomizer.withExistingVolume(ec2DeviceName, osDeviceName, mountPoint, region, availabilityZone, volumeId) :
                EbsVolumeCustomizer.withNewVolume(ec2DeviceName, osDeviceName, mountPoint, filesystemType, availabilityZone, sizeInGib, false);
    }

    private List<JcloudsLocationCustomizer> createOrAttachVolumeCustomizers(JcloudsLocation location, Map<String, String> volumes, int sizeInGib) {
        List<JcloudsLocationCustomizer> result = Lists.newArrayList();
        for (Map.Entry<String, String> entry : volumes.entrySet()) {
            String volumeId = entry.getKey();
            String mountPoint = entry.getValue();
            result.add(createOrAttachVolumeCustomizer(location, volumeId, mountPoint, sizeInGib));
        }
        return result;
    }

    private NodeType getNodeType() {
        return getConfig(NODE_TYPE);
    }

    @Override
    public MarkLogicNodeDriver getDriver() {
        return (MarkLogicNodeDriver) super.getDriver();
    }

    public String getHostName() {
        return getAttribute(HOSTNAME);
    }

    public String getGroupName() {
        return getConfig(GROUP);
    }

    @Override
    public void createForest(Forest forest) {
        getDriver().createForest(forest);
    }

    @Override
    public void createDatabaseWithForest(String name) {
        getDriver().createDatabaseWithForest(name);
    }

    @Override
    public void createDatabase(Database database) {
        getDriver().createDatabase(database);
    }

    @Override
    public void createRestAppServer(String name, String database, String groupName, String port) {
        LOG.info(format("Creating appServer '%s'", name));
        getDriver().createAppServer(name, database, groupName, port);
    }

    @Override
    public void attachForestToDatabase(String forestName, String databaseName) {
        getDriver().attachForestToDatabase(forestName, databaseName);
    }

    @Override
    public void createGroup(String groupName) {
        getDriver().createGroup(groupName);
    }

    @Override
    public void assignHostToGroup(String hostName, String groupName) {
        getDriver().assignHostToGroup(hostName, groupName);
    }

    @Override
    public Set<String> scanForests() {
        return getDriver().scanForests();
    }

    @Override
    public Set<String> scanDatabases() {
        return getDriver().scanDatabases();
    }

    @Override
    public boolean isUp() {
        return getAttribute(SERVICE_UP);
    }

    @Override
    public MarkLogicCluster getCluster() {
        return getConfig(CLUSTER);
    }

    @Override
    public void attachReplicaForest(String primaryForestName, String replicaForestName) {
        getDriver().attachReplicaForest(primaryForestName,replicaForestName);
    }

    @Override
       public void enableForest(String forestName, boolean enabled) {
        getDriver().enableForest(forestName, enabled);
    }

    @Override
    public void deleteForestConfiguration(String forestName) {
        getDriver().deleteForestConfiguration(forestName);
    }

    @Override
    public void setForestHost(String forestName, String hostname) {
        getDriver().setForestHost(forestName, hostname);
    }

    @Override
    public String getForestStatus(String forestName) {
        return getDriver().getForestStatus(forestName);
    }

    @Override
    public String getPassword() {
        return getConfig(PASSWORD);
    }

    @Override
    public String getUser() {
       return getConfig(USER);
    }
}
