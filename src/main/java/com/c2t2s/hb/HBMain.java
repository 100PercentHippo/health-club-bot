package com.c2t2s.hb;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.NoSuchElementException;

public class HBMain {

    private static final String version = "0.10.2"; //Update this in pom.xml too
    private static final char commandPrefix = '+';
    private static HashMap<String, Command> commands = new HashMap<>();
    private static Server server;

    public static void main(String[] args) {
        commands.put("help", HBMain::handleHelp);
        commands.put("test", HBMain::handleTest);
        commands.put("roll", HBMain::handleRoll);
        commands.put("r", HBMain::handleRoll);
        commands.put("version", HBMain::handleVersion);
        commands.put("claim", HBMain::handleClaim);
        commands.put("balance", HBMain::handleBalance);
        commands.put("bal", HBMain::handleBalance);
        commands.put("give", HBMain::handleGive);
        commands.put("rob", HBMain::handleRob);
        commands.put("workout", HBMain::handleWorkout);
        commands.put("leaderboard", HBMain::handleLeaderboard);
        commands.put("guess", HBMain::handleGuess);
        commands.put("slots", HBMain::handleSlots);
        DiscordApi api = new DiscordApiBuilder().setToken(args[0]).login().join();
        api.addMessageCreateListener(HBMain::handleMessage);
    }

    interface Command {
        void execute(MessageCreateEvent event, String args);
    }

    private static void handleMessage(MessageCreateEvent event) {
    	String content = event.getMessageContent().toLowerCase();
    	if (!content.isEmpty() && content.charAt(0) == commandPrefix) {
            content = content.substring(1); //Remove the prefix character
            String[] args = content.split(" ", 2);
            if (commands.containsKey(args[0])) {
                commands.get(args[0]).execute(event, args.length > 1 ? args[1] : "");
            } else { //Received command not present in command map
            	event.getChannel().sendMessage("Unrecognized command, try `+help`");
            }
        } else if (content.contains("i love health bot")) {
        	event.getChannel().sendMessage(":heart:");
        }
    }

    private static void handleHelp(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage("Health Bot Version " + version
                + "\nCommands:"
                + "\n\t+help Displays this help text"
                + "\n\t+version Display the bot's current version"
                + "\n\t+claim Claim coins!"
                + "\n\t+balance Check your balance"
                + "\n\t+rob Attempt to rob The Bank to steal some of The Money, you might be caught!"
                + "\n\t+guess <guess> <amount> Guess a number from 1 to 10, win coins if correct"
                + "\n\t+slots <bid> Roll the slots with that much as wager. Default wager is 10"
                + "\n\t+give <amount> <@User> Gives money to another person"
                + "\n\t+leaderboard Check who's the richest"
                + "\n\t+roll [number] Roll a number up to the inputted max");
    }

    private static void handleVersion(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(version);
    }

    private static void handleTest(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage("Hi! I'm Health Bot");
    }

    private static void handleWorkout(MessageCreateEvent event, String args) {
        //event.getMember().ifPresent(member -> {
        //    String response = DBConnection.handleWorkout(member);
        //    event.getMessage().getChannel().block().createMessage(response).block();
        //});
    	event.getChannel().sendMessage("Nice job! I'll start tracking this again soon (tm). Running a casino is just more profitable");
    }

    //TODO: Handle negative modifiers in dice rolls
    private static void handleRoll(MessageCreateEvent event, String args) {
        int max = 0;
        Random random = new Random();
        String finalMessage = "";
        try {
            if (args.contains("d")) {
                //Dice rolling
                //args.replace("-\\s*-", "");
                args.replace("-", "+-");
                args.replace("\\s", "");
                String[] pieces = args.split("\\+");
                String message = "";
                int total = 0;
                for (int i = 0; i < pieces.length; ++i) {
                    boolean negative = false;
                    if (pieces[i].startsWith("-")) {
                        pieces[i] = pieces[i].substring(1);
                        negative = true;
                    }
                    if (!pieces[i].contains("d")) {
                        int roll = Integer.parseInt(pieces[i]);
                        message += ((negative ? " - " : " + ") + roll);
                        total += (negative ? -1 : 1) * roll;
                        continue;
                    }
                    String[] splitArgs = pieces[i].split("d");
                    // If a NumberFormatException occurs, pass it up, don't catch
                    int numDice = Integer.parseInt(splitArgs[0]);
                    int diceSize = Integer.parseInt(splitArgs[1]);
                    String text = "";
                    for (int j = 0; i < numDice; ++i) {
                        int roll = random.nextInt(diceSize) + 1;
                        total += (roll * (negative ? -1 : 1));
                        text += (negative ? " - " : " + ") + "`" + roll + "`";
                    }
                    text = text.substring(2, text.length());
                    if (message.length() != 0) {
                        message += (negative ? " - " : " + ");
                    }
                    message += text;
                }
                finalMessage = message + "\n`" + total + "`";
            } else {
                // Deathrolling
                max = Integer.parseInt(args);
                if (max < 0) {
                	finalMessage = "Negative numbers make me sad :slight_frown:";
                }
                int roll = random.nextInt(max) + 1;
                finalMessage = "" + roll + (roll == 1 ? "\nIt's been a pleasure doing business with you :slight_smile: :moneybag:" : "");
            }
        } catch (NumberFormatException e) {
            // Unrecognized syntax
        	finalMessage = "Unrecognized roll syntax. Try `+roll 3` or `+roll 2d6`";
        }
        event.getChannel().sendMessage(finalMessage);
    }
    
