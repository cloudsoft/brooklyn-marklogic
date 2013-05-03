package io.cloudsoft.marklogic.demo;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.entity.webapp.tomcat.TomcatServer;
import brooklyn.location.Location;
import brooklyn.location.basic.PortRanges;

import java.util.Collection;

public class MarkLogicDemoApp extends AbstractApplication {
    private TomcatServer tomcat;

    //MarkLogicCluster cluster;

    @Override
    public void init() {
        tomcat = addChild(EntitySpecs.spec(TomcatServer.class)
                .configure(TomcatServer.SUGGESTED_VERSION, "7.0.39")
                .configure(TomcatServer.HTTP_PORT, PortRanges.fromInteger(8080))
                .configure(TomcatServer.ROOT_WAR, "classpath:/demo-war-0.1.0-SNAPSHOT.war" ));

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
