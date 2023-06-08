package io.unlogged.weaver;

import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.Descriptor;
import com.insidious.common.weaver.EventType;

public class DataInfoProvider {

    /**
     * Create a new Data ID and record the information.
     *
     * @param line             specifies the line number.
     * @param instructionIndex specifies the location of the instruction in the ASM's InsnList object.
     * @param eventType        is an event type (decided by the instruction type).
     * @param valueDesc        specifies a data type of the value observed by the event.
     * @param attributes       specifies additional static information obtained from bytecode.
     * @return the next available id which can be used for the dataInfo
     */
    public int nextDataId(int line, int instructionIndex, EventType eventType, Descriptor valueDesc, String attributes) {
//        DataInfo entry = new DataInfo(classId, methodId - 1, dataId, line, instructionIndex, eventType, valueDesc,
//                attributes);
//        dataEntries.add(entry);
//        return dataId++;
        return 0;
    }


}
