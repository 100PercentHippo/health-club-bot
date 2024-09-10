package com.c2t2s.hb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.c2t2s.hb.HBMain.CasinoCommand;

public class CommandAccessControl {

    // Hide default constructor
    private CommandAccessControl() {}

    private static Set<Long> adminUsers = new HashSet<>();
    private static Map<Long, CasinoServer> servers = new HashMap<>();

    private static final long CHANNEL_NOT_FOUND = -1;

    private static class CasinoServer {
        String serverName;
        long eventChannel;
        String eventChannelName;
        Set<Long> casinioChannels = new HashSet<>();

        CasinoServer(String serverName) {
            this.serverName = serverName;
            eventChannel = CHANNEL_NOT_FOUND;
            eventChannelName = "";
        }

        CasinoServer(String serverName, long eventChannel, String eventChannelName) {
            this.serverName = serverName;
            this.eventChannel = eventChannel;
            this.eventChannelName = eventChannelName;
        }
    }

    static void initialize() {
        adminUsers = fetchAdminUsers();
        servers = fetchCasinoServers();
        populateCasinoChannels(servers);
    }

    static boolean isValid(long uid, CasinoCommand command, long server, long channel) {
        if (adminUsers.contains(uid)) {
            return true;
        }
        if (!servers.containsKey(server)) {
            return false;
        }
        CasinoServer casinoServer = servers.get(server);
        return (command.isValidInCasinoChannels() && casinoServer.casinioChannels.contains(channel))
            || (command.isValidInGachaChannels() && casinoServer.eventChannel == channel);
    }

    static boolean addServer(long server, String serverName, long uid) {
        if (!logAddServer(server, serverName, uid)) {
            return false;
        }
        servers.put(server, new CasinoServer(serverName));
        return true;
    }

    static String handleRegisterCasinoChannel(long uid, long server, String serverName,
            long channel, String channelName) {
        if (servers.containsKey(server) && servers.get(server).casinioChannels.contains(channel)) {
            return "Unable to add casino channel: Specified channel is already registered";
        }

        if (!servers.containsKey(server)) {
            if (!addServer(server, serverName, uid)) {
                return "Unable to add casino channel: DB Error adding this server";
            }
        }

        if (!logAddCasinoChannel(server, channel, channelName, uid)) {
            return "Unable to add casino channel: DB Error adding this channel";
        }
        servers.get(server).casinioChannels.add(channel);
        System.out.println("#" + channelName + " registered as a casino channel for server "
            + serverName + " by " + uid);
        return "Channel registered";
    }

    static String handleRegisterEventChannel(long uid, long server, String serverName, long channel, String channelName) {
        if (servers.containsKey(server)) {
            if (servers.get(server).eventChannel == channel) {
                return "Unable to add event channel: Specified channel is already registered";
            } else if (servers.get(server).eventChannel != CHANNEL_NOT_FOUND) {
                return "Unable to add event channel: An event channel is already registered for this server (#"
                    + servers.get(server).eventChannelName + ")";
            }
        }

        if (!servers.containsKey(server)) {
            if (!addServer(server, serverName, uid)) {
                return "Unable to add event channel: DB Error adding this server";
            }
        }

        if (!logSetEventChannel(server, channel, channelName)) {
            return "Unable to add event channel: DB Error adding this channel";
        }
        servers.get(server).eventChannel = channel;
        servers.get(server).eventChannelName = channelName;
        System.out.println("#" + channelName + " registered as the event channel for server "
            + serverName + " by " + uid);
        return "Channel registered";
    }

    static String handleDeregisterCasinoChannel(long uid, long server, long channel) {
        if (!servers.containsKey(server)) {
            return "Unable to deregister casino channel: Server is not registered";
        }

        CasinoServer casinoServer = servers.get(server);

        if (casinoServer.casinioChannels.contains(channel)) {
            return "Unable to deregister casino channel: Specified channel is not registered";
        }

        String channelName = logRemoveCasinoChannel(server, channel);
        if (channelName == null) {
            return "Unable to deregister casino channel: DB Error removing channel";
        }
        casinoServer.casinioChannels.remove(channel);
        System.out.println("#" + channelName + " deregistered as a casino channel for server "
            + casinoServer.serverName + " by " + uid);
        return "Channel deregistered";
    }

