package io.cloudsoft.marklogic;

import org.testng.annotations.Test;

@Test(groups = { "Live" })
public class MarkLogicEc2LiveTest extends AbstractMarkLogicLiveTest {

    private static final String PROVIDER = "aws-ec2";
    private static final String SMALL_HARDWARE_ID = "m1.small";
    private static final String US_EAST_1_REGION_NAME = "us-east-1";
    private static final String EU_WEST_1_REGION_NAME = "eu-west-1";

    @Override
    public String getProvider() {
        return PROVIDER;
    }
    
    @Test
    void test_Amazon_Linux_AMI_on_US_East_1() throws Exception {
        testMarkLogicNode(PROVIDER, US_EAST_1_REGION_NAME, "ami-3275ee5b", SMALL_HARDWARE_ID, "ec2-user");
    }

    @Test
    void test_Amazon_Linux_AMI_EU_West_1() throws Exception {
        testMarkLogicNode(PROVIDER, EU_WEST_1_REGION_NAME, "ami-44939930", SMALL_HARDWARE_ID, "ec2-user");
    }
    
    @Test
    void test_Centos_5_6_on_US_East_1() throws Exception {
        testMarkLogicNode(PROVIDER, US_EAST_1_REGION_NAME, "ami-49e32320", SMALL_HARDWARE_ID, "root");
    }
    
    @Test
    void test_Centos_5_6_on_EU_West_1() throws Exception {
        testMarkLogicNode(PROVIDER, EU_WEST_1_REGION_NAME, "ami-da3003ae", SMALL_HARDWARE_ID, "root");
    }
    
    @Test
    void test_Centos_6_3_on_US_East_1() throws Exception {
        testMarkLogicNode(PROVIDER, US_EAST_1_REGION_NAME, "ami-7d7bfc14", SMALL_HARDWARE_ID, "root");
    }
    
    @Test
    void test_Centos_6_3_on_EU_West_1() throws Exception {
        testMarkLogicNode(PROVIDER, EU_WEST_1_REGION_NAME, "ami-0ca7a878", SMALL_HARDWARE_ID, "root");
    }
    
    @Test
    void test_RHEL_6_on_US_East_1() throws Exception {
        testMarkLogicNode(PROVIDER, US_EAST_1_REGION_NAME, "ami-b30983da", SMALL_HARDWARE_ID, "root");
    }
    
    @Test
    void test_RHEL_6_on_EU_West_1() throws Exception {
        testMarkLogicNode(PROVIDER, EU_WEST_1_REGION_NAME, "ami-c07b75b4", SMALL_HARDWARE_ID, "root");
    }
    
}
