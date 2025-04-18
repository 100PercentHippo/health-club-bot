package com.c2t2s.hb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class CasinoDB {

    // Hide default constructor
    private CasinoDB() {}

    static final int DEFAULT_UPDATE_RETURN = -1;

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(System.getenv("JDBC_DATABASE_URL"),
            System.getenv("JDBC_USERNAME"), System.getenv("JDBC_PASSWORD"));
    }

    static boolean addUser(long uid, String name) {
        boolean error = false;
        int index = name.indexOf('#');
        String nickname;
        if (index > 0) {
            nickname = name.substring(0, index);
        } else {
            nickname = name;
        }
        String query = "INSERT INTO money_user (uid, name, nickname, balance) VALUES(?, ?, ?, 1000) ON CONFLICT (uid) DO NOTHING;";
        List<String> uidStatements = new ArrayList<>();
        uidStatements.add("INSERT INTO job_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO slots_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO guess_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO minislots_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO hugeguess_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO moneymachine_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO overunder_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO blackjack_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO gacha_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO event_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        uidStatements.add("INSERT INTO workout_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;");
        for (AllOrNothing.Difficulty difficulty: AllOrNothing.Difficulty.values()) {
            uidStatements.add("INSERT INTO allornothing_user (uid, rolls_to_double) VALUES (" + uid
                + ", " + difficulty.rollsToDouble + ") ON CONFLICT (uid, rolls_to_double) DO NOTHING;");
        }
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Statement statement = null;
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setLong(1, uid);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, nickname);
            error = preparedStatement.executeUpdate() < 1;
            if (!error) {
                statement = connection.createStatement();
                for (String entry: uidStatements) {
                    statement.addBatch(entry);
                }
                statement.executeBatch();
                statement.close();
            }
            connection.close();
            AllOrNothing.addUserToCache(uid);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Query was: " + query);
        } finally {
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
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

    static boolean executeUpdate(String query) {
        Connection connection = null;
        Statement statement = null;
        int result = DEFAULT_UPDATE_RETURN;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            result = statement.executeUpdate(query);
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Query was: " + query);
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
        return result > 0;
    }

    static boolean executeValidatedUpdate(String query, String... userArgs) {
        Connection connection = null;
        PreparedStatement statement = null;
        int result = DEFAULT_UPDATE_RETURN;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(query);
            for (int i = 0; i < userArgs.length; ++i) {
                // SQL is 1-indexed
                statement.setString(i + 1, userArgs[i]);
            }
            result = statement.executeUpdate();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Query was: " + query);
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
        return result > 0;
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
            System.out.println("Query was: " + query);
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

    static <T> T executeValidatedQueryWithReturn(String query, ResultSetConsumer<T> parseResult,
            T defaultValue, String... userArgs) {
        Connection connection = null;
        PreparedStatement statement = null;
        T output = defaultValue;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(query);
            for (int i = 0; i < userArgs.length; ++i) {
                // SQL is 1-indexed
                statement.setString(i + 1, userArgs[i]);
            }
            ResultSet results = statement.executeQuery();
            output = parseResult.apply(results);
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Query was: " + query);
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
        return executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return results.getTimestamp(1);
            }
            return defaultValue;
        }, defaultValue);
    }

    static List<Long> executeListQuery(String query) {
        List<Long> defaultValue = new ArrayList<>();
        return executeQueryWithReturn(query, results -> {
            List<Long> output = defaultValue;
            while (results.next()) {
                output.add(results.getLong(1));
            }
            return output;
        }, defaultValue);
    }

    static Set<Long> executeSetQuery(String query) {
        Set<Long> defaultValue = new HashSet<>();
        return executeQueryWithReturn(query, results -> {
            Set<Long> output = defaultValue;
            while (results.next()) {
                output.add(results.getLong(1));
            }
            return output;
        }, defaultValue);
    }

    static int executeIntQuery(String query) {
        int defaultValue = 0;
        return executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return results.getInt(1);
            }
            return -1;
        }, defaultValue);
    }

    static long executeLongQuery(String query) {
        long defaultValue = -1L;
        return executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return results.getLong(1);
            }
            return defaultValue;
        }, defaultValue);
    }

    static String executeStringQuery(String query) {
        String defaultValue = "";
        return executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return results.getString(1);
            }
            return defaultValue;
        }, defaultValue);
    }

    static List<HBMain.AutocompleteIdOption> executeAutocompleteIdQuery(String query) {
        List<HBMain.AutocompleteIdOption> defaultValue = new ArrayList<>();
        return executeQueryWithReturn(query, results -> {
            List<HBMain.AutocompleteIdOption> output = defaultValue;
            while (results.next()) {
                long id = results.getLong(1);
                String description = results.getString(2);
                output.add(new HBMain.AutocompleteIdOption(id, description));
            }
            return output;
        }, defaultValue);
    }
}
