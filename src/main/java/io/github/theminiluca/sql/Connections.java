package io.github.theminiluca.sql;

import java.sql.Connection;
import java.sql.SQLException;

public class Connections {

    public String name;
    public final Connection connection;

    public Connections(String name, Connection connection) {
        this.name = name;
        this.connection = connection;
    }

    public Connections name(String name) {
        this.name = name;
        return new Connections(name, connection);
    }

    public String name() {
        return name;
    }

    public Connection connection() {
        return connection;
    }

    public String url() {
        try {
            return this.connection.getMetaData().getURL().replace("jdbc:sqlite:", "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Connections{");
        sb.append("name='").append(name).append('\'');
        sb.append(", connection=").append(connection);
        sb.append('}');
        return sb.toString();
    }
}
