package com.ct2ts.hb;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;

import java.util.HashMap;
import java.util.Map;

public class HBMain {

    public static void main(String[] args) {
        Map<String, Command> commands = new HashMap();
        commands.put("ping", event -> event.getMessage()
                .getChannel().block().createMessage("35ms").block());
        final DiscordClient client = new DiscordClientBuilder(args[0]).build();
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    final String content = event.getMessage().getContent().orElse("");
                    for (final Map.Entry<String, Command> entry : commands.entrySet()) {
                        if (content.startsWith('+' + entry.getKey())) {
                            entry.getValue().execute(event);
                            break;
                        }
                    }
                });
        client.login().block();
    }

    interface Command {
        void execute(MessageCreateEvent event);
    }
}
