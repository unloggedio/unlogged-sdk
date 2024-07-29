package io.unlogged.autoexecutor.testutils.autoCIUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.unlogged.autoexecutor.testutils.entity.DiffResultType;
import io.unlogged.autoexecutor.testutils.entity.DifferenceInstance;
import io.unlogged.autoexecutor.testutils.entity.DifferenceResult;
import io.unlogged.autoexecutor.testutils.entity.ValueDifference;
import io.unlogged.command.ResponseType;


import java.util.*;


public class DiffUtils {
    static final private ObjectMapper objectMapper = new ObjectMapper();

    static public DifferenceResult calculateDifferencesAeCi(String origial, String actual) {
        return calculateDifferences(origial, actual, ResponseType.NORMAL);
    }

    static private DifferenceResult calculateDifferences(String originalString, String actualString, ResponseType responseType) {
        //replace Boolean with enum
        if (responseType != null &&
                (responseType.equals(ResponseType.EXCEPTION) || responseType.equals(ResponseType.FAILED))) {
            return new DifferenceResult(null, DiffResultType.ACTUAL_EXCEPTION, getFlatMapFor(originalString), null);
        }
        try {
            JsonNode m1;
            if (originalString == null || originalString.isEmpty()) {
                m1 = objectMapper.createObjectNode();
            } else {
                m1 = objectMapper.readTree(originalString);
            }
            JsonNode m2 = objectMapper.readTree(actualString);
            if (m2 == null) {
                m2 = objectMapper.createObjectNode();
            }

            Map<String, Object> objectMapDifference = compareObjectNodes(m1, m2);
//            System.out.println(res);

//            res.entriesOnlyOnLeft().forEach((key, value) -> System.out.println(key + ": " + value));
            JsonNode leftOnly = (JsonNode) objectMapDifference.get("left");

//            res.entriesOnlyOnRight().forEach((key, value) -> System.out.println(key + ": " + value));
            JsonNode rightOnly = (JsonNode) objectMapDifference.get("right");

//            res.entriesDiffering().forEach((key, value) -> System.out.println(key + ": " + value));
            Map<String, ValueDifference> differences = (Map<String, ValueDifference>) objectMapDifference.get(
                    "differences");
            List<DifferenceInstance> differenceInstances = getDifferenceModel(leftOnly, rightOnly, differences);
            if (differenceInstances.size() == 0) {
                //no differences
                return new DifferenceResult(differenceInstances, DiffResultType.SAME, leftOnly, rightOnly);
            } else if (originalString == null || originalString.isEmpty()) {
                return new DifferenceResult(differenceInstances, DiffResultType.NO_ORIGINAL, leftOnly, rightOnly);
            } else {
//                merge left and right differences
//                or iterate and create a new pojo that works with 1 table model
                return new DifferenceResult(differenceInstances, DiffResultType.DIFF, leftOnly, rightOnly);
            }
        } catch (Exception e) {
            if ((originalString == null && actualString == null) || Objects.equals(originalString, actualString)) {
                return new DifferenceResult(new LinkedList<>(), DiffResultType.SAME, null, null);
            }
            //happens for malformed jsons or primitives.
            DifferenceInstance instance = new DifferenceInstance("Return Value", originalString, actualString,
                    DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE);
            ArrayList<DifferenceInstance> differenceInstances = new ArrayList<>();
            differenceInstances.add(instance);
            return new DifferenceResult(differenceInstances, DiffResultType.DIFF, null, null);
        }
    }

    public static Map<String, Object> compareObjectNodes(JsonNode node1, JsonNode node2) {
        Map<String, Object> differencesMap = new HashMap<>();
        compareObjectNodes(node1, node2, "", differencesMap);
        return differencesMap;
    }

