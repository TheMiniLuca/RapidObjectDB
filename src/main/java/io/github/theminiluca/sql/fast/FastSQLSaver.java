package io.github.theminiluca.sql.fast;

import io.github.theminiluca.sql.Logger.SimpleLogger;
import io.github.theminiluca.sql.SQL;
import io.github.theminiluca.sql.SQLMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FastSQLSaver {

    private Connection connection;
    private ScheduledExecutorService backupService = new ScheduledThreadPoolExecutor(4);
    private final List<Object> dataClasses = new ArrayList<>();

    public FastSQLSaver(String host, String database, String user, String password) throws SQLException {
        connect(host, user, password, database);
    }

    public void connect(String host, String user, String password, String database) throws SQLException {
        System.out.println("Trying to connect with the database...");
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mariadb://"+host+"/"+database+"?connectTimeout=0&socketTimeout=0&autoReconnect=true",
                    user,
                    password
            );
            createFastMapInfoTable();
        }catch (SQLException e) {
            System.out.println("Failed to connect with database.");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            System.out.println("Success to connect with database.");
        }
    }

    private void createFastMapInfoTable() throws SQLException {
        connection.prepareStatement("""
            CREATE TABLE IF NOT EXISTS `!fastmaps` (
                `table_name` TINYTEXT NOT NULL COLLATE 'utf8mb4_general_ci',
                `key_class` TINYTEXT NOT NULL COLLATE 'utf8mb4_general_ci',
                `value_class` TINYTEXT NOT NULL COLLATE 'utf8mb4_general_ci',
                UNIQUE INDEX `table_name` (`table_name`) USING HASH
            )
            COLLATE='utf8mb4_general_ci'
            ENGINE=InnoDB
            ;
        """).execute();
    }

    public void registerBackupTask(Object o, int delay, TimeUnit unit) {
        loadMapWithObject(o);
        dataClasses.add(o);
        backupService.scheduleWithFixedDelay(() -> saveMapWithObject(o), delay, delay, unit);
    }

    private void backupMap(String name, Map<String,Object> map) {
        try {
            clean(name);
            String st = "INSERT INTO `%s` (`key`, `value`) VALUES (?, ?);".formatted(name);
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                PreparedStatement stmt = connection.prepareStatement(st);
                stmt.setString(1, entry.getKey());
                stmt.setString(2, serializeObject(entry.getValue()));
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void clean(String name) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("TRUNCATE TABLE `%s`;".formatted(name));
        stmt.execute();
    }

    private void saveMapWithObject(Object o) {
        for (Field field : o.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SQL.class) && field.getType().isAssignableFrom(FastMap.class)) {
                try {
                    field.setAccessible(true);
                    SQL annotation = field.getAnnotation(SQL.class);
                    SimpleLogger.INSTANCE.logf(0, "Found SQL annotation in %s (%s)", o.getClass().getName(), annotation.value());
                    createMap(annotation.value());
                    backupMap(annotation.value(), (FastMap<String, Object>) field.get(o));
                } catch (ClassCastException | IllegalAccessException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void loadMapWithObject(Object o) {
        for (Field field : o.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SQL.class) && field.getType().isAssignableFrom(FastMap.class)) {
                try {
                    field.setAccessible(true);
                    SQL annotation = field.getAnnotation(SQL.class);
                    SimpleLogger.INSTANCE.logf(0, "Found SQL annotation in %s (%s)", o.getClass().getName(), annotation.value());
                    createMap(annotation.value());
                    field.set(o, new FastMap<>(loadMap(annotation.value())));
                } catch (ClassCastException | IllegalAccessException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private Map<String,Object> loadMap(String name) throws SQLException {
        Map<String, Object> o = new HashMap<>();
        ResultSet set = connection.prepareStatement("SELECT * FROM `%s`;".formatted(name)).executeQuery();
        while (set.next()) {
            o.put(set.getString(1), set.getString(2));
        }
        return o;
    }

    private String serializeObject(Object o) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(o);
        os.flush();
        os.close();
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    private void createMap(String table) throws SQLException {
        connection.prepareStatement("""
            CREATE TABLE IF NOT EXISTS `%s` (
                `key` TEXT NOT NULL COLLATE 'utf8mb4_general_ci',
                `value` TEXT NOT NULL COLLATE 'utf8mb4_general_ci'
            )
            COLLATE='utf8mb4_general_ci'
            ENGINE=InnoDB
            ;
        """.formatted(table)).execute();
    }

    public void close() {
        backupService.shutdown();
        for(Object o : dataClasses) {
            saveMapWithObject(o);
        }
    }
}
