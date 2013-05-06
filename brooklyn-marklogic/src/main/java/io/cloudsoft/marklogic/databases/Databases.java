package io.cloudsoft.marklogic.databases;

import brooklyn.entity.Group;
import brooklyn.entity.basic.AbstractGroup;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.NamedParameter;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.MarkLogicCluster;
import io.cloudsoft.marklogic.MarkLogicNode;

@ImplementedBy(DatabasesImpl.class)
public interface Databases extends AbstractGroup {

    @SetFromFlag("cluster")
    public static final BasicConfigKey<MarkLogicCluster> CLUSTER = new BasicConfigKey<MarkLogicCluster>(
            MarkLogicCluster.class, "marklogic.databases.cluster", "The cluster");

    MethodEffector<Void> CREATE_DATABASE =
            new MethodEffector<Void>(MarkLogicNode.class, "createDatabase");

    @Description("Creates a new database")
    void createDatabase(
            @NamedParameter("name") @Description("The name of the database") String name);
}
