package com.c2t2s.hb;

import java.util.HashSet;
import java.util.Set;

import com.c2t2s.hb.HBMain.CasinoCommand;

public class CommandAccessControl {

    // Hide default constructor
    private CommandAccessControl() {}

    private static Set<Long> casinoChannels = new HashSet<>();
    private static Set<Long> eventChannels = new HashSet<>();
    private static Set<Long> adminUsers = new HashSet<>();

    private static final long CHANNEL_NOT_FOUND = -1;

    static void initialize() {
        fetchAdminUsers();
        fetchCasinoChannels();
        fetchEventChannels();
    }

    static boolean isValid(long uid, CasinoCommand command, long channel) {
        return (command.isValidInCasinoChannels() && casinoChannels.contains(channel))
            || (command.isValidInGachaChannels() && eventChannels.contains(channel))
            || adminUsers.contains(uid);
    }

    static String handleRegisterCasinoChannel(long uid, long server, String serverName,
            long channel, String channelName) {
        if (casinoChannelExists(server, channel)) {
            return "Unable to add casino channel: Specified channel is already registered";
        }

        if (!serverExists(server)) {
            addServer(server, serverName, uid);
        }

        addCasinoChannel(server, channel, channelName, uid);
        System.out.println("#" + channelName + " registered as a casino channel for server "
            + serverName + " by " + uid);
        return "Channel registered";
    }

    static String handleRegisterEventChannel(long uid, long server, String serverName, long channel, String channelName) {
        if (!serverExists(server)) {
            addServer(server, serverName, uid);
        }

        long existingEventChannel = fetchEventChannel(server);
        if (existingEventChannel != CHANNEL_NOT_FOUND) {
            if (existingEventChannel == channel) {
                return "Unable to add event channel: Specified channel is already registered";
            } else {
                return "Unable to add event channel: An event channel is already registered for this server (#"
                    + fetchEventChannelName(server) + ")";
            }
        }

        setEventChannel(server, channel, channelName);
        System.out.println("#" + channelName + " registered as the event channel for server "
            + serverName + " by " + uid);
        return "Channel registered";
    }

    static String handleDeregisterCasinoChannel(long uid, long server, long channel) {
        if (!casinoChannelExists(server, channel)) {
            return "Unable to deregister casino channel: Specified channel is not registered";
        }

        String channelName = removeCasinoChannel(server, channel);
        System.out.println("#" + channelName + " deregistered as a casino channel for server "
            + fetchServerName(server) + " by " + uid);
        return "Channel deregistered";
    }

    static String handleDeregisterEventChannel(long uid, long server, long channel) {
        if (!eventChannelExists(server)) {
            return "Unable to deregister event channel: No event channel is registered for this server";
        } else if (fetchEventChannel(server) != channel) {
            return "Unable to deregister event channel: Specified channel is not the event channel (event channel is #"
                + fetchEventChannel(server) + ")";
        }

        String channelName = removeEventChannel(server);
        System.out.println("#" + channelName + " deregistered as event channel for server "
            + fetchServerName(server) + " by " + uid);
        return "Channel deregistered";
    }

    static Set<Long> getGachaChannels() {
        return eventChannels;
    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS admin_user (
    //   uid bigint PRIMARY KEY
    // )

    // CREATE TABLE IF NOT EXISTS casino_server (
    //   server_id bigint PRIMARY KEY,
    //   name varchar(100) NOT NULL,
    //   event_channel bigint DEFAULT NULL,
    //   event_channel_name varchar(100) DEFAULT NULL,
    //   added_by bigint NOT NULL,
    //   CONSTRAINT casino_server_added_by FOREIGN KEY(added_by) REFERENCES admin_user(uid)
    // )

    // CREATE TABLE IF NOT EXISTS casino_channel (
    //   channel_id bigint PRIMARY KEY,
    //   server_id bigint NOT NULL,
    //   name varchar(100) NOT NULL,
    //   added_by bigint NOT NULL,
    //   CONSTRAINT casino_channel_server_id FOREIGN KEY(server_id) REFERENCES casino_server(server_id),
    //   CONSTRAINT casino_channel_added_by FOREIGN KEY(added_by) REFERENCES admin_user(uid)
    // )

    private static void fetchAdminUsers() {

    }

    private static void fetchCasinoChannels() {

    }

    private static void fetchEventChannels() {

    }

    private static boolean casinoChannelExists(long server, long channel) {

    }

    private static boolean eventChannelExists(long server) {

    }

    private static boolean serverExists(long server) {

    }

    private static long fetchEventChannel(long server) {

    }

    private static String fetchServerName(long server) {

    }

    private static String fetchEventChannelName(long server) {

    }

    private static boolean addServer(long server, String name, long uid) {

    }

    private static boolean addCasinoChannel(long server, long channel, String channelName, long uid) {

    }

    private static boolean setEventChannel(long server, long channel, String channelName) {

    }

    // Returns name of removed channel
    private static String removeCasinoChannel(long server, long channel) {

    }

    // Returns name of removed channel
    private static String removeEventChannel(long server) {

    }

}
