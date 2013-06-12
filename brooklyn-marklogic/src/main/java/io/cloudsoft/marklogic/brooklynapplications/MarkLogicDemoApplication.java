package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.proxy.AbstractController;
import brooklyn.entity.proxy.LoadBalancer;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.JavaWebAppService;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.entity.webapp.WebAppServiceConstants;
import brooklyn.entity.webapp.jboss.JBoss7Server;
import brooklyn.location.Location;
import brooklyn.policy.autoscaling.AutoScalerPolicy;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.forests.Forest;
import io.cloudsoft.marklogic.forests.UpdatesAllowed;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

import java.util.Collection;

import static brooklyn.entity.java.JavaEntityMethods.javaSysProp;
import static brooklyn.entity.proxying.EntitySpecs.spec;
import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

public class MarkLogicDemoApplication extends AbstractApplication {

    private String appServiceName = "DemoService";
    private int appServicePort = 8011;
    private String databaseName = "DemoDatabase";
    private String password = "hap00p";
    private String username = "admin";
    private ControlledDynamicWebAppCluster web;
    private MarkLogicCluster markLogicCluster;

    @Override
    public void init() {
        markLogicCluster = addChild(spec(MarkLogicCluster.class)
                .displayName("MarkLogic Cluster")
                .configure(MarkLogicCluster.INITIAL_D_NODES_SIZE, 1)
                .configure(MarkLogicCluster.INITIAL_E_NODES_SIZE, 1)
                .configure(MarkLogicCluster.LOAD_BALANCER_SPEC, spec(NginxController.class)
                        .displayName("LoadBalancer")
                        .configure("port", 80)
                                //todo: temporary hack to feed the app port to nginx.
                        .configure("portNumberSensor", MarkLogicNode.APP_SERVICE_PORT)
                )
        );

        web = addChild(BasicEntitySpec.newInstance(ControlledDynamicWebAppCluster.class)
                .displayName("WebApp cluster")
                .configure("initialSize", 1)
                .configure(ControlledDynamicWebAppCluster.CONTROLLER_SPEC, spec(NginxController.class)
                        .displayName("WebAppCluster Nginx")
                        .configure("port", 8080)
                        .configure("portNumberSensor", WebAppService.HTTP_PORT))
                .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, spec(JBoss7Server.class)
                        .configure("initialSize", 1)
                        .configure("httpPort", 8080)

                        .configure(javaSysProp("marklogic.host"), attributeWhenReady(markLogicCluster.getLoadBalancer(), AbstractController.HOSTNAME))
                        .configure(javaSysProp("marklogic.port"), "" + appServicePort)
                        .configure(javaSysProp("marklogic.password"), password)
                        .configure(javaSysProp("marklogic.user"), username)
                        .configure(JavaWebAppService.ROOT_WAR, "classpath:/demo-war-0.1.0-SNAPSHOT.war")));

        web.getCluster().addPolicy(AutoScalerPolicy.builder()
                .metric(WebAppServiceConstants.REQUESTS_PER_SECOND_LAST)
                .sizeRange(1, 5)
                .metricRange(10, 100)
                .build());
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        LOG.info("=========================== MarkLogicDemoApp: Starting postStart =========================== ");

        super.postStart(locations);

        printInfo();

        Database db = markLogicCluster.getDatabases().createDatabase(databaseName);
        String targetHost = markLogicCluster.getDNodeGroup().getAnyStartedMember().getHostName();
        Forest forest = markLogicCluster.getForests().createForest("demoForest", targetHost, null, null, null, UpdatesAllowed.ALL.toString(), true, false);


        //Forest forest = markLogicCluster.getForests().createForestWithSpec(...);
        //Database db = markLogicCluster.getDatabases().createDatabaseWithForest(databaseName);
        //db.assign(forest);

        //markLogicCluster.getDatabases().createDatabaseWithForest(databaseName);
        //MarkLogicNode node = (MarkLogicNode) markLogicCluster.getDNodeGroup().getMembers().iterator().next();

        //markLogicCluster.getForests().createForestWithSpec("demoForest", node.getHostName(), null, null, null, UpdatesAllowed.ALL.toString(), "true", "false");

        markLogicCluster.getAppservices().createRestAppServer(appServiceName, databaseName, "Default", "" + appServicePort);

        LOG.info("=========================== MarkLogicDemoApp: Finished postStart =========================== ");
    }

    private void printInfo() {
        MarkLogicNode node = markLogicCluster.getENodeGroup().getAnyStartedMember();
        String hostName = node.getHostName();

        LOG.info("MarkLogic Nginx http://" + markLogicCluster.getLoadBalancer().getAttribute(Attributes.HOSTNAME));
        LOG.info("Web Nginx  http://" + web.getController().getAttribute(Attributes.HOSTNAME));
        int k = 1;
        for (Entity entity : web.getCluster().getMembers()) {
            LOG.info("   " + k + " JBoss member  http://" + entity.getAttribute(Attributes.HOSTNAME) + ":" + entity.getAttribute(JBoss7Server.HTTP_PORT));
            k++;
        }

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

}
