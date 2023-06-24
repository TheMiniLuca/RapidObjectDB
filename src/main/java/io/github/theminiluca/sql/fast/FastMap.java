package io.github.theminiluca.sql.fast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <h3>Deprecated</h3>
 * Use RapidSyncManager and default Map with SQL annotation on.
 * */
@Deprecated
public class FastMap<K,V> implements Map<K,V> {

    private final Map<K, V> root;

    public FastMap() {
        this.root = new HashMap<>();
    }

    protected FastMap(Map<K,V> root) {
        this.root = root;
    }

    @Override
    public int size() {
        return root.size();
    }

    @Override
    public boolean isEmpty() {
        return root.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return root.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return root.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return root.get(key);
    }

    @Override
    public V put(K key, V value) {
        return root.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return root.remove(key);
    }

    @Override
    public void putAll(Map m) {
        root.putAll(m);
    }

    @Override
    public void clear() {
        root.clear();
    }

    @Override
    public Set<K> keySet() {
        return root.keySet();
    }

    @Override
    public Collection<V> values() {
        return root.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return root.entrySet();
    }
}
