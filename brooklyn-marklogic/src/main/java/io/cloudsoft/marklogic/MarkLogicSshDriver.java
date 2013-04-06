package io.cloudsoft.marklogic;

import static brooklyn.util.ssh.CommonCommands.dontRequireTtyForSudo;
import static brooklyn.util.ssh.CommonCommands.ok;
import static brooklyn.util.ssh.CommonCommands.sudo;
import static java.lang.String.format;

import java.util.LinkedList;
import java.util.List;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.location.basic.SshMachineLocation;

public class MarkLogicSshDriver extends AbstractSoftwareProcessSshDriver implements MarkLogicDriver {

	public MarkLogicSshDriver(EntityLocal entity, SshMachineLocation machine) {
		super(entity, machine);
	}

	public String getDownloadFilename() {
    	// TODO To support other platforms, need to customize this based on OS
    	return "MarkLogic-"+getVersion()+".x86_64.rpm";
    }

	@Override
	public void install() {
		// TODO Where do we get join-cluster.xqy etc from?

        DownloadResolver resolver = entity.getApplication().getManagementContext().getEntityDownloadsManager().newDownloader(this);
        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();

        List<String> commands = new LinkedList<String>();
        // TODO Could use this if wasn't password protected:
        //      commands.addAll(CommonCommands.downloadUrlAs(urls, saveAs));
        commands.add(dontRequireTtyForSudo());
        //commands.add(format("curl -o %s -O -XPOST -d'email=aled.sage@gmail.com&pass=djJ17VXDw1dyFbT' -f -L " +
        //        "\"https://developer.marklogic.com/download/binaries/6.0/%s\"",
        //        getDownloadFilename(),
        //        saveAs));
        commands.add(ok(sudo("rpm -e MarkLogic")));
        commands.add(sudo("cp /home/ec2-user/marklogic_install/MarkLogic-7.0-ea1_20130315.x86_64.rpm ."));
        commands.add(sudo("rpm -i MarkLogic-7.0-ea1_20130315.x86_64.rpm"));
        commands.add(sudo("sed -i 's/MARKLOGIC_EC2_HOST=1/MARKLOGIC_EC2_HOST=0/' /etc/sysconfig/MarkLogic"));

        commands.add("pushd .");
        commands.add("cd /home/ec2-user/marklogic_install/");
        commands.add(sudo("cp join-cluster.xqy qa-restart.xqy transfer-cluster-config.xqy /opt/MarkLogic/Admin"));
        commands.add(sudo("mkdir /var/opt/xqy"));
        commands.add(sudo("cp xqy/bookmark.xqy xqy/delete.xqy xqy/search-debug.xqy xqy/search.xqy  xqy/update.xqy xqy/verify.xqy xqy/view.xqy /var/opt/xqy"));
        //todo: create_appserver.xqy is removed from the next list of files to be copied since it doesn't exist.
        commands.add(sudo("cp get_db_id.xqy stats.xqy http-server-status.xqy get-hosts.xqy attach_replica.xqy detach_replica.xqy create_markmail_forests.xqy create_forests.xqy create_forests_with_fastdir.xqy create_s3_forests.xqy create_s3_forests_with_fastdir.xqy create_s3_replica_forests.xqy create_s3_replica_forests_with_fastdir.xqy create_replica_forests.xqy create_replica_forests_with_fastdir.xqy create_markmail_database.xqy attach_markmail_forests.xqy  create_httpserver.xqy create_role.xqy rewrite-hostname.xqy rewrite-assignments.xqy  /opt/MarkLogic/Admin"));
        commands.add(sudo("./reset_ml_on_startup"));
        commands.add("popd");

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

        // TODO Do stuff like:
        //      curl --digest -u admin:hap00p http://$master_instance:8001/rewrite-hostname.xqy?oldhost=$node_ip\&newhost=$instance_name
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
        log.info("connect url: "+entity.getAttribute(MarkLogicNode.URL));
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
