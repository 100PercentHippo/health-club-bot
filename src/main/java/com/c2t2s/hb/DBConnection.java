package com.c2t2s.hb;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.util.Snowflake;
import discord4j.core.object.entity.Member;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*; //TODO: Remove the *
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DBConnection {

    // private static Map<Snowflake, Integer> users = new HashMap<>();
    // private static List<String> complements = new ArrayList<>();
    // private static List<String> roles = Arrays.asList("Novice", "Apprentice", "Decent", "Experienced", "Veteran", "Expert",
            // "Master", "Hero", "Legend", "Demi-God", "Immortal");
    // private static Map<String, Snowflake> roleIDs = new HashMap<>();
    // //TODO: Don't use this int, make it random
    // private static int nextComplement = 0;
    // public static boolean initialized = false;

    // public static void initialize(MessageCreateEvent event) {
        // if (!initialized) {
            // // Read in all the messages in the channel to populate the fake DB
            // event.getMessage().getChannel().block().getMessagesBefore(event.getMessage().getId()).doOnNext(message -> {
                // if (message.getContent().orElse("").equalsIgnoreCase("+workout")) {
                    // Snowflake id = message.getAuthorAsMember().block().getId();
                    // if (!users.containsKey(id)) {
                        // users.put(id, 0);
                    // }
                    // int count = users.get(id);
                    // users.replace(id, ++count);
                // }
            // }).blockLast();
            // // We don't want to double count the message we're about to handle
            // event.getMember().ifPresent(member -> {
                // int count = users.get(member.getId());
                // users.put(member.getId(), --count);
            // });
            // // Populate a list of the roles we care about with their internal IDs
            // event.getGuild().block().getRoles().doOnNext(role -> {
                // if (roles.contains(role.getName())) {
                    // roleIDs.put(role.getName(), role.getId());
                // }
            // }).blockLast();
            // //TODO: Find a more elegant solution to list initialization
            // //TODO: Include the user's name in the messages
            // complements.add("Good job!");
            // complements.add("Well done!");
            // complements.add("Keep it up!");
            // complements.add("Yay!");
            // complements.add("Wow!");
            // //TODO: Read in old message to populate users list
            // //TODO: Remove the users list and actually hook up a DB
            // initialized = true;
        // }
    // }

    // public static String handleWorkout(Member member) {
        // Snowflake id = member.getId();

        // //Insert the user if this is the first one
        // if (!users.containsKey(id)) {
            // users.put(id, 0);
        // }

        // //Update the workout count
        // int count = users.get(id);
        // users.replace(id, ++count);

        // //Check if this levels them up
        // //TODO: This is a rather non-elegant implementation, make it nicer
        // String levelUpMessage = "";
        // if (count == 5)  {
            // levelUpMessage = "\nCongratulations! You leveled up to Novice!";
            // applyRole(member, "Novice");
        // } else if (count == 15) {
            // levelUpMessage = "\nCongratulations! You leveled up to Apprentice!";
            // applyRole(member, "Apprentice");
        // } else if (count == 30) {
            // levelUpMessage = "\nCongratulations! You leveled up to Decent!";
            // applyRole(member, "Decent");
        // } else if (count == 50) {
            // levelUpMessage = "\nCongratulations! You leveled up to Experienced!";
            // applyRole(member, "Experienced");
        // }
        // //If people get to higher levels before the DB is ready, they're either
        // //cheating or I'm being overly lazy

        // Random random = new Random();
        // return complements.get(random.nextInt(complements.size()))
                // + " You now have " + count + " point" + (count != 1 ? "s" : "") + "." + levelUpMessage;
    // }

    // private static void applyRole(Member member, String newRole) {
        // //if (!member.getRoleIds().contains(roleIDs.get(newRole))) {
        // //    // Remove all potentially incorrect roles
        // //    for (Snowflake role : roleIDs.values()) {
        // //        member.removeRole(role).doOnError(e -> {}).block();
        // //    }
        // //    // Add the current role
        // //    member.addRole(roleIDs.get(newRole)).doOnError(e -> {
        // //        //TODO: Handle
        // //    }).block();
        // //}
    // }
	
    private static Connection getConnection() throws URISyntaxException, SQLException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));
        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + "?sslmode=require";
        return DriverManager.getConnection(dbUrl, username, password);
    }
	
	//CREATE TABLE IF NOT EXISTS money_user (
	//	uid bigint PRIMARY KEY,
	//	balance int DEFAULT 0,
	//	last_claim timestamp DEFAULT '2021-01-01 00:00:00'
	//);
	
	private static String handleClaim(long uid) {
		boolean error = false;
		String query = "SELECT last_claim FROM money_user WHERE uid = " + uid + ";";
		boolean found = true;
		String result;
		Timestamp time;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
			if (results.next()) {
				time = results.getTimestamp();
				long elapsedTime = System.currentTimeMillis() - time.getTime();
				if (elapsedTime < 3600000) {
					long timeLeft = 3600000 - elapsedTime;
					result = "You already claimed this hour! Try again in " + TimeUnit.MILLISECONDS.toMinutes(timeLeft) + " minutes and " + TimeUnit.MILLISECONDS.toSeconds(timeLeft) + " seconds.";
				} else {
					query = "UPDATE money_user SET (balance, last_claim) = (balance + 100, NOW()) WHERE uid = " + uid + " RETURNING balance;";
					ResultSet results = statement.executeQuery(query);
					if (results.next()) {
						int balance = results.getInt(0);
						result = "Claimed 100 coins! New balance is " + balance;
					} else {
						result = "Error claiming coins :slight_frown:";
					}
				}
			} else {
				found = false;
			}
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
        if (!found) {
			return insertMoneyUser(uid);
		}
        return result;
	}
	
	private static String insertMoneyUser(long uid) {
        boolean error = false;
		String query = "INSERT INTO money_user VALUES(" + uid + ",1000,NOW()) ON CONFLICT (uid) DO NOTHING;";
		int inserted = 0;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            inserted = statement.executeUpdate(query);
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
        if (!error && inserted != 0) {
			return "Welcome! You have been given an initial balance of 1000 coins";
        } else {
			return "Unable to add new user. Something may have gone wrong :slight_frown:";
		}
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
