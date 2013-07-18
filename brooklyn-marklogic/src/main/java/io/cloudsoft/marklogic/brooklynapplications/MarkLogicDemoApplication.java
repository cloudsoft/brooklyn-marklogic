package io.cloudsoft.marklogic.brooklynapplications;

import static brooklyn.entity.proxying.EntitySpecs.spec;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forest;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.forests.UpdatesAllowed;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jclouds.googlecomputeengine.GoogleComputeEngineApiMetadata;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.location.Location;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.util.CommandLineUtil;
import brooklyn.util.config.ConfigBag;
import brooklyn.util.text.Identifiers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class MarkLogicDemoApplication extends AbstractApplication {
    private final String user = System.getProperty("user.name");

    private int appServicePort = 8011;
    private String password = "hap00p";
    private String username = "admin";
//    private ControlledDynamicWebAppCluster web;
    private MarkLogicCluster markLogicCluster;

    @Override
    public void start(Collection<? extends Location> locations) {
        for (Location loc : locations) {
            if (isJcloudsLocation(loc, "google-compute-engine")) {
                ConfigBag rawConfig = ((JcloudsLocation)loc).getRawLocalConfigBag();
                for (Map.Entry<Object,Object> entry : GoogleComputeEngineApiMetadata.defaultProperties().entrySet()) {
                    rawConfig.putStringKey((String)entry.getKey(), entry.getValue());
                }
                rawConfig.putStringKey("groupId", "brooklyn-marklogic");
                //rawConfig.putStringKey("locationId", "us-central1-a");
                rawConfig.putStringKey("region", "us-central1-a");
                //rawConfig.putStringKey("endpoint", "https://www.googleapis.com/compute/v1beta15");
            }
        }
        super.start(locations);
    }

    private boolean isJcloudsLocation(Location loc, String provider) {
        return (loc instanceof JcloudsLocation) && ((JcloudsLocation)loc).getProvider().equals(provider);
    }

    @Override
    public void init() {
        markLogicCluster = addChild(spec(MarkLogicCluster.class)
                .displayName("MarkLogic Cluster")
                .configure(MarkLogicCluster.INITIAL_D_NODES_SIZE, 3)
                .configure(MarkLogicCluster.INITIAL_E_NODES_SIZE, 1)
                .configure(MarkLogicNode.VOLUME_SIZE, 100)
                .configure(MarkLogicNode.BACKUP_VOLUME_SIZE, 100)
                .configure(MarkLogicNode.IS_FORESTS_EBS, false)
                .configure(MarkLogicNode.IS_VAR_OPT_EBS, false)
                .configure(MarkLogicNode.IS_BACKUP_EBS, false)
                .configure(MarkLogicNode.IS_REPLICA_EBS, false)
                .configure(MarkLogicNode.IS_FASTDIR_EBS, false)
                .configure(MarkLogicCluster.LOAD_BALANCER_SPEC, spec(NginxController.class)
                        .displayName("LoadBalancer")
                        .configure("port", 80)
                                //todo: temporary hack to feed the app port to nginx.
                        .configure("portNumberSensor", MarkLogicNode.APP_SERVICE_PORT)
                        
                        // FIXME hack to open cdh+nginx ports (because on GCE shared by network for all nodes)
                        .configure(NginxController.PROVISIONING_PROPERTIES, ImmutableMap.<String,Object>of("inboundPorts", ImmutableList.of(8000, 8001, 8002, 8011, 80, 443)))
                )
        );

//        web = addChild(BasicEntitySpec.newInstance(ControlledDynamicWebAppCluster.class)
//                .displayName("WebApp cluster")
//                .configure("initialSize", 1)
//                .configure(ControlledDynamicWebAppCluster.CONTROLLER_SPEC, spec(NginxController.class)
//                        .displayName("WebAppCluster Nginx")
//                        .configure("port", 8080)
//                        .configure("portNumberSensor", WebAppService.HTTP_PORT))
//                .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, spec(JBoss7Server.class)
//                        .configure("initialSize", 1)
//                        .configure("httpPort", 8080)
//
//                        .configure(javaSysProp("marklogic.host"), attributeWhenReady(markLogicCluster.getLoadBalancer(), AbstractController.HOSTNAME))
//                        .configure(javaSysProp("marklogic.port"), "" + appServicePort)
//                        .configure(javaSysProp("marklogic.password"), password)
//                        .configure(javaSysProp("marklogic.user"), username)
//                        .configure(JavaWebAppService.ROOT_WAR, "classpath:/demo-war-0.1.0-SNAPSHOT.war")));
//
//        web.getCluster().addPolicy(AutoScalerPolicy.builder()
//                .metric(WebAppServiceConstants.REQUESTS_PER_SECOND_LAST)
//                .sizeRange(1, 5)
//                .metricRange(10, 100)
//                .build());
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        LOG.info("=========================== MarkLogicDemoApp: Starting postStart =========================== ");

        super.postStart(locations);

        printInfo();

        MarkLogicGroup dgroup = markLogicCluster.getDNodeGroup();
        Databases databases = markLogicCluster.getDatabases();
        MarkLogicNode node1 = dgroup.getAnyUpMember();
        MarkLogicNode node2 = dgroup.getAnyOtherUpMember(node1.getHostName());
        Forests forests = markLogicCluster.getForests();

        Database database = databases.createDatabaseWithSpec(spec(Database.class)
                .configure(Database.NAME, "database-" + user)
                .configure(Database.JOURNALING, "strict")
        );

        createReplicatedForrest(databases, node1, node2, forests, database, "forest1");
        createReplicatedForrest(databases, node2, node1, forests, database, "forest2");

        String appServiceName = "DemoService";
        markLogicCluster.getAppservices().createRestAppServer(appServiceName, database.getName(), "Default",  appServicePort);

        LOG.info("=========================== MarkLogicDemoApp: Finished postStart =========================== ");
    }

    private void createReplicatedForrest(Databases databases, MarkLogicNode node1, MarkLogicNode node2, Forests forests, Database database, String forestBaseName) {
        String primaryForestId = Identifiers.makeRandomId(8);
        Forest primaryForest = forests.createForestWithSpec(spec(Forest.class)
                .configure(Forest.HOST, node1.getHostName())
                .configure(Forest.NAME, forestBaseName + "-primary")
                .configure(Forest.DATA_DIR, "/var/opt/mldata/" + primaryForestId)
                .configure(Forest.LARGE_DATA_DIR, "/var/opt/mldata/" + primaryForestId)
                .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                .configure(Forest.REBALANCER_ENABLED, true)
                .configure(Forest.FAILOVER_ENABLED, true)
        );

        String replicaForestId = Identifiers.makeRandomId(8);
        Forest replicaForest = forests.createForestWithSpec(spec(Forest.class)
                .configure(Forest.HOST, node2.getHostName())
                .configure(Forest.NAME, forestBaseName + "-replica")
                .configure(Forest.DATA_DIR, "/var/opt/mldata/" + replicaForestId)
                .configure(Forest.LARGE_DATA_DIR, "/var/opt/mldata/" + replicaForestId)
                .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                .configure(Forest.REBALANCER_ENABLED, true)
                .configure(Forest.FAILOVER_ENABLED, true));

        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("open");

        forests.attachReplicaForest(primaryForest.getName(), replicaForest.getName());
        databases.attachForestToDatabase(primaryForest.getName(), database.getName());

        primaryForest.awaitStatus("open");
        replicaForest.awaitStatus("sync replicating");
    }

    private void printInfo() {
        MarkLogicNode node = markLogicCluster.getENodeGroup().getAnyUpMember();
        String hostName = node.getHostName();

        LOG.info("MarkLogic Nginx http://" + markLogicCluster.getLoadBalancer().getAttribute(Attributes.HOSTNAME));
//        LOG.info("Web Nginx  http://" + web.getController().getAttribute(Attributes.HOSTNAME));
        int k = 1;
//        for (Entity entity : web.getCluster().getMembers()) {
//            LOG.info("   " + k + " JBoss member  http://" + entity.getAttribute(Attributes.HOSTNAME) + ":" + entity.getAttribute(JBoss7Server.HTTP_PORT));
//            k++;
//        }

        LOG.info("MarkLogic Cluster is available at 'http://" + hostName + ":8000'");
        LOG.info("MarkLogic Cluster summary is available at 'http://" + hostName + ":8001'");
        LOG.info("E-Nodes");
        k = 1;
        for (Entity entity : markLogicCluster.getENodeGroup().getMembers()) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8000");
            k++;
        }

        LOG.info("D-Nodes");
        k = 1;
        for (Entity entity : markLogicCluster.getDNodeGroup().getMembers()) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8000");
            k++;
        }
        LOG.info("MarkLogic Monitoring Dashboard is available at 'http://" + hostName + ":8002/dashboard'");
    }

    public static void main(String[] argv) {
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "localhost");

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(
                        EntitySpecs.appSpec(MarkLogicDemoApplication.class)
                        .displayName("MarkLogic demo"))
                .webconsolePort(port)
                .location(location)
                .start();
    }
}
