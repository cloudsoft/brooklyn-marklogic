package io.cloudsoft.marklogic;

import io.cloudsoft.marklogic.databases.Database;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicLong;

public class AppServiceLiveTest extends AbstractMarklogicLiveTest {
    public final static AtomicLong ID_GENERATOR = new AtomicLong();

    @Test
    public void testCreateRestAppService() throws Exception {
        LOG.info("-----------------testCreateRestAppService-----------------");

        int port = 8011;
        String appServiceName = user + "-app" + ID_GENERATOR.incrementAndGet();


        Database database = createDatabase();
        appServices.createRestAppServer(appServiceName, database.getName(), "Default", "" + port);

        //todo: we should do a connect to the given url to make sure something is running there.
        //but the port is not open in firewall
        //String url =  "http://"+egroup.getAnyStartedMember().getHostName()+":"+port;
        //HttpTestUtils.assertContentContainsText(url, "Hello");
    }
}
