package com.c2t2s.hb;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class EventDriver {

    // Hide default constructor
    private EventDriver() {}

    Map<Long, ServerEventDriver> serverDrivers = new HashMap<>();
    Queue<Event> upcomingEvents = new LinkedList<>();

    private static final int HOURS_BETWEEN_EVENTS = 12;

    void initialize() {
        Event nextEvent = fetchNextEvent();

        if (nextEvent == null) {
            // If no upcoming events in DB, generate one
            Event lastEvent = fetchLastEvent();
            Instant now = Instant.now();
            Instant lastTime;
            Event.EventType lastEventType;

            if (lastEvent != null) {
                lastTime = lastEvent.getEndTime().toInstant();
                lastEventType = lastEvent.getType();
            } else {
                // No events in DB at all. This shouldn't ever happen,
                // but if it does we just need a placeholder value.
                // Details like timezone therefore aren't important
                lastTime = now.minusSeconds(1);
                // Set last type to a bonus event so next is Fishing
                lastEventType = Event.EventType.SUPER_GUESS;
            }

            // Events happen every 12 hours so figure out the next
            // time from the last one
            long missedEvents = Duration.between(lastTime, now).toHours() / HOURS_BETWEEN_EVENTS;
            Timestamp nextTime = Timestamp.from(lastTime.plus((missedEvents + 1) * HOURS_BETWEEN_EVENTS, ChronoUnit.HOURS));
            nextEvent = EventFactory.createEvent(getNextEventType(lastEventType), nextTime);
            storeEvent(nextEvent);
        }

        upcomingEvents.add(nextEvent);

        // TODO: Populate the next 3 events

        // TODO: Check if we need to resolve the next event immediately

        // TODO: Start a timer
    }

    private static Event.EventType getNextEventType(Event.EventType type) {
        // Fish -> Rob -> Work -> Pick -> Bonus
        if (type == Event.EventType.PICKPOCKET) {
            // Pick random non-base event type
            int bonusEventIndex = HBMain.RNG_SOURCE.nextInt(Event.EventType.values().length - Event.BASE_EVENT_COUNT);
            return Event.EventType.values()[bonusEventIndex + Event.BASE_EVENT_COUNT];
        }

        switch (type) {
            case FISH:
                return Event.EventType.ROB;
            case ROB:
                return Event.EventType.WORK;
            case WORK:
                return Event.EventType.PICKPOCKET;
            // PICKPOCKET was handled above
            default:
                return Event.EventType.FISH;
        }
    }

    static String checkUpcomingEvents(Long server) {
        return "placeholder";
    }

    private static class ServerEventDriver {

    }

    //////////////////////////////////////////////////////////

    private Event fetchNextEvent() {
        //TODO
        return null;
    }

    private Event fetchLastEvent() {
        //TODO
        return null;
    }

    private void storeEvent(Event event) {
        //TODO
    }

    private Event getUpcomingEvent(Timestamp time) {
        //TODO
        return null;
    }
}
