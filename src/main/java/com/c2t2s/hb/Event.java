package com.c2t2s.hb;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.javacord.api.entity.message.Message;

import static java.util.Map.entry;

import java.awt.Color;
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
    static final Color MISC_EVENT_COLOR = new Color(255, 120, 17); // Orange
    enum EventType {
        FISH(EVENTTYPE_ID_FISH, GachaItems.ITEM_STAT.FISH, new Color(91, 170, 255)), // Light Blue
        ROB(EVENTTYPE_ID_ROB, GachaItems.ITEM_STAT.ROB, new Color(255, 213, 0)), // Gold
        WORK(EVENTTYPE_ID_WORK, GachaItems.ITEM_STAT.WORK, new Color(91, 41, 3)), // Brown
        PICKPOCKET(EVENTTYPE_ID_PICKPOCKET, GachaItems.ITEM_STAT.PICK, new Color(0, 136, 50)), // Money Green
        AVERAGE(EVENTTYPE_ID_AVERAGE, GachaItems.ITEM_STAT.MISC, MISC_EVENT_COLOR),
        SUPER_GUESS(EVENTTYPE_ID_SUPER_GUESS, GachaItems.ITEM_STAT.MISC, MISC_EVENT_COLOR),
        SUPER_SLOTS(EVENTTYPE_ID_SUPER_SLOTS, GachaItems.ITEM_STAT.MISC, MISC_EVENT_COLOR);

        int id;
        GachaItems.ITEM_STAT assocatedStat;
        Color embedColor;

        private EventType(int id, GachaItems.ITEM_STAT associatedStat, Color embedColor) {
            this.id = id;
            this.assocatedStat = associatedStat;
            this.embedColor = embedColor;
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

    abstract HBMain.EmbedResponse createInitialMessage();

    abstract HBMain.EmbedResponse createReminderMessage();

    abstract Queue<HBMain.EmbedResponse> createResolutionMessages();

    abstract HBMain.EmbedResponse createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character, long selection);

    abstract String createAboutMessage();

    abstract String createEmbedTitle();

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

    HBMain.EmbedResponse createEmbedResponse(String message) {
        return new HBMain.EmbedResponse(type.embedColor, message, createEmbedTitle());
    }

    HBMain.EmbedResponse createEmbedResponse(String message,
            Queue<HBMain.EmbedResponse.InlineBlock> inlineBlocks) {
        return createEmbedResponse(message, inlineBlocks, false);
    }

    HBMain.EmbedResponse createEmbedResponse(String message,
            Queue<HBMain.EmbedResponse.InlineBlock> inlineBlocks, boolean shouldCopy) {
        return createEmbedResponse(message).setInlineBlocks(inlineBlocks, shouldCopy);
    }

    HBMain.EmbedResponse createErrorResponse(String message) {
        return new HBMain.EmbedResponse(Color.RED, message);
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

        Queue<HBMain.EmbedResponse> messages = createResolutionMessages();
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

        HBMain.EmbedResponse joinMessage = createPublicUserJoinMessage(user, character, selection);
        if (joinMessage.getMessage().equals(INVALID_SELECTION_MESSAGE)) {
            totalPayoutMultiplier -= character.getCharacterStats().getStat(type.assocatedStat);
            participatingUsers.remove(uid);
            return joinMessage.getMessage();
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

        @Override
        HBMain.EmbedResponse createInitialMessage() {
            HBMain.EmbedResponse response = createEmbedResponse(
                "A new Fishing event is starting, destination: " + details.destination);

            StringBuilder builder = new StringBuilder();
            builder.append("Shallow water\nCommon fish - ");
            builder.append(BASE_COMMON_FISH_VALUE);
            builder.append(" coins, roll ");
            builder.append(getRequiredRoll(0, true));
            builder.append("+\nUncommon fish - ");
            builder.append(BASE_UNCOMMON_FISH_VALUE);
            builder.append(" coins, roll ");
            int bigRoll = getRequiredRoll(0, false);
            builder.append(bigRoll);
            if (bigRoll < 100) {
                builder.append('+');
            }
            response.addInlineBlock("Potential fish for Boats 1 & 2:", builder.toString());

            builder = new StringBuilder();
            builder.append("Deep water\nUncommon fish - ");
            builder.append(BASE_UNCOMMON_FISH_VALUE);
            builder.append(" coins, roll ");
            builder.append(getRequiredRoll(0, true));
            builder.append("+\nRare fish - ");
            builder.append(BASE_RARE_FISH_VALUE);
            builder.append(" coins, roll ");
            bigRoll = getRequiredRoll(0, false);
            builder.append(bigRoll);
            if (bigRoll < 100) {
                builder.append('+');
            }
            response.addInlineBlock("Potential fish for Boat 3:", builder.toString());

            response.setFooter(JOIN_COMMAND_PROMPT);
            return response;
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

        Queue<HBMain.EmbedResponse.InlineBlock> displayCurrentState() {
            return new LinkedList<>(Arrays.asList(
                new HBMain.EmbedResponse.InlineBlock("Boat 1",
                    printCurrentState(boat1Users, false)),
                new HBMain.EmbedResponse.InlineBlock("Boat 2",
                    printCurrentState(boat2Users, false)),
                new HBMain.EmbedResponse.InlineBlock("Boat 3",
                    printCurrentState(boat3Users, true)))
            );
        }

        String printCurrentState(List<FishParticipant> participants, boolean deep) {
            int easyFishValue = deep ? BASE_UNCOMMON_FISH_VALUE : BASE_COMMON_FISH_VALUE;
            int hardFishValue = deep ? BASE_RARE_FISH_VALUE : BASE_UNCOMMON_FISH_VALUE;
            String easyFishRarity = deep ? "Uncommon" : "Rare";
            String hardFishRarity = deep ? "Rare" : "Uncommon";

            StringBuilder builder = new StringBuilder();
            if (participants.isEmpty()) {
                builder.append("[Empty]");
            } else {
                builder.append(participants);
                builder.append(" participant");
                builder.append(Casino.getPluralSuffix(participants.size()));
                builder.append(':');
                for (FishParticipant participant : participants) {
                    builder.append('\n');
                    builder.append(participant.nickname);
                }
            }
            builder.append("\n\n");
            builder.append(easyFishValue);
            builder.append(" coin ");
            builder.append(easyFishRarity);
            builder.append(" on ");
            builder.append(getRequiredRoll(participants.size(), true));
            builder.append("+\n");
            builder.append(hardFishValue);
            builder.append(" coin ");
            builder.append(hardFishRarity);
            builder.append(" on ");
            int highRoll = getRequiredRoll(participants.size(), false);
            builder.append(highRoll);
            if (highRoll < 100) {
                builder.append('+');
            }
            return builder.toString();
        }

        @Override
        HBMain.EmbedResponse createReminderMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Ending in ");
            builder.append(EVENT_ENDING_REMINDER_WINDOW.toMinutes());
            builder.append(" minutes!\n\nCurrent fishing boat fleet:");
            return createEmbedResponse(builder.toString()).setInlineBlocks(displayCurrentState());
        }

        @Override
        Queue<HBMain.EmbedResponse> createResolutionMessages() {
            Queue<HBMain.EmbedResponse> messageFrames = new LinkedList<>();
            for (int seconds = COUNTDOWN_SECONDS; seconds > 0; seconds--) {
                messageFrames.add(createEmbedResponse("Starting in " + seconds
                    + " seconds"));
            }

            long payout = 0;
            Deque<HBMain.EmbedResponse.InlineBlock> inlineBlocks = new LinkedList<>();
            inlineBlocks.add(new HBMain.EmbedResponse.InlineBlock("Boat 1", ""));
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));
            payout += resolveBoat(boat1Users, inlineBlocks, messageFrames, false);
            inlineBlocks.add(new HBMain.EmbedResponse.InlineBlock("Boat 2", ""));
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));
            payout += resolveBoat(boat2Users, inlineBlocks, messageFrames, false);
            inlineBlocks.add(new HBMain.EmbedResponse.InlineBlock("Boat 3", ""));
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));
            payout += resolveBoat(boat3Users, inlineBlocks, messageFrames, true);

            inlineBlocks.add(new HBMain.EmbedResponse.InlineBlock("Payout:", ""));
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));
            StringBuilder payoutBuilder = new StringBuilder();
            payoutBuilder.append(payout);
            payoutBuilder.append(" coins");
            inlineBlocks.peekLast().setBody(payoutBuilder.toString());
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));
            payout *= getPayoutMultiplier();
            payoutBuilder.append("\nx");
            payoutBuilder.append(Stats.twoDecimals.format(getPayoutMultiplier()));
            inlineBlocks.peekLast().setBody(payoutBuilder.toString());
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));
            payoutBuilder.append("\n= ");
            payoutBuilder.append(payout);
            payoutBuilder.append(" each");
            inlineBlocks.peekLast().setBody(payoutBuilder.toString());
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));

            awardCharacterXp();

            // TODO: Log FishParticipants in DB

            return messageFrames;
        }

        int resolveBoat(List<FishParticipant> participants,
                Deque<HBMain.EmbedResponse.InlineBlock> displayBlocks,
                Queue<HBMain.EmbedResponse> messageFrames, boolean deep) {
            int highestRoll = 0;
            int payout = 0;

            if (participants.isEmpty()) {
                displayBlocks.peekLast().setBody("Empty :(");
                messageFrames.add(createEmbedResponse("", displayBlocks, true));
                return 0;
            }

            StringBuilder builder = new StringBuilder();
            for (FishParticipant participant : participants) {
                if (builder.length() != 0) {
                    builder.append('\n');
                }
                builder.append(participant.nickname);
                builder.append(": ");
                displayBlocks.peekLast().setBody(builder.toString());
                messageFrames.add(createEmbedResponse("", displayBlocks, true));
                participant.roll = HBMain.RNG_SOURCE.nextInt(100) + 1;
                if (participant.roll > highestRoll) { highestRoll = participant.roll; }
                builder.append('`');
                builder.append(TWO_DIGITS.format(participant.roll));
                builder.append('`');
                displayBlocks.peekLast().setBody(builder.toString());
                messageFrames.add(createEmbedResponse("", displayBlocks, true));
            }
            builder.append("\n\tHighest Roll: `");
            builder.append(TWO_DIGITS.format(highestRoll));
            builder.append("`. You catch: ");
            displayBlocks.peekLast().setBody(builder.toString());
            messageFrames.add(createEmbedResponse("", displayBlocks, true));
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
            displayBlocks.peekLast().setBody(builder.toString());
            messageFrames.add(createEmbedResponse("", displayBlocks, true));
            return payout;
        }

        @Override
        HBMain.EmbedResponse createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character,
                long selection) {
            FishParticipant participant = new FishParticipant(user.getUid(), user.getNickname());
            if (selection == BOAT_1_VALUE) {
                boat1Users.add(participant);
            } else if (selection == BOAT_2_VALUE) {
                boat2Users.add(participant);
            } else if (selection == BOAT_3_VALUE) {
                boat3Users.add(participant);
            } else {
                return createErrorResponse(INVALID_SELECTION_MESSAGE);
            }

            StringBuilder builder = new StringBuilder();
            builder.append(user.getNickname());
            builder.append(" joined with ");
            builder.append(character.getDisplayName());
            builder.append(' ');
            builder.append(character.getCharacterStats().printStat(type.assocatedStat));
            builder.append(".\nTotal payout bonus is now +");
            builder.append(ONE_DECIMAL.format(getPayoutBonusPercent()));
            builder.append("%\n\nFishing fleet is now:");
            return createEmbedResponse(builder.toString()).setInlineBlocks(displayCurrentState());
        }

        @Override
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

        @Override
        String createEmbedTitle() {
            return "Fishing Event to " + details.destination;
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