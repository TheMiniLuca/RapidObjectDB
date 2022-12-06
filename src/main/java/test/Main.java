package test;

import io.github.theminiluca.sql.Connections;
import io.github.theminiluca.sql.SQLManager;

import java.util.HashMap;
import java.util.UUID;

public class Main {
    public static final HashMap<String, User> users = new HashMap<>();

    public static User getUser(UUID uniqueId) {
        return users.getOrDefault(uniqueId.toString(), null);
    }

    public static void main(String[] args) {
        Connections connections = SQLManager.driver("users", "userdata.db");
        SQLManager.createTable(User.class, connections);
        SQLManager.migration(User.class, connections);
        SQLManager.load(User.class, users);
        System.out.println(users.size());
        long ms = System.currentTimeMillis();
//        for (int i = 0; i < 100; i++) {
//            User user = new User(UUID.randomUUID(), "%x".formatted(i));
//            users.put(user.getUniqueId().toString(), user);
//        }
        System.out.println(System.currentTimeMillis() - ms + " ms");
        int i = 0;
        for (User loopUser : Main.users.values()) {
            loopUser.setAnInt(loopUser.getAnInt()+1);
            loopUser.setEmail("%x@%x".formatted(i, i * i));
            i++;
        }
        long ms1 = System.currentTimeMillis();
        System.out.println("저장 시작");
        for (User loopUser : Main.users.values()) {
            SQLManager.save(loopUser);
        }
        System.out.println(System.currentTimeMillis() - ms1 + " ms");
    }
}
