package io.github.theminiluca.sql.main;

import io.github.theminiluca.sql.SQL;
import io.github.theminiluca.sql.SQLConstructor;
import io.github.theminiluca.sql.SQLManager;
import io.github.theminiluca.sql.SQLObject;

import java.util.HashMap;

public class B implements SQLObject {
    @SQL(primary = true)
    public final String uid;


    @SQLConstructor
    public B(String id) {
        this.uid = id;
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


