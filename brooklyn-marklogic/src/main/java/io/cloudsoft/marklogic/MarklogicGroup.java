package io.cloudsoft.marklogic;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.NamedParameter;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(MarkLogicGroupImpl.class)
public interface MarkLogicGroup extends Entity {

    @SetFromFlag("name")
    ConfigKey<String> NAME = new BasicConfigKey<String>(
            String.class, "marklogic.group.name",
            "The name of the Group", null);

    //todo: we need a whole bunch of extra parameters here. It is just place holder method to see we
    //are going in the right direction
    @Description("Creates a AppServer within this group")
    void createAppServer(
            @NamedParameter("name") @Description("The name of the appServer") String name);
}
