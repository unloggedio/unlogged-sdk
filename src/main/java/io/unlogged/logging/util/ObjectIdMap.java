package io.unlogged.logging.util;

import com.insidious.common.BloomFilterUtil;
import orestes.bloomfilter.BloomFilter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;


/**
 * This object assigns a unique ID to each object reference.
 * Conceptually, this is a kind of IdentityHashMap from Object to long.
 */
public class ObjectIdMap {

    public static final int INT_MAP_CAPACITY = 1024 * 1024;
    private final long nextId;
    private Entry[] entries;
    private int capacity;
    private int threshold;
    private final int andKey;
    private final int size;
    private final int INT_MAX_BIT = 30;
    private final int idCount = 0;


    /**
     * Create an instance.
     *
     * @param initialCapacity is the size of an internal array to manage the contents.
     * @param outputDir location to save the object map, optional
     */
    public ObjectIdMap(int initialCapacity, File outputDir) throws IOException {
        size = 0;
        nextId = 1;

        // To ensure capacity == 0b100...000, so that andKey == 0b111...111
        for (int i = 0; i < INT_MAX_BIT + 1; ++i) {
            capacity = 1 << i;
            if (capacity > initialCapacity) {
                break;
            }
        }
        andKey = 0xffffffff - 1;
//        File objectIdMapFile = new File(outputDir.getAbsolutePath() + "/objectids.dat");
//        objectIdMapFile.delete();

//        recentIdsBuffer = new CircularFifoBuffer(capacity / 4);

//        objectIdContainer = ChronicleMap
//                .of(LongValue.class, LongValue.class)
//                .name("objectid-map")
//                .entries(capacity)
//                .createPersistedTo(objectIdMapFile);
//        threshold = capacity / 2;
//        entries = new Entry[capacity];
        aggregatedProbeIdSet = BloomFilterUtil.newBloomFilterForProbes(initialCapacity);

    }

    BloomFilter<Integer> aggregatedProbeIdSet;

//    CircularFifoBuffer recentIdsBuffer;


    /**
     * Translate an object into an ID.
     *
     * @param o is an object used in the logging target program.
     * @return an ID corresponding to the object.
     * 0 is returned for null.
     */

//    IntIntMap4 objectMap = new IntIntMap4(1024 * 1024, 0.5f);

//    public long getIdOld(Object o) {
//        if (o == null) {
//            return 0L;
//        }
//        idCount++;
//        int id = System.identityHashCode(o);
//
//        int val = objectMap.get(id);
//        if (val == 0) {
//            objectMap.put(id, id);
//            onNewObjectId(o, id);
//        } else {
//            int x = id + 1;
//        }
//        return id;
//
//    }
//
//    ChronicleMap<LongValue, LongValue> objectIdContainer;
//    ThreadLocal<LongValue> keyHolder = ThreadLocal.withInitial(() -> Values.newHeapInstance(LongValue.class));
//    ThreadLocal<LongValue> valueHolder = ThreadLocal.withInitial(() -> Values.newHeapInstance(LongValue.class));
//
//
//    ExecutorService singleExecutor = Executors.newFixedThreadPool(1);
//    public long getIdChronicleMap(Object o) {
//        if (o == null) {
//            return 0L;
//        }
//        LongValue longIdValue = keyHolder.get();
//        int hash = System.identityHashCode(o);
//
////        long index = hash & andKey;
//        longIdValue.setValue(hash);
//        if (objectIdContainer.containsKey(longIdValue)) {
//            return hash;
//        }
//        idCount++;
////        int objectId = hash;
//
////        Entry newEntry = new Entry(o, objectId, objectIdContainer.get(longIdValue), hash);
//
//        if (objectIdContainer.size() > capacity * 0.8 && recentIdsBuffer.size() > (0.6 * (capacity / 4))) {
//            List<Long> idsToRemove = (List<Long>) recentIdsBuffer.stream().collect(Collectors.toList());
//            recentIdsBuffer.clear();
//            if (idsToRemove.size() > 0) {
//                OldIdCleaner idCleaner = new OldIdCleaner(objectIdContainer, idsToRemove);
//                singleExecutor.submit(idCleaner);
//            }
//        }
//        recentIdsBuffer.add(longIdValue);
//
//        try {
//
//            LongValue valueItem = valueHolder.get();
//            valueItem.setValue(hash);
//            objectIdContainer.put(longIdValue, valueItem);
//        } catch (IllegalStateException ise) {
//            System.err.println("Too many objects being generated, recoding will be skip [" + objectIdContainer.size() + "]");
//        }
//
//        onNewObjectId(o, hash);
//
//        return hash;
//    }


