package io.unlogged.logging.perthread;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.index.hash.HashIndex;
import com.googlecode.cqengine.index.radixinverted.InvertedRadixTreeIndex;
import com.googlecode.cqengine.persistence.disk.DiskPersistence;
import com.insidious.common.BloomFilterUtil;
import com.insidious.common.UploadFile;
import com.insidious.common.cqengine.ObjectInfoDocument;
import com.insidious.common.cqengine.StringInfoDocument;
import com.insidious.common.cqengine.TypeInfoDocument;
import io.unlogged.logging.IErrorLogger;
import io.unlogged.logging.perthread.IndexOutputStream;
import orestes.bloomfilter.BloomFilter;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ArchivedIndexWriter implements IndexOutputStream {

    public static final String WEAVE_DAT_FILE = "class.weave.dat";
    public static final String INDEX_TYPE_DAT_FILE = "index.type.dat";
    public static final String INDEX_STRING_DAT_FILE = "index.string.dat";
    public static final String INDEX_OBJECT_DAT_FILE = "index.object.dat";
    public static final String INDEX_EVENTS_DAT_FILE = "index.events.dat";

    private final IErrorLogger errorLogger;
    private final Lock indexWriterLock = new ReentrantLock();
    private final String outputDir;
    private final File currentArchiveFile;
    final private BloomFilter<Long> aggregatedValueSet;
    final private BloomFilter<Integer> aggregatedProbeIdSet;
    private final String classWeavePath;
    private BlockingQueue<StringInfoDocument> stringsToIndex;
    private BlockingQueue<TypeInfoDocument> typesToIndex;
    private BlockingQueue<ObjectInfoDocument> objectsToIndex;
    private ConcurrentIndexedCollection<TypeInfoDocument> typeInfoIndex;
    private ConcurrentIndexedCollection<StringInfoDocument> stringInfoIndex;
    private ConcurrentIndexedCollection<ObjectInfoDocument> objectInfoIndex;
    private DiskPersistence<ObjectInfoDocument, Long> objectInfoDocumentIntegerDiskPersistence;
    private DiskPersistence<StringInfoDocument, Long> stringInfoDocumentStringDiskPersistence;
    private DiskPersistence<TypeInfoDocument, Integer> typeInfoDocumentStringDiskPersistence;
    private List<UploadFile> fileListToUpload = new LinkedList<>();
    private ZipOutputStream archivedIndexOutputStream;

    public ArchivedIndexWriter(File archiveFile, String classWeaveFileStream, IErrorLogger errorLogger) throws IOException {
        this.errorLogger = errorLogger;
        this.classWeavePath = classWeaveFileStream;
        outputDir = archiveFile.getParent() + "/";
        this.currentArchiveFile = archiveFile;

        initIndexQueues();

        errorLogger.log("prepare index archive: " + currentArchiveFile.getName());
        archivedIndexOutputStream = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(currentArchiveFile)));
        aggregatedValueSet = BloomFilterUtil.newBloomFilterForValues(BloomFilterUtil.BLOOM_AGGREGATED_FILTER_BIT_SIZE);
        aggregatedProbeIdSet = BloomFilterUtil.newBloomFilterForProbes(
                BloomFilterUtil.BLOOM_AGGREGATED_FILTER_BIT_SIZE);


        initialiseIndexes();
        errorLogger.log("completed preparing indexes for archive: " + currentArchiveFile.getName());

    }

    public File getArchiveFile() {
        return currentArchiveFile;
    }

    private void initIndexQueues() {
        typesToIndex = new ArrayBlockingQueue<>(1);
        objectsToIndex = new ArrayBlockingQueue<>(1);
        stringsToIndex = new ArrayBlockingQueue<>(1);
    }

    private void initialiseIndexes() {

        String archiveName = currentArchiveFile.getName()
                .split(".zip")[0];

        File typeIndexFile = new File(outputDir + archiveName + "-" + INDEX_TYPE_DAT_FILE);
        File stringIndexFile = new File(outputDir + archiveName + "-" + INDEX_STRING_DAT_FILE);
        File objectIndexFile = new File(outputDir + archiveName + "-" + INDEX_OBJECT_DAT_FILE);

        if (typeIndexFile.exists()) {
            typeIndexFile.delete();
        }
        if (stringIndexFile.exists()) {
            stringIndexFile.delete();
        }
        if (objectIndexFile.exists()) {
            objectIndexFile.delete();
        }

        typeInfoDocumentStringDiskPersistence =
                DiskPersistence.onPrimaryKeyInFile(TypeInfoDocument.TYPE_ID, typeIndexFile);
        stringInfoDocumentStringDiskPersistence =
                DiskPersistence.onPrimaryKeyInFile(StringInfoDocument.STRING_ID, stringIndexFile);
        objectInfoDocumentIntegerDiskPersistence =
                DiskPersistence.onPrimaryKeyInFile(ObjectInfoDocument.OBJECT_ID, objectIndexFile);

        typeInfoIndex = new ConcurrentIndexedCollection<>(typeInfoDocumentStringDiskPersistence);
        stringInfoIndex = new ConcurrentIndexedCollection<>(stringInfoDocumentStringDiskPersistence);
        objectInfoIndex = new ConcurrentIndexedCollection<>(objectInfoDocumentIntegerDiskPersistence);

        typeInfoIndex.addIndex(HashIndex.onAttribute(TypeInfoDocument.TYPE_NAME));
        stringInfoIndex.addIndex(InvertedRadixTreeIndex.onAttribute(StringInfoDocument.STRING_VALUE));
        objectInfoIndex.addIndex(HashIndex.onAttribute(ObjectInfoDocument.OBJECT_TYPE_ID));
    }

    public void drainQueueToIndex(
            List<ObjectInfoDocument> objectsToIndex,
            Queue<TypeInfoDocument> typesToIndex,
            List<StringInfoDocument> stringsToIndex
    ) {
        errorLogger.log("drain queue to index: " + currentArchiveFile.getName() + ":"
                + " [" + objectsToIndex.size() + "]"
                + " [" + typesToIndex.size() + "]"
                + " [" + stringsToIndex.size() + "]"
        );
        long start = System.currentTimeMillis();
        int itemCount = objectsToIndex.size() + typesToIndex.size()  + stringsToIndex.size();

        if (itemCount == 0) {
            return;
        }


        objectInfoIndex.addAll(objectsToIndex);
        typeInfoIndex.addAll(typesToIndex);
        stringInfoIndex.addAll(stringsToIndex);

        long end = System.currentTimeMillis();

        errorLogger.log("Took [" + (end - start) / 1000 + "] seconds to index [" + itemCount + "] items");
    }

    @Override
    public int fileCount() {
        return fileListToUpload.size();
    }


    public void completeArchive(
            BlockingQueue<StringInfoDocument> stringsToIndexTemp,
            BlockingQueue<ObjectInfoDocument> objectsToIndexTemp,
            BlockingQueue<TypeInfoDocument> typesToIndexTemp
    ) {

        indexWriterLock.lock();

        long start = System.currentTimeMillis();
        errorLogger.log("lock acquired to finish archive: " + currentArchiveFile.getName());

        try {


            long endTime = new Date().getTime();


            try {


                ZipEntry classWeaveEntry = new ZipEntry(WEAVE_DAT_FILE);
                archivedIndexOutputStream.putNextEntry(classWeaveEntry);

                FileInputStream weaveInputStream = new FileInputStream(classWeavePath);
                copy(weaveInputStream, archivedIndexOutputStream);
                weaveInputStream.close();
                archivedIndexOutputStream.closeEntry();


                ZipEntry indexEntry = new ZipEntry(INDEX_EVENTS_DAT_FILE);

                archivedIndexOutputStream.putNextEntry(indexEntry);
                DataOutputStream outputStream = new DataOutputStream(archivedIndexOutputStream);

                List<UploadFile> fileIndexBytesCopy = fileListToUpload;
                fileListToUpload = new LinkedList<>();

                outputStream.writeInt(fileIndexBytesCopy.size());
                for (UploadFile fileToUpload : fileIndexBytesCopy) {
                    outputStream.writeInt(fileToUpload.path.length());
                    outputStream.writeBytes(fileToUpload.path);
                    outputStream.writeLong(fileToUpload.threadId);


                    byte[] valueByteArray = new byte[0];
//                            BloomFilterConverter.toJson(fileToUpload.valueIdBloomFilter)
//                            .toString()
//                            .getBytes();
                    byte[] probeByteArray = new byte[0];
//                            BloomFilterConverter.toJson(fileToUpload.probeIdBloomFilter)
//                            .toString()
//                            .getBytes();


                    outputStream.writeInt(valueByteArray.length);
                    outputStream.write(valueByteArray);

                    outputStream.writeInt(probeByteArray.length);
                    outputStream.write(probeByteArray);
                }

                byte[] aggregatedValueFilterSerialized = new byte[0];
//                        BloomFilterConverter.toJson(aggregatedValueSet)
//                        .toString()
//                        .getBytes();
                byte[] aggregatedProbeFilterSerialized = new byte[0];
//                        BloomFilterConverter.toJson(aggregatedProbeIdSet)
//                        .toString()
//                        .getBytes();
//                System.err.println("Aggregated value filter for [" + currentArchiveFile.getName() + "] -> " + aggregatedValueFilterSerialized.length);
//                System.err.println("Aggregated probe filter for [" + currentArchiveFile.getName() +
//                        "] -> " + aggregatedProbeFilterSerialized.length);


                outputStream.writeInt(aggregatedValueFilterSerialized.length);
                outputStream.write(aggregatedValueFilterSerialized);

                outputStream.writeInt(aggregatedProbeFilterSerialized.length);
                outputStream.write(aggregatedProbeFilterSerialized);

                outputStream.writeLong(endTime);
                outputStream.flush();
                archivedIndexOutputStream.closeEntry();

                List<ObjectInfoDocument> pendingObjects = new ArrayList<>(objectsToIndexTemp.size() + 1);
                Queue<TypeInfoDocument> pendingTypes = new ArrayBlockingQueue<>(typesToIndexTemp.size() + 1);
                List<StringInfoDocument> pendingStrings = new ArrayList<>();
                stringsToIndexTemp.drainTo(pendingStrings);
                objectsToIndexTemp.drainTo(pendingObjects);
                typesToIndexTemp.drainTo(pendingTypes);

                drainQueueToIndex(pendingObjects, pendingTypes, pendingStrings);


                String currentArchiveName = currentArchiveFile.getName().split(".zip")[0];


                ZipEntry stringIndexEntry = new ZipEntry(INDEX_STRING_DAT_FILE);
                archivedIndexOutputStream.putNextEntry(stringIndexEntry);
                Path stringIndexFilePath = FileSystems.getDefault()
                        .getPath(outputDir + currentArchiveName + "-" + INDEX_STRING_DAT_FILE);
                Files.copy(stringIndexFilePath, archivedIndexOutputStream);
                stringIndexFilePath.toFile()
                        .delete();
                archivedIndexOutputStream.closeEntry();

                ZipEntry typeIndexEntry = new ZipEntry(INDEX_TYPE_DAT_FILE);
                archivedIndexOutputStream.putNextEntry(typeIndexEntry);
                Path typeIndexFilePath = FileSystems.getDefault()
                        .getPath(outputDir + currentArchiveName + "-" + INDEX_TYPE_DAT_FILE);
                Files.copy(typeIndexFilePath, archivedIndexOutputStream);
                typeIndexFilePath.toFile()
                        .delete();
                archivedIndexOutputStream.closeEntry();

                ZipEntry objectIndexEntry = new ZipEntry(INDEX_OBJECT_DAT_FILE);
                archivedIndexOutputStream.putNextEntry(objectIndexEntry);
                Path objectIndexFilePath = FileSystems.getDefault()
                        .getPath(outputDir + currentArchiveName + "-" + INDEX_OBJECT_DAT_FILE);
                Files.copy(objectIndexFilePath, archivedIndexOutputStream);
                objectIndexFilePath.toFile().delete();
                archivedIndexOutputStream.closeEntry();

            } catch (IOException e) {
                errorLogger.log(e);
            } finally {
                archivedIndexOutputStream.close();
            }
        } catch (Exception e) {
            errorLogger.log(e);
        } finally {
            long end = System.currentTimeMillis();
            errorLogger.log(
                    "Took [" + ((end - start) / 1000) + "] seconds to complete archive: " + currentArchiveFile.getName());
            try {
                indexWriterLock.unlock();
            } catch (Exception e) {
                e.printStackTrace();
                // whut
            }
        }
    }

    public void close() {
//        shutdown = true;
        completeArchive(stringsToIndex, objectsToIndex, typesToIndex);
    }


    @Override
    public void writeFileEntry(UploadFile logFile) throws IOException {

        long currentTimestamp = System.currentTimeMillis();
        File fileToUpload = new File(logFile.path);
        fileListToUpload.add(logFile);

        String fileName = currentTimestamp + "@" + fileToUpload.getName();

        ZipEntry eventsFileZipEntry = new ZipEntry(fileName);
        archivedIndexOutputStream.putNextEntry(eventsFileZipEntry);
        FileInputStream fis = new FileInputStream(fileToUpload);
        copy(fis, archivedIndexOutputStream);
        fis.close();
        archivedIndexOutputStream.flush();
        archivedIndexOutputStream.closeEntry();
        long end = System.currentTimeMillis();

        errorLogger.log("[" + currentArchiveFile.getName() + "] Add files to archive: " + logFile.path + " " +
                "took - " + (end - currentTimestamp) / 1000 + " ms");
    }

    void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
    }

    public void addValueId(long value) {
//        System.err.println("Add value to aggregated value filter: " + value);
//        aggregatedValueSet.add(value);
    }

    public void addProbeId(int value) {
//        aggregatedProbeIdSet.add(value);
    }
}