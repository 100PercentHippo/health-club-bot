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

import org.javacord.api.entity.message.Message;

import static java.util.Map.entry;

import java.text.DecimalFormat;
import java.text.NumberFormat;

abstract class Event {

    static class EventFactory {

        // Hide default constructor
        private EventFactory() {}

        static Event createEvent(long server, EventType type) {
            return new FishEvent(server, Duration.ofMinutes(2));
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
        FISH(EVENTTYPE_ID_FISH, GachaItems.ITEM_STAT.FISH),
        ROB(EVENTTYPE_ID_ROB, GachaItems.ITEM_STAT.ROB),
        WORK(EVENTTYPE_ID_WORK, GachaItems.ITEM_STAT.WORK),
        PICKPOCKET(EVENTTYPE_ID_PICKPOCKET, GachaItems.ITEM_STAT.PICK),
        AVERAGE(EVENTTYPE_ID_AVERAGE, GachaItems.ITEM_STAT.MISC),
        SUPER_GUESS(EVENTTYPE_ID_SUPER_GUESS, GachaItems.ITEM_STAT.MISC),
        SUPER_SLOTS(EVENTTYPE_ID_SUPER_SLOTS, GachaItems.ITEM_STAT.MISC);

        int id;
        GachaItems.ITEM_STAT assocatedStat;
        private EventType(int id, GachaItems.ITEM_STAT associatedStat) {
            this.id = id;
            this.assocatedStat = associatedStat;
        }

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

    static class Participant {
        long uid;
        String nickname;
        long payout = 0;

        Participant(long uid, String nickname) {
            this.uid = uid;
            this.nickname = nickname;
        }
    }

    protected EventType type;
    protected long server;
    protected Duration timeUntilResolution;
    private Map<Long, String> joinSelections;
    private List<HBMain.AutocompleteIdOption> options;

    private Set<Long> participatingUsers = new HashSet<>();
    private boolean complete = false;
    protected long seed;
    protected String initialMessageBase;
    protected Message initialMessage;
    int totalPayoutMultiplier = 0;

    static final Duration EVENT_ENDING_REMINDER_WINDOW = Duration.ofMinutes(1);
    static final Duration EVENT_LOCK_DURATION = Duration.ofSeconds(15);
    static final Duration NEW_EVENT_DELAY = Duration.ofMinutes(1);
    static final int COUNTDOWN_SECONDS = 10;
    static final String JOIN_COMMAND_PROMPT = "\n\nJoin with `/gacha event join` for coins, pulls, and character xp!";
    static final String INVALID_SELECTION_MESSAGE = "Invalid selection for this event";
    static final NumberFormat TWO_DIGITS = new DecimalFormat("00");
    static final NumberFormat ONE_DECIMAL = new DecimalFormat("0.0");

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

    abstract String createAboutMessage();

    boolean canUsersRejoin() { return true; }

    double getPayoutBonusPercent() {
        return totalPayoutMultiplier / 10.0;
    }

    double getPayoutMultiplier() {
        return 1.0 + (totalPayoutMultiplier / 1000.0);
    }

    void awardCharacterXp() {
        // TODO
    }

    Void initialize() {
        if (timeUntilResolution.compareTo(EVENT_ENDING_REMINDER_WINDOW) < 0) {
            CasinoServerManager.schedule(this::lockEvent, timeUntilResolution);
        } else {
            CasinoServerManager.schedule(this::postReminder,
                timeUntilResolution.minus(EVENT_ENDING_REMINDER_WINDOW));
        }

        CasinoServerManager.sendEventMessage(server, createInitialMessage());
        return null;
    }

    Void postReminder() {
        if (!CasinoServerManager.hasEvent(server)) { return null; }
        CasinoServerManager.schedule(this::resolve, EVENT_ENDING_REMINDER_WINDOW);
        CasinoServerManager.sendEventMessage(server, createReminderMessage());
        return null;
    }

    Void lockEvent() {
        complete = true;
        CasinoServerManager.schedule(this::resolve, EVENT_LOCK_DURATION);
        return null;
    }

    Void resolve() {
        if (!CasinoServerManager.hasEvent(server)) { return null; }
        complete = true; // Redundant, but just in case
        CasinoServerManager.schedule(() -> {
            CasinoServerManager.beginNewEvent(server);
            return null;
        }, NEW_EVENT_DELAY);

        Queue<String> messages = createResolutionMessages();
        // TODO: Log event output
        CasinoServerManager.sendMultipartEventMessage(server, messages);
        return null;
    }

    List<HBMain.AutocompleteIdOption> handleSelectionAutocomplete() {
        return options;
    }

    String handleUserJoin(long uid, Gacha.GachaCharacter character, long selection) {
        if (complete) {
            return "Unable to join event: Event has ended";
        } else if (!CasinoServerManager.hasEvent(server)) {
            return "Unable to join event: No event found";
        } else if (!joinSelections.containsKey(selection)) {
            return "Unable to join event: Unrecognized selection " + selection;
        } else  if (participatingUsers.contains(uid) && !canUsersRejoin()) {
            return "Unable to join event: You are already participating in this event!";
        } else if (character.getCharacterStats().getStat(type.assocatedStat) < 0) {
            return "Unable to join event: Cannot join events with characters whose bonus is negative ("
                + character.getDisplayName() + " has a " + type.assocatedStat.getStatName()
                + " bonus of " + character.getCharacterStats().getStat(type.assocatedStat);
        }
        Casino.User user = Casino.getUser(uid);
        if (user == null) {
            return Casino.USER_NOT_FOUND_MESSAGE;
        }
        // TODO: Check if the user is participating in an event on another server

        if (participatingUsers.contains(uid)) {
            // Change selection
            return null;
        }

        totalPayoutMultiplier += character.getCharacterStats().getStat(type.assocatedStat);
        participatingUsers.add(uid);

        String joinMessage = createPublicUserJoinMessage(user, character, selection);
        if (joinMessage.equals(INVALID_SELECTION_MESSAGE)) {
            totalPayoutMultiplier -= character.getCharacterStats().getStat(type.assocatedStat);
            participatingUsers.remove(uid);
            return joinMessage;
        }

        CasinoServerManager.sendEventMessage(server, joinMessage);

        StringBuilder output = new StringBuilder("Successfully joined ");
        output.append(type.name().replace('_', ' ').toLowerCase());
        output.append(" event with ");
        output.append(character.getDisplayName());
        output.append(' ');
        output.append(character.getCharacterStats().printStat(type.assocatedStat));
        output.append("\nYour selection was: ");
        output.append(joinSelections.get(selection));
        if (canUsersRejoin()) {
            output.append("\nTo change your character or selection, join the event again");
        }
        return output.toString();
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

        String createAboutMessage() { return "Test"; }
    }

    private static class FishEvent extends Event {
        private static final long BOAT_1_VALUE = 1;
        private static final long BOAT_2_VALUE = 2;
        private static final long BOAT_3_VALUE = 3;
        private static final int BASE_EASY_ROLL_REQUIREMENT = 50;
        private static final int BASE_HARD_ROLL_REQUIREMENT = 100;
        private static final int ROLL_REDUCTION_PER_PARTICIPANT = 10;
        private static final int BASE_COMMON_FISH_VALUE = 100;
        private static final int BASE_UNCOMMON_FISH_VALUE = 200;
        private static final int BASE_RARE_FISH_VALUE = 300;

        static class FishEventDetails {
            String destination;
            String shallowCommon;
            String shallowUncommon;
            String deepUncommon;
            String deepRare;

            FishEventDetails(String destination, String shallowCommon, String shallowUncommon,
                    String deepUncommon, String deepRare) {
                this.destination = destination;
                this.shallowCommon = shallowCommon;
                this.shallowUncommon = shallowUncommon;
                this.deepUncommon = deepUncommon;
                this.deepRare = deepRare;
            }
        }

        static class FishParticipant extends Participant {
            int roll = 0;
            boolean wasHighest = false;
            boolean gotCommon = false;
            boolean gotUncommon = false;
            boolean gotRare = false;

            FishParticipant(long uid, String nickname) {
                super(uid, nickname);
            }
        }

        FishEventDetails details;
        List<FishParticipant> boat1Users = new ArrayList<>();
        List<FishParticipant> boat2Users = new ArrayList<>();
        List<FishParticipant> boat3Users = new ArrayList<>();

        FishEvent(long server, Duration timeUntilResolution) {
            super(EventType.FISH, server, timeUntilResolution,
                Map.ofEntries(entry(BOAT_1_VALUE, "Boat 1"),
                              entry(BOAT_2_VALUE, "Boat 2"),
                              entry(BOAT_3_VALUE, "Boat 3")));
            details = fetchFishEventDetails(seed);
        }

        String createInitialMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("A new Fishing event is starting, destination: ");
            builder.append(details.destination);
            builder.append("\nPotential fish for Boat 1 and Boat 2 (shallow water):");
            builder.append("\n\tCommon fish - ");
            builder.append(BASE_COMMON_FISH_VALUE);
            builder.append(" coins, requires a roll of ");
            builder.append(getRequiredRoll(0, true));
            builder.append("+\n\tUncommon fish - ");
            builder.append(BASE_UNCOMMON_FISH_VALUE);
            builder.append(" coins, requires a roll of ");
            builder.append(getRequiredRoll(0, false));
            builder.append("+\nPotential fish for Boat 3 (deep water):");
            builder.append("\n\tUncommon fish - ");
            builder.append(BASE_UNCOMMON_FISH_VALUE);
            builder.append(" coins, requires a roll of ");
            builder.append(getRequiredRoll(0, true));
            builder.append("+\n\tRare fish - ");
            builder.append(BASE_RARE_FISH_VALUE);
            builder.append(" coins, requires a roll of ");
            builder.append(getRequiredRoll(0, false));
            builder.append('+');
            builder.append(JOIN_COMMAND_PROMPT);
            return builder.toString();
        }

        int getRequiredRoll(int participants, boolean isEasy) {
            if (participants == 0) { participants = 1; }
            int requirement =  (isEasy ? BASE_EASY_ROLL_REQUIREMENT : BASE_HARD_ROLL_REQUIREMENT)
                - (ROLL_REDUCTION_PER_PARTICIPANT * (participants - 1));
            if (requirement < 0) {
                return 1;
            }
            return requirement;
        }

        String displayCurrentState() {
            StringBuilder builder = new StringBuilder();
            builder.append("\nBoat 1: ");
            builder.append(boat1Users.size());
            builder.append(" participant");
            builder.append(Casino.getPluralSuffix(boat1Users.size()));
            builder.append(". ");
            builder.append(BASE_COMMON_FISH_VALUE);
            builder.append(" coin Common on ");
            builder.append(getRequiredRoll(boat1Users.size(), true));
            builder.append("+, ");
            builder.append(BASE_UNCOMMON_FISH_VALUE);
            builder.append(" coin Uncommon on ");
            builder.append(getRequiredRoll(boat1Users.size(), false));
            builder.append("\nBoat 2: ");
            builder.append(boat2Users.size());
            builder.append(" participant");
            builder.append(Casino.getPluralSuffix(boat2Users.size()));
            builder.append(". ");
            builder.append(BASE_COMMON_FISH_VALUE);
            builder.append(" coin Common on ");
            builder.append(getRequiredRoll(boat2Users.size(), true));
            builder.append("+, ");
            builder.append(BASE_UNCOMMON_FISH_VALUE);
            builder.append(" coin Uncommon on ");
            builder.append(getRequiredRoll(boat2Users.size(), false));
            builder.append("\nBoat 3: ");
            builder.append(boat3Users.size());
            builder.append(" participant");
            builder.append(Casino.getPluralSuffix(boat3Users.size()));
            builder.append(". ");
            builder.append(BASE_UNCOMMON_FISH_VALUE);
            builder.append(" coin Common on ");
            builder.append(getRequiredRoll(boat3Users.size(), true));
            builder.append("+, ");
            builder.append(BASE_RARE_FISH_VALUE);
            builder.append(" coin Uncommon on ");
            builder.append(getRequiredRoll(boat3Users.size(), false));
            return builder.toString();
        }

        String createReminderMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Fishing event to ");
            builder.append(details.destination);
            builder.append(" ending in ");
            builder.append(EVENT_ENDING_REMINDER_WINDOW);
            builder.append(" minutes! Current event state:\n");
            builder.append(displayCurrentState());
            return builder.toString();
        }

