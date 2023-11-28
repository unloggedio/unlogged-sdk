package io.unlogged.logging.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;


/**
 * This class added type ID management and file save features to ObjectIdMap class.
 */
public class ObjectIdAggregatedStream extends ObjectIdMap {

    private final String lineSeparator = "\n";
    private final AggregatedFileLogger aggregatedLogger;
    private final TypeIdAggregatedStreamMap typeToId;

    /**
     * Create an instance to record object types.
     *
     * @param aggregatedLogger destination logger object
     * @param typeToId         is an object to translate a type into an integer representing a type.
     * @param outputDir        location to save the object map
     * @throws IOException when failed to save object map to outputDir
     */
    public ObjectIdAggregatedStream(
            AggregatedFileLogger aggregatedLogger,
            TypeIdAggregatedStreamMap typeToId, File outputDir) throws IOException {
        super(8 * 1024 * 1024, outputDir);
        this.typeToId = typeToId;
        this.aggregatedLogger = aggregatedLogger;
    }

    /**
     * Register a type for each new object.
     * This is separated from onNewObjectId because this method
     * calls TypeIdMap.createTypeRecord that may call a ClassLoader's method.
     * If the ClassLoader is also monitored by SELogger,
     * the call indirectly creates another object ID.
     */
    @Override
    protected void onNewObject(Object o) {
        typeToId.getTypeIdString(o.getClass());
    }

    /**
     * Record an object ID and its Type ID in a file.
     * In case of String and Throwable, this method also record their textual contents.
     */
    @Override
    protected void onNewObjectId(Object o, long id) {
        int typeId = typeToId.getTypeIdString(o.getClass());
//        System.err.println("new object [" + id + "] of type [" + typeId + "] -> " + o.getClass().getCanonicalName() + " => " + o);
        aggregatedLogger.writeNewObjectType(id, typeId);

//        if (o instanceof String) {
//            String stringObject = (String) o;
//            if (stringObject.length() < 1001) {
//                aggregatedLogger.writeNewString(id, stringObject);
//            }
//        } else
        if (o instanceof Throwable) {
            try {
                Throwable t = (Throwable) o;
                long causeId = getId(t.getCause());

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                DataOutputStream output = new DataOutputStream(outputStream);
                output.writeLong(id);
                byte[] messageBytes = t.getMessage().getBytes();
                output.writeInt(messageBytes.length);
                output.write(messageBytes);
                output.writeLong(causeId);

                StackTraceElement[] trace = t.getStackTrace();

                // todo: recording only first item in the stack trace for now, need to record the
                //  whole stack trace for better reproducibility ?
                for (int i = 0; i < 1; ++i) {
                    StackTraceElement e = trace[i];

                    byte[] classNameBytes = e.getClassName().getBytes();
                    byte[] methodNameBytes = e.getMethodName().getBytes();
                    byte[] fileNameBytes = e.getFileName().getBytes();

                    output.writeBoolean(e.isNativeMethod());

                    output.writeInt(classNameBytes.length);
                    output.write(classNameBytes);

                    output.writeInt(methodNameBytes.length);
                    output.write(methodNameBytes);

                    output.writeInt(fileNameBytes.length);
                    output.write(fileNameBytes);

                    output.writeInt(e.getLineNumber());

                }
            } catch (Throwable e) {
                // ignore all exceptions
            }
        }
    }

    /**
     * Close the files written by this object.
     */
    public synchronized void close() {

    }

}
