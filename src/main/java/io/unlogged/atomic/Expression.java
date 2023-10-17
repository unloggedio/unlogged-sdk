package io.unlogged.atomic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;

public enum Expression {
    SELF,
    SIZE,
    LENGTH;

    public JsonNode compute(JsonNode actualValue) {
        switch (this) {

            case SELF:
                return actualValue;
            case SIZE:
                return new IntNode(actualValue.size());
            case LENGTH:
                return new IntNode(actualValue.asText().length());
        }
        return actualValue;
    }
}
