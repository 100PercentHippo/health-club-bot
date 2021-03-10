package com.c2t2s.hb;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*; //TODO: Remove the *
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class Casino {
	
	public static class User {
		private int work;
		private int fish;
		private int pick;
		private int rob;
		private long balance;
		private boolean inJail;
		private Timestamp timer;
		
		public User(int w, int f, int p, int r, long b, boolean jail, Timestamp time) {
			work = w;
			fish = f;
			pick = p;
			rob = r;
			balance = b;
			inJail = jail;
			timer = time;
		}
		
		public int getMorality() {
			return (2 * work) + fish - pick - (2 * rob);
		}
		
		public long getBalance() {
			return balance;
		}
		
		public boolean isJailed() {
			return inJail;
		}
		
		public Timestamp getTimer() {
			return timer;
		}
	}
	
	private static String formatTime(long time) {
		long hours = TimeUnit.MILLISECONDS.toHours(time);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(time) - (60 * hours);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(time) - (3600 * hours) - (60 * minutes);
		return ((hours > 0) ? (hours + " hour" + (hours == 1 ? "" : "s") + ", ") : "")
		    + ((minutes > 0) ? (minutes + " minute" + (minutes == 1 ? "" : "s") + " and ") : "")
		    + seconds + " second" + (seconds == 1 ? "" : "s") + ".";
	}
	
// Payout:
//  :mechanic:     25% 150
//   (:scientist:)     250 (50% if high morality)
//  :farmer:       25% 250
//   (:firefighter:)   250 (50% if high morality)
//  :chef:         20% 200
//  :detective:     5% 200
//  :artist:       25% 250
	
	public static String handleWork(long uid) {
		User user = getUser(uid);
		if (user == null) {
			return "Unable to fetch user. If you're new type `+claim` to start";
		}
		long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
		if (remainingTime > 0) {
			if (user.isJailed()) {
				return "You're still in jail! Your sentence ends in " + formatTime(remainingTime);
			} else {
				return "You're still tired. Try again in " + formatTime(remainingTime);
			}
		}
		Random random = new Random();
		int roll = random.nextInt(100);
		if (roll < 25) {
			if (user.getMorality() > 5 && random.nextInt(2) == 0) {
				return ":scientist: You use your connections and work in The Lab for 2 hours and make 250 coins! Your new balance is " + logWork(uid, 250);
			} else {
				return ":mechanic: You work as a mechanic for 2 hours and make 150 coins. Your new balance is " + logWork(uid, 150);
			}
		} else if (roll < 50) {
			if (user.getMorality() > 5 && random.nextInt(2) == 0) {
				return ":firefighter: You use your connections and put out fires and save kittens for 2 hours and make 250 coins! Your new balance is " + logWork(uid, 250);
			} else {
				return ":farmer: You work hard in a field for 2 hours and make 150 coins. It ain't much, but it's honest work. Your new balance is " + logWork(uid, 150);
			}
		} else if (roll < 70) {
			return ":chef: You work as a chef for 2 hours and make 200 coins. Your new balance is " + logWork(uid, 200);
		} else if (roll < 75) {
			return ":detective: You work as a detective trying to find a missing satellite. You're unable to find it after 2 hours, but are still paid 200 coins. Your new balance is " + logWork(uid, 200);
		} else {
			return ":artist: You make an artistic masterpiece and sell it for 250 coins! Your new balance is " + logWork(uid, 250);
		}
	}
	
// Payout:
//  :fish:              80% 40/50/60
//  :satellite_orbital:  5% 75
//  :octopus:            5% 75
//  :crab:               5% 100
//  :ring:               5% 250 (400 if high morality)
	
	public static String handleFish(long uid) {
		User user = getUser(uid);
		if (user == null) {
			return "Unable to fetch user. If you're new type `+claim` to start";
		}
		long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
		if (remainingTime > 0) {
			if (user.isJailed()) {
				return "There's no fishing pool in jail! Your sentence ends in " + formatTime(remainingTime);
			} else {
				return "You're still tired. Try again in " + formatTime(remainingTime);
			}
		}
		Random random = new Random();
		int roll = random.nextInt(100);
		if (roll < 80) {
			int fish = (random.nextInt(3) + 4);
			return ":fish: You fish for 30 minutes and catch " + fish
				+ " fish. You sell them for " + (fish * 10) + " coins. Your new balance is "
			    + logFish(uid, false, fish * 10);
		} else if (roll < 85) {
			return ":satellite_orbital: You fish up a satellite??? You're not sure how it got there, but you turn it into The Lab, and they pay you 75 coins. Your new balance is "
		        + logFish(uid, false, 75);
		} else if (roll < 90) {
			return ":octopus: You fish up a octopus, and cook it into delicious sushi worth 75 coins. Your new balance is "
		        + logFish(uid, false, 75);
		} else if (roll < 95) {
			return ":crab: You fish up crab. It pays you 100 coins to let it return to its dance party. Your new balance is "
		        + logFish(uid, false, 100);
		} else {
			if (user.getMorality() > 5) {
				return ":ring: You fish up a ring. Since you're a good person you return it to its rightful owner and are rewarded with 400 coins! Your new balance is "
			        + logFish(uid, true, 400);
			} else {
				return ":ring: You fish up a ring, and sell it for 250 coins! Your new balance is "
			        + logFish(uid, true, 250);
			}
		}
	}
	
// Payout:
//  :books:             5% -10
//   (:slot_machine:)       400 (replaces books if bad)
//  :motorway:          10% 0
//  :house_adandoned:   10% 5
//  :house:             25% 200
//  :convenience_store: 25% 300
//  :bank:              25% 350
	
	public static String handleRob(long uid) {
		User user = getUser(uid);
		if (user == null) {
			return "Unable to fetch user. If you're new type `+claim` to start";
		}
		long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
		if (remainingTime > 0) {
			if (user.isJailed()) {
				return "The guard gives you a funny look. You're still in jail for " + formatTime(remainingTime);
			} else {
				return "You're still tired. Try again in " + formatTime(remainingTime);
			}
		}
		Random random = new Random();
		if (random.nextInt(2) == 0) {
			robFailed(uid);
			return "You were caught! You are dragged off to jail for 2 hours.";
		}
		int roll = random.nextInt(100);
		if (roll < 5) {
			if (user.getMorality() < -10) {
				return ":slot_machine: You use your criminal knowledge and rob the slot machine of 400 coins! Your new balance is "
			        + logRob(uid, true, 400) + "\nWait! Get away from that!";
			} else if (user.getBalance() > 10) {
				return ":book: You rob The Bank! Wait, that's not The Bank, that's The Library. You pay the late fee of 10 coins for your overdue books and leave before the cops arrive. Your new balance is "
			        + logRob(uid, false, -10);
			} else {
				logRob(uid, false, 0);
				return ":book: You rob The Bank! Wait, that's not The Bank, that's The Library. You quickly leave before the cops arrive.";
			}
		} else if (roll < 15) {
			logRob(uid, false, 0);
			return ":motorway: You attempt a highway robbery, but your horse and six shooter are no match for modern automobiles.";
		} else if (roll < 25) {
			return ":house_abandoned: You rob a house, but find it empty and abandoned. You pick up 5 coins off the ground and leave. Your new balance is "
		        + logRob(uid, false, 5);
		} else if (roll < 50) {
			return ":house: You rob a rich looking house and get away with 200 coins. Your new balance is "
		        + logRob(uid, false, 200);
		} else if (roll < 75) {
			return ":convenience_store: You rob a convenience store and grab 300 coins from the register! Your new balance is "
				+ logRob(uid, true, 300);
		} else {
			return ":bank: You rob The Bank and grab 350 coins worth of diamonds! Your new balance is "
		        + logRob(uid, true, 350);
		}
	}
	
// Payout:
//  :paperclip:         10% 0
//  :satellite_orbital:  5% 0
//  :lungs:              5% 0 (150 if low morality)
//  :moneybags:         50% 50/70/90
//  :computer:          15% 100
//  :medal:             10% 125
//  :gem:                5% 250
	
	public static String handlePickpocket(long uid) {
		User user = getUser(uid);
		if (user == null) {
			return "Unable to fetch user. If you're new type `+claim` to start";
		}
		long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
		if (remainingTime > 0) {
			if (user.isJailed()) {
				return "The guard gives you a funny look. You're still in jail for " + formatTime(remainingTime);
			} else {
				return "You're still tired. Try again in " + formatTime(remainingTime);
			}
		}
		Random random = new Random();
		if (random.nextInt(2) == 0) {
			pickFailed(uid);
			return "You were caught! You are dragged off to jail for 30 minutes.";
		}
		int roll = random.nextInt(100);
		if (roll < 10) {
			logPick(uid, false, 0);
		    return ":paperclip: You steal a paperclip, which you use to bundle together your wanted posters you took down.";
		} else if (roll < 15) {
			if (user.getMorality() < -10) {
				return ":lungs: You pickpocket a pair of lungs. Using your criminal connections you find a buyer who pays 150 coins. Your new balance is "
			        + logPick(uid, true, 150);
			} else {
				logPick(uid, false, 0);
				return ":lungs: You pickpocket a pair of lungs???? This was supposed to be a petty theft! You drop them on the ground and quickly run away.";
			}
		} else if (roll < 20) {
			logPick(uid, false, 0);
			return ":satellite_orbital: You pickpocket an orbital satellite???? Unsure what to do with it you ditch it in a nearby lake.";
		} else if (roll < 70) {
			int haul = 50 + (random.nextInt(3) * 20);
			return ":moneybags: You successfully pickpocket " + haul + " coins. Your new balance is "
				+ logPick(uid, false, haul);
		} else if (roll < 85) {
			return ":computer: You pickpocket a laptop computer! You sell it for 100 coins, and your new balance is "
		        + logPick(uid, false, 100);
		} else if (roll < 95) {
			return ":medal: You pickpocket a medal of pure gold! You sell it for 125 coins, and your new balance is "
		        + logPick(uid, false, 125);
		} else {
			return ":gem: You grab a large diamond worth 250 coins!! Your new balance is "
		        + logPick(uid, true, 250);
		}
	}
	
	public static String handleClaim(long uid, String name) {
		User user = getUser(uid);
		String response = "";
		if (user == null) {
			boolean error = addUser(uid, name);
	        if (!error) {
				response += "Welcome! You have been given an initial balance of 1000 coins";
	        } else {
				response += "Unable to add new user. Something may have gone wrong :slight_frown:";
			}
		}
		return response + "\nTo earn money, use one of the following commands:"
		    + "\n\t`+work` Work for 2 hours. This is a lawful pursuit"
		    + "\n\t`+fish` Fish for 30 minutes. This is a lawful pursuit"
		    + "\n\t`+pickpocket` Attempt to pickpocket. This is a criminal pursuit and risks 30 minutes in jail"
		    + "\n\t`+rob` Attempt a robbery. This is a crimal pursuit and risks 2 hours of jail time"
		    + "\nIf you get a particularly lawful or particularly crimal record, you may get unique options."
		    + "You can also gamble with `+guess`, `+slots`, or `+minislots`";
	}
	
// Payout:
//  Correct:        1/10  6:1
//  Close:          1/5   2:1
//  Dealer mistake: 1/200 2.5:1
	
	public static String handleGuess(long uid, int guess, int amount) {
		long balance = checkBalance(uid);
		if (balance < 0) {
			return "Unable to guess. Balance check failed or was negative (" + balance +")";
		} else if (balance < amount) {
			return "Your balance of " + balance + " is not enough to cover that!";
		}
		Random random = new Random();
		int correct = random.nextInt(10) + 1;
		if (guess == correct) {
			if (guess == 1 || guess == 10) {
				guessWin(uid, amount, 6 * amount);
				return "Correct!! Big win of " + (6 * amount) + "! New balance is " + addMoney(uid, 6 * amount);
			}
			guessWin(uid, amount, 5 * amount);
			return "Correct! You win " + (5 * amount) + "! New balance is " + addMoney(uid, 5 * amount);
		} else if (guess + 1 == correct || guess - 1 == correct) {
			guessClose(uid, amount, 1 * amount);
			return "Very close. The value was " + correct + ". You get " + (1 * amount) + " as a consolation prize. New balance is " + addMoney(uid, 1 * amount);
		} else {
			if (random.nextInt(140) == 0) {
				guessMistake(uid, amount, ((int)2.5 * amount));
				return "The correct value was " + (random.nextInt(5) + 11) + ". Wait, that isn't right. Here, take " + ((int)2.5 * amount) + " to pretend that never happened. New balance is " + addMoney(uid, ((int)2.5 * amount));
			}
			guessLoss(uid, amount);
		    return "The correct value was " + correct + ". Your new balance is " + addMoney(uid, -1 * amount);
		}
	}
	
// Payout:
//  Correct:        1/10  10:1
	
	public static String handleBigGuess(long uid, int guess, int amount) {
		long balance = checkBalance(uid);
		if (balance < 0) {
			return "Unable to guess. Balance check failed or was negative (" + balance +")";
		} else if (balance < amount) {
			return "Your balance of " + balance + " is not enough to cover that!";
		}
		Random random = new Random();
		int correct = random.nextInt(10) + 1;
		if (guess == correct) {
			guessWin(uid, amount, 10 * amount);
			return "Correct! You win " + (10 * amount) + "! New balance is " + addMoney(uid, 10 * amount);
		} else {
			guessLoss(uid, amount);
		    return "The correct value was " + correct + ". Your new balance is " + addMoney(uid, -1 * amount);
		}
	}
	
// Payout:
//	5 of a kind: 1/625              150:1
//	4 of a kind: 4/125              8:1
//	3 of a kind: 32/125             1:1
//	Rainbow:     24/625             6.5:1
//	1 diamond:   1/20               1:1
//	2 diamonds:  1/1000             10:1
//	3 diamonds:  1/1 000 000        100:1
//	4 diamonds:  1/20 000 000       1000:1
//	5 diamonds:  1/10 000 000 000   10000:1
	
	public static String handleSlots(long uid, int amount) {
		long balance = checkBalance(uid);
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
			winnings += 150 * amount;
		} else if (cherries == 4 || oranges == 4 || lemons == 4 || blueberries == 4 || grapes == 4) {
			output += ":moneybag: 4 of a kind!! :moneybag: ";
			winnings += 8 * amount;
		} else if (cherries == 3 || oranges == 3 || lemons == 3 || blueberries == 3 || grapes == 3) {
			output += "3 of a kind. ";
			winnings += amount;
		} else if (cherries == 1 && oranges == 1 && lemons == 1 && blueberries == 1 && grapes == 1) {
			output += "Fruit salad! ";
			winnings += (int)(6.5 * amount);
		}
		if (diamonds > 0) {
			output += ":gem: " + diamonds + " diamond" + (diamonds == 1 ? "" : "s") + "! :gem: ";
			if (diamonds > 3) { output += "Jackpot!!! "; }
			winnings += amount * (int)Math.pow(10, diamonds - 1);
		}
		balance = addMoney(uid, winnings - amount);
		if (winnings > 0) {
			output += "Total winnings: " + (winnings) + " ";
		}
	    output += "New balance: " + balance;
	    logSlots(uid, amount, winnings, diamonds);
		return output;
	}
	
