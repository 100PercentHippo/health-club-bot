package com.c2t2s.hb;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.ServerTextChannel;

import com.c2t2s.hb.Event.EventType;
import com.c2t2s.hb.Gacha.GachaCharacter;
import com.c2t2s.hb.HBMain.AutocompleteStringOption;
import com.c2t2s.hb.HBMain.CasinoCommand;

public class CasinoServerManager {

    // Hide default constructor
    private CasinoServerManager() {}

    private static Set<Long> adminUsers = new HashSet<>();
    private static Map<Long, CasinoServer> servers = new HashMap<>();
    private static ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);

    private static class CasinoServer {
        private long serverId;
        private String serverName;
        private long moneyMachinePot;

        private ServerTextChannel eventChannel;
        private Set<Long> casinoChannelIds;
        private List<ServerTextChannel> casinioChannels;
        private Set<Long> users = new HashSet<>();

        private Event activeEvent;

        CasinoServer(long serverId, String serverName, ServerTextChannel eventChannel,
                Set<Long> casinoChannelIds, List<ServerTextChannel> casinoChannels, Set<Long> users,
                long moneyMachinePot) {
            this.serverId = serverId;
            this.serverName = serverName;
            this.eventChannel = eventChannel;
            this.casinoChannelIds = casinoChannelIds;
            this.casinioChannels = casinoChannels;
            this.users = users;
            this.moneyMachinePot = moneyMachinePot;

            initializeEvent();
        }

        long getMoneyMachinePot() { return moneyMachinePot; }
        void setMoneyMachinePot(long amount) { moneyMachinePot = amount; }

        boolean isEventChannel(long channelId) {
            return eventChannel != null && eventChannel.getId() == channelId;
        }

        List<Message> sendCasinoMessage(String message) {
            return sendCasinoMessage(message, false);
        }

        List<Message> sendCasinoMessage(String message, boolean skipReturn) {
            if (skipReturn) {
                for (ServerTextChannel channel : casinioChannels) {
                    channel.sendMessage(message);
                }
                return null;
            }

            List<Message> sentMessages = new ArrayList<>();
            for (ServerTextChannel channel : casinioChannels) {
                try {
                    sentMessages.add(channel.sendMessage(message).join());
                } catch (CancellationException | CompletionException e) {
                    e.printStackTrace();
                }
            }
            return sentMessages;
        }

        Message sendEventMessage(String message) {
            return sendEventMessage(message, false);
        }

        Message sendEventMessage(String message, boolean skipReturn) {
            if (skipReturn) {
                eventChannel.sendMessage(message);
                return null;
            }
            return eventChannel.sendMessage(message).join();
        }

        void initializeEvent() {
            if (eventChannel == null) { return; }
            activeEvent = Event.EventFactory.createEvent(serverId, EventType.FISH);
            schedule(activeEvent::initialize, Duration.ofSeconds(15));
        }

        void createNewEvent() {
            activeEvent = Event.EventFactory.createEvent(serverId,
                EventType.getNextEventType(activeEvent.getType()));
            activeEvent.initialize();
        }
    }

    static class FetchedServer {
        private long serverId;
        private String name;
        private long eventChannel;
        private long moneyMachinePot;

        FetchedServer(long serverId, String name, long eventChannel, long moneyMachinePot) {
            this.serverId = serverId;
            this.name = name;
            this.eventChannel = eventChannel;
            this.moneyMachinePot = moneyMachinePot;
        }
    }

    static void initialize(DiscordApi api) {
        adminUsers = fetchAdminUsers();
        for (FetchedServer fetchedServer : fetchCasinoServers()) {
            initializeServer(fetchedServer, api);
        }
    }

    static void initializeServer(FetchedServer fetchedServer, DiscordApi api) {
        ServerTextChannel eventChannel = null;
        if (fetchedServer.eventChannel > 0) {
            eventChannel = getTextChannel(api, fetchedServer.eventChannel);
        }
        Set<Long> casinoChannelIds = fetchCasinoChannels(fetchedServer.serverId);
        Set<Long> users = fetchCasinoUsers(fetchedServer.serverId);

        List<ServerTextChannel> casinoChannels = new ArrayList<>();
        for (long channelId : casinoChannelIds) {
            ServerTextChannel casinoChannel = getTextChannel(api, channelId);
            if (casinoChannel != null) {
                casinoChannels.add(casinoChannel);
            }
        }

        servers.put(fetchedServer.serverId, new CasinoServer(fetchedServer.serverId,
            fetchedServer.name, eventChannel, casinoChannelIds, casinoChannels, users,
            fetchedServer.moneyMachinePot));
    }

    static ServerTextChannel getTextChannel(DiscordApi api, long channelId) {
        Channel casinoChannel = api.getChannelById(channelId).orElse(null);
        if (casinoChannel == null) { return null; }
        return casinoChannel.asServerTextChannel().orElse(null);
    }

    static boolean isValid(long uid, CasinoCommand command, long server, long channel) {
        if (adminUsers.contains(uid)) {
            return true;
        }
        if (!servers.containsKey(server)) {
            return false;
        }
        CasinoServer casinoServer = servers.get(server);
        if (!casinoServer.users.contains(uid)) {
            addServerUser(uid, server);
        }
        return (command.isValidInCasinoChannels() && casinoServer.casinoChannelIds.contains(channel))
            || (command.isValidInGachaChannels() && casinoServer.isEventChannel(channel));
    }

    static void addServerUser(long uid, long server) {
        if (logAddServerUser(uid, server)) {
            System.out.println("Added user " + uid + " to server " + server);
            servers.get(server).users.add(uid);
        }
    }

    static boolean addServer(long server, String serverName, long uid, DiscordApi api) {
        FetchedServer newServer = logAddServer(server, serverName, uid);
        if (newServer == null) {
            return false;
        }
        initializeServer(newServer, api);
        return true;
    }

    static String handleRegisterCasinoChannel(long uid, long serverId, String serverName,
            Channel channel, DiscordApi api) {
        if (!servers.containsKey(serverId)) {
            boolean added = addServer(serverId, serverName, uid, api);
            if (!added) {
                return "Unable to add casino channel: DB Error adding this server";
            }
        }
        CasinoServer server = servers.get(serverId);

        if (server.casinoChannelIds.contains(channel.getId())) {
            return "Unable to add casino channel: Specified channel is already registered";
        }

        ServerTextChannel textChannel = channel.asServerTextChannel().orElse(null);
        if (textChannel == null) {
            return "Unable to add casino channel: Specified channel is not a server text channel";
        }

        if (!logAddCasinoChannel(serverId, textChannel.getId(), textChannel.getName(), uid)) {
            return "Unable to add casino channel: DB Error adding this channel";
        }

        server.casinioChannels.add(textChannel);
        server.casinoChannelIds.add(textChannel.getId());
        System.out.println("#" + textChannel.getName()
            + " registered as a casino channel for server " + serverName + " by " + uid);
        return "Channel registered";
    }

    static String handleRegisterEventChannel(long uid, long serverId, String serverName,
            Channel channel, DiscordApi api) {
        if (!servers.containsKey(serverId)) {
            boolean added = addServer(serverId, serverName, uid, api);
            if (!added) {
                return "Unable to add event channel: DB Error adding this server";
            }
        }

        ServerTextChannel textChannel = channel.asServerTextChannel().orElse(null);
        if (textChannel == null) {
            return "Unable to add event channel: Specified channel is not a server text channel";
        }

        CasinoServer server = servers.get(serverId);

        if (server.eventChannel != null) {
            if (server.eventChannel.getId() == textChannel.getId()) {
                return "Unable to add event channel: Specified channel is already registered";
            } else {
                return "Unable to add event channel: An event channel is already registered for this server (#"
                    + server.eventChannel.getName() + ")";
            }
        }

        if (!logSetEventChannel(server.serverId, textChannel.getId(), textChannel.getName())) {
            return "Unable to add event channel: DB Error adding this channel";
        }
        server.eventChannel = textChannel;
        System.out.println("#" + textChannel.getName() + " registered as the event channel for server "
            + serverName + " by " + uid);
        return "Channel registered";
    }

    static String handleDeregisterCasinoChannel(long uid, long serverId, Channel channel) {
        if (!servers.containsKey(serverId)) {
            return "Unable to deregister casino channel: Server is not registered";
        }

        ServerTextChannel textChannel = channel.asServerTextChannel().orElse(null);
        if (textChannel == null) {
            return "Unable to deregister casino channel: Specified channel is not a server text channel";
        }

        CasinoServer server = servers.get(serverId);

        if (!server.casinoChannelIds.contains(channel.getId())) {
            return "Unable to deregister casino channel: Specified channel is not registered";
        }

        if (!logRemoveCasinoChannel(serverId, channel.getId())) {
            return "Unable to deregister casino channel: DB Error removing channel";
        }
        server.casinioChannels.remove(textChannel);
        server.casinoChannelIds.remove(channel.getId());
        System.out.println("#" + textChannel.getName()
            + " deregistered as a casino channel for server " + server.serverName + " by " + uid);
        return "Channel deregistered";
    }

    static String handleDeregisterEventChannel(long uid, long serverId, Channel channel) {
        if (!servers.containsKey(serverId)) {
            return "Unable to deregister casino channel: Server is not registered";
        }

        ServerTextChannel textChannel = channel.asServerTextChannel().orElse(null);
        if (textChannel == null) {
            return "Unable to deregister casino channel: Specified channel is not a server text channel";
        }

        CasinoServer server = servers.get(serverId);

        if (server.eventChannel == null) {
            return "Unable to deregister event channel: No event channel is registered for this server";
        } else if (server.eventChannel.getId() != textChannel.getId()) {
            return "Unable to deregister event channel: Specified channel is not the event channel (event channel is #"
                + server.eventChannel.getName() + ")";
        }

        if (!logRemoveEventChannel(serverId)) {
            return "Unable to deregister event channel: DB Error removing event channel";
        }
        String channelName = server.eventChannel.getName();
        server.eventChannel = null;
        System.out.println("#" + channelName + " deregistered as event channel for server "
            + server.serverName + " by " + uid);
        return "Channel deregistered";
    }

    static String handleEventJoin(long server, long uid, String characterUniqueId,
            long selection) {
        if (!servers.containsKey(server)) {
            return "Unable to join event: Server is not registered";
        }

        if (servers.get(server).activeEvent == null) {
            return "Unable to join event: No active event in this server";
        }

        GachaCharacter character;
        try {
            character = GachaCharacter.fromUniqueId(uid, characterUniqueId);
        } catch (IllegalArgumentException e) {
            return "Unable to join event: " + e.getMessage();
        }

        return servers.get(server).activeEvent.handleUserJoin(uid, character, selection);
    }

    static List<HBMain.AutocompleteStringOption> handleEventCharacterAutocomplete(long server,
            long uid, String partialName) {
        if (!servers.containsKey(server)) {
            return new ArrayList<>();
        }
        if (servers.get(server).activeEvent == null) {
            return Gacha.getCharacters(uid, partialName);
        }
        return Gacha.getCharacters(uid, partialName, servers.get(server).activeEvent.getType());
    }

    static List<HBMain.AutocompleteIdOption> handleEventSelectionAutocomplete(long server) {
        if (!servers.containsKey(server) || servers.get(server).activeEvent == null) {
            return new ArrayList<>();
        }
        return servers.get(server).activeEvent.handleSelectionAutocomplete();
    }

    static long getMoneyMachinePot(long server) {
        if (!servers.containsKey(server)) {
            return -1;
        }
        return servers.get(server).getMoneyMachinePot();
    }

    static void setMoneyMachinePot(long server, long amount) {
        if (!servers.containsKey(server)) { return; }
        servers.get(server).setMoneyMachinePot(logSetMoneyMachinePot(server, amount));
    }

    static void increaseMoneyMachinePot(long server, long amount) {
        if (!servers.containsKey(server)) { return; }
        servers.get(server).setMoneyMachinePot(logIncreaseMoneyMachinePot(server, amount));
    }

    static void sendMessage(long server, String message) {
        if (!servers.containsKey(server)) { return; }
        servers.get(server).sendCasinoMessage(message);
    }

    static void sendEventMessage(long server, String message) {
        if (!servers.containsKey(server)) { return; }
        servers.get(server).sendEventMessage(message);
    }

    static void sendMultipartEventMessage(long server, Queue<String> messageParts) {
        if (!servers.containsKey(server)) { return; }
        Message message = servers.get(server).sendEventMessage(messageParts.poll(), false);
        timer.schedule(() -> updateMessage(message, messageParts), 1, TimeUnit.SECONDS);
    }

    static Void updateMessage(Message message, Queue<String> updatedContents) {
        message.edit(updatedContents.poll());
        if (!updatedContents.isEmpty()) {
            timer.schedule(() -> updateMessage(message, updatedContents), 1, TimeUnit.SECONDS);
        }
        return null;
    }

    static void schedule(Callable<Void> method, Duration timeUntil) {
        timer.schedule(method, timeUntil.toSeconds(), TimeUnit.SECONDS);
    }

    static void beginNewEvent(long server) {
        if (!servers.containsKey(server)) { return; }
        servers.get(server).createNewEvent();
    }

    static boolean hasEvent(long server) {
        return servers.containsKey(server) && servers.get(server).activeEvent != null;
    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS admin_user (
    //   uid bigint PRIMARY KEY
    // );

    // CREATE TABLE IF NOT EXISTS casino_server (
    //   server_id bigint PRIMARY KEY,
    //   name varchar(100) NOT NULL,
    //   event_channel bigint DEFAULT NULL,
    //   event_channel_name varchar(100) DEFAULT NULL,
    //   added_by bigint NOT NULL,
    //   money_machine_pot bigint NOT NULL DEFAULT 1000,
    //   CONSTRAINT casino_server_added_by FOREIGN KEY(added_by) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS casino_channel (
    //   channel_id bigint PRIMARY KEY,
    //   server_id bigint NOT NULL,
    //   name varchar(100) NOT NULL,
    //   added_by bigint NOT NULL,
    //   CONSTRAINT casino_channel_server_id FOREIGN KEY(server_id) REFERENCES casino_server(server_id),
    //   CONSTRAINT casino_channel_added_by FOREIGN KEY(added_by) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS casino_server_user (
    //   server_id bigint,
    //   uid bigint,
    //   PRIMARY KEY(server_id, uid),
    //   CONSTRAINT casino_server_user_server_id FOREIGN KEY(server_id) REFERENCES casino_server(server_id),
    //   CONSTRAINT casino_server_user_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    private static Set<Long> fetchAdminUsers() {
        return CasinoDB.executeSetQuery("SELECT uid FROM admin_user;");
    }

    private static List<FetchedServer> fetchCasinoServers() {
        String query = "SELECT server_id, name, event_channel, money_machine_pot FROM casino_server;";
        List<FetchedServer> servers = new ArrayList<>();
        return CasinoDB.executeQueryWithReturn(query, results -> {
            while (results.next()) {
                servers.add(new FetchedServer(results.getLong(1), results.getString(2),
                    results.getLong(3), results.getLong(4)));
            }
            return servers;
        }, servers);
    }

    private static Set<Long> fetchCasinoChannels(long server) {
        String query = "SELECT channel_id FROM casino_channel WHERE server_id = "
            + server + ";";
        return CasinoDB.executeSetQuery(query);
    }

    private static Set<Long> fetchCasinoUsers(long server) {
        String query = "SELECT uid FROM casino_server_user WHERE server_id = "
            + server + ";";
        return CasinoDB.executeSetQuery(query);
    }

    private static boolean logAddServerUser(long uid, long server) {
        String query = "INSERT INTO casino_server_user (server_id, uid) SELECT "
            + server + ", " + uid + " WHERE EXISTS (SELECT uid FROM money_user WHERE uid = "
            + uid + ");";
        return CasinoDB.executeUpdate(query);
    }

    private static FetchedServer logAddServer(long server, String name, long uid) {
        String query = "INSERT INTO casino_server (server_id, name, added_by) VALUES ("
            + server + ", ?, " + uid + ") ON CONFLICT (server_id) DO NOTHING RETURNING "
            + "server_id, name, event_channel, money_machine_pot;";
        return CasinoDB.executeValidatedQueryWithReturn(query, results -> {
            if (results.next()) {
                return new FetchedServer(results.getLong(1), results.getString(2),
                    results.getLong(3), results.getLong(4));
            }
            return null;
        }, null, name);
    }

    private static boolean logAddCasinoChannel(long server, long channel, String channelName, long uid) {
        String query = "INSERT INTO casino_channel (channel_id, server_id, name, added_by) VALUES ("
            + channel + ", " + server + ",?," + uid + ");";
        return CasinoDB.executeValidatedUpdate(query, channelName);
    }

    private static boolean logSetEventChannel(long server, long channel, String channelName) {
        String query = "UPDATE casino_server SET (event_channel, event_channel_name) = ("
            + channel + ", ?) WHERE server_id = " + server + ";";
        return CasinoDB.executeValidatedUpdate(query, channelName);
    }

    private static boolean logRemoveCasinoChannel(long server, long channel) {
        String query = "DELETE FROM casino_channel WHERE channel_id = "
            + channel + " AND server_id  = " + server + ";";
        return CasinoDB.executeUpdate(query);
    }

    private static boolean logRemoveEventChannel(long server) {
        String query = "UPDATE casino_server SET (event_channel, event_channel_name) = (NULL, NULL) WHERE server_id = "
            + server + ";";
        return CasinoDB.executeUpdate(query);
    }

    private static long logSetMoneyMachinePot(long server, long amount) {
        return CasinoDB.executeLongQuery("UPDATE casino_server SET money_machine_pot = " + amount
            + " WHERE server_id = " + server + " RETURNING money_machine_pot;");
    }

    private static long logIncreaseMoneyMachinePot(long server, long amount) {
        return CasinoDB.executeLongQuery("UPDATE casino_server SET money_machine_pot = money_machine_pot + "
            + amount + " WHERE server_id = " + server + " RETURNING money_machine_pot;");
    }
}
