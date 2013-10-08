package io.cloudsoft.marklogic.api;

public interface MarkLogicApi {

    public AdminApi getAdminApi();
    public AdminApi getAdminApi(int port);

}
