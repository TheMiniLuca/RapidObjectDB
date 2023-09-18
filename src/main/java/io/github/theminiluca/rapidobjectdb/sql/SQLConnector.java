package io.github.theminiluca.rapidobjectdb.sql;

import io.github.theminiluca.rapidobjectdb.objects.ObjectSerializer;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * @version 2.1.0
 * @since 2.0.0-SNAPSHOT
 * */
public abstract class SQLConnector {

    private Connection connection;
    private final Map<String, ObjectSerializer<?>> objectSerializer = new HashMap<>();

    private final String url;
    private final String db;
    private final int port;
    private final String user;
    private final String password;

    public SQLConnector(String url, String database, int port, String user, String password) {
        this.url = url;
        this.db = database;
        this.port = port;
        this.user = user;
        this.password = password;
        this.connection = openConnection(url, database, port, user, password);
    }

    public void insert(String table, String[] keyList, Object... values) throws RuntimeException {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(insertFormat(table, keyList, keyList.length));
            setPreparedValues(preparedStatement, values);
            preparedStatement.executeUpdate();
        }catch (SQLException | IOException e) {
            if(e instanceof SQLException e1 && isConnectionError(e1)) {
                connection = openConnection(url, db, port, user, password);
                insert(table, keyList, values);
                return;
            }
            throw new RuntimeException(e);
        }
    }

    public void insertOrUpdate(String table, String[] keyList, Object... values) throws RuntimeException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertOrUpdate(table, keyList, keyList.length))) {
            setPreparedValues(preparedStatement, values);
            setPreparedValues(preparedStatement, values.length, values);
            preparedStatement.executeUpdate();
        }catch (SQLException | IOException e) {
            e.printStackTrace();
            if(e instanceof SQLException e1 && isConnectionError(e1)) {
                connection = openConnection(url, db, port, user, password);
                insertOrUpdate(table, keyList, values);
                return;
            }
            throw new RuntimeException(e);
        }
    }

    public boolean contains(String table, String[] keyList, Object... values) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(containsFormat(table, keyList))) {
            setPreparedValues(preparedStatement, values);
            return preparedStatement.executeQuery().first();
        }catch (SQLException | IOException e) {
            if(e instanceof SQLException e1 && isConnectionError(e1)) {
                connection = openConnection(url, db, port, user, password);
                return contains(table, keyList, values);
            }
            throw new RuntimeException(e);
        }
    }


    public void update(String table, String[] keyList, Object... values) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateFormat(table, keyList, keyList.length))) {
            setPreparedValues(preparedStatement, values);
            preparedStatement.executeUpdate();
        }catch (SQLException | IOException e) {
            if(e instanceof SQLException e1 && isConnectionError(e1)) {
                connection = openConnection(url, db, port, user, password);
                update(table, keyList, values);
                return;
            }
            throw new RuntimeException(e);
        }
    }

    public void delete(String table, String key, Object value) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteFormat(table, key))) {
            setPreparedValues(preparedStatement, value);
            preparedStatement.execute();
        } catch (SQLException | IOException e) {
            if(e instanceof SQLException e1 && isConnectionError(e1)) {
                connection = openConnection(url, db, port, user, password);
                delete(table, key, value);
                return;
            }
            throw new RuntimeException(e);
        }
    }

    public ResultSet select(String table, String[] toSelect, String[] keyList, Object... value) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(selectFormat(table, toSelect, keyList));
            setPreparedValues(preparedStatement, value);

            return preparedStatement.executeQuery();
        } catch (SQLException | IOException e) {
            if(e instanceof SQLException e1 && isConnectionError(e1)) {
                connection = openConnection(url, db, port, user, password);
                return select(table, toSelect, keyList, value);
            }
            throw new RuntimeException(e);
        }
    }

    public ResultSet selectAll(String table) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(selectAllFormat(table));
            return preparedStatement.executeQuery();
        } catch (SQLException e) {
            if(isConnectionError(e)) {
                connection = openConnection(url, db, port, user, password);
                return selectAll(table);
            }
            throw new RuntimeException(e);
        }
    }

    public abstract void clearTable(String name) throws SQLException;

    public Connection getNative() {
        return connection;
    }

    public void setPreparedValues(PreparedStatement preparedStatement, Object... values) throws SQLException, IOException {
        setPreparedValues(preparedStatement, 0, values);
    }

    private void setPreparedValues(PreparedStatement preparedStatement, int offset, Object... values) throws SQLException, IOException {
        for (int i=1+offset; i<values.length+1+offset; i++) {
            if(objectSerializer.containsKey(values[i-1-offset].getClass().getName())) {
                preparedStatement.setString(i, "$RDB_OBJECT_SERIALIZED_DATA_"+values[i-1-offset].getClass().getName()+"::"+objectSerializer.get(values[i-1-offset].getClass().getName()).encodeN(values[i-1-offset]));
            }else if(values[i-1-offset] instanceof String s) {
                preparedStatement.setString(i, "$RDB_STRING_DATA_"+s);
            }else if(values[i-1-offset] instanceof Integer it) {
                preparedStatement.setInt(i, it);
            }else if(values[i-1-offset] instanceof Long l) {
                preparedStatement.setLong(i, l);
            }else if(values[i-1-offset] instanceof BigDecimal d) {
                preparedStatement.setBigDecimal(i, d);
            }else if(values[i-1-offset] instanceof Short s) {
                preparedStatement.setShort(i, s);
            }else if(values[i-1-offset] instanceof Float f) {
                preparedStatement.setFloat(i, f);
            }else if(values[i-1-offset] instanceof Double d) {
                preparedStatement.setDouble(i, d);
            }else if(values[i-1-offset] instanceof Boolean b){
                preparedStatement.setBoolean(i, b);
            }else if(values[i-1-offset] instanceof byte[] b) {
                preparedStatement.setBytes(i, b);
            }else if(values[i-1-offset] instanceof Byte b) {
                preparedStatement.setByte(i, b);
            }else if(values[i-1-offset] instanceof Array array) {
                preparedStatement.setArray(i, array);
            }else if(values[i-1-offset] instanceof Timestamp t) {
                preparedStatement.setTimestamp(i, t);
            }else if(values[i-1-offset] instanceof Time t) {
                preparedStatement.setTime(i, t);
            }else if(values[i-1-offset] instanceof Date d) {
                preparedStatement.setDate(i, d);
            }else if(values[i-1-offset] instanceof InputStream ipt) {
                preparedStatement.setBinaryStream(i, ipt);
            }else if(values[i-1-offset] instanceof Blob b) {
                preparedStatement.setBlob(i, b);
            }else if(values[i-1-offset] instanceof Reader r) {
                preparedStatement.setClob(i, r);
            }else if(values[i-1-offset] instanceof NClob c) {
                preparedStatement.setNClob(i, c);
            }else if(values[i-1-offset] instanceof SQLType t) {
                preparedStatement.setNull(i, t.getVendorTypeNumber());
            }else if(values[i-1-offset] instanceof RowId r) {
                preparedStatement.setRowId(i, r);
            }else if(values[i-1-offset] instanceof SQLXML s) {
                preparedStatement.setSQLXML(i, s);
            }else if(values[i-1-offset] instanceof Ref f){
                preparedStatement.setRef(i, f);
            }else {
                ByteArrayOutputStream bopt = new ByteArrayOutputStream();
                try (ObjectOutputStream oopt = new ObjectOutputStream(bopt)) {
                    oopt.writeObject(values[i-1-offset]);
                } catch (ObjectStreamException o) {
                    throw new IllegalArgumentException("Argument is not compatible with SQL", o);
                }catch (IOException e) {
                    throw new RuntimeException(e);
                }finally {
                    byte[] b = bopt.toByteArray();
                    bopt.close();
                    preparedStatement.setString(i, "$RDB_BASE64_ENCODED_DATA_"+Base64.getEncoder().encodeToString(b));
                }
            }
        }
    }

    public String getObjectType(Object o) {
        if(objectSerializer.containsKey(o.getClass().getName())) {
            return "LONGTEXT";
        }else if(o instanceof String) {
            return "TEXT";
        }else if(o instanceof Integer) {
            return "INTEGER";
        }else if(o instanceof Long) {
            return "BIGINT";
        }else if(o instanceof BigDecimal) {
            return "BIGINT";
        }else if(o instanceof Short) {
            return "SMALLINT";
        }else if(o instanceof Float) {
            return "FLOAT";
        }else if(o instanceof Double) {
            return "DOUBLE";
        }else if(o instanceof Boolean){
            return "BIT";
        }else if(o instanceof byte[]) {
            return "VARBINARY";
        }else if(o instanceof Byte) {
            return "TINYINT";
        }else if(o instanceof Array) {
            return "ARRAY";
        }else if(o instanceof Timestamp) {
            return "TIMESTAMP";
        }else if(o instanceof Time) {
            return "TIME";
        }else if(o instanceof Date) {
            return "DATE";
        }else if(o instanceof InputStream) {
            return "LONGVARBINARY";
        }else if(o instanceof Blob) {
            return "BLOB";
        }else if(o instanceof Reader) {
            return "LONGVARCHAR";
        }else if(o instanceof NClob) {
            return "LONGNVARCHAR";
        }else if(o instanceof RowId) {
            return "ROWID";
        }else if(o instanceof SQLXML) {
            return "XML";
        }else if(o instanceof Ref){
            return "REF";
        }else {
            return "LONGTEXT";
        }
    }

    public String getObjectTypeOfClass(Class<?> c) {
        if(objectSerializer.containsKey(c.getName())) {
            return "LONGTEXT";
        }else if(c.isAssignableFrom(String.class)) {
            return "TEXT";
        }else if(c.isAssignableFrom(Integer.class)) {
            return "INTEGER";
        }else if(c.isAssignableFrom(Long.class)) {
            return "BIGINT";
        }else if(c.isAssignableFrom(BigDecimal.class)) {
            return "BIGINT";
        }else if(c.isAssignableFrom(Short.class)) {
            return "SMALLINT";
        }else if(c.isAssignableFrom(Float.class)) {
            return "FLOAT";
        }else if(c.isAssignableFrom(Double.class)) {
            return "DOUBLE";
        }else if(c.isAssignableFrom(Boolean.class)){
            return "BIT";
        }else if(c.isAssignableFrom(byte[].class)) {
            return "VARBINARY";
        }else if(c.isAssignableFrom(Byte.class)) {
            return "TINYINT";
        }else if(c.isAssignableFrom(Array.class)) {
            return "ARRAY";
        }else if(c.isAssignableFrom(Timestamp.class)) {
            return "TIMESTAMP";
        }else if(c.isAssignableFrom(Time.class)) {
            return "TIME";
        }else if(c.isAssignableFrom(Date.class)) {
            return "DATE";
        }else if(c.isAssignableFrom(InputStream.class)) {
            return "LONGVARBINARY";
        }else if(c.isAssignableFrom(Blob.class)) {
            return "BLOB";
        }else if(c.isAssignableFrom(Reader.class)) {
            return "LONGVARCHAR";
        }else if(c.isAssignableFrom(NClob.class)) {
            return "LONGNVARCHAR";
        }else if(c.isAssignableFrom(RowId.class)) {
            return "ROWID";
        }else if(c.isAssignableFrom(SQLXML.class)) {
            return "XML";
        }else if(c.isAssignableFrom(Ref.class)){
            return "REF";
        }else {
            return "LONGTEXT";
        }
    }

    public Object getObject(Object o) {
        if(objectSerializer.containsKey(o.getClass().getName())) {
            return objectSerializer.get(o.getClass().getName()).decode(((String) o).substring(28));
        }else if(o instanceof String s) {
            if(s.startsWith("$RDB_STRING_DATA_")) return s.substring(17);
            else if(s.startsWith("$RDB_BASE64_ENCODED_DATA_")) {
                try (ByteArrayInputStream ipt = new ByteArrayInputStream(Base64.getDecoder().decode(s.substring(25)));
                    ObjectInputStream oipt = new ObjectInputStream(ipt);
                ) {
                    return oipt.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }else if(s.startsWith("$RDB_OBJECT_SERIALIZED_DATA_")) {
                String ss = s.substring(28).split("::")[0];
                System.out.println(ss);
                if(objectSerializer.containsKey(ss)) {
                    return objectSerializer.get(ss).decode(s.substring(30+ss.length()));
                } else throw new RuntimeException(new NotSerializableException("Can't find serializable type!"));
            } else return s;
        }else return o;
    }

    public <T> void registerObjectSerializer(Class<? extends T> t, ObjectSerializer<T> objectSerializer) {
        this.objectSerializer.put(t.getName(), objectSerializer);
    }

    public <T> void removeObjectSerializer(T t) {
        this.objectSerializer.remove(t.getClass().getName());
    }

    public void clearObjectSerializer() {
        this.objectSerializer.clear();
    }

    public void close() throws SQLException {
        connection.close();
    }

    public abstract String insertFormat(String table, String[] keyList, int size);
    public abstract String insertOrUpdate(String table, String[] keyList, int size);
    public abstract String updateFormat(String table, String[] keyList, int size);
    protected abstract String containsFormat(String table, String[] keyList);
    public abstract String deleteFormat(String table, String key);
    public abstract String selectFormat(String table, String[] toSelect, String[] keyList);
    public abstract String selectAllFormat(String table);
    public abstract boolean isConnectionError(SQLException e);

    protected abstract Connection openConnection(String url, String database, int port, String user, String password);
    public static String questionMarkGenerator(int size) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<size; i++) {
            sb.append("?").append(",");
        }
        return sb.deleteCharAt(sb.length()-1).toString();
    }
}
