package io.github.theminiluca.rapidobjectdb;

import io.github.theminiluca.rapidobjectdb.annotation.SQL;
import io.github.theminiluca.rapidobjectdb.objects.DatabaseType;
import io.github.theminiluca.rapidobjectdb.objects.ObjectSerializer;
import io.github.theminiluca.rapidobjectdb.objects.FieldSyncer;
import io.github.theminiluca.rapidobjectdb.objects.fieldsyncers.MapFieldSyncer;
import io.github.theminiluca.rapidobjectdb.sql.MariaDBConnector;
import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;
import io.github.theminiluca.rapidobjectdb.utils.SyncThreadFactory;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class RapidSyncManager {

    private final SQLConnector connector;
    private final ScheduledExecutorService service;
    private final Map<Object, ScheduledFuture<?>> backupTasks = new HashMap<>();
    private final Map<Class<?>, FieldSyncer> fieldSyncers = new HashMap<>();

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
        } else {
            throw new UnsupportedOperationException("This feature is not added.");
        }
        this.service = new ScheduledThreadPoolExecutor(corePoolSize, new SyncThreadFactory(url, port, database));
        fieldSyncers.put(Map.class, new MapFieldSyncer());
    }

    public void registerBackup(Object o, long time, TimeUnit unit) {
        startupLoader(o);
        backupTasks.put(o, service.scheduleWithFixedDelay(() -> uploadData(o), time, time, unit));
    }

    private void uploadData(Object o) {
        for(Field f : o.getClass().getFields()) {
            if(f.isAnnotationPresent(SQL.class)) {
                try {
                    SQL annotation = f.getAnnotation(SQL.class);
                    f.setAccessible(true);
                    if(fieldSyncers.containsKey(f.getType())) {
                        fieldSyncers.get(f.getType()).saveField(annotation, f.get(o), connector);
                    }else if(f.getType().isAssignableFrom(Map.class)) {
                        fieldSyncers.get(Map.class).saveField(annotation, f.get(o), connector);
                    }else {
                        throw new UnsupportedOperationException("Can't find FieldSyncer that can store %s type of field!".formatted(f.getType().getName()));
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startupLoader(Object o) {
        for(Field f : o.getClass().getFields()) {
            if(f.isAnnotationPresent(SQL.class)) {
                try {
                    SQL annotation = f.getAnnotation(SQL.class);
                    f.setAccessible(true);
                    if (fieldSyncers.containsKey(f.getType())) {
                        f.set(o, fieldSyncers.get(f.getType()).loadField(annotation, connector));
                    } else if (f.getType().isAssignableFrom(Map.class)) {
                        f.set(o, fieldSyncers.get(Map.class).loadField(annotation, connector));
                    } else {
                        throw new UnsupportedOperationException("Can't find FieldSyncer that can load %s type of field!".formatted(f.getType().getName()));
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

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

    public <T> void registerObjectSerializer(T t, ObjectSerializer<T> objectSerializer) {
        connector.registerObjectSerializer(t, objectSerializer);
    }

    public <T> void removeObjectSerializer(T t) {
        connector.removeObjectSerializer(t);
    }

    public void clearObjectSerializer() {
        connector.clearObjectSerializer();
    }

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
