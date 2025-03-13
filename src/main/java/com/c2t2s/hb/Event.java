package com.c2t2s.hb;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

abstract class Event {

    static class EventFactory {

        // Hide default constructor
        private EventFactory() {}

        static Event createEvent(long server) {
            return new TestEvent(server, Duration.ofSeconds(10));
        }
    }

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

    protected EventType type;
    protected long server;
    protected Duration timeUntilResolution;

    static final Duration EVENT_ENDING_REMINDER_WINDOW = Duration.ofSeconds(5);
    static final Duration NEW_EVENT_DELAY = Duration.ofMinutes(5);

    protected Event(EventType type, long server, Duration timeUntilResolution) {
        this.type = type;
        this.server = server;
        this.timeUntilResolution = timeUntilResolution;
    }

    EventType getType() {
        return type;
    }

    abstract String createInitialMessage();

    abstract void handleJoinMessage(long uid, Gacha.GachaCharacter character, int selection);

    abstract String createReminderMessage();

    abstract Queue<String> createResolutionMessages();

    void awardCharacterXp() {
        // TODO
    }

    Void initialize() {
        if (timeUntilResolution.compareTo(EVENT_ENDING_REMINDER_WINDOW) < 0) {
            CasinoServerManager.schedule(this::resolve, timeUntilResolution);
        } else {
            CasinoServerManager.schedule(this::postReminder,
                timeUntilResolution.minus(EVENT_ENDING_REMINDER_WINDOW));
        }

        CasinoServerManager.sendEventMessage(server, createInitialMessage());
        return null;
    }

    Void postReminder() {
        CasinoServerManager.schedule(this::resolve, EVENT_ENDING_REMINDER_WINDOW);
        CasinoServerManager.sendEventMessage(server, createReminderMessage());
        return null;
    }

    Void resolve() {
        CasinoServerManager.schedule(() -> {
            CasinoServerManager.beginNewEvent(server);
            return null;
        }, NEW_EVENT_DELAY);

        CasinoServerManager.sendMultipartEventMessage(server, createResolutionMessages());
        return null;
    }

    private static class TestEvent extends Event {
        TestEvent(long server, Duration timeUntilResolution) {
            super(EventType.FISH, server, timeUntilResolution);
        }

        String createInitialMessage() {
            return "Event starting";
        }

        String createReminderMessage() {
            return "Ending soon!";
        }

        Queue<String> createResolutionMessages() {
            return new LinkedList<>(Arrays.asList("D", "D O", "D O N", "D O N E"));
        }

        void handleJoinMessage(long uid, Gacha.GachaCharacter character, int selection) {}
    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS event (
    //  server bigint NOT NULL,
    //  eventId integer NOT NULL,
    //  type integer NOT NULL,
    //  theme integer NOT NULL,
    //  completed boolean NOT NULL DEFAULT FALSE,
    //  PRIMARY KEY(server, eventId),
    //  CONSTRAINT event_server_id FOREIGN KEY(server) REFERENCES casino_server(server_id)
    // );


}