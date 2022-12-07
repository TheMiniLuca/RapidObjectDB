package io.github.theminiluca.sql;

import test.User;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

public final class SQLManager {

    public static final HashMap<String, Connections> CONNECTIONS_CLASS = new HashMap<>();


    @SuppressWarnings("unchecked")
    public static <T extends SQLObject> void load(Class<? extends SQLObject> clazz, HashMap<String, T> hash) {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        if (connections != null) {
            String sql = "SELECT * FROM " + connections.name;
            LinkedHashMap<String, Class<?>> tableTypes = createTable(clazz);

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
                        }
                    }
                    hash.put(res.getString(primaryKey(clazz)), (T) deserialize(clazz, instance));
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("first. create driver");
        }
    }

    public static void save(SQLObject object) {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(object.getClass().getName(), null);
        if (connections != null) {
            String sql;
            if (!exists(object, connections))
                sql = "INSERT INTO " + connections.name + "(" + columns(object.getClass(), true) + ") VALUES (" + values(object) + ")";
            else
                sql = "UPDATE " + connections.name + " SET " + setvalues(object) + " WHERE " + primaryKey(object.getClass()) + "=" + wrapperMark(serialize(object).get(primaryKey(object.getClass())).toString());

            try {
                PreparedStatement statement = connections.connection.prepareStatement(sql);
                statement.executeUpdate();
                statement.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("first. create driver");
        }
    }

    private static String wrapperMark(String text) {
        return "\"" +
                text +
                "\"";
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

    public static void createTable(Class<? extends SQLObject> clazz, Connections connection) {
        if (hasDefaultsMethod(clazz)) {
            try {
                Statement statement = connection.connection.createStatement();
                String sql = "CREATE TABLE IF NOT EXISTS " + connection.name + "(" + columns(clazz, false) + ")";
                CONNECTIONS_CLASS.put(clazz.getName(), connection);
                statement.execute(sql);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("defaults method is not exists");
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
            Set<String> columns1 = new HashSet<>(createTable(clazz).keySet());
            columns1.removeAll(columns);
            if (columns1.size() != 0) {
                for (String name : columns1) {
                    Class<?> classes = createTable(clazz).get(name);
                    String type = getcolumn(classes);
                    String sql = "ALTER TABLE " + connections.name + " ADD " + name + " " + type;
                    sta.execute(sql);
                }
            }

            columns1 = new HashSet<>(createTable(clazz).keySet());
            columns.removeAll(columns1);
            if (columns.size() != 0) {
                for (String name : columns) {
                    String sql = "ALTER TABLE " + connections.name + " DROP COLUMN " + name;
                    sta.execute(sql);
                }
            }

        } catch (Exception e) {

        }
    }


    @SuppressWarnings("unchecked")
    private static String columns(Class<? extends SQLObject> clazz, boolean noType) {
        try {
            StringBuilder builder = new StringBuilder();
            LinkedHashMap<String, Class<?>> hash = createTable(clazz);
            int i = 0;
            for (Map.Entry<String, Class<?>> entry : hash.entrySet()) {
                String name = entry.getKey();
                Class<?> classes = entry.getValue();
                String type;
                if (!noType) {
                    type = getcolumn(classes);
                    builder.append("%s %s".formatted(name, type));
                    if (name.equals(primaryKey(clazz))) {
                        builder.append(" primary key");
                    }
                    builder.append(i == hash.size() - 1 ? "" : ",");
                } else {
                    builder.append("%s".formatted(name)).append(i == hash.size() - 1 ? "" : ",");
                }
                i++;
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getcolumn(Class<?> classes) {
        String type;
        if (classes.equals(long.class)) {
            type = "LONG";
        } else if (classes.equals(int.class)) {
            type = "INT";
        } else if (classes.equals(UUID.class)) {
            type = "VARCHAR(32)";
        } else if (classes.equals(String.class)) {
            type = "LONGTEXT";
        } else throw new IllegalArgumentException("지원하지 않는 컬럼값입니다.");
        return type;
    }

    private static String setvalues(SQLObject object) {
        return values(object, true);
    }

    private static String values(SQLObject object) {
        return values(object, false);
    }

    private static String values(SQLObject object, boolean update) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Object> entry : serialize(object).entrySet()) {
            Object ob = entry.getValue();
            String name = entry.getKey();
            if (update) builder.append(name).append("=");
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
            }
            builder.append(i == serialize(object).size() - 1 ? "" : ", ");
            i++;
        }
        return builder.toString();
    }

    public static boolean hasDefaultsMethod(Class<? extends SQLObject> clazz) {
        try {
            clazz.getDeclaredMethod("primaryKey");
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    private static LinkedHashMap<String, Class<?>> createTable(Class<? extends SQLObject> clazz) {
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(SQL.class)).toList();
        LinkedHashMap<String, Class<?>> hash = new LinkedHashMap<>();
        for (Field field : fields) {
            hash.put(field.getName(), field.getType());
        }
        return hash;
    }

    private static <T extends SQLObject> LinkedHashMap<String, Object> serialize(T object) {
        List<Field> fields = Arrays.stream(object.getClass().getDeclaredFields()).filter(field ->
                field.isAnnotationPresent(SQL.class)).toList();
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
            return (String) clazz.getDeclaredMethod("primaryKey").invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T extends SQLObject> T deserialize(Class<T> clazz, Map<String, Object> hash) {
        try {
            List<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(field ->
                    field.isAnnotationPresent(SQL.class)).toList();
            Class<?>[] classes = new Class[fields.size()];
            for (int i = 0; i < classes.length; i++) {
                classes[i] = fields.get(i).getType();
            }
            Object[] objects = new Object[fields.size()];
            for (int i = 0; i < classes.length; i++) {
                String name = fields.get(i).getName();
                objects[i] = hash.getOrDefault(name, null);
            }
            Constructor<T> constructor;
            constructor = clazz.getConstructor(classes);
            constructor.setAccessible(true);
            return constructor.newInstance(objects);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
