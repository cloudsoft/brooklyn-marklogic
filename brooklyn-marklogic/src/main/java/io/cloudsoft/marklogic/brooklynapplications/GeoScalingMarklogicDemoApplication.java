package io.cloudsoft.marklogic.brooklynapplications;

import brooklyn.config.StringConfigMap;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.dns.geoscaling.GeoscalingDnsService;
import brooklyn.entity.group.DynamicFabric;
import brooklyn.entity.proxy.nginx.NginxController;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.EntitySpecs;
import brooklyn.entity.webapp.ControlledDynamicWebAppCluster;
import brooklyn.entity.webapp.JavaWebAppService;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.entity.webapp.jboss.JBoss7Server;
import brooklyn.location.Location;
import brooklyn.location.basic.PortRanges;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static brooklyn.entity.java.JavaEntityMethods.javaSysProp;
import static brooklyn.entity.proxying.EntitySpecs.spec;
import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;
import static com.google.common.base.Preconditions.checkNotNull;

public class GeoScalingMarklogicDemoApplication extends AbstractApplication {
    public static final Logger log = LoggerFactory.getLogger(GeoScalingMarklogicDemoApplication.class);

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

        webGeoDns = addChild(EntitySpecs.spec(GeoscalingDnsService.class)
                .displayName("Web GeoScaling DNS")
                .configure("username", checkNotNull(config.getFirst("brooklyn.geoscaling.username"), "username"))
                .configure("password", checkNotNull(config.getFirst("brooklyn.geoscaling.password"), "password"))
                .configure("primaryDomainName", checkNotNull(config.getFirst("brooklyn.geoscaling.primaryDomain"), "primaryDomain"))
                .configure("smartSubdomainName", "brooklyn"));

        marklogicGeoDns = addChild(EntitySpecs.spec(GeoscalingDnsService.class)
                .displayName("Marklogic GeoScaling DNS")
                .configure("username", checkNotNull(config.getFirst("brooklyn.geoscaling.username"), "username"))
                .configure("password", checkNotNull(config.getFirst("brooklyn.geoscaling.password"), "password"))
                .configure("primaryDomainName", checkNotNull(config.getFirst("brooklyn.geoscaling.primaryDomain"), "primaryDomain"))
                .configure("smartSubdomainName", "brooklyn"));

        markLogicFabric = addChild(EntitySpecs.spec(DynamicFabric.class)
                .displayName("MarkLogic Fabric")
                .configure(NginxController.PROXY_HTTP_PORT, PortRanges.fromInteger(80))
                .configure(DynamicFabric.MEMBER_SPEC, spec(MarkLogicCluster.class)
                        .displayName("MarkLogic Cluster")
                        .configure(MarkLogicCluster.INITIAL_D_NODES_SIZE, 1)
                        .configure(MarkLogicCluster.INITIAL_E_NODES_SIZE, 1)));
        marklogicGeoDns.setTargetEntityProvider(markLogicFabric);

        webFabric = addChild(EntitySpecs.spec(DynamicFabric.class)
                .displayName("Web Fabric")
                .configure(DynamicFabric.MEMBER_SPEC, BasicEntitySpec.newInstance(ControlledDynamicWebAppCluster.class)
                        .displayName("WebApp cluster")
                        .configure("initialSize", 1)
                        .configure(ControlledDynamicWebAppCluster.CONTROLLER_SPEC, spec(NginxController.class)
                                .displayName("WebAppCluster Nginx")
                                .configure("port", 80)
                                .configure("portNumberSensor", WebAppService.HTTP_PORT))
                        .configure(ControlledDynamicWebAppCluster.MEMBER_SPEC, spec(JBoss7Server.class)
                                .configure("initialSize", 1)
                                .configure("httpPort", 8080)
                                .configure(javaSysProp("marklogic.host"), attributeWhenReady(marklogicGeoDns, GeoscalingDnsService.HOSTNAME))
                                .configure(javaSysProp("marklogic.port"), "" + 80)
                                .configure(javaSysProp("marklogic.password"), password)
                                .configure(javaSysProp("marklogic.user"), username)
                                .configure(JavaWebAppService.ROOT_WAR, "classpath:/demo-war-0.1.0-SNAPSHOT.war"))
                ));
        webGeoDns.setTargetEntityProvider(webFabric);
    }

    @Override
    public void postStart(Collection<? extends Location> locations) {
        LOG.info("=========================== GeoScalingMarklogicDemoApplication: Starting postStart =========================== ");

        super.postStart(locations);

        for (Entity member : markLogicFabric.getMembers()) {
            if (member instanceof MarkLogicCluster) {
                MarkLogicCluster markLogicCluster = (MarkLogicCluster) member;
                markLogicCluster.getDatabases().createDatabaseWithForest(databaseName);
                markLogicCluster.getAppservices().createRestAppServer(appServiceName, databaseName, "Default", "" + appServicePort);
            }
        }

        LOG.info("webGeoDns hostname: " + webGeoDns.getHostname());
        LOG.info("marklogicGenoDns hostname: " + marklogicGeoDns.getHostname());

        LOG.info("=========================== GeoScalingMarklogicDemoApplication: Finished postStart =========================== ");
    }


}
