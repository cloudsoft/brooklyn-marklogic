package io.cloudsoft.marklogic;

import brooklyn.entity.basic.AbstractEntity;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.NamedParameter;

public class MarkLogicGroupImpl extends AbstractEntity implements MarkLogicGroup {

    @Override
    public void createAppServer(@NamedParameter("name") @Description("The name of the appServer") String name) {
        //todo: we need to forward this call to the right location.
    }
}
