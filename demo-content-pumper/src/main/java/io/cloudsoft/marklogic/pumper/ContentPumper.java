package io.cloudsoft.marklogic.pumper;

import java.util.Collection;

import com.google.common.base.Supplier;
import com.google.common.reflect.TypeToken;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.trait.Startable;

@ImplementedBy(ContentPumperImpl.class)
public interface ContentPumper extends Entity, Startable {

    ConfigKey<Supplier<Collection<String>>> DATA_SUPPLIER = ConfigKeys.newConfigKey(
            new TypeToken<Supplier<Collection<String>>>() {},
            "marklogic.contentPump.dataSupplier",
            "A supplier of URLs for the content pump. Defaults to the official US government FTP site",
            new GovFtpUrlSupplier());

    /** Pump data to the given server once */
    public void pumpTo(String host, int port, String username, String password);

    /** Pump data to the given server continually */
    @Effector(description = "Starts pumping data to the MarkLogic server at the given host and port")
    public void startPumping(
            @EffectorParam(name = "host", description = "Hostname of a MarkLogic server") String host,
            @EffectorParam(name = "port", description = "Port of XDBC service running on the server") int port,
            @EffectorParam(name = "username", description = "Username for the MarkLogic server") String username,
            @EffectorParam(name = "password", description = "Password for the MarkLogic server") String password);

    @Effector(description = "Stops pumping data to all MarkLogic servers")
    public void stopAllPumping();

    @Effector(description = "Stops pumping data to the MarkLogic server at the given host and port")
    public void stopPumping(
            @EffectorParam(name = "host", description = "Hostname of a MarkLogic server") String host,
            @EffectorParam(name = "port", description = "Port of XDBC service running on the server") int port);

}
