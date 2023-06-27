package io.unlogged.core.weaver;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;
import com.insidious.common.weaver.MethodInfo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

/**
 * This object generates data IDs and records the weaving process
 * for a single class file.
 * If a bytecode weaving is terminated by an error,
 * the generated data IDs are disabled by disposing this log object.
 */
public class WeaveLog {

    public static final String SEPARATOR = ",";
    private final int classId;
    private final DataInfoProvider dataInfoProvider;
//    private int methodId;
    private final ArrayList<DataInfo> dataEntries;
    private final ArrayList<MethodInfo> methodEntries;
    private final StringWriter logger;
    private final PrintWriter loggerWrapper;
    private String fullClassName;
    private int methodId;

    /**
     * Create an instance having the given state.
     *
     * @param classId      is the ID used by the target class (if succeeded)
     * @param dataInfoProvider   is the first ID of the data IDs of the target class
     */
    public WeaveLog(int classId, DataInfoProvider dataInfoProvider) {
        this.classId = classId;
        this.dataInfoProvider = dataInfoProvider;
        dataEntries = new ArrayList<>();
        methodEntries = new ArrayList<>();
        logger = new StringWriter();
        loggerWrapper = new PrintWriter(logger);
    }

    /**
     * @return the full class name that is currently woven.
     */
    public String getFullClassName() {
        return fullClassName;
    }

    /**
     * Record the current target class with a full class name obtained from a target class file.
     * This is separated from the constructor because the class name is unavailable
     * at the beginning of the weaving process.
     *
     * @param name fully qualified class name of the weaved target
     */
    public void setFullClassName(String name) {
        this.fullClassName = name;
    }


    /**
     * Record a woven method name.
     *
     * @param className      is a class name.
     * @param methodName     is a method name.
     * @param methodDesc     is a descriptor representing the signature.
     * @param access         represents modifiers, e.g. static.
     * @param sourceFileName specifies a source file name recorded in the class file.
     * @param methodHash hash for the method being visited
     */
    public void startMethod(String className, String methodName, String methodDesc, int access, String sourceFileName, String methodHash) {
        methodId = dataInfoProvider.nextMethodId();
        MethodInfo entry = new MethodInfo(classId, methodId, className, methodName, methodDesc, access,
                sourceFileName, methodHash);
        methodEntries.add(entry);
    }

    /**
     * Create a new Data ID and record the information.
     *
     * @param line             specifies the line number.
     * @param instructionIndex specifies the location of the instruction in the ASM's InsnList object.
     * @param eventType        is an event type (decided by the instruction type).
     * @param valueDesc        specifies a data type of the value observed by the event.
     * @param attributes       specifies additional static information obtained from bytecode.
     * @return the next available id which can be used for the dataInfo
     */
    public int nextDataId(int line, int instructionIndex, EventType eventType, Descriptor valueDesc, String attributes) {
        int dataId = dataInfoProvider.nextProbeId(eventType);
        DataInfo entry = new DataInfo(classId, methodId, dataId, line, instructionIndex, eventType,
                valueDesc, attributes);
        dataEntries.add(entry);
        return dataId;
    }

    /**
     * Record an error message.
     * @param message message to be logged
     */
    public void log(String message) {
        loggerWrapper.println(message);
    }

    /**
     * @return a string including the error messages recorded in this object.
     */
    public String getLog() {
        loggerWrapper.close();
        return logger.toString();
    }

    /**
     * @return data ID objects created during the weaving.
     */
    public ArrayList<DataInfo> getDataEntries() {
        return dataEntries;
    }

    /**
     * @return methods processed by this weaving.
     */
    public ArrayList<MethodInfo> getMethods() {
        return methodEntries;
    }


}
