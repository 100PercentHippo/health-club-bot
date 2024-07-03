package com.c2t2s.hb;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class EventDriver {

    // Hide default constructor
    private EventDriver() {}

    Map<Long, ServerEventDriver> serverDrivers = new HashMap<>();
    Deque<Event> upcomingEvents = new LinkedList<>();

    private static final int HOURS_BETWEEN_EVENTS = 12;
    private static final int STORED_UPCOMING_EVENTS = 4;

    void initialize() {
        Event lastEvent = fetchLastEvent();
        Event nextEvent = fetchNextEvent();
        Instant now = Instant.now();

        if (lastEvent == null) {
            lastEvent = EventFactory.createEvent(Event.EventType.SUPER_GUESS, now.minusSeconds(1), true);
        }

        // Events happen every 12 hours so figure out the next
        // upcoming event time from the last one
        long missedEvents = Duration.between(lastEvent.getEndTime(), now).toHours() / HOURS_BETWEEN_EVENTS;
        Instant nextEventTime = lastEvent.getEndTime().plus((missedEvents + 1) * HOURS_BETWEEN_EVENTS, ChronoUnit.HOURS);

        if (nextEvent.getEndTime().isAfter(nextEventTime)) {
            nextEvent = EventFactory.createEvent(getNextEventType(lastEvent.getType()), nextEventTime);
            storeEvent(nextEvent);
        }

        // If last event never completed complete it now
        if (!lastEvent.isComplete()) {
            // TODO: Complete event
        }

        // Populate all the upcoming events
        upcomingEvents.add(nextEvent);
        while (upcomingEvents.size() < STORED_UPCOMING_EVENTS) {
            upcomingEvents.add(generateNextEvent(upcomingEvents.peekLast()));
        }

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

    private static Event generateNextEvent(Event previousEvent) {
        Instant nextEndTime = previousEvent.getEndTime().plus(HOURS_BETWEEN_EVENTS, ChronoUnit.HOURS);
        Event nextEvent = getUpcomingEvent(nextEndTime);

        if (nextEvent == null) {
            nextEvent = EventFactory.createEvent(getNextEventType(previousEvent.getType()), nextEndTime);
            storeEvent(nextEvent);
        }

        return nextEvent;
    }

    static String checkUpcomingEvents(Long server) {
        return "placeholder";
    }

    private static class ServerEventDriver {

    }

    //////////////////////////////////////////////////////////

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
