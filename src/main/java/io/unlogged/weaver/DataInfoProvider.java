package io.unlogged.weaver;

import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;

import java.util.concurrent.atomic.AtomicInteger;

public class DataInfoProvider {

    private final AtomicInteger classId = new AtomicInteger(0);
    private final AtomicInteger methodId = new AtomicInteger(0);
    private final AtomicInteger probeId = new AtomicInteger(0);
    public int nextClassId() {
        return classId.addAndGet(1);
    }

    public int nextMethodId() {
        return methodId.addAndGet(1);
    }

    public int nextProbeId() {
        return probeId.addAndGet(1);
    }
}
