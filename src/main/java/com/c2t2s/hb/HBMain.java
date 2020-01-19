package com.c2t2s.hb;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;

import java.util.HashMap;

public class HBMain {

    private static final String version = "0.2.0"; //Update this in pom.xml too
    private static final char commandPrefix = '+';
    private static HashMap<String, Command> commands = new HashMap<>();

    public static void main(String[] args) {
        commands.put("help", HBMain::handleHelp);
        commands.put("test", HBMain::handleTest);
        final DiscordClient client = new DiscordClientBuilder(args[0]).build();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(HBMain::HandleMessage);
        client.login().block();
    }

    interface Command {
        void execute(MessageCreateEvent event, String args);
    }

    private static void HandleMessage(MessageCreateEvent event) {
        String content = event.getMessage().getContent().orElse("");
        if (!content.isEmpty() && content.charAt(0) == commandPrefix) {
            content = content.substring(1); //Remove the prefix character
            String[] args = content.split(" ", 2);
            if (commands.containsKey(args[0])) {
                commands.get(args[0]).execute(event, args.length > 1 ? args[1] : "");
            } else { //Received command not present in command map
                event.getMessage().getChannel().block()
                        .createMessage("Unrecognized command, try `+help`");
            }
        }
    }

    private static void handleHelp(MessageCreateEvent event, String args) {
        event.getMessage().getChannel().block()
                .createMessage("Health Bot Version " + version
                + "\nCommands:"
                + "\n\t+help Displays this help text"
                + "\n\t+test Placeholder test command");
    }

    private static void handleTest(MessageCreateEvent event, String args) {
        event.getMessage().getChannel().block().createMessage("Hi! I'm Health Bot").block();
    }
}
