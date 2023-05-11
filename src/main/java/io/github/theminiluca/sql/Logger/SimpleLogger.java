package io.github.theminiluca.sql.Logger;

import java.time.LocalTime;

public class SimpleLogger {
    public static SimpleLogger INSTANCE = new SimpleLogger("SQLiteManager");
    private final String name;
    private Level loggingLevel;

    public SimpleLogger(String name) {
        this(name, Level.INFO);
    }

    public SimpleLogger(String name, int loggingLevel) {
        this(name, Level.getLevelByID(loggingLevel));
    }

    public SimpleLogger(String name, Level loggingLevel) {
        this.name = name;
        this.loggingLevel = loggingLevel;
    }

    public void setLoggingLevel(int loggingLevel) {
        setLoggingLevel(Level.getLevelByID(loggingLevel));
    }

    public void setLoggingLevel(Level loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public void log(Level level, String message) {
        if(level.getLevel() >= loggingLevel.getLevel()) System.out.printf("%s[ %s | %s | %s ] %s\033[0m%n", level.getColor(), level.getName(), name, LocalTime.now(), message);
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
