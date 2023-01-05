package io.github.theminiluca.sql.main;

import io.github.theminiluca.sql.Connections;
import io.github.theminiluca.sql.SQLManager;

import java.util.HashMap;

public class Main {

    public static final HashMap<String, A> hash = new HashMap<>();

    public static void main(String[] args) {
        SQLManager.DEBUGGING = true;
        Connections connections = SQLManager.driver("a", "db.sqlite3");
        SQLManager.setConnection(B.class, connections);
        SQLManager.sqlite(A.class, connections, hash);
        A a = new A("%x".formatted(System.currentTimeMillis()%1000));
        a.lists.add(new B("%x".formatted(System.currentTimeMillis()%10000)));
        a.saveSQL();
    }
}
