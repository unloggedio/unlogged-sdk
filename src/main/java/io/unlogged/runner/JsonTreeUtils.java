package io.unlogged.runner;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonTreeUtils {

    public static JsonNode getValueFromJsonNode(JsonNode objectNode, String selectedKey) {
        if (selectedKey == null || selectedKey.equals("/")) {
            selectedKey = "";
        }
        return objectNode.at(selectedKey);
    }

}
