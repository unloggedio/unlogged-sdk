package io.unlogged;

public class Pair<U, V> {
    U u;
    V v;

    public Pair(U u, V v) {
        this.u = u;
        this.v = v;
    }

    public U getFirst() {
        return u;
    }

    public V getSecond() {
        return v;
    }
}
