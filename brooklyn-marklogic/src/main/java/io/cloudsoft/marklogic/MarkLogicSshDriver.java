package io.cloudsoft.marklogic;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.MutableMap;
import brooklyn.util.exceptions.Exceptions;
import com.google.common.collect.ImmutableMap;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.cloudsoft.marklogic.forests.Forest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static brooklyn.util.ssh.CommonCommands.dontRequireTtyForSudo;
import static brooklyn.util.ssh.CommonCommands.sudo;

public class MarkLogicSshDriver extends AbstractSoftwareProcessSshDriver implements MarkLogicDriver {

    public final static AtomicInteger counter = new AtomicInteger(2);
    private final int nodeId;

    public MarkLogicSshDriver(EntityLocal entity, SshMachineLocation machine) {
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

    public String getCluster() {
        return entity.getConfig(MarkLogicNode.CLUSTER).replace(" ", "%20");
    }

    public String getMasterAddress() {
        return entity.getConfig(MarkLogicNode.MASTER_ADDRESS);
    }

    public boolean isMaster() {
        return entity.getConfig(MarkLogicNode.IS_MASTER);
    }

    public File getBrooklynMarkLogicHome() {
        String home = System.getenv("BROOKLYN_MARKLOGIC_HOME");
        if (home == null) {
            home = System.getProperty("user.dir");
            log.warn("BROOKLYN_MARKLOGIC_HOME not found in environment, defaulting to [{}]", home);
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
        boolean master = isMaster();
        if (master) {
            log.info("Starting installation of MarkLogic master " + getHostname());
            uploadFiles();
        } else {
            log.info("Slave " + getHostname() + " waiting for master to be up");
            uploadFiles();

            //a very nasty hack to wait on the service up from the

            entity.getConfig(MarkLogicNode.IS_BACKUP_EBS);
            log.info("Starting installation of MarkLogic slave " + getHostname());
        }

        //uninstall marklogic; this functionality is useful when machines are being reused (e.g for testing to speed things up).
        uninstall();

        String installScript = processTemplate(new File(getScriptDirectory(), "install.txt"));
        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(installScript);
        newScript(MutableMap.of("nonStandardLayout","true"),INSTALLING)
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        if (master) {
            log.info("Finished installation of MarkLogic master " + getHostname());
        } else {
            log.info("Finished installation of MarkLogic slave " + getHostname());
        }
    }

    private void uninstall(){
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
        log.info("Starting upload to" + getHostname());


        File uploadDirectory = getUploadDirectory();
        String targetDirectory = "./";
        uploadFiles(uploadDirectory, targetDirectory);

        log.info("Finished upload to " + getHostname());
    }

    private void uploadFiles(File dir, String targetDirectory) {
        getLocation().exec(Arrays.asList("mkdir -p " + targetDirectory), MutableMap.of());

        for (File file : dir.listFiles()) {
            final String targetLocation = targetDirectory + "/" + file.getName();
            if (file.isDirectory()) {
                uploadFiles(file, targetLocation);
            } else if (file.isFile() && !file.getName().equals(".DS_Store")) {
                log.info("Copying file: " + targetLocation);
                getLocation().copyTo(file, targetLocation);
            }
        }
    }

    @Override
    public void customize() {
        // no-op; everything done in install()
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

        if (isMaster()) {
            log.info("Successfully launched MarkLogic master " + getHostname());
        } else {
            log.info("Successfully launched MarkLogic slave " + getHostname());
        }
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
            log.warn("Error creating configuration file for " + entity, e);
            throw Exceptions.propagate(e);
        }
    }

    @Override
    public void createForest(Forest forest) {
        log.info("Starting create forest" + forest.getName());

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("forest", forest);
        File installScriptFile = new File(getScriptDirectory(), "create_forest.txt");
        String installScript = processTemplate(installScriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(installScript);
        newScript("createForest")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        log.info("Finished creating forest" + forest.getName());
  }

    @Override
    public void createDatabase(String name) {
        log.debug("Starting create database " + name);

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("database", name);
        File installScriptFile = new File(getScriptDirectory(), "create_database.txt");
        String installScript = processTemplate(installScriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(installScript);
        newScript("createDatabase")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        log.debug("Finished creating database" + name);
    }

    @Override
    public void createAppServer(String name, String database, String port) {
        log.debug("Starting create appServer " + name);

        Map<String, Object> extraSubstitutions = (Map<String, Object>) (Map) MutableMap.of("appServer", name, "port",port,"database",database);
        File installScriptFile = new File(getScriptDirectory(), "create_appserver.txt");
        String installScript = processTemplate(installScriptFile, extraSubstitutions);

        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(installScript);
        newScript("createRestAppServer")
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        log.debug("Finished creating appServer" + name);
    }
}
