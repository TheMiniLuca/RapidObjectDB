package io.github.theminiluca.rapidobjectdb.objects.table;

public interface TableStructure {
    String getName();
    void addColumn(TableColumn col);
    void updateColumn(TableColumn col);
    default void removeColumn(TableColumn col) {
        removeColumn(col.getName());
    }
    void removeColumn(String name);
}