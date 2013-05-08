package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.JavaWebAppService;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.entity.webapp.jboss.JBoss7Server;
import brooklyn.location.Location;
import io.cloudsoft.marklogic.appservers.AppServices;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import io.cloudsoft.marklogic.nodes.NodeType;

import java.util.Collection;

import static brooklyn.entity.java.JavaEntityMethods.javaSysProp;
import static brooklyn.entity.proxying.EntitySpecs.spec;
import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

public class MarkLogicDemoApplication extends AbstractApplication {
    private MarkLogicGroup eNodeGroup;
    private NginxController marklogicNginx;
    private MarkLogicGroup dNodeGroup;

    private String appServiceName = "DemoService";
    private int appServicePort = 8011;
    private String databaseName = "DemoDatabase";
    final String password = "hap00p";
    final String username = "admin";
    private Databases databases;
    private AppServices appservices;
    private ControlledDynamicWebAppCluster web;

    @Override
    public void init() {
        //todo: we need to split up in d-node group size and e-node group size.

        String initialClusterSizeValue = getManagementContext().getConfig().getFirst("brooklyn.marklogicCluster.initial-cluster-size");
        int initialClusterSize = 2;
        if (initialClusterSizeValue != null && !initialClusterSizeValue.isEmpty()) {
            initialClusterSize = Integer.parseInt(initialClusterSizeValue);
        }

        eNodeGroup = addChild(spec(MarkLogicGroup.class)
                .configure(MarkLogicGroup.INITIAL_SIZE, 1)
                .configure(MarkLogicGroup.NODE_TYPE, NodeType.E_NODE)
                .configure(MarkLogicGroup.GROUP_NAME, "ENodes")
        );

        dNodeGroup = addChild(spec(MarkLogicGroup.class)
                .configure(MarkLogicGroup.INITIAL_SIZE, 1)
                .configure(MarkLogicGroup.PRIMARY_STARTUP_GROUP, eNodeGroup)
                .configure(MarkLogicGroup.NODE_TYPE, NodeType.D_NODE)
                .configure(MarkLogicGroup.GROUP_NAME, "DNodes")
        );

        databases = addChild(spec(Databases.class)
                .configure(Databases.GROUP, eNodeGroup)
        );

        appservices = addChild(spec(AppServices.class)
                .configure(AppServices.CLUSTER, eNodeGroup)
        );

        marklogicNginx = addChild(spec(NginxController.class)
                .configure("cluster", eNodeGroup)
                .configure("port", appServicePort)
                        //todo: temporary hack to feed the app port to nginx.
                .configure("portNumberSensor", MarkLogicNode.APP_SERVICE_PORT)
        );

        web = addChild(BasicEntitySpec.newInstance(ControlledDynamicWebAppCluster.class)
                .displayName("WebApp cluster")
                .configure("initialSize", 1)
                .configure(ControlledDynamicWebAppCluster.CONTROLLER_SPEC, spec(NginxController.class)
                        .configure("port", 8080)
                        .configure("portNumberSensor", WebAppService.HTTP_PORT))
                .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, spec(JBoss7Server.class)
                        .configure("initialSize", 1)
                        .configure("httpPort", 8080)
                        .configure(javaSysProp("marklogicCluster.host"), attributeWhenReady(marklogicNginx, NginxController.HOSTNAME))
                        .configure(javaSysProp("marklogicCluster.port"), "" + appServicePort)
                        .configure(javaSysProp("marklogicCluster.password"), password)
                        .configure(javaSysProp("marklogicCluster.user"), username)
                        .configure(JavaWebAppService.ROOT_WAR, "classpath:/demo-war-0.1.0-SNAPSHOT.war")));
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        LOG.info("=========================== MarkLogicDemoApp: Starting postStart =========================== ");

        super.postStart(locations);

        MarkLogicNode masterNode = eNodeGroup.getAttribute(MarkLogicGroup.MASTER_NODE);
        String masterHost = masterNode.getAttribute(Attributes.HOSTNAME);

        LOG.info("MarkLogic Nginx http://" + marklogicNginx.getAttribute(Attributes.HOSTNAME));
        LOG.info("Web Nginx  http://" + web.getController().getAttribute(Attributes.HOSTNAME));
        int k = 1;
        for (Entity entity : web.getCluster().getMembers()) {
            LOG.info("   " + k + " JBoss member  http://" + entity.getAttribute(Attributes.HOSTNAME) + ":" + entity.getAttribute(JBoss7Server.HTTP_PORT));
            k++;
        }

        LOG.info("MarkLogic master server is available at 'http://" + masterHost + ":8000'");
        LOG.info("MarkLogic Cluster summary is available at 'http://" + masterHost + ":8001'");
        LOG.info("E-Nodes");
        k = 1;
        for (Entity entity : eNodeGroup.getMembers()) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8000");
            k++;
        }


        LOG.info("D-Nodes");
        k = 1;
        for (Entity entity : dNodeGroup.getMembers()) {
            LOG.info("   " + k + " MarkLogic node http://" + entity.getAttribute(MarkLogicNode.HOSTNAME) + ":8000");
            k++;
        }
        LOG.info("MarkLogic Monitoring Dashboard is available at 'http://" + masterHost + ":8002/dashboard'");

        databases.createDatabase(databaseName);
        appservices.createRestAppServer(appServiceName, databaseName, "Default", "" + appServicePort);


        LOG.info("=========================== MarkLogicDemoApp: Finished postStart =========================== ");
    }

}
