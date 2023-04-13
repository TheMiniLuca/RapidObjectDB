package io.github.theminiluca.sql;

import io.github.theminiluca.sql.Logger.SimpleLogger;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

public class SQLSyncManager {

    //SQL 관련
    SQLMan sqlManager;

    private final Map<Object, SavingExceptionHandler> savingExceptionHandlers = new HashMap<>();

    //Manager 관련
    private final HashMap<Object, Thread> autoSaveThreads = new HashMap<>();

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
     * @param period    period
     */
    public void scheduleAutoSaveTask(Object dataClass, long period) {
        startupLoad(dataClass);
        autoSaveThreads.put(dataClass, new Thread(() -> {
            while (true) {
                saveMapWithClass(dataClass);
                saveListWithClass(dataClass);
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

    /**
     * Register Saving Exception
     *
     * @param i          id
     * @param handler    handler
     */
    public void registerSavingExceptionHandler(int i, SavingExceptionHandler handler) {
        savingExceptionHandlers.put(i, handler);
    }

    @SuppressWarnings("unchecked")
    public void saveMapWithClass(Object dataClass) {
        for (Field field : dataClass.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SQL.class) && field.getType().isAssignableFrom(SQLMap.class)) {
                try {
                    field.setAccessible(true);
                    SQL annotation = field.getAnnotation(SQL.class);
                    sqlManager.createMapTable(annotation.tableName());
                    SQLMap<String, Object> hash = (SQLMap<String, Object>) field.get(dataClass);
                    saveMap(annotation.tableName(), hash, annotation.savingException());
                } catch (ClassCastException | IllegalAccessException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void saveListWithClass(Object dataClass) {
        for (Field field : dataClass.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SQL.class) && field.getType().isAssignableFrom(SQLList.class)) {
                try {
                    field.setAccessible(true);
                    SQLList<Values> list = (SQLList<Values>) field.get(dataClass);
                    sqlManager.createListTable(field.getAnnotation(SQL.class).tableName(), list.size());
                    saveList(field.getAnnotation(SQL.class).tableName(), list);
                } catch (ClassCastException | IllegalAccessException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void startupLoad(Object dataClass) {
        for (Field field : dataClass.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SQL.class)) {
                try {
                    field.setAccessible(true);
                    if (field.getType().isAssignableFrom(SQLMap.class)) {
                        sqlManager.createMapTable(field.getAnnotation(SQL.class).tableName());
                        field.set(dataClass, loadMap(field.getAnnotation(SQL.class).tableName(), field.getAnnotation(SQL.class).savingException()));
                    } else if (field.getType().isAssignableFrom(SQLList.class)) {
                        SQLList<?> list = loadList(field.getAnnotation(SQL.class).tableName());
                        if (list != null) field.set(dataClass, list);
                    }
                } catch (ClassCastException | IllegalAccessException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void saveMap(String name, SQLMap<String, Object> map, int i) {
        try {
            removeKeys(name, map.removeQueue);
            String key;
            if(i != -1 && savingExceptionHandlers.containsKey(i)) {
                while ((key = map.updatedKey.poll()) != null) {
                    if (sqlManager.doesItExist(name, key)) {
                        sqlManager.update(name, key, savingExceptionHandlers.get(i).onSerialize(map.get(key)));
                    } else {
                        sqlManager.insert(name, key, savingExceptionHandlers.get(i).onSerialize(map.get(key)));
                    }
                }
            }else {
                while ((key = map.updatedKey.poll()) != null) {
                    if (sqlManager.doesItExist(name, key)) {
                        sqlManager.update(name, key, sqlManager.serialize((SQLObject) map.get(key)));
                    } else {
                        sqlManager.insert(name, key, sqlManager.serialize((SQLObject) map.get(key)));
                    }
                }
            }
        } catch (SQLException e) {
            SimpleLogger.INSTANCE.log(3, "Unable to perform insert/update.");
            throw new RuntimeException(e);
        }
    }

    public void saveList(String name, SQLList<Values> list) {
        try {
            for (Integer[] i : list.actionQueue) {
                switch (i[0]) {
                    case -2: {
                        sqlManager.execute("DELETE FROM %s;".formatted(name));
                        break;
                    }
                    case -1: {
                        sqlManager.delete(name, i[1]);
                        break;
                    }
                    case 0: {
                        sqlManager.update(name, i[1], list.get(i[1]));
                        break;
                    }
                    case 1: {
                        sqlManager.insert(name, list.get(i[1]));
                        break;
                    }
                }
                list.actionQueue.remove(0);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ConcurrentModificationException ignored) {
        }
    }

    private SQLMap<String, Object> loadMap(String name, int i) {
        try {
            HashMap<String, Object> map = new HashMap<>();

            ResultSet set = sqlManager.execute("SELECT * FROM `%s`;".formatted(name));

            if (i != -1 && savingExceptionHandlers.containsKey(i)) {
                while (set.next()) {
                    map.put(set.getString(1), savingExceptionHandlers.get(i).onDeserialize(set.getString(2)));
                }
            }else {
                while (set.next()) {
                    map.put(set.getString(1), sqlManager.deserialize(set.getString(2)));
                }
            }
            return new SQLMap<>(map);
        }catch (SQLException e) {
            SimpleLogger.INSTANCE.log(3, "Unable to perform select.");
            throw new RuntimeException(e);
        }
    }

    private <T extends SQLObject> SQLList<Values<T>> loadList(String name) {
        try {
            int size;
            ArrayList<Values<T>> list = new ArrayList<>();

            ResultSet sizeSet = sqlManager.execute("SHOW COLUMNS FROM `%s`;".formatted(name));
            sizeSet.last();
            String s = sizeSet.getString(1);
            size = Integer.parseInt(s.substring(1, s.length()));

            ResultSet set = sqlManager.execute("SELECT * FROM `%s`;".formatted(name));

            while (set.next()) {
                Values<T> val = new Values<>(size);
                for (int i = 0; i < size; i++) {
                    val.replace(i, sqlManager.deserialize(set.getString(2 + i)));
                }
                val.updatedIndex.clear();
                list.add(set.getInt(1) - 1, val);
            }
            return new SQLList<>(size, list);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1146) return null;
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

    public void close() {
        SimpleLogger.INSTANCE.log(0, "Auto-Saving maps.");
        for (Object key : autoSaveThreads.keySet()) {
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
     */
    public void createMapTable(String tableName) throws SQLException {
        connection.prepareStatement("create table if not exists `" + tableName + "` (`%s` TEXT, `%s` LONGTEXT);".formatted(KEY_COLUMNS_NAME, VALUE_COLUMNS_NAME)).execute();
    }

    public void createListTable(String tableName, int size) throws SQLException {
        connection.prepareStatement("create table if not exists `%s` (idx int NOT NULL AUTO_INCREMENT, %s, PRIMARY KEY (idx));".formatted(tableName, toK(size))).execute();
    }

    private String toK(int i) {
        StringBuilder sb = new StringBuilder();
        for (int ii = 0; ii < i + 1; ii++) {
            sb.append("`i%s` LONGTEXT,".formatted(ii));
        }
        return sb.substring(0, sb.length() - 1);
    }

    public void insert(String tableName, String key, String value) throws SQLException {
        connection.prepareStatement("INSERT INTO `%s` (`%s`, `%s`) VALUES ('%s', '%s');".formatted(tableName, KEY_COLUMNS_NAME, VALUE_COLUMNS_NAME, key, value)).execute();
    }

    public <T extends SQLObject> void insert(String table, Values<T> values) throws SQLException {
        connection.prepareStatement("INSERT INTO `%s` %s;".formatted(table, insertValuesToString(values))).execute();
    }

    public void update(String tableName, String key, String value) throws SQLException {
        connection.prepareStatement("UPDATE `%s` SET `%s`='%s' WHERE `%s`='%s';".formatted(tableName, VALUE_COLUMNS_NAME, value, KEY_COLUMNS_NAME, key)).execute();
    }

    public <T extends SQLObject> void update(String tableName, int index, Values<T> values) throws SQLException {
        connection.prepareStatement("UPDATE `%s` SET %s WHERE `idx`=%s;".formatted(tableName, updatedValuesToString(values), index));
    }

    public void delete(String tableName, String key) throws SQLException {
        connection.prepareStatement("DELETE FROM `%s` WHERE `%s`='%s';".formatted(tableName, KEY_COLUMNS_NAME, key)).execute();
    }

    public void delete(String tableName, int index) throws SQLException {
        connection.prepareStatement("DELETE FROM `%s` WHERE `idx`=%s;".formatted(tableName, index)).execute();
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

    private <T extends SQLObject> String updatedValuesToString(Values<T> values) {
        StringBuilder sb = new StringBuilder();
        for (Integer updatedIndex : values.updatedIndex) {
            sb.append("`i%s`='%s',".formatted(updatedIndex, serialize(values.get(updatedIndex))));
        }
        return sb.substring(0, sb.length() - 1);
    }

    private <T extends SQLObject> String insertValuesToString(Values<T> values) {
        StringBuilder sb1 = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < values.getLength(); i++) {
            sb1.append("`i%s`,".formatted(i));
            sb2.append("'%s',".formatted(serialize(values.get(i))));
        }
        return String.format("(%s) VALUES (%s)", sb1.substring(0, sb1.length() - 1), sb2.substring(0, sb2.length() - 1));
    }

    @SuppressWarnings("unchecked")
    <T extends SQLObject> String serialize(T t) {
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
    <T extends SQLObject> T deserialize(String base64) {
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
}