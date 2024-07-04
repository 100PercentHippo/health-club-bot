package com.c2t2s.hb;

import java.sql.Timestamp;
import java.time.Instant;

class EventFactory {

    // Hide default constructor
    private EventFactory() {}

    static Event createEvent(Event.EventType type, Instant endTime) {
        return createEvent(type, endTime, Event.Holiday.NONE);
    }

    static Event createEvent(Event.EventType type, Instant endTime, Event.Holiday holiday) {
        return createEvent(type, endTime, holiday, false);
    }

    static Event createEvent(Event.EventType type, Instant endTime, Event.Holiday holiday,
            boolean complete) {
        return createEvent(type, endTime, holiday, complete, false);
    }

    static Event createEvent(Event.EventType type, Instant endTime, Event.Holiday holiday,
            boolean complete, boolean locked) {
        // TODO
        return null;
    }
}

abstract class Event {

    // It is probably obvious looking at these enums that I'm a C++ dev
    static final int BASE_EVENT_COUNT = 4; // FISH, ROB, WORK, PICKPOCKET
    static final int EVENTTYPE_ID_FISH = 0;
    static final int EVENTTYPE_ID_ROB = 1;
    static final int EVENTTYPE_ID_WORK = 2;
    static final int EVENTTYPE_ID_PICKPOCKET = 3;
    static final int EVENTTYPE_ID_AVERAGE = 4;
    static final int EVENTTYPE_ID_SUPER_GUESS = 5;
    static final int EVENTTYPE_ID_SUPER_SLOTS = 6;
    static final int EVENTTYPE_ID_SUPER_OVERUNDER = 7;
    enum EventType {
        FISH(EVENTTYPE_ID_FISH), ROB(EVENTTYPE_ID_ROB), WORK(EVENTTYPE_ID_WORK),
        PICKPOCKET(EVENTTYPE_ID_PICKPOCKET), AVERAGE(EVENTTYPE_ID_AVERAGE),
        SUPER_GUESS(EVENTTYPE_ID_SUPER_GUESS), SUPER_SLOTS(EVENTTYPE_ID_SUPER_SLOTS),
        SUPER_OVERUNDER(EVENTTYPE_ID_SUPER_OVERUNDER);

        int id;
        private EventType(int id) { this.id = id; }

        static EventType fromId(int id) {
            switch (id) {
                case EVENTTYPE_ID_ROB:
                    return ROB;
                case EVENTTYPE_ID_WORK:
                    return WORK;
                case EVENTTYPE_ID_PICKPOCKET:
                    return PICKPOCKET;
                case EVENTTYPE_ID_AVERAGE:
                    return AVERAGE;
                case EVENTTYPE_ID_SUPER_GUESS:
                    return SUPER_GUESS;
                case EVENTTYPE_ID_SUPER_SLOTS:
                    return SUPER_SLOTS;
                case EVENTTYPE_ID_SUPER_OVERUNDER:
                    return SUPER_OVERUNDER;
                case EVENTTYPE_ID_FISH:
                default:
                    return FISH;
            }
        }

        static EventType getNextEventType(EventType previousType) {
            // Fish -> Rob -> Work -> Pick -> Bonus
            if (previousType == EventType.PICKPOCKET) {
                // Pick random non-base event type
                int bonusEventIndex = HBMain.RNG_SOURCE.nextInt(EventType.values().length - BASE_EVENT_COUNT);
                return EventType.values()[bonusEventIndex + BASE_EVENT_COUNT];
            }

            switch (previousType) {
                case FISH:
                    return EventType.ROB;
                case ROB:
                    return EventType.WORK;
                case WORK:
                    return EventType.PICKPOCKET;
                // PICKPOCKET was handled above
                default:
                    return EventType.FISH;
            }
        }
    }

    static final int HOLIDAY_ID_NONE = 0;
    static final int HOLIDAY_ID_HALLOWEEN = 1;
    static final int HOLIDAY_ID_THANKSGIVING = 2;
    static final int HOLIDAY_ID_CHRISTMAS = 3;
    enum Holiday {
        NONE(HOLIDAY_ID_NONE), HALLOWEEN(HOLIDAY_ID_HALLOWEEN),
        THANKSGIVING(HOLIDAY_ID_THANKSGIVING), CHRISTMAS(HOLIDAY_ID_CHRISTMAS);

        int id;
        private Holiday(int id) { this.id = id; }

        static Holiday fromId(int id) {
            switch (id) {
                case HOLIDAY_ID_HALLOWEEN:
                    return HALLOWEEN;
                case HOLIDAY_ID_THANKSGIVING:
                    return THANKSGIVING;
                case HOLIDAY_ID_CHRISTMAS:
                    return CHRISTMAS;
                case HOLIDAY_ID_NONE:
                default:
                    return NONE;
            }
        }
    }

    protected EventType type;
    protected Holiday holiday;
    protected boolean isComplete;
    protected boolean locked;
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

    abstract HBMain.MultistepResponse completeEvent();

    abstract void awardCharacterXp();

    abstract HBMain.SingleResponse createInitialMessage();

    abstract HBMain.SingleResponse createReminderMessage();

    abstract HBMain.SingleResponse createJoinMessage(long uid);

    abstract HBMain.SingleResponse handleUserJoin(long uid, long cid, int selection);

    abstract HBMain.SingleResponse switchUserSelection(long uid, long cid, int selection);

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS event (
    //  endTime timestamp NOT NULL,
    //  server bigint NOT NULL,
    //  type integer NOT NULL DEFAULT 0,
    //  holiday integer NOT NULL DEFAULT 0,
    //  locked boolean NOT NULL DEFAULT FALSE,
    //  completed boolean NOT NULL DEFAULT FALSE,
    //  PRIMARY KEY(endTime, server),
    //  CONSTRAINT event_server_id FOREIGN KEY(server) REFERENCES event_server(server)
    // );

    static Event executeEventQuery(String query) {
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                Timestamp endTime = results.getTimestamp(1);
                int type = results.getInt(2);
                int holiday = results.getInt(3);
                boolean completed = results.getBoolean(4);
                boolean locked = results.getBoolean(5);
                return EventFactory.createEvent(EventType.fromId(type), endTime.toInstant(),
                    Holiday.fromId(holiday), completed, locked);
            }
            return null;
        }, null);
    }
}