        Queue<String> createResolutionMessages() {
            Queue<String> messageFrames = new LinkedList<>();
            StringBuilder builder = new StringBuilder();
            builder.append("Fishing event to ");
            builder.append(details.destination);
            builder.append(":\n\n");
            for (int seconds = COUNTDOWN_SECONDS; seconds > 0; seconds--) {
                messageFrames.add(builder.toString() + "Starting in " + seconds + " seconds");
            }

            long payout = 0;
            builder.append("Boat 1:");
            messageFrames.add(builder.toString());
            payout += resolveBoat(boat1Users, builder, messageFrames, false);
            builder.append("\nBoat 2:");
            messageFrames.add(builder.toString());
            payout += resolveBoat(boat2Users, builder, messageFrames, false);
            builder.append("\nBoat 3:");
            messageFrames.add(builder.toString());
            payout += resolveBoat(boat3Users, builder, messageFrames, true);

            builder.append("\n\nPayout: ");
            builder.append(payout);
            messageFrames.add(builder.toString());
            payout *= getPayoutMultiplier();
            builder.append(" x");
            builder.append(Stats.twoDecimals.format(getPayoutMultiplier()));
            builder.append(" = ");
            messageFrames.add(builder.toString());
            builder.append(payout);
            builder.append(" each");
            messageFrames.add(builder.toString());

            awardCharacterXp();

            // TODO: Log FishParticipants in DB

            return messageFrames;
        }

