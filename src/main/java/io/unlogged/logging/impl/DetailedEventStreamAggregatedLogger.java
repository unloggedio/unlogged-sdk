package io.unlogged.logging.impl;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import com.insidious.common.weaver.ClassInfo;

import io.unlogged.logging.IEventLogger;
import io.unlogged.logging.ObjectMapperFactory;
import io.unlogged.logging.SerializationMode;
import io.unlogged.logging.util.AggregatedFileLogger;
import io.unlogged.logging.util.ObjectIdAggregatedStream;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


/**
 * This class is an implementation of IEventLogger that records
 * a sequence of runtime events in files.
 * <p>
 * The detailed recorder serializes all the object values in addition to the object id being
 * otherwise recorded. The serialized data is to be used for test case generation
 * <p>
 * This object creates three types of files:
 * 1. log-*.slg files recording a sequence of events,
 * 2. LOG$Types.txt recording a list of type IDs and their corresponding type names,
 * 3. ObjectIdMap recording a list of object IDs and their type IDs.
 * Using the second and third files, a user can know classes in an execution trace.
 */
public class DetailedEventStreamAggregatedLogger implements IEventLogger {

    //    public static final Duration MILLI_1 = Duration.of(1, ChronoUnit.MILLIS);
    public static final String FAILED_TO_RECORD_MESSAGE =
            "{\"error\": \"failed to serialize object\", \"message\":\"";
    private static boolean isReactive = false;
    private final AggregatedFileLogger aggregatedLogger;
    private final ObjectIdAggregatedStream objectIdMap;
    private final Boolean DEBUG = Boolean.parseBoolean(System.getProperty("UNLOGGED_DEBUG"));
    private final ThreadLocal<Boolean> isRecording = ThreadLocal.withInitial(() -> false);
    final private boolean serializeValues = true;
    private final Set<Integer> probesToRecord = new HashSet<>();
    private final Set<Long> valueToSkip = new HashSet<>();
    private final SerializationMode SERIALIZATION_MODE = SerializationMode.JACKSON;
    private final Map<String, WeakReference<Object>> objectMap = new HashMap<>();
    private final ThreadLocal<ObjectMapper> objectMapper = ThreadLocal.withInitial(
            ObjectMapperFactory::createObjectMapperReactive);
    private final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    private final Map<Integer, Integer> firstProbeId = new HashMap<>();
    InvertedRadixTree<Boolean> invertedRadixTree = new ConcurrentInvertedRadixTree<>(new DefaultCharArrayNodeFactory());
    private ClassLoader targetClassLoader;

    /**
     * Create an instance of logging object.
     *
     * @param objectIdMap      object to id converter
     * @param aggregatedLogger writer
     */
    public DetailedEventStreamAggregatedLogger(
//            String includedPackage,
            ObjectIdAggregatedStream objectIdMap,
            AggregatedFileLogger aggregatedLogger) {

//        this.includedPackage = includedPackage;
        this.aggregatedLogger = aggregatedLogger;
        this.objectIdMap = objectIdMap;
        try {
            Class.forName("reactor.core.publisher.Mono");
            isReactive = true;
        } catch (Exception e) {
        }

        initSkipPackages();

    }

    private void initSkipPackages() {

        invertedRadixTree.put("com.google", true);
        invertedRadixTree.put("java.util.stream", true);
        invertedRadixTree.put("org.pf4j", true);
        invertedRadixTree.put("org.elasticsearch.client", true);
        invertedRadixTree.put("org.apache", true);
        invertedRadixTree.put("io.lettuce", true);
        invertedRadixTree.put("com.querydsl", true);
        invertedRadixTree.put("org.hibernate", true);
        invertedRadixTree.put("org.jgrapht", true);
        invertedRadixTree.put("ch.qos", true);
        invertedRadixTree.put("io.dropwizard", true);
        invertedRadixTree.put("org.redis", true);
        invertedRadixTree.put("redis", true);
        invertedRadixTree.put("co.elastic", true);
        invertedRadixTree.put("io.unlogged", true);
        invertedRadixTree.put("com.insidious", true);
        invertedRadixTree.put("java.awt", true);
        invertedRadixTree.put("sun.nio", true);
        invertedRadixTree.put("javax.swing", true);
        invertedRadixTree.put("com.j256", true);
        invertedRadixTree.put("net.openhft", true);
        invertedRadixTree.put("com.intellij", true);
        invertedRadixTree.put("java.lang.Class", true);
//        invertedRadixTree.put("reactor.core.publisher.Flux", true);
//        invertedRadixTree.put("reactor.core.publisher.Mono", true);
        invertedRadixTree.put("reactor.util", true);
        invertedRadixTree.put("io.undertow", true);
        invertedRadixTree.put("org.thymeleaf", true);
        invertedRadixTree.put("tech.jhipster", true);
        invertedRadixTree.put("com.github", true);
        invertedRadixTree.put("com.zaxxer", true);
        invertedRadixTree.put("com.fasterxml", true);
        invertedRadixTree.put("org.slf4j", true);
        invertedRadixTree.put("java.io", true);
        invertedRadixTree.put("java.util.regex", true);
        invertedRadixTree.put("java.util.Base64", true);
//        invertedRadixTree.put("java.util.concurrent", true);
        invertedRadixTree.put("com.amazon", true);
        invertedRadixTree.put("com.hubspot", true);
    }