    private static void handleClaim(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(DBConnection.handleClaim(event.getMessageAuthor().getId()));
    }
    
    private static void handleRob(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(DBConnection.handleRob(event.getMessageAuthor().getId()));
    }
    
    private static void handleBalance(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(DBConnection.handleBalance(event.getMessageAuthor().getId()));
    }
    
    private static void handleGive(MessageCreateEvent event, String args) {
		String response = "";
		int amount = 0;
    	if (!args.contains(" ")) {
    		response = "Unable to process transaction, not enough arguments. Sample usage:\n\t+give 100 @100% Hippo (you will need to ping the user)";
    	} else {
		    String firstArg = args.substring(0, args.indexOf(' '));
    		try {
    		    amount = Integer.parseInt(firstArg);
    		    List<User> mentions = event.getMessage().getMentionedUsers();
    	    	if (mentions.isEmpty()) {
    	    		response = "Unable to process transaction, no users were mentioned! Sample usage:\n\t+give 100 @100% Hippo (you will need to ping the user)";
    	    	} else {
    	    		long recepientUid = mentions.get(0).getId();
    	    		response = DBConnection.handleGive(event.getMessageAuthor().getId(), recepientUid, amount);
    	    	}
    		} catch (NumberFormatException e) {
    			response = "Unable to parse amount \"" + firstArg + "\". Sample usage:\n\t+give 100 @100% Hippo (you will need to ping the user)";
    		}
    	}
    	event.getChannel().sendMessage(response);
    }
    
    private static void handleLeaderboard(MessageCreateEvent event, String args) {
    	try {
    	    server = event.getServer().get();
    	} catch (NoSuchElementException e) { }
    	System.out.println("Author: " + event.getMessageAuthor().getId() + " " + getUsername(event.getMessageAuthor().getId())
    	                   + "\nUser: " + event.getMessageAuthor().asUser().get().getId() + " " + getUsername(event.getMessageAuthor().asUser().get().getId()));
    	event.getChannel().sendMessage(DBConnection.handleLeaderboard());
    }
    
    public static void handleGuess(MessageCreateEvent event, String args) {
    	String response = "";
    	try {
    		if (!args.contains(" ")) {
    			response = "Not enough arguments to guess. Sample usage: +guess 5 100";
    	   	} else {
    	    	int guess = Integer.parseInt(args.substring(0, args.indexOf(' ')));
    	    	int amount = Integer.parseInt(args.substring(args.indexOf(' ')).trim());
    	     	if (guess < 1 || guess > 10) {
    		    	response = "Insead of " + guess + ", guess a number from 1 to 10";
    	    	} else if (amount < 10) {
    			    response = "Minimum bid for guessing is 10 coins";
    	    	} else {
    	    		response = DBConnection.handleGuess(event.getMessageAuthor().getId(), guess, amount);
    		    }
    		}
    	} catch (NumberFormatException e) {
    		response = "Unable to parse arguments \"" + args + "\". Sample usage: `+guess 5` or `+guess 5 100`";
    	}
    	event.getChannel().sendMessage(response);
    }
    
    public static String getUsername(long uid) {
    	try {
    		return server.getMemberById(uid).get().getNicknameMentionTag();
    	} catch (NoSuchElementException e) {
    		System.out.println("Server: " + server + ", uid: " + uid);
    		return "<@!" + uid + ">";
    	}
    }
    
    public static void handleSlots(MessageCreateEvent event, String args) {
    	String response = "";
    	if (args.trim().isEmpty()) {
    		response = DBConnection.handleSlots(event.getMessageAuthor().getId(), 10);
    	} else {
    		try {
        		int bid = Integer.parseInt(args.trim());
        		if (bid < 10) {
        			response = "Minimum bid for slots is 10 coins";
        		} else {
            	    response = DBConnection.handleSlots(event.getMessageAuthor().getId(), bid);
        		}
        	} catch (NumberFormatException e) {
        		response = "Unable to parse argument \"" + args + "\". Sample usage: `+slots` or `+slots 20`";
        	}
    	}
    	event.getChannel().sendMessage(response);
    }
}
