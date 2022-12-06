package io.github.theminiluca.sql;

import java.sql.Connection;

public class Connections {

    public final String name;
    public final Connection connection;

    public Connections(String name, Connection connection) {
        this.name = name;
        this.connection = connection;
    }

    public String getName() {
        return name;
    }

    public Connection getConnection() {
        return connection;
    }
}
