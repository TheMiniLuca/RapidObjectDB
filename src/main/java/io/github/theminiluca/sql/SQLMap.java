package io.github.theminiluca.sql;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class SQLMap<K,V> implements Map<K,V>, Serializable {

    private final Map<K,V> root;
    protected Queue<Object> removeQueue = new LinkedBlockingQueue<>();
    protected Queue<K> updatedKey = new LinkedBlockingQueue<>();
    protected Queue<K> gotKey = new LinkedBlockingQueue<>();

    public SQLMap() {
        this.root = new HashMap<>();
    }

    public SQLMap(Map<K,V> root) {
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
        if(gotKey != null && !gotKey.contains(key)) gotKey.add((K) key);
        return root.get(key);
    }

    @Override
    public String toString() {
        return root.toString();
    }

    @Override
    public V put(K key, V value) {
        addUpdatedKey(key);
        return root.put(key, value);
    }

    @Override
    public V remove(Object key) {
        if(removeQueue != null) removeQueue.offer(key);
        return root.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        addAllUpdatedKey(m.keySet());
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

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        addUpdatedKey(key);
        return root.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        addUpdatedKey(key);
        return root.replace(key, value);
    }

    private void addUpdatedKey(K key) {
        if(updatedKey!=null && !updatedKey.contains(key)) updatedKey.add(key);
    }

    private void addAllUpdatedKey(Set<? extends K> keys) {
        for (K key : keys) {
            addUpdatedKey(key);
        }
    }
}
