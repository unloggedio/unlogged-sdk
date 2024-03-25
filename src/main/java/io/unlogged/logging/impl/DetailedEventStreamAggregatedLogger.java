package io.unlogged.logging.impl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import com.insidious.common.weaver.ClassInfo;
import io.unlogged.logging.IEventLogger;
import io.unlogged.logging.SerializationMode;
import io.unlogged.logging.util.AggregatedFileLogger;
import io.unlogged.logging.util.ObjectIdAggregatedStream;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Stream;


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
    public static final Duration ONE_MILLISECOND = Duration.ofMillis(1);
    private final List<String> JACKSON_PROPERTY_NAMES_SET_FALSE = Arrays.asList(
            "FAIL_ON_UNKNOWN_PROPERTIES",
            "FAIL_ON_IGNORED_PROPERTIES",
            "FAIL_ON_NULL_FOR_PRIMITIVES",
            "FAIL_ON_NULL_CREATOR_PROPERTIES",
            "FAIL_ON_MISSING_CREATOR_PROPERTIES",
            "FAIL_ON_NUMBERS_FOR_ENUMS",
            "FAIL_ON_TRAILING_TOKENS"
    );
    //    private final FSTConfiguration fstObjectMapper;
    private final AggregatedFileLogger aggregatedLogger;
    //    private final TypeIdAggregatedStreamMap typeToId;
    private final ObjectIdAggregatedStream objectIdMap;
    //    private final String includedPackage;
    private final Boolean DEBUG = Boolean.parseBoolean(System.getProperty("UNLOGGED_DEBUG"));
    //    private final ThreadLocal<ByteArrayOutputStream> threadOutputBuffer =
//            ThreadLocal.withInitial(ByteArrayOutputStream::new);
    private final ThreadLocal<Boolean> isRecording = ThreadLocal.withInitial(() -> false);
    final private boolean serializeValues = true;
    //    private final Map<String, WeaveLog> classMap = new HashMap<>();
//    private final Map<Integer, DataInfo> callProbes = new HashMap<>();
    private final Set<Integer> probesToRecord = new HashSet<>();
    private final Set<Long> valueToSkip = new HashSet<>();
    private final SerializationMode SERIALIZATION_MODE = SerializationMode.JACKSON;
    private final ThreadLocal<ByteArrayOutputStream> output =
            ThreadLocal.withInitial(() -> new ByteArrayOutputStream(1_000_000));
    //    private final Set<String> classesToIgnore = new HashSet<>();
