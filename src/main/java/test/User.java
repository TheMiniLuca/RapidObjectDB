package test;

import io.github.theminiluca.sql.SQLObject;

import java.util.LinkedHashMap;
import java.util.UUID;

public class User implements SQLObject {

    private final UUID uniqueId;
    private final String name;

    private int anInt = 0;

    private String email = null;

    public User(UUID uniqueId, String name) {
        this.uniqueId = uniqueId;
        this.name = name;
    }

    User(UUID uniqueId, String name, int anInt, String email) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.anInt = anInt;
        this.email = email;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }

    public int getAnInt() {
        return anInt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAnInt(int anInt) {
        this.anInt = anInt;
    }

    public static LinkedHashMap<String, Class<?>> createTable() {
        LinkedHashMap<String, Class<?>> hash = new LinkedHashMap<>();
        hash.put("uniqueId", UUID.class);
        hash.put("name", String.class);
        hash.put("int", int.class);
        //hash.put("email", String.class);
        return hash;
    }

    @Override
    public LinkedHashMap<String, Object> serialize() {
        LinkedHashMap<String, Object> hash = new LinkedHashMap<>();
        hash.put("uniqueId", uniqueId);
        hash.put("name", name);
        hash.put("int", anInt);
        //hash.put("email", email);
        return hash;
    }

    public static User deserialize(LinkedHashMap<String, Object> hash) {
        try {
            return new User((UUID) hash.get("uniqueId"), (String) hash.get("name"), (int) hash.get("int"), (String) hash.get("email"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String primaryKey() {
        return "uniqueId";
    }
}
