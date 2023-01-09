package io.github.theminiluca.sql.main;

import io.github.theminiluca.sql.Connections;
import io.github.theminiluca.sql.SQLManager;

import java.util.*;

public class Main {

    public static final HashMap<String, A> hash = new HashMap<>();

    public static void main(String[] args) {
        SQLManager.DEBUGGING = true;
        Connections connections = SQLManager.driver("a", "db.sqlite3");
        SQLManager.setConnection(B.class, connections);
        SQLManager.sqlite(A.class, connections, hash);
        A a = new A("%x".formatted(new Random().nextInt(100000)));
        HashMap<String, HashMap<String, Integer>> hash = new HashMap<>();
        hash.put("123", new HashMap<>());
        hash.put("1234", new HashMap<>());
        hash.get("1234").put("12345", 1);
        a.lists.add(new B("%x".formatted(new Random().nextInt(100000))
                , new ArrayList<>(Arrays.asList(System.currentTimeMillis()
                , (long) new Random().nextInt(100000))), hash));
        a.saveSQL();
        System.out.println("123 : " + Main.hash);
    }
}
