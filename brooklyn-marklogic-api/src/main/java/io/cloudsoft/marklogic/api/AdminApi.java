package io.cloudsoft.marklogic.api;

public interface AdminApi {

    public boolean setServerLicense();

    public long getServerTimestamp();

    public boolean isServerUp();

}
