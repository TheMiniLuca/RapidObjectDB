package io.github.theminiluca.rapidobjectdb;

import io.github.theminiluca.rapidobjectdb.annotation.SQL;
import io.github.theminiluca.rapidobjectdb.map.SQLSavableMap;
import io.github.theminiluca.rapidobjectdb.objects.DatabaseType;
import io.github.theminiluca.rapidobjectdb.objects.ObjectSerializer;
import io.github.theminiluca.rapidobjectdb.objects.FieldSyncer;
import io.github.theminiluca.rapidobjectdb.objects.fieldsyncers.ListFieldSyncer;
import io.github.theminiluca.rapidobjectdb.objects.fieldsyncers.MapFieldSyncer;
import io.github.theminiluca.rapidobjectdb.sql.MariaDBConnector;
import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;
import io.github.theminiluca.rapidobjectdb.sql.SQLiteConnector;
import io.github.theminiluca.rapidobjectdb.utils.SyncThreadFactory;

import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <h1>RapidSyncManager</h1>
 * This is a Rapid-Sync-Manager.<br/>
 * <br/>
 * This class helps you to save your map!
 * @version 2.1.0-SNAPSHOT
 * @since 2.0.0-SNAPSHOT
 * */
public class RapidSyncManager {

    private final SQLConnector connector;
    private final ScheduledExecutorService service;
    private final Map<Object, ScheduledFuture<?>> backupTasks = new HashMap<>();
    private final Map<Class<?>, FieldSyncer> fieldSyncers = new HashMap<>();

    public RapidSyncManager(File f) {
        this(f, 4);
    }

    public RapidSyncManager(File f, int corePoolSize) {
        this(f.getAbsolutePath(), -1, null, null, null, DatabaseType.SQLite, corePoolSize);
    }

    public RapidSyncManager(String url, int port, String database, String user, String password) {
        this(url, port, database, user, password, DatabaseType.MariaDB);
    }

    public RapidSyncManager(String url, int port, String database, String user, String password, int corePoolSize) {
        this(url, port, database, user, password, DatabaseType.MariaDB, corePoolSize);
    }

    public RapidSyncManager(String url, int port, String database, String user, String password, DatabaseType type) {
        this(url, port, database, user, password, type, 4);
    }

    public RapidSyncManager(String url, int port, String database, String user, String password, DatabaseType type, int corePoolSize) {
        if(type == DatabaseType.MariaDB) {
            this.connector = new MariaDBConnector(url, database, port, user, password);
        } else if(type == DatabaseType.SQLite) {
            this.connector = new SQLiteConnector(new File(url));
        }else {
            throw new UnsupportedOperationException("This feature is not added.");
        }
        this.service = new ScheduledThreadPoolExecutor(corePoolSize, new SyncThreadFactory(url, port, database));
        fieldSyncers.put(Map.class, new MapFieldSyncer());
        fieldSyncers.put(List.class, new ListFieldSyncer());
    }

    public SQLConnector getConnector() {
        return connector;
    }

    /**
     * <h2>Register Backup Task</h2>
     * @param dataClass Class that holds Map(s).
     * @param time Delay
     * @param unit TimeUnit
     * */
    public void registerBackup(Object dataClass, long time, TimeUnit unit) {
        startupLoader(dataClass);
        backupTasks.put(dataClass, service.scheduleWithFixedDelay(() -> uploadData(dataClass), time, time, unit));
    }

    private void uploadData(Object o) {
        for(Field f : o.getClass().getDeclaredFields()) {
            if(f.isAnnotationPresent(SQL.class)) {
                try {
                    SQL annotation = f.getAnnotation(SQL.class);
                    f.setAccessible(true);
                    if(f.get(o) instanceof SQLSavableMap<?,?> m){
                        try {
                            m.saveMap(connector, annotation.value());
                        }catch (RuntimeException e){
                            if(e.getCause() instanceof SQLException e1) {
                                String message = e1.getMessage().toLowerCase();
                                if(message.contains("no such table") || message.contains("doesn't exist")) {
                                    m.createTable(connector, annotation.value());
                                    m.saveMap(connector, annotation.value());
                                }
                            }
                        }
                    }else if(fieldSyncers.containsKey(f.getType())) {
                        saveField(f.getType(), annotation, f, o);
                    }else {
                        for(Class<?> c : fieldSyncers.keySet()) {
                            if(c.isAssignableFrom(f.getType())) {
                                saveField(c, annotation, f, o);
                                return;
                            }
                        }
                        throw new UnsupportedOperationException("Can't find FieldSyncer that can store %s type of field!".formatted(f.getType().getName()));
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveField(Class<?> c, SQL annotation, Field f, Object o) throws SQLException, IllegalAccessException {
        try {
            fieldSyncers.get(c).saveField(annotation, f.get(o), connector);
        }catch (RuntimeException | SQLException e) {
            if(e.getCause()!=null&&e.getCause() instanceof NotSerializableException) throw e;
            else if(e instanceof SQLException e1) {
                String message = e1.getMessage().toLowerCase();
                if((message.contains("no such table") || message.contains("doesn't exist")) && fieldSyncers.get(c).createTable(annotation.value(), f.get(o), connector)) fieldSyncers.get(f.getType()).saveField(annotation, f.get(o), connector);
                else throw e1;
            }else throw e;
        }
    }

    private void startupLoader(Object o) {
        for(Field f : o.getClass().getDeclaredFields()) {
            if(f.isAnnotationPresent(SQL.class)) {
                try {
                    SQL annotation = f.getAnnotation(SQL.class);
                    f.setAccessible(true);
                    if(f.get(o) instanceof SQLSavableMap<?,?> m) {
                        m.createTable(connector, annotation.value());
                        m.initialize(connector, annotation.value());
                    }else {
                        Object of = (fieldSyncers.containsKey(f.getType()) ? fieldSyncers.get(f.getType()).loadField(annotation, connector) : (
                                Map.class.isAssignableFrom(f.getType()) ? fieldSyncers.get(Map.class).loadField(annotation, connector)
                                        : f
                        ));
                        if (f.equals(of)) {
                            throw new UnsupportedOperationException("Can't find FieldSyncer that can load %s type of field!".formatted(f.getType().getName()));
                        } else if (of != null) {
                            f.set(o, of);
                        }
                    }
                } catch (IllegalAccessException | SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * <h2>Cancel Backup Task</h2>
     * @param o Class that holds Map(s).
     * */
    public void removeBackup(Object o) {
        backupTasks.get(o).cancel(false);
        backupTasks.remove(o);
    }

    public void registerFieldSyncer(Class<?> clazz, FieldSyncer fieldSyncer) {
        fieldSyncers.put(clazz, fieldSyncer);
    }

    public void removeFieldSyncer(Class<?> clazz) {
        fieldSyncers.remove(clazz);
    }

    public <T> void registerObjectSerializer(Class<? extends T> t, ObjectSerializer<T> objectSerializer) {
        connector.registerObjectSerializer(t, objectSerializer);
    }

    public <T> void removeObjectSerializer(T t) {
        connector.removeObjectSerializer(t);
    }

    public void clearObjectSerializer() {
        connector.clearObjectSerializer();
    }

    /**
     * <h1>Close</h1>
     * This method can save data and close safely!
     * */
    public void close() {
        service.shutdownNow();
        for(Object o : backupTasks.keySet()) {
            uploadData(o);
        }
        fieldSyncers.clear();
        backupTasks.clear();
        try {
            connector.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
