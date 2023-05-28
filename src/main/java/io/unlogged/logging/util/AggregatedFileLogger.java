package io.unlogged.logging.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface AggregatedFileLogger {
    void writeNewObjectType(long id, long typeId);

    void writeNewString(long id, String stringObject);

    void writeNewException(byte[] toString);

    void writeEvent(int id, long value);


    void writeNewTypeRecord(int typeId, String typeName, byte[] toString);

    void writeWeaveInfo(byte[] byteArray);

    void shutdown() throws IOException, InterruptedException;

    void writeEvent(int dataId, long objectId, byte[] toByteArray);

    void writeEvent(int dataId, long objectId, ByteArrayOutputStream outputStream);
}