    static String handleDeregisterEventChannel(long uid, long server, long channel) {
        if (!servers.containsKey(server)) {
            return "Unable to deregister casino channel: Server is not registered";
        }

        CasinoServer casinoServer = servers.get(server);

        if (casinoServer.eventChannel == CHANNEL_NOT_FOUND) {
            return "Unable to deregister event channel: No event channel is registered for this server";
        } else if (casinoServer.eventChannel != channel) {
            return "Unable to deregister event channel: Specified channel is not the event channel (event channel is #"
                + casinoServer.eventChannelName + ")";
        }

        if (!logRemoveEventChannel(server)) {
            return "Unable to deregister event channel: DB Error removing event channel";
        }
        casinoServer.eventChannel = CHANNEL_NOT_FOUND;
        casinoServer.eventChannelName = "";
        System.out.println("#" + casinoServer.eventChannelName + " deregistered as event channel for server "
            + casinoServer.serverName + " by " + uid);
        return "Channel deregistered";
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
    //   money_machine_pot bigint NOT NULL DEFAULT 0,
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

    private static Map<Long, CasinoServer> fetchCasinoServers() {
        String query = "SELECT server_id, name, event_channel, event_channel_name FROM casino_server;";
        Map<Long, CasinoServer> servers = new HashMap<>();
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                long serverId = results.getLong(1);
                String serverName = results.getString(2);
                long channelId = results.getLong(3);
                String channelName = results.getString(4);
                servers.put(serverId, new CasinoServer(serverName, channelId, channelName));
            }
            return servers;
        }, servers);
    }

    private static void populateCasinoChannels(Map<Long, CasinoServer> servers) {
        for (Map.Entry<Long, CasinoServer> entry : servers.entrySet()) {
            String query = "SELECT channel_id FROM casino_channel WHERE server_id = "
                + entry.getKey() + ";";
            entry.getValue().casinioChannels = CasinoDB.executeSetQuery(query);
        }
    }

    // In this and below, validate the String names
    private static boolean logAddServer(long server, String name, long uid) {
        String query = "INSERT INTO casino_server (server_id, name, added_by) VALUES ("
            + server + ", ?, " + uid + ") ON CONFLICT (server_id) DO NOTHING;";
        int result = CasinoDB.executeValidatedUpdate(query, name);
        return CasinoDB.wasInsertSuccessful(result);
    }

    private static boolean logAddCasinoChannel(long server, long channel, String channelName, long uid) {
        String query = "INSERT INTO casino_channel (channel_id, server_id, name, added_by) VALUES ("
            + channel + ", " + server + ",?," + uid + ");";
        int result = CasinoDB.executeValidatedUpdate(query, channelName);
        return CasinoDB.wasInsertSuccessful(result);
    }

    private static boolean logSetEventChannel(long server, long channel, String channelName) {
        String query = "UPDATE casino_server SET (event_channel, event_channel_name) = ("
            + channel + ", ?) WHERE server_id = " + server + ";";
        int result = CasinoDB.executeValidatedUpdate(query, channelName);
        return CasinoDB.wasInsertSuccessful(result);
    }

    // Returns name of removed channel, null if the operation was unsuccessful
    private static String logRemoveCasinoChannel(long server, long channel) {
        String query = "DELETE FROM casino_channel WHERE channel_id = "
            + channel + " AND server_id  = " + server + " RETURNING name;";
        return CasinoDB.executeStringQuery(query);
    }

    private static boolean logRemoveEventChannel(long server) {
        String query = "UPDATE casino_server SET (event_channel, event_channel_name) = (NULL, NULL) WHERE server_id = "
            + server + ";";
        int result = CasinoDB.executeUpdate(query);
        return CasinoDB.wasInsertSuccessful(result);
    }

}
