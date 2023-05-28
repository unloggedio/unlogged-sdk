package io.unlogged.logging.perthread;

import com.insidious.common.UploadFile;

import java.io.IOException;

public class UploadFileQueueImpl implements UploadFileQueue {

    private final RawFileCollector rawFileCollectorCron;

    public UploadFileQueueImpl(RawFileCollector rawFileCollectorCron) {
        this.rawFileCollectorCron = rawFileCollectorCron;
    }

    @Override
    public void add(UploadFile uploadFile) throws IOException {
//        rawFileCollectorCron.pushFileToIndex(uploadFile);
    }
}
