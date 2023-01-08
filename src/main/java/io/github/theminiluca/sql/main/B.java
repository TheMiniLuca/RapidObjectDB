package io.github.theminiluca.sql.main;

import io.github.theminiluca.sql.SQL;
import io.github.theminiluca.sql.SQLConstructor;
import io.github.theminiluca.sql.SQLManager;
import io.github.theminiluca.sql.SQLObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class B implements SQLObject {
    @SQL(primary = true)
    public final String uid;
    @SQL
    public final List<Long> longs;


    @SQLConstructor
    public B(String id, ArrayList<Long> lists) {
        this.uid = id;
        this.longs = lists;
    }


    @Override
    public String toString() {
        return "B{" + "id='" + uid + '\'' +
                '}';
    }

    @Override
    public void saveSQL() {
        SQLManager.save(this);
    }
}


