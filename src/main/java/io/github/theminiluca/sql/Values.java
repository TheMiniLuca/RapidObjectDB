package io.github.theminiluca.sql;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class Values<T extends SQLObject> {

    private final int size;
    private final T[] array;
    protected List<Integer> updatedIndex = new ArrayList<>();

    public Values(int size) {
        this(size, (T[]) new SQLObject[size]);
    }

    public Values(T... values) {
        this(values.length, values);
    }

    public Values(int size, T... values) {
        this.size = size;
        this.array = values;
    }

    public void replace(int index, T value) {
        if(!updatedIndex.contains(index)) updatedIndex.add(index);
        array[index] = value;
    }

    public T get(int index) {
        return array[index];
    }

    public int getLength() {
        return size;
    }
}
