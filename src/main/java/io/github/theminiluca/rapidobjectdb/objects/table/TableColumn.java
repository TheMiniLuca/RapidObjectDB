package io.github.theminiluca.rapidobjectdb.objects.table;

public interface TableColumn {
    void setName(String name);
    void setDataType(String dataType);
    void setLength(int length);
    String getName();
    String getDataType();
    int length();
}
