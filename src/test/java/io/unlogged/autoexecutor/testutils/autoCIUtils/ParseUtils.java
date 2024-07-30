package io.unlogged.autoexecutor.testutils.autoCIUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.unlogged.mocking.DeclaredMock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseUtils {
    public static Map<String, List<String>> getTypeAndParameter(String input) {
        List<String> types = new ArrayList<>();
        List<String> parameters = new ArrayList<>();
        String[] argumentPairs = input.split("\\n");

        for (int i = 0; i < argumentPairs.length; i++) {
            String argument = argumentPairs[i];
            if (argument.equals("]")) {
                continue;
            }
            String parts[] = argument.split(":", 2);
            types.add(cleanText(parts[0]));
            parameters.add(cleanText(parts[1]));
        }
        HashMap<String, List<String>> typesAndParams = new HashMap<>();
        types.set(types.size() - 1, cleanLast(types.get(types.size() - 1)));
        parameters.set(types.size() - 1, cleanLast(parameters.get(parameters.size() - 1)));
        typesAndParams.put("types", types);
        typesAndParams.put("parameters", parameters);
        return typesAndParams;
    }

    private static String cleanText(String text) {
        if (text.startsWith("[")) {
            text = text.substring(1);
        }
        if (text.startsWith(",")) {
            text = text.substring(1);
        }
        return text.trim();
    }

    private static String cleanLast(String text) {
        if (text.endsWith(",")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        return text;
    }

    public static List<DeclaredMock> getDeclaredMocksFrom(String jsonStringInput) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            TypeReference<List<DeclaredMock>> typeReference = new TypeReference<List<DeclaredMock>>() {
            };
            return objectMapper.readValue(jsonStringInput, typeReference);
        } catch (Exception e) {
            System.out.println("Unable to convert input to List of declared mocks " + e);
            return new ArrayList<>();
        }
    }
}
