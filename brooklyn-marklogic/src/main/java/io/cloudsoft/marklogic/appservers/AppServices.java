package io.cloudsoft.marklogic.appservers;

import brooklyn.entity.basic.AbstractGroup;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.NamedParameter;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;

@ImplementedBy(AppServicesImpl.class)
public interface AppServices extends AbstractGroup {

    @SetFromFlag("cluster")
    public static final BasicConfigKey<MarkLogicGroup> CLUSTER = new BasicConfigKey<MarkLogicGroup>(
            MarkLogicGroup.class, "marklogic.appservices.cluster", "The cluster");

    MethodEffector<Void> CREATE_APPSERVER =
            new MethodEffector<Void>(MarkLogicNode.class, "createRestAppServer");

    @Description("Creates a new Rest AppServer")
    void createRestAppServer(
            @NamedParameter("name") @Description("The name of the appServer") String name,
            @NamedParameter("database") @Description("The name of the database") String database,
            @NamedParameter("group") @Description("The name of the group this appServer belongs to") String group,
            @NamedParameter("port") @Description("The port of the appServer") String port);

}
