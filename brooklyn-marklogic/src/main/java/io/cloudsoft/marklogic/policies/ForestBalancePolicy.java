package io.cloudsoft.marklogic.policies;

import brooklyn.entity.Group;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.policy.ha.HASensors;
import brooklyn.policy.ha.MemberFailureDetectionPolicy;
import brooklyn.util.flags.SetFromFlag;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Detects when members of a group have failed/recovered, and emits ENTITY_FAILED or
 * ENTITY_RECOVERED accordingly.
 * <p/>
 * This policy should be associated with a group to monitor its members:
 * <pre>
 * {@code
 *     group.addPolicy(new MemberFailureDetectionPolicy(...));
 * }
 * </pre>
 * <p/>
 * Basic "failure" is defined as the service being "running" but isUp having flipped from
 * true to false.
 * <p/>
 * These criteria can be further configured using "onlyReportIfPreviouslyUp" and
 * "useServiceStateRunning".
 *
 * @author aled
 */
public class ForestBalancePolicy extends MemberFailureDetectionPolicy {
    private static final Logger LOG = LoggerFactory.getLogger(ForestBalancePolicy.class);

    @SetFromFlag
    private MarkLogicGroup group;

    public ForestBalancePolicy(Map<String, ?> flags) {
        super(flags);
    }

    @Override
    public void setEntity(EntityLocal entity) {
        super.setEntity(entity);

        subscribeToMembers((Group) entity, HASensors.ENTITY_FAILED, new SensorEventListener<HASensors.FailureDescriptor>() {
            @Override
            public void onEvent(SensorEvent<HASensors.FailureDescriptor> event) {
                MarkLogicNode node = (MarkLogicNode) event.getSource();
                LOG.info("ForestBalancePolicy node:" + node.getHostname() + " failed");
            }
        });

        subscribeToMembers((Group) entity, HASensors.ENTITY_RECOVERED, new SensorEventListener<HASensors.FailureDescriptor>() {
            @Override
            public void onEvent(SensorEvent<HASensors.FailureDescriptor> event) {
                MarkLogicNode node = (MarkLogicNode) event.getSource();
                LOG.info("ForestBalancePolicy node:" + node.getHostname() + " recovered");
            }
        });

    }
}
