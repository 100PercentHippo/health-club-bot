package com.c2t2s.hb;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Random;

public class HBMain {

    private static final String version = "0.5.1"; //Update this in pom.xml too
    private static final char commandPrefix = '+';
    private static HashMap<String, Command> commands = new HashMap<>();

    public static void main(String[] args) {
        commands.put("help", HBMain::handleHelp);
        commands.put("test", HBMain::handleTest);
        commands.put("workout", HBMain::handleWorkout);
        commands.put("roll", HBMain::handleRoll);
        commands.put("r", HBMain::handleRoll);
        commands.put("version", HBMain::handleVersion);
        final DiscordClient client = new DiscordClientBuilder(args[0]).build();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(HBMain::HandleMessage);
        client.login().block();
    }

    interface Command {
        void execute(MessageCreateEvent event, String args);
    }

    private static void HandleMessage(MessageCreateEvent event) {
        String content = event.getMessage().getContent().orElse("").toLowerCase();
        if (!content.isEmpty() && content.charAt(0) == commandPrefix) {
            content = content.substring(1); //Remove the prefix character
            String[] args = content.split(" ", 2);
            if (commands.containsKey(args[0])) {
                commands.get(args[0]).execute(event, args.length > 1 ? args[1] : "");
            } else { //Received command not present in command map
                event.getMessage().getChannel().block()
                        .createMessage("Unrecognized command, try `+help`").block();
            }
        } else if (content.contains("i love health bot")) {
            event.getMessage().getChannel().block()
                    .createMessage(":heart:").block();
        }
    }

    private static void handleHelp(MessageCreateEvent event, String args) {
        event.getMessage().getChannel().block()
                .createMessage("Health Bot Version " + version
                + "\nCommands:"
                + "\n\t+help Displays this help text"
                + "\n\t+version Display the bot's current version"
                + "\n\t+workout Report that you've completed a workout"
                + "\n\t+roll [number] Roll a number up to the inputted max"
                + "\n\t+test Placeholder test command").block();
    }

    private static void handleVersion(MessageCreateEvent event, String args) {
        event.getMessage().getChannel().block().createMessage(version).block();
    }

    private static void handleTest(MessageCreateEvent event, String args) {
        event.getMessage().getChannel().block().createMessage("Hi! I'm Health Bot").block();
    }

    private static void handleWorkout(MessageCreateEvent event, String args) {
        if (!DBConnection.initialized) {
            DBConnection.initialize(event);
        }
        event.getMember().ifPresent(member -> {
            String response = DBConnection.handleWorkout(member);
            event.getMessage().getChannel().block().createMessage(response).block();
        });
    }

    //TODO: Handle negative modifiers in dice rolls
    private static void handleRoll(MessageCreateEvent event, String args) {
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
                event.getMessage().getChannel().block().createMessage(message + "\n`" + total + "`").block();
            } else {
                // Deathrolling
                max = Integer.parseInt(args);
                if (max < 0) {
                    event.getMessage().getChannel().block().createMessage("Negative numbers make me sad :slight_frown:").block();
                    return;
                }
                int roll = random.nextInt(max) + 1;;
                String oneText = (roll == 1) ? "\nIt's been a pleasure doing business with you :slight_smile: :moneybag:" : "";
                event.getMessage().getChannel().block().createMessage("" + roll + oneText).block();
            }
        } catch (NumberFormatException e) {
            // Unrecognized syntax
            event.getMessage().getChannel().block().createMessage("Unrecognized roll syntax. Try `+roll 3` or `+roll 2d6`").block();
            return;
        }
    }
}
