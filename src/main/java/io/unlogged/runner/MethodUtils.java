package io.unlogged.runner;

import io.unlogged.MethodSignatureParser;
import io.unlogged.atomic.MethodUnderTest;
import io.unlogged.command.AgentCommand;
import io.unlogged.command.AgentCommandRequest;
import io.unlogged.command.AgentCommandRequestType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodUtils {
    public static AgentCommandRequest createExecuteRequestWithParameters(
            MethodUnderTest methodUnderTest,
            ClassUnderTest classUnderTest,
            List<String> parameterValues,
            boolean processArgumentTypes) {

        AgentCommandRequest agentCommandRequest = new AgentCommandRequest();
        agentCommandRequest.setCommand(AgentCommand.EXECUTE);

        agentCommandRequest.setMethodSignature(methodUnderTest.getSignature());
        agentCommandRequest.setClassName(classUnderTest.getQualifiedClassName());
        agentCommandRequest.setMethodName(methodUnderTest.getName());

        if (methodUnderTest.getSignature() == null) {
            System.err.println("Method signature is null: " + methodUnderTest);
            return null;
        }
        List<String> methodSignatureTypes = MethodSignatureParser.parseMethodSignature(methodUnderTest.getSignature());
        // remove return type
        methodSignatureTypes.remove(methodSignatureTypes.size() - 1);

        String[] parameterCanonicalStrings = new String[methodSignatureTypes.size()];
        for (int i = 0; i < methodSignatureTypes.size(); i++) {
            parameterCanonicalStrings[i] = getDottedClassName(methodSignatureTypes.get(i));
        }
        agentCommandRequest.setParameterTypes(Arrays.asList(parameterCanonicalStrings));


        if (processArgumentTypes && parameterValues != null) {
            ArrayList<String> convertedParameterValues = new ArrayList<>(parameterValues.size());
            List<String> parameterTypes = agentCommandRequest.getParameterTypes();
            for (int i = 0; i < parameterValues.size(); i++) {
                String parameterValue = parameterValues.get(i);
                String parameterType = parameterTypes.get(i);
                if (parameterType.equals("float")) {
                    parameterValue = String.valueOf(Float.intBitsToFloat(Integer.parseInt(parameterValue)));
                } else if (parameterType.equals("double")) {
                    parameterValue = String.valueOf(Double.longBitsToDouble(Long.parseLong(parameterValue)));
                }
                convertedParameterValues.add(parameterValue);
            }
            parameterValues = convertedParameterValues;
        }


        agentCommandRequest.setMethodParameters(parameterValues);
        agentCommandRequest.setRequestType(AgentCommandRequestType.REPEAT_INVOKE);
        return agentCommandRequest;
    }

    public static String getDottedClassName(String className) {
        if (className == null) {
            return null;
        }
        if (className.contains(".")) {
            return className;
        }

        if (className.endsWith(";")) {
            className = className.substring(0, className.length() - 1);
        }

        while (className.startsWith("[")) {
            className = className.substring(1) + "[]";
        }
        if (className.startsWith("L")) {
            className = className.substring(1);
        }

        String dottedName = className.replace('/', '.');
        if (dottedName.contains("$$")) {
            dottedName = dottedName.substring(0, dottedName.indexOf("$$"));
        }
        return dottedName;
    }



}
