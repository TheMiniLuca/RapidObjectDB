package io.github.theminiluca.sql;

import java.lang.reflect.*;
import java.security.MessageDigest;
import java.sql.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class SQLManager {

    public static boolean DEBUGGING = false;

    public static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static final Map<String, Connections> CONNECTIONS_CLASS = new HashMap<>();

    public static String standardateto(long ms) {
        return new SimpleDateFormat(DATE_FORMAT).format(new Date(ms));
    }

    public static String standardate() {
        return new SimpleDateFormat(DATE_FORMAT).format(new Date(System.currentTimeMillis()));
    }

    public static <T extends SQLObject> void sqlite(Class<T> clazz,
                                                    Connections connections, HashMap<String, T> hash) {
        try {
            if (DEBUGGING) System.out.println("sqlite :" + connections.name);
            if (!migration(clazz, connections, hash) && new File(connections.connection.getMetaData().getURL()).exists())
                migration(clazz, connections, hash);
            else
                createTable(clazz, connections);
            SQLManager.load(clazz, hash);

        } catch (Exception e) {

        }
    }

    public static void loggingFile(Connections connections, File loggingFile) {
        String url = connections.url();
        System.out.println(url + " 데이터 베이스 로깅중..");
        new File(loggingFile.getPath() + File.separator).mkdir();
        copyFile(url, loggingFile.getPath() + File.separator + standardate().replace(":", ";") + ".sqlite3");
        System.out.println(url + " 데이터 베이스 로깅 성공!");

    }

    public static void zipFile(String sourceFile, String zipFile) {
        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            File fileToZip = new File(sourceFile);
            FileInputStream fis = new FileInputStream(fileToZip);

            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }

            zos.closeEntry();
            fis.close();
            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void unzipFile(String zipFile, String destDirectory) {
        File directory = new File(destDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(destDirectory + File.separator + zipEntry.getName());
                try (FileOutputStream fos = new FileOutputStream(newFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    byte[] bytesIn = new byte[4096];
                    int read = 0;
                    while ((read = zis.read(bytesIn)) != -1) {
                        bos.write(bytesIn, 0, read);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void copyFile(String source, String destination) {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static <T extends SQLObject> boolean migration(Class<T> clazz, Connections connections, HashMap<String, T> hash) {
        if (migration(clazz) == null) return false;
        String url = connections.url();
        File file = new File(url);
        if (file.renameTo(new File(file.getName() + "_migration")))
            throw new RuntimeException("기존 데이터베이스의 이름이 변경되지 않았습니다.");
        createTable(clazz, driver(connections.name, url));
        System.out.println("마이그레이션 중..");
        long ms = System.currentTimeMillis();
        for (T value : hash.values()) {
            value.saveSQL();
        }
        System.out.println("마이그레이션 종료. ( 소요된 시간 : " + (System.currentTimeMillis() - ms) + " ms )");
        return true;
    }


    public static String tablename(String first, String last) {
        return first + "_" + last;
    }

    public static <T extends SQLObject> void load(Class<? extends SQLObject> clazz, HashMap<String, T> hash) {
        Connections connections = CONNECTIONS_CLASS.get(clazz.getName());
        load(clazz, connections.name, hash);
    }

    @SuppressWarnings("unchecked")
    public static <T extends SQLObject> void load(Class<? extends SQLObject> clazz, String table, HashMap<String, T> hash) {
        Connections connections = CONNECTIONS_CLASS.get(clazz.getName());
        LinkedHashMap<String, Object> instance = null;
        String sql = null;
        try {

            sql = "select * from " + table;
            Statement sta = connections.connection.createStatement();
            ResultSet res = sta.executeQuery(sql);
            while (res.next()) {
                instance = new LinkedHashMap<>();
                String primary = res.getString(primaryKey(clazz));
                for (String key : getcolumns(clazz).keySet()) {
                    instance.put(key, res.getObject(key));
                }
                for (Map.Entry<String, Field> entry : tableList(clazz).entrySet()) {
                    instance.put(entry.getKey(), loadList(connections, entry.getValue(), tablename(connections.name, entry.getKey()), clazz, primary));
                }
                for (Map.Entry<String, Field> entry : tableHash(clazz).entrySet()) {
                    instance.put(entry.getKey(), loadHash(connections, entry.getValue(), tablename(connections.name, entry.getKey()), clazz, primary));
                }
                hash.put(primary, (T) deserialize(clazz, instance));
            }
        } catch (Exception e) {

        } finally {
            if (DEBUGGING)
                System.out.println(sql);
        }
    }


    private static void loadingCore(Connections connections, String tableName, Class<? extends SQLObject> generic, LinkedHashMap<String, Object> instance, ResultSet res) throws SQLException {
        Class<? extends SQLObject> sqlClass = generic;
        String nextPrimary = res.getString(primaryKey(sqlClass));
        for (String key : getcolumns(sqlClass).keySet()) {
            instance.put(key, res.getObject(key));
        }
        for (Map.Entry<String, Field> entry : tableList(sqlClass).entrySet()) {
            instance.put(entry.getKey(), loadList(connections, entry.getValue(), tablename(tableName, entry.getKey()), sqlClass, nextPrimary));
        }
        for (Map.Entry<String, Field> entry : tableHash(sqlClass).entrySet()) {
            instance.put(entry.getKey(), loadHash(connections, entry.getValue(), tablename(tableName, entry.getKey()), sqlClass, nextPrimary));
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<Object, Object> loadHash(Connections connections, Field field, String tableName, Class<? extends SQLObject> superClass, String primary) {
        Map<Object, Object> hashmap = new HashMap<>();
        Class<?> generic_1 = getgeneric_1(field);
        Class<?> generic_2 = getgeneric_2(field);
        LinkedHashMap<String, Object> instance;
        String sql;
        sql = "select * from " + tableName + " where " + primaryKey(superClass) + "=?";
        try {
            PreparedStatement sta = connections.connection.prepareStatement(sql);
            sta.setString(1, primary);
            ResultSet res = sta.executeQuery();
            boolean isObject = Arrays.stream(generic_1.getInterfaces()).filter(aClass -> aClass.equals(SQLObject.class)).findFirst().orElse(null) != null;
            while (res.next()) {
                instance = new LinkedHashMap<>();
                if (isObject) {
                    loadingCore(connections, tableName, (Class<? extends SQLObject>) generic_1, instance, res);
                    hashmap.put(res.getObject("keys"), deserialize((Class<? extends SQLObject>) generic_2, instance));
                } else {
                    String col = getclassname(generic_2);
                    hashmap.put(res.getObject("keys"), res.getObject(col));
                }
            }
        } catch (Exception e) {
            if (DEBUGGING) {
                System.err.println(field.getName());
            }
            e.printStackTrace();
        } finally {
            if (DEBUGGING) {
                System.out.println(sql);
            }

        }
        return hashmap;
    }

    public static void setConnection(Class<? extends SQLObject> clazz, Connections connections) {
        CONNECTIONS_CLASS.put(clazz.getName(), connections);
    }


    public static String getclassname(Class<?> clazz) {
        String type = null;
        if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            type = "long";
        } else if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            type = "int";
        } else if (clazz.equals(UUID.class)) {
            type = "uuid";
        } else if (clazz.equals(String.class)) {
            type = "longtext";
        } else if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            type = "boolean";
        } else return clazz.getSimpleName();
        return type;
    }


    public static Class<?> getgeneric_1(Field field) {
        ParameterizedType generic = (ParameterizedType) field.getGenericType();
        return (Class<?>) generic.getActualTypeArguments()[0];
    }

    public static Class<?> getgeneric_2(Field field) {
        ParameterizedType generic = (ParameterizedType) field.getGenericType();
        return (Class<?>) generic.getActualTypeArguments()[1];
    }

    private static boolean exists(SQLObject object, Connections connections) {
        String sql = null;
        try {
            sql = "select * from " + connections.name + " where " + primaryKey(object.getClass()) + "=?";
            PreparedStatement statement = connections.connection.prepareStatement(sql);
            statement.setObject(1, primaryValue(object));
            ResultSet result = statement.executeQuery();
            return result.next();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (DEBUGGING) {
                System.out.println("exists functions : " + sql);
                System.out.println("exists functions : " + object.getClass().getName());
                System.out.println("exists functions : " + CONNECTIONS_CLASS);
            }
        }
    }

    public static Connections driver(String tableName, String path) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            return new Connections(tableName, connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void createList(Class<? extends SQLObject> clazz, String tableName, Class<?> generic) {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        String sql = null;
        try {
            Statement statement = connections.connection.createStatement();
            String primary = getcolumn(getcolumns(clazz).get(primaryKey(clazz)));
            String column = getcolumn(generic);
            if (column == null) {
                sql = "create table if not exists " + tableName +
                        "(" + primaryKey(clazz) + " " + primary + ", indexs int, " + columns((Class<? extends SQLObject>) generic, false, true) + ")";
            } else {
                sql = "create table if not exists " + tableName +
                        "(" + primaryKey(clazz) + " " + primary + ", indexs int, " + column.toLowerCase() + " " + column + ")";
            }
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (DEBUGGING)
                System.out.println("createList function : " + sql);
        }
    }

    @SuppressWarnings("unchecked")
    private static void createHash(Class<? extends SQLObject> clazz, String tableName, Field generic) {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        String sql = null;
        try {
            Statement statement = connections.connection.createStatement();
            String primary = getcolumn(getcolumns(clazz).get(primaryKey(clazz)));
            Class<?> generic_1 = getgeneric_1(generic);
            Class<?> generic_2 = getgeneric_2(generic);
            String column = getcolumn(generic_2);
            if (column == null) {
                sql = "create table if not exists " + tableName +
                        "(" + primaryKey(clazz) + " " + primary + ", keys " + getcolumn(generic_1) + ", " + columns((Class<? extends SQLObject>) generic_2, false, true) + ")";
            } else {
                sql = "create table if not exists " + tableName +
                        "(" + primaryKey(clazz) + " " + primary + ", keys " + getcolumn(generic_1) + ", " + column.toLowerCase() + " " + column + ")";
            }
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (DEBUGGING)
                System.out.println("createHash : " + sql);
        }
    }

    public static void save(SQLObject object) {
        save(object, getDriver(object));
    }

    public static void save(SQLObject object, Connections connections) {
        String sql;
        String delete;
        if (exists(object, connections)) {
            try {
                delete = "DELETE FROM " + connections.name + " WHERE " + primaryKey(object.getClass()) + "=?";
                PreparedStatement statement = connections.connection.prepareStatement(delete);
                statement.setObject(1, serialize(object).get(primaryKey(object.getClass())));
                statement.execute();
                statement.close();
            } catch (Exception e) {

            }
        }
        sql = "INSERT INTO " + connections.name + "(" + columns(object.getClass(), true) + ") VALUES (" + specifiers(object) + ")";
        try {
            PreparedStatement statement = connections.connection.prepareStatement(sql);
            int pr = 1;
            HashMap<String, Object> values = getvalues(object);
            for (String key : getcolumns(object.getClass()).keySet()) {
                statement.setObject(pr, values.get(key));
                pr++;
            }
            statement.execute();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (DEBUGGING)
                System.out.println("save function : " + sql);
        }
        saveList(object);
        saveHash(object);
    }

    public static void saveHash(SQLObject object) {
        saveHash(object, null);
    }

    @SuppressWarnings("unchecked")
    public static void saveList(SQLObject object, String table) {
        Connections connections = getDriver(object);
        String sql = null;
        for (Map.Entry<String, Field> entry : tableList(object.getClass()).entrySet()) {
            try {
                String tableName = tablename((table == null ? connections.name : table), entry.getValue().getName());
                sql = "delete from " + tableName + " where " + primaryKey(object.getClass()) + "=?";
                PreparedStatement statement = connections.connection.prepareStatement(sql);
                statement.setObject(1, serialize(object).get(primaryKey(object.getClass())).toString());
                statement.execute();
                int index = 0;
                int n = 0;
                for (Object obj : (List<?>) entry.getValue().get(object)) {
                    if (obj instanceof SQLObject sqlObject) {
                        sql = "insert into " + tableName + "(" + primaryKey(object.getClass()) + ", indexs, " + columns((Class<? extends SQLObject>) obj.getClass(), true) +
                                ") values (?, ?, " + specifiers(sqlObject) + ")";
                        statement = connections.connection.prepareStatement(sql);
                        n = 0;
                        System.out.println(sqlObject.getClass() + ": " + primaryValue(sqlObject));
                        statement.setObject(++n, primaryValue(object));
                        statement.setInt(++n, index);
                        HashMap<String, Object> values = getvalues(sqlObject);
                        for (Map.Entry<String, Class<?>> entry1 : getcolumns((sqlObject).getClass()).entrySet()) {
                            statement.setObject(++n, values.get(entry1.getKey()));
                        }
                        createLists(sqlObject.getClass(), tableName);
                        createHashes(sqlObject.getClass(), tableName);
                        saveList(sqlObject, tableName);
                        saveHash(sqlObject, tableName);
                    } else {
                        sql = "insert into " + tableName + "(" + primaryKey(object.getClass()) + ", indexs, " + getclassname(obj.getClass()) +
                                ") values (?, ?, ?)";
                        statement = connections.connection.prepareStatement(sql);
                        n = 0;
                        statement.setObject(++n, primaryValue(object));
                        statement.setInt(++n, index);
                        statement.setObject(++n, obj);
                    }
                    statement.execute();
                    index++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(entry.getKey() + "에서 오류.");
            } finally {
                if (DEBUGGING)
                    System.out.println("saveList function : " + sql);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void saveHash(SQLObject object, String table) {
        Connections connections = getDriver(object);
        String sql = null;
        for (Map.Entry<String, Field> entry : tableHash(object.getClass()).entrySet()) {
            try {
                String tableName = tablename((table == null ? connections.name : table), entry.getValue().getName());
                sql = "delete from " + tableName + " where " + primaryKey(object.getClass()) + "=?";
                PreparedStatement statement = connections.connection.prepareStatement(sql);
                statement.setObject(1, serialize(object).get(primaryKey(object.getClass())).toString());
                statement.execute();
                Object ob = entry.getValue().get(object);
                for (Map.Entry<Object, Object> entry1 : ((Map<Object, Object>) ob).entrySet()) {
                    String column = getcolumn(entry.getValue().getClass());
                    if (column == null) {
                        statement = connections.connection.prepareStatement(sql);
                        sql = "insert into " + tableName + "(" + primaryKey(object.getClass()) + ", keys," + columns((Class<? extends SQLObject>) entry1.getValue().getClass(), false) +
                                ") values (?, ?, " + specifiers((SQLObject) entry1) + ")";
                        int n = 0;
                        statement.setObject(++n, primaryValue(object));
                        statement.setObject(++n, entry.getKey());
                        HashMap<String, Object> values = getvalues(((SQLObject) entry));
                        for (Map.Entry<String, Class<?>> entry2 : getcolumns(((SQLObject) entry).getClass()).entrySet()) {
                            statement.setObject(++n, values.get(entry2.getKey()));
                        }
                        createLists(object.getClass(), tableName);
                        createHashes(object.getClass(), tableName);
                        saveList(object, tableName);
                        saveHash(object, tableName);
                    } else {
                        statement = connections.connection.prepareStatement(sql);
                        sql = "insert into " + tableName + "(" + primaryKey(object.getClass()) + ", keys," + column +
                                ") values (?, ?, ?)";
                        int n = 0;
                        statement.setObject(++n, primaryValue((SQLObject) entry));
                        statement.setObject(++n, entry.getKey());
                        statement.setObject(++n, entry1.getValue());
                    }
                    statement.execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(entry.getKey() + "에서 오류.");
            } finally {
                if (DEBUGGING)
                    System.out.println("saveHash function : " + sql);
            }
        }

    }

    public static void saveList(SQLObject object) {
        saveList(object, null);
    }

    public static Connections getDriver(SQLObject object) throws RuntimeException {
        return getDriver(object.getClass());
    }

    public static Connections getDriver(Class<? extends SQLObject> clazz) throws RuntimeException {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        if (connections == null) {
            if (DEBUGGING)
                throw new RuntimeException("driver is not exists");
            else
                throw new RuntimeException("driver is not exists ( " + clazz + " )");
        }
        return connections;
    }

    public static void createLists(Class<? extends SQLObject> clazz, String table) {
        for (Field column : tableList(clazz).values()) {
            String columnName = column.getName();
            createList(clazz, tablename(table, columnName), getgeneric_1(column));
        }
    }

    public static void createHashes(Class<? extends SQLObject> clazz, String table) {
        for (Field column : tableHash(clazz).values()) {
            String columnName = column.getName();
            createHash(clazz, tablename(table, columnName), column);
        }
    }

    public static void createTable(Class<? extends SQLObject> clazz, Connections connection) {
        String sql = null;
        try {
            Statement statement = connection.connection.createStatement();
            sql = "create table if not exists " + connection.name + "(" + columns(clazz, false) + ")";
            setConnection(clazz, connection);
            statement.execute(sql);
            connection.connection.prepareStatement("pragma busy_timeout = 30000").execute();
            createLists(clazz, connection.name);
            createHashes(clazz, connection.name);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (DEBUGGING) {
                System.out.println("createTable : " + sql);
            }
        }
    }


    private static String columns(Class<? extends SQLObject> clazz, boolean noType) {
        return columns(clazz, noType, false);
    }

    private static String columns(Class<? extends SQLObject> clazz, boolean noType, boolean noPrimary) {
        try {
            StringBuilder builder = new StringBuilder();
            LinkedHashMap<String, Class<?>> hash = getcolumns(clazz);
            int i = 0;
            for (Map.Entry<String, Class<?>> entry : hash.entrySet()) {
                String name = entry.getKey();
                Class<?> classes = entry.getValue();
                String type;
                if (!noType) {
                    type = getcolumn(classes);
                    builder.append("%s %s".formatted(name, type));
                    if (!noPrimary)
                        if (name.equals(primaryKey(clazz))) {
                            builder.append(" PRIMARY KEY");
                        }
                    builder.append(i == hash.size() - 1 ? "" : ",");
                } else {
                    builder.append("%s".formatted(name)).append(i == hash.size() - 1 ? "" : ",");
                }
                i++;
            }
            return builder.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getcolumn(Class<?> classes) {
        String type;
        if (classes.equals(long.class) || classes.equals(Long.class)) {
            type = "LONG";
        } else if (classes.equals(int.class) || classes.equals(Integer.class)) {
            type = "INT";
        } else if (classes.equals(UUID.class)) {
            type = "VARCHAR(32)";
        } else if (classes.equals(String.class)) {
            type = "LONGTEXT";
        } else if (classes.equals(boolean.class) || classes.equals(Boolean.class)) {
            type = "BOOLEAN";
        } else return null;
        return type;
    }


    public static int getSQLType(Class<?> clazz) {
        if (clazz == String.class) {
            return Types.VARCHAR;
        } else if (clazz == Integer.class || clazz == int.class) {
            return Types.INTEGER;
        } else if (clazz == Double.class || clazz == double.class) {
            return Types.DOUBLE;
        } else if (clazz == Float.class || clazz == float.class) {
            return Types.FLOAT;
        } else if (clazz == Long.class || clazz == long.class) {
            return Types.BIGINT;
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return Types.BOOLEAN;
        } else if (clazz == java.sql.Date.class) {
            return Types.DATE;
        } else if (clazz == java.sql.Time.class) {
            return Types.TIME;
        } else if (clazz == java.sql.Timestamp.class) {
            return Types.TIMESTAMP;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + clazz.getName());
        }
    }


    private static void preparedvalue(PreparedStatement statement, Object ob, Class<?> clazz, int parameterIndex) {
        try {
            if (ob == null) {
                statement.setNull(parameterIndex, getSQLType(clazz));
            } else if (ob instanceof Integer) {
                statement.setInt(parameterIndex, (int) ob);
            } else if (ob instanceof UUID) {
                statement.setString(parameterIndex, String.valueOf(ob));
            } else if (ob instanceof String) {
                statement.setString(parameterIndex, (String) ob);
            } else if (ob instanceof Long) {
                statement.setLong(parameterIndex, (long) ob);
            } else if (ob instanceof Boolean) {
                statement.setBoolean(parameterIndex, (boolean) ob);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported type: " + clazz.getName());
        }
    }

    private static String specifiers(SQLObject object) {
        StringBuilder builder = new StringBuilder();
        HashMap<String, Object> hash = getvalues(object);
        int i = 0;
        for (Object entry : hash.values()) {
            builder.append("?");
            builder.append(i == hash.size() - 1 ? "" : ", ");
            i++;
        }
        return builder.toString();
    }

    private static HashMap<String, Object> getvalues(SQLObject object) {
        HashMap<String, Object> migration = migration(object.getClass());
        LinkedHashMap<String, Object> hash = serialize(object);
        if (migration == null) return hash;
        hash.putAll(migration);
        return hash;
    }


    private static <T extends SQLObject> LinkedHashMap<String, Field> tableList(Class<T> clazz) {
        List<Field> fields = fields(clazz).stream().filter(field -> field.getType().equals(List.class)).toList();
        LinkedHashMap<String, Field> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            hash.put(field.getName(), field);
        }
        return hash;
    }

    private static LinkedHashMap<String, Field> tableColumns(Class<? extends SQLObject> clazz) {
        List<Field> fields = fields(clazz).stream().filter
                (field -> !(field.getType().equals(List.class) || field.getType().equals(Map.class) || field.getType().equals(HashMap.class))).toList();
        LinkedHashMap<String, Field> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            hash.put(field.getName(), field);
        }
        return hash;
    }

    private static LinkedHashMap<String, Field> tableHash(Class<? extends SQLObject> clazz) {
        List<Field> fields = fields(clazz).stream().filter(field -> (field.getType().equals(Map.class) || field.getType().equals(HashMap.class))).toList();
        LinkedHashMap<String, Field> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            hash.put(field.getName(), field);
        }
        return hash;
    }

//    private static LinkedHashMap<String, Class<?>> classArgument(Class<? extends SQLObject> clazz) {
//        List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(field
//                -> field.isAnnotationPresent(SQL.class) && !field.getType().equals(List.class)).toList();
//        LinkedHashMap<String, Class<?>> hash = new LinkedHashMap<>();
//        for (Field field : fields) {
//            hash.put(field.getName(), field.getType());
//        }
//        return hash;
//    }


    private static LinkedHashMap<String, Class<?>> getcolumns(Class<? extends SQLObject> clazz) {
        List<Field> fields = fields(clazz).stream()
                .filter(field -> !(field.getType().equals(List.class) || field.getType().equals(Map.class) || field.getType().equals(HashMap.class))).toList();
        LinkedHashMap<String, Class<?>> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            hash.put(field.getName(), field.getType());
        }
        return hash;
    }

    private static LinkedHashMap<String, Class<?>> getlists(Class<? extends SQLObject> clazz) {
        List<Field> fields = fields(clazz).stream().filter(field -> field.getType().equals(List.class)).toList();
        LinkedHashMap<String, Class<?>> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            hash.put(field.getName(), field.getType());
        }
        return hash;
    }

    private static LinkedHashMap<String, Class<?>> gethashes(Class<? extends SQLObject> clazz) {
        List<Field> fields = fields(clazz).stream().filter(field -> field.getType().equals(Map.class) || field.getType().equals(HashMap.class)).toList();
        LinkedHashMap<String, Class<?>> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            hash.put(field.getName(), field.getType());
        }
        return hash;
    }


    @SuppressWarnings("unchecked")
    private static List<Field> fields(Class<? extends SQLObject> clazz) {
        Method method = serialize(clazz);
        if (method == null) {
            return Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(SQL.class)).toList();
        } else {
            try {
                LinkedHashSet<String> invoke = (LinkedHashSet<String>) method.invoke(null);
                return Arrays.stream(clazz.getDeclaredFields()).filter(field -> invoke.contains(field.getName())).toList();
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
//        return Arrays.stream(clazz.getDeclaredFields()).filter(field ->
//                field.isAnnotationPresent(SQL.class) && !field.getType().equals(List.class) && !field.getType().equals(Map.class) && !field.getType().equals(HashMap.class)).toList();
    }


    private static <T extends SQLObject> LinkedHashMap<String, Object> serialize(T object) {
        List<Field> fields = fields(object.getClass()).stream()
                .filter(field -> !(field.getType().equals(List.class) || field.getType().equals(Map.class) || field.getType().equals(HashMap.class))).toList();
        LinkedHashMap<String, Object> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                hash.put(field.getName(), field.get(object));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("%s".formatted(field.getName()));
            }
        }
        return hash;
    }

    @SuppressWarnings("unchecked")
    private static String primaryKey(Class<? extends SQLObject> clazz) {
        try {
            Method method = serialize(clazz);
            if (method == null) {
                List<Field> fields = fields(clazz).stream().filter(field -> field.getAnnotation(SQL.class).primary()).toList();
                if (fields.size() > 1) {
                    System.err.printf("%s 에 primary 된 설정이 2개 이상있습니다.%n", clazz.getName());
                }
                return fields.get(0).getName();
            } else {
                return ((LinkedHashSet<String>) method.invoke(null)).iterator().next();
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static Object primaryValue(SQLObject object) {
        return serialize(object).get(primaryKey(object.getClass()));
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, Object> migration(Class<? extends SQLObject> clazz) {
        try {
            Method migration = clazz.getDeclaredMethod("migration");
            if (migration.invoke(null) != null) return (HashMap<String, Object>) migration.invoke(null);
            else return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static Method serialize(Class<? extends SQLObject> clazz) {
        try {
            return clazz.getDeclaredMethod("serialize", Set.class);
        } catch (Exception e) {
            return null;
        }
    }


    public static boolean isMigration(Class<? extends SQLObject> clazz) {
        return getcolumns(clazz).keySet().equals(getcolumns_db(clazz));
    }

    public static Set<String> getcolumns_db(Class<? extends SQLObject> clazz) {
        try {
            Connections connections = getDriver(clazz);
            Statement sta = connections.connection.createStatement();
            ResultSet res = sta.executeQuery("PRAGMA table_info(" + connections.name + ")");
            Set<String> columns = new HashSet<>();
            while (res.next()) {
                columns.add(res.getString("name"));
            }
            return columns;
        } catch (Exception e) {
            return null;
        }
    }

    public static Set<String> getables_db(Class<?> clazz, Connection connection) {
        try {
            Statement sta = connection.createStatement();
            ResultSet res = sta.executeQuery("select tbl_name as columns from sqlite_master where type='table'");
            Set<String> columns = new HashSet<>();
            while (res.next()) {
                columns.add(res.getString("columns"));
            }
            return columns;
        } catch (Exception e) {
            return null;
        }
    }


//    public static void migration(Class<? extends SQLObject> clazz, Connection connection, String table) {
//        if (isMigration(clazz)) {
//            Statement sta = null;
//            try {
//                sta = connection.createStatement();
//                if (migration(clazz) == null) {
//                    throw new RuntimeException("마이그래이션할 오브젝트에 migration 메소드가 정의되지 않았거나 null 값입니다.");
//                } else {
//                    HashMap<String, Object> migration = migration(clazz);
//                    Set<String> columns_db = getcolumns_db(clazz);
//                    LinkedHashMap<String, Class<?>> getcolumns = getcolumns(clazz);
//                    Set<String> columns = getcolumns.keySet();
//
//                    Set<String> alter_columns = new HashSet<>(columns);
//                    alter_columns.remove(columns_db);
//                    if (alter_columns.size() != 0) {
//                        for (String name : alter_columns) {
//                            Class<?> classes = getcolumns.get(name);
//                            String type = getcolumn(classes);
//                            String sql = "alter table " + table + " add " + name + " " + type;
//                            sta.execute(sql);
//                        }
//                    }
//                    alter_columns = new HashSet<>(columns_db);
//                    alter_columns.removeAll(columns);
//                    if (alter_columns.size() != 0) {
//                        for (String name : columns) {
//                            String sql = "alter table " + table + " drop column " + name;
//                            sta.execute(sql);
//                        }
//                    }
//                }
//                for (Map.Entry<String, Field> entry : tableList(clazz).entrySet()) {
//                    migrationList(connection, entry.getValue(), tablename(table, entry.getKey()), clazz);
//                }
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private static void migrationList(Connection connection, Field field, String table, Class<? extends SQLObject> superClass) {
//        try {
//            Statement sta = connection.createStatement();
//            Class<?> clazz = getgeneric_1(field);
//            Set<String> table_db = getables_db(clazz, connection);
//            LinkedHashMap<String, Class<?>> getlists = getlists(superClass);
//            Set<String> lists = getlists.keySet();
//
//            Set<String> alter_tables = new HashSet<>(lists);
//            alter_tables.removeAll(table_db);
//            if (alter_tables.size() != 0) {
//                for (String name : alter_tables) {
//                    String sql = "drop table " + name;
//                    sta.execute(sql);
//                }
//            }
//
//            alter_tables = new HashSet<>(table_db);
//            alter_tables.removeAll(lists);
//            if (alter_tables.size() != 0) {
//                for (String name : alter_tables) {
//                    Class<? extends SQLObject> sqlObject = (Class<? extends SQLObject>) clazz;
//                    createList(sqlObject, tablename(table, name), 13);
//                }
//            }
//
////            if (hasSQLObject(clazz)) {
////                Class<? extends SQLObject> sqlObject = (Class<? extends SQLObject>) clazz;
////                migration(sqlObject, connection, table);
////            } else {
////
////            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    @SuppressWarnings("unchecked")
    public static List<Object> loadList(Connections connections, Field field, String tableName, Class<? extends SQLObject> superClass, String primary) {
        final List<Object> list = new ArrayList<>();
        Class<?> generic = getgeneric_1(field);
        LinkedHashMap<String, Object> instance;
        String sql;
        sql = "select * from " + tableName + " where " + primaryKey(superClass) + "=?" + " order by indexs asc";
        try {
            PreparedStatement sta = connections.connection.prepareStatement(sql);
            sta.setObject(1, primary);
            ResultSet res = sta.executeQuery();
            while (res.next()) {
                instance = new LinkedHashMap<>();
                if (hasSQLObject(generic)) {
                    loadingCore(connections, tableName, (Class<? extends SQLObject>) generic, instance, res);
                    list.add(deserialize((Class<? extends SQLObject>) generic, instance));
                } else {
                    String col = getclassname(generic);
                    list.add(res.getObject(col));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static boolean hasSQLObject(Class<?> generic) {
        return Arrays.stream(generic.getInterfaces()).filter(aClass -> aClass.equals(SQLObject.class)).findFirst().orElse(null) != null;
    }

    public static SQLObject deserialize(Class<? extends SQLObject> clazz, LinkedHashMap<String, Object> hash) {
        Object[] objects = null;
        Class<?>[] classes = null;
        List<Field> fields = new ArrayList<>();
        HashMap<String, Object> migration = migration(clazz);
        try {
            fields = fields(clazz);
            objects = new Object[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                String name = fields.get(i).getName();
                objects[i] = hash.getOrDefault(name, migration == null ? null : migration.getOrDefault(name, null));
            }
            classes = new Class[fields.size()];
            for (int i = 0; i < classes.length; i++) {
                try {
                    if (objects[i] != null)
                        classes[i] = objects[i].getClass();
                    else classes[i] = fields.get(i).getClass();
                } catch (NullPointerException e) {
                    classes[i] = fields.get(i).getClass();
                }
            }
            Constructor<? extends SQLObject> constructor;
            constructor = clazz.getConstructor(classes);
            constructor.setAccessible(true);
            return constructor.newInstance(objects);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (DEBUGGING) {
                System.err.println("deserialize : " + Arrays.stream(objects).map(ob -> ob + " : " + ob.getClass().getSimpleName()).toList());
                System.err.println("deserialize : " + Arrays.toString(classes));
                System.err.println("deserialize : " + fields.stream().map(Field::getName).toList());
            }
        }
    }
}
