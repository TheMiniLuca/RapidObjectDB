package io.github.theminiluca.sql.main;

import io.github.theminiluca.sql.SQL;
import io.github.theminiluca.sql.SQLConstructor;
import io.github.theminiluca.sql.SQLManager;
import io.github.theminiluca.sql.SQLObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        final StringBuilder sb = new StringBuilder("B{");
        sb.append("uid='").append(uid).append('\'');
        sb.append(", longs=").append(longs);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void saveSQL() {
        SQLManager.save(this);
    }
}


