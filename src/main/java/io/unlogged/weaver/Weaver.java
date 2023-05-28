package io.unlogged.weaver;


import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.LogLevel;
import com.insidious.common.weaver.MethodInfo;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import io.unlogged.Runtime;
import io.unlogged.logging.IErrorLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * This class manages bytecode injection process and weaving logs.
 */
public class Weaver implements IErrorLogger {

    public static final String PROPERTY_FILE = "weaving.properties";
    public static final String SEPARATOR = ",";
    public static final char SEPARATOR_CHAR = ',';
    public static final String CLASS_ID_FILE = "classes.txt";
    public static final String METHOD_ID_FILE = "methods.txt";
    public static final String DATA_ID_FILE = "dataids.txt";
    public static final String ERROR_LOG_FILE = "log.txt";

    public static final String CATEGORY_WOVEN_CLASSES = "woven-classes";
    public static final String CATEGORY_ERROR_CLASSES = "error-classes";

    private final File outputDir;
    private final String lineSeparator = "\n";
    private final WeaveConfig config;
    final private TreeMaker treeMaker;
    final private JavacElements elementUtils;
    private Writer dataIdWriter;
    private PrintStream logger;
    private int classId;
    private int confirmedDataId;
    private int confirmedMethodId;
    private Writer methodIdWriter;
    private boolean dumpOption;

    /**
     * Set up the object to manage a weaving process.
     * This constructor creates files to store the information.
     *
     * @param outputDir    location to save the weave data
     * @param config       weave configuration
     * @param treeMaker
     * @param elementUtils
     */
    public Weaver(File outputDir, WeaveConfig config, TreeMaker treeMaker, JavacElements elementUtils) {
        this.treeMaker = treeMaker;
        this.elementUtils = elementUtils;
        assert outputDir.isDirectory() && outputDir.canWrite();

        this.outputDir = outputDir;
        this.config = config;
        confirmedDataId = 0;
        confirmedMethodId = 0;
        classId = 0;

        try {
            logger = new PrintStream(new File(outputDir, ERROR_LOG_FILE));
        } catch (FileNotFoundException e) {
            logger = System.out;
            logger.println("[unlogged] failed to open " + ERROR_LOG_FILE + " in " + outputDir.getAbsolutePath());
            logger.println("[unlogged] using System.out instead.");
        }

        String agentVersion = Runtime.class.getPackage().getImplementationVersion();

        logger.printf("Java version: %s%n", System.getProperty("java.version"));
        logger.printf("Agent version: %s%n", agentVersion);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            digest = null;
        }

    }

    /**
     * Record a message
     *
     * @param message string to be logged
     */
    @Override
    public void log(String message) {
        logger.println(message);
    }

    /**
     * Record a runtime error.
     *
     * @param throwable object to be logged
     */
    @Override
    public void log(Throwable throwable) {
        throwable.printStackTrace(logger);
    }

    /**
     * Close files written by the weaver.
     */
    public void close() {
        config.save(new File(outputDir, PROPERTY_FILE));
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
     * @param jcClassDecl is the content of the class.
     */
    public void weave(JCTree.JCClassDecl jcClassDecl) {
        String className = jcClassDecl.getSimpleName().toString();

        String hash = getClassHash(jcClassDecl);
        LogLevel level = LogLevel.Normal;
        WeaveLog log = new WeaveLog(classId, confirmedMethodId, confirmedDataId);
        try {
            ClassTransformer classTransformer = new ClassTransformer(log, config, jcClassDecl, treeMaker, elementUtils);


            String[] interfaces = new String[jcClassDecl.implementing.size()];
            List<JCTree.JCExpression> implementing = jcClassDecl.implementing;

            for (int i = 0; i < implementing.size(); i++) {
                JCTree.JCExpression jcExpression = implementing.get(i);
                interfaces[i] = jcExpression.type.toString();
            }


            ClassInfo classIdEntry = new ClassInfo(
                    classId, "",
                    "classTransformer.getSourceFileName()",
                    className, level, hash,
                    "classTransformer.getClassLoaderIdentifier()",
                    interfaces,
                    "jcClassDecl.extending.type.toString()",
                    jcClassDecl.typarams.toString()
            );


            byte[] classWeaveInfoByteArray = finishClassProcess(classIdEntry, log);
            // TODO
            // Logging.recordWeaveInfo(classWeaveInfoByteArray, classIdEntry, log);

//            return classTransformer.getWeaveResult();

        } catch (Throwable e) {
            log("Failed to weave " + className);
            log(e);
            if (dumpOption) doSave(className, jcClassDecl, CATEGORY_ERROR_CLASSES);
        }
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
            byte[] classInfoBytes = classInfo.toBytes();
//            System.err.println("ClassBytes: " + new String(classInfoBytes));
            out.write(classInfoBytes);


        } catch (IOException e) {
            e.printStackTrace(logger);
        }
        classId++;

        confirmedDataId = result.getNextDataId();
        try {
            ArrayList<DataInfo> dataInfoEntries = result.getDataEntries();
            out.writeInt(dataInfoEntries.size());
            for (DataInfo dataInfo : dataInfoEntries) {
                byte[] classWeaveBytes = dataInfo.toBytes();
                out.write(classWeaveBytes);
            }
        } catch (IOException e) {
            e.printStackTrace(logger);
        }

        // Commit method IDs to the final output
        confirmedMethodId = result.getNextMethodId();
        try {
            ArrayList<MethodInfo> methods = result.getMethods();
            out.writeInt(methods.size());
            for (MethodInfo method : methods) {
                byte[] methodBytes = method.toBytes();
                out.write(methodBytes);

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
     * @param jcClassDecl byte content of a class file.
     * @return a string representation of SHA-1 hash.
     */
    private String getClassHash(JCTree.JCClassDecl jcClassDecl) {
        return String.valueOf(jcClassDecl.hashCode());
//        if (digest != null) {
//            byte[] hash = digest.digest(targetClass);
//            StringBuilder hex = new StringBuilder(hash.length * 2);
//            for (byte b : hash) {
//                String l = "0" + Integer.toHexString(b);
//                hex.append(l.substring(l.length() - 2));
//            }
//            return hex.toString();
//        } else {
//            return "";
//        }
    }

    /**
     * Write a woven class into a file for a user who
     * would like to see the actual file (e.g. for debugging).
     *
     * @param name        specifies a class name.
     * @param jcClassDecl is the bytecode content.
     * @param category    specifies a directory name (CATEGORY_WOVEN_CLASSES, CATEGORY_ERROR_CLASSES).
     */
    private void doSave(String name, JCTree.JCClassDecl jcClassDecl, String category) {
        try {
            File classDir = new File(outputDir, category);
            File classFile = new File(classDir, name + ".class");
            classFile.getParentFile().mkdirs();
            Files.write(classFile.toPath(), jcClassDecl.toString().getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log("Saved " + name + " to " + classFile.getAbsolutePath());
        } catch (IOException e) {
            log(e);
        }
    }

}