        int resolveBoat(List<FishParticipant> participants, StringBuilder builder,
                Queue<String> messageFrames, boolean deep) {
            int highestRoll = 0;
            int payout = 0;

            if (participants.isEmpty()) {
                builder.append("\n\tEmpty :(");
                messageFrames.add(builder.toString());
                return 0;
            }

            for (FishParticipant participant : participants) {
                builder.append("\n\t\t");
                builder.append(participant.nickname);
                builder.append(": ");
                messageFrames.add(builder.toString());
                participant.roll = HBMain.RNG_SOURCE.nextInt(100) + 1;
                if (participant.roll > highestRoll) { highestRoll = participant.roll; }
                builder.append('`');
                builder.append(TWO_DIGITS.format(participant.roll));
                builder.append('`');
                messageFrames.add(builder.toString());
            }
            builder.append("\n\tHighest Roll: `");
            builder.append(TWO_DIGITS.format(highestRoll));
            builder.append("`. You catch: ");
            messageFrames.add(builder.toString());
            if (highestRoll >= getRequiredRoll(boat1Users.size(), false)) {
                payout = deep ? BASE_RARE_FISH_VALUE : BASE_UNCOMMON_FISH_VALUE;
                builder.append(deep ? details.deepRare : details.shallowUncommon);
                builder.append(" (+");
                builder.append(payout);
                builder.append(" coins)");
                for (FishParticipant participant : participants) {
                    participant.gotRare = deep;
                    participant.gotUncommon = !deep;
                    if (participant.roll == highestRoll) { participant.wasHighest = true; }
                }
            } else if (highestRoll >= getRequiredRoll(boat1Users.size(), true)) {
                payout = deep ? BASE_UNCOMMON_FISH_VALUE : BASE_COMMON_FISH_VALUE;
                builder.append(deep ? details.deepUncommon : details.shallowCommon);
                builder.append(" (+");
                builder.append(payout);
                builder.append(" coins)");
                for (FishParticipant participant : participants) {
                    participant.gotUncommon = deep;
                    participant.gotCommon = !deep;
                    if (participant.roll == highestRoll) { participant.wasHighest = true; }
                }
            } else {
                builder.append(" Nothing :(");
            }
            messageFrames.add(builder.toString());
            return payout;
        }

