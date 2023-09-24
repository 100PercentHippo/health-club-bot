package com.c2t2s.hb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

class CasinoDB {

    // Hide default constructor
    private CasinoDB() {}

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(System.getenv("JDBC_DATABASE_URL"),
            System.getenv("JDBC_USERNAME"), System.getenv("JDBC_PASSWORD"));
    }

    static boolean addUser(long uid, String name) {
        boolean error = false;
        String query = "INSERT INTO money_user (uid, name, balance) VALUES(" + uid + ", '" + name +"', 1000) ON CONFLICT (uid) DO NOTHING;";
        String job = "INSERT INTO job_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String slots = "INSERT INTO slots_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String guess = "INSERT INTO guess_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String minislots = "INSERT INTO minislots_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String hugeguess = "INSERT INTO hugeguess_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String monemachine = "INSERT INTO moneymachine_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String overunder = "INSERT INTO overunder_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String blackjac = "INSERT INTO blackjack_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String gacha = "INSERT INTO gacha_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String event = "INSERT INTO event_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        List<String> allornothing = new ArrayList<>();
        for (AllOrNothing.Difficulty difficulty: AllOrNothing.Difficulty.values()) {
            allornothing.add("INSERT INTO allornothing_user (uid, rolls_to_double) VALUES (" + uid
                + ", " + difficulty.rollsToDouble + ") ON CONFLICT (uid, rolls_to_double) DO NOTHING;");
        }
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            error = statement.executeUpdate(query) < 1;
            if (!error) {
                statement.executeUpdate(job);
                statement.executeUpdate(slots);
                statement.executeUpdate(guess);
                statement.executeUpdate(minislots);
                statement.executeUpdate(hugeguess);
                statement.executeUpdate(monemachine);
                statement.executeUpdate(overunder);
                statement.executeUpdate(blackjac);
                statement.executeUpdate(gacha);
                statement.executeUpdate(event);
                for (String entry: allornothing) {
                    statement.executeUpdate(entry);
                }
            }
            statement.close();
            connection.close();
            AllOrNothing.addUserToCache(uid);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return error;
    }

    static void executeUpdate(String query) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    interface ResultSetConsumer <T> {
        public T apply(ResultSet results) throws SQLException;
    }

    static <T> T executeQueryWithReturn(String query, ResultSetConsumer<T> parseResult, T defaultValue) {
        Connection connection = null;
        Statement statement = null;
        T output = defaultValue;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            output = parseResult.apply(results);
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return output;
    }

    static Timestamp executeTimestampQuery(String query) {
        Timestamp defaultValue = new Timestamp(0);
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return results.getTimestamp(1);
            }
            return defaultValue;
        }, defaultValue);
    }

    static List<Long> executeListQuery(String query) {
        List<Long> defaultValue = new ArrayList<>();
        return CasinoDB.executeQueryWithReturn(query, results -> {
            List<Long> output = defaultValue;
            while (results.next()) {
                output.add(results.getLong(1));
            }
            return output;
        }, defaultValue);
    }

    static int executeIntQuery(String query) {
        int defaultValue = 0;
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return results.getInt(1);
            }
            return -1;
        }, defaultValue);
    }

    static long executeLongQuery(String query) {
        long defaultValue = -1L;
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return results.getLong(1);
            }
            return defaultValue;
        }, defaultValue);
    }
}
