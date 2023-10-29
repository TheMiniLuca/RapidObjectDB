package io.github.theminiluca.rapidobjectdb.map;

import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;
import io.github.theminiluca.rapidobjectdb.utils.SQLUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * <h2>SQL Based Map</h2>
 * As you know, SQL-Based Map is Map that does not store any data in Java but It store and load from the SQL Server.
 * @since 2.1.0
 * */
public class LazyMap<K, V> extends HashMap<K, V> implements SQLSavableMap<K, V> {
    private final static String[] keyL = new String[]{"key", "value"};

    private String tableName;
    private final HashSet<K> removalSet = new HashSet<>();
    private final Class<? extends K> k;
    private final Class<? extends V> v;
    private SQLConnector connector;

    public LazyMap(Class<? extends K> k, Class<? extends V> v) {
        this.k = k;
        this.v = v;
    }

    @Override
    public void initialize(SQLConnector connector, String tableName) {
        this.connector = connector;
        this.tableName = tableName;
    }

    @Override
    public void saveMap(SQLConnector connector, String tableName) throws RuntimeException {
        for (Object k : removalSet) {
            connector.delete(tableName, "key", k);
        }
        for (Entry<K, V> entry : super.entrySet()) {
            connector.insertOrUpdate(tableName, keyL, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void createTable(SQLConnector connector, String tableName) throws SQLException {
        SQLUtils.createTable(connector, tableName, connector.getObjectTypeOfClass(k), connector.getObjectTypeOfClass(v));
    }

    @Override
    public V put(K key, V value) {
        removalSet.remove(key);
        return super.put(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        removalSet.remove(key);
        return super.putIfAbsent(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        super.putAll(m);
    }


    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key) || connector.contains(tableName, new String[]{"key"}, key);
    }

    @Override
    public boolean containsValue(Object value) {
        return super.containsValue(value) || connector.contains(tableName, new String[]{"value"}, value);
    }

    @Override
    public V get(Object key) {
        if(!removalSet.contains((K)key) && !super.containsKey(key) && connector.contains(tableName, new String[]{"key"}, key)) {
            ResultSet set = connector.select(tableName, new String[]{"value"}, new String[]{"key"}, key);
            try {
                set.first();
                put((K) key, (V) connector.getObject(set.getObject(1)));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return super.get(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        if(v!=null) return v;
        else return defaultValue;
    }

    @Override
    public V remove(Object key) {
        removalSet.add((K) key);
        return super.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value) {
        removalSet.add((K) key);
        return super.remove(key, value);
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("LazyMap#size() is not supported due to performance issue.");
    }

    @Override
    public void clear() {
        try {
            connector.clearTable(tableName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        super.clear();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("LazyMap#keySet() is not supported due to performance issue.");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("LazyMap#values() is not supported due to performance issue.");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("LazyMap#entrySet() is not supported due to performance issue.");
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        throw new UnsupportedOperationException("LazyMap#forEach(BiConsumer<? super K, ? super V> action) is not supported due to performance issue.");
    }
}
