package com.c2t2s.hb;

import java.sql.Timestamp;

class EventFactory {

    // Hide default constructor
    private EventFactory() {}

    static Event createEvent(Event.EventType type, Timestamp endTime) {
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
    protected boolean isBonus;
    private Timestamp endTime;

    boolean isBonus() {
        return isBonus;
    }

    Timestamp getEndTime() {
        return endTime;
    }

    EventType getType() {
        return type;
    }
}
