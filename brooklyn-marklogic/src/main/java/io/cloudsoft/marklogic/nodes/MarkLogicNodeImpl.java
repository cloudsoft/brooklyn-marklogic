package io.cloudsoft.marklogic.nodes;

import static java.lang.String.format;

import io.cloudsoft.marklogic.appservers.RestAppServer;
import io.cloudsoft.marklogic.clusters.MarkLogicCluster;
import io.cloudsoft.marklogic.databases.Database;
import io.cloudsoft.marklogic.forests.Forest;
import io.cloudsoft.marklogic.forests.Forests;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.AttributeSensor;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.event.feed.function.FunctionFeed;
import brooklyn.event.feed.function.FunctionPollConfig;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class MarkLogicNodeImpl extends SoftwareProcessImpl implements MarkLogicNode {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicNodeImpl.class);

    private final AtomicInteger deviceNameSuffix = new AtomicInteger('h');
    private static final String NODE_NAME = "mynodename";

    private FunctionFeed serviceUp;

    private final Object attributeSetMutex = new Object();
    
    @Override
    public void init() {
        //we give it a bit longer timeout for starting up
        setConfig(ConfigKeys.START_TIMEOUT, 240);

        //todo: ugly.. we don't want to get the properties  this way, but for the time being it works.
        setConfig(WEBSITE_USERNAME, getManagementContext().getConfig().getFirst("brooklyn.marklogic.website-username"));
        setConfig(WEBSITE_PASSWORD, getManagementContext().getConfig().getFirst("brooklyn.marklogic.website-password"));
        setConfig(LICENSE_KEY, getManagementContext().getConfig().getFirst("brooklyn.marklogic.license-key"));
        setConfig(LICENSEE, getManagementContext().getConfig().getFirst("brooklyn.marklogic.licensee"));
        setConfig(CLUSTER_NAME, getManagementContext().getConfig().getFirst("brooklyn.marklogic.cluster"));
        String configuredVersion = getManagementContext().getConfig().getFirst("brooklyn.marklogic.version");
        if (configuredVersion != null && !configuredVersion.isEmpty()) {
            setConfig(SoftwareProcess.SUGGESTED_VERSION, configuredVersion);
        }

        subscribe(this, MarkLogicNode.SERVICE_UP, new SensorEventListener<Boolean>() {
            Boolean previous;

            @Override
            public void onEvent(SensorEvent<Boolean> event) {
                Boolean newValue = event.getValue();
                if (newValue != null && !newValue.equals(previous)) {
                    onServiceUp(newValue);
                }
                previous = newValue;
            }
        });
    }

    private void onServiceUp(boolean up) {
        if (up) {
            LOG.info("MarkLogicNode: " + getHostName() + " is up");
            Forests forests = getCluster().getForests();
            Forest targetForest = null;
            for (Forest forest : forests.asList()) {
                if (forest.createdByBrooklyn() && forest.getMaster() == null) {
                    targetForest = forest;
                    break;
                }
            }

            if (targetForest != null)
                forests.moveForest(targetForest.getName(), getHostName());
        } else {
            LOG.info("MarkLogicNode: " + getHostName() + " is not up");
        }
    }

   @Override
   public void stop() {
        LOG.info("Stopping MarkLogicNode: "+getHostName()+" Moving all forests out");
        getCluster().getForests().moveAllForestFromNode(getHostName());
        LOG.info("Stopping MarkLogicNode: "+getHostName()+" Finished Moving all forests out, continue to stop");
        super.stop();
        LOG.info("Stopping MarkLogicNode: "+getHostName()+" Node now completely shutdown");
   }

    public Class getDriverInterface() {
        return MarkLogicNodeDriver.class;
    }

    /**
     * Sets up the polling of sensors.
     */
    @Override
    protected void connectSensors() {
        super.connectSensors();

        serviceUp = FunctionFeed.builder()
                .entity(this)
                .period(5000)
                .poll(new FunctionPollConfig<Boolean, Boolean>(SERVICE_UP)
                        .onError(Functions.constant(Boolean.FALSE))
                        .callable(new Callable<Boolean>() {
                            public Boolean call() {
                                return getDriver().isRunning();
                            }
                        }))
                .build();
    }


    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();

        if (serviceUp != null) serviceUp.stop();
    }

    /**
     * The ports to be opened in the VM (e.g. in the aws-ec2 security group created by jclouds).
     */
    @Override
    protected Collection<Integer> getRequiredOpenPorts() {
        // TODO What ports need to be open?
        // I got these from `sudo netstat -antp` for the MarkLogic daemon
        // TODO If want to use a pre-existing security group instead, can add to
        //      obtainProvisioningFlags() something like:
        //      .put("securityGroups", groupName)
        //TODO: the 8011 port has been added so we can register an application on that port. In the future this needs to come
        //from the application, but for the time being it is hard coded.
        int bindPort = getConfig(BIND_PORT);
        int foreignBindPort = getConfig(FOREIGN_BIND_PORT);
        return ImmutableSet.copyOf(Iterables.concat(super.getRequiredOpenPorts(), ImmutableList.of(bindPort, foreignBindPort, 8000, 8001, 8002, 8011)));
    }

    private NodeType getNodeType() {
        return getConfig(NODE_TYPE);
    }

    @Override
    public MarkLogicNodeDriver getDriver() {
        return (MarkLogicNodeDriver) super.getDriver();
    }

    public String getHostName() {
        return getAttribute(HOSTNAME);
    }

    public String getGroupName() {
        return getConfig(GROUP);
    }

    @Override
    public void createForest(Forest forest) {
        getDriver().createForest(forest);
        addToAttributeSet(FOREST_NAMES, forest.getName());
    }

    @Override
    public void createDatabaseWithForest(String name) {
        getDriver().createDatabaseWithForest(name);
    }

    @Override
    public void createDatabase(Database database) {
        getDriver().createDatabase(database);
    }

    @Override
    public void createRestAppServer(RestAppServer appServer) {
        getDriver().createAppServer(appServer);
    }

    @Override
    public void attachForestToDatabase(String forestName, String databaseName) {
        getDriver().attachForestToDatabase(forestName, databaseName);
    }

    @Override
    public void unmount(Forest forest) {
        getDriver().unmountForest(forest);
        removeFromAttributeSet(FOREST_NAMES, forest.getName());
    }

    @Override
    public void mount(Forest forest) {
        getDriver().mountForest(forest);
        addToAttributeSet(FOREST_NAMES, forest.getName());
    }

    @Override
    public void createGroup(String groupName) {
        getDriver().createGroup(groupName);
    }

    @Override
    public void assignHostToGroup(String hostName, String groupName) {
        getDriver().assignHostToGroup(hostName, groupName);
    }

    @Override
    public Set<String> scanForests() {
        return getDriver().scanForests();
    }

    @Override
    public Set<String> scanDatabases() {
        return getDriver().scanDatabases();
    }

    @Override
    public boolean isUp() {
        return getAttribute(SERVICE_UP);
    }

    @Override
    public MarkLogicCluster getCluster() {
        return getConfig(CLUSTER);
    }

    @Override
    public void attachReplicaForest(String primaryForestName, String replicaForestName) {
        getDriver().attachReplicaForest(primaryForestName, replicaForestName);
    }

    @Override
    public void enableForest(String forestName, boolean enabled) {
        getDriver().enableForest(forestName, enabled);
    }

    @Override
    public void deleteForestConfiguration(String forestName) {
        getDriver().deleteForestConfiguration(forestName);
        removeFromAttributeSet(FOREST_NAMES, forestName);
    }

    @Override
    public void setForestHost(String forestName, String hostname) {
        getDriver().setForestHost(forestName, hostname);
    }

    @Override
    public String getForestStatus(String forestName) {
        return getDriver().getForestStatus(forestName);
    }

    @Override
    public String getPassword() {
        return getConfig(PASSWORD);
    }

    @Override
    public String getAdminConnectUrl() {
        return "http://"+getAttribute(MarkLogicNode.HOSTNAME)+":8001";
    }

    @Override
    public String getUser() {
        return getConfig(USER);
    }
    
    private <T> void addToAttributeSet(AttributeSensor<Set<T>> attribute, T newval) {
        Set<T> newvals = Sets.newLinkedHashSet();
        newvals.add(newval);
        synchronized (attributeSetMutex) {
            Set<T> existing = getAttribute(attribute);
            if (existing != null) newvals.addAll(existing);
            setAttribute(attribute, newvals);
        }
    }
    
    private <T> void removeFromAttributeSet(AttributeSensor<Set<T>> attribute, T oldval) {
        Set<T> newvals = Sets.newLinkedHashSet();
        synchronized (attributeSetMutex) {
            Set<T> existing = getAttribute(attribute);
            if (existing != null) newvals.addAll(existing);
            newvals.remove(oldval);
            setAttribute(attribute, newvals);
        }
    }
}
