package io.cloudsoft.marklogic;

import java.util.List;

import brooklyn.entity.basic.*;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.EntitySpecs;
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

    MarkLogicCluster cluster;

    @Override
    public void init() {
        // TODO Syntax below is improving massively in next 0.5.0-M3 or rc.1 release!
        cluster = (MarkLogicCluster) addChild(getEntityManager().createEntity(BasicEntitySpec.newInstance(MarkLogicCluster.class)
                .configure(MarkLogicCluster.INITIAL_SIZE, 2)));


    }

    /**
     * Launches the application, along with the brooklyn web-console.
     */
    public static void main(String[] argv)throws Exception {
        // TODO Syntax below is improving massively in next 0.5.0-M3 or rc.1 release!

        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "named:marklogic-uswest2");

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpecs.appSpec(MarkLogicApp.class))
                .webconsolePort(port)
                .location(location)
                .start();

        StartableApplication app = (StartableApplication) launcher.getApplications().get(0);
        Entities.dumpInfo(app);

        LOG.info("Press return to shut down the cluster");
        System.in.read(); //wait for the user to type a key
        app.stop();


//        BrooklynServerDetails server = BrooklynLauncher.newLauncher()
//                .webconsolePort(port)
//                .launch();
//
//        Location loc = server.getManagementContext().getLocationRegistry().resolve(location);
//
//        //StartableApplication app = new BasicWordpressApp()
//        //        .appDisplayName("Simple wordpress app")
//        //        .manage(server.getManagementContext());
//
//
//        MarkLogicApp app = new MarkLogicApp();
//        app.setDisplayName("MarkLogic app");
//        app.manage()
//                .manage(server.getManagementContext());
//
//        app.start(ImmutableList.of(loc));
//
//        Entities.dumpInfo(app);
	}
}
