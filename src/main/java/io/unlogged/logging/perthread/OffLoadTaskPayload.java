package io.unlogged.logging.perthread;

public class OffLoadTaskPayload {
    int threadId;
    int probeId;
    long value;

    public int getThreadId() {
        return threadId;
    }

    public int getProbeId() {
        return probeId;
    }

    public long getValue() {
        return value;
    }

    public OffLoadTaskPayload(int threadId, int probeId, long value) {
        this.threadId = threadId;
        this.probeId = probeId;
        this.value = value;
    }
}
