package com.c2t2s.hb;

import discord4j.core.object.util.Snowflake;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*; //TODO: Remove the *
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBConnection {

    private static Map<Snowflake, Integer> users;
    private static List<String> complements;
    //TODO: Don't use this int, make it random
    private static int nextComplement = 0;

    public static void initialize() {
        users = new HashMap<>();
        //TODO: Find a more elegant solution to list initialization
        //TODO: Include the user's name in the messages
        complements = new ArrayList<>();
        complements.add("Good job!");
        complements.add("Well done!");
        complements.add("Keep it up!");
        complements.add("Yay!");
        complements.add("Wow!");
        //TODO: Read in old message to populate users list
        //TODO: Remove the users list and actually hook up a DB
    }

    public static String handleWorkout(Snowflake id) {
        //Insert the user if this is the first one
        boolean newUser = false;
        if (!users.containsKey(id)) {
            users.put(id, 0);
            newUser = true;
        }

        //Update the workout count
        int count = users.get(id);
        users.replace(id, ++count);

        //Check if this levels them up
        //TODO: This is a rather non-elegant implementation, make it nicer
        String levelUpMessage = "";
        if (count == 5)  {
            levelUpMessage = "\nCongratulations! You leveled up to Novice!";
        } else if (count == 15) {
            levelUpMessage = "\nCongratulations! You leveled up to Apprentice!";
        } else if (count == 30) {
            levelUpMessage = "\nCongratulations! You leveled up to Decent!";
        }
        //If people get to higher levels before the DB is ready, they're either
        //cheating or I'm being overly lazy

        //If they're new, warn that this isn't a database and may be wiped
        String newUserMessage = newUser
                ? "\nWarning: This is a temporary solution, and data may be wiped."
                : "";

        return complements.get(nextComplement++ % complements.size())
                + " You now have " + count + " points." + levelUpMessage
                + newUserMessage;
    }

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
