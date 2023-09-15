package io.github.theminiluca.rapidobjectdb.sql;

import io.github.theminiluca.sql.Logger.SimpleLogger;
import org.sqlite.SQLiteException;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static io.github.theminiluca.rapidobjectdb.utils.SQLUtils.keyArrayToString;
import static io.github.theminiluca.rapidobjectdb.utils.SQLUtils.setFormatGenerator;

/**
 * <h2>MariaDB Connector</h2>
 * This class helps to communicate with SQLite file
 * @version 2.0.2
 * @since 2.0.2-SNAPSHOT
 * */
public class SQLiteConnector extends SQLConnector {

    private static final String insert = "INSERT INTO `%s` (%s) VALUES (%s);";
    private static final String insertOrUpdate = """
            INSERT INTO `%s` (%s) VALUES (%s) ON CONFLICT(`%s`) DO UPDATE SET %s;
            """;
    private static final String update = "UPDATE `%s` SET %s;";
    private static final String delete = "DELETE FROM `%s` WHERE %s;";
    private static final String select = "SELECT %s FROM `%s` WHERE %s;";
    private static final String selectALL = "SELECT * FROM `%s`;";

    private int updateCount = 0;
    private final Thread committee = new Thread(() -> {
        while (true) {
            if(updateCount > 1) updateCount = 1;
            else if (updateCount == 1){
                try {
                    getNative().commit();
                } catch (SQLException e) {
                    if(e.getMessage().contains("connection closed")) return;
                    throw new RuntimeException(e);
                }
                updateCount = 0;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }
    });

    public SQLiteConnector(File path) {
        super(path.getAbsolutePath(), null, -1, null, null);
        try {
            getNative().setAutoCommit(false);
            committee.start();

            getNative().nativeSQL("PRAGMA journal_mode=WAL;");
            getNative().nativeSQL("PRAGMA synchronous=NORMAL;");
            getNative().commit();
        } catch (SQLException e) {
            committee.interrupt();
            try {
                close();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearTable(String name) throws SQLException {
        try (PreparedStatement stmt = getNative().prepareStatement("DELETE FROM `%s`;".formatted(name))) {
            stmt.execute();
        }
    }

    @Override
    public String insertFormat(String table, String[] keyList, int size) {
        updateCount++;
        return insert.formatted(table, keyArrayToString(keyList), questionMarkGenerator(size));
    }

    @Override
    public String insertOrUpdate(String table, String[] keyList, int size) {
        updateCount++;
        return insertOrUpdate.formatted(table, keyArrayToString(keyList), questionMarkGenerator(size), keyList[0], setFormatGenerator(keyList));
    }

    @Override
    public String updateFormat(String table, String[] keyList, int size) {
        updateCount++;
        return update.formatted(table, setFormatGenerator(keyList));
    }

    @Override
    public String deleteFormat(String table, String key) {
        updateCount++;
        return delete.formatted(table, "`"+key+"`=?");
    }

    @Override
    public String selectFormat(String table, String[] toSelect, String[] keyList) {
        return select.formatted(keyArrayToString(toSelect), table, setFormatGenerator(keyList));
    }

    @Override
    public String selectAllFormat(String table) {
        return selectALL.formatted(table);
    }

    @Override
    public boolean isConnectionError(SQLException e) {
        return false;
    }

    @Override
    protected Connection openConnection(String url, String database, int port, String user, String password) {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(
                    "jdbc:sqlite:%s".formatted(url)
            );
        } catch (SQLException e) {
            SimpleLogger.INSTANCE.log(4, "Cannot connect open File.");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            SimpleLogger.INSTANCE.log(4, "Unable to find SQLite Driver.");
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getObjectType(Object o) {
        String objectType = super.getObjectType(o);
        if(objectType.contains("TEXT")) return "TEXT";
        else if(objectType.contains("INT")) return "INTEGER";
        return objectType;
    }

    @Override
    public void close() throws SQLException {
        committee.interrupt();
        getNative().commit();
        super.close();
    }
}
