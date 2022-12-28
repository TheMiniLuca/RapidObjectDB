package test;

import io.github.theminiluca.sql.SQL;
import io.github.theminiluca.sql.SQLManager;
import io.github.theminiluca.sql.SQLObject;

import java.util.*;

public class User implements SQLObject {

    @SQL(primary = true)
    private final UUID uniqueId;
    @SQL
    private final String name;
    @SQL
    private int anInt = 0;
    @SQL
    private String email = null;
    @SQL
    private long firstJoin = 0;

    @SQL
    private List<Long> history = new ArrayList<>();
    @SQL
    private Map<String, String> hash = new HashMap<>();

    public User(UUID uniqueId, String name) {
        this.uniqueId = uniqueId;
        this.name = name;
    }

    public User(UUID uniqueId, String name, Integer anInt, String email, Long firstJoin, ArrayList<Long> history, HashMap<String, String> hash) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.anInt = anInt;
        this.email = email;
        this.firstJoin = firstJoin;
        this.history = history == null ? new ArrayList<>() : history;
        this.hash = hash == null ? new HashMap<>() : hash;
    }

    public Map<String, String> getHash() {
        return hash;
    }

    public void setHash(Map<String, String> hash) {
        this.hash = hash;
    }

    public List<Long> getHistory() {
        return history;
    }

    public void setHistory(List<Long> history) {
        this.history = history;
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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("User{");
        sb.append("uniqueId=").append(uniqueId);
        sb.append(", name='").append(name).append('\'');
        sb.append(", anInt=").append(anInt);
        sb.append(", email='").append(email).append('\'');
        sb.append(", firstJoin=").append(firstJoin);
        sb.append(", history=").append(history);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public void saveSQL() {
        SQLManager.save(this);
    }
}
