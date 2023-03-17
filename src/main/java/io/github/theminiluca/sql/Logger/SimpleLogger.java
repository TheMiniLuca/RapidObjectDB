package io.github.theminiluca.sql.Logger;

import java.time.LocalTime;

public class SimpleLogger {
    public static SimpleLogger INSTANCE = new SimpleLogger("SQLiteManager");
    private final String name;

    public SimpleLogger(String name) {
        this.name = name;
    }

    public void log(Level level, String message) {
        System.out.printf("%s[ %s | %s | %s ] %s\033[0m%n", level.getColor(), level.getName(), name, LocalTime.now(), message);
    }

    public void log(int level, String message) {
        log(Level.getLevelByID(level), message);
    }

    public void logf(Level level, String message, Object... args) {
        log(level, message.formatted(args));
    }

    public void logf(int level, String message, Object... args) {
        logf(Level.getLevelByID(level), message, args);
    }
}
