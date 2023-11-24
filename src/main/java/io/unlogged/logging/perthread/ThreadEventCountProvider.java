package io.unlogged.logging.perthread;

public interface ThreadEventCountProvider {
    int getThreadEventCount(int currentThreadId) ;
}
