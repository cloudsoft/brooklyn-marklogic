package io.cloudsoft.marklogic;

import java.util.List;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.launcher.BrooklynServerDetails;
import brooklyn.location.Location;
import brooklyn.util.CommandLineUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * App to create a MarkLogic cluster (in a single availability zone).
 * 
 * This can be launched by either:
 * <ul>
 *   <li>Running the main method
 *   <li>Running {@code export BROOKLYN_CLASSPATH=$(pwd)/target/classes; brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp}
 * </ul>
 */
public class MarkLogicApp extends AbstractApplication {

	@Override
	public void postConstruct() {
		super.postConstruct();
		
		// TODO Syntax below is improving massively in next 0.5.0-M3 or rc.1 release!
		MarkLogicCluster cluster = (MarkLogicCluster) addChild(getEntityManager().createEntity(BasicEntitySpec.newInstance(MarkLogicCluster.class)
				.configure(MarkLogicCluster.INITIAL_SIZE, 1)));
	}

	/**
	 * Launches the application, along with the brooklyn web-console.
	 * 
	 * TODO Presumably want a CentOS or RHEL AMI
     *   ami-800d86b0, rightscale-us-west-2/RightImage_CentOS_6.3_x64_v5.8.8.8.manifest.xml, 411009282317, Public, available, Cent OS, instance store, paravirtual
     *
     * Include in your brooklyn.properties:
     *   brooklyn.location.named.marklogic-uswest2=jclouds:aws-ec2:us-west-2
     *   brooklyn.location.named.marklogic-uswest2.imageId=us-west-2/ami-800d86b0
     *   brooklyn.location.named.marklogic-uswest2.user=root
	 */
	public static void main(String[] argv) {
		// TODO Syntax below is improving massively in next 0.5.0-M3 or rc.1 release!
		
		List<String> args = Lists.newArrayList(argv);
        String port =  CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "named:marklogic-uswest2");

        BrooklynServerDetails server = BrooklynLauncher.newLauncher()
                .webconsolePort(port)
                .launch();

        Location loc = server.getManagementContext().getLocationRegistry().resolve(location);

        StartableApplication app = ApplicationBuilder.builder(MarkLogicApp.class)
                .displayName("MarkLogic app")
                .manage(server.getManagementContext());

        app.start(ImmutableList.of(loc));

        Entities.dumpInfo(app);
	}
}
