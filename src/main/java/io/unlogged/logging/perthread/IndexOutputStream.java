package io.unlogged.logging.perthread;

import com.insidious.common.UploadFile;
import com.insidious.common.cqengine.ObjectInfoDocument;
import com.insidious.common.cqengine.StringInfoDocument;
import com.insidious.common.cqengine.TypeInfoDocument;

import java.io.IOException;
import java.util.List;
import java.util.Queue;

public interface IndexOutputStream {
    void writeFileEntry(UploadFile uploadFile) throws IOException;

    void close();

    void addValueId(long valueId);

    void addProbeId(int probeId);

    void drainQueueToIndex(
            List<ObjectInfoDocument> objectsToIndex,
            Queue<TypeInfoDocument> typesToIndex,
            List<StringInfoDocument> stringsToIndex);

    int fileCount();
}
