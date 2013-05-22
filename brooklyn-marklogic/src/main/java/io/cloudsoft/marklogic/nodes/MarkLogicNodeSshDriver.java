package io.cloudsoft.marklogic.nodes;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.MutableMap;
import brooklyn.util.exceptions.Exceptions;
import com.google.common.collect.ImmutableMap;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.forests.Forest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static brooklyn.util.ssh.CommonCommands.dontRequireTtyForSudo;
import static brooklyn.util.ssh.CommonCommands.sudo;

public class MarkLogicNodeSshDriver extends AbstractSoftwareProcessSshDriver implements MarkLogicNodeDriver {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicNodeSshDriver.class);

    public final static AtomicInteger counter = new AtomicInteger(2);
    private final int nodeId;

    public MarkLogicNodeSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
        this.nodeId = counter.getAndIncrement();
    }

    public String getDownloadFilename() {
        // TODO To support other platforms, need to customize this based on OS
        return "MarkLogic-" + getVersion() + ".x86_64.rpm";
    }

    public int getNodeId() {
        return nodeId;
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


        File uploadDirectory = getUploadDirectory();
        String targetDirectory = "./";
        uploadFiles(uploadDirectory, targetDirectory);

        LOG.info("Finished upload to {}", getHostname());
    }

    private void uploadFiles(File dir, String targetDirectory) {
        getLocation().exec(Arrays.asList("mkdir -p " + targetDirectory), MutableMap.of());

        for (File file : dir.listFiles()) {
            final String targetLocation = targetDirectory + "/" + file.getName();
            if (file.isDirectory()) {
                uploadFiles(file, targetLocation);
            } else if (file.isFile() && !file.getName().equals(".DS_Store")) {
                LOG.info("Copying file: " + targetLocation);
                getLocation().copyTo(file, targetLocation);
            }
        }
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
            LOG.info("Additional host {} waiting for initial host to be up", getHostname());
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
            LOG.info("Finished customize of additional host {}", getHostname());
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

        LOG.info("Successfully launched MarkLogic  host {}", getHostname());
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

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("forest", forest);
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

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("database", name);
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

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("database", database);
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

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("appServer", name, "port", port, "database", database, "groupName", groupName);
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

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("group", name);
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

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("groupName", groupName, "hostName", hostAddress);
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
        LOG.debug("Attach forest {} to database {}",forestName,databaseName);

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("forestName", forestName, "databaseName", databaseName);
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

        LOG.debug("Finished attaching forest {} to database {}",forestName,databaseName);
    }

    @Override
    public Set<String> scanAppServices() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> scanDatabases() {
        File scriptFile = new File(getScriptDirectory(), "scan_databases.txt");
        String script = processTemplate(scriptFile);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        //todo:
        int exitStatus = getMachine().run(MutableMap.of("out", stdout, "err", stderr), script, new HashMap());
        String s = new String(stdout.toByteArray());

        Set<String> databases = new HashSet();
        String[] split = s.split("\n");
        for (int k = 0; k < split.length - 1; k++) {
            final String database = split[k].trim();
            int i = database.indexOf("<");
            databases.add(database.substring(2, i));
        }
        return databases;
    }

    @Override
    public Set<String> scanForests() {
        File scriptFile = new File(getScriptDirectory(), "scan_forests.txt");
        String script = processTemplate(scriptFile);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(script);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        //todo:
        int exitStatus = getMachine().run(MutableMap.of("out", stdout, "err", stderr), script, new HashMap());
        String s = new String(stdout.toByteArray());

        Set<String> forests = new HashSet();
        String[] split = s.split("\n");
        for (int k = 0; k < split.length - 1; k++) {
            forests.add(split[k]);
        }
        return forests;

    }
}