//    private final Kryo kryo;
    private final Map<String, WeakReference<Object>> objectMap = new HashMap<>();
    InvertedRadixTree<Boolean> invertedRadixTree = new ConcurrentInvertedRadixTree<>(
            new DefaultCharArrayNodeFactory());
    private boolean isLombokPresent;
    private ClassLoader targetClassLoader;
    private final ThreadLocal<ObjectMapper> objectMapper = ThreadLocal.withInitial(() -> {
        String jacksonVersion = ObjectMapper.class.getPackage().getImplementationVersion();
        if (jacksonVersion != null && (jacksonVersion.startsWith("2.9") || jacksonVersion.startsWith("2.8"))) {
            ObjectMapper objectMapper1 = new ObjectMapper();

            try {
                JsonMappingException jme = new JsonMappingException(new DummyClosable(), "load class");
                jme.prependPath(new JsonMappingException.Reference("from dummy"));
            } catch (Exception e) {
                //
            }

            for (DeserializationFeature value : DeserializationFeature.values()) {
                if (JACKSON_PROPERTY_NAMES_SET_FALSE.contains(value.name())) {
                    objectMapper1.configure(value, false);
                }
            }
            objectMapper1.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
            objectMapper1.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

            // potentially
//            jacksonBuilder.findAndAddModules();
            List<String> jacksonModules = Arrays.asList(
                    "com.fasterxml.jackson.datatype.jdk8.Jdk8Module",
                    "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
                    "com.fasterxml.jackson.datatype.joda.JodaModule",
//                        "com.fasterxml.jackson.module.blackbird.BlackbirdModule",
                    "com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule",
                    "com.fasterxml.jackson.module.mrbean.MrBeanModule",
//                        "com.fasterxml.jackson.module.afterburner.AfterburnerModule",
                    "com.fasterxml.jackson.module.paranamer.ParanamerModule",
                    "software.fitz.jackson.module.force.ForceDeserializerModule"
            );
            for (String jacksonModule : jacksonModules) {
                try {
                    //checks for presence of this module class, if not present throws exception
                    Class<?> jdk8Module = Class.forName(jacksonModule);
                    objectMapper1.registerModule((Module) jdk8Module.getDeclaredConstructor().newInstance());
                } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                    // jdk8 module not found
                } catch (InvocationTargetException
                         | InstantiationException
                         | IllegalAccessException
                         | NoSuchMethodException e) {
                    // do not die
//                    throw new RuntimeException(e);
                }
            }

//            try {
//                Class<?> kotlinModuleClass = Class.forName("com.fasterxml.jackson.module.kotlin.KotlinModule");
//                KotlinModule kotlinModule = new KotlinModule.Builder().build();
//                objectMapper1.registerModule(kotlinModule);
//            } catch (ClassNotFoundException e) {
//                // kotlin module for jackson not present on classpath
//            }


            return objectMapper1;
        } else {
            // For 2.13.1
            // Load JsonMappingException class force load so that we don't get a StackOverflow when we are in a cycle
            JsonMappingException jme = new JsonMappingException(new DummyClosable(), "load class");
            jme.prependPath(new JsonMappingException.Reference("from dummy"));
            JsonMapper.Builder jacksonBuilder = JsonMapper.builder();

            for (DeserializationFeature value : DeserializationFeature.values()) {
                if (JACKSON_PROPERTY_NAMES_SET_FALSE.contains(value.name())) {
                    jacksonBuilder.configure(value, false);
                }
            }

            jacksonBuilder.annotationIntrospector(new JacksonAnnotationIntrospector() {
                @Override
                public boolean hasIgnoreMarker(AnnotatedMember m) {
                    String fullName = m.getFullName();
                    if (m.getDeclaringClass().getCanonicalName().contains("_$$_")) {
                        return true;
                    }
                    String rawTypeCanonicalName = m.getRawType().getCanonicalName();
                    if (rawTypeCanonicalName.equals("javassist.util.proxy.MethodHandler")) {
                        return true;
                    }
                    if (fullName.contains(".$Proxy")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.startsWith("java.lang.Thread")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.startsWith("java.util.function.")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.startsWith("java.lang.reflect.")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.startsWith("jdk.internal.reflect.")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.startsWith("io.mongock.")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.startsWith("sun.reflect.")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.equals("sun.nio.ch.Interruptible")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.equals("java.security.AccessControlContext")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.equals("java.lang.ClassLoader")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.equals("java.lang.Runnable")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.startsWith("reactor.core.")) {
                        return true;
                    }
                    if (fullName.startsWith("reactor.")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.startsWith("io.netty.resolver")) {
                        return true;
                    }
                    if (rawTypeCanonicalName.startsWith("org.reactivestreams.")) {
                        return true;
                    }
//                    System.out.println("hasIgnoreMarker: " + m.getFullName() + " => " + rawTypeCanonicalName);
                    return false;
                }

                @Override
                public Object findSerializer(Annotated a) {
                    if (Objects.equals(a.getRawType(), Date.class)) {
                        return null;
                    }
                    return super.findSerializer(a);
                }

                @Override
                public JsonPOJOBuilder.Value findPOJOBuilderConfig(AnnotatedClass ac) {
//                    System.err.println("Find POJO builder config: " + ac.getName());
                    if (ac.hasAnnotation(
                            JsonPOJOBuilder.class)) {//If no annotation present use default as empty prefix
                        return super.findPOJOBuilderConfig(ac);
                    }
                    return new JsonPOJOBuilder.Value("build", "");
                }

                @Override
                public Class<?> findPOJOBuilder(AnnotatedClass ac) {
//                    System.err.println("Find POJO builder: " + ac.getName());

//                    if (isLombokPresent) {
//                        System.err.println("Annotation found: " + ac.hasAnnotation(lombokBuilderAnnotation));
//                    }

                    if (ac.getRawType().getCanonicalName().startsWith("java.")) {
                        return null;
                    }

                    if (isLombokPresent) {
                        try {
                            String classFullyQualifiedName = ac.getName();
                            String classSimpleName = classFullyQualifiedName.substring(
                                    classFullyQualifiedName.lastIndexOf(".") + 1);
                            String lombokClassBuilderName = ac.getName() + "$" + classSimpleName + "Builder";
//                            System.err.println("Lookup builder by nameclean: " + lombokClassBuilderName);
                            if (ac.getRawType().getClassLoader() != null) {
                                return ac.getRawType().getClassLoader().loadClass(lombokClassBuilderName);
                            } else {
                                return targetClassLoader.loadClass(lombokClassBuilderName);
                            }
                        } catch (ClassNotFoundException e) {
                            return super.findPOJOBuilder(ac);
                        }
                    }
                    return super.findPOJOBuilder(ac);
                }
            });
            DateFormat df = new SimpleDateFormat("MMM d, yyyy HH:mm:ss aaa");
            jacksonBuilder.defaultDateFormat(df);
            jacksonBuilder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            jacksonBuilder.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

            try {
                Field fieldWriteSelfReferencesAsNull = SerializationFeature.class.getDeclaredField(
                        "WRITE_SELF_REFERENCES_AS_NULL");
                // field found
                jacksonBuilder.configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true);
            } catch (NoSuchFieldException e) {
                // no field WRITE_SELF_REFERENCES_AS_NULL
            }


            try {
//                Class.forName("javax.persistence.ElementCollection");
                Class<?> hibernateClassPresent = Class.forName("org.hibernate.SessionFactory");
                Class<?> hibernateModule = Class.forName(
                        "com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module");
                Module module = (Module) hibernateModule.getDeclaredConstructor().newInstance();
                Class<?> featureClass = Class.forName(
                        "com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module$Feature");
                Method configureMethod = hibernateModule.getMethod("configure", featureClass, boolean.class);
                configureMethod.invoke(module, featureClass.getDeclaredField("FORCE_LAZY_LOADING").get(null), true);
                configureMethod.invoke(module,
                        featureClass.getDeclaredField("REPLACE_PERSISTENT_COLLECTIONS").get(null), true);
                configureMethod.invoke(module, featureClass.getDeclaredField("USE_TRANSIENT_ANNOTATION").get(null),
                        false);
                jacksonBuilder.addModule(module);
//                System.out.println("Loaded hibernate module");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
//                e.printStackTrace();
//                System.out.println("Failed to load hibernate module: " + e.getMessage());
                // hibernate module not found
                // add a warning in System.err here ?
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchFieldException e) {
                throw new RuntimeException(e);
            }

            // potentially
//            jacksonBuilder.findAndAddModules();
            List<String> jacksonModules = Arrays.asList(
                    "com.fasterxml.jackson.datatype.jdk8.Jdk8Module",
                    "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
                    "com.fasterxml.jackson.datatype.joda.JodaModule",
//                        "com.fasterxml.jackson.module.blackbird.BlackbirdModule",
                    "com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule",
                    "com.fasterxml.jackson.module.mrbean.MrBeanModule",
//                        "com.fasterxml.jackson.module.afterburner.AfterburnerModule",
                    "com.fasterxml.jackson.module.paranamer.ParanamerModule",
                    "software.fitz.jackson.module.force.ForceDeserializerModule"
            );
            for (String jacksonModule : jacksonModules) {
                try {
                    //checks for presence of this module class, if not present throws exception
                    Class<?> jdk8Module = Class.forName(jacksonModule);
                    jacksonBuilder.addModule((Module) jdk8Module.getDeclaredConstructor().newInstance());
                } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                    // jdk8 module not found
                } catch (InvocationTargetException
                         | InstantiationException
                         | IllegalAccessException
                         | NoSuchMethodException e) {
                    // do not die ?
//                    throw new RuntimeException(e);
                }
            }