    /**
     * Close all file streams used by the object.
     */
    public void close() {
//        System.out.printf("[videobug] close event stream aggregated logger\n");
        objectIdMap.close();
        try {
            aggregatedLogger.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object getObjectByClassName(String className) {
        if (!objectMap.containsKey(className)) {
            return null;
        }
        WeakReference<Object> objectWeakReference = objectMap.get(className);
        Object objectInstance = objectWeakReference.get();
        if (objectInstance == null) {
            objectMap.remove(className);
            return null;
        }
        if (targetClassLoader == null) {
            targetClassLoader = objectInstance.getClass().getClassLoader();
        }
        return objectInstance;
    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public Object recordEvent(final int dataId, Object value) {
        if (isRecording.get()) {
            return value;
        }
        String className;
        Class<?> valueClass = value == null ? Object.class : value.getClass();
        if (value != null) {
            className = valueClass.getCanonicalName();
            if (className == null) {
                className = valueClass.getName();
            }
        } else {
            className = "";
        }
        if (!className.contains("Lambda") && !className.contains("$Unlogged")) {
//            System.err.println("Object instance: " + className);
            String originalClassName = className;
            if (className.contains("_$")) {
                className = className.substring(0, className.indexOf("_$"));
            } else if (className.contains("$")) {
                className = className.substring(0, className.indexOf('$'));
            }
            if (!objectMap.containsKey(className)) {
                objectMap.put(className, new WeakReference<>(value));
            } else if (originalClassName.contains("$EnhancerBySpringCGLIB")) {
                objectMap.put(className, new WeakReference<>(value));
            } else if (originalClassName.contains("$SpringCGLIB")) {
                objectMap.put(className, new WeakReference<>(value));
            }
            if (targetClassLoader == null && value != null) {
                targetClassLoader = valueClass.getClassLoader();
            }
        }


        long objectId = objectIdMap.getId(value);

        if (serializeValues && probesToRecord.size() > 0 && probesToRecord.contains(dataId) && !valueToSkip.contains(
                objectId)) {

            if (DEBUG && value != null) {
                System.out.println("record serialized value for probe [" + dataId + "] -> " + valueClass);
            }


            // write data into OutputStream
            byte[] bytes = new byte[0];
            try {
                isRecording.set(true);


//                if (className == null) {
//                    System.err.println("Class name is null: " + value + " - " + value.getClass());
//                }

//                if (value != null) {
//                    System.out.println("[" + dataId + "] Serialize class: " + value.getClass().getName());
//                }
                if (value instanceof Class) {
                    bytes = ((Class<?>) value).getCanonicalName().getBytes(StandardCharsets.UTF_8);
                } else if (
                        invertedRadixTree.getKeysPrefixing(className).iterator().hasNext()
                                || className.contains("java.lang.reflect")
                                || className.contains("reactor.core.scheduler")
                                || className.contains("com.mongodb")
                                || (className.startsWith("org.glassfish")
                                && !className.equals("org.glassfish.jersey.message.internal.OutboundJaxrsResponse"))
                                || (className.startsWith("org.springframework")
                                && (!className.startsWith("org.springframework.http")
                                && !className.startsWith("org.springframework.data.domain")))
                                || value instanceof Iterator
                                || value instanceof Stream
                ) {
//                    System.err.println("Removing probe["+ className +"]: " + dataId);
                    probesToRecord.remove(dataId);
                } else if (SERIALIZATION_MODE == SerializationMode.JACKSON) {

//                    if (className.startsWith("reactor.core")) {
//                    }
//                    objectMapper.writeValue(outputStream, value);
//                    outputStream.flush();
//                    bytes = outputStream.toByteArray();
                    if (isReactive && value instanceof Mono) {
                        final long newValueId = System.nanoTime();
                        Mono<?> value1 = (Mono<?>) value;
                        buffer.clear();
                        buffer.putLong(newValueId);
                        aggregatedLogger.writeEvent(dataId, objectId, buffer.array());
                        final Integer firstProbeIdFinal = firstProbeId.get(dataId);
//                        System.err.println("SubscribeToMono ["+dataId+"] [" + objectId + "] => " + newValueId + " => " + firstProbeIdFinal);

                        return value1
                                .doOnError((result) -> {
                                    try {
                                        byte[] bytesAllocatedNew = objectMapper.get().writeValueAsBytes(result);
//                                System.err.println(
//                                        "Async doOnError[" + objectId + "]: " + firstProbeIdFinal + " == " + new String(
//                                                bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    } catch (JsonProcessingException e) {
                                        //
                                        byte[] bytesAllocatedNew = result.toString().getBytes(StandardCharsets.UTF_8);
//                                System.err.println("AsyncReal doOnErrorReal[" + objectId + "]: " + firstProbeIdFinal +
//                                        " " +
//                                        "== " + new String(bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    }

                                })
                                .doOnNext((result) -> {
                                    try {
                                        byte[] bytesAllocatedNew = objectMapper.get().writeValueAsBytes(result);
//                                        System.err.println(
//                                                "Async doOnSuccess[" + objectId + "]: " + firstProbeIdFinal + " == " + new String(
//                                                        bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    } catch (JsonProcessingException e) {
                                        //
                                        byte[] bytesAllocatedNew = result.toString().getBytes(StandardCharsets.UTF_8);
//                                        System.err.println("Async doOnSuccessReal[" + objectId + "]: " + firstProbeIdFinal +
//                                                " == " + new String(bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    }

                                });
//                        return value1;
                    } else if (isReactive && value instanceof Flux) {
                        final long newValueId = System.nanoTime();
                        Flux<?> fluxValue = (Flux<?>) value;
                        buffer.clear();
                        buffer.putLong(newValueId);
                        aggregatedLogger.writeEvent(dataId, objectId, buffer.array());
                        final Integer firstProbeIdFinal = firstProbeId.get(dataId);
//                        System.err.println(
//                                "SubscribeToFlux [" + dataId + "] [" + objectId + "] => " + newValueId + " => " + firstProbeIdFinal);

                        return fluxValue
                                .doOnError((result) -> {
                                    try {
                                        byte[] bytesAllocatedNew = objectMapper.get().writeValueAsBytes(result);
//                                        System.err.println(
//                                                "Async doOnError[" + objectId + "][" + dataId + "]: " + firstProbeIdFinal + " == "
//                                                        + new String(bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    } catch (JsonProcessingException e) {
                                        //
                                        byte[] bytesAllocatedNew = result.toString().getBytes(StandardCharsets.UTF_8);
//                                        System.err.println(
//                                                "AsyncReal doOnErrorReal[" + objectId + "]: " + firstProbeIdFinal +
//                                                        " " +
//                                                        "== " + new String(bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    }

                                })
                                .doOnNext((result) -> {
                                    try {
                                        byte[] bytesAllocatedNew = objectMapper.get().writeValueAsBytes(result);
//                                        System.err.println(
//                                                "Async doOnSuccess[" + objectId + "][" + dataId + "]: "
//                                                        + firstProbeIdFinal +
//                                                        " == " + new String(
//                                                        bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    } catch (JsonProcessingException e) {
                                        //
                                        byte[] bytesAllocatedNew = result.toString().getBytes(StandardCharsets.UTF_8);
//                                        System.err.println(
//                                                "Async doOnSuccessReal[" + objectId + "]: " + firstProbeIdFinal +
//                                                        " == " + new String(bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    }

                                });
//                        return fluxValue;
                    } else if (value instanceof Future) {
                        Future<?> futureValue = (Future<?>) value;
                        try {
                            Object value1 = futureValue.get(100, TimeUnit.MILLISECONDS);
                            bytes = objectMapper.get().writeValueAsBytes(value1);
                        } catch (TimeoutException te) {
                            bytes = objectMapper.get().writeValueAsBytes("{\"message\": \"failed to read future\"}");
                        }
                    } else if (value instanceof byte[]) {
                        bytes = (byte[]) value;
                    } else {
                        bytes = objectMapper.get().writeValueAsBytes(value);
                    }
                    if (DEBUG) {
                        System.err.println(
                                "[" + dataId + "] record serialized value for probe [" + valueClass + "] [" + objectId + "] ->" +
                                        " " + new String(bytes));
                    }
                    // ######################################
                } else if (SERIALIZATION_MODE == SerializationMode.FST) {
//                    objectMapper.writeValue(outputStream, value);
//                    outputStream.flush();
//                    bytes = outputStream.toByteArray();
//                    bytes = fstObjectMapper.asByteArray(value);
//                    if (bytes.length > 10000) {
//                        probesToRecord.remove(dataId);
//                        bytes = new byte[0];
//                    }
//                    System.err.println(
//                            "[" + dataId + "] record serialized value for probe [" + value.getClass() + "] [" + objectId + "] ->" +
//                                    " " + bytes.length + " : " + Base64.getEncoder().encodeToString(bytes));
                    // ######################################
                } else if (SERIALIZATION_MODE == SerializationMode.OOS) {
                    // ################# USING OOS #####################
                    // # using ObjectOutputStream
                    //                System.out.println("Registration " + registration.toString() + " - ");
//                    ObjectOutputStream oos = new ObjectOutputStream(out);
//                    serializer.serialize(out2, value);
//                    oos.writeObject(value);
                    // ######################################

                } else if (SERIALIZATION_MODE == SerializationMode.KRYO) {
                    // # using kryo
                    // ################ USING KRYO ######################
//                    Output output = outputContainer.get();
//                    ByteArrayOutputStream buffer = (ByteArrayOutputStream) output.getOutputStream();
//                    output.reset();
//                    kryo.writeObject(output, value);
//                    output.flush();
//                    bytes = output.toBytes();
                    // ######################################
                }


            } catch (Throwable e) {
                if (e instanceof JsonMappingException) {
                    bytes = (FAILED_TO_RECORD_MESSAGE + e.getMessage() + "\"}").getBytes();
                }
                probesToRecord.remove(dataId);
                valueToSkip.add(objectId);
//                if (value != null) {
//                    kryo.register(value.getClass());
//                    String message = e.getMessage();
//                System.err.println("ThrowSerialized [" + value.getClass().getCanonicalName() + "]" +
//                        " [" + dataId + "] error -> " + e.getMessage() + " -> " + e.getClass().getCanonicalName());
//                e.printStackTrace();
//                    if (message.startsWith("Class is not registered")) {
//                        String className = message.split(":")[1];
//                        try {
//                            Class<?> classType = Class.forName(className);
//                            kryo.register(classType);
//                        } catch (ClassNotFoundException ex) {
////                            ex.printStackTrace();
//                        }
//                    }
//                e.printStackTrace();
//                }
                // ignore if we cannot record the variable information
            } finally {
                isRecording.set(false);
            }
            aggregatedLogger.writeEvent(dataId, objectId, bytes);
//            outputStream.reset();
        } else {
//            System.err.println("No serialization for: " + dataId);
            aggregatedLogger.writeEvent(dataId, objectId);
        }
        return value;
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value.
     */
    public void recordEvent(int dataId, int value) {
        if (isRecording.get()) {
            return;
        }

//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     */
    public void recordEvent(int dataId, long value) {
        if (isRecording.get()) {
            return;
        }

//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value.
     */
    public void recordEvent(int dataId, byte value) {
        if (isRecording.get()) {
            return;
        }

//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value.
     */
    public void recordEvent(int dataId, short value) {
        if (isRecording.get()) {
            return;
        }

//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value.
     */
    public void recordEvent(int dataId, char value) {
        if (isRecording.get()) {
            return;
        }

//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value (true = 1, false = 0).
     */
    public void recordEvent(int dataId, boolean value) {
        if (isRecording.get()) {
            return;
        }

        int longValue = value ? 1 : 0;
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, longValue);
        aggregatedLogger.writeEvent(dataId, longValue);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value preserving the information.
     */
    public void recordEvent(int dataId, double value) {
        if (isRecording.get()) {
            return;
        }

        long longValue = Double.doubleToRawLongBits(value);
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, longValue);
        aggregatedLogger.writeEvent(dataId, longValue);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value preserving the information.
     */
    public void recordEvent(int dataId, float value) {
        if (isRecording.get()) {
            return;
        }

        int longValue = Float.floatToRawIntBits(value);
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, longValue);
        aggregatedLogger.writeEvent(dataId, longValue);
    }

    @Override
    public void recordWeaveInfo(byte[] byteArray, ClassInfo classIdEntry, List<Integer> probeIdsToRecord) {
        if (probeIdsToRecord.size() > 0) {
            Integer firstProbeIdThisBatch = probeIdsToRecord.get(0);
            for (Integer i : probeIdsToRecord) {
                firstProbeId.put(i, firstProbeIdThisBatch);
            }

            probesToRecord.addAll(probeIdsToRecord);
        }
        aggregatedLogger.writeWeaveInfo(byteArray);
    }

    @Override
    public void setRecordingPaused(boolean b) {
        isRecording.set(b);
    }


    @Override
    public ClassLoader getTargetClassLoader() {
        return targetClassLoader;
    }

    @Override
    public void registerClass(Integer id, Class<?> type) {
    }

	@Override
	public void modifyThreadDepth(long delta) {
		aggregatedLogger.modifyThreadDepth(delta);
	}
}
