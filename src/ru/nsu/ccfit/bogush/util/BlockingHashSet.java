package ru.nsu.ccfit.bogush.util;

import java.util.Collection;
import java.util.HashSet;

public class BlockingHashSet<E> {
    private final HashSet<E> set;

    public BlockingHashSet() {
        set = new HashSet<>();
    }

    public BlockingHashSet(Collection<? extends E> c) {
        set = new HashSet<>(c);
    }

    public BlockingHashSet(int initialCapacity, float loadFactor) {
        set = new HashSet<>(initialCapacity, loadFactor);
    }

    public BlockingHashSet(int initialCapacity) {
        set = new HashSet<>(initialCapacity);
    }

    public synchronized E take(E value) throws InterruptedException {
        while (!set.contains(value)) {
            this.wait();
        }
        return this.take(value);
    }

    public synchronized boolean put(E value) {
        boolean updated = set.add(value);
        if (updated) this.notifyAll();
        return updated;
    }
}
