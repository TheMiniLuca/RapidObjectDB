package io.github.theminiluca.rapidobjectdb.map;

import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RELMap<K, V> implements SQLSavableMap<K, V> {
    private static ScheduledExecutorService globalExecutor;

    private final HashMap<K, Object[]> map = new HashMap<>();
    private final ScheduledExecutorService serviceExecutor;
    private final HashSet<K> removalSet = new HashSet<>();
    private String tableName;
    private SQLConnector connector;
    private final long delay;
    private final TimeUnit unit;

    public RELMap(ScheduledExecutorService serviceExecutor, long delay, TimeUnit unit) {
        this.serviceExecutor = (serviceExecutor==null? (globalExecutor==null?globalExecutor=Executors.newScheduledThreadPool(1):globalExecutor):serviceExecutor);
        this.delay = delay;
        this.unit = unit;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        return connector.contains(this.tableName, new String[]{"key"}, key);
    }

    @Override
    public boolean containsValue(Object value) {
        return connector.contains(this.tableName, new String[]{"value"}, value);
    }

    @Override
    public V get(Object key) {
        if(!map.containsKey(key)) return load(key);
        else {
            ((ScheduledFuture<?>) map.get(key)[1]).cancel(true);
            map.get(key)[1] = serviceExecutor.schedule(() -> map.remove(key), delay, unit);
            return (V) map.get(key)[0];
        }
    }

    @Override
    public V put(K key, V value) {
        return map.put();
    }

    @Override
    public V remove(Object key) {
        return new Object[0];
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<K> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }


    private V load(Object o) {

    }

    @Override
    public void initialize(SQLConnector connector, String tableName) {
        this.connector = connector;
        this.tableName = tableName;
    }

    @Override
    public void createTable(SQLConnector connector, String tableName) throws SQLException {

    }

    @Override
    public void saveMap(SQLConnector connector, String tableName) {

    }
}
