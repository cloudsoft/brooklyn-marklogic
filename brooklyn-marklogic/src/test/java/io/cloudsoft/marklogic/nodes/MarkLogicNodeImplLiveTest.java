package io.cloudsoft.marklogic.nodes;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import brooklyn.config.BrooklynProperties;
import brooklyn.config.BrooklynProperties.Factory;
import brooklyn.entity.BrooklynMgmtContextTestSupport;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.location.Location;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.EntityTestUtils;
import brooklyn.util.collections.MutableMap;
import io.cloudsoft.marklogic.AbstractMarklogicFullClusterLiveTest;

public class MarkLogicNodeImplLiveTest extends BrooklynMgmtContextTestSupport {

    public static final Logger LOG = LoggerFactory.getLogger(AbstractMarklogicFullClusterLiveTest.class);

    public static final String PROVIDER = "aws-ec2";
    public static final String REGION_NAME = "us-east-1";
    public static final String LOCATION_SPEC = PROVIDER + ":" + REGION_NAME;
    public static final String IMAGE_ID = "ami-3275ee5b"; // A general purpose CentOS image
    public static final String MEDIUM_HARDWARE_ID = "m1.medium";

    protected Location jcloudsLocation;

    /** Obtains a VM running in EC2 */
    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        BrooklynProperties brooklynProperties = Factory.newDefault();

        // Don't let any defaults from brooklyn.properties (except credentials) interfere with test
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".image-description-regex");
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".image-name-regex");
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".image-id");
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".inboundPorts");
        brooklynProperties.remove("brooklyn.jclouds." + PROVIDER + ".hardware-id");

        // Also removes scriptHeader (e.g. if doing `. ~/.bashrc` and `. ~/.profile`, then that can cause "stdin: is not a tty")
        brooklynProperties.remove("brooklyn.ssh.config.scriptHeader");

        Map<String, ?> flags = MutableMap.of(
                "tags", ImmutableList.of(getClass().getName()),
                "imageId", REGION_NAME + "/" + IMAGE_ID,
//                "user", "ec2-user",
                "hardwareId", MEDIUM_HARDWARE_ID);
        mgmt = new LocalManagementContext(brooklynProperties);
        jcloudsLocation = mgmt.getLocationRegistry().resolve(LOCATION_SPEC, flags);

        super.setUp();

    }

    @Test(groups = "Live")
    public void testStartMarkLogicImplOnEC2() {
        MarkLogicNode entity = app.createAndManageChild(EntitySpec.create(MarkLogicNode.class)
                .configure(MarkLogicNode.IS_FORESTS_EBS, true)
                .configure(MarkLogicNode.IS_VAR_OPT_EBS, false)
                .configure(MarkLogicNode.IS_BACKUP_EBS, false));

        app.start(ImmutableList.of(jcloudsLocation));
        EntityTestUtils.assertAttributeEqualsEventually(app, Startable.SERVICE_UP, true);
    }



}
