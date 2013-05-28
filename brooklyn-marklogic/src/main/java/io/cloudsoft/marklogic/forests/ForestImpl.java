package io.cloudsoft.marklogic.forests;

import brooklyn.entity.basic.AbstractEntity;
import brooklyn.event.feed.http.HttpFeed;
import io.cloudsoft.marklogic.groups.MarkLogicGroup;
import io.cloudsoft.marklogic.nodes.MarkLogicNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ForestImpl extends AbstractEntity implements Forest {

    private static final Logger LOG = LoggerFactory.getLogger(ForestImpl.class);

    private HttpFeed httpFeed;

    @Override
    public String getName() {
        return getConfig(NAME);
    }

    @Override
    public String getHost() {
        return getConfig(HOST);
    }

    @Override
    public String getDataDir() {
        return getConfig(DATA_DIR);
    }

    @Override
    public String getLargeDataDir() {
        return getConfig(LARGE_DATA_DIR);
    }

    @Override
    public String getFastDataDir() {
        return getConfig(FAST_DATA_DIR);
    }

    @Override
    public UpdatesAllowed getUpdatesAllowed() {
        return getConfig(UPDATES_ALLOWED);
    }

    @Override
    public boolean isRebalancerEnabled() {
        return getConfig(REBALANCER_ENABLED);
    }

    @Override
    public boolean isFailoverEnabled() {
        return getConfig(FAILOVER_ENABLED);
    }

    @Override
    public String getStatus() {
        return getAttribute(STATUS);
    }

    @Override
    public void init() {
        super.init();
        connectSensors();
    }

    public MarkLogicNode getAnyUpNode() {
        final MarkLogicGroup group = getConfig(GROUP);
        if(group == null)throw new NullPointerException("Group was not configured");
        return group.getAnyStartedMember();
    }

    private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void connectSensors() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    MarkLogicNode node = getAnyUpNode();
                    if (node == null) {
                        LOG.info("No node found to check forest: " + getName());
                        return;
                    }
                    String status = node.getForestStatus(getName());
                    LOG.info(status);
                } catch (Exception e) {
                    LOG.info("failed to check status", e);
                }
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);


        // "up" is defined as returning a valid HTTP response from nginx (including a 404 etc)
        //HttpFeed httpFeed = HttpFeed.builder()
        //       .entity(this)
        //       .period(getConfig(HTTP_POLL_PERIOD))
        //       .baseUri(accessibleRootUrl)
        //       .baseUriVars(ImmutableMap.of("include-runtime", "true"))
        //       .poll(new HttpPollConfig<Boolean>(SERVICE_UP)
        //               .onSuccess(new Function<HttpPollValue, Boolean>() {
        //                   @Override
        //                   public Boolean apply(HttpPollValue input) {
        ///                       // Accept any nginx response (don't assert specific version), so that sub-classing
        //                       // for a custom nginx build is not strict about custom version numbers in headers
        //                       List<String> actual = input.getHeaderLists().get("Server");
        //                       return actual != null && actual.size() == 1 && actual.get(0).startsWith("nginx");
        //                    }
        //                })
        //                .onError(Functions.constant(false)))
        //        .build();
    }
}
