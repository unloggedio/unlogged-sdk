package io.unlogged.logging.util;

//import net.openhft.chronicle.core.values.LongValue;
//import net.openhft.chronicle.map.ChronicleMap;
//import net.openhft.chronicle.values.Values;

public class OldIdCleaner {
//    private final ChronicleMap<LongValue, LongValue> objectIdContainer;
//    private List<Long> recentIdsBuffer;
//
//    public OldIdCleaner(ChronicleMap<LongValue, LongValue> objectIdContainer, List<Long> recentIdsBuffer) {
//
//        this.objectIdContainer = objectIdContainer;
//        this.recentIdsBuffer = recentIdsBuffer;
//    }
//
//    public void run() {
//        Set<Long> set = objectIdContainer.keySet().stream().map(LongValue::getValue).collect(Collectors.toSet());
//        set.removeAll(recentIdsBuffer);
//        LongValue longVal = Values.newHeapInstance(LongValue.class);
//        set.forEach(idToRemove -> {
//            longVal.setValue(idToRemove);
//            objectIdContainer.remove(longVal);
//        });
//        longVal.close();
//        System.out.println(objectIdContainer.size() + " keys remain after removing " + recentIdsBuffer.size() + " keys ");
//    }
}
