package com.c2t2s.hb;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*; //TODO: Remove the *
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Random;

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
	
	public static String handleRob(long uid) {
		Timestamp time = checkClaimTime(uid);
		String response = "";
		if (time == null) {
			if (addUser(uid)) {
				response = "Welcome! You have been given an initial balance of 1000 coins\n";
			} else {
				return "Unable to add new user, something went wrong :slight_frown:. Try +claim";
			}
		}
		long remainingWait = time.getTime() - System.currentTimeMillis();
		if (remainingWait > 0) {
			long hours = TimeUnit.MILLISECONDS.toHours(remainingWait);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingWait) - (60 * hours);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingWait) - (3600 * hours) - (60 * minutes);
			return response + "You are unable to rob! Try again in "
				+ ((hours > 0) ? (hours + " hour" + (hours == 1 ? "" : "s") + ", ") : "")
			    + ((minutes > 0) ? (minutes + " minute" + (minutes == 1 ? "" : "s") + " and ") : "")
			    + seconds + " second" + (seconds == 1 ? "" : "s") + ".";
		} else {
			Random random = new Random();
			if (random.nextInt(2) == 0) { // Busted
				setClaimTime(uid, "2 hours");
				return response + "You were caught! You are dragged off to jail for 2 hours.";
			} else { // Succeeded!
				if (random.nextInt(10) == 9) {
					int haul = (random.nextInt(3) * 250) + 500;
					int balance = addMoney(uid, haul);
					return response + "You find a safebox packed with diamonds! You sell them for " + haul + " coins! Your new balance is " + balance;
				} else {
			    	int haul = (random.nextInt(3) + 1) * 100;
			     	int balance = addMoney(uid, haul);
				    return response + "Your heist was successful, and you make away with a haul of " + haul + " coins. Your new balance is " + balance;
				}
			}
		}
	}
	
	public static String handlePickpocket(long uid) {
		Timestamp time = checkClaimTime(uid);
		String response = "";
		if (time == null) {
			if (addUser(uid)) {
				response = "Welcome! You have been given an initial balance of 1000 coins\n";
			} else {
				return "Unable to add new user, something went wrong :slight_frown:. Try +claim";
			}
		}
		long remainingWait = time.getTime() - System.currentTimeMillis();
		if (remainingWait > 0) {
			long hours = TimeUnit.MILLISECONDS.toHours(remainingWait);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingWait) - (60 * hours);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingWait) - (3600 * hours) - (60 * minutes);
			return response + "You are unable to pickpocket! Try again in "
				+ ((hours > 0) ? (hours + " hour" + (hours == 1 ? "" : "s") + ", ") : "")
			    + ((minutes > 0) ? (minutes + " minute" + (minutes == 1 ? "" : "s") + " and ") : "")
			    + seconds + " second" + (seconds == 1 ? "" : "s") + ".";
		} else {
			Random random = new Random();
			if (random.nextInt(2) == 0) { // Busted
				setClaimTime(uid, "30 minutes");
				return response + "You were caught! You are dragged off to jail for 30 minutes.";
			} else { // Succeeded!
				if (random.nextInt(10) == 9) {
					int haul = (random.nextInt(3) * 75) + 125;
					int balance = addMoney(uid, haul);
					return response + "You find a purse filled with diamonds! You sell them for " + haul + " coins! Your new balance is " + balance;
				} else {
			    	int haul = (random.nextInt(3) + 1) * 25;
			     	int balance = addMoney(uid, haul);
				    return response + "Your heist was successful, and you make away with a haul of " + haul + " coins. Your new balance is " + balance;
				}
			}
		}
	}
	
	public static String handleClaim(long uid) {
		Timestamp time = checkClaimTime(uid);
		if (time == null) {
			return insertMoneyUser(uid);
		}
		long remainingWait = time.getTime() - System.currentTimeMillis();
		if (remainingWait > 0) {
			long hours = TimeUnit.MILLISECONDS.toHours(remainingWait);
			long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingWait) - (60 * hours);
			long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingWait) - (3600 * hours) - (60 * minutes);
			return "You are unable to claim! Try again in "
				+ ((hours > 0) ? (hours + " hour" + (hours == 1 ? "" : "s") + ", ") : "")
			    + ((minutes > 0) ? (minutes + " minute" + (minutes == 1 ? "" : "s") + " and ") : "")
			    + seconds + " second" + (seconds == 1 ? "" : "s") + ".";
		} else {
			int balance = addMoney(uid, 100);
			setClaimTime(uid, "1 hour");
			return "Claimed 100 coins! New balance is " + balance;
		}
	}
	
	public static String handleGuess(long uid, int guess, int amount) {
		int balance = checkBalance(uid);
		if (balance < 0) {
			return "Unable to guess. Balance check failed or was negative (" + balance +")";
		} else if (balance < amount) {
			return "Your balance of " + balance + " is not enough to cover that!";
		}
		Random random = new Random();
		int correct = random.nextInt(10) + 1;
		if (guess == correct) {
			return "Correct! You win " + (10 * amount) + "! New balance is " + addMoney(uid, 10 * amount);
		} else {
		    return "The correct value was " + correct + ". Your new balance is " + addMoney(uid, -1 * amount);
		}
	}
	
