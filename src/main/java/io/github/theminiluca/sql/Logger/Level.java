package io.github.theminiluca.sql.Logger;

public enum Level {
    DEBUG(0, "Debug", ""),
    INFO(1, "Info", "\033[0;34m"),
    WARNING(2, "Warning", "\033[43m"),
    ERROR(3, "Error", "\033[0;31m"),
    FATAL(4, "Fatal", "\033[41m");


    private final int level;
    private final String name;
    private final String color;


    Level(int level, String name, String color) {
        this.level = level;
        this.name = name;
        this.color = color;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public static Level getLevelByID(int level) {
        switch (level) {
            case 0 -> {
                return Level.DEBUG;
            }
            case 1 -> {
                return Level.INFO;
            }
            case 2 -> {
                return Level.WARNING;
            }
            case 3 -> {
                return Level.ERROR;
            }
            case 4 -> {
                return Level.FATAL;
            }
            default -> {
                return null;
            }
        }
    }
}
