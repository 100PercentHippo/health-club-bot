package com.c2t2s.hb;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static java.util.Map.entry;

abstract class Event {

    static class EventFactory {

        // Hide default constructor
        private EventFactory() {}

        static Event createEvent(long server, EventType type) {
            return new TestEvent(type, server, Duration.ofMinutes(2));
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
    enum EventType {
        FISH(EVENTTYPE_ID_FISH), ROB(EVENTTYPE_ID_ROB), WORK(EVENTTYPE_ID_WORK),
        PICKPOCKET(EVENTTYPE_ID_PICKPOCKET), AVERAGE(EVENTTYPE_ID_AVERAGE),
        SUPER_GUESS(EVENTTYPE_ID_SUPER_GUESS), SUPER_SLOTS(EVENTTYPE_ID_SUPER_SLOTS);

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
    private Map<Long, String> joinSelections;
    private List<HBMain.AutocompleteIdOption> options;

    private Set<Long> participatingUsers = new HashSet<>();
    private boolean complete = false;

    static final Duration EVENT_ENDING_REMINDER_WINDOW = Duration.ofMinutes(1);
    static final Duration NEW_EVENT_DELAY = Duration.ofMinutes(1);

    protected Event(EventType type, long server, Duration timeUntilResolution,
            Map<Long, String> joinSelections) {
        this.type = type;
        this.server = server;
        this.timeUntilResolution = timeUntilResolution;
        this.joinSelections = joinSelections;

        options = new ArrayList<>();
        for (Map.Entry<Long, String> entry : joinSelections.entrySet()) {
            options.add(new HBMain.AutocompleteIdOption(entry.getKey(), entry.getValue()));
        }
    }

    EventType getType() {
        return type;
    }

    abstract String createInitialMessage();

    abstract String createReminderMessage();

    abstract Queue<String> createResolutionMessages();

    abstract String createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character, long selection);

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
        complete = true;
        CasinoServerManager.schedule(() -> {
            CasinoServerManager.beginNewEvent(server);
            return null;
        }, NEW_EVENT_DELAY);

        CasinoServerManager.sendMultipartEventMessage(server, createResolutionMessages());
        return null;
    }

    List<HBMain.AutocompleteIdOption> handleSelectionAutocomplete() {
        return options;
    }

    String handleUserJoin(long uid, Gacha.GachaCharacter character, long selection) {
        if (complete) {
            return "Unable to join event: Event has ended";
        }
        if (!joinSelections.containsKey(selection)) {
            return "Unable to join event: Unrecognized selection " + selection;
        }
        if (participatingUsers.contains(uid)) {
            return "Unable to join event: You are already participating in this event!";
        }
        Casino.User user = Casino.getUser(uid);
        if (user == null) {
            return Casino.USER_NOT_FOUND_MESSAGE;
        }
        // TODO: Check if the user is participating in an event on another server

        participatingUsers.add(uid);
        CasinoServerManager.sendEventMessage(server,
            createPublicUserJoinMessage(user, character, selection));
        return "Succesfully joined" + type.name().replace('_', ' ') + " event with selection"
            + joinSelections.get(selection);
    }

    private static class TestEvent extends Event {
        private static long SELECTION_1_VALUE = 1;
        private static long SELECTION_2_VALUE = 2;

        TestEvent(EventType type, long server, Duration timeUntilResolution) {
            super(type, server, timeUntilResolution,
                Map.ofEntries(entry(SELECTION_1_VALUE, "Option 1"),
                              entry(SELECTION_2_VALUE, "Option 2")));
        }

        String createInitialMessage() {
            return type.name() + " event starting";
        }

        String createReminderMessage() {
            return "Ending soon!";
        }

        Queue<String> createResolutionMessages() {
            return new LinkedList<>(Arrays.asList("D", "D O", "D O N", "D O N E"));
        }

        String createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character, long selection) {
            return user.getNickname() + " joined with " + character.getDisplayName() + " and selection " + selection;
        }
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