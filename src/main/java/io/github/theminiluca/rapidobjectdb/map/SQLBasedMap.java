package io.github.theminiluca.rapidobjectdb.map;

import io.github.theminiluca.rapidobjectdb.RapidSyncManager;
import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static io.github.theminiluca.rapidobjectdb.utils.SQLUtils.createTable;

/**
 * <h2>SQL Based Map</h2>
 * As you know, SQL-Based Map is Map that does not store any data in Java but It store and load from the SQL Server.
 * @since 2.0.2
 * */
public class SQLBasedMap<K,V> implements Map<K,V> {

    private final String tableName;
    private final SQLConnector connector;
    private final Class<? extends K> k;
    private final Class<? extends V> v;

    public SQLBasedMap(String tableName, Class<? extends K> k, Class<? extends V> v, RapidSyncManager syncManager) throws SQLException {
        this.tableName = tableName;
        this.connector = syncManager.getConnector();
        this.k = k;
        this.v = v;
        createTable(connector, tableName, connector.getObjectType(k), connector.getObjectType(v));
    }

    @Override
    public int size() {
        try {
            return connector.getNative().prepareStatement("SELECT COUNT(*) FROM %s;".formatted(tableName)).executeQuery().getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        try (PreparedStatement preparedStatement = connector.getNative().prepareStatement("SELECT 1 FROM %s WHERE `key`=?;".formatted(tableName))) {
            connector.setPreparedValues(preparedStatement, key);
            return preparedStatement.executeQuery().first();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        try (PreparedStatement preparedStatement = connector.getNative().prepareStatement("SELECT 1 FROM %s WHERE `value`=?;".formatted(tableName))) {
            connector.setPreparedValues(preparedStatement, value);
            return preparedStatement.executeQuery().first();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public V get(Object key) {
        ResultSet set = connector.select(tableName, new String[]{"value"}, new String[]{"key"}, key);
        try {
            set.first();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try {
            return (V) connector.getObject(set.getObject(1));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public V put(K key, V value) {
        connector.insertOrUpdate(tableName, new String[]{"key", "value"}, key, value);
        return null;
    }

    @Override
    public V remove(Object key) {
        connector.delete(tableName, "key", key);
        return null;
    }

    @Override
    public void clear() {
        connector.clearTable(tableName);
    }

    @Override
    public Set<K> keySet() {
        try (ResultSet set = connector.selectAll(tableName)) {
            List<K> kl = new ArrayList<>();
            while (set.next()) {
                kl.add((K) connector.getObject(set.getObject(1)));
            }
            return Set.copyOf(kl);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<V> values() {
        ResultSet set = connector.selectAll(tableName);
        try {
            List<V> vl = new ArrayList<>();
            while (set.next()) {
                vl.add((V) connector.getObject(set.getObject(2)));
            }
            return Set.copyOf(vl);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        ResultSet set = connector.selectAll(tableName);
        try {
            List<Entry<K,V>> kl = new ArrayList<>();
            while (set.next()) {
                kl.add(Map.entry(
                        (K)connector.getObject(set.getObject(1)),
                        (V)connector.getObject(set.getObject(2))
                ));
            }
            return Set.copyOf(kl);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("Not supported");
    }
}
