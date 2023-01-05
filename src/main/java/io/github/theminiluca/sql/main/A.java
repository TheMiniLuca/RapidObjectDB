package io.github.theminiluca.sql.main;

import io.github.theminiluca.sql.SQL;
import io.github.theminiluca.sql.SQLConstructor;
import io.github.theminiluca.sql.SQLManager;
import io.github.theminiluca.sql.SQLObject;

import java.util.ArrayList;
import java.util.List;

public class A implements SQLObject {
    @SQL(primary = true)
    public final String id;
    @SQL
    public List<B> lists;

    @SQLConstructor
    public A(String id, ArrayList<B> lists) {
        this.id = id;
        this.lists = lists;
    }

    public A(String id) {
        this.id = id;
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

    @Override
    public void saveSQL() {
        SQLManager.save(this);
    }
}
