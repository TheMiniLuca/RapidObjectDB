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
        SQLManager.DEBUGGING = false;
        SQLManager.sqlite(User.class, connections, users);

        System.out.println(users.size());
        if (users.size() <= 100)
            for (int i = 0; i < 100; i++) {
                User user = new User(UUID.randomUUID(), "%x".formatted(i));
                users.put(user.getUniqueId().toString(), user);
            }
        long ms1 = System.currentTimeMillis();
        System.out.println("저장 시작");
        for (User loopUser : Main.users.values()) {
            System.out.println(loopUser.getHistory());
            loopUser.getHistory().add(System.currentTimeMillis());
            loopUser.setEmail("aa@gmail.com");
            System.out.println(loopUser.getHash());
            loopUser.getHash().put(UUID.randomUUID().toString(), "123");
            SQLManager.save(loopUser);
        }
        System.out.println(System.currentTimeMillis() - ms1 + " ms");
    }
}
