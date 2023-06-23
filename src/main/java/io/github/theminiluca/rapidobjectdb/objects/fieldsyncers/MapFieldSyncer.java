package io.github.theminiluca.rapidobjectdb.objects.fieldsyncers;

import io.github.theminiluca.rapidobjectdb.annotation.SQL;
import io.github.theminiluca.rapidobjectdb.objects.FieldSyncer;
import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;

import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MapFieldSyncer implements FieldSyncer {
    private static final String[] key_value = new String[]{"key","value"};

    @Override
    public Object loadField(SQL sql, SQLConnector connector) {
        Map<Object, Object> m = new HashMap<>();
        try {
            ResultSet set = connector.selectAll(sql.value());
            while (set.next()) {
                m.put(connector.getObject(set.getObject(1)), connector.getObject(set.getObject(2)));
            }
            return m;
        }catch (Exception e) {
            if(e.getCause() instanceof SQLSyntaxErrorException && e.getCause().getMessage().toLowerCase().contains("doesn't exist")) return m;
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveField(SQL sql, Object field, SQLConnector connector) {
        Map<?, ?> m = (Map<?, ?>) field;
        try {
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                connector.insertOrUpdate(sql.value(), key_value, entry.getKey(), entry.getValue());
            }
        }catch (RuntimeException e) {
            try {
                createTable(connector.getNative(), sql.value(), connector.getObjectType(m.values().stream().findAny().get()), connector.getObjectType(m.keySet().stream().findAny().get()));
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static void createTable(Connection connection, String name, String keyType, String valueType) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("CREATE TABLE IF NOT EXISTS %s (`key` %s, `value` %s, UNIQUE INDEX `key` (`key`) USING HASH);".formatted(name, keyType, valueType));
        stmt.execute();
    }
}
