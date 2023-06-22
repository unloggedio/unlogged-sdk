package io.unlogged.core.bytecode;


import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.LogLevel;
import com.insidious.common.weaver.MethodInfo;
import io.unlogged.weaver.TypeHierarchy;
import io.unlogged.weaver.WeaveLog;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * This class manages bytecode injection process and weaving logs.
 */
public class Weaver {

//    public static final String PROPERTY_FILE = "weaving.properties";
    public static final String SEPARATOR = ",";
//    public static final char SEPARATOR_CHAR = ',';
//    public static final String CLASS_ID_FILE = "classes.txt";
//    public static final String METHOD_ID_FILE = "methods.txt";
//    public static final String DATA_ID_FILE = "dataids.txt";
//    public static final String ERROR_LOG_FILE = "log.txt";

    public static final String CATEGORY_WOVEN_CLASSES = "woven-classes";
    public static final String CATEGORY_ERROR_CLASSES = "error-classes";

    //    private final File outputDir;
    private final String lineSeparator = "\n";
    private final WeaveConfig config;
    private final TypeHierarchy typeHierarchy;
    private Writer dataIdWriter;
    private PrintStream logger;
    private int classId;
    private int confirmedDataId;
    private int confirmedMethodId;
    private Writer methodIdWriter;
    private boolean dumpOption;
    private MessageDigest digest;

    /**
     * Set up the object to manage a weaving process.
     * This constructor creates files to store the information.
     * <p>
     * //     * @param outputDir location to save the weave data
     *
     * @param config weave configuration
     */
    public Weaver(WeaveConfig config, TypeHierarchy typeHierarchy) {
//        assert outputDir.isDirectory() && outputDir.canWrite();

        this.typeHierarchy = typeHierarchy;
//        this.outputDir = outputDir;
        this.config = config;
        confirmedDataId = 0;
        confirmedMethodId = 0;
        classId = 0;

//        try {
//            logger = new PrintStream(new File(outputDir, ERROR_LOG_FILE));
//        } catch (FileNotFoundException e) {
        logger = System.out;
//            logger.println("[unlogged] failed to open " + ERROR_LOG_FILE + " in " + outputDir.getAbsolutePath());
//            logger.println("[unlogged] using System.out instead.");
//        }

        String agentVersion = "1.0.1";

//        logger.printf("Java version: %s%n", System.getProperty("java.version"));
//        logger.printf("Agent version: %s%n", agentVersion);

        try {
            this.digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            this.digest = null;
        }

    }

    /**
     * Set the bytecode dump option.
     *
     * @param dump If true is set, the weaver writes the woven class files to the output directory.
     */
    public void setDumpEnabled(boolean dump) {
        this.dumpOption = dump;
    }


    /**
     * Execute bytecode injection for a given class.
     *
     * @param container specifies a location (e.g. a Jar file path) where a class is loaded.
     * @param className specifies a class file path.
     * @param target    is the content of the class.
     * @return a byte array containing the woven class.  This method returns null if an error occurred.
     */
    public InstrumentedClass weave(String container, String className, byte[] target) throws IOException {
        assert container != null;

        String hash = getClassHash(target);
        LogLevel level = LogLevel.Normal;
        WeaveLog weaveLog = new WeaveLog(classId, confirmedMethodId, confirmedDataId);
//        try {
        ClassTransformer classTransformer;
        try {
            classTransformer = new ClassTransformer(weaveLog, config, target, typeHierarchy);
        } catch (RuntimeException e) {
            if ("Method code too large!".equals(e.getMessage())) {
                // Retry to generate a smaller bytecode by ignoring a large array init block
                try {
                    weaveLog = new WeaveLog(classId, confirmedMethodId, confirmedDataId);
                    level = LogLevel.IgnoreArrayInitializer;
                    WeaveConfig smallerConfig = new WeaveConfig(config, level);
                    classTransformer = new ClassTransformer(weaveLog, smallerConfig, target, typeHierarchy);
//                    log("LogLevel.IgnoreArrayInitializer: " + container + "/" + className);
                } catch (RuntimeException e2) {
                    if ("Method code too large!".equals(e.getMessage())) {
                        weaveLog = new WeaveLog(classId, confirmedMethodId, confirmedDataId);
                        // Retry to generate further smaller bytecode by ignoring except for entry and exit events
                        level = LogLevel.OnlyEntryExit;
                        WeaveConfig smallestConfig = new WeaveConfig(config, level);
                        classTransformer = new ClassTransformer(weaveLog, smallestConfig, target, typeHierarchy);
//                        log("LogLevel.OnlyEntryExit: " + container + "/" + className);
                    } else {
                        // this jumps to catch (Throwable e) in this method
//                        log("Failed to weave class: " + className);
//                        log(e);
                        return new InstrumentedClass(target, new byte[0]);
//                            throw e2;
                    }
                }
            } else {
//                log("Failed to weave class: " + className);
//                log(e);
                return new InstrumentedClass(target, new byte[0]);
                // this jumps to catch (Throwable e) in this method
//                    throw e;
            }
        }

        ClassInfo classIdEntry
                = new ClassInfo(
                classId, container, classTransformer.getSourceFileName(),
                weaveLog.getFullClassName(), level, hash,
                classTransformer.getClassLoaderIdentifier(),
                classTransformer.getInterfaces(),
                classTransformer.getSuperName(),
                classTransformer.getSignature()
        );

//            System.err.println("New Class [" + classIdEntry.getClassId() + "] in file ["
//                    + classIdEntry.getFilename() + "] => [" + classIdEntry.getClassName() + "]");
        byte[] classWeaveInfoByteArray = finishClassProcess(classIdEntry, weaveLog);
//            Logging.recordWeaveInfo(classWeaveInfoByteArray, classIdEntry, log);

        return new InstrumentedClass(classTransformer.getWeaveResult(), classWeaveInfoByteArray);

//        } catch (Throwable e) {
//            if (container != null && container.length() > 0) {
//                log("Failed to weave " + className + " in " + container);
//            } else {
//                log("Failed to weave " + className);
//            }
//            log(e);
//            if (dumpOption) doSave(className, target, CATEGORY_ERROR_CLASSES);
//            return null;
//        }
    }

