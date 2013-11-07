package io.cloudsoft.marklogic.client;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class MarkLogicObjectMapper {

    /**
     * An object mapper configured to ignore unknown properties when deserialising and to only
     * serialise fields annotated {@link com.fasterxml.jackson.annotation.JsonProperty}.
     */
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .enable(SerializationFeature.WRAP_ROOT_VALUE)
            .setSerializationInclusion(Include.NON_NULL)

            // Only serialise annotated fields
            .setVisibility(PropertyAccessor.ALL, Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
}