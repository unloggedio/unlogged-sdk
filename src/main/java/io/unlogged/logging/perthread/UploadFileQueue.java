package io.unlogged.logging.perthread;

import com.insidious.common.UploadFile;

import java.io.IOException;

public interface UploadFileQueue {
    public void add(UploadFile uploadFile) throws IOException;
}
