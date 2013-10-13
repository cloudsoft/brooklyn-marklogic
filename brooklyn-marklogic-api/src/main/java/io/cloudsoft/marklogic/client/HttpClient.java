package io.cloudsoft.marklogic.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

public class HttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClient.class);

    private final URI endpoint;
    private final DefaultHttpClient httpClient = new DefaultHttpClient(new PoolingClientConnectionManager());

    public HttpClient(String endpoint, String username, String password) {
        try {
            this.endpoint = new URI(checkNotNull(endpoint, "endpoint"));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Couldn't turn endpoint into URI", e);
        }
        setCredentialsProvider(username, password);
    }

    public HttpClient(URI endpoint, String username, String password) {
        this.endpoint = checkNotNull(endpoint, "endpoint");
        setCredentialsProvider(username, password);
    }

    public RequestBuilder newRequest(String uri) {
        return new RequestBuilder(httpClient, endpoint, uri);
    }

    /** Sets username and password on httpClient */
    private void setCredentialsProvider(String username, String password) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                new AuthScope(this.endpoint.getHost(), AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(username, password));
        httpClient.setCredentialsProvider(credentialsProvider);
    }

    public static class RequestBuilder {
        private static final ObjectMapper MAPPER = MarkLogicObjectMapper.newObjectMapper();
        private static final AtomicInteger requestCounter = new AtomicInteger(0);

        private final org.apache.http.client.HttpClient client;
        private final URI endpoint;
        private final String path;

        private int port;
        private Map<String, Object> parameters;

        public RequestBuilder(org.apache.http.client.HttpClient client, URI endpoint, String path) {
            this.client = checkNotNull(client, "client");
            this.endpoint = checkNotNull(endpoint, "endpoint");
            this.path = checkNotNull(path, "path");
            checkArgument(path.length() == 0 || path.charAt(0) == '/', "Path must begin with /");
        }

        public RequestBuilder port(int port) {
            this.port = port;
            return this;
        }

        public RequestBuilder contentType() {
            return this;
        }

        public RequestBuilder accept() {
            return this;
        }

        public RequestBuilder queryParam(String name, Object value) {
            this.parameters.put(name, String.valueOf(value));
            return this;
        }

        public Response get() {
            HttpGet request = new HttpGet(makeRequestUri());
            return doRequest(request);
        }

        public Response post() {
            HttpPost request = new HttpPost(makeRequestUri());
            return doRequest(request);
        }

        public Response post(Object body) {
            HttpPost request = new HttpPost(makeRequestUri());
            attachEntityToRequest(request, checkNotNull(body, "body"));
            return doRequest(request);
        }

        public Response postForm(Map<String, ?> body) {
            throw new UnsupportedOperationException();
        }

        public Response put(Object body) {
            HttpPut request = new HttpPut(makeRequestUri());
            attachEntityToRequest(request, checkNotNull(body, "body"));
            return doRequest(request);
        }

        public Response head() {
            HttpHead request = new HttpHead(makeRequestUri());
            return doRequest(request);
        }

        public Response delete() {
            HttpDelete request = new HttpDelete(makeRequestUri());
            return doRequest(request);
        }

        /** Combines endpoint constructor parameter with arguments given to builder methods. */
        private URI makeRequestUri() {
            try {
                return new URIBuilder(endpoint)
                        .setPath(endpoint.getPath() + path)
                        .setPort(port)
                        .build();
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        }

        private Response doRequest(HttpRequestBase request) {
            int requestCount = requestCounter.incrementAndGet();
            request.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            LOG.debug("Request #{}: {}", requestCount, request);
            try {
                HttpResponse httpResponse = client.execute(request);
                LOG.debug("Response #{}: {}", requestCount, httpResponse);
                Response response = Responses.newResponse(httpResponse);
                if (response.isErrorResponse()) {
                    LOG.warn("Request #{} errored: {}", requestCount, response.toString());
                }
                return response;
            } catch (IOException e) {
                return Responses.newExceptionResponse(e);
            }
        }

        private void attachEntityToRequest(HttpEntityEnclosingRequestBase request, Object entity) {
            StringWriter writer = new StringWriter();
            try {
                MAPPER.writeValue(writer, entity);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }

            LOG.trace("Marshalled "+entity.getClass().getSimpleName()+": " + writer.toString());
            HttpEntity httpEntity = new StringEntity(writer.toString(), ContentType.APPLICATION_JSON);
            request.setEntity(httpEntity);
        }
    }

}
