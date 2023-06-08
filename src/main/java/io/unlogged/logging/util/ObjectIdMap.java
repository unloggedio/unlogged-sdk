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


    public long getId(Object o) {
        if (o == null) {
            return 0L;
        }
        int hash = System.identityHashCode(o);

        if (aggregatedProbeIdSet.contains(hash)) {
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
