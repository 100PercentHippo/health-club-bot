package com.c2t2s.hb;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.entity.user.User;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.NoSuchElementException;
import java.lang.Thread;

public class HBMain {

    private static final String version = "1.3.2"; //Update this in pom.xml too
    private static final char commandPrefix = '+';
    private static HashMap<String, Command> commands = new HashMap<>();

    public static void main(String[] args) {
        commands.put("help", HBMain::handleHelp);
        commands.put("test", HBMain::handleTest);
        commands.put("roll", HBMain::handleRoll);
        commands.put("r", HBMain::handleRoll);
        commands.put("ver", HBMain::handleVersion);
        commands.put("version", HBMain::handleVersion);
        commands.put("claim", HBMain::handleClaim);
        commands.put("balance", HBMain::handleBalance);
        commands.put("bal", HBMain::handleBalance);
        commands.put("pay", HBMain::handleGive);
        commands.put("give", HBMain::handleGive);
        commands.put("rob", HBMain::handleRob);
        commands.put("robert", HBMain::handleRob);
        commands.put("workout", HBMain::handleWorkout);
        commands.put("leaderboard", HBMain::handleLeaderboard);
        commands.put("leaderboards", HBMain::handleLeaderboard);
        commands.put("guess", HBMain::handleGuess);
        commands.put("bigguess", HBMain::handleBigGuess);
        commands.put("hugeguess", HBMain::handleHugeGuess);
        commands.put("slots", HBMain::handleSlots);
        commands.put("pickpocket", HBMain::handlePickpocket);
        commands.put("pick", HBMain::handlePickpocket);
        commands.put("work", HBMain::handleWork);
        commands.put("fish", HBMain::handleFish);
        commands.put("minislots", HBMain::handleMinislots);
        commands.put("pot", HBMain::handlePot);
        commands.put("feed", HBMain::handleFeed);
        commands.put("moneymachine", HBMain::handleFeed);
        commands.put("amogus", HBMain::handleAmogus);
        commands.put("overunder", HBMain::handleOverUnder);
        commands.put("over", HBMain::handleOver);
        commands.put("under", HBMain::handleUnder);
        commands.put("same", HBMain::handleSame);
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
                + "\n\t+claim Register with the casino if you're new or get a refresher of main commands"
                + "\n\t+balance Check your balance"
                + "\n\t+leaderboard Check who's the richest"
                + "\n\t+roll [number] Roll a number up to the inputted max"
                + "\nIncome commands:"
                + "\n\t+work Work for 2 hours to earn some coins"
                + "\n\t+fish Fish for 30 minutes to earn some coins"
                + "\n\t+rob Attempt to rob The Bank to steal some of The Money, you might be caught!"
                + "\n\t+pickpocket Attempt a petty theft of pickpocketting"
                + "\n\t+give <amount> <@User> Gives money to another person"
                + "\nGambling commands:"
                + "\n\t+guess <guess> <amount> Guess a number from 1 to 10, win coins if correct"
                + "\n\t+bigguess <guess> <amount> Guess a number from 1 to 10, no consolation prizes"
                + "\n\t+hugeguess <guess> <amount> Guess a number from 1 to 100!"
                + "\n\t+slots <bid> Roll the slots with that much as wager. Default wager is 10"
                + "\n\t+minislots <bid> Roll the minislots with that much as wager. Default 5"
    	        + "\n\t+moneymachine <amount> Feed the money machine"
    	        + "\n\t+pot Check the current money machine pot"
    	        + "\n\t+overunder Multiple rounds of predicting if the next number is over or under"
    	        + "\n\t\tPlace predictions with +over, +under, or +same");
    }

    private static void handleVersion(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(version);
    }

    private static void handleTest(MessageCreateEvent event, String args) {
    	if (!event.getMessageAuthor().getDiscriminatedName().contains("Hippo")) {
        	event.getChannel().sendMessage("Hi! I'm Health Bot");
        	return;
    	}
    	Random random = new Random();
    	int zero = 0, one = 0, two = 0, three = 0;
    	for (int i = 0; i < 100; ++i) {
    	int target = random.nextInt(10) + 1;
    	if ((target < 6 && (target = random.nextInt() + 1) > 5) || (target = random.nextInt() + 1) < 6) {
    		if ((target < 6 && (target = random.nextInt() + 1) > 5) || (target = random.nextInt() + 1) < 6) {
    			if ((target < 6 && (target = random.nextInt() + 1) > 5) || (target = random.nextInt() + 1) < 6) {
    				three++;
    			} else {
    				two++;
    			}
    		} else {
    			one++;
    		}
    	} else {
    		zero++;
    	}
    	}
    	event.getChannel().sendMessage("0:" + zero + " 1:" + one + " 2:" + two + " 3:" + three);
    }

    private static void handleWorkout(MessageCreateEvent event, String args) {
        //event.getMember().ifPresent(member -> {
        //    String response = Casino.handleWorkout(member);
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
    	event.getChannel().sendMessage(Casino.handleClaim(event.getMessageAuthor().getId(), event.getMessageAuthor().getDiscriminatedName()));
    }
    
    private static void handleRob(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Casino.handleRob(event.getMessageAuthor().getId()));
    }
    
    private static void handlePickpocket(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Casino.handlePickpocket(event.getMessageAuthor().getId()));
    }
    
    private static void handleBalance(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Casino.handleBalance(event.getMessageAuthor().getId()));
    }
    
    private static void handleWork(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Casino.handleWork(event.getMessageAuthor().getId()));
    }
    
    private static void handleFish(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Casino.handleFish(event.getMessageAuthor().getId()));
    }
    
    private static void handleGive(MessageCreateEvent event, String args) {
		String response = "";
		int amount = 0;
    	if (!args.contains(" ")) {
    		response = "Unable to process transaction, not enough arguments. Sample usage:\n\t`+give 100 @100% Hippo` (you will need to ping the user)";
    	} else {
		    String firstArg = args.substring(0, args.indexOf(' '));
    		try {
    		    amount = Integer.parseInt(firstArg);
    		    List<User> mentions = event.getMessage().getMentionedUsers();
    	    	if (mentions.isEmpty()) {
    	    		response = "Unable to process transaction, no users were mentioned! Sample usage:\n\t`+give 100 @100% Hippo` (you will need to ping the user)";
    	    	} else {
    	    		long recepientUid = mentions.get(0).getId();
    	    		response = Casino.handleGive(event.getMessageAuthor().getId(), recepientUid, amount);
    	    	}
    		} catch (NumberFormatException e) {
    			response = "Unable to parse amount \"" + firstArg + "\". Sample usage:\n\t`+give 100 @100% Hippo` (you will need to ping the user)";
    		}
    	}
    	event.getChannel().sendMessage(response);
    }
    
    private static void handleLeaderboard(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Casino.handleLeaderboard());
    }
    
    public static void handleGuess(MessageCreateEvent event, String args) {
    	String response = "";
    	try {
    		if (!args.contains(" ")) {
    			response = "Not enough arguments to guess. Sample usage: `+guess <guess> <amount>` `+guess 5 10`";
    	   	} else {
    	    	int guess = Integer.parseInt(args.substring(0, args.indexOf(' ')));
    	    	int amount = Integer.parseInt(args.substring(args.indexOf(' ')).trim());
    	     	if (guess < 1 || guess > 10) {
    		    	response = "Instead of " + guess + ", guess a number from 1 to 10";
    	    	} else if (amount < 1) {
    			    response = "Minimum bid for guessing is 1 coin";
    	    	} else {
    	    		response = Casino.handleGuess(event.getMessageAuthor().getId(), guess, amount);
    		    }
    		}
    	} catch (NumberFormatException e) {
    		response = "Unable to parse arguments \"" + args + "\". Sample usage: `+guess <guess> <amount>` `+guess 5 10`";
    	}
    	event.getChannel().sendMessage(response);
    }
    
    public static void handleBigGuess(MessageCreateEvent event, String args) {
    	String response = "";
    	try {
    		if (!args.contains(" ")) {
    			response = "Not enough arguments to guess. Sample usage: `+bigguess <guess> <amount>` `+bigguess 5 10`";
    	   	} else {
    	    	int guess = Integer.parseInt(args.substring(0, args.indexOf(' ')));
    	    	int amount = Integer.parseInt(args.substring(args.indexOf(' ')).trim());
    	     	if (guess < 1 || guess > 10) {
    		    	response = "Instead of " + guess + ", guess a number from 1 to 10";
    	    	} else if (amount < 1) {
    			    response = "Minimum bid for guessing is 1 coin";
    	    	} else {
    	    		response = Casino.handleBigGuess(event.getMessageAuthor().getId(), guess, amount);
    		    }
    		}
    	} catch (NumberFormatException e) {
    		response = "Unable to parse arguments \"" + args + "\". Sample usage: `+bigguess <guess> <amount>` `+bigguess 5 10`";
    	}
    	event.getChannel().sendMessage(response);
    }
    
    public static void handleHugeGuess(MessageCreateEvent event, String args) {
    	String response = "";
    	try {
    		if (!args.contains(" ")) {
    			response = "Not enough arguments to guess. Sample usage: `+hugeguess <guess> <amount>` `+hugeguess 5 10`";
    	   	} else {
    	    	int guess = Integer.parseInt(args.substring(0, args.indexOf(' ')));
    	    	int amount = Integer.parseInt(args.substring(args.indexOf(' ')).trim());
    	     	if (guess < 1 || guess > 100) {
    		    	response = "Instead of " + guess + ", guess a number from 1 to 100";
    	    	} else if (amount < 1) {
    			    response = "Minimum bid for guessing is 1 coin";
    	    	} else {
    	    		response = Casino.handleHugeGuess(event.getMessageAuthor().getId(), guess, amount);
    		    }
    		}
    	} catch (NumberFormatException e) {
    		response = "Unable to parse arguments \"" + args + "\". Sample usage: `+hugeguess <guess> <amount>` `+hugeguess 5 10`";
    	}
    	event.getChannel().sendMessage(response);
    }
    
    public static void handleSlots(MessageCreateEvent event, String args) {
    	String response = "";
    	if (args.trim().isEmpty()) {
    		response = Casino.handleSlots(event.getMessageAuthor().getId(), 10);
    		event.getChannel().sendMessage(response);
    	} else {
    		try {
        		int bid = Integer.parseInt(args.trim());
        		if (bid < 10) {
        			response = "Minimum bid for slots is 10 coins";
        	    	event.getChannel().sendMessage(response);
        		} else {
            	    response = Casino.handleSlots(event.getMessageAuthor().getId(), bid);
            	    event.getChannel().sendMessage(response);
        		}
        	} catch (NumberFormatException e) {
        		response = "Unable to parse argument \"" + args + "\". Sample usage: `+slots` or `+slots 20`";
            	event.getChannel().sendMessage(response);
        	}
    	}
    }
    
    public static void handleMinislots(MessageCreateEvent event, String args) {
    	String response = "";
    	if (args.trim().isEmpty()) {
    		response = Casino.handleMinislots(event.getMessageAuthor().getId(), 5);
    	} else {
    		try {
        		int bid = Integer.parseInt(args.trim());
        		if (bid < 5) {
        			response = "Minimum bid for mini slots is 5 coins";
        		} else {
            	    response = Casino.handleMinislots(event.getMessageAuthor().getId(), bid);
        		}
        	} catch (NumberFormatException e) {
        		response = "Unable to parse argument \"" + args + "\". Sample usage: `+minislots` or `+minislots 10`";
        	}
    	}
		event.getChannel().sendMessage(response);
    }
    
    public static void handlePot(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Casino.handlePot());
    }
    
    public static void handleFeed(MessageCreateEvent event, String args) {
    	String response = "";
    	if (args.trim().isEmpty()) {
    		response = "Expected an amount. Sample usage: `+moneymachine 100`";
    	} else {
    		try {
        		int bid = Integer.parseInt(args.trim());
        		if (bid < 1) {
        			response = "The money machine requires real sustenence of 1 or more coins";
        		} else {
            		response = Casino.handleFeed(event.getMessageAuthor().getId(), bid);
        		}
        	} catch (NumberFormatException e) {
        		response = "Unable to parse argument \"" + args + "\". Sample usage: `+moneymachine 100`";
        	}
    	}
    	event.getChannel().sendMessage(response);
    }
    
    public static void handleAmogus(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage("Due to your overwhelming sus-ness, your criminal rating has been maxed, and you have been sent to jail for 12 hours");
    }
    
    public static void handleOverUnder(MessageCreateEvent event, String args) {
    	String response = "";
    	if (args.trim().isEmpty()) {
    		response = Casino.handleOverUnderInitial(event.getMessageAuthor().getId(), 10);
    	} else {
    		try {
        		int bid = Integer.parseInt(args.trim());
        		if (bid < 5) {
        			response = "Minimum bid for overunder is 10 coins";
        		} else {
            	    response = Casino.handleOverUnderInitial(event.getMessageAuthor().getId(), bid);
        		}
        	} catch (NumberFormatException e) {
        		response = "Unable to parse argument \"" + args + "\". Sample usage: `+overunder` or `+overunder 10`";
        	}
    	}
		event.getChannel().sendMessage(response);
    }
    
    public static void handleOver(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(
    			Casino.handleOverUnderFollowup(event.getMessageAuthor().getId(), Casino.PREDICTION_OVER));
    }
    
    public static void handleUnder(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(
    			Casino.handleOverUnderFollowup(event.getMessageAuthor().getId(), Casino.PREDICTION_UNDER));
    }
    
    public static void handleSame(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(
    			Casino.handleOverUnderFollowup(event.getMessageAuthor().getId(), Casino.PREDICTION_SAME));
    }
}