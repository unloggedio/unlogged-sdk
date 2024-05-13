package io.unlogged.logging;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
//import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class ObjectMapperFactory {
    private static final List<String> JACKSON_PROPERTY_NAMES_SET_FALSE = Arrays.asList(
            "FAIL_ON_UNKNOWN_PROPERTIES",
            "FAIL_ON_IGNORED_PROPERTIES",
            "FAIL_ON_NULL_FOR_PRIMITIVES",
            "FAIL_ON_NULL_CREATOR_PROPERTIES",
            "FAIL_ON_MISSING_CREATOR_PROPERTIES",
            "FAIL_ON_NUMBERS_FOR_ENUMS",
            "FAIL_ON_TRAILING_TOKENS"
    );
    private static boolean isLombokPresent;

    private static boolean isReactivePresent = false;

    static {
        try {
            Class<?> lombokBuilderAnnotation = Class.forName("lombok.Builder");
            isLombokPresent = true;
//            System.err.println("Lombok found: " + lombokBuilderAnnotation.getCanonicalName());
        } catch (ClassNotFoundException e) {
//            System.err.println("Lombok not found");
            isLombokPresent = false;
        }
        try {
            Class<?> lombokBuilderAnnotation = Class.forName("reactor.core.publisher.Mono");
            isReactivePresent = true;
//            System.err.println("Lombok found: " + lombokBuilderAnnotation.getCanonicalName());
        } catch (ClassNotFoundException e) {
//            System.err.println("Lombok not found");
            isReactivePresent = false;
        }

    }

    public static ObjectMapper createObjectMapper() {
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
                    objectMapper1.registerModule(
                            (com.fasterxml.jackson.databind.Module) jdk8Module.getDeclaredConstructor().newInstance());
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
            JsonMappingException jme = new JsonMappingException(new DummyClosable(),
                    "load class");
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
                                return this.getClass().getClassLoader().loadClass(lombokClassBuilderName);
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
                com.fasterxml.jackson.databind.Module module = (com.fasterxml.jackson.databind.Module) hibernateModule.getDeclaredConstructor()
                        .newInstance();
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

    }

    public static ObjectMapper createObjectMapperReactive() {
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

            if (isReactivePresent) {
                objectMapper1.registerModule(new ReactiveModule());
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
                    objectMapper1.registerModule(
                            (com.fasterxml.jackson.databind.Module) jdk8Module.getDeclaredConstructor().newInstance());
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
            JsonMappingException jme = new JsonMappingException(new DummyClosable(),
                    "load class");
            jme.prependPath(new JsonMappingException.Reference("from dummy"));
            JsonMapper.Builder jacksonBuilder = JsonMapper.builder();

            for (DeserializationFeature value : DeserializationFeature.values()) {
                if (JACKSON_PROPERTY_NAMES_SET_FALSE.contains(value.name())) {
                    jacksonBuilder.configure(value, false);
                }
            }

            if (isReactivePresent) {
                jacksonBuilder.addModule(new ReactiveModule());
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
                                return this.getClass().getClassLoader().loadClass(lombokClassBuilderName);
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
                com.fasterxml.jackson.databind.Module module = (com.fasterxml.jackson.databind.Module) hibernateModule.getDeclaredConstructor()
                        .newInstance();
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

    }

    private static class DummyClosable implements Closeable {

        @Override
        public void close() throws IOException {

        }
    }

    static class ReactiveModule extends SimpleModule {
        ReactiveModule() {
            addSerializer(Mono.class, new MonoSerializer());
            addSerializer(Flux.class, new FluxSerializer());
        }
    }

    public static class MonoSerializer extends JsonSerializer<Mono> {
        @Override
        public void serialize(Mono mono, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeObject(mono.block()); // Blocks until the Mono emits a value
        }
    }

    public static class FluxSerializer extends JsonSerializer<Flux> {
        @Override
        public void serialize(Flux flux, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartArray(); // Start writing JSON array
            flux.toIterable().forEach(element -> {
                try {
                    jsonGenerator.writeObject(element); // Write each element of Flux
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            jsonGenerator.writeEndArray(); // End JSON array
        }
    }
}