        String createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character,
                long selection) {
            FishParticipant participant = new FishParticipant(user.getUid(), user.getNickname());
            if (selection == BOAT_1_VALUE) {
                boat1Users.add(participant);
            } else if (selection == BOAT_2_VALUE) {
                boat2Users.add(participant);
            } else if (selection == BOAT_3_VALUE) {
                boat3Users.add(participant);
            } else {
                return INVALID_SELECTION_MESSAGE;
            }

            StringBuilder builder = new StringBuilder();
            builder.append(user.getNickname());
            builder.append(" joined with ");
            builder.append(character.getDisplayName());
            builder.append(' ');
            builder.append(character.getCharacterStats().printStat(type.assocatedStat));
            builder.append(". Total payout bonus is now +");
            builder.append(ONE_DECIMAL.format(getPayoutBonusPercent()));
            builder.append("\nEvent state is now:");
            builder.append(displayCurrentState());
            return builder.toString();
        }

        String createAboutMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Fish events are cooperative events where players embark on boats to ");
            builder.append("catch fish. Three boats set out - two to shallow water and one to ");
            builder.append("deep water with rarer fish (but the same odds). At the conclusion of ");
            builder.append("the event all participants roll a number from 1-100, and the highest ");
            builder.append("roll in a boat determines its catch. The value of all fish caught is ");
            builder.append("combined and all participants receive that amount of coins. The ");
            builder.append("required rolls for fish start at ");
            builder.append(BASE_EASY_ROLL_REQUIREMENT);
            builder.append(" and ");
            builder.append(BASE_HARD_ROLL_REQUIREMENT);
            builder.append(" and decrease by 10 for every player in that boat after the first (");
            builder.append("e.g. the required highest roll for an uncommon catch in a boat ");
            builder.append("headed to shallow water with 3 people is ");
            builder.append(getRequiredRoll(3, false));
            builder.append(" or more).\n\nEither send everyone fishing in deep water to catch ");
            builder.append("the rare fish, or spread out for a higher potential payout!");
            return builder.toString();
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

    static FishEvent.FishEventDetails fetchFishEventDetails(long seed) {
        return new FishEvent.FishEventDetails("Test Land", "Common Fish", "Uncommon Fish",
            "Uncommon Fish", "Rare Fish");
    }


}