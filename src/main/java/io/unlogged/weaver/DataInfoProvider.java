package io.unlogged.weaver;

import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;

public class DataInfoProvider {

    private int classId = 0;
    private int methodId = 0;
    private int probeId = 0;

    public int nextClassId() {
        return classId++;
    }

    public int nextMethodId() {
        return methodId++;
    }

    public int nextProbeId() {
        return probeId++;
    }
}
