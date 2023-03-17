package io.github.theminiluca.sql;

import io.github.theminiluca.sql.Logger.SimpleLogger;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class SQLSyncManager {

    //SQL 관련
    SQLMan sqlManager;


    //Manager 관련
    private HashMap<Object, Thread> autoSaveThreads = new HashMap<>();

    public SQLSyncManager(String url, int port, String database, String user, String pw) {
        sqlManager = new SQLMan(url, port, database, user, pw);
    }

    public void connectSQL() {
        sqlManager.connect();
    }

    /**
     * Schedules Auto-Save task
     *
     * @param dataClass class
     * @param period period
     * */
    public void scheduleAutoSaveTask(Object dataClass, long period) {
        startupLoad(dataClass);
        autoSaveThreads.put(dataClass, new Thread(() -> {
            while (true) {
                saveMapWithClass(dataClass);
                try {
                    Thread.sleep(period);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }));
        autoSaveThreads.get(dataClass).start();
    }

    public void cancelAutoSaveTask(Object dataClass) {
        autoSaveThreads.get(dataClass).stop();
        autoSaveThreads.remove(dataClass);
    }

    private void saveMapWithClass(Object dataClass){
        for (Field field : dataClass.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Save.class)) {
                try {
                    sqlManager.createTable(field.getAnnotation(Save.class).tableName());
                    SQLMap<String, SQLObject> hash = (SQLMap<String, SQLObject>) field.get(dataClass);
                    saveMap(field.getAnnotation(Save.class).tableName(), hash);
                } catch (ClassCastException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void startupLoad(Object dataClass) {
        for (Field field : dataClass.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Save.class)) {
                try {
                    sqlManager.createTable(field.getAnnotation(Save.class).tableName());
                    field.set(dataClass, loadMap(field.getAnnotation(Save.class).tableName()));
                } catch (ClassCastException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void saveMap(String name, SQLMap<String, SQLObject> map) {
        try {
            removeKeys(name, map.removeQueue);
            String key;
            while ((key = map.updatedKey.poll()) != null) {
                if (sqlManager.doesItExist(name, key)) {
                    sqlManager.update(name, key, serialize(map.get(key)));
                }else {
                    sqlManager.insert(name, key, serialize(map.get(key)));
                }
            }
        }catch (SQLException e) {
            SimpleLogger.INSTANCE.log(3, "Unable to perform insert/update.");
            throw new RuntimeException(e);
        }
    }

    private SQLMap<String, SQLObject> loadMap(String name) {
        try {
            HashMap<String, SQLObject> map = new HashMap<>();

            ResultSet set = sqlManager.execute("SELECT * FROM `%s`;".formatted(name));
            while (set.next()) {
                map.put(set.getString(1), deserialize(set.getString(2)));
            }
            return new SQLMap<>(map);
        } catch (SQLException e) {
            SimpleLogger.INSTANCE.log(3, "Unable to perform select.");
            throw new RuntimeException(e);
        }
    }

    private void removeKeys(String name, Queue<Object> keys) {
        String key;
        while ((key = (String) keys.poll()) != null) {
            try {
                sqlManager.delete(name, key);
            } catch (SQLException e) {
                SimpleLogger.INSTANCE.logf(2, "Unable to delete key(%s).", key);
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends SQLObject> String serialize(T t) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(t);
            out.flush();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends SQLObject> T deserialize(String base64) {
        ByteArrayInputStream bipt = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        ObjectInputStream ipt = null;
        try {
            ipt = new ObjectInputStream(bipt);
            return (T) ipt.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (ipt != null) ipt.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

    public void close() {
        SimpleLogger.INSTANCE.log(0, "Auto-Saving maps.");
        for(Object key : autoSaveThreads.keySet()) {
            autoSaveThreads.get(key).stop();
            saveMapWithClass(key);
        }
        try {
            sqlManager.close();
            SimpleLogger.INSTANCE.log(0, "Success to close SQL Connection.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
class SQLMan {
    private Connection connection;

    private final String url;
    private final int port;
    private final String database;
    private final String user;
    private final String password;

    private final String KEY_COLUMNS_NAME = "key";
    private final String VALUE_COLUMNS_NAME = "value";

    public SQLMan(String url, int port, String database, String user, String password) {
        this.url = url;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public void connect() {
        SimpleLogger.INSTANCE.logf(0, "Connecting to MariaDB Server... (%s:%s)", url, port);
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mariadb://%s:%s/%s".formatted(url, port, database),
                    user, password
            );
            SimpleLogger.INSTANCE.log(0, "Success to connect to the MariaDB Server");
        } catch (SQLException e) {
            SimpleLogger.INSTANCE.log(4, "Cannot connect to the MariaDB Server.");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            SimpleLogger.INSTANCE.log(4, "Unable to find MariaDB Driver.");
            throw new RuntimeException(e);
        }
    }

    /**
    * 데이터 베이스에 해당 이름에 테이블이 없을 시 새로운 테이블 생성
     *
     * @param tableName 테이블 이름
    * */
    public void createTable(String tableName) throws SQLException {
        connection.prepareStatement("create table if not exists `" + tableName + "` (`%s` TEXT, `%s` LONGTEXT);".formatted(KEY_COLUMNS_NAME, VALUE_COLUMNS_NAME)).execute();
    }

    public void insert(String tableName, String key, String value) throws SQLException {
        connection.prepareStatement("INSERT INTO `%s` (`%s`, `%s`) VALUES ('%s', '%s');".formatted(tableName, KEY_COLUMNS_NAME, VALUE_COLUMNS_NAME, key, value)).execute();
    }

    public void update(String tableName, String key, String value) throws SQLException {
        connection.prepareStatement("UPDATE `%s` SET `%s`='%s' WHERE `%s`='%s';".formatted(tableName, VALUE_COLUMNS_NAME, value, KEY_COLUMNS_NAME, key)).execute();
    }

    public void delete(String tableName, String key) throws SQLException {
        connection.prepareStatement("DELETE FROM `%s` WHERE `%s`='%s';".formatted(tableName, KEY_COLUMNS_NAME, key)).execute();
    }

    public ResultSet execute(String sql) throws SQLException {
        return connection.prepareStatement(sql).executeQuery();
    }

    public boolean doesItExist(String tableName, String key) throws SQLException {
        ResultSet r = connection.prepareStatement("SELECT 1 FROM `%s` WHERE `%s`='%s';".formatted(tableName, KEY_COLUMNS_NAME, key)).executeQuery();
        return r.first();
    }

    public void close() throws SQLException {
        connection.close();
    }
}