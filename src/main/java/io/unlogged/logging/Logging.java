package io.unlogged.logging;

import io.unlogged.logging.impl.DetailedEventStreamAggregatedLogger;
import io.unlogged.logging.impl.EventStreamAggregatedLogger;
import io.unlogged.logging.util.AggregatedFileLogger;
import io.unlogged.logging.util.ObjectIdAggregatedStream;
import io.unlogged.logging.util.TypeIdAggregatedStreamMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;


/**
 * This class provides static members for logging execution.
 * The weaver component inserts method calls to this class.
 * Array-related recording methods (recordArrayLoad, recordArrayStore,
 * recordMultiNewArray, and recordMultiNewArrayContents) are provided
 * to simplify the weaver's code.
 */
public class Logging {

    /**
     * The instance to record events.
     * A system must call one of initialize methods OR directly set an instance to this field.
     */
    static IEventLogger INSTANCE = new DiscardEventLogger();


    public static EventStreamAggregatedLogger initialiseAggregatedLogger(
            AggregatedFileLogger aggregatedLogger,
            File outputDir) throws IOException {
        TypeIdAggregatedStreamMap typeToId = new TypeIdAggregatedStreamMap(aggregatedLogger);
        ObjectIdAggregatedStream objectIdMap = new ObjectIdAggregatedStream(aggregatedLogger, typeToId, outputDir);

        EventStreamAggregatedLogger instance = new EventStreamAggregatedLogger(objectIdMap, aggregatedLogger);
        INSTANCE = instance;
        return instance;
    }

    public static DetailedEventStreamAggregatedLogger initialiseDetailedAggregatedLogger(
            AggregatedFileLogger aggregatedLogger, File outputDir
    ) throws IOException {
        TypeIdAggregatedStreamMap typeToId = new TypeIdAggregatedStreamMap(aggregatedLogger);
        ObjectIdAggregatedStream objectIdMap = new ObjectIdAggregatedStream(aggregatedLogger, typeToId, outputDir);

        DetailedEventStreamAggregatedLogger instance = new DetailedEventStreamAggregatedLogger(objectIdMap,
                aggregatedLogger);
        INSTANCE = instance;
        return instance;
    }

    /**
     * A method to record an event associated to an object.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(Object value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event associated to an object.
     *
     * @param value
     * @param dataId
     */
    public static Mono<?> recordEvent(Mono<?> value, int dataId) {
        return (Mono<?>) INSTANCE.recordEvent(dataId, value);
    }

