package io.cloudsoft.marklogic;

import brooklyn.entity.basic.SoftwareProcessDriver;

public interface MarkLogicDriver extends SoftwareProcessDriver {

    void createForest(Forest forest);

    void createDatabase(String name);
}
