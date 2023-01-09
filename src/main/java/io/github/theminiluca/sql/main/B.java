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

    @SQL
    public final HashMap<String, HashMap<String, Integer>> longHash;



    @SQLConstructor
    public B(String id, ArrayList<Long> lists, HashMap<String, HashMap<String, Integer>> longHash) {
        this.uid = id;
        this.longs = lists;
        this.longHash = longHash;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("B{");
        sb.append("uid='").append(uid).append('\'');
        sb.append(", longs=").append(longs);
        sb.append(", loadHash=").append(longHash);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void saveSQL() {
        SQLManager.save(this);
    }
}


