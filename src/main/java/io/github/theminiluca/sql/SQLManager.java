package io.github.theminiluca.sql;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SQLManager {
    private Connection connection;

    public static String KEY_COLUMNS_NAME = "keys";
    public static String VALUE_COLUMNS_NAME = "json";

    private final String user;
    private final String password;
    private String host;
    private int port;

    public SQLManager(String user, String password) {
        this.user = user;
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
    public Connection getConnection() {
        return connection;
    }


    // 데이터베이스에 연결해주는 함수 만들어줘
    public void connect(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            // MariaDB 클래스 로드
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mariadb://%s:%s/minecraft".formatted(host, port),
                    user, password
            );
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void createTable(Class<? extends SQLObject> clazz) {
        try {
            Statement statement = connection.createStatement();
            statement.execute("create table if not exists `" + tablename(clazz) + "` (`%s` TEXT, `%s` LONGTEXT);".formatted(KEY_COLUMNS_NAME, VALUE_COLUMNS_NAME));
//            connection.prepareStatement("pragma busy_timeout = 30000").execute(); -> SQLite 문법 / MariaDB에서 불필요
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private String tablename(Class<? extends SQLObject> clazz) {
        return clazz.getSimpleName().toLowerCase();
    }

    // 해쉬맵에 Map<String, Serializable> 이걸 데이터 베이스에 저장 하는 메드홉 작성해줘
    public void saveMap(Class<? extends SQLObject> clazz, Map<String, SQLObject> map) {
        try {
            Statement stmt = connection.createStatement();

            for (Map.Entry<String, SQLObject> val : map.entrySet()) {
                stmt.execute("INSERT INTO `%s` (`%s`, `%s`) VALUES ('%s', '%s') ON DUPLICATE KEY UPDATE `%s`='%s', `%s`='%s';"
                        .formatted(tablename(clazz), KEY_COLUMNS_NAME, VALUE_COLUMNS_NAME, val.getKey(), serialize(val.getValue()), KEY_COLUMNS_NAME, val.getKey(), VALUE_COLUMNS_NAME, serialize(val.getValue())));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends SQLObject> Map<String, T> loadMap(Class<T> clazz) {
        try {
            ResultSet set = connection.prepareStatement("SELECT * FROM " + tablename(clazz)).executeQuery();

            Map<String, T> map = new HashMap<>();

            while (set.next()) {
                map.put(set.getString(1), deserialize(set.getString(2)));
            }

            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private  <T extends SQLObject> String serialize(T t) {
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

    public static Class<?> getgeneric_2(Field field) {
        ParameterizedType generic = (ParameterizedType) field.getGenericType();
        return (Class<?>) generic.getActualTypeArguments()[1];
    }
    @SuppressWarnings("unchecked")
    public void autoSave(Class<?> dataClass) {
        //long def = System.currentTimeMillis();
        for (Field field : dataClass.getDeclaredFields()) {
//            long ms = System.currentTimeMillis();
            if (field.isAnnotationPresent(Save.class)) {
                try {
                    Class<? extends SQLObject> clazz = (Class<? extends SQLObject>) getgeneric_2(field);
                    HashMap<String, SQLObject> hash = new HashMap<>((HashMap<String, SQLObject>) field.get(null));
                    saveMap(clazz, hash);
//                    logger.info(((double) System.currentTimeMillis() - ms) / 1000.0 + " 초 " + field.getName() + " 저장완료");
                } catch (ClassCastException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
//        logger.info(((double) System.currentTimeMillis() - def) / 1000.0 + " 초 전체 저장완료");
    }

    @SuppressWarnings("unchecked")
    private  <T extends SQLObject> T deserialize(String base64) {
        ByteArrayInputStream bipt = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        ObjectInputStream ipt = null;
        try {
            ipt = new ObjectInputStream(bipt);
            return (T) ipt.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if(ipt != null) ipt.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }



}