// Payout
//  3 of a kind: 1/25      5:1
//  2 of a kind: 12/25     1.6:1
//  1 diamond:   3/100     0.4:1
//  2 diamonds:  3/10000   10:1
//  3 diamonds:  1/1000000 100:1
	
	public static String handleMinislots(long uid, int amount) {
		long balance = checkBalance(uid);
		if (balance < 0) {
			return "Unable to guess. Balance check failed or was negative (" + balance +")";
		} else if (balance < amount) {
			return "Your balance of " + balance + " is not enough to cover that!";
		}
		Random random = new Random();
		int cherries = 0, oranges = 0, lemons = 0, blueberries = 0, grapes = 0, diamonds = 0;
		String output = "Bid " + amount + " on mini slots\n";
		int winnings = 0;
		for (int i = 0; i < 3; i++) {
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
		if (cherries == 3 || oranges == 3 || lemons == 3 || blueberries == 3 || grapes == 3) {
			output += ":moneybag: 3 of a kind! :moneybag: ";
			winnings += 5 * amount;
		} else if (cherries == 2 || oranges == 2 || lemons == 2 || blueberries == 2 || grapes == 2) {
			output += "2 of a kind. ";
			winnings += (int)(1.6 * amount);
		}
		if (diamonds == 1) {
			output += "1 bonus diamond :gem:";
			winnings += (int)(0.4 * amount);
		} else if (diamonds > 1) {
			output += ":gem: " + diamonds + " diamond" + (diamonds == 1 ? "" : "s") + "! :gem: Jackpot!!! ";
			winnings += amount * (int)Math.pow(10, diamonds - 1);
		}
		balance = addMoney(uid, winnings - amount);
		if (winnings > 0) {
			output += "Total winnings: " + (winnings) + " ";
		}
	    output += "New balance: " + balance;
	    logMinislots(uid, amount, winnings, diamonds);
		return output;
	}
	
	public static String handleBalance(long uid) {
		long balance = checkBalance(uid);
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
		long donorBalance = checkBalance(donorUid);
		if (donorBalance < 0) {
			return "Unable to give money. Balance check failed or was negative (" + donorBalance +")";
		} else if (donorBalance < amount) {
			return "Your balance of " + donorBalance + " is not enough to cover that!";
		}
		if (donorUid == recipientUid) {
			return "You give yourself " + amount + ". Your balance is unchanged for some reason.";
		}
		long recipientBalance = checkBalance(recipientUid);
		if (recipientBalance == -1) {
			return "Unable to give money. Has that user run `+claim`?";
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
    //  name varchar(40) DEFAULT '',
	//	balance bigint DEFAULT 0,
    //  in_jail boolean DEFAULT false,
	//	last_claim timestamp DEFAULT '2021-01-01 00:00:00',
    //  timestamp2 timestamp DEFAULT '2021-01-01 00:00:00'
	//);
    
    //CREATE TABLE IF NOT EXISTS job_user (
    //  uid bigint PRIMARY KEY,
    //  work_count integer DEFAULT 0,
    //  work_profit bigint DEFAULT 0,
    //  fish_count integer DEFAULT 0,
    //  fish_jackpots integer DEFAULT 0,
    //  fish_profit bigint DEFAULT 0,
    //  pick_count integer DEFAULT 0,
    //  pick_fails integer DEFAULT 0,
    //  pick_jackpots integer DEFAULT 0,
    //  pick_profit bigint DEFAULT 0,
    //  rob_count integer DEFAULT 0,
    //  rob_fails integer DEFAULT 0,
    //  rob_jackpots integer DEFAULT 0,
    //  rob_profit bigint DEFAULT 0,
    //  jail_time bigint DEFAULT 0,
    //  CONSTRAINT jobs_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    //);
    
    //CREATE TABLE IF NOT EXISTS slots_user (
    //  uid bigint PRIMARY KEY,
    //  pulls integer DEFAULT 0,
    //  diamonds integer DEFAULT 0,
    //  spent bigint DEFAULT 0,
    //  winnings bigint DEFAULT 0,
    //  CONSTRAINT slots_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    //);
    
    //CREATE TABLE IF NOT EXISTS guess_user (
    //  uid bigint PRIMARY KEY,
    //  guesses integer DEFAULT 0,
    //  correct integer DEFAULT 0,
    //  close integer DEFAULT 0,
    //  mistake integer DEFAULT 0,
    //  spent bigint DEFAULT 0,
    //  winnings bigint DEFAULT 0,
    //  CONSTRAINT guess_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    //);
    
    //CREATE TABLE IF NOT EXISTS minislots_user (
    //  uid bigint PRIMARY KEY,
    //  pulls integer DEFAULT 0,
    //  diamonds integer DEFAULT 0,
    //  spent integer DEFAULT 0,
    //  winnings integer DEFAULT 0,
    //  CONSTRAINT minislots_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
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
	
	private static long checkBalance(long uid) {
		return executeBalanceQuery("SELECT balance FROM money_user WHERE uid = " + uid + ";");
	}
	
	private static long addMoney(long uid, int amount) {
		return executeBalanceQuery("UPDATE money_user SET balance = balance + "
	        + amount + " WHERE uid = " + uid + " RETURNING balance;");
	}
	
	private static long addWorkMoney(long uid, int amount, String delay) {
		return executeBalanceQuery("UPDATE money_user SET (in_jail, balance, last_claim) = (false, balance + "
	        + amount + ", NOW() + INTERVAL '" + delay + "') WHERE uid = " + uid + " RETURNING balance;");
	}
	
	private static long setJailTime(long uid, String interval) {
		return executeBalanceQuery("UPDATE money_user SET (in_jail, last_claim) = (true, NOW() + INTERVAL '"
	        + interval + "') WHERE uid = " + uid + ";");
	}
	
	private static boolean addUser(long uid, String name) {
		boolean error = false;
		String query = "INSERT INTO money_user (uid, name, balance) VALUES(" + uid + ", '" + name +"', 1000) ON CONFLICT (uid) DO NOTHING;";
		String job = "INSERT INTO job_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
		String slots = "INSERT INTO slots_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
		String guess = "INSERT INTO guess_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
		String minislots = "INSERT INTO minislots_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
		int inserted = 0;
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
		String query = "SELECT name, balance FROM money_user ORDER BY balance DESC LIMIT 3;";
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
            	String name = results.getString(1);
            	if (name.contains("#")) {
            		name = name.substring(0, name.indexOf('#'));
            	}
            	leaderboard += name + " " + results.getLong(2) + "\n";
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
	
	private static void guessWin(long uid, int spent, int winnings) {
		executeUpdate("UPDATE guess_user SET (guesses, correct, spent, winnings) = (guesses + 1, correct + 1, spent + "
	        + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
	}
	
	private static void guessClose(long uid, int spent, int winnings) {
		executeUpdate("UPDATE guess_user SET (guesses, close, spent, winnings) = (guesses + 1, close + 1, spent + "
	        + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
	}
	
	private static void guessLoss(long uid, int spent) {
		executeUpdate("UPDATE guess_user SET (guesses, spent) = (guesses + 1, spent + "
	        + spent + ") WHERE uid = " + uid + ";");
	}
	
	private static void guessMistake(long uid, int spent, int winnings) {
		executeUpdate("UPDATE guess_user SET (guesses, mistake, spent, winnings) = (guesses + 1, mistake + 1, spent + "
	        + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
	}
	
	private static void logSlots(long uid, int spent, int winnings, int diamonds) {
		executeUpdate("UPDATE slots_user SET (pulls, diamonds, spent, winnings) = (pulls + 1, diamonds + "
	        + diamonds + ", spent + " + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
	}
	
	private static void logMinislots(long uid, int spent, int winnings, int diamonds) {
		executeUpdate("UPDATE minislots_user SET (pulls, diamonds, spent, winnings) = (pulls + 1, diamonds + "
	        + diamonds + ", spent + " + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
	}
	
	private static User getUser(long uid) {
		String query = "SELECT work_count, fish_count, pick_count, rob_count, balance, in_jail, last_claim FROM money_user NATURAL JOIN job_user WHERE uid = " + uid + ";";
		Connection connection = null;
        Statement statement = null;
        User user = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
            	int work = results.getInt(1);
            	int fish = results.getInt(2);
            	int pick = results.getInt(3);
            	int rob = results.getInt(4);
            	long balance = results.getLong(5);
            	boolean isJail = results.getBoolean(6);
            	Timestamp time = results.getTimestamp(7);
            	user = new Casino.User(work, fish, pick, rob, balance, isJail, time);
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
        return user;
	}
	
	private static long logWork(long uid, int income) {
		long balance = addWorkMoney(uid, income, "2 hours");
		executeUpdate("UPDATE job_user SET (work_count, work_profit) = (work_count + 1, "
	        + "work_profit + " + income + ") WHERE uid = " + uid + ";");
		return balance;
	}
	
	private static long logFish(long uid, boolean rare, int income) {
		long balance = addWorkMoney(uid, income, "30 minutes");
		executeUpdate("UPDATE job_user SET (fish_count, fish_jackpots, fish_profit) = (fish_count + 1, fish_jackpots + " 
	        + (rare ? 1 : 0) + ", fish_profit + " + income + ") WHERE uid = " + uid + ";");
		return balance;
	}
	
	private static void pickFailed(long uid) {
		setJailTime(uid, "30 minutes");
		executeUpdate("UPDATE job_user SET (pick_count, pick_fails, jail_time) = (pick_count + 1, pick_fails + 1, jail_time + 30) WHERE uid = "
	        + uid + ";");
	}
	
	private static long logPick(long uid, boolean rare, int income) {
		long balance = addMoney(uid, income);
		executeBalanceQuery("UPDATE job_user SET (pick_count, pick_jackpots, pick_profit) = (pick_count + 1, pick_jackpots + "
	        + (rare ? 1 : 0) + ", pick_profit + " + income + ") WHERE uid = " + uid + ";");
		return balance;
	}
	
	private static void robFailed(long uid) {
		setJailTime(uid, "2 hours");
		executeUpdate("UPDATE job_user SET (pick_count, rob_fails, jail_time) = (pick_count + 1, rob_fails + 1, jail_time + 120) WHERE uid = "
		        + uid + ";");
	}
	
	private static long logRob(long uid, boolean rare, int income) {
		long balance = addMoney(uid, income);
		executeBalanceQuery("UPDATE job_user SET (rob_count, rob_jackpots, rob_profit) = (rob_count + 1, rob_jackpots + "
	        + (rare ? 1 : 0) + ", rob_profit + " + income + ") WHERE uid = " + uid + ";");
		return balance;
	}
	
	private static void executeUpdate(String query) {
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
	
	private static long executeBalanceQuery(String query) {
		Connection connection = null;
        Statement statement = null;
        long balance = 0;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
            	balance = results.getLong(1);
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

}
