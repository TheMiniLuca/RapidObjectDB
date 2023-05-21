package io.github.theminiluca.sql;

import io.github.theminiluca.sql.Logger.SimpleLogger;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

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
                SimpleLogger.INSTANCE.log(0, "Auto Save Event Fired");
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
     * @param i       id
     * @param handler handler
     */
    public void registerSavingExceptionHandler(int i, SavingExceptionHandler handler) {
        savingExceptionHandlers.put(i, handler);
    }

    @SuppressWarnings("unchecked")
    public void saveMapWithClass(Object dataClass) {
        SimpleLogger.INSTANCE.log(0, "Saving Map With Class...");
        for (Field field : dataClass.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SQL.class) && field.getType().isAssignableFrom(SQLMap.class)) {
                try {
                    field.setAccessible(true);
                    SQL annotation = field.getAnnotation(SQL.class);
                    SimpleLogger.INSTANCE.logf(0, "Found SQL annotation in %s (%s)", dataClass.getClass().getName(), annotation.value());
                    sqlManager.createMapTable(annotation.value());
                    SQLMap<String, Object> hash = (SQLMap<String, Object>) field.get(dataClass);
                    Queue<Object> uk = new LinkedBlockingQueue<>(hash.updatedKey);
                    Queue<Object> gk = new LinkedBlockingQueue<>(hash.gotKey);
                    Queue<Object> rq = new LinkedBlockingQueue<>(hash.removeQueue);
                    hash.updatedKey.clear();
                    hash.gotKey.clear();
                    hash.removeQueue.clear();
                    field.set(dataClass, hash);
                    saveMap(annotation.value(), hash, annotation, uk, gk, rq);
                } catch (ClassCastException | IllegalAccessException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        SimpleLogger.INSTANCE.log(0, "Saved Map!");
    }

    public void saveListWithClass(Object dataClass) {
        for (Field field : dataClass.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SQL.class) && field.getType().isAssignableFrom(SQLList.class)) {
                try {
                    field.setAccessible(true);
                    SQLList<Values> list = (SQLList<Values>) field.get(dataClass);
                    sqlManager.createListTable(field.getAnnotation(SQL.class).value(), list.size());
                    saveList(field.getAnnotation(SQL.class).value(), list);
                } catch (ClassCastException | IllegalAccessException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void startupLoad(Object dataClass) {
        for (Field field : dataClass.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(SQL.class)) {
                try {
                    field.setAccessible(true);
                    if (field.getType().isAssignableFrom(SQLMap.class)) {
                        sqlManager.createMapTable(field.getAnnotation(SQL.class).value());
                        field.set(dataClass, loadMap(field.getAnnotation(SQL.class).value(), field.getAnnotation(SQL.class).savingException()));
                    } else if (field.getType().isAssignableFrom(SQLList.class)) {
                        SQLList<?> list = loadList(field.getAnnotation(SQL.class).value());
                        if (list != null) field.set(dataClass, list);
                    }
                } catch (ClassCastException | IllegalAccessException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void saveMap(String name, SQLMap<String, Object> map, SQL annotation, Queue<Object> uk, Queue<Object> gk, Queue<Object> rq) {
        SimpleLogger.INSTANCE.log(0, "Saving Map "+name);
        try {
            if(!annotation.resetTableAtSave() || annotation.checkValueChangesAtSave()) {
                removeKeys(name, rq);
            }else {
                clearTable(name);
            }
            String key;
            if(!annotation.resetTableAtSave() && !annotation.checkValueChangesAtSave()) {
                map.gotKey = null;
                if (annotation.savingException() != -1 && savingExceptionHandlers.containsKey(annotation.savingException())) {
                    while ((key = (String) uk.poll()) != null) {
                        sqlManager.insertOrUpdate(name, key, savingExceptionHandlers.get(annotation.savingException()).serialize(map.get(key)));
                    }
                } else {
                    while ((key = (String) uk.poll()) != null) {
                        sqlManager.insertOrUpdate(name, key, sqlManager.serialize(map.get(key)));
                    }
                }
            }else if (!annotation.checkValueChangesAtSave()) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    sqlManager.insert(name, entry.getKey(), (annotation.savingException() != -1 && savingExceptionHandlers.containsKey(annotation.savingException())
                            ? savingExceptionHandlers.get(annotation.savingException()).serialize(entry.getValue()) : sqlManager.serialize(entry.getValue())));
                }
            }else {
                while ((key = (String) uk.poll()) != null) {
                    sqlManager.insertOrUpdate(name, key, (annotation.savingException() != -1 && savingExceptionHandlers.containsKey(annotation.savingException()) ? savingExceptionHandlers.get(annotation.savingException()).serialize(map.get(key)) : sqlManager.serialize(map.get(key))));
                }
                while ((key = (String) gk.poll()) != null) {
                    System.out.println("gk " + name + ' ' + key);
                    sqlManager.insertOrUpdate(name, key, (annotation.savingException() != -1 && savingExceptionHandlers.containsKey(annotation.savingException()) ? savingExceptionHandlers.get(annotation.savingException()).serialize(map.get(key))
                            : sqlManager.serialize(map.get(key))));
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
                    map.put(set.getString(1), savingExceptionHandlers.get(i).deserialize(set.getString(2)));
                }
            } else {
                while (set.next()) {
                    map.put(set.getString(1), sqlManager.deserialize(set.getString(2)));
                }
            }
            return new SQLMap<>(map);
        } catch (SQLException e) {
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
                    val.replace(i, (T) sqlManager.deserialize(set.getString(2 + i)));
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

    private void clearTable(String name) {
        try {
            sqlManager.executeF("DROP TABLE %s;", name);
            sqlManager.createMapTable(name);
        } catch (SQLException e) {
            SimpleLogger.INSTANCE.logf(2, "Unable to clear table with drop & insert method.");
            throw new RuntimeException(e);
        }
    }

    public void close() {
        SimpleLogger.INSTANCE.log(0, "Auto-Saving maps.");
        for (Object key : autoSaveThreads.keySet()) {
            autoSaveThreads.get(key).stop();
            saveMapWithClass(key);
            saveListWithClass(key);
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
    protected Connection connection;

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
        connection.prepareStatement("CREATE TABLE IF NOT EXISTS `%s` (`%s` TEXT NOT NULL UNIQUE, `%s` LONGTEXT NOT NULL);".formatted(tableName, KEY_COLUMNS_NAME, VALUE_COLUMNS_NAME)).execute();
    }

    public void createListTable(String tableName, int size) throws SQLException {
        connection.prepareStatement("create table if not exists `%s` (idx int NOT NULL AUTO_INCREMENT, %s, PRIMARY KEY (idx));".formatted(tableName, toK(size))).execute();
    }

    public void createRankboardTable(String name) throws SQLException {
        connection.prepareStatement("CREATE TABLE IF NOT EXISTS `%s` (`uniqueValue` TEXT, `score` INTEGER);".formatted(name)).execute();
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

    public void insertOrUpdate(String tableName, String key, String value) throws SQLException {
        connection.prepareStatement("REPLACE INTO %s (`%s`, `%s`) VALUES ('%s', '%s');"
                .formatted(
                        tableName, KEY_COLUMNS_NAME, VALUE_COLUMNS_NAME, key, value
                )).execute();
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

    public ResultSet executeF(String sql, Object... values) throws SQLException {
        return connection.prepareStatement(sql.formatted(values)).executeQuery();
    }

    public boolean doesItExist(String tableName, String key) throws SQLException {
        ResultSet r = connection.prepareStatement("SELECT * FROM `%s` WHERE `%s`='%s';".formatted(tableName, KEY_COLUMNS_NAME, key)).executeQuery();
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

    public byte[] toForceByteArray(Object o) {
        ObjectOutputStream objOut = null;
        ByteArrayOutputStream byteOut = null;
        try {
            Class<?> clazz = o.getClass();
            Field[] fields = clazz.getDeclaredFields();
            byteOut = new ByteArrayOutputStream();
            objOut = new ObjectOutputStream(byteOut);
            for (Field field : fields) {
                field.setAccessible(true);
                if (!Modifier.isTransient(field.getModifiers())) {
                    objOut.writeObject(field.get(o));
                }
            }

            objOut.flush();
            return byteOut.toByteArray();
        } catch (IOException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                assert objOut != null;
                objOut.close();
                byteOut.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @SuppressWarnings("unchecked")
    <T extends SQLObject> String serialize(Object t) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(t);
            out.flush();
            out.close();
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
    <T extends SQLObject> Object deserialize(String base64) {
        ByteArrayInputStream bipt = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        ObjectInputStream ipt = null;
        try {
            ipt = new ObjectInputStream(bipt);
            return ipt.readObject();
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