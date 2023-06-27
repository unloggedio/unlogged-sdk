package io.unlogged.weaver;

import com.insidious.common.weaver.EventType;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class DataInfoProvider {

    private final AtomicInteger classId = new AtomicInteger(0);
    private final AtomicInteger methodId = new AtomicInteger(0);
    private final AtomicInteger probeId = new AtomicInteger(0);
    private OutputStream probeOutputStream;

    public int nextClassId() {
        return classId.addAndGet(1);
    }

    public int nextMethodId() {
        return methodId.addAndGet(1);
    }

    public int nextProbeId(EventType eventType) {
        int nextProbeId = probeId.addAndGet(1);
        if (
                eventType.equals(EventType.CALL_PARAM) ||
                        eventType.equals(EventType.METHOD_PARAM) ||
                        eventType.equals(EventType.CALL_RETURN) ||
                        eventType.equals(EventType.METHOD_NORMAL_EXIT) ||
                        eventType.equals(EventType.METHOD_EXCEPTIONAL_EXIT)
        ) {
            byte[] buffer = new byte[4];
            buffer[0] = (byte) (nextProbeId >>> 24);
            buffer[1] = (byte) (nextProbeId >>> 16);
            buffer[2] = (byte) (nextProbeId >>> 8);
            buffer[3] = (byte) (nextProbeId >>> 0);
            try {
                probeOutputStream.write(buffer);
            } catch (IOException e) {
                // should never happen
            }
        }
        return nextProbeId;
    }

    public OutputStream getProbeOutputStream() {
        return probeOutputStream;
    }

    public void setProbeOutputStream(OutputStream probeOutputStream) {
        this.probeOutputStream = probeOutputStream;
    }
}
