package io.cloudsoft.marklogic.dto;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

import io.cloudsoft.marklogic.client.MarkLogicObjectMapper;

public abstract class MarshallingTest {

    public <T> T unmarshalFile(String filename, Class<T> as) {
        try {
            return MarkLogicObjectMapper.MAPPER.readValue(Resources.getResource(filename), as);
        } catch (IOException e) {
            throw new RuntimeException("Unmarshal to " + as + " failed", e);
        }
    }

    public <T> T unmarshalFile(String filename, TypeReference<T> as) {
        try {
            return MarkLogicObjectMapper.MAPPER.readValue(Resources.getResource(filename), as);
        } catch (IOException e) {
            throw new RuntimeException("Unmarshal to " + as + " failed", e);
        }
    }

}
