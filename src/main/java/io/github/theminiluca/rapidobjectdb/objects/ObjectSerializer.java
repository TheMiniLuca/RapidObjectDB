package io.github.theminiluca.rapidobjectdb.objects;

public abstract class ObjectSerializer<T> {
    public String encodeN(Object o) {
        return encode((T)o);
    }
    public abstract String encode(T t);
    public abstract T decode(String s);
}
