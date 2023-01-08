package io.github.theminiluca.sql.main;

import io.github.theminiluca.sql.Connections;
import io.github.theminiluca.sql.SQLManager;

import java.util.*;

public class Main {

    public static final HashMap<String, A> hash = new HashMap<>();

    public static void main(String[] args) {
        SQLManager.DEBUGGING = false;
        Connections connections = SQLManager.driver("a", "db.sqlite3");
        SQLManager.setConnection(B.class, connections);
        SQLManager.sqlite(A.class, connections, hash);
        A a = new A("%x".formatted(new Random().nextInt(100000)));
        HashMap<String, Long> hash = new HashMap<>();
        hash.put("%x".formatted(new Random().nextInt(100000)), (long) new Random().nextInt(100000));
        a.lists.add(new B("%x".formatted(new Random().nextInt(100000))
                , new ArrayList<>(Arrays.asList(System.currentTimeMillis()
                , (long) new Random().nextInt(100000)))));
        a.saveSQL();
    }
}
