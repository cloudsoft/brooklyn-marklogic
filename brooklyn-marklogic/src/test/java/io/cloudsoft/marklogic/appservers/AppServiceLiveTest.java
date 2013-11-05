package io.cloudsoft.marklogic.appservers;

import org.testng.annotations.Test;

import io.cloudsoft.marklogic.SingleNodeLiveTest;
import io.cloudsoft.marklogic.databases.Database;

public class AppServiceLiveTest extends SingleNodeLiveTest {

    @Test(groups = {"Live"})
    public void testCreateRestAppService() throws Exception {
        LOG.info("-----------------testCreateRestAppService-----------------");

        int port = 8011;
        String appServiceName = user + "-app" + ID_GENERATOR.incrementAndGet();

        Database database = createDatabase();
        appServices.createAppServer(
                AppServerKind.HTTP, appServiceName, database, "Default", port);

        //todo: we should do a connect to the given url to make sure something is running there.
        //but the port is not open in firewall
        //String url =  "http://"+egroup.getAnyStartedMember().getHostName()+":"+port;
        //HttpTestUtils.assertContentContainsText(url, "Hello");
    }
}
