package io.cloudsoft.marklogic.pumper;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.ResourceUtils;
import brooklyn.util.ssh.BashCommands;

public class ContentPumpSshDriver extends AbstractSoftwareProcessSshDriver implements ContentPumpDriver {

    private static final Logger LOG = LoggerFactory.getLogger(ContentPumpSshDriver.class);
    private static final String MLCP_URL = "http://developer.marklogic.com/download/binaries/mlcp/marklogic-contentpump-1.0.3-bin.zip";

    public ContentPumpSshDriver(ContentPumperImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public void pumpTo(String host, int port, String username, String password) {
        LOG.debug("Pumping data from {} to {}:{}", new Object[]{getLocation(), host, port});
        String mlcpSh = Joiner.on(' ').join(
                getInstallDir()+"/mlcp/marklogic-contentpump-1.0.3/bin/mlcp.sh import",
                "-host", host,
                "-port", port,
                "-username", username,
                "-password", password,
                "-input_file_path", getRunDir(),
                "-input_compressed true",
                "-mode local");

        int result = newScript(LAUNCHING)
                .body.append("export JVM_OPTS=\"-Xmx2048m -Xms512m\"", mlcpSh)
                .execute();
        LOG.debug("Finished pumping data from {} to {}:{}. Result code: {}", new Object[]{getLocation(), host, port, result});
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public void install() {
        LOG.info("Installing {}: Downloading pubchem XML and MLCP", getEntity());
        List<String> commands = Lists.newArrayList();
        getDataUrls();
        commands.add(BashCommands.INSTALL_CURL);
        commands.add(BashCommands.INSTALL_ZIP);
        commands.add(BashCommands.INSTALL_UNZIP);
        commands.add(BashCommands.installJava6OrFail());
        commands.add(String.format("curl -L --retry 5 --continue-at - -o %s %s", "mlcp.zip", MLCP_URL));
        for (String target : getDataUrls()) {
            commands.add(String.format("curl -L --retry 5 --continue-at - -O %s", target));
        }
        commands.add("echo 'Waiting for all downloads to complete'");
        commands.add("unzip -n -d mlcp mlcp.zip");

        newScript(INSTALLING)
                .failOnNonZeroResultCode()
                .body.append(commands)
                .execute();

        InputStream splitter = ResourceUtils.create(this)
                .getResourceFromUrl("classpath://io/cloudsoft/marklogic/pumper/splitter.awk");
        getLocation().copyTo(splitter, getInstallDir()+"/splitter.awk");
    }

    @Override
    public void customize() {
        LOG.info("Customising {}: Formatting pubchem XML", getEntity());
        List<String> commands = Lists.newArrayList();
        commands.add("chmod +x " + getInstallDir() + "/splitter.awk");
        commands.add(String.format("for zip in `ls %s/*.xml.gz`; do " +
                "gzip --decompress --stdout $zip | %s/splitter.awk -v filename=`basename $zip`; " +
                "done", getInstallDir(), getInstallDir()));
        newScript(CUSTOMIZING)
                .failOnNonZeroResultCode()
                .body.append(commands)
                .execute();
    }

    @Override
    public void launch() {
        LOG.info("Launched {}", getEntity());
    }

    @Override
    public void stop() {
        LOG.info("Stopped {}", getEntity());
    }

    private Collection<String> getDataUrls() {
        return entity.getConfig(ContentPumper.DATA_SUPPLIER).get();
    }
}