package io.unlogged.logging.perthread;

import com.insidious.common.UploadFile;
import com.insidious.common.cqengine.ObjectInfoDocument;
import com.insidious.common.cqengine.StringInfoDocument;
import com.insidious.common.cqengine.TypeInfoDocument;
import io.unlogged.logging.IErrorLogger;
import io.unlogged.logging.util.FileNameGenerator;
import io.unlogged.logging.util.NetworkClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RawFileCollector implements Runnable {
    public static final int MAX_CONSECUTIVE_FAILURE_COUNT = 10;
    public static final int FAILURE_SLEEP_DELAY = 10;
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(4);
    private final IErrorLogger errorLogger;
    private final BlockingQueue<UploadFile> fileList;
    private final FileNameGenerator indexFileNameGenerator;
    //    private final List<byte[]> classWeaves = new LinkedList<>();
    private final Queue<TypeInfoDocument> typeInfoDocuments;
    private final NetworkClient networkClient;
    private final BlockingQueue<TypeInfoDocument> typesToIndex;
    private final List<ObjectInfoDocument> EMPTY_LIST = new ArrayList<>();
    private final List<StringInfoDocument> EMPTY_STRING_LIST = new ArrayList<>();
    private final Queue<TypeInfoDocument> EMPTY_TYPE_LIST = new ArrayBlockingQueue<>(1);
    private final FileOutputStream classWeaveFileRaw;
    private final File outputDir;
    private final BlockingQueue<StringInfoDocument> stringsToIndex;
    private final BlockingQueue<ObjectInfoDocument> objectsToIndex;
    private final ArchiveCloser archiveCloser;
    private final BlockingQueue<ArchivedIndexWriter> archiveQueue = new ArrayBlockingQueue<>(100);
    public int filesPerArchive = 0;
    private boolean shutdown = false;
    private boolean shutdownComplete = false;
    private boolean skipUploads;
    private ArchivedIndexWriter archivedIndexWriter;
    private int fileCount = 0;
    private AtomicBoolean isDraining = new AtomicBoolean(false);

    public RawFileCollector(int filesPerArchive,
                            FileNameGenerator indexFileNameGenerator,
                            NetworkClient networkClient,
                            IErrorLogger errorLogger,
                            File outputDir) throws IOException {
        this.filesPerArchive = filesPerArchive;
        this.networkClient = networkClient;
        this.indexFileNameGenerator = indexFileNameGenerator;
        this.errorLogger = errorLogger;
        this.fileList = new ArrayBlockingQueue<>(1024 * 128);
        this.typeInfoDocuments = new ArrayBlockingQueue<>(1024 * 1024);
        typesToIndex = new ArrayBlockingQueue<>(1024 * 1024);
        stringsToIndex = new ArrayBlockingQueue<>(1024 * 1024);
        objectsToIndex = new ArrayBlockingQueue<>(1024 * 1024);

        this.outputDir = outputDir;
//        errorLogger.log("Created raw file collector, files per archive: " + filesPerArchive);
        finalizeArchiveAndUpload();
        classWeaveFileRaw = new FileOutputStream(new File(outputDir + "/" + "class.weave.dat"));
        archiveCloser = new ArchiveCloser(archiveQueue, errorLogger);
        EXECUTOR_SERVICE.submit(archiveCloser);

    }

    private final Lock archiveSwapLock = new ReentrantLock();

    private void finalizeArchiveAndUpload() throws IOException {


        archiveSwapLock.lock();
        ArchivedIndexWriter archivedIndexWriterOld = archivedIndexWriter;
        archivedIndexWriter = new ArchivedIndexWriter(indexFileNameGenerator.getNextFile(),
                outputDir + "/class.weave.dat", errorLogger);
        archiveSwapLock.unlock();

        fileCount = 0;
        if (archivedIndexWriterOld != null) {
            boolean added = archiveQueue.offer(archivedIndexWriterOld);
            if (!added) {
                errorLogger.log("Failed to close archive queue, queue is full");
            }
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                long start = System.currentTimeMillis();
//                errorLogger.log(start + " : run raw file collector cron: " + shutdown);
                if (shutdown) {
                    return;
                }
                try {
                    EXECUTOR_SERVICE.submit(() -> {
                        try {
                            if (!archiveSwapLock.tryLock()) {
                                return;
                            }
                            drainItemsToIndex(archivedIndexWriter);
                            archiveSwapLock.unlock();
                        } catch (Throwable e) {
                            errorLogger.log(e);
                        }
                    });
                    upload();
                } catch (IOException e) {
                    errorLogger.log(e);
                }
                Thread.sleep(1000);
            }
        } catch (Throwable e) {
            errorLogger.log("failed to write archived index to disk: " + e.getMessage());
        }
    }


    public void shutdown() {
        shutdown = true;
        errorLogger.log("shutting down raw file collector");
        EXECUTOR_SERVICE.shutdownNow();
    }

    public void upload() throws IOException {
        if (shutdownComplete) {
            return;
        }
        try {
//            errorLogger.log("wait for log file");
            UploadFile logFile = fileList.poll(1, TimeUnit.SECONDS);
//            errorLogger.log("got log file");
            if (logFile == null) {
                if (fileCount > 0 || shutdown) {
                    errorLogger.log(
                            "files from queue, currently [" + fileCount + "] files in list : shutdown: " + shutdown);
                    finalizeArchiveAndUpload();
                    return;
                }
//                errorLogger.log("nothing to load: " + shutdown);
                return;
            }

            List<UploadFile> logFiles = new LinkedList<>();
            fileList.drainTo(logFiles, filesPerArchive - archivedIndexWriter.fileCount());
            logFiles.add(logFile);

//            errorLogger.log("add [" + logFiles.size() + "] files");
            for (UploadFile file : logFiles) {
                File fileToAddToArchive = new File(file.path);
                archivedIndexWriter.writeFileEntry(file);
                fileCount++;
                errorLogger.log("delete [" + file.path + "]");
                fileToAddToArchive.delete();
            }
        } catch (IOException e) {
            System.err.println("[unlogged] failed to upload file: " + e.getMessage());
            errorLogger.log(e);
        } catch (InterruptedException e) {
            errorLogger.log("file upload cron interrupted, shutting down");
        } finally {
//            errorLogger.log("finally check can archive [" + archivedIndexWriter.getArchiveFile()
//                    .getName() + "]: " + archivedIndexWriter.fileCount() + " >= " + filesPerArchive);
            if (archivedIndexWriter.fileCount() >= filesPerArchive || shutdown) {
                finalizeArchiveAndUpload();
            }
            if (shutdown) {
                shutdownComplete = true;
            }
        }
    }

    public void drainItemsToIndex(ArchivedIndexWriter writer) {
        if (!isDraining.compareAndSet(false, true)) {
            return;
        }
        try {


            errorLogger.log("[" + writer.getArchiveFile().getName() + "] Drain items to index: " +
                    objectsToIndex.size() + ", " + typesToIndex.size() + ", " + stringsToIndex.size());
            List<StringInfoDocument> stringInfoDocuments = new ArrayList<>();


            List<ObjectInfoDocument> objectInfoDocuments = new ArrayList<>(objectsToIndex.size());
            objectsToIndex.drainTo(objectInfoDocuments);

            List<TypeInfoDocument> newTypes = new ArrayList<>();
            typesToIndex.drainTo(newTypes);

            typeInfoDocuments.addAll(newTypes);

            stringsToIndex.drainTo(stringInfoDocuments);

            if (objectInfoDocuments.size() == 0 && stringInfoDocuments.size() == 0 && typeInfoDocuments.size() == 0) {
                errorLogger.log("no new data to record, return");
                return;
            }

            writer.drainQueueToIndex(objectInfoDocuments, EMPTY_TYPE_LIST, stringInfoDocuments);
            errorLogger.log("[" + writer.getArchiveFile().getName() + "] Drained");
        } finally {
            isDraining.set(false);
        }


    }


    public void indexObjectTypeEntry(long id, int typeId) {
        objectsToIndex.offer(new ObjectInfoDocument(id, typeId));
    }

    public void indexStringEntry(long id, String stringObject) {
        stringsToIndex.offer(new StringInfoDocument(id, stringObject));

    }

    public void addValueId(long valueId) {
        archivedIndexWriter.addValueId(valueId);

    }

    public void addProbeId(int probeId) {
        archivedIndexWriter.addProbeId(probeId);
    }

    public void indexTypeEntry(int typeId, String typeName, byte[] typeInfoBytes) {
//        System.err.println("Offering type [" + typeId + "] -> " + typeName + ". Now collected " + typesToIndex.size());
        typesToIndex.offer(new TypeInfoDocument(typeId, typeName, typeInfoBytes));
    }

    synchronized public void addClassWeaveInfo(byte[] byteArray) {
        try {
            classWeaveFileRaw.write(byteArray);
            classWeaveFileRaw.flush();
        } catch (IOException e) {
            errorLogger.log("Failed to write class weave information: " + e.getMessage());
        }
    }

    public BlockingQueue<UploadFile> getFileQueue() {
        return this.fileList;
    }

    public class ArchiveCloser implements Runnable {

        private final BlockingQueue<ArchivedIndexWriter> archiveQueue;
        private final IErrorLogger errorLogger;

        public ArchiveCloser(BlockingQueue<ArchivedIndexWriter> archiveQueue, IErrorLogger errorLogger) {
            this.archiveQueue = archiveQueue;
            this.errorLogger = errorLogger;
        }

        @Override
        public void run() {
            while (true) {
                try {
//                    errorLogger.log("Waiting for next archive to close");
                    ArchivedIndexWriter archivedIndexWriterOld = archiveQueue.take();
                    try {
//                        errorLogger.log("closing archive: " + archivedIndexWriterOld.getArchiveFile().getName());
                        drainItemsToIndex(archivedIndexWriterOld);
                        archivedIndexWriterOld.drainQueueToIndex(EMPTY_LIST, typeInfoDocuments, EMPTY_STRING_LIST);
                        archivedIndexWriterOld.close();
                        errorLogger.log("closed archive: " + archivedIndexWriterOld.getArchiveFile().getName());
                    } catch (Throwable e) {
                        errorLogger.log(e);
                    }

                    if (networkClient != null && !networkClient.getServerUrl().equals("") && !networkClient.getServerUrl().equals("null")) {
                        File archiveFile = archivedIndexWriterOld.getArchiveFile();
                        try {
                            errorLogger.log("uploading file: " + archiveFile.getAbsolutePath());
                            networkClient.uploadFile(archiveFile.getAbsolutePath(), errorLogger.getPath());
                        } catch (IOException e) {
                            errorLogger.log("failed to upload archive file: " + e.getMessage());
                        } finally {
                            archiveFile.delete();
                        }
                    }

                } catch (InterruptedException e) {
                    errorLogger.log("Archive closer worker was interrupted: " + e.getMessage());
                    break;
                } catch (Exception somethingElse) {
                    errorLogger.log("Archive closer worker was interrupted but not closing: " + somethingElse.getMessage());
                    errorLogger.log(somethingElse);
                }
            }

        }
    }
}