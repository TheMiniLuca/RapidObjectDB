package test;

import io.github.theminiluca.sql.SQL;
import io.github.theminiluca.sql.SQLObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class User implements SQLObject {

    @SQL(primary = true)
    private final UUID uniqueId;
    @SQL
    private final String name;

    @SQL
    private final List<Long> joinLists = new ArrayList<>();
    @SQL
    private int anInt = 0;
    @SQL
    private String email = null;
    @SQL
    private long firstJoin = 0;

    public User(UUID uniqueId, String name) {
        this.uniqueId = uniqueId;
        this.name = name;
    }

   public User(UUID uniqueId, String name, int anInt, String email, long firstJoin) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.anInt = anInt;
        this.email = email;
        this.firstJoin = firstJoin;
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

//    public static LinkedHashMap<String, Class<?>> createTable() {
//        LinkedHashMap<String, Class<?>> hash = new LinkedHashMap<>();
//        hash.put("uniqueId", UUID.class);
//        hash.put("name", String.class);
//        hash.put("int", int.class);
//        hash.put("firstJoin", long.class);
//        return hash;
//    }

    public long getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(long firstJoin) {
        this.firstJoin = firstJoin;
    }

    public static String primaryKey() {
        return "uniqueId";
    }
}
