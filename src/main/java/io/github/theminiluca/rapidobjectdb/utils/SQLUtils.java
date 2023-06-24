package io.github.theminiluca.rapidobjectdb.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLUtils {
    public static void createTable(Connection connection, String name, String keyType, String valueType) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("CREATE TABLE IF NOT EXISTS %s (`key` %s, `value` %s, UNIQUE INDEX `key` (`key`) USING HASH);".formatted(name, keyType, valueType));
        stmt.execute();
    }
}
