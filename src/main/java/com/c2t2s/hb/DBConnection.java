package com.c2t2s.hb;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;

public class DBConnection {

    private static Connection getConnection() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));
        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + "?sslmode=require";
        return DriverManager.getConnection(dbUrl, username, password);
    }

    private static boolean ExecuteQuery(String query) {
        boolean error = false;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            results.close();
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
            error = true;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                error = true;
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                error = true;
            }
        }
        return error;
    }

}
