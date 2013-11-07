package io.cloudsoft.marklogic;

public class SingleNodeLiveTest extends AbstractMarkLogicLiveTest {

    @Override
    public int getNumberOfDNodes() {
        return 1;
    }

    @Override
    public int getNumberOfENodes() {
        return 0;
    }

}
