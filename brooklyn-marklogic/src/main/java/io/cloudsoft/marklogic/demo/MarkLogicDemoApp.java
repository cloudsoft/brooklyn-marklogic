package io.cloudsoft.marklogic.demo;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.database.mysql.MySqlNode;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.java.JavaEntityMethods;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.entity.webapp.JavaWebAppService;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.entity.webapp.jboss.JBoss7Server;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.location.Location;
import brooklyn.management.Task;
import brooklyn.util.GroovyJavaMethods;
import brooklyn.util.MutableMap;
import brooklyn.util.task.BasicTask;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import io.cloudsoft.marklogic.AppServer;
import io.cloudsoft.marklogic.MarkLogicCluster;
import io.cloudsoft.marklogic.MarkLogicNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;
import static brooklyn.event.basic.DependentConfiguration.formatString;

public class MarkLogicDemoApp extends AbstractApplication {
    private DynamicCluster jbossCluster;
    private NginxController jbossNginx;
    private MarkLogicCluster markLogicCluster;
    private NginxController marklogicNginx;

    String appServiceName = "DemoService";
    int appServicePort = 8011;
    String databaseName = "DemoDatabase";

    @Override
    public void init() {
        String initialClusterSizeValue = getManagementContext().getConfig().getFirst("brooklyn.marklogic.initial-cluster-size");
        int initialClusterSize = 2;
        if (initialClusterSizeValue != null && !initialClusterSizeValue.isEmpty()) {
            initialClusterSize = Integer.parseInt(initialClusterSizeValue);
        }

        //currently we make a single marklogic cluster where the nodes will contains forests and appservers (rest). This is going
        //to be split up in multiple clusters; one cluster with e-nodes and one cluster with d-nodes.
        markLogicCluster = addChild(EntitySpecs.spec(MarkLogicCluster.class)
                .configure(MarkLogicCluster.INITIAL_SIZE, initialClusterSize)
        );

        AppServer appServer = addChild(EntitySpecs.spec(AppServer.class)
                .configure(AppServer.NAME, appServiceName)
                .configure(AppServer.DATABASE, databaseName)
                .configure(AppServer.PORT, appServicePort)
        );

        //the marklogic cluster will
        marklogicNginx = addChild(EntitySpecs.spec(NginxController.class)
                .configure("cluster", markLogicCluster)
                        //.configure("domain", "localhost")
                .configure("port", appServicePort) //todo: normally 7000
                        //todo: temporary hack to feed the app port to nginx.
                .configure("portNumberSensor", MarkLogicNode.APP_SERVICE_PORT)
        );

        jbossCluster = addChild(EntitySpecs.spec(DynamicCluster.class)
                .configure(DynamicCluster.MEMBER_SPEC, EntitySpecs.spec(JBoss7Server.class))
                .configure("initialSize", 3)
                .configure("httpPort", 8080)
                .configure(JavaEntityMethods.javaSysProp("marklogic.host"), attributeWhenReady(marklogicNginx, NginxController.HOSTNAME))
                .configure(JavaEntityMethods.javaSysProp("marklogic.port"), "" + appServicePort)
                //todo: this should be retrieved from the marklogic node
                .configure(JavaEntityMethods.javaSysProp("marklogic.password"), "hap00p")
                //todo: this should be retrieved from the marklogic node
                .configure(JavaEntityMethods.javaSysProp("marklogic.user"), "admin")
                .configure(JavaWebAppService.ROOT_WAR, "classpath:/demo-war-0.1.0-SNAPSHOT.war"));

        jbossNginx = addChild(EntitySpecs.spec(NginxController.class)
                .configure("cluster", jbossCluster)
                .configure("port", 8080)
                .configure("portNumberSensor", WebAppService.HTTP_PORT));
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        LOG.info("=========================== MarkLogicDemoApp: Starting postStart =========================== ");

        super.postStart(locations);

        MarkLogicNode masterNode = markLogicCluster.getAttribute(MarkLogicCluster.MASTER_NODE);
        String masterHost = masterNode.getMasterAddress();

        LOG.info("MarkLogic Nginx http://" +marklogicNginx.getAttribute(Attributes.HOSTNAME));
        LOG.info("JBoss Nginx  http://" +jbossNginx.getAttribute(Attributes.HOSTNAME));
        int k=1;
        for(Entity entity: jbossCluster.getMembers()){
            LOG.info("   "+k+" JBoss member  http://" +entity.getAttribute(Attributes.HOSTNAME)+":"+entity.getAttribute(JBoss7Server.HTTP_PORT));
            k++;
        }

        LOG.info("MarkLogic master server is available at 'http://" +masterHost + ":8000'");
        LOG.info("MarkLogic Cluster summary is available at 'http://" +masterHost +":8001'");
        k=1;
        for(Entity entity: markLogicCluster.getMembers()){
            LOG.info("   "+k+" MarkLogic node http://" +entity.getAttribute(MarkLogicNode.HOSTNAME)+":8000");
            k++;
        }
        LOG.info("MarkLogic Monitoring Dashboard is available at 'http://" +masterHost +":8002/dashboard'");

        masterNode.createDatabase(databaseName);
        masterNode.createAppServer(appServiceName, databaseName, "" + appServicePort);

        LOG.info("=========================== MarkLogicDemoApp: Finished postStart =========================== ");
    }

}
