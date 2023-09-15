package io.github.theminiluca.rapidobjectdb.map;

import java.util.Map;

public interface SQLSavableMap<K,V> extends Map<K,V> {
    void saveMap();
}
