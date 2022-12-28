package io.github.theminiluca.sql;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public final class SQLManager {

    public static boolean DEBUGGING = false;

    public static final HashMap<String, Connections> CONNECTIONS_CLASS = new HashMap<>();

    public static <T extends SQLObject> void sqlite(Class<? extends SQLObject> clazz, Connections connections, HashMap<String, T> hash) {
        createTable(clazz, connections);
        migration(clazz, connections);
        try {
            SQLManager.load(clazz, hash);
        } catch (Exception e) {

        }
    }
    @SuppressWarnings("unchecked")
    public static void loadHash(Class<? extends SQLObject> clazz, HashMap<String, Object> instance, String primary) {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        if (connections == null) {
            throw new RuntimeException("first. create driver");
        }
        LinkedHashMap<String, Field> tables = tableHash(clazz);
        String sql;
        for (Map.Entry<String, Field> entry : tables.entrySet()) {
            String tableName = connections.name + "_" + entry.getValue().getName();
            sql = "select * from " + tableName + " where " + primaryKey(clazz) + "=" + wrapperMark(primary);
            try {
                Statement sta = connections.connection.createStatement();
                ResultSet res = sta.executeQuery(sql);
                Map<String, Object> hash = new HashMap<>();
                while (res.next()) {
                    Class<?> generic = getgeneric_2(entry.getValue());
                    if (getcolumn(generic) == null) {
                        LinkedHashMap<String, Object> tableHash = new LinkedHashMap<>();
                        for (String col : getcolumns((Class<? extends SQLObject>) generic).keySet()) {
                            if (generic.equals(long.class) || generic.equals(Long.class)) {
                                tableHash.put(col, res.getLong(col));
                            } else if (generic.equals(int.class) || generic.equals(Integer.class)) {
                                tableHash.put(col, res.getInt(col));
                            } else if (generic.equals(UUID.class)) {
                                tableHash.put(col, UUID.fromString(res.getString(col)));
                            } else if (generic.equals(String.class)) {
                                tableHash.put(col, res.getString(col));
                            } else if (generic.equals(boolean.class) || generic.equals(Boolean.class)) {
                                tableHash.put(col, res.getBoolean(col));
                            }
                        }
                        hash.put(res.getString("keys"), deserialize((Class<SQLObject>) getgeneric_1(entry.getValue()), tableHash));
                    } else {
                        Class<?> classes = getgeneric_1(entry.getValue());
                        String col = getcolumn(classes);
                        if (classes.equals(long.class) || classes.equals(Long.class)) {
                            hash.put(res.getString("keys"), res.getLong(col));
                        } else if (classes.equals(int.class) || classes.equals(Integer.class)) {
                            hash.put(res.getString("keys"), res.getInt(col));
                        } else if (classes.equals(UUID.class)) {
                            hash.put(res.getString("keys"), UUID.fromString(res.getString(col)));
                        } else if (classes.equals(String.class)) {
                            hash.put(res.getString("keys"), res.getString(col));
                        } else if (classes.equals(boolean.class) || classes.equals(Boolean.class)) {
                            hash.put(res.getString("keys"), res.getBoolean(col));
                        }
                    }
                }
                instance.put(entry.getKey(), hash);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (DEBUGGING)
                    System.out.println(sql);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadList(Class<? extends SQLObject> clazz, HashMap<String, Object> instance, String primary) {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        if (connections == null) {
            throw new RuntimeException("first. create driver");
        }
        LinkedHashMap<String, Field> tables = tableList(clazz);
        String sql;
        for (Map.Entry<String, Field> entry : tables.entrySet()) {
            String tableName = connections.name + "_" + entry.getValue().getName();
            sql = "select * from " + tableName + " where " + primaryKey(clazz) + "=" + wrapperMark(primary) + " order by indexs asc";
            try {
                Statement sta = connections.connection.createStatement();
                ResultSet res = sta.executeQuery(sql);
                List<Object> list = new ArrayList<>();
                while (res.next()) {
                    Class<?> generic = getgeneric_1(entry.getValue());
                    if (getcolumn(generic) == null) {
                        LinkedHashMap<String, Object> tableList = new LinkedHashMap<>();
                        for (String col : getcolumns((Class<? extends SQLObject>) generic).keySet()) {
                            if (generic.equals(long.class) || generic.equals(Long.class)) {
                                tableList.put(col, res.getLong(col));
                            } else if (generic.equals(int.class) || generic.equals(Integer.class)) {
                                tableList.put(col, res.getInt(col));
                            } else if (generic.equals(UUID.class)) {
                                tableList.put(col, UUID.fromString(res.getString(col)));
                            } else if (generic.equals(String.class)) {
                                tableList.put(col, res.getString(col));
                            } else if (generic.equals(boolean.class) || generic.equals(Boolean.class)) {
                                tableList.put(col, res.getBoolean(col));
                            }
                        }
                        list.add(deserialize((Class<SQLObject>) getgeneric_1(entry.getValue()), tableList));
                    } else {
                        Class<?> classes = getgeneric_1(entry.getValue());
                        String col = getcolumn(classes);
                        if (classes.equals(long.class) || classes.equals(Long.class)) {
                            list.add(res.getLong(col));
                        } else if (classes.equals(int.class) || classes.equals(Integer.class)) {
                            list.add(res.getInt(col));
                        } else if (classes.equals(UUID.class)) {
                            list.add(UUID.fromString(res.getString(col)));
                        } else if (classes.equals(String.class)) {
                            list.add(res.getString(col));
                        } else if (classes.equals(boolean.class) || classes.equals(Boolean.class)) {
                            list.add(res.getBoolean(col));
                        }
                    }
                }
                instance.put(entry.getKey(), list);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (DEBUGGING)
                    System.out.println(sql);
            }
        }
    }


    @SuppressWarnings("unchecked")
    public static <T extends SQLObject> void load(Class<? extends SQLObject> clazz, HashMap<String, T> hash) throws Exception {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        String sql = "SELECT * FROM " + connections.name;
        LinkedHashMap<String, Class<?>> tableTypes = getcolumns(clazz);

        LinkedHashMap<String, Object> instance = new LinkedHashMap<>();

        try {
            Statement sta = connections.connection.createStatement();
            ResultSet res = sta.executeQuery(sql);
            while (res.next()) {
                for (Map.Entry<String, Class<?>> entry : tableTypes.entrySet()) {
                    String name = entry.getKey();
                    Class<?> classes = entry.getValue();
                    if (classes.equals(long.class)) {
                        instance.put(name, res.getLong(name));
                    } else if (classes.equals(int.class)) {
                        instance.put(name, res.getInt(name));
                    } else if (classes.equals(UUID.class)) {
                        instance.put(name, UUID.fromString(res.getString(name)));
                    } else if (classes.equals(String.class)) {
                        instance.put(name, res.getString(name));
                    } else if (classes.equals(boolean.class)) {
                        instance.put(name, res.getBoolean(name));
                    }
                }
                loadList(clazz, instance, res.getString(primaryKey(clazz)));
                loadHash(clazz, instance, res.getString(primaryKey(clazz)));
                hash.put(res.getString(primaryKey(clazz)), (T) deserialize(clazz, instance));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (DEBUGGING)
                System.out.println(sql);
        }

    }


    private static String wrapperMark(String text) {
        return "\"" +
                text +
                "\"";
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
        try {
            Statement statement = connections.connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM " + connections.name + " WHERE " + primaryKey(object.getClass()) + "="
                    + wrapperMark(serialize(object).get(primaryKey(object.getClass())).toString()));
            return result.next();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
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
    private static void createList(Class<? extends SQLObject> clazz, String columnName, Class<?> generic) {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        String sql = null;
        try {
            Statement statement = connections.connection.createStatement();
            String primary = getcolumn(getcolumns(clazz).get(primaryKey(clazz)));
            String column = getcolumn(generic);
            if (column == null) {
                sql = "CREATE TABLE IF NOT EXISTS " + connections.name + "_" + columnName +
                        "(" + primaryKey(clazz) + " " + primary + ", indexs INT, " + columns((Class<? extends SQLObject>) generic, false, true) + ")";
            } else {
                sql = "CREATE TABLE IF NOT EXISTS " + connections.name + "_" + columnName +
                        "(" + primaryKey(clazz) + " " + primary + ", indexs INT, " + column.toLowerCase() + " " + column + ")";
            }
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (DEBUGGING)
                System.out.println(sql);
        }
    }

    @SuppressWarnings("unchecked")
    private static void createHash(Class<? extends SQLObject> clazz, String columnName, Class<?> generic) {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        String sql = null;
        try {
            Statement statement = connections.connection.createStatement();
            String primary = getcolumn(getcolumns(clazz).get(primaryKey(clazz)));
            String column = getcolumn(generic);
            if (column == null) {
                sql = "CREATE TABLE IF NOT EXISTS " + connections.name + "_" + columnName +
                        "(" + primaryKey(clazz) + " " + primary + ", keys LONGTEXT, " + columns((Class<? extends SQLObject>) generic, false, true) + ")";
            } else {
                sql = "CREATE TABLE IF NOT EXISTS " + connections.name + "_" + columnName +
                        "(" + primaryKey(clazz) + " " + primary + ", keys LONGTEXT, " + column.toLowerCase() + " " + column + ")";
            }
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (DEBUGGING)
                System.out.println(sql);
        }
    }

    public static void save(SQLObject object) {
        Connections connections = getDriver(object);
        String sql;
        if (!exists(object, connections))
            sql = "INSERT INTO " + connections.name + "(" + columns(object.getClass(), true) + ") VALUES (" + values(object) + ")";
        else
            sql = "UPDATE " + connections.name + " SET " + setvalues(object) + " WHERE " + primaryKey(object.getClass()) + "=" + wrapperMark(serialize(object).get(primaryKey(object.getClass())).toString());
        try {
            PreparedStatement statement = connections.connection.prepareStatement(sql);
            statement.execute();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            if (DEBUGGING)
                System.out.println(sql);
        }
        saveList(object);
        saveHash(object);
    }

    @SuppressWarnings("unchecked")
    public static void saveHash(SQLObject object) {
        Connections connections = getDriver(object);
        String sql = null;
        for (Map.Entry<String, Field> entry : tableHash(object.getClass()).entrySet()) {
            try {
                String tableName = connections.name + "_" + entry.getValue().getName();
                sql = "DELETE FROM " + tableName + " WHERE " + primaryKey(object.getClass()) + "=" + wrapperMark(serialize(object).get(primaryKey(object.getClass())).toString());
                PreparedStatement statement = connections.connection.prepareStatement(sql);
                statement.execute();
                Object ob = entry.getValue().get(object);
                for (Map.Entry<String, Object> obj : ((Map<String, Object>) ob).entrySet()) {
                    String primary = getcolumn(getcolumns(object.getClass()).get(primaryKey(object.getClass())));
                    String column = getcolumn(obj.getValue().getClass());
                    if (column == null) {
                        sql = "INSERT INTO " + tableName + "(" + primaryKey(object.getClass()) + ", keys," + columns((Class<? extends SQLObject>) obj.getValue().getClass(), false) +
                                ") values (" + primary + " " + wrapperMark(obj.getKey()) + " " + values((SQLObject) obj.getValue());
                    } else {
                        sql = "INSERT INTO " + tableName + "(" + primaryKey(object.getClass()) + ", keys," + column +
                                ") values (" + value(serialize(object).get(primaryKey(object.getClass()))) + ", " + wrapperMark(obj.getKey()) + ", " + value(obj.getValue()) + ")";
                    }
                    statement = connections.connection.prepareStatement(sql);
                    statement.execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(entry.getKey() + "에서 오류.");
            } finally {
                if (DEBUGGING)
                    System.out.println(sql);
            }
        }

    }


    @SuppressWarnings("unchecked")
    public static void saveList(SQLObject object) {
        Connections connections = getDriver(object);
        String sql = null;
        for (Map.Entry<String, Field> entry : tableList(object.getClass()).entrySet()) {
            try {
                String tableName = connections.name + "_" + entry.getValue().getName();
                sql = "DELETE FROM " + tableName + " WHERE " + primaryKey(object.getClass()) + "=" + wrapperMark(serialize(object).get(primaryKey(object.getClass())).toString());
                PreparedStatement statement = connections.connection.prepareStatement(sql);
                statement.execute();
                int n = 0;
                for (Object obj : (List<?>) entry.getValue().get(object)) {
                    String primary = getcolumn(getcolumns(object.getClass()).get(primaryKey(object.getClass())));
                    String column = getcolumn(obj.getClass());
                    if (column == null) {
                        sql = "INSERT INTO " + tableName + "(" + primaryKey(object.getClass()) + ", indexs," + columns((Class<? extends SQLObject>) obj.getClass(), false) +
                                ") values (" + primary + " " + n + " " + values((SQLObject) obj);
                    } else {
                        sql = "INSERT INTO " + tableName + "(" + primaryKey(object.getClass()) + ", indexs," + column +
                                ") values (" + value(serialize(object).get(primaryKey(object.getClass()))) + ", " + n + ", " + value(obj) + ")";
                    }
                    statement = connections.connection.prepareStatement(sql);
                    statement.execute();
                    n++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(entry.getKey() + "에서 오류.");
            } finally {
                if (DEBUGGING)
                    System.out.println(sql);
            }
        }

    }

    public static Connections getDriver(SQLObject object) throws RuntimeException {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(object.getClass().getName(), null);
        if (connections == null) {
            throw new RuntimeException("driver is not exists");
        }
        return connections;
    }

    public static void createTable(Class<? extends SQLObject> clazz, Connections connection) {
        String sql = null;
        try {
            Statement statement = connection.connection.createStatement();
            sql = "CREATE TABLE IF NOT EXISTS " + connection.name + "(" + columns(clazz, false) + ")";
            CONNECTIONS_CLASS.put(clazz.getName(), connection);
            statement.execute(sql);
            connection.connection.prepareStatement("PRAGMA busy_timeout = 30000").execute();
            for (Field column : tableList(clazz).values()) {
                String columnName = column.getName();
                createList(clazz, columnName, getgeneric_1(column));
            }
            for (Field column : tableHash(clazz).values()) {
                String columnName = column.getName();
                createHash(clazz, columnName, getgeneric_1(column));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (DEBUGGING)
                System.out.println(sql);
        }
    }


    public static void migration(Class<? extends SQLObject> clazz, Connections connections) {
        try {
            Statement sta = connections.connection.createStatement();
            ResultSet res = sta.executeQuery("PRAGMA table_info(" + connections.name + ")");
            Set<String> columns = new HashSet<>();
            while (res.next()) {
                columns.add(res.getString("name"));
            }
            Set<String> columns1 = new HashSet<>(getcolumns(clazz).keySet());
            columns1.removeAll(columns);
            if (columns1.size() != 0) {
                for (String name : columns1) {
                    Class<?> classes = getcolumns(clazz).get(name);
                    String type = getcolumn(classes);
                    String sql = "ALTER TABLE " + connections.name + " ADD " + name + " " + type;
                    sta.execute(sql);
                }
            }

            columns1 = new HashSet<>(getcolumns(clazz).keySet());
            columns.removeAll(columns1);
            if (columns.size() != 0) {
                for (String name : columns) {
                    String sql = "ALTER TABLE " + connections.name + " DROP COLUMN " + name;
                    sta.execute(sql);
                }
            }
            migrationList(clazz, connections);
            migrationHash(clazz, connections);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @SuppressWarnings("unchecked")
    private static void migrationHash(Class<? extends SQLObject> clazz, Connections connections) {
        try {
            Statement sta = connections.connection.createStatement();
            ResultSet res = sta.executeQuery("select * from sqlite_master");
            Set<String> tables = new HashSet<>();
            while (res.next()) {
                tables.add(res.getString("tbl_name"));
            }
            Set<String> tables1 = new HashSet<>(tableHash(clazz).keySet().stream().map(text -> connections.name + "_" + text).toList());
            tables1.removeAll(tables);
            if (tables1.size() != 0) {
                for (String name : tables1) {
                    String sql = "DROP TABLE " + name;
                    sta.execute(sql);
                }
            }

            tables1 = new HashSet<>(tableHash(clazz).keySet().stream().map(text -> connections.name + "_" + text).toList());
            tables.removeAll(tables1);
            if (tables.size() != 0) {
                for (String name : tables) {
                    Class<? extends SQLObject> classes = (Class<? extends SQLObject>) tableHash(clazz).get(name).getType();
                    createHash(classes, name, getgeneric_2(tableList(clazz).get(name)));
                }
            }
        } catch (Exception e) {

        }
    }
    @SuppressWarnings("unchecked")
    private static void migrationList(Class<? extends SQLObject> clazz, Connections connections) {
        try {
            Statement sta = connections.connection.createStatement();
            ResultSet res = sta.executeQuery("select * from sqlite_master");
            Set<String> tables = new HashSet<>();
            while (res.next()) {
                tables.add(res.getString("tbl_name"));
            }
            Set<String> tables1 = new HashSet<>(tableList(clazz).keySet().stream().map(text -> connections.name + "_" + text).toList());
            tables1.removeAll(tables);
            if (tables1.size() != 0) {
                for (String name : tables1) {
                    String sql = "DROP TABLE " + name;
                    sta.execute(sql);
                }
            }

            tables1 = new HashSet<>(tableList(clazz).keySet().stream().map(text -> connections.name + "_" + text).toList());
            tables.removeAll(tables1);
            if (tables.size() != 0) {
                for (String name : tables) {
                    Class<? extends SQLObject> classes = (Class<? extends SQLObject>) tableList(clazz).get(name).getType();
                    createList(classes, name, getgeneric_1(tableList(clazz).get(name)));
                }
            }
        } catch (Exception e) {

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

    private static String setvalues(SQLObject object) {
        return values(object, true);
    }

    private static String values(SQLObject object) {
        return values(object, false);
    }

    private static String value(Object ob) {
        StringBuilder builder = new StringBuilder();
        if (ob == null) {
            builder.append("null");
        } else if (ob instanceof Integer) {
            builder.append((int) ob);
        } else if (ob instanceof UUID) {
            builder.append("\"%s\"".formatted(ob));
        } else if (ob instanceof String) {
            builder.append("\"%s\"".formatted(ob));
        } else if (ob instanceof Long) {
            builder.append((long) ob);
        } else if (ob instanceof Boolean) {
            builder.append((boolean) ob);
        }
        return builder.toString();
    }

    private static String values(SQLObject object, boolean update) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Object> entry : serialize(object).entrySet()) {
            Object ob = entry.getValue();
            String name = entry.getKey();
            if (update) builder.append(name).append("=");
            builder.append(value(ob));
            builder.append(i == serialize(object).size() - 1 ? "" : ", ");
            i++;
        }
        return builder.toString();
    }


    private static <T extends SQLObject> LinkedHashMap<String, Field> tableList(Class<T> clazz) {
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(field
                -> field.isAnnotationPresent(SQL.class) && field.getType().equals(List.class)).toList();
        LinkedHashMap<String, Field> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            hash.put(field.getName(), field);
        }
        return hash;
    }

    private static <T extends SQLObject> LinkedHashMap<String, Field> tableHash(Class<T> clazz) {
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(field
                -> field.isAnnotationPresent(SQL.class) && field.getType().equals(Map.class)).toList();
        LinkedHashMap<String, Field> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            hash.put(field.getName(), field);
        }
        return hash;
    }

    private static <T extends SQLObject> LinkedHashMap<String, Class<?>> classArgument(Class<T> clazz) {
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(field
                -> field.isAnnotationPresent(SQL.class) && !field.getType().equals(List.class)).toList();
        LinkedHashMap<String, Class<?>> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            hash.put(field.getName(), field.getType());
        }
        return hash;
    }


    private static LinkedHashMap<String, Class<?>> getcolumns(Class<? extends SQLObject> clazz) {
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(field
                -> field.isAnnotationPresent(SQL.class) && !field.getType().equals(List.class)).toList();
        LinkedHashMap<String, Class<?>> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            hash.put(field.getName(), field.getType());
        }
        return hash;
    }

    private static <T extends SQLObject> LinkedHashMap<String, Object> serialize(T object) {
        List<Field> fields = Arrays.stream(object.getClass().getDeclaredFields()).filter(field ->
                field.isAnnotationPresent(SQL.class) && !field.getType().equals(List.class) && !field.getType().equals(Map.class)).toList();
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

    private static String primaryKey(Class<? extends SQLObject> clazz) {
        try {
            return Objects.requireNonNull(Arrays.stream(clazz.getDeclaredFields()).filter(field ->
                    field.isAnnotationPresent(SQL.class) && field.getAnnotation(SQL.class).primary()).findFirst().orElse(null)).getName();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public static <T extends SQLObject> SQLObject deserialize(Class<T> clazz, LinkedHashMap<String, Object> hash) {
        try {
            List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(field ->
                    field.isAnnotationPresent(SQL.class)).toList();
            Object[] objects = new Object[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                String name = fields.get(i).getName();
                objects[i] = hash.getOrDefault(name, null);
            }
            Class<?>[] classes = new Class[fields.size()];
            for (int i = 0; i < classes.length; i++) {
                classes[i] = objects[i].getClass();
            }
            Constructor<T> constructor;
            constructor = clazz.getConstructor(classes);
            constructor.setAccessible(true);
            return constructor.newInstance(objects);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
