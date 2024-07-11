package io.unlogged.logging.perthread;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class OtelConfig {

    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("unlogged-spring-maven-demo");

    private static ObjectMapper instance;

    static {
        instance = new ObjectMapper();
    }

    public static void makeSpan(String keyValue, Object objectValue) {

        // serialise object
        Span span = tracer.spanBuilder("custom_tracer.1").startSpan();
        try {
            String objectString = instance.writeValueAsString(objectValue);
            span.setAttribute(keyValue, objectString);
        }
        catch (JsonProcessingException e){
            e.printStackTrace();
            span.setAttribute(keyValue, "null");
        }
        span.end();
    }

    public static void registerMethod(
            Span span,
            int id,
            String className,
            String methodName,
            String argumentTypes,
            String returnType,
            boolean isStatic,
            boolean isPublic,
            boolean usesFields,
            int methodAccess,
            String methodDesc
    ) {

        MethodMetadata methodMetadata = new MethodMetadata(className, methodName, argumentTypes, returnType, isStatic, isPublic, usesFields, methodAccess, methodDesc);
        try {
            String objectString = instance.writeValueAsString(methodMetadata);
            span.setAttribute("methodRegistration-" + id, objectString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            span.setAttribute("method_registration", "method registration have failed");
        }
    }

}