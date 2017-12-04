package ru.nsu.ccfit.bogush.util;

import java.util.HashMap;
import java.util.Map;

public class BlockingHashMap<K, V> {
    private HashMap<K, V> map;

    private boolean blocking = true;

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean b) {
        blocking = b;
    }

    public BlockingHashMap() {
        map = new HashMap<>();
    }

    public BlockingHashMap(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    public BlockingHashMap(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    public BlockingHashMap(Map<? extends K, ? extends V> m) {
        map = new HashMap<>(m);
    }

    public synchronized V take(K key) throws InterruptedException {
        // TODO: 12/4/17 unblock if no more packets expected
        while (!map.containsKey(key) && blocking) {
            this.wait();
        }
        return map.remove(key);
    }

    public synchronized boolean put(K key, V value) {
        boolean updated = null == map.put(key, value);
        if (updated) this.notifyAll();
        return updated;
    }

    public synchronized boolean remove(V packet) {
        return map.values().remove(packet);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }
}