    /**
     * Write the weaving result to files.
     * Without calling this method, this object discards data when a weaving failed.
     *
     * @param classInfo records the class information.
     * @param result    records the state after weaving.
     * @return updated byte code with probes added
     */
    public byte[] finishClassProcess(ClassInfo classInfo, WeaveLog result) {

        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(boas);

        try {
//            byte[] classInfoBytes = classInfo.toBytes();
//            out.write(classInfoBytes);
            classInfo.writeToOutputStream(out);
//            System.err.println("ClassBytes: " + new String(classInfoBytes));


        } catch (IOException e) {
            e.printStackTrace(logger);
        }
        classId++;

//        confirmedDataId = result.getNextDataId();
        try {
            ArrayList<DataInfo> dataInfoEntries = result.getDataEntries();
            out.writeInt(dataInfoEntries.size());
            for (DataInfo dataInfo : dataInfoEntries) {
//                byte[] classWeaveBytes = dataInfo.toBytes();
//                out.write(classWeaveBytes);
                dataInfo.writeToStream(out);
            }
        } catch (IOException e) {
            e.printStackTrace(logger);
        }

        // Commit method IDs to the final output
//        confirmedMethodId = result.getNextMethodId();
        try {
            ArrayList<MethodInfo> methods = result.getMethods();
            out.writeInt(methods.size());
            for (MethodInfo method : methods) {
//                byte[] methodBytes = method.toBytes();
//                out.write(methodBytes);
                method.writeToOutputStream(out);

            }
        } catch (IOException e) {
            e.printStackTrace(logger);
        }

        return boas.toByteArray();
    }


    /**
     * Compute SHA-1 Hash for logging.
     * The hash is important to identify an exact class because
     * multiple versions of a class may be loaded on a Java Virtual Machine.
     *
     * @param targetClass byte content of a class file.
     * @return a string representation of SHA-1 hash.
     */
    private String getClassHash(byte[] targetClass) {
        if (digest != null) {
            byte[] hash = digest.digest(targetClass);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String l = "0" + Integer.toHexString(b);
                hex.append(l.substring(l.length() - 2));
            }
            return hex.toString();
        } else {
            return "";
        }
    }

//    /**
//     * Write a woven class into a file for a user who
//     * would like to see the actual file (e.g. for debugging).
//     *
//     * @param name     specifies a class name.
//     * @param b        is the bytecode content.
//     * @param category specifies a directory name (CATEGORY_WOVEN_CLASSES, CATEGORY_ERROR_CLASSES).
//     */
//    private void doSave(String name, byte[] b, String category) {
//        try {
//            File classDir = new File(outputDir, category);
//            File classFile = new File(classDir, name + ".class");
//            classFile.getParentFile().mkdirs();
//            Files.write(classFile.toPath(), b, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
//                    StandardOpenOption.TRUNCATE_EXISTING);
//            log("Saved " + name + " to " + classFile.getAbsolutePath());
//        } catch (IOException e) {
//            log(e);
//        }
//    }

}
