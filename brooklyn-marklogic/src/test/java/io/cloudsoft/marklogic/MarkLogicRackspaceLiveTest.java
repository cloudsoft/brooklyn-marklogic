package io.cloudsoft.marklogic;

import org.testng.annotations.Test;

@Test(groups = { "Live" })
public class MarkLogicRackspaceLiveTest extends AbstractMarkLogicLiveTest {

    private static final String PROVIDER = "cloudservers-uk";
    private static final String SMALL_HARDWARE_ID = "2";
    
    @Override
    public String getProvider() {
        return PROVIDER;
    }
    
    @Test
    void test_Centos_6_3_on_UK() throws Exception {
        testMarkLogicNode(PROVIDER, null, "127", SMALL_HARDWARE_ID, "root");
    }

    /*
    @Test
    void test_Centos_5_6_on_UK() throws Exception {
        testMarkLogicNode(PROVIDER, null, "114", SMALL_HARDWARE_ID, "root");
    }
    
    @Test
    void test_RHEL_6_on_UK() throws Exception {
        testMarkLogicNode(PROVIDER, null, "111", SMALL_HARDWARE_ID, "root");
    }
    */
    
}
