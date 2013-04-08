package io.cloudsoft.marklogic;

import brooklyn.catalog.CatalogConfig;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.event.basic.BasicConfigKey;
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
        cluster = addChild(EntitySpecs.spec(MarkLogicCluster.class).configure(MarkLogicCluster.INITIAL_SIZE, 3));
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
