package io.unlogged.logging.perthread;

import java.util.concurrent.atomic.AtomicInteger;

public interface ThreadEventCountProvider {
    AtomicInteger getThreadEventCount(int currentThreadId) ;
}
