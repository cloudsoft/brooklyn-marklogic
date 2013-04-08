package io.cloudsoft.marklogic;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.location.basic.SshMachineLocation;

import java.util.LinkedList;
import java.util.List;

import static brooklyn.util.ssh.CommonCommands.dontRequireTtyForSudo;
import static brooklyn.util.ssh.CommonCommands.sudo;

public class MarkLogicSshDriver extends AbstractSoftwareProcessSshDriver implements MarkLogicDriver {

    public MarkLogicSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    public String getDownloadFilename() {
        // TODO To support other platforms, need to customize this based on OS
        return "MarkLogic-" + getVersion() + ".x86_64.rpm";
    }

    public int getFcount() {
        return entity.getConfig(MarkLogicNode.FCOUNT);
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

    @Override
    public void install() {
        boolean master = isMaster();
        if (!master) {
            log.info("Slave " + getHostname() + " waiting for master to be up");
            //a very nasty hack to wait on the service up from the
            entity.getConfig(MarkLogicNode.IS_BACKUP_EBS);
            log.info("Starting installation of slave " + getHostname());
        } else {
            log.info("Starting installation of master " + getHostname());
        }


        String f = master ? "/install_master.txt" : "/install_slave.txt";
        String installScript = processTemplate(MarkLogicSshDriver.class.getResource(f).toString());
        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(installScript);
        newScript(INSTALLING)
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

        if (!master) {
            log.info("Finished installation of slave " + getHostname());
        } else {
            log.info("Finished installation of master " + getHostname());
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

        //// TODO Where does clusterJoin.py etc come from?
        //if (entity.getConfig(MarkLogicNode.IS_MASTER)) {
        //    // TODO
        //} else {
        //    //String masterInstance = entity.getConfig(MarkLogicNode.MASTER_ADDRESS);
        //    //commands.add(sudo(format("python clusterJoin.py -n hosts.txt -u ec2-user -l license.txt -c %s > init_ml", masterInstance)));
        //    // TODO More stuff like this
        //}

        newScript(LAUNCHING)
                .failOnNonZeroResultCode()
                .body.append(commands)
                .execute();

        if (isMaster()) {
            log.info("Successfully launched master " + getHostname());
        } else {
            log.info("Successfully launched slave " + getHostname());
        }
    }

    @Override
    public void postLaunch() {
        entity.setAttribute(MarkLogicNode.URL, String.format("http://%s:%s", getHostname(), 8001));
        //todo: remove me
        log.info("---------------------------------------------------------");
        log.info("connect url: " + entity.getAttribute(MarkLogicNode.URL));
        log.info("---------------------------------------------------------");
    }

    public boolean isRunning() {
        // TODO Aled made this up: is this right?
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
}
