package io.cloudsoft.marklogic;

import brooklyn.entity.Entity;
import brooklyn.entity.proxying.ImplementedBy;

@ImplementedBy(DatabaseImpl.class)
public interface Database extends Entity {
}