    public long getId(Object o) {
        if (o == null) {
            return 0L;
        }
        int hash = System.identityHashCode(o);

        if (aggregatedProbeIdSet.contains(hash)) {
//            System.out.println("Object " + hash + " already present of type " + o.getClass());
            return hash;
        }

        if (aggregatedProbeIdSet.getFalsePositiveProbability() > 0.01) {
            System.out.println("Clearing probe id bloom filter");
            aggregatedProbeIdSet.clear();
        }
        aggregatedProbeIdSet.add(hash);
        onNewObjectId(o, hash);

        return hash;
    }


    /**
     * A placeholder for handling a new object.
     * This method is called when a new object is found, before a new ID is assigned.
     *
     * @param o is the object passed to the getId method.
     */
    protected void onNewObject(Object o) {
    }

    /**
     * A placeholder for handling a new object.
     * This method is called when a new object is found, after a new ID is assigned.
     *
     * @param o  is the object passed to the getId method.
     * @param id is the ID assigned to the object.
     */
    protected void onNewObjectId(Object o, long id) {
    }


//    /**
//     * Translate an object into an ID.
//     *
//     * @param o is an object used in the logging target program.
//     * @return an ID corresponding to the object.
//     * 0 is returned for null.
//     */
//    public synchronized long getSynchronizedId(Object o) {
//        if (o == null) {
//            return 0L;
//        }
//
//        int hash = System.identityHashCode(o);
//
//        // Search the object.  If found, return the registered ID.
//        int index = hash & andKey;
//        Entry e = entries[index];
//        while (e != null) {
//            if (hash == e.hashcode) {
//                return e.objectId;
//            }
//            e = e.next;
//        }
//
//        // If not found, create a new entry for the given object.
//        // First, prepares a new object
//        onNewObject(o);
//
//        // Update an entry.  index is re-computed because andKey may be updated by onNewObject.
//        index = hash & andKey;
//        Entry oldEntry = entries[index];
//        long id = nextId;
//        nextId++;
//        e = new Entry(o, id, oldEntry, hash);
//        entries[index] = e;
//        size++;
//        onNewObjectId(o, id);
//
//        if (size >= threshold) {
//            resize();
//        }
//        return id;
//    }
//
//
//    /**
//     * Enlarge the internal array for entries.
//     */
//    private void resize() {
//        if (capacity == (1 << INT_MAX_BIT)) {
//            capacity = Integer.MAX_VALUE;
//            threshold = Integer.MAX_VALUE;
//            andKey = capacity;
//        } else {
//            capacity = capacity * 2;
//            threshold = threshold * 2;
//            andKey = capacity - 1;
//        }
//
//        Entry[] newEntries = new Entry[capacity];
//        // Copy contents of the hash table
//        for (int from = 0; from < entries.length; ++from) {
//            Entry fromEntry = entries[from];
//            entries[from] = null;
//            while (fromEntry != null) {
//                Entry nextEntry = fromEntry.next;
//                if (fromEntry.hashcode != 0) {
//                    // Copy non-null entries
//                    int index = fromEntry.hashcode & andKey;
//                    fromEntry.next = newEntries[index];
//                    newEntries[index] = fromEntry;
//                } else {
//                    // skip null object entry
//                    fromEntry.next = null;
//                    size--;
//                }
//                fromEntry = nextEntry;
//            }
//        }
//        entries = newEntries;
//    }

    /**
     * @return the number of objects stored in the map.
     */
    public int size() {
        return idCount;
    }

    /**
     * @return the size of the hash table inside the map.
     * This method is declared for debugging.
     */
    public int capacity() {
        return capacity;
    }

    public void close() {

    }

    /**
     * A simple list structure to store a registered object and its ID.
     * ~ approx 20 bytes each object
     */
    public static class Entry implements Serializable {
        //        private final WeakReference<Object> reference;
        private final int hashcode;
        private final long objectId;
        private final WeakReference<Object> reference;
        private final Entry next;

        public Entry(Object o, long id, Entry e, int hashcode) {
            this.reference = new WeakReference<Object>(o);
            this.objectId = id;
            this.next = e;
            this.hashcode = hashcode;
        }
    }

}
