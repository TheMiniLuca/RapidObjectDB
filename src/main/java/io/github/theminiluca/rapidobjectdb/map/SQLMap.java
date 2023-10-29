package io.github.theminiluca.rapidobjectdb.map;

import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;

import java.sql.SQLException;
import java.util.Map;

public interface SQLMap<K, V> extends Map<K, V> {
    /**
     * Initialize map with data
     * @param connector SQLConnector
     * @param tableName Name of the table
     * */
    void initialize(SQLConnector connector, String tableName);
    /**
     * Save map with data
     * @param connector SQLConnector
     * @param tableName Name of the table
     * */
    void createTable(SQLConnector connector, String tableName) throws SQLException;
}
