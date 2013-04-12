package io.cloudsoft.marklogic;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.Location;
import brooklyn.util.CommandLineUtil;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

/**
 * App to create a MarkLogic cluster (in a single availability zone).
 * <p/>
 * This can be launched by either:
 * <ul>
 * <li>Running the main method
 * <li>Running {@code export BROOKLYN_CLASSPATH=$(pwd)/target/classes; brooklyn launch --app io.cloudsoft.marklogic.MarkLogicApp}
 * </ul>
 */
public class MarkLogicApp extends AbstractApplication {

    //@CatalogConfig(label="Marklogic Licensee", priority=1)
   // public static final ConfigKey<String> DB_SETUP_SQL_URL = new BasicConfigKey<String>(String.class,
   //         "app.db_sql", "The MarkLogic Licensee","");

   // @CatalogConfig(label="Marklogic LicenseKey", priority=1)
   // public static final ConfigKey<String> DB_SETUP_SQL_URL1 = new BasicConfigKey<String>(String.class,
   //         "app.db_sql", "The MarkLogic License-key","");


    MarkLogicCluster cluster;

    @Override
    public void init() {
        cluster = addChild(EntitySpecs.spec(MarkLogicCluster.class).configure(MarkLogicCluster.INITIAL_SIZE, 6));
    }

   @Override
   public void postStart(Collection<? extends Location> locations) {
      super.postStart(locations);
      LOG.info("MarkLogic server is available at 'http://" +
              cluster.getAttribute(MarkLogicCluster.MASTER_NODE).getAttribute(Attributes.HOSTNAME) + ":8000'");
      LOG.info("MarkLogic Cluster summary is available at 'http://" +
              cluster.getAttribute(MarkLogicCluster.MASTER_NODE).getAttribute(Attributes.HOSTNAME) +
              ":8001'");
      LOG.info("MarkLogic Monitoring Dashboard is available at 'http://" +
              cluster.getAttribute(MarkLogicCluster.MASTER_NODE).getAttribute(Attributes.HOSTNAME) +
              ":8002/dashboard'");
   }

   /**
     * Launches the application, along with the brooklyn web-console.
     */
    public static void main(String[] argv) throws Exception {
        List<String> args = Lists.newArrayList(argv);
        String port =  CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "localhost");

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpecs.appSpec(MarkLogicApp.class).displayName("Brooklyn MarkLogic Application"))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }
}
