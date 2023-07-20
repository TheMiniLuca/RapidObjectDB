package io.github.theminiluca.rapidobjectdb.sql;

import io.github.theminiluca.sql.Logger.SimpleLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static io.github.theminiluca.rapidobjectdb.utils.SQLUtils.keyArrayToString;
import static io.github.theminiluca.rapidobjectdb.utils.SQLUtils.setFormatGenerator;

/**
 * <h2>MariaDB Connector</h2>
 * This class helps to communicate with Maria DataBase Server.
 * @version 2.0.1
 * @since 2.0.0-SNAPSHOT
 * */
public class MariaDBConnector extends SQLConnector{

    private static final String insert = "INSERT INTO `%s` (%s) VALUES (%s);";
    private static final String insertOrUpdate = "INSERT INTO `%s` (%s) VALUES (%s) ON DUPLICATE KEY UPDATE %s;";
    private static final String update = "UPDATE `%s` SET %s;";
    private static final String delete = "DELETE FROM `%s` WHERE %s;";
    private static final String select = "SELECT %s FROM `%s` WHERE %s;";
    private static final String selectALL = "SELECT * FROM `%s`;";

    public MariaDBConnector(String url, String database, int port, String user, String password) {
        super(url, database, port, user, password);
    }

    @Override
    public String insertFormat(String table, String[] keyList, int size) {
        return insert.formatted(table, keyArrayToString(keyList), questionMarkGenerator(size));
    }

    @Override
    public String insertOrUpdate(String table, String[] keyList, int size) {
        return insertOrUpdate.formatted(table, keyArrayToString(keyList), questionMarkGenerator(size), setFormatGenerator(keyList));
    }

    @Override
    public String updateFormat(String table, String[] keyList, int size) {
        return update.formatted(table, setFormatGenerator(keyList));
    }

    @Override
    public String deleteFormat(String table, String key) {
        return delete.formatted(table, "`"+key+"`=?");
    }

    @Override
    public void clearTable(String name) {
        try (PreparedStatement stmt = getNative().prepareStatement("TRUNCATE TABLE `%s`;".formatted(name))) {
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
        return e.getMessage().toLowerCase().contains("connection");
    }

    @Override
    protected Connection openConnection(String url, String database, int port, String user, String password) {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            return DriverManager.getConnection(
                    "jdbc:mariadb://%s:%s/%s".formatted(url, port, database),
                    user, password
            );
        } catch (SQLException e) {
            SimpleLogger.INSTANCE.log(4, "Cannot connect to the MariaDB Server.");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            SimpleLogger.INSTANCE.log(4, "Unable to find MariaDB Driver.");
            throw new RuntimeException(e);
        }
    }
}
