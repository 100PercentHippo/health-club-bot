package com.c2t2s.hb;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;

import java.util.HashMap;
import java.util.Random;

public class HBMain {

    private static final String version = "0.4.1"; //Update this in pom.xml too
    private static final char commandPrefix = '+';
    private static HashMap<String, Command> commands = new HashMap<>();

    public static void main(String[] args) {
        commands.put("help", HBMain::handleHelp);
        commands.put("test", HBMain::handleTest);
        commands.put("workout", HBMain::handleWorkout);
        commands.put("roll", HBMain::handleRoll);
        DBConnection.initialize();
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
                + "\n\t+workout Report that you've completed a workout"
                + "\n\t+test Placeholder test command").block();
    }

    private static void handleTest(MessageCreateEvent event, String args) {
        event.getMessage().getChannel().block().createMessage("Hi! I'm Health Bot").block();
    }

    private static void handleWorkout(MessageCreateEvent event, String args) {
        event.getMember().ifPresent(member -> {
            String response = DBConnection.handleWorkout(member.getId());
            event.getMessage().getChannel().block().createMessage(response).block();
        });
    }

    private static void handleRoll(MessageCreateEvent event, String args) {
        int max = Integer.parseInt(args);
        Random random = new Random();
        int roll = random.nextInt(max - 1) + 1;
        event.getMessage().getChannel().block().createMessage("" + roll).block();
    }
}