//            try {
//                Class<?> kotlinModuleClass = Class.forName("com.fasterxml.jackson.module.kotlin.KotlinModule");
//                KotlinModule kotlinModule = new KotlinModule.Builder().build();
//                jacksonBuilder.addModule(kotlinModule);
//            } catch (ClassNotFoundException e) {
//                // kotlin module for jackson not present on classpath
//            }


            JsonMapper objectMapperInstance = jacksonBuilder.build();
            objectMapperInstance.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                    .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            return objectMapperInstance;
        }
    });
    private Map<Integer, Integer> firstProbeId = new HashMap<>();

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
            Class<?> lombokBuilderAnnotation = Class.forName("lombok.Builder");
            isLombokPresent = true;
//            System.err.println("Lombok found: " + lombokBuilderAnnotation.getCanonicalName());
        } catch (ClassNotFoundException e) {
//            System.err.println("Lombok not found");
            isLombokPresent = false;
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
//        invertedRadixTree.put("reactor.core", true);
        invertedRadixTree.put("reactor.core.publisher.Flux", true);
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
    public Object recordEvent(int dataId, Object value) {
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


        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
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
                                || value instanceof Flux
                ) {
//                    System.err.println("Removing probe["+ className +"]: " + dataId);
                    probesToRecord.remove(dataId);
                } else if (SERIALIZATION_MODE == SerializationMode.JACKSON) {

//                    if (className.startsWith("reactor.core")) {
//                    }
//                    objectMapper.writeValue(outputStream, value);
//                    outputStream.flush();
//                    bytes = outputStream.toByteArray();
                    if (className.startsWith("reactor.core.publisher.Mono")) {
                        final long newValueId = System.nanoTime();
                        Mono<?> value1 = (Mono<?>) value;
                        buffer.clear();
                        buffer.putLong(newValueId);
                        aggregatedLogger.writeEvent(dataId, objectId, buffer.array());
                        final Integer firstProbeIdFinal = firstProbeId.get(dataId);
                        System.err.println("SubscribeToMono ["+dataId+"] [" + objectId + "] => " + newValueId + " => " + firstProbeIdFinal);

                        value1
                                .doOnError((result) -> {
                                    try {
                                        byte[] bytesAllocatedNew = objectMapper.get().writeValueAsBytes(result);
                                System.err.println(
                                        "Async doOnError[" + objectId + "]: " + firstProbeIdFinal + " == " + new String(
                                                bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    } catch (JsonProcessingException e) {
                                        //
                                        byte[] bytesAllocatedNew = result.toString().getBytes(StandardCharsets.UTF_8);
                                System.err.println("AsyncReal doOnErrorReal[" + objectId + "]: " + firstProbeIdFinal +
                                        " " +
                                        "== " + new String(bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    }

                                })
                                .doOnNext((result) -> {
                                    try {
                                        byte[] bytesAllocatedNew = objectMapper.get().writeValueAsBytes(result);
                                        System.err.println(
                                                "Async doOnSuccess[" + objectId + "]: " + firstProbeIdFinal + " == " + new String(
                                                        bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    } catch (JsonProcessingException e) {
                                        //
                                        byte[] bytesAllocatedNew = result.toString().getBytes(StandardCharsets.UTF_8);
                                        System.err.println("Async doOnSuccessReal[" + objectId + "]: " + firstProbeIdFinal +
                                                " == " + new String(bytesAllocatedNew) + " => " + newValueId);
                                        aggregatedLogger.writeEvent(firstProbeIdFinal, newValueId, bytesAllocatedNew);
                                    }

                                }).subscribe();
                        return value1;
                    } else if (value instanceof Future) {
                        Future<?> futureValue = (Future<?>) value;
                        bytes = objectMapper.get().writeValueAsBytes(futureValue.get());
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
    public void setRecording(boolean b) {
        isRecording.set(b);
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper.get();
    }

    @Override
    public ClassLoader getTargetClassLoader() {
        return targetClassLoader;
    }

    @Override
    public void registerClass(Integer id, Class<?> type) {
    }

    private static class DummyClosable implements Closeable {

        @Override
        public void close() throws IOException {

        }
    }


}
