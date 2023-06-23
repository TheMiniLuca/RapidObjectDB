package io.github.theminiluca.rapidobjectdb.objects;

import io.github.theminiluca.rapidobjectdb.annotation.SQL;
import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;

public interface FieldSyncer {
    Object loadField(SQL sql, SQLConnector connector);
    void saveField(SQL sql, Object field, SQLConnector connector);
}