    public static Flux<?> recordEvent(Flux<?> value, int dataId) {
        return (Flux<?>) INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event associated to a throwable object.
     * This method is defined for type checking.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(Throwable value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event associated to a boolean value.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(boolean value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event associated to a byte value.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(byte value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    public static void recordEvent(Integer value, int dataId) {
        if (value != null) {
            INSTANCE.recordEvent(dataId, (int) value);
        } else {
            INSTANCE.recordEvent(dataId, value);
        }
    }

    public static void recordEvent(Long value, int dataId) {
        if (value != null) {
            INSTANCE.recordEvent(dataId, (long) value);
        } else {
            INSTANCE.recordEvent(dataId, value);
        }
    }

    public static void recordEvent(Short value, int dataId) {
        if (value != null) {
            INSTANCE.recordEvent(dataId, (short) value);
        } else {
            INSTANCE.recordEvent(dataId, value);
        }
    }

    public static void recordEvent(Byte value, int dataId) {
        if (value != null) {
            INSTANCE.recordEvent(dataId, (byte) value);
        } else {
            INSTANCE.recordEvent(dataId, value);
        }
    }

    public static void recordEvent(Float value, int dataId) {
        if (value != null) {
            INSTANCE.recordEvent(dataId, (float) value);
        } else {
            INSTANCE.recordEvent(dataId, value);
        }
    }

    public static void recordEvent(Double value, int dataId) {
        if (value != null) {
            INSTANCE.recordEvent(dataId, (double) value);
        } else {
            INSTANCE.recordEvent(dataId, value);
        }
    }

    public static void recordEvent(Character value, int dataId) {
        if (value != null) {
            INSTANCE.recordEvent(dataId, (char) value);
        } else {
            INSTANCE.recordEvent(dataId, value);
        }
    }

    public static void recordEvent(Boolean value, int dataId) {
        if (value == null || !value) {
            INSTANCE.recordEvent(dataId, 0);
        } else {
            INSTANCE.recordEvent(dataId, 1);
        }
    }

    /**
     * A method to record an event associated to a char value.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(char value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event associated to a short integer value.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(short value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event associated to an integer value.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(int value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event associated to a long integer value.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(long value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event associated to a floating point number.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(float value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event associated to a double value.
     *
     * @param value
     * @param dataId
     */
    public static void recordEvent(double value, int dataId) {
        INSTANCE.recordEvent(dataId, value);
    }

    /**
     * A method to record an event without a data value.
     *
     * @param dataId dataId of the probe which generated the event
     */
    public static void recordEvent(int dataId) {
        INSTANCE.recordEvent(dataId, 0);
    }

    /**
     * A method to record an ArrayLoad event.
     *
     * @param array  specifies an array object.
     * @param index  specifies an array index.
     * @param dataId specifies an event.
     */
    public static void recordArrayLoad(Object array, int index, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        INSTANCE.recordEvent(dataId + 1, index);
    }

    /**
     * A method to record an ArrayStore event.
     * This method is prepared to reduce the number of instructions for logging.
     *
     * @param array  specifies an array object.
     * @param index  specifies an array index.
     * @param value  specifies a data written to the array.
     * @param dataId specifies an event.
     */
    public static void recordArrayStore(Object array, int index, byte value, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        INSTANCE.recordEvent(dataId + 1, index);
        INSTANCE.recordEvent(dataId + 2, value);
    }

    /**
     * A method to record an ArrayStore event.
     * This method is prepared to reduce the number of instructions for logging.
     *
     * @param array  specifies an array object.
     * @param index  specifies an array index.
     * @param value  specifies a data written to the array.
     * @param dataId specifies an event.
     */
    public static void recordArrayStore(Object array, int index, char value, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        INSTANCE.recordEvent(dataId + 1, index);
        INSTANCE.recordEvent(dataId + 2, value);
    }

    /**
     * A method to record an ArrayStore event.
     * This method is prepared to reduce the number of instructions for logging.
     *
     * @param array  specifies an array object.
     * @param index  specifies an array index.
     * @param value  specifies a data written to the array.
     * @param dataId specifies an event.
     */
    public static void recordArrayStore(Object array, int index, double value, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        INSTANCE.recordEvent(dataId + 1, index);
        INSTANCE.recordEvent(dataId + 2, value);
    }

    /**
     * A method to record an ArrayStore event.
     * This method is prepared to reduce the number of instructions for logging.
     *
     * @param array  specifies an array object.
     * @param index  specifies an array index.
     * @param value  specifies a data written to the array.
     * @param dataId specifies an event.
     */
    public static void recordArrayStore(Object array, int index, float value, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        INSTANCE.recordEvent(dataId + 1, index);
        INSTANCE.recordEvent(dataId + 2, value);
    }

    /**
     * A method to record an ArrayStore event.
     * This method is prepared to reduce the number of instructions for logging.
     *
     * @param array  specifies an array object.
     * @param index  specifies an array index.
     * @param value  specifies a data written to the array.
     * @param dataId specifies an event.
     */
    public static void recordArrayStore(Object array, int index, int value, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        INSTANCE.recordEvent(dataId + 1, index);
        INSTANCE.recordEvent(dataId + 2, value);
    }

    /**
     * A method to record an ArrayStore event.
     * This method is prepared to reduce the number of instructions for logging.
     *
     * @param array  specifies an array object.
     * @param index  specifies an array index.
     * @param value  specifies a data written to the array.
     * @param dataId specifies an event.
     */
    public static void recordArrayStore(Object array, int index, long value, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        INSTANCE.recordEvent(dataId + 1, index);
        INSTANCE.recordEvent(dataId + 2, value);
    }

    /**
     * A method to record an ArrayStore event.
     * This method is prepared to reduce the number of instructions for logging.
     *
     * @param array  specifies an array object.
     * @param index  specifies an array index.
     * @param value  specifies a data written to the array.
     * @param dataId specifies an event.
     */
    public static void recordArrayStore(Object array, int index, short value, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        INSTANCE.recordEvent(dataId + 1, index);
        INSTANCE.recordEvent(dataId + 2, value);
    }

    /**
     * A method to record an ArrayStore event.
     * This method is prepared to reduce the number of instructions for logging.
     *
     * @param array  specifies an array object.
     * @param index  specifies an array index.
     * @param value  specifies a data written to the array.
     * @param dataId specifies an event.
     */
    public static void recordArrayStore(Object array, int index, Object value, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        INSTANCE.recordEvent(dataId + 1, index);
        INSTANCE.recordEvent(dataId + 2, value);
    }

    /**
     * A method to record a MultiNewArray event.
     * This method is prepared to reduce the number of instructions for logging.
     *
     * @param array  specifies an array object.
     * @param dataId specifies an event.
     */
    public static void recordMultiNewArray(Object array, int dataId) {
        INSTANCE.recordEvent(dataId, array);
        recordMultiNewArrayContents((Object[]) array, dataId);
    }

    /**
     * This method scans the contents of an array and records their IDs.
     */
    private static void recordMultiNewArrayContents(Object[] array, int dataId) {
        LinkedList<Object[]> arrays = new LinkedList<Object[]>();
        arrays.addFirst(array);
        while (!arrays.isEmpty()) {
            Object[] asArray = arrays.removeFirst();
            INSTANCE.recordEvent(dataId + 1, asArray);
            for (int index = 0; index < asArray.length; ++index) {
                Object element = asArray[index];
                Class<?> elementType = element.getClass();
                if (element != null && elementType.isArray()) {
                    INSTANCE.recordEvent(dataId + 2, element);
                    if (elementType.getComponentType().isArray()) {
                        arrays.addLast((Object[]) element);
                    }
                }
            }
        }
    }

    public static IEventLogger initialiseDiscardLogger() {
        INSTANCE = new DiscardEventLogger();
        return INSTANCE;
    }
}
