package io.github.theminiluca.rapidobjectdb.objects.fieldsyncers;

import io.github.theminiluca.rapidobjectdb.annotation.SQL;
import io.github.theminiluca.rapidobjectdb.objects.FieldSyncer;
import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;
import org.sqlite.SQLiteException;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static io.github.theminiluca.rapidobjectdb.utils.SQLUtils.createTable;

/**
 * <strong>Map Field Syncer</strong><br/><br/>
 * Map Field Syncer is a one of Field Syncer that is pre-built in this library.
 * This Field Syncer saves Map data to sql.
 * @version 2.0.1
 * @since 2.0.0-SNAPSHOT
 * */
public class MapFieldSyncer implements FieldSyncer {
    private static final String[] key_value = new String[]{"key","value"};

    @Override
    public Object loadField(SQL sql, SQLConnector connector) {
        Map<Object, Object> m = new HashMap<>();
        try (ResultSet set = connector.selectAll(sql.value())) {
            while (set.next()) {
                m.put(connector.getObject(set.getObject(1)), connector.getObject(set.getObject(2)));
            }
            return m;
        }catch (Exception e) {
            if(
                    (e.getCause() instanceof SQLSyntaxErrorException && e.getCause().getMessage().toLowerCase().contains("doesn't exist"))
                    || (e.getCause() instanceof SQLiteException && e.getCause().getMessage().toLowerCase().contains("missing database"))
            ) return m;
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveField(SQL sql, Object field, SQLConnector connector) {
        Map<?, ?> m = (Map<?, ?>) field;
        try {
            connector.clearTable(sql.value());
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                connector.insertOrUpdate(sql.value(), key_value, entry.getKey(), entry.getValue());
            }
        }catch (RuntimeException e) {
            try {
                createTable(connector, sql.value(), connector.getObjectType(m.values().stream().findAny().get()), connector.getObjectType(m.keySet().stream().findAny().get()));
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
