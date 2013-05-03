package io.cloudsoft.marklogic.demo;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.entity.webapp.JavaWebAppService;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.entity.webapp.jboss.JBoss7Server;
import brooklyn.location.Location;

import java.util.Collection;

public class MarkLogicDemoApp extends AbstractApplication {
    private DynamicCluster cluster;
    private NginxController nginx;

    //MarkLogicCluster cluster;

    @Override
    public void init() {
        cluster = addChild(EntitySpecs.spec(DynamicCluster.class)
                .configure(DynamicCluster.MEMBER_SPEC, EntitySpecs.spec(JBoss7Server.class))
                .configure("initialSize", 2)
                .configure("httpPort", 8080)
                .configure(JavaWebAppService.ROOT_WAR, "classpath:/demo-war-0.1.0-SNAPSHOT.war"));

        nginx = addChild(EntitySpecs.spec(NginxController.class)
                .configure("cluster", cluster)
                .configure("domain", "localhost")
                .configure("port", 8000)
                .configure("portNumberSensor", WebAppService.HTTP_PORT));


        //String initialClusterSizeValue = getManagementContext().getConfig().getFirst("brooklyn.marklogic.initial-cluster-size");
        //int initialClusterSize = 2;
        //if (initialClusterSizeValue != null && !initialClusterSizeValue.isEmpty()) {
        //    initialClusterSize = Integer.parseInt(initialClusterSizeValue);
        //}
        //
        //cluster = addChild(EntitySpecs.spec(MarkLogicCluster.class).configure(MarkLogicCluster.INITIAL_SIZE, initialClusterSize));
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        //super.postStart(locations);
        //LOG.info("MarkLogic server is available at 'http://" +
        //        cluster.getAttribute(MarkLogicCluster.MASTER_NODE).getAttribute(Attributes.HOSTNAME) + ":8000'");
        //LOG.info("MarkLogic Cluster summary is available at 'http://" +
        //        cluster.getAttribute(MarkLogicCluster.MASTER_NODE).getAttribute(Attributes.HOSTNAME) +
        //        ":8001'");
        //LOG.info("MarkLogic Monitoring Dashboard is available at 'http://" +
        //       cluster.getAttribute(MarkLogicCluster.MASTER_NODE).getAttribute(Attributes.HOSTNAME) +
        //        ":8002/dashboard'");
    }
}