    private static void compareObjectNodes(JsonNode node1, JsonNode node2, String path,
                                           Map<String, Object> differencesMap) {
        ObjectNode leftOnly = objectMapper.createObjectNode();
        ObjectNode rightOnly = objectMapper.createObjectNode();
        ObjectNode common = objectMapper.createObjectNode();
        Map<String, ValueDifference> differences = new HashMap<>();

        Iterator<String> fieldNames = node1.fieldNames();

        int leftFieldNameCount = 0;
        while (fieldNames.hasNext()) {
            leftFieldNameCount++;
            String fieldName = fieldNames.next();
            JsonNode value1 = node1.get(fieldName);
            JsonNode value2 = node2.get(fieldName);

            if (value2 == null) {
                leftOnly.put(fieldName, value1.toString());
            } else if (value1.equals(value2)) {
                common.put(fieldName, value1.toString());
            } else {
                if (value1.isObject() && value2.isObject()) {
                    compareObjectNodes(value1, value2, path + fieldName + ".", differencesMap);
                } else {
                    if (value1.isTextual()) {
                        differences.put(fieldName, new ValueDifference(value1.textValue(), value2.textValue()));
                    } else {
                        differences.put(fieldName, new ValueDifference(value1.toString(), value2.toString()));
                    }
                }
            }
        }

        fieldNames = node2.fieldNames();
        int rightFieldNameCount = 0;
        while (fieldNames.hasNext()) {
            rightFieldNameCount++;
            String fieldName = fieldNames.next();
            JsonNode value1 = node1.get(fieldName);
            if (value1 == null) {
                rightOnly.put(fieldName, node2.get(fieldName).toString());
            }
        }
        if (leftFieldNameCount == 0 && rightFieldNameCount > 0) {
            leftOnly.put(Objects.equals(path, "") ? "/" : path, node1.toString());
        } else if (leftFieldNameCount > 0 && rightFieldNameCount == 0) {
            rightOnly.put(Objects.equals(path, "") ? "/" : path, node2.toString());
        } else if (leftFieldNameCount == 0 && rightFieldNameCount == 0) {
            if (node1.equals(node2)) {
                common.put(Objects.equals(path, "") ? "/" : path, node1.toString());
            } else {
                differences.put(Objects.equals(path, "") ? "/" : path,
                        new ValueDifference(node1.toString(), node2.toString()));
            }
        } else {
            // both objects had fields
        }

        differencesMap.put("left", leftOnly);
        differencesMap.put("right", rightOnly);
        differencesMap.put("common", common);
        differencesMap.put("differences", differences);
    }


    public static JsonNode getFlatMapFor(String s1) {
        try {
            JsonNode m1;
            if (s1 == null || s1.isEmpty() || s1.equals("null")) {
                m1 = objectMapper.getNodeFactory().objectNode();
            } else {
                JsonNode map;
                try {
                    map = objectMapper.readTree(s1);
                } catch (Exception e) {
                    map = objectMapper.getNodeFactory().textNode(s1);
                }
                m1 = map;
                m1 = flatten(m1);
            }
            return m1;
        } catch (Exception e) {
            JsonNode m1 = objectMapper.getNodeFactory().textNode(s1);
            return m1;
        }
    }

    static private List<DifferenceInstance> getDifferenceModel(
            JsonNode left, JsonNode right,
            Map<String, ValueDifference> differences
    ) {
        ArrayList<DifferenceInstance> differenceInstances = new ArrayList<>();
        for (String key : differences.keySet()) {
            DifferenceInstance instance = new DifferenceInstance(key, differences.get(key).leftValue(),
                    differences.get(key).rightValue(), DifferenceInstance.DIFFERENCE_TYPE.DIFFERENCE);
            differenceInstances.add(instance);
        }
        if (left instanceof ObjectNode) {
            ObjectNode leftObject = (ObjectNode) left;
            for (Iterator<String> it = leftObject.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                DifferenceInstance instance = new DifferenceInstance(key, left.get(key),
                        "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
                differenceInstances.add(instance);
            }
        } else if (left instanceof ArrayNode) {
            ArrayNode leftObject = (ArrayNode) left;
            for (Iterator<String> it = leftObject.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                DifferenceInstance instance = new DifferenceInstance(key, left.get(key),
                        "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
                differenceInstances.add(instance);
            }
        } else {
            DifferenceInstance instance = new DifferenceInstance("Left", left,
                    "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
            differenceInstances.add(instance);
        }

        if (right instanceof ObjectNode) {
            ObjectNode leftObject = (ObjectNode) right;
            for (Iterator<String> it = leftObject.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                DifferenceInstance instance = new DifferenceInstance(key, right.get(key),
                        "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
                differenceInstances.add(instance);
            }
        } else if (right instanceof ArrayNode) {
            ArrayNode leftObject = (ArrayNode) right;
            for (Iterator<String> it = leftObject.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                DifferenceInstance instance = new DifferenceInstance(key, right.get(key),
                        "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
                differenceInstances.add(instance);
            }
        } else {
            DifferenceInstance instance = new DifferenceInstance("Left", right,
                    "", DifferenceInstance.DIFFERENCE_TYPE.LEFT_ONLY);
            differenceInstances.add(instance);
        }

        return differenceInstances;
    }

    public static JsonNode flatten(JsonNode node) {
        ObjectNode flattenedNode = objectMapper.createObjectNode();
        flatten("", node, flattenedNode);
        return flattenedNode;
    }

    private static void flatten(String prefix, JsonNode node, ObjectNode flattenedNode) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String newPrefix = prefix.isEmpty() ? field.getKey() : prefix + "/" + field.getKey();
                flatten(newPrefix, field.getValue(), flattenedNode);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String newPrefix = prefix + "/" + i;
                flatten(newPrefix, node.get(i), flattenedNode);
            }
        } else {
            flattenedNode.set(prefix, node);
        }
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
