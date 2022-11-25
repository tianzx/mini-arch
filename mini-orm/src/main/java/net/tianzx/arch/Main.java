package net.tianzx.arch;

import java.sql.Connection;

public class Main {
    private Connection connection;
    public static void main(String[] args) {
    }

    public Connection getConnection() {
        try {
            Class.forName("");
        }catch (Exception e) {

        }
        return connection;
    }
}
