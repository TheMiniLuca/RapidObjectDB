package io.github.theminiluca.sql;

import test.User;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.*;

public final class SQLManager {

    public static final HashMap<String, Connections> CONNECTIONS_CLASS = new HashMap<>();


    @SuppressWarnings("unchecked")
    public static <T extends SQLObject> void load(Class<?> clazz, HashMap<String, T> hash) {
        Connections connections = CONNECTIONS_CLASS.getOrDefault(clazz.getName(), null);
        if (connections != null) {
            String sql = "SELECT * FROM " + connections.name;
            LinkedHashMap<String, Class<?>> tableTypes = createTable(clazz);

            LinkedHashMap<String, Object> instance = new LinkedHashMap<>();

            try {
                Statement sta = connections.connection.createStatement();
                ResultSet res = sta.executeQuery(sql);
                Method deserialize = deserialize(clazz);
                while (res.next()) {
                    for (Map.Entry<String, Class<?>> entry : tableTypes.entrySet()) {
                        String name = entry.getKey();
                        Class<?> classes = entry.getValue();
                        if (classes.equals(int.class)) {
                            instance.put(name, res.getInt(name));
                        } else if (classes.equals(UUID.class)) {
                            instance.put(name, UUID.fromString(res.getString(name)));
                        } else if (classes.equals(String.class)) {
                            instance.put(name, res.getString(name));
                        }
                    }
                    hash.put(res.getString(primaryKey(clazz)), (T) deserialize.invoke(null, instance));
                }
            } catch (Exception e) {
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
                sql = "UPDATE " + connections.name + " SET " + setvalues(object) + " WHERE " + primaryKey(object.getClass()) + "=" + wrapperMark(object.serialize().get(primaryKey(object.getClass())).toString());

            try {
                PreparedStatement statement = connections.connection.prepareStatement(sql);
                statement.executeUpdate();
                statement.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                System.out.println(sql);
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
            ResultSet result = statement.executeQuery("SELECT * FROM " + connections.name + " WHERE " + primaryKey(object.getClass()) + "=" + wrapperMark(object.serialize().get(primaryKey(object.getClass())).toString()));
            return result.next();
        } catch (Exception e) {
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

    public static void createTable(Class<?> clazz, Connections connection) {
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



    public static void migration(Class<?> clazz, Connections connections) {
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
                    String sql = "ALTER TABLE " + connections.name + " ADD " + name + " TEXT";
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
    private static String columns(Class<?> clazz, boolean noType) {
        try {
            StringBuilder builder = new StringBuilder();
            LinkedHashMap<String, Class<?>> hash = createTable(clazz);
            int i = 0;
            for (Map.Entry<String, Class<?>> entry : hash.entrySet()) {
                String name = entry.getKey();
                if (!noType) {
                    builder.append("%s TEXT".formatted(name));
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

    private static String setvalues(SQLObject object) {
        return values(object, true);
    }

    private static String values(SQLObject object) {
        return values(object, false);
    }

    private static String values(SQLObject object, boolean update) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, Object> entry : object.serialize().entrySet()) {
            Object ob = entry.getValue();
            String name = entry.getKey();
            if (update) builder.append(name).append("=");
            if (ob == null) {
                builder.append("null");
            }else
            if (ob instanceof Integer) {
                builder.append((int) ob);
            } else if (ob instanceof UUID) {
                builder.append("\"%s\"".formatted(ob));
            } else if (ob instanceof String) {
                builder.append("\"%s\"".formatted(ob));
            }
            builder.append(i == object.serialize().size() - 1 ? "" : ", ");
            i++;
        }
        return builder.toString();
    }

    public static boolean hasDefaultsMethod(Class<?> clazz) {
        try {
            clazz.getDeclaredMethod("createTable");
            clazz.getDeclaredMethod("primaryKey");
            clazz.getDeclaredMethod("deserialize", LinkedHashMap.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    @SuppressWarnings("unchecked")
    private static LinkedHashMap<String, Class<?>> createTable(Class<?> clazz) {
        try {
            return (LinkedHashMap<String, Class<?>>) clazz.getDeclaredMethod("createTable").invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static String primaryKey(Class<?> clazz) {
        try {
            return (String) clazz.getDeclaredMethod("primaryKey").invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Method deserialize(Class<?> clazz) {
        try {
            return clazz.getDeclaredMethod("deserialize", LinkedHashMap.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
