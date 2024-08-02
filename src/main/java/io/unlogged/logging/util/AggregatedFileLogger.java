package io.unlogged.logging.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public interface AggregatedFileLogger {
    void writeNewObjectType(long id, long typeId);

    void writeEvent(int id, long value);


    void writeNewTypeRecord(int typeId, String typeName, byte[] toString);

    void writeWeaveInfo(byte[] byteArray);

    void shutdown() throws IOException, InterruptedException;

    void writeEvent(int dataId, long objectId, byte[] toByteArray);

    void writeEvent(int dataId, long objectId, ByteArrayOutputStream outputStream);

    void errorLog(String message);

    void errorLog(Throwable throwable);

	void modifyThreadDepth(long delta);
}
