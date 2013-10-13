package io.cloudsoft.marklogic.client;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class MarkLogicObjectMapper {

    /**
     * Returns an object mapper configured to ignore unknown properties when deserialising
     * and to only serialise fields annotated {@link com.fasterxml.jackson.annotation.JsonProperty}.
     */
    public static com.fasterxml.jackson.databind.ObjectMapper newObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(Include.NON_NULL)

                // Only serialise annotated fields
                .setVisibility(PropertyAccessor.ALL, Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    }

}