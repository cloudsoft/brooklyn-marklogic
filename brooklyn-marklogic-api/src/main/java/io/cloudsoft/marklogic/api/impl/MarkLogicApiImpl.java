package io.cloudsoft.marklogic.api.impl;

import java.net.URI;

import io.cloudsoft.marklogic.api.ForestApi;
import io.cloudsoft.marklogic.api.MarkLogicApi;
import io.cloudsoft.marklogic.client.HttpClient;

public class MarkLogicApiImpl implements MarkLogicApi {

    private final HttpClient client;

    public MarkLogicApiImpl(URI endpoint, String username, String password) {
        client = new HttpClient(endpoint, username, password);
    }

    public MarkLogicApiImpl(String endpoint, String username, String password) {
        client = new HttpClient(endpoint, username, password);
    }

    @Override
    public AdminApiImpl getAdminApi() {
        return new AdminApiImpl(client);
    }

    @Override
    public AdminApiImpl getAdminApi(int port) {
        return new AdminApiImpl(client, port);
    }

    @Override
    public ForestApiImpl getForestApi() {
        return new ForestApiImpl(client);
    }

    @Override
    public ForestApiImpl getForestApi(int port) {
        return new ForestApiImpl(client, port);
    }
}
