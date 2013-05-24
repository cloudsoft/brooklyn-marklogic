package io.cloudsoft.marklogic.nodes;

import static brooklyn.util.ssh.CommonCommands.dontRequireTtyForSudo;
import static brooklyn.util.ssh.CommonCommands.sudo;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.forests.Forest;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.util.MutableMap;
import brooklyn.util.exceptions.Exceptions;
import brooklyn.util.text.Strings;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class MarkLogicNodeSshDriver extends AbstractSoftwareProcessSshDriver implements MarkLogicNodeDriver {

    /*
     * TODO Comment taken from Denis' original script for how he set up his volumes.
     * 
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

    
    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicNodeSshDriver.class);

    public final static AtomicInteger counter = new AtomicInteger(2);
    private final int nodeId;
    
    // Use device suffixes h through p; reuse where possible
    private final List<Character> freeDeviceNameSuffixes = Lists.newLinkedList();
    {
        for (char c = 'h'; c < 'p'; c++) {
            freeDeviceNameSuffixes.add(c);
        }
    }
    
    public MarkLogicNodeSshDriver(MarkLogicNodeImpl entity, SshMachineLocation machine) {
        super(entity, machine);
        this.nodeId = counter.getAndIncrement();
    }

    @Override
    public MarkLogicNodeImpl getEntity() {
        return (MarkLogicNodeImpl) super.getEntity();
    }
    
    public String getDownloadFilename() {
        // TODO To support other platforms, need to customize this based on OS
        return "MarkLogic-" + getVersion() + ".x86_64.rpm";
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return Integer.toString(getNodeId());
    }

    public int getFcount() {
        return entity.getConfig(MarkLogicNode.FCOUNT);
    }

    public String getWebsiteUsername() {
        return entity.getConfig(MarkLogicNode.WEBSITE_USERNAME);
    }

    public String getWebsitePassword() {
        return entity.getConfig(MarkLogicNode.WEBSITE_PASSWORD);
    }

    public String getUser() {
        return entity.getConfig(MarkLogicNode.USER);
    }

    public String getPassword() {
        return entity.getConfig(MarkLogicNode.PASSWORD);
    }

    public String getLicenseKey() {
        return entity.getConfig(MarkLogicNode.LICENSE_KEY);
    }

    public String getAwsAccessKey() {
        return entity.getConfig(MarkLogicNode.AWS_ACCESS_KEY);
    }

    public String getAwsSecretKey() {
        return entity.getConfig(MarkLogicNode.AWS_SECRET_KEY);
    }

    public String getLicensee() {
        return entity.getConfig(MarkLogicNode.LICENSEE).replace(" ", "%20");
    }

    public String getClusterName() {
        return entity.getConfig(MarkLogicNode.CLUSTER_NAME).replace(" ", "%20");
    }

    public boolean isInitialHost() {
        return entity.getConfig(MarkLogicNode.IS_INITIAL_HOST);
    }

    public File getBrooklynMarkLogicHome() {
        String home = System.getenv("BROOKLYN_MARKLOGIC_HOME");
        if (home == null) {
            home = System.getProperty("user.dir");
            LOG.warn("BROOKLYN_MARKLOGIC_HOME not found in environment, defaulting to [{}]", home);
        }
        return new File(home);
    }

    public File getScriptDirectory() {
        return new File(getBrooklynMarkLogicHome(), "scripts");
    }

    public File getUploadDirectory() {
        return new File(getBrooklynMarkLogicHome(), "upload");
    }

    @Override
    public void install() {
        LOG.info("Setting up volumes of MarkLogic host {}", getHostname());
        
        if (getMachine() instanceof JcloudsSshMachineLocation) {
            JcloudsSshMachineLocation jcloudsMachine = (JcloudsSshMachineLocation) getMachine();
            JcloudsLocation jcloudsLocation = jcloudsMachine.getParent();
            String varOptVolumeId = entity.getConfig(MarkLogicNode.VAR_OPT_VOLUME);
            Boolean isVarOptEbs = entity.getConfig(MarkLogicNode.IS_VAR_OPT_EBS);
            Boolean isBackupEbs = entity.getConfig(MarkLogicNode.IS_BACKUP_EBS);
            String backupVolumeId = entity.getConfig(MarkLogicNode.BACKUP_VOLUME);
            Integer volumeSize = entity.getConfig(MarkLogicNode.VOLUME_SIZE);
            Integer backupVolumeSize = entity.getConfig(MarkLogicNode.BACKUP_VOLUME_SIZE);

            if ("aws-ec2".equals(jcloudsLocation.getProvider())) {
                if (isVarOptEbs) {
                    if (Strings.isBlank(varOptVolumeId)) {
                        String newVolumeId = createAttachAndMountVolume("/var/opt", volumeSize, getNodeName()+"-varopt");
                        entity.setAttribute(MarkLogicNode.VAR_OPT_VOLUME, newVolumeId);
                    } else {
                        attachAndMountVolume(varOptVolumeId, "/var/opt");
                    }
                }
                
                if (isBackupEbs) {
                    if (Strings.isBlank(backupVolumeId)) {
                        String newVolumeId = createAttachAndMountVolume("/var/opt/backup", backupVolumeSize, getNodeName()+"-backup");
                        entity.setAttribute(MarkLogicNode.BACKUP_VOLUME, newVolumeId);
                    } else {
                        attachAndMountVolume(backupVolumeId, "/var/opt/backup");
                    }
                }
            } else {
                LOG.warn("Volumes currently not supported for machine {} in location {}", getMachine(), getMachine().getParentLocation());
            }
        }
        
        LOG.info("Starting installation of MarkLogic host {}", getHostname());

        uninstall();
        uploadFiles();

        String script = processTemplate(new File(getScriptDirectory(), "install.txt"));
        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript(MutableMap.of("nonStandardLayout", "true"), INSTALLING)
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.info("Finished installation of MarkLogic host {}", getHostname());
    }

    private void uninstall() {
        String installScript = processTemplate(new File(getScriptDirectory(), "uninstall.txt"));
        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(installScript);
        newScript("uninstall")
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();
    }

    private void uploadFiles() {
        LOG.info("Starting upload to {}", getHostname());
        try {
            File dir = getUploadDirectory();
            File zipFile = File.createTempFile("upload", "zip");
            zipFile.deleteOnExit();
            zip(dir, zipFile);
            getLocation().copyTo(zipFile, "./upload.zip");
            zipFile.delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Finished upload to {}", getHostname());

    }

    public static void zip(File directory, File zipfile) throws IOException {
        URI base = directory.toURI();
        Deque<File> queue = new LinkedList<File>();
        queue.push(directory);
        OutputStream out = new FileOutputStream(zipfile);
        Closeable res = out;
        try {
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.pop();
                for (File kid : directory.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath();
                    if (kid.isDirectory()) {
                        queue.push(kid);
                        name = name.endsWith("/") ? name : name + "/";
                        zout.putNextEntry(new ZipEntry(name));
                    } else if (!kid.getName().equals(".DS_Store")) {
                        zout.putNextEntry(new ZipEntry(name));
                        copy(kid, zout);
                        zout.closeEntry();
                    }
                }
            }
        } finally {
            res.close();
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int readCount = in.read(buffer);
            if (readCount < 0) {
                break;
            }
            out.write(buffer, 0, readCount);
        }
    }

    private static void copy(File file, OutputStream out) throws IOException {
        InputStream in = new FileInputStream(file);
        try {
            copy(in, out);
        } finally {
            in.close();
        }
    }


    private void uploadFiles(File file, String targetDirectory) {
        getLocation().exec(Arrays.asList("mkdir -p " + targetDirectory), MutableMap.of());
        getLocation().copyTo(file, targetDirectory);
    }


    @Override
    public void customize() {
        final MarkLogicCluster cluster = ((MarkLogicNode) getEntity()).getCluster();
        boolean isInitialHost = cluster == null || cluster.claimToBecomeInitialHost();
        getEntity().setConfig(MarkLogicNode.IS_INITIAL_HOST, isInitialHost);

        File scriptFile;
        Map<String, Object> substitutions = new HashMap<String, Object>();
        if (isInitialHost) {
            LOG.info("Starting customize of MarkLogic initial host {}", getHostname());
            scriptFile = new File(getScriptDirectory(), "customize_initial_host.txt");
            substitutions = new HashMap<String, Object>();
        } else {
            LOG.info("Additional host {} waiting for MarkLogic initial host to be up", getHostname());
            MarkLogicNode node = cluster.getAnyNodeOrWait();
            LOG.info("Starting customize of Marklogic additional host {}", getHostname());
            scriptFile = new File(getScriptDirectory(), "customize_additional_host.txt");
            substitutions.put("clusterHostName", node.getHostName());
        }

        String script = processTemplate(scriptFile, substitutions);
        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript(MutableMap.of("nonStandardLayout", "true"), INSTALLING)
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        if (isInitialHost) {
            LOG.info("Finished customize of MarkLogic initial host {}", getHostname());
        } else {
            LOG.info("Finished customize of Marklogic additional host {}", getHostname());
        }
    }

    @Override
    public void launch() {
        List<String> commands = new LinkedList<String>();
        commands.add(sudo("/etc/init.d/MarkLogic start"));
        commands.add("sleep 10"); // Have seen cases where startup takes some time

        newScript(LAUNCHING)
                .failOnNonZeroResultCode()
                .body.append(commands)
                .execute();

        LOG.info("Successfully launched MarkLogic host {}", getHostname());
    }

    @Override
    public void postLaunch() {
        entity.setAttribute(MarkLogicNode.URL, String.format("http://%s:%s", getHostname(), 8001));
    }

    public boolean isRunning() {
        int exitStatus = newScript(LAUNCHING)
                .failOnNonZeroResultCode()
                .body.append(sudo("/etc/init.d/MarkLogic status | grep running"))
                .execute();
        return exitStatus == 0;
    }

    @Override
    public void stop() {
        newScript(LAUNCHING)
                .failOnNonZeroResultCode()
                .body.append(sudo("/etc/init.d/MarkLogic stop"))
                .execute();
    }

    //todo: method can be removed when we upgrade to brooklyn 0.6
    public String processTemplate(File templateConfigFile, Map<String, Object> extraSubstitutions) {
        return processTemplate(templateConfigFile.toURI().toASCIIString(), extraSubstitutions);
    }

    //todo: method can be removed when we upgrade to brooklyn 0.6
    public String processTemplate(String templateConfigUrl, Map<String, Object> extraSubstitutions) {
        Map<String, Object> config = getEntity().getApplication().getManagementContext().getConfig().asMapWithStringKeys();
        Map<String, Object> substitutions = ImmutableMap.<String, Object>builder()
                .putAll(config)
                .put("entity", entity)
                .put("driver", this)
                .put("location", getLocation())
                .putAll(extraSubstitutions)
                .build();

        try {
            String templateConfigFile = getResourceAsString(templateConfigUrl);

            Configuration cfg = new Configuration();
            StringTemplateLoader templateLoader = new StringTemplateLoader();
            templateLoader.putTemplate("config", templateConfigFile);
            cfg.setTemplateLoader(templateLoader);
            Template template = cfg.getTemplate("config");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Writer out = new OutputStreamWriter(baos);
            template.process(substitutions, out);
            out.flush();

            return new String(baos.toByteArray());
        } catch (Exception e) {
            LOG.warn("Error creating configuration file for " + entity, e);
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void createForest(Forest forest) {
        LOG.debug("Starting create forest {}", forest.getName());

        if (getMachine() instanceof JcloudsSshMachineLocation) {
            JcloudsSshMachineLocation jcloudsMachine = (JcloudsSshMachineLocation) getMachine();
            JcloudsLocation jcloudsLocation = jcloudsMachine.getParent();
            Boolean isForestsEbs = entity.getConfig(MarkLogicNode.IS_FORESTS_EBS);
            Integer volumeSize = entity.getConfig(MarkLogicNode.VOLUME_SIZE);
            Boolean isFastdirEbs = entity.getConfig(MarkLogicNode.IS_FASTDIR_EBS);
            String regularMountPoint = forest.getFastDataDir();
            String fastdirMountPoint = forest.getDataDir();

            if ("aws-ec2".equals(jcloudsLocation.getProvider())) {
                if (isForestsEbs && regularMountPoint != null) {
                    // TODO In startup_script, mount points are:
                    //   /var/opt/mldata/$sdb_bucket_name-$node_name-fastdir-$vol_count
                    //   /var/opt/mldata/$sdb_bucket_name-$node_name-replica-$vol_count
                    //   /var/opt/mldata/$sdb_bucket_name-$node_name-$vol_count
                    
                    String regularVolumeId = createAttachAndMountVolume(regularMountPoint, volumeSize, "forest-datadir-"+forest.getName()+"-"+getNodeId());
                    forest.setDataDirVolumeId(regularVolumeId);
                }
                
                if (isFastdirEbs && fastdirMountPoint != null && !fastdirMountPoint.equals(regularMountPoint)) {
                    String fastdirVolumeId = createAttachAndMountVolume(fastdirMountPoint, volumeSize, "forest-fastdir-"+forest.getName()+"-"+getNodeId());
                    forest.setFastDirVolumeId(fastdirVolumeId);
                }
                
            } else {
                LOG.warn("Volumes currently not supported for machine {} in location {}", getMachine(), getMachine().getParentLocation());
            }
        }
        
        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("forest", forest);
        File scriptFile = new File(getScriptDirectory(), "create_forest.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("createForest")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished creating forest {}", forest.getName());
    }

    @Override
    public void createDatabaseWithForest(String name) {
        LOG.debug("Starting create database-with-forest {}", name);

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("database", name);
        File scriptFile = new File(getScriptDirectory(), "create_database_with_forest.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("createDatabaseWithForest")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished creating database-with-forest {}", name);
    }

    @Override
    public void createDatabase(Database database) {
        LOG.debug("Starting create database {}", database.getName());

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("database", database);
        File scriptFile = new File(getScriptDirectory(), "create_database.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("create_database")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();


        LOG.debug("Finished create database {}", database.getName());
    }

    @Override
    public void createAppServer(String name, String database, String groupName, String port) {
        LOG.debug("Starting create appServer{} ", name);

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("appServer", name, "port", port, "database", database, "groupName", groupName);
        File scriptFile = new File(getScriptDirectory(), "create_appserver.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("createRestAppServer")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished creating appServer {}", name);
    }

    @Override
    public void createGroup(String name) {
        LOG.debug("Starting create group {}", name);

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("group", name);
        File scriptFile = new File(getScriptDirectory(), "create_group.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("createGroup")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished creating group {}", name);
    }

    @Override
    public void assignHostToGroup(String hostAddress, String groupName) {
        LOG.debug("Assigning host '" + hostAddress + "'+ to group " + groupName);

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("groupName", groupName, "hostName", hostAddress);
        File scriptFile = new File(getScriptDirectory(), "assign_host_to_group.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("assignHostToGroup")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished Assigning host '" + hostAddress + "'+ to group " + groupName);
    }

    @Override
    public void attachForestToDatabase(String forestName, String databaseName) {
        LOG.debug("Attach forest {} to database {}", forestName, databaseName);

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("forestName", forestName, "databaseName", databaseName);
        File scriptFile = new File(getScriptDirectory(), "attach_forest_to_database.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("attachForestToDatabase")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished attaching forest {} to database {}", forestName, databaseName);
    }

    @Override
    public void attachReplicaForest(String primaryForestName, String replicaForestName) {

        LOG.debug("Attach replica forest {} to forest {}", replicaForestName, primaryForestName);

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("primaryForestName", primaryForestName, "replicaForestName", replicaForestName);
        File scriptFile = new File(getScriptDirectory(), "attach_replica_forest.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("attachReplicaForest")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished Attach replica forest {} to forest {}", replicaForestName, primaryForestName);

    }

    @Override
    public void enableForest(String forestName, boolean enabled) {

        LOG.debug("Enabling forest {} {}", forestName, enabled);

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("forestName", forestName, "enabled", enabled);
        File scriptFile = new File(getScriptDirectory(), "enable_forest.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("enableForest")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished Enabling forest {} {}", forestName, enabled);
    }

    @Override
    public void deleteForestConfiguration(String forestName) {
        LOG.debug("Deleting configuration for forest {}", forestName);

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("forestName", forestName);
        File scriptFile = new File(getScriptDirectory(), "delete_forest_config_only.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("deleteForest")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished deleting configuration for forest {}", forestName);
    }

    @Override
    public void setForestHost(String forestName, String hostName) {
        LOG.debug("Setting forest {} host {}", forestName, hostName);

        Map<String, Object> extraSubstitutions = MutableMap.<String, Object>of("forestName", forestName, "hostName",hostName);
        File scriptFile = new File(getScriptDirectory(), "forest_set_host.txt");
        String script = processTemplate(scriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);
        newScript("setForestHost")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        LOG.debug("Finished setting forest {} host {}", forestName, hostName);
    }


    @Override
    public Set<String> scanAppServices() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> scanDatabases() {
        LOG.debug("Scanning databases");

        File scriptFile = new File(getScriptDirectory(), "scan_databases.txt");
        String script = processTemplate(scriptFile);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        //todo:
        int exitStatus = getMachine().run(MutableMap.of("out", stdout, "err", stderr), script, new HashMap());
        if (exitStatus != 0) {
            LOG.error("Failed to databases");
            return ImmutableSet.of();
        }
        String s = new String(stdout.toByteArray());

        Set<String> databases = new HashSet<String>();
        String[] split = s.split("\n");
        for (int k = 0; k < split.length - 1; k++) {
            databases.add(split[k]);
        }

        return databases;
    }

    @Override
    public Set<String> scanForests() {
        LOG.debug("Scanning forests");

        File scriptFile = new File(getScriptDirectory(), "scan_forests.txt");
        String script = processTemplate(scriptFile);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        //todo:
        int exitStatus = getMachine().run(MutableMap.of("out", stdout, "err", stderr), script, new HashMap());
        if (exitStatus != 0) {
            LOG.error("Failed to scan forests");
            return Collections.EMPTY_SET;
        }
        String s = new String(stdout.toByteArray());

        Set<String> forests = new HashSet();
        String[] split = s.split("\n");
        for (int k = 0; k < split.length - 1; k++) {
            forests.add(split[k]);
        }
        return forests;
    }
    
    private String createAttachAndMountVolume(String mountPoint, int volumeSize, String tagNameSuffix) {
        JcloudsSshMachineLocation jcloudsMachine = (JcloudsSshMachineLocation) getMachine();
        JcloudsLocation jcloudsLocation = jcloudsMachine.getParent();
        
        char deviceSuffix = claimDeviceSuffix();
        String volumeDeviceName = "/dev/sd" + deviceSuffix;
        String osDeviceName = "/dev/xvd" + deviceSuffix;
        String filesystemType = "ext3";
        String availabilityZone = jcloudsMachine.getNode().getLocation().getId();
        Map<String,String> tags = ImmutableMap.of("Name", "marklogic-"+getClusterName()+(tagNameSuffix!=null ? "-"+tagNameSuffix : ""));
        
        if ("aws-ec2".equals(jcloudsLocation.getProvider())) {
            return EbsVolumeCustomizer.createAttachAndMountNewVolume(jcloudsMachine, volumeDeviceName, osDeviceName, mountPoint, filesystemType, availabilityZone, volumeSize, tags);
        } else {
            throw new IllegalStateException("Cannot handle volumes in location "+jcloudsLocation);
        }
    }
    
    private void attachAndMountVolume(String volumeId, String mountPoint) {
        JcloudsSshMachineLocation jcloudsMachine = (JcloudsSshMachineLocation) getMachine();
        JcloudsLocation jcloudsLocation = jcloudsMachine.getParent();
        
        char deviceSuffix = claimDeviceSuffix();
        String volumeDeviceName = "/dev/sd" + deviceSuffix;
        String osDeviceName = "/dev/xvd" + deviceSuffix;
        String filesystemType = "ext3";
        
        if ("aws-ec2".equals(jcloudsLocation.getProvider())) {
            EbsVolumeCustomizer.attachAndMountVolume(jcloudsMachine, volumeId, volumeDeviceName, osDeviceName, mountPoint, filesystemType);
        } else {
            throw new IllegalStateException("Cannot handle volumes in location "+jcloudsLocation);
        }
    }

    private char claimDeviceSuffix() {
        Character suffix;
        synchronized (freeDeviceNameSuffixes) {
            if (freeDeviceNameSuffixes.isEmpty()) {
                throw new IllegalStateException("No device-name suffixes available; all in use for "+getEntity());
            }
            suffix = freeDeviceNameSuffixes.remove(0);
        }
        return Character.toLowerCase(suffix);
    }
    
    private void releaseDeviceSuffix(Character suffix) {
        checkNotNull(suffix, "device-suffix must not be null for entity %s", suffix, getEntity());
        synchronized (freeDeviceNameSuffixes) {
            checkState(!freeDeviceNameSuffixes.contains(suffix), "Attempt to release device-suffix %s when not claimed, for entity %s", suffix, getEntity());
            freeDeviceNameSuffixes.add(0, suffix);
        }
    }
}
