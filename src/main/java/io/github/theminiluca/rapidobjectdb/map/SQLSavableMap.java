package io.github.theminiluca.rapidobjectdb.map;

import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;

import java.sql.SQLException;
import java.util.Map;
/**
 * <h2>SQL Savable Map</h2>
 * A custom map that can save data to sql.
 * @version 2.1.0
 * @since 2.0.8
 * */
public interface SQLSavableMap<K,V> extends Map<K,V> {
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
    void saveMap(SQLConnector connector, String tableName);
    /**
     * Save map with data
     * @param connector SQLConnector
     * @param tableName Name of the table
     * */
    void createTable(SQLConnector connector, String tableName) throws SQLException;
}
