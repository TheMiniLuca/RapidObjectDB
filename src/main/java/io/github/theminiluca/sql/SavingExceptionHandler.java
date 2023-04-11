package io.github.theminiluca.sql;

public abstract class SavingExceptionHandler {
    public abstract String onSerialize(Object o);
    public abstract Object onDeserialize(String s);
}
