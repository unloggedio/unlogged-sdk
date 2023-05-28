package io.unlogged.logging;


import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This interface defines a set of methods for recording runtime events.
 * Classes implementing this interface should provide an actual logging strategy.
 * selogger.logging.Logging class uses this interface to record events.
 */
public interface IEventLogger {

    /**
     * Close the logger.
     * An implementation class may use this method to release any resources used for logging.
     */
    public void close();

    public Object getObjectByClassName(String name);

    /**
     * Record an event occurrence and a value.
     *
     * @param dataId specifies an event and its bytecode location.
     * @param value  contains a value to be recorded.
     */
    public void recordEvent(int dataId, Object value);

    /**
     * Record an event occurrence and a value.
     *
     * @param dataId specifies an event and its bytecode location.
     * @param value  contains a value to be recorded.
     */
    public void recordEvent(int dataId, int value);
//	public void recordEvent(int dataId, Integer value);
//	public void recordEvent(int dataId, Long value);
//	public void recordEvent(int dataId, Short value);
//	public void recordEvent(int dataId, Boolean value);
//	public void recordEvent(int dataId, Double value);
//	public void recordEvent(int dataId, Float value);
//	public void recordEvent(int dataId, Byte value);
//	public void recordEvent(int dataId, Date value);

    /**
     * Record an event occurrence and a value.
     *
     * @param dataId specifies an event and its bytecode location.
     * @param value  contains a value to be recorded.
     */
    public void recordEvent(int dataId, long value);

    /**
     * Record an event occurrence and a value.
     *
     * @param dataId specifies an event and its bytecode location.
     * @param value  contains a value to be recorded.
     */
    public void recordEvent(int dataId, byte value);

    /**
     * Record an event occurrence and a value.
     *
     * @param dataId specifies an event and its bytecode location.
     * @param value  contains a value to be recorded.
     */
    public void recordEvent(int dataId, short value);

    /**
     * Record an event occurrence and a value.
     *
     * @param dataId specifies an event and its bytecode location.
     * @param value  contains a value to be recorded.
     */
    public void recordEvent(int dataId, char value);

    void registerClass(Integer id, Class<?> type);

    /**
     * Record an event occurrence and a value.
     *
     * @param dataId specifies an event and its bytecode location.
     * @param value  contains a value to be recorded.
     */
    public void recordEvent(int dataId, boolean value);

    /**
     * Record an event occurrence and a value.
     *
     * @param dataId specifies an event and its bytecode location.
     * @param value  contains a value to be recorded.
     */
    public void recordEvent(int dataId, double value);

    /**
     * Record an event occurrence and a value.
     *
     * @param dataId specifies an event and its bytecode location.
     * @param value  contains a value to be recorded.
     */
    void recordEvent(int dataId, float value);
//	void recordWeaveInfo(byte[] byteArray, ClassInfo classIdEntry, WeaveLog log);

    void setRecording(boolean b);

    ObjectMapper getObjectMapper();

    ClassLoader getTargetClassLoader();
}
