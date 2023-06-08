package io.unlogged.logging.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.unlogged.logging.IEventLogger;
import io.unlogged.logging.util.AggregatedFileLogger;
import io.unlogged.logging.util.ObjectIdAggregatedStream;

import java.util.Date;

/**
 * This class is an implementation of IEventLogger that records
 * a sequence of runtime events in files.
 * This object creates three types of files:
 * 1. log-*.slg files recording a sequence of events,
 * 2. LOG$Types.txt recording a list of type IDs and their corresponding type names,
 * 3. ObjectIdMap recording a list of object IDs and their type IDs.
 * Using the second and third files, a user can know classes in an execution trace.
 */
public class EventStreamAggregatedLogger implements IEventLogger {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AggregatedFileLogger aggregatedLogger;
    //    private final TypeIdAggregatedStreamMap typeToId;
    private final ObjectIdAggregatedStream objectIdMap;

    /**
     * Create an instance of logging object.
     *
     * @param objectIdMap      object id mapper to convert object instances to persistent id
     * @param aggregatedLogger writer
     */
    public EventStreamAggregatedLogger(ObjectIdAggregatedStream objectIdMap,
                                       AggregatedFileLogger aggregatedLogger
    ) {
//        System.out.printf("[unlogged] new event stream aggregated logger\n");
        this.aggregatedLogger = aggregatedLogger;
        this.objectIdMap = objectIdMap;
    }

    @Override
    public void registerClass(Integer id, Class<?> type) {

    }

    public ObjectIdAggregatedStream getObjectIdMap() {
        return objectIdMap;
    }

    /**
     * Close all file streams used by the object.
     */
    public void close() {
        System.out.printf("[unlogged] close event stream aggregated logger\n");
        objectIdMap.close();
        try {
            aggregatedLogger.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getObjectByClassName(String name) {
        return null;
    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public void recordEvent(int dataId, Object value) {
        long objectId = objectIdMap.getId(value);
        aggregatedLogger.writeEvent(dataId, objectId);
    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public void recordEvent(int dataId, Integer value) {
        if (value == null) {
            aggregatedLogger.writeEvent(dataId, 0);
        } else {
            aggregatedLogger.writeEvent(dataId, value.longValue());
        }
    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public void recordEvent(int dataId, Long value) {
        if (value == null) {
            aggregatedLogger.writeEvent(dataId, 0);
        } else {
            aggregatedLogger.writeEvent(dataId, value.longValue());
        }

    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public void recordEvent(int dataId, Short value) {
        if (value == null) {
            aggregatedLogger.writeEvent(dataId, 0);
        } else {
            aggregatedLogger.writeEvent(dataId, value.longValue());
        }

    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public void recordEvent(int dataId, Boolean value) {
        aggregatedLogger.writeEvent(dataId, value == null ? 0 : value ? 1 : 0);
    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public void recordEvent(int dataId, Float value) {
        if (value == null) {
            aggregatedLogger.writeEvent(dataId, 0);
        } else {
            aggregatedLogger.writeEvent(dataId, value.longValue());
        }

    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public void recordEvent(int dataId, Byte value) {
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public void recordEvent(int dataId, Date value) {
        if (value == null) {
            aggregatedLogger.writeEvent(dataId, 0);
        } else {
            aggregatedLogger.writeEvent(dataId, value.getTime());
        }

    }

    /**
     * Record an event and an object.
     * The object is translated into an object ID.
     */
    public void recordEvent(int dataId, Double value) {
        long longValue = Double.doubleToRawLongBits(value);
        aggregatedLogger.writeEvent(dataId, longValue);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value.
     */
    public void recordEvent(int dataId, int value) {
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     */
    public void recordEvent(int dataId, long value) {
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value.
     */
    public void recordEvent(int dataId, byte value) {
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value.
     */
    public void recordEvent(int dataId, short value) {
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value.
     */
    public void recordEvent(int dataId, char value) {
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, value);
        aggregatedLogger.writeEvent(dataId, value);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value (true = 1, false = 0).
     */
    public void recordEvent(int dataId, boolean value) {
        int longValue = value ? 1 : 0;
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, longValue);
        aggregatedLogger.writeEvent(dataId, longValue);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value preserving the information.
     */
    public void recordEvent(int dataId, double value) {
        long longValue = Double.doubleToRawLongBits(value);
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, longValue);
        aggregatedLogger.writeEvent(dataId, longValue);
    }

    /**
     * Record an event and an integer value.
     * To simplify the file writing process, the value is translated into a long value preserving the information.
     */
    public void recordEvent(int dataId, float value) {
        int longValue = Float.floatToRawIntBits(value);
//        System.out.printf("Record event in event stream aggregated logger %s -> %s\n", dataId, longValue);
        aggregatedLogger.writeEvent(dataId, longValue);
    }

//    @Override
//    public void recordWeaveInfo(byte[] byteArray, ClassInfo classIdEntry, WeaveLog log) {
//        aggregatedLogger.writeWeaveInfo(byteArray);
//    }

    @Override
    public void setRecording(boolean b) {

    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public ClassLoader getTargetClassLoader() {
        return null;
    }


}
