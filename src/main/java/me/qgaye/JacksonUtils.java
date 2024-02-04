package me.qgaye;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JacksonUtils {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static String serialize(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonNode toJsonNode(Object o) {
        try {
            return mapper.valueToTree(o);
        } catch (Exception e) {
            return null;
        }
    }

    public static JsonNode read(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

}
