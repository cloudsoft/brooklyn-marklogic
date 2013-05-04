package io.cloudsoft.marklogic.demo;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.entity.webapp.JavaWebAppService;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.entity.webapp.jboss.JBoss7Server;
import brooklyn.location.Location;
import brooklyn.util.MutableMap;
import io.cloudsoft.marklogic.MarkLogicCluster;
import io.cloudsoft.marklogic.MarkLogicNode;

import java.util.Collection;

public class MarkLogicDemoApp extends AbstractApplication {
    private DynamicCluster jbossCluster;
    private NginxController nginx;
    private MarkLogicCluster markLogicCluster;

    @Override
    public void init() {
        String initialClusterSizeValue = getManagementContext().getConfig().getFirst("brooklyn.marklogic.initial-cluster-size");
        int initialClusterSize = 2;
        if (initialClusterSizeValue != null && !initialClusterSizeValue.isEmpty()) {
            initialClusterSize = Integer.parseInt(initialClusterSizeValue);
        }

        markLogicCluster = addChild(EntitySpecs.spec(MarkLogicCluster.class).configure(MarkLogicCluster.INITIAL_SIZE, initialClusterSize));
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        super.postStart(locations);

        String appServiceName = "ExampleService";
        String appServicePort = "8011";
        String databaseName = "DemoDatabase";

        MarkLogicNode masterNode = markLogicCluster.getAttribute(MarkLogicCluster.MASTER_NODE);

        String masterHost = masterNode.getMasterAddress();
        masterNode.createDatabase(databaseName);
        masterNode.createAppServer(appServiceName, databaseName, appServicePort);

        MutableMap<String, String> webAppProperties = MutableMap.of(
                "marklogic.host", masterHost,
                "marklogic.port", appServicePort,
                "marklogic.user", masterNode.getConfig(MarkLogicNode.USER),
                "marklogic.password", masterNode.getConfig(MarkLogicNode.PASSWORD));

        jbossCluster = addChild(EntitySpecs.spec(DynamicCluster.class)
                .configure(DynamicCluster.MEMBER_SPEC, EntitySpecs.spec(JBoss7Server.class))
                .configure("initialSize", 2)
                .configure("httpPort", 8080)
                .configure(JavaWebAppService.JAVA_SYSPROPS, webAppProperties)
                .configure(JavaWebAppService.ROOT_WAR, "classpath:/demo-war-0.1.0-SNAPSHOT.war"));

        nginx = addChild(EntitySpecs.spec(NginxController.class)
                .configure("cluster", jbossCluster)
                .configure("domain", "localhost")
                .configure("port", 7000)
                .configure("portNumberSensor", WebAppService.HTTP_PORT));
    }
}
