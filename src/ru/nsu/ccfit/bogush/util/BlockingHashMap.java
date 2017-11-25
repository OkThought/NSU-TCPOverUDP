package ru.nsu.ccfit.bogush.util;

import java.util.HashMap;
import java.util.Map;

public class BlockingHashMap<K, V> {
    private HashMap<K, V> map;

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

    public synchronized V take(Object key) throws InterruptedException {
        while (!map.containsKey(key)) {
            this.wait();
        }
        return map.get(key);
    }

    public synchronized boolean put(K key, V value) {
        boolean updated = null == map.put(key, value);
        if (updated) this.notifyAll();
        return updated;
    }
}
