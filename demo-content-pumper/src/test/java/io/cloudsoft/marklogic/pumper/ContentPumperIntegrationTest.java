package io.cloudsoft.marklogic.pumper;

import static org.testng.Assert.assertFalse;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.location.LocationSpec;
import brooklyn.location.basic.LocalhostMachineProvisioningLocation;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.EntityTestUtils;
import brooklyn.test.entity.TestApplication;

public class ContentPumperIntegrationTest {

    public ManagementContext ctx;
    private TestApplication app;
    private LocalhostMachineProvisioningLocation localhostProvisioningLocation;

    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        ctx = new LocalManagementContext();
    	localhostProvisioningLocation = ctx.getLocationManager()
                .createLocation(LocationSpec.create(LocalhostMachineProvisioningLocation.class));
        app = ApplicationBuilder.newManagedApp(TestApplication.class, ctx);
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app.getManagementContext());
    }

    @Test(groups = "Integration")
    public void testDownloadAndFormatPharmaXml() {
        ContentPumper pumper = app.createAndManageChild(EntitySpec.create(ContentPumper.class)
                .configure(ContentPumper.DATA_SUPPLIER, new LiveTestUrlSupplier()));
        app.start(ImmutableList.of(localhostProvisioningLocation));
        EntityTestUtils.assertAttributeEqualsEventually(pumper, Startable.SERVICE_UP, true);
        app.stop();
        assertFalse(pumper.getAttribute(Startable.SERVICE_UP));
    }

}
