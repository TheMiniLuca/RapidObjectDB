package io.github.theminiluca.sql.main;

import io.github.theminiluca.sql.SQL;
import io.github.theminiluca.sql.SQLConstructor;
import io.github.theminiluca.sql.SQLManager;
import io.github.theminiluca.sql.SQLObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class A implements SQLObject {
    @SQL(primary = true)
    public final String id;
    @SQL
    public final int anInt;

    @SQL
    public List<B> lists;

    @SQLConstructor
    public A(String id, Integer anInt, ArrayList<B> lists) {
        this.id = id;
        this.anInt = anInt;
        this.lists = lists;
    }

    public A(String id) {
        this.id = id;
        this.anInt = 0;
        this.lists = new ArrayList<>();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("A{");
        sb.append("id='").append(id).append('\'');
        sb.append(", lists=").append(lists);
        sb.append('}');
        return sb.toString();
    }

    public static HashMap<String, Object> migration() {
        HashMap<String, Object> hash = new HashMap<>();
        hash.put("anInt", 0);
        return hash;
    }

    @Override
    public void saveSQL() {
        SQLManager.save(this);
    }
}
