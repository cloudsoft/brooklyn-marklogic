package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.config.StringConfigMap;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.dns.geoscaling.GeoscalingDnsService;
import brooklyn.entity.group.DynamicFabric;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.JavaWebAppService;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.entity.webapp.jboss.JBoss7Server;
import brooklyn.location.Location;
import brooklyn.util.text.Identifiers;
import io.cloudsoft.marklogic.appservers.AppServerKind;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.databases.Databases;
import io.cloudsoft.marklogic.forests.Forest;
import io.cloudsoft.marklogic.forests.Forests;
import io.cloudsoft.marklogic.forests.UpdatesAllowed;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static brooklyn.entity.java.JavaEntityMethods.javaSysProp;
import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;
import static com.google.common.base.Preconditions.checkNotNull;

public class GeoScalingMarkLogicDemoApplication extends AbstractApplication {

    private static final Logger LOG = LoggerFactory.getLogger(GeoScalingMarkLogicDemoApplication.class);
    private final String user = System.getProperty("user.name");

    private String appServiceName = "DemoService";
    private int appServicePort = 8011;
    private String databaseName = "DemoDatabase";
    private String password = "hap00p";
    private String username = "admin";
    private DynamicFabric markLogicFabric;
    private GeoscalingDnsService marklogicGeoDns;
    private DynamicFabric webFabric;
    private GeoscalingDnsService webGeoDns;

    @Override
    public void init() {
        StringConfigMap config = getManagementContext().getConfig();

        webGeoDns = getEntityManager().createEntity(EntitySpec.create(GeoscalingDnsService.class)
                .displayName("Web GeoScaling DNS")
                .configure("username", checkNotNull(config.getFirst("brooklyn.geoscaling.username"), "username"))
                .configure("password", checkNotNull(config.getFirst("brooklyn.geoscaling.password"), "password"))
                .configure("primaryDomainName", checkNotNull(config.getFirst("brooklyn.geoscaling.primaryDomain"), "primaryDomain"))
                .configure("smartSubdomainName", "brooklyn"));

        marklogicGeoDns = getEntityManager().createEntity(EntitySpec.create(GeoscalingDnsService.class)
                .displayName("MarkLogic GeoScaling DNS")
                .configure("username", checkNotNull(config.getFirst("brooklyn.geoscaling.username"), "username"))
                .configure("password", checkNotNull(config.getFirst("brooklyn.geoscaling.password"), "password"))
                .configure("primaryDomainName", checkNotNull(config.getFirst("brooklyn.geoscaling.primaryDomain"), "primaryDomain"))
                .configure("smartSubdomainName", "brooklyn"));

        webFabric = addChild(EntitySpec.create(DynamicFabric.class)
                .displayName("Web Fabric")
                .configure(DynamicFabric.MEMBER_SPEC, EntitySpec.create(ControlledDynamicWebAppCluster.class)
                        .displayName("WebApp cluster")
                        .configure("initialSize", 1)
                        .configure(ControlledDynamicWebAppCluster.CONTROLLER_SPEC, EntitySpec.create(NginxController.class)
                                .displayName("WebAppCluster Nginx")
                                .configure("port", 80)
                                .configure("portNumberSensor", WebAppService.HTTP_PORT))
                        .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, EntitySpec.create(JBoss7Server.class)
                                .configure("initialSize", 1)
                                .configure("httpPort", 8080)
                                .configure(javaSysProp("marklogic.host"), attributeWhenReady(marklogicGeoDns, GeoscalingDnsService.HOSTNAME))
                                .configure(javaSysProp("marklogic.port"), "" + 80)
                                .configure(javaSysProp("marklogic.password"), password)
                                .configure(javaSysProp("marklogic.user"), username)
                                .configure(JavaWebAppService.ROOT_WAR, "classpath:/demo-war-0.1.0-SNAPSHOT.war"))
                ));

        markLogicFabric = addChild(EntitySpec.create(DynamicFabric.class)
                .displayName("MarkLogic Fabric")
                .configure(MarkLogicCluster.LOAD_BALANCER_SPEC, EntitySpec.create(NginxController.class)
                        .displayName("LoadBalancer")
                        .configure("port", 80)
                                //todo: temporary hack to feed the app port to nginx.
                        .configure("portNumberSensor", MarkLogicNode.APP_SERVICE_PORT)
                )
                .configure(DynamicFabric.MEMBER_SPEC, EntitySpec.create(MarkLogicCluster.class)
                        .displayName("MarkLogic Cluster")
                        .configure(MarkLogicCluster.INITIAL_D_NODES_SIZE, 1)
                        .configure(MarkLogicCluster.INITIAL_E_NODES_SIZE, 1)));

        addChild(marklogicGeoDns);
        marklogicGeoDns.setTargetEntityProvider(markLogicFabric);

        addChild(webGeoDns);
        webGeoDns.setTargetEntityProvider(webFabric);
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        LOG.info("=========================== GeoScalingMarkLogicDemoApplication: Starting postStart =========================== ");

        super.postStart(locations);

        for (Entity member : markLogicFabric.getMembers()) {
            if (member instanceof MarkLogicCluster) {
                MarkLogicCluster markLogicCluster = (MarkLogicCluster) member;

                final Databases databases = markLogicCluster.getDatabases();

                Database database = databases.createDatabaseWithSpec(EntitySpec.create(Database.class)
                        .configure(Database.NAME, databaseName)
                        .configure(Database.JOURNALING, "strict")
                );

                Forests forests  = markLogicCluster.getForests();
                String forestId = Identifiers.makeRandomId(8);

                Forest forest = forests.createForestWithSpec(EntitySpec.create(Forest.class)
                        .configure(Forest.HOST, markLogicCluster.getDNodeGroup().getAnyUpMember().getHostName())
                        .configure(Forest.NAME, forestId)
                        .configure(Forest.DATA_DIR, "/var/opt/mldata/" + forestId)
                        .configure(Forest.LARGE_DATA_DIR, "/var/opt/mldata/" + forestId)
                        .configure(Forest.UPDATES_ALLOWED, UpdatesAllowed.ALL)
                        .configure(Forest.REBALANCER_ENABLED, true)
                        .configure(Forest.FAILOVER_ENABLED, true)
                );

                databases.attachForestToDatabase(forest, database);
                markLogicCluster.getAppServices().createAppServer(
                        AppServerKind.HTTP, appServiceName, database, "Default", appServicePort);
            }
        }

        LOG.info("webGeoDns hostname: " + webGeoDns.getHostname());
        LOG.info("marklogicGenoDns hostname: " + marklogicGeoDns.getHostname());

        LOG.info("=========================== GeoScalingMarkLogicDemoApplication: Finished postStart =========================== ");
    }


}
