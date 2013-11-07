package io.cloudsoft.marklogic.api;

public interface MarkLogicApi {

    public AdminApi getAdminApi();
    public AdminApi getAdminApi(int port);

    public ForestApi getForestApi();
    public ForestApi getForestApi(int port);

}
