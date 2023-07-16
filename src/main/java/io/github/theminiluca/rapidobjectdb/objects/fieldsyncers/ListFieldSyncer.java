package io.github.theminiluca.rapidobjectdb.objects.fieldsyncers;

import io.github.theminiluca.rapidobjectdb.annotation.SQL;
import io.github.theminiluca.rapidobjectdb.objects.FieldSyncer;
import io.github.theminiluca.rapidobjectdb.sql.SQLConnector;
import org.sqlite.SQLiteException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.util.ArrayList;
import java.util.List;

import static io.github.theminiluca.rapidobjectdb.utils.SQLUtils.createTable;

/**
 * <strong>List Field Syncer</strong><br/><br/>
 * List Field Syncer is a one of Field Syncer that is pre-built in this library.
 * This Field Syncer saves List data to sql.
 * @version 2.0.6
 * @since 2.0.5-SNAPSHOT
 * */
public class ListFieldSyncer implements FieldSyncer {
    private static final String[] key_value = new String[]{"key","value"};
    @Override
    public Object loadField(SQL sql, SQLConnector connector) {
        List<Object> rv = new ArrayList<>();
        try (ResultSet set = connector.getNative().prepareStatement("SELECT `value` FROM `%s` ORDER BY `key` ASC;".formatted(sql.value())).executeQuery()) {
            while (set.next()) {
                rv.add(connector.getObject(set.getObject(1)));
            }
            return rv;
        }catch (Exception e1) {
            Throwable e = (e1.getCause() != null ? e1.getCause() : e1);
            if(
                    (e instanceof SQLSyntaxErrorException && e.getMessage().toLowerCase().contains("doesn't exist"))
                            || (e instanceof SQLiteException && e.getMessage().toLowerCase().contains("no such table"))
            ) return null;
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveField(SQL sql, Object field, SQLConnector connector) {
        List<?> list = (List<?>) field;
        try {
            connector.clearTable(sql.value());
            for (int i=0; i<list.size(); i++) {
                connector.insert(sql.value(), key_value, i, list.get(i));
            }
        }catch (RuntimeException e) {
            try {
                if(list.size() > 0) {
                    createTable(connector, sql.value(), "INTEGER", connector.getObjectType(list.stream().findAny().get()));
                    saveField(sql, field, connector);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
