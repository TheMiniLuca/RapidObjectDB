package io.github.theminiluca.rapidobjectdb.objects;

/**
 * <h2>Object Serializer</h2><br/>
 * As you can know with the name of the class, this class serializes Object to String and de-serializes String to Object.
 * @since 2.0.0-SNAPSHOT
 * */
public abstract class ObjectSerializer<T> {
    public String encodeN(Object o) {
        return encode((T)o);
    }
    /**
     * Encode to String
     * */
    public abstract String encode(T t);
    /**
     * Decode to Object
     * */
    public abstract T decode(String s);
}