// Payout:
//	5 of a kind: 4/125              8:1
//	4 of a kind: 1/25               6:1
//	3 of a kind: 12/25              0.5:1
//	Rainbow:     1/24               6:1
//	1 diamond:   1/20               1:1
//	2 diamonds:  1/1000             10:1
//	3 diamonds:  1/1 000 000        100:1
//	4 diamonds:  1/20 000 000       1000:1
//	5 diamonds:  1/10 000 000 000   10000:1
	
	public static String handleSlots(long uid, int amount) {
		int balance = checkBalance(uid);
		if (balance < 0) {
			return "Unable to guess. Balance check failed or was negative (" + balance +")";
		} else if (balance < amount) {
			return "Your balance of " + balance + " is not enough to cover that!";
		}
		Random random = new Random();
		int cherries = 0, oranges = 0, lemons = 0, blueberries = 0, grapes = 0, diamonds = 0;
		String output = "Bid " + amount + " on slots\n";
		int winnings = 0;
		for (int i = 0; i < 5; i++) {
			switch (random.nextInt(5)) {
			case 0:
				output += ":cherries:";
				cherries++;
				break;
			case 1:
				output += ":tangerine:";
				oranges++;
				break;
			case 2:
				output += ":lemon:";
				lemons++;
				break;
			case 3:
				output += ":blueberries:";
				blueberries++;
				break;
			case 4:
				if (random.nextInt(20) == 10) {
					output += ":gem:";
					diamonds++;
				} else {
					output += ":grapes:";
					grapes++;
				}
				break;
			}
		}
		output += "\n";
		if (cherries == 5 || oranges == 5 || lemons == 5 || blueberries == 5 || grapes == 5) {
			output += ":moneybag::moneybag: 5 OF A KIND!!! :moneybag::moneybag:";
			winnings += 8 * amount;
		} else if (cherries == 4 || oranges == 4 || lemons == 4 || blueberries == 4 || grapes == 4) {
			output += ":moneybag: 4 of a kind!! :moneybag: ";
			winnings += 6 * amount;
		} else if (cherries == 3 || oranges == 3 || lemons == 3 || blueberries == 3 || grapes == 3) {
			output += "3 of a kind. ";
			winnings += (int)(0.5 * amount);
		} else if (cherries == 1 && oranges == 1 && lemons == 1 && blueberries == 1 && grapes == 1) {
			output += "Fruit salad! ";
			winnings += 6 * amount;
		}
		if (diamonds > 0) {
			output += ":gem: " + diamonds + " diamond" + (diamonds == 1 ? "" : "s") + "! :gem: ";
			if (diamonds > 3) { output += "Jackpot!!! "; }
			winnings += (int)Math.pow(10, diamonds);
		}
		balance = addMoney(uid, winnings - amount);
		if (winnings > 0) {
			output += "Total winnings: " + (winnings) + " ";
		}
	    output += "New balance: " + balance;
		return output;
	}
	
	private static String insertMoneyUser(long uid) {
        boolean error = addUser(uid);
        if (!error) {
			return "Welcome! You have been given an initial balance of 1000 coins";
        } else {
			return "Unable to add new user. Something may have gone wrong :slight_frown:";
		}
    }
	
	public static String handleBalance(long uid) {
		int balance = checkBalance(uid);
		if (balance < 0) {
			return "There was an issue checking your balance, value returned was " + balance;
		} else {
			return "Your current balance is " + balance + " coin" + (balance == 1 ? "" : "s") + ".";
		}
	}
	
	public static String handleGive(long donorUid, long recipientUid, int amount) {
		if (amount <= 0) {
			return "Can't give someone a negative number of coins. Try asking them nicely if you want money.";
		}
		int donorBalance = checkBalance(donorUid);
		if (donorBalance < 0) {
			return "Unable to give money. Balance check failed or was negative (" + donorBalance +")";
		} else if (donorBalance < amount) {
			return "Your balance of " + donorBalance + " is not enough to cover that!";
		}
		int recipientBalance = checkBalance(recipientUid);
		if (recipientBalance < 0) {
			insertMoneyUser(recipientUid);
		}
		donorBalance = addMoney(donorUid, -1 * amount);
		addMoney(recipientUid, amount);
        if (donorBalance < 0) {
        	return "Unable to process transaction";
        } else {
        	return "Gave " + amount + ", your new balance is " + donorBalance;
        }
	}
	
	public static String handleLeaderboard() {
		return parseLeaderboard();
	}
	
	//////////////////////////////////////////////////////////
	
    private static Connection getConnection() throws URISyntaxException, SQLException {
        return DriverManager.getConnection(System.getenv("JDBC_DATABASE_URL"));
    }
	
	//CREATE TABLE IF NOT EXISTS money_user (
	//	uid bigint PRIMARY KEY,
	//	balance int DEFAULT 0,
	//	last_claim timestamp DEFAULT '2021-01-01 00:00:00'
	//);
	
	private static Timestamp checkClaimTime(long uid) {
		String query = "SELECT last_claim FROM money_user WHERE uid = " + uid + ";";
		Timestamp time = null;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
			if (results.next()) {
				time = results.getTimestamp(1);
			}
            results.close();
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
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
        return time;
	}
	
	private static void setClaimTime(long uid, String interval) {
		String query = "UPDATE money_user SET last_claim = NOW() + INTERVAL '" + interval + "' WHERE uid = " + uid + ";";
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
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
	
	private static int checkBalance(long uid) {
		boolean error = false;
		String query = "SELECT balance FROM money_user WHERE uid = " + uid + ";";
		int balance = 0;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
            	balance = results.getInt(1);
            } else {
            	balance = -1;
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
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
        return balance;
	}
	
	private static int addMoney(long uid, int amount) {
		String query = "UPDATE money_user SET balance = balance + " + amount + " WHERE uid = " + uid + " RETURNING balance;";
        Connection connection = null;
        Statement statement = null;
        int balance = 0;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
            	balance = results.getInt(1);
            } else {
            	balance = -1;
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
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
        return balance;
	}
	
	private static boolean addUser(long uid) {
		boolean error = false;
		String query = "INSERT INTO money_user VALUES(" + uid + ",1000,NOW()) ON CONFLICT (uid) DO NOTHING;";
		int inserted = 0;
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            if (statement.executeUpdate(query) < 1) {
            	error = true;
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
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
	
	private static String parseLeaderboard() {
		String query = "SELECT * FROM money_user ORDER BY balance DESC LIMIT 3;";
        Connection connection = null;
        Statement statement = null;
        String leaderboard = "";
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            int place = 1;
            while (results.next()) {
            	leaderboard += "#" + place++ + " ";
            	leaderboard += HBMain.getUsername(results.getLong(1));
            	leaderboard += " " + results.getInt(2) + "\n";
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
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
        return leaderboard;
	}

}
