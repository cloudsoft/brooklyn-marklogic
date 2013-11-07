package io.cloudsoft.marklogic.api.impl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import io.cloudsoft.marklogic.api.AdminApi;
import io.cloudsoft.marklogic.client.Response;
import io.cloudsoft.marklogic.client.HttpClient;

public class AdminApiImpl implements AdminApi {

    private final HttpClient client;
    private final int port;

    /** Creates AdminApiImpl pointing at port 8001. */
    public AdminApiImpl(HttpClient client) {
        this(client, 8001);
    }

    public AdminApiImpl(HttpClient client, int port) {
        this.client = client;
        this.port = port;
    }

    @Override
    public long getServerTimestamp() {
        Response response = client.newRequest("/admin/v1/timestamp")
                .port(port)
                .get();
        String date = response.getResponseContentAsString();
        /** This is the simplest way to parse ISO8601 dates I could find.
          * SimpleDateFormat just can't do it. */
        return DatatypeConverter.parseDateTime(date).getTimeInMillis();
    }

    @Override
    public boolean isServerUp() {
        Response response = client.newRequest("/admin/v1/timestamp")
                .port(port)
                .head();
        return response.getStatusCode() == 200;
    }

    @Override
    public boolean removeNodeFromCluster() {
        Response response = client.newRequest("/admin/v1/host-config")
                .port(port)
                .delete();
        return response.getStatusCode() == 202;
    }
}
