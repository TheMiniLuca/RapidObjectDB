package io.github.theminiluca.rapidobjectdb.utils;

import io.github.theminiluca.rapidobjectdb.sql.MariaDBConnector;
import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;
import io.github.theminiluca.rapidobjectdb.sql.SQLiteConnector;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLUtils {
    public static void createTable(SQLConnector connector, String name, String keyType, String valueType) throws SQLException {
        System.out.println(name);
        PreparedStatement stmt = null;
        if(connector instanceof MariaDBConnector) {
            stmt = connector.getNative().prepareStatement("CREATE TABLE IF NOT EXISTS %s (`key` %s, `value` %s, UNIQUE INDEX `key` (`key`) USING HASH);".formatted(name, keyType, valueType));
        }else if(connector instanceof SQLiteConnector) {
            if(keyType.contains("TEXT")) keyType = "TEXT";
            if(valueType.contains("TEXT")) valueType = "TEXT";
            stmt = connector.getNative().prepareStatement("CREATE TABLE IF NOT EXISTS %s (`key` %s UNIQUE, `value` %s);".formatted(name, keyType, valueType));
        }
        stmt.execute();
    }

    public static String keyArrayToString(String[] keys) {
        StringBuilder sb = new StringBuilder();
        for(String key : keys) {
            sb.append("`").append(key).append("`,");
        }
        return sb.deleteCharAt(sb.length()-1).toString();
    }

    public static String setFormatGenerator(String[] keys) {
        StringBuilder sb = new StringBuilder();
        for(String key : keys) {
            sb.append("`").append(key).append("`=?,");
        }
        return sb.deleteCharAt(sb.length()-1).toString();
    }
}
