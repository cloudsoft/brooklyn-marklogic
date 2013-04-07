package io.cloudsoft.marklogic;

import static brooklyn.util.ssh.CommonCommands.dontRequireTtyForSudo;
import static brooklyn.util.ssh.CommonCommands.sudo;
import static java.lang.String.format;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.location.basic.SshMachineLocation;
import com.google.common.base.Throwables;

public class MarkLogicSshDriver extends AbstractSoftwareProcessSshDriver implements MarkLogicDriver {

	public MarkLogicSshDriver(EntityLocal entity, SshMachineLocation machine) {
		super(entity, machine);
	}

	public String getDownloadFilename() {
    	// TODO To support other platforms, need to customize this based on OS
    	return "MarkLogic-"+getVersion()+".x86_64.rpm";
   }

    public int getFcount(){
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

    @Override
    public void install() {
        log.info("---------------------------------------------------------");
        log.info("connect url: "+ getHostname());
        log.info("---------------------------------------------------------");


        String installFile = MarkLogicSshDriver.class.getResource("/install.txt").getFile();
        String installScript = processTemplate(installFile);
        List<String> commands = new LinkedList<String>();
        commands.add(dontRequireTtyForSudo());
        commands.add(installScript);
        newScript(INSTALLING)
                .failOnNonZeroResultCode()
                .setFlag("allocatePTY", true)
                .body.append(commands)
                .execute();

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


        // TODO Where does clusterJoin.py etc come from?
        if (entity.getConfig(MarkLogicNode.IS_MASTER)) {
        	// TODO
        } else {
        	String masterInstance = entity.getConfig(MarkLogicNode.MASTER_ADDRESS);
        	commands.add(sudo(format("python clusterJoin.py -n hosts.txt -u ec2-user -l license.txt -c %s > init_ml", masterInstance)));
        	// TODO More stuff like this
        }

        newScript(LAUNCHING)
                .failOnNonZeroResultCode()
                .body.append(commands)
                .execute();
	}

    @Override
    public void postLaunch() {
        entity.setAttribute(MarkLogicNode.URL, String.format("http://%s:%s", getHostname(), 8001));
        //todo: remove me
        log.info("---------------------------------------------------------");
        log.info("connect url: "+ entity.getAttribute(MarkLogicNode.URL));
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

   private String createASCIIString(String path, String query) {
      try {
         return "'" + new URI("http", null, getHostname(), 8001, path, query, null).toASCIIString() + "'";
      } catch (URISyntaxException e) {
         throw Throwables.propagate(e);
      }
   }

}
