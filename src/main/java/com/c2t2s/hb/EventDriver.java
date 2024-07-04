package com.c2t2s.hb;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

public class EventDriver {

    // Hide default constructor
    private EventDriver() {}

    List<EventServer> eventServers = new ArrayList<>();
    Timer timer = new Timer();

    public void initialize() {
        List<EventServer> servers = fetchEventServers(timer);
        if (servers == null) {
            System.out.println("Error fetching event servers during initialization");
            return;
        }

        for (EventServer server: servers) {
            eventServers.add(server);
            server.initialize();
        }
    }

    private static class EventServer {
        long channel;
        Timer timer;
        Deque<Event> upcomingEvents = new LinkedList<>();

        private static final int HOURS_BETWEEN_EVENTS = 12;
        private static final int STORED_UPCOMING_EVENTS = 4;
        private static final int MINUTES_UNTIL_EVENT_START = 5;
        private static final int MINUTES_LEFT_AT_FINAL_REMINDER = 10;

        EventServer(long channel, Timer timer) {
            this.channel = channel;
            this.timer = timer;
        }

        void initialize() {
            Event lastEvent = fetchLastEvent();
            Event nextEvent = fetchNextEvent();
            Instant now = Instant.now();

            if (lastEvent == null) {
                lastEvent = EventFactory.createEvent(Event.EventType.SUPER_GUESS, now.minusSeconds(1),
                    Event.Holiday.NONE, true);
            }

            // Events happen every 12 hours so figure out the next
            // upcoming event time from the last one
            long missedEvents = Duration.between(lastEvent.getEndTime(), now).toHours() / HOURS_BETWEEN_EVENTS;
            Instant nextEventTime = lastEvent.getEndTime().plus((missedEvents + 1) * HOURS_BETWEEN_EVENTS, ChronoUnit.HOURS);

            if (nextEvent.getEndTime().isAfter(nextEventTime)) {
                nextEvent = EventFactory.createEvent(Event.EventType.getNextEventType(lastEvent.getType()), nextEventTime);
                storeEvent(nextEvent);
            }

            // If last event never completed complete it now
            if (!lastEvent.isComplete()) {
                lastEvent.completeEvent();
            }

            // Populate all the upcoming events
            upcomingEvents.add(nextEvent);
            while (upcomingEvents.size() < STORED_UPCOMING_EVENTS) {
                upcomingEvents.add(generateNextEvent(upcomingEvents.peekLast()));
            }

            // TODO: Start a timer
        }

        private static Event generateNextEvent(Event previousEvent) {
            Instant nextEndTime = previousEvent.getEndTime().plus(HOURS_BETWEEN_EVENTS, ChronoUnit.HOURS);
            Event nextEvent = getUpcomingEvent(nextEndTime);

            if (nextEvent == null) {
                nextEvent = EventFactory.createEvent(Event.EventType.getNextEventType(previousEvent.getType()), nextEndTime);
                storeEvent(nextEvent);
            }

            return nextEvent;
        }

        void sendMessage(HBMain.SingleResponse message) {
            // TODO
        }

        void sendMessage(HBMain.MultistepResponse message) {
            // TODO
        }

        void resolveEvent(Instant endTime) {
            // TODO
        }

        static String checkUpcomingEvents(Long server) {
            return "placeholder";
        }

    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS event_server (
    //  channel bigint PRIMARY KEY,
    //  name varchar(40) NOT NULL DEFAULT ''
    // );

    // CREATE TABLE IF NOT EXISTS user_event_server (
    //   channel bigint NOT NULL,
    //   uid bigint NOT NULL,
    //   cid bigint NOT NULL,
    //   selection integer NOT NULL,
    //   PRIMARY KEY(server, uid),
    //   CONSTRAINT user_event_server_server FOREIGN KEY(channel) REFERENCES event_server(channel),
    //   CONSTRAINT user_event_server_cid FOREIGN KEY(uid, cid) REFERENCES gacha_user_character(uid, cid)
    // );

    private List<EventServer> fetchEventServers(Timer timer) {
        // TODO
        return null;
    }

    private static Event fetchNextEvent() {
        //TODO
        return null;
    }

    private static Event fetchLastEvent() {
        //TODO
        return null;
    }

    private static void storeEvent(Event event) {
        //TODO
    }

    private static Event getUpcomingEvent(Instant time) {
        Timestamp sqlTime = Timestamp.from(time);
        //TODO
        return null;
    }
}
