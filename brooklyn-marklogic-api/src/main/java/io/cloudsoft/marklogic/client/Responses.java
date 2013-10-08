package io.cloudsoft.marklogic.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;

public class Responses {

    private Responses() {}

    public static Response newResponse(HttpResponse httpResponse) {
        return new ValidResponse(httpResponse);
    }

    public static Response newExceptionResponse(Exception exception) {
        return new ExceptionResponse(exception);
    }

    private static class ExceptionResponse extends Response {

        private final Exception exception;

        protected ExceptionResponse(Exception exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }

        // TODO: -1 suggests the superclass isn't right.
        @Override
        public int getStatusCode() {
            return -1;
        }

        @Override
        public boolean isErrorResponse() {
            return true;
        }

        @Override
        public <T> T get(Class<T> type) {
            throw new UnsupportedOperationException("Exception responses cannot be cast to instances of: " + type.getName());
        }

        @Override
        public <T> List<T> getList(Class<T> listType) {
            throw new UnsupportedOperationException("Exception responses cannot be cast to lists of: " + listType.getName());
        }

        // TODO: Or throw exception for parity with get and getList
        @Override
        public String getResponseContentAsString() {
            return "";
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("exception", exception)
                    .toString();
        }
    }

    private static class ValidResponse extends Response {
        private static final Logger LOG = LoggerFactory.getLogger(ValidResponse.class);

        private final HttpResponse response;
        private final ObjectMapper mapper;

        public ValidResponse(HttpResponse httpResponse) {
            this.response = httpResponse;
            this.mapper = null;
        }

        @Override
        public int getStatusCode() {
            return response.getStatusLine().getStatusCode();
        }

        @Override
        public boolean isErrorResponse() {
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode < 200 || statusCode >= 400;
        }

        /**
         * @see org.apache.http.util.EntityUtils#consume(org.apache.http.HttpEntity)
         */
        public Response consumeResponse() {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            return this;
        }

        public HttpResponse getResponse() {
            return response;
        }

        @Override
        public <T> T get(Class<T> type) {
            T object = unmarshalResponseEntity(mapper.constructType(type));
            consumeResponse();
            return object;
        }

        @Override
        public <T> List<T> getList(Class<T> listType) {
            JavaType collectionType = mapper.getTypeFactory().constructCollectionType(List.class, listType);
            List<T> object = unmarshalResponseEntity(mapper.constructType(collectionType));
            consumeResponse();
            return object;
        }

        private <T> T unmarshalResponseEntity(JavaType type) {
            if (isErrorResponse()) {
                LOG.debug("Request errored[{}], not unmarshalling to {}",
                        response.getStatusLine().getStatusCode(), type.getGenericSignature());
                return null;
            }

            String responseContent = getResponseContentAsString();
            if (LOG.isTraceEnabled()) LOG.trace(responseContent);
            try {
                T unmarshalled = mapper.readValue(responseContent, type);
                if (LOG.isTraceEnabled()) LOG.trace("Unmarshalled: " + unmarshalled.toString());
                return unmarshalled;
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public String getResponseContentAsString() {
            InputStream in = null;
            try {
                in = response.getEntity().getContent();
                return CharStreams.toString(new InputStreamReader(in, "UTF-8"));
            } catch (IOException e) {
                throw Throwables.propagate(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        if (LOG.isTraceEnabled())
                            LOG.trace("Exception closing response stream", e);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("statusCode", getStatusCode())
                    .add("response", getResponse())
                    .toString();
        }
    }

}
