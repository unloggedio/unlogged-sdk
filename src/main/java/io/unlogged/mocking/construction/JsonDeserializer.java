package io.unlogged.mocking.construction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class JsonDeserializer {

    private final ObjectMapper objectMapper;
    private final Map<String, Function<Object, Object>> typeHandlers;

    public JsonDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.typeHandlers = new HashMap<>();
        initializeTypeHandlers();
    }

    private void initializeTypeHandlers() {
        typeHandlers.put(CompletableFuture.class.getCanonicalName(),
                (value) -> java.util.concurrent.CompletableFuture.supplyAsync(() -> value));
        typeHandlers.put(Optional.class.getCanonicalName(), Optional::of);
        typeHandlers.put(LocalDate.class.getCanonicalName(), e -> LocalDate.parse((CharSequence) e));
        typeHandlers.put(LocalDateTime.class.getCanonicalName(), e -> LocalDateTime.parse((CharSequence) e));
        typeHandlers.put(BigDecimal.class.getCanonicalName(), e -> new BigDecimal(String.valueOf(e)));
        typeHandlers.put(URI.class.getCanonicalName(), e -> URI.create(String.valueOf(e)));
        typeHandlers.put(UUID.class.getCanonicalName(), e -> UUID.fromString(String.valueOf(e)));
    }

    public Object createInstance(String value, JavaType typeReference) {
        if (typeReference.containedTypeCount() > 0) {
            String topClassCanonicalName = typeReference.getRawClass().getCanonicalName();
            Function<Object, Object> handler = typeHandlers.get(topClassCanonicalName);
            if (handler != null) {
                Object containedValue = createInstance(value, typeReference.containedType(0));
                return handler.apply(containedValue);
            }
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException e) {
            return e;
        }
    }
}