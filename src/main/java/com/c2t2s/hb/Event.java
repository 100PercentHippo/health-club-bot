package com.c2t2s.hb;

import java.time.Instant;

class EventFactory {

    // Hide default constructor
    private EventFactory() {}

    static Event createEvent(Event.EventType type, Instant endTime) {
        return createEvent(type, endTime, false);
    }

    static Event createEvent(Event.EventType type, Instant endTime, boolean complete) {
        // TODO
        return null;
    }
}

abstract class Event {
    static final int BASE_EVENT_COUNT = 4; // FISH, ROB, WORK, PICKPOCKET
    enum EventType {
        FISH, ROB, WORK, PICKPOCKET, AVERAGE, UNIQUE, SUPER_GUESS, SUPER_SLOTS, SUPER_OVERUNDER
    }

    protected EventType type;
    protected boolean isComplete;
    private Instant endTime;


    boolean isComplete() {
        return isComplete;
    }

    Instant getEndTime() {
        return endTime;
    }

    EventType getType() {
        return type;
    }
}
