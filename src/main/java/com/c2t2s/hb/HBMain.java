package com.c2t2s.hb;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.entity.user.User;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

public class HBMain {

    private static final String version = "2.0.3"; //Update this in pom.xml too

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("API key is required as first argument");
            return;
        }
        DiscordApi api = new DiscordApiBuilder().setToken(args[0]).login().join();
        if (args.length > 1 && args[1].equalsIgnoreCase("init")) {
        	initCommands(api);
        }
        api.addSlashCommandCreateListener(event -> {
            SlashCommandInteraction interaction = event.getSlashCommandInteraction();
            switch (interaction.getCommandName()) {
                case "version":
                    interaction.createImmediateResponder().setContent(version).respond();
                    break;
                case "help":
                    interaction.createImmediateResponder().setContent(getHelpText()).respond();
                    break;
                case "changelog":
                    interaction.createImmediateResponder().setContent(getChangelog()).respond();
                    break;
                case "roll":
                    interaction.createImmediateResponder().setContent(handleRoll(interaction.getArgumentStringValueByIndex(0).get())).respond();
                    break;
                case "guess":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleGuess(interaction.getUser().getId(),
                            interaction.getArgumentLongValueByIndex(0).get(),
                            interaction.getArgumentLongValueByIndex(1).orElse(10L))).respond();
                    break;
                case "hugeguess":
                    interaction.createImmediateResponder().setContent(
                        Casino.handleHugeGuess(interaction.getUser().getId(),
                            interaction.getArgumentLongValueByIndex(0).get(),
                            interaction.getArgumentLongValueByIndex(1).orElse(10L))).respond();
                    break;
            }
        });
    }
    
    private static void initCommands(DiscordApi api) {
        System.out.println("Registering commands with discord");
        //SlashCommand.with("version", "Check the current bot version").createGlobal(api).join();
        SlashCommand.with("help", "Print available Casino Bot commands").createGlobal(api).join();
        SlashCommand.with("changelog", "Print recent Casino Bot changelog").createGlobal(api).join();
        SlashCommand.with("roll", "Roll a random number. Supports deathrolling (`/roll 10`) or RPG style dice (`/roll 1d20`)",
            Arrays.asList(SlashCommandOption.createStringOption("argument", "What to roll. Either a number (`100`) or an RPG style sequence (`1d20`)", true)))
            .createGlobal(api).join();
        //SlashCommand.with("guess", "Guess a number from 1 to 10!",
        //    Arrays.asList(SlashCommandOption.createLongOption("guess", "What you think the number will be", true, 1, 10),
        //        SlashCommandOption.createLongOption("wager", "Amount to wager, default 10", false, 1, 100000)))
        //    .createGlobal(api).join();
        SlashCommand.with("hugeguess", "Guess a number from 1 to 100!",
            Arrays.asList(SlashCommandOption.createLongOption("guess", "What you think the number will be", true, 1, 100),
                SlashCommandOption.createLongOption("wager", "Amount to wager, default 10", false, 1, 100000)))
            .createGlobal(api).join();
        System.out.println("Command registration complete");
    }

    private static String getHelpText() {
        return "Casino Bot Verstion " + version
            + "\nCommands:"
            + "\n\t`/help` Displays this help text"
            + "\n\t`/changelog` View recent changes to the bot"
            + "\n\t`/roll` Roll a random number."
            + "\n\t\tEither deathrolling (e.g. `100`) or RPG style dice (e.g. `1d20`)"
            + "\n\t`/guess` Guess a number from 1 to 10"
            + "\n\t`/hugeguess` Guess a number from 1 to 100";
    }

    private static String getChangelog() {
        return "2.0.3"
            + "\n\t- Added `/changelog`. Readded `/help`, `/roll`, and `/hugeguess`"
            + "\n2.0.2"
            + "\n\t- Fixed bot not responding to guesses with default wagers"
            + "\n2.0.1"
            + "\n\t- Readded `/guess`"
            + "\n2.0.0"
            + "\n\t- Bot is back! Added one sample command";
    }

    //TODO: Handle negative modifiers in dice rolls
    private static String handleRoll(String args) {
        int max = 0;
        Random random = new Random();
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
                    for (int j = 0; j < numDice; ++j) {
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
                return message + "\n`" + total + "`";
            } else {
                // Deathrolling
                max = Integer.parseInt(args);
                if (max < 1) {
                	return "Negative numbers make me sad :slight_frown:";
                }
                int roll = random.nextInt(max) + 1;
                return "" + roll + (roll == 1 ? "\nIt's been a pleasure doing business with you :slight_smile: :moneybag:" : "");
            }
        } catch (NumberFormatException e) {
            // Unrecognized syntax
        	return "Unrecognized roll syntax. Try `/roll 3` or `/roll 2d6`";
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Old code before slash commands
    ///////////////////////////////////////////////////////////////////////////

    //  private static void handleMessage(MessageCreateEvent event) {
    // 	String content = event.getMessageContent();
    // 	if (!content.isEmpty() && content.charAt(0) == commandPrefix) {
    //         content = content.substring(1); //Remove the prefix character
    //         String[] args = content.split(" ", 2);
    //         args[0] = args[0].toLowerCase();
    //         if (commands.containsKey(args[0])) {
    //             commands.get(args[0]).execute(event, args.length > 1 ? args[1] : "");
    //         } else { //Received command not present in command map
    //         	event.getChannel().sendMessage("Unrecognized command, try `+help`");
    //         }
    //     } else if (content.contains("i love health bot")) {
    //     	event.getChannel().sendMessage(":heart:");
    //     }
    // }

    private static void handleHelp(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage("Health Bot Version " + version
                + "\nCommands:"
                + "\n\t+help Displays this help text"
                + "\n\t+version Display the bot's current version"
                + "\n\t+claim Register with the casino if you're new or get a refresher of main commands"
                + "\n\t+balance Check your balance"
                + "\n\t+leaderboard [entries to show] Check who's the richest"
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
    	        + "\n\t\tPlace predictions with +over, +under, or +same"
    	        + "\n\t+blackjack <amount> Start a new game of blackjack, played with +hit or +stand");
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
    	String response = "";
    	try {
    		if (!args.contains(" ")) {
    	    	response = Casino.handleLeaderboard(3);
    		} else {
    			int entries = Integer.parseInt(args.substring(args.indexOf(' ')).trim());
    			if (entries < 1) {
    				response = "Minimum entries to show must be 1 or greater";
    			} else if (entries > 10) {
    				response = "Maximum leaderboard entries is 10.";
    			} else {
    				response = Casino.handleLeaderboard(entries);
    			}
    		}
    	} catch (NumberFormatException e) {
    		response = "Unable to parse argument \"" + args + "\". Sample usage: `+leaderboard [entries]` `+leaderboard` `+leaderboard 3`";
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
        			response = "The money machine requires real sustenance of 1 or more coins";
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
        			response = "Minimum bid for overunder is 5 coins";
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
    
    public static void handleBlackjack(MessageCreateEvent event, String args) {
    	String response = "";
    	if (args.trim().isEmpty()) {
    		response = Blackjack.handleBlackjack(event.getMessageAuthor().getId(), 10);
    	} else {
    		try {
        		int bid = Integer.parseInt(args.trim());
        		if (bid < 10) {
        			response = "Minimum bid for blackjack is 10 coins";
        		} else {
            	    response = Blackjack.handleBlackjack(event.getMessageAuthor().getId(), bid);
        		}
        	} catch (NumberFormatException e) {
        		response = "Unable to parse argument \"" + args + "\". Sample usage: `+blackjack` or `+blackjack 10`";
        	}
    	}
		event.getChannel().sendMessage(response);
    }
    
    public static void handleHit(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Blackjack.handleHit(event.getMessageAuthor().getId()));
    }
    
    public static void handleStand(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Blackjack.handleStand(event.getMessageAuthor().getId()));
    }
    
    public static void handleCreateWager(MessageCreateEvent event, String args) {
    	String[] splitArgs = args.split("\\|+");
    	String response = "";
    	if (splitArgs.length < 3) {
    		response = "Not enough arguments provided. Sample usage `+createwager Title|Option A|Option B|...`";
    	} else if (splitArgs.length > 11) {
    		response = "Too many options provided. Wagers can have a maximum of 10 options";
    	} else {
    		List<String> options = new ArrayList<String>(Arrays.asList(splitArgs));
    		options.remove(0);
    		response = Wagers.createWager(event.getMessageAuthor().getId(), splitArgs[0], options);
    	}
    	event.getChannel().sendMessage(response);
    }
    
    public static void handleCloseWager(MessageCreateEvent event, String args) {
    	handleSetWagerClosed(event, args, true);
    }
    
    public static void handleOpenWager(MessageCreateEvent event, String args) {
    	handleSetWagerClosed(event, args, false);
    }
    
    private static void handleSetWagerClosed(MessageCreateEvent event, String args, boolean isClosed) {
    	String response = "";
    	try {
    		int id = Integer.parseInt(args.trim());
    		if (id < 0) {
    			response = "Wager id must not be negative";
    		} else {
        	    response = Wagers.setClosed(event.getMessageAuthor().getId(), id, isClosed);
    		}
    	} catch (NumberFormatException e) {
    		response = "Unable to parse argument \"" + args + "\". Sample usage: `+closewager 22` or `+openwager 22`";
    	}
    	event.getChannel().sendMessage(response);
    }
    
    public static void handlePayoutWager(MessageCreateEvent event, String args) {
    	String[] splitArgs = args.trim().split(" ");
    	String response = "";
    	if (splitArgs.length < 2) {
    		response = "Not enough arguments provided. Sample usage: `+payoutwager <wager id> <correct option>` `+payoutwager 22 3`";
    	} else {
    		boolean secondArg = false;
        	try {
        		int id = Integer.parseInt(splitArgs[0]);
        		secondArg = true;
        		int correct = Integer.parseInt(splitArgs[1]);
        		if (id < 0) {
        			response = "Wager id must not be negative";
        		} else if (correct < 0 || correct > 10) {
        			response = "Correct response must be between 1 and 10";
        		} else {
        			response = Wagers.payoutWager(event.getMessageAuthor().getId(), id, correct);
        		}
        	} catch (NumberFormatException e) {
        		response = "Unable to parse argument " + (secondArg ? "2" : "1")
        			+ " \"" + splitArgs[(secondArg ? 1 : 0)]
        			+ "\". Sample usage: `+payoutwager <wager id> <correct option>` `+payoutwager 22 3`";
        	}
    	}
    	event.getChannel().sendMessage(response);
    }
    
    public static void handlePlaceBet(MessageCreateEvent event, String args) {
    	String[] splitArgs = args.trim().split(" ");
    	String response = "";
    	if (splitArgs.length < 3) {
    		response = "Not enough arguments provided. Sample usage: `+bet <amount> <wager id> <option>` `+bet 100 22 3`";
    	} else {
    		int current = 0;
        	try {
        		long amount = Long.parseLong(splitArgs[current++]);
        		int id = Integer.parseInt(splitArgs[current++]);
        		int option = Integer.parseInt(splitArgs[current++]);
        		if (id < 0) {
        			response = "Wager id must not be negative";
        		} else if (option < 0 || option > 10) {
        			response = "Correct response must be between 1 and 10";
        		} else {
        			response = Wagers.placeBet(event.getMessageAuthor().getId(), id, option, amount);
        		}
        	} catch (NumberFormatException e) {
        		response = "Unable to parse arguments \"" + args + "\". Sample usage: `+bet <amount> <wager id> <option>` `+bet 100 22 3`";
        	}
    	}
    	event.getChannel().sendMessage(response);
    }
    
    public static void handleWagerInfo(MessageCreateEvent event, String args) {
    	String response = "";
    	if (args.trim().isEmpty()) {
    		response = "Not enough arguments provided. Sample usage `+wagerinfo <id>` `+wagerinfo 22`";
    	} else {
    		try {
        		int id = Integer.parseInt(args.trim());
        		if (id < 0) {
        			response = "Wager id must not be negative";
        		} else {
            	    response = Wagers.getWagerInfo(id);
        		}
        	} catch (NumberFormatException e) {
        		response = "Unable to parse argument \"" + args + "\". Sample usage: `+wagerinfo <id>` `+wagerinfo 22`";
        	}
    	}
		event.getChannel().sendMessage(response);
    }
    
    public static void handleOpenWagers(MessageCreateEvent event, String args) {
    	event.getChannel().sendMessage(Wagers.openWagers());
    }
}
