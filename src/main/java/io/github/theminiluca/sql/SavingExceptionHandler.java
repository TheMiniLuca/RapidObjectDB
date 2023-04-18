package io.github.theminiluca.sql;

public abstract class SavingExceptionHandler {
    public abstract String serialize(Object o);
    public abstract Object deserialize(String s);
}
