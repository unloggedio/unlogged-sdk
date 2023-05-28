package io.unlogged.logging.perthread;


import io.unlogged.logging.IErrorLogger;

import java.io.OutputStream;
import java.util.Map;
import java.util.function.Function;

class FileEventCountThresholdChecker implements Runnable {

    private final Map<Integer, OutputStream> threadFileMap;
    private final ThreadEventCountProvider threadEventCountProvider;
    private final Function<Integer, Void> onExpiryRunner;
    private final IErrorLogger errorLogger;

    public FileEventCountThresholdChecker(
            Map<Integer, OutputStream> threadFileMap,
            ThreadEventCountProvider threadEventCountProvider,
            Function<Integer, Void> onExpiryRunner,
            IErrorLogger errorLogger) {
        this.errorLogger = errorLogger;
        assert onExpiryRunner != null;
        this.threadEventCountProvider = threadEventCountProvider;
        this.threadFileMap = threadFileMap;
        this.onExpiryRunner = onExpiryRunner;
    }

    @Override
    public void run() {
        Integer[] keySet = threadFileMap.keySet().toArray(new Integer[0]);
//        errorLogger.log("started event count checker cron for threads: " + Arrays.toString(keySet));
        for (Integer theThreadId : keySet) {
            int eventCount = threadEventCountProvider.getThreadEventCount(theThreadId).get();
            if (eventCount > 0) {
//                errorLogger.log("thread [" + theThreadId + "] has [" + eventCount + "] events, flushing");
                onExpiryRunner.apply(theThreadId);
            }
        }
    }

    public void shutdown() {
        Integer[] keySet = threadFileMap.keySet().toArray(new Integer[0]);
//        errorLogger.log("started event count checker cron for threads: " + Arrays.toString(keySet));
        for (Integer theThreadId : keySet) {
            int eventCount = threadEventCountProvider.getThreadEventCount(theThreadId).get();
            onExpiryRunner.apply(theThreadId);
        }
    }

}