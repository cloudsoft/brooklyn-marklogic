package io.cloudsoft.marklogic.pumper;

import brooklyn.entity.basic.SoftwareProcessDriver;

public interface ContentPumpDriver extends SoftwareProcessDriver {

    public void pumpTo(String host, int port, String username, String password);

}
