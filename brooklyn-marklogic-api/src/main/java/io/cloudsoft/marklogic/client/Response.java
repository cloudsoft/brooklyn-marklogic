package io.cloudsoft.marklogic.client;

import java.util.List;

public abstract class Response {

    public abstract int getStatusCode();

    public abstract boolean isErrorResponse();

    public abstract <T> T get(Class<T> type);

    public abstract <T> List<T> getList(Class<T> listType);

    public abstract String getResponseContentAsString();

}
