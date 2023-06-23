package io.github.theminiluca.rapidobjectdb.objects;

import io.github.theminiluca.rapidobjectdb.annotation.SQL;
import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;

/**
 * <h2>Field Syncer</h2><br/>
 * Field Syncer helps to loadField from the SQL and saveField to the SQL.
 * @since 2.0.0-SNAPSHOT
 * */
public interface FieldSyncer {
    /**
     * Load value from the SQL
     * @param sql SQL annotation
     * @param connector SQL Connector
     * @return Object that is type of field
     * */
    Object loadField(SQL sql, SQLConnector connector);
    /**
     * Upload value to the SQL
     * @param sql SQL annotation
     * @param field Field like a {@code HashMap}
     * @param connector SQL Connector
     * */
    void saveField(SQL sql, Object field, SQLConnector connector);
}
