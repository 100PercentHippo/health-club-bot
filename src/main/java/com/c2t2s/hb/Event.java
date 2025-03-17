package com.c2t2s.hb;

import static java.util.Map.entry;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import java.util.TreeMap;

import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;

abstract class Event {

    static class EventFactory {

        // Hide default constructor
        private EventFactory() {}

        static Event createEvent(long server, EventType type) {
            if (type == EventType.FISH) {
                return new FishEvent(server, Duration.ofMinutes(2));
            } else {
                return new PickEvent(server, Duration.ofMinutes(2));
            }
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
        long cid;
        int characterMultiplier;
        long payout = 0;

        Participant(long uid, String nickname, long cid, int characterMultiplier) {
            this.uid = uid;
            this.nickname = nickname;
            this.cid = cid;
            this.characterMultiplier = characterMultiplier;
        }
    }

    protected EventType type;
    protected long server;
    protected Duration timeUntilResolution;
    private Map<Long, String> joinSelections;
    private List<HBMain.AutocompleteIdOption> options = new ArrayList<>();

    private Set<Long> participatingUsers = new HashSet<>();
    private boolean complete = false;
    protected long seed;
    protected int totalPayoutMultiplier = 0;
    protected boolean supportsUserSelections = false;

    static final Duration EVENT_ENDING_REMINDER_WINDOW = Duration.ofMinutes(1);
    static final Duration EVENT_LOCK_DURATION = Duration.ofSeconds(15);
    static final Duration NEW_EVENT_DELAY = Duration.ofMinutes(1);
    static final int COUNTDOWN_SECONDS = 10;
    static final String JOIN_COMMAND_PROMPT = "\n\nJoin with `/gacha event join` for coins, pulls, and character xp!";
    static final String INVALID_SELECTION_PREFIX = "Invalid selection: ";
    static final NumberFormat TWO_DIGITS = new DecimalFormat("00");
    static final NumberFormat ONE_DECIMAL = new DecimalFormat("0.0");
    static final HBMain.EmbedResponse.InlineBlock EMPTY_INLINE_BLOCK
        = new HBMain.EmbedResponse.InlineBlock("\u200B", "\u200B");

    protected Event(EventType type, long server, Duration timeUntilResolution,
            Map<Long, String> joinSelections) {
        this.type = type;
        this.server = server;
        this.timeUntilResolution = timeUntilResolution;
        this.joinSelections = joinSelections;

        for (Map.Entry<Long, String> entry : joinSelections.entrySet()) {
            // Options are displayed in reverse order in Discord, so add every element to the front
            // the member will be backwards, but Discord and the constructor arg can be correct
            options.add(0, new HBMain.AutocompleteIdOption(entry.getKey(), entry.getValue()));
        }
    }

    protected Event(EventType type, long server, Duration timeUntilResolution) {
        this.type = type;
        this.server = server;
        this.timeUntilResolution = timeUntilResolution;
        // options and joinSelections will be empty
    }

    EventType getType() {
        return type;
    }

    abstract String createEmbedTitle();

    abstract HBMain.EmbedResponse createInitialMessage();

    abstract String createAboutMessage();

    abstract HBMain.EmbedResponse createPublicUserJoinMessage(Casino.User user,
        Gacha.GachaCharacter character, long selection);

    abstract HBMain.EmbedResponse createPublicUserRejoinMessage(Casino.User user,
        Gacha.GachaCharacter character, long selection);

    abstract HBMain.EmbedResponse createReminderMessage();

    abstract Queue<HBMain.EmbedResponse> createResolutionMessages();

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
        } else if (!supportsUserSelections && !joinSelections.containsKey(selection)) {
            // If options was empty, any user input is valid
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
        if (joinMessage.getMessage().startsWith(INVALID_SELECTION_PREFIX)) {
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

    void createResolutionCountdown(Queue<HBMain.EmbedResponse> messageFrames) {
        for (int seconds = COUNTDOWN_SECONDS; seconds > 0; seconds--) {
            messageFrames.add(createEmbedResponse("Starting in " + seconds
                + " seconds"));
        }
    }

    ActionRow createAboutButton() {
        return ActionRow.of(Button.secondary(HBMain.GACHA_EVENT_PREFIX + ".about", "How do "
            + type.name().replace('_', ' ').toLowerCase() + " events work?"));
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

            FishParticipant(long uid, String nickname, long cid, int characterMultiplier) {
                super(uid, nickname, cid, characterMultiplier);
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
        String createEmbedTitle() {
            return "Fishing Event to " + details.destination;
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
            response.setButtons(createAboutButton());
            return response;
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
                builder.append(participants.size());
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
        HBMain.EmbedResponse createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character,
                long selection) {
            FishParticipant participant = new FishParticipant(user.getUid(), user.getNickname(),
                character.getId(), character.getCharacterStats().getStat(type.assocatedStat));
            if (selection == BOAT_1_VALUE) {
                boat1Users.add(participant);
            } else if (selection == BOAT_2_VALUE) {
                boat2Users.add(participant);
            } else if (selection == BOAT_3_VALUE) {
                boat3Users.add(participant);
            } else {
                return createErrorResponse(INVALID_SELECTION_PREFIX + "Unrecognized selection");
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
            return createEmbedResponse(builder.toString(), displayCurrentState())
                .setFooter(JOIN_COMMAND_PROMPT);
        }

        @Override
        HBMain.EmbedResponse createPublicUserRejoinMessage(Casino.User user,
                Gacha.GachaCharacter character, long selection) {
            // TODO
            return null;
        }

        @Override
        HBMain.EmbedResponse createReminderMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Ending in ");
            builder.append(EVENT_ENDING_REMINDER_WINDOW.toMinutes());
            builder.append(" minutes!\n\nCurrent fishing boat fleet:");
            return createEmbedResponse(builder.toString(), displayCurrentState())
                .setFooter(JOIN_COMMAND_PROMPT);
        }

        @Override
        Queue<HBMain.EmbedResponse> createResolutionMessages() {
            Queue<HBMain.EmbedResponse> messageFrames = new LinkedList<>();
            createResolutionCountdown(messageFrames);

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
            builder.append("`.\nYou catch: ");
            displayBlocks.peekLast().setBody(builder.toString());
            messageFrames.add(createEmbedResponse("", displayBlocks, true));
            if (highestRoll >= getRequiredRoll(boat1Users.size(), false)) {
                payout = deep ? BASE_RARE_FISH_VALUE : BASE_UNCOMMON_FISH_VALUE;
                builder.append(deep ? details.deepRare : details.shallowUncommon);
                builder.append(" \n+");
                builder.append(payout);
                builder.append(" coins");
                for (FishParticipant participant : participants) {
                    participant.gotRare = deep;
                    participant.gotUncommon = !deep;
                    if (participant.roll == highestRoll) { participant.wasHighest = true; }
                }
            } else if (highestRoll >= getRequiredRoll(boat1Users.size(), true)) {
                payout = deep ? BASE_UNCOMMON_FISH_VALUE : BASE_COMMON_FISH_VALUE;
                builder.append(deep ? details.deepUncommon : details.shallowCommon);
                builder.append(" \n+");
                builder.append(payout);
                builder.append(" coins");
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
    }

    private static class PickEvent extends Event {
        private static final int MIN_PICK_TARGETS = 15;
        private static final int AVERAGE_PICK_TARGETS = 40;
        private static final int PICK_TARGETS_STD_DEV = 10;
        private static final int POT_PER_PLAYER = 200;
        private static final long DEFAULT_SELECTION_FILLER = -10;

        static class PickEventDetails {
            int totalTargets;
            String destination;

            PickEventDetails(int totalTargets, String destination) {
                this.totalTargets = totalTargets;
                this.destination = destination;
            }
        }

        static class PickParticipant extends Participant {
            int targets;
            int successfulTargets = 0;
            int joinOrder;

            PickParticipant(long uid, String nickname, long cid, int characterMultiplier,
                    int targets, int joinOrder) {
                super(uid, nickname, cid, characterMultiplier);
                this.targets = targets;
                this.joinOrder = joinOrder;
            }
        }

        PickEventDetails details;
        List<PickParticipant> participants = new ArrayList<>();

        PickEvent(long server, Duration timeUntilResolution) {
            super(EventType.PICKPOCKET, server, timeUntilResolution,
                Map.ofEntries(entry(DEFAULT_SELECTION_FILLER,
                    "Enter a number of targets from 1-99")));
            supportsUserSelections = true;
            details = fetchPickEventDetails(seed);
        }

        int currentCoinsPerTarget() {
            int participantCount = participants.isEmpty() ? 1 : participants.size();
            return participantCount * POT_PER_PLAYER / details.totalTargets;
        }

        @Override
        String createEmbedTitle() {
            return "Pickpocketing event at " + details.destination;
        }

        @Override
        HBMain.EmbedResponse createInitialMessage() {
            HBMain.EmbedResponse response = createEmbedResponse(
                "A new Pickpocketing event is starting, destination: " + details.destination);
            response.addInlineBlock("Total targets:", Integer.toString(details.totalTargets));
            response.addInlineBlock("Coins per target:",
                currentCoinsPerTarget() + "\n(Increases as players join)");
            response.setFooter(JOIN_COMMAND_PROMPT);
            response.setButtons(createAboutButton());
            return response;
        }

        @Override
        String createAboutMessage() {
            return "During a pickpocketing event there are a number of unaware targets at the "
                + "given location. The amount of coins each target is carrying increases with the "
                + "number of event participants. Each player secretely picks a number of targets "
                + "to pickpocket, and receives coins from each target once the event ends. "
                + "However, if the event participants collectively pick more pockets than the "
                + "number of targets, the targets realize what is happening, and half of them run "
                + "away! If there aren't enough targets for everyone's selection, the "
                + "participants who selected the fewest targets go first*, and receive coins from "
                + "the targets they're able to pickpocket, while targets remain.\n\nExample 1: "
                + "There are 10 total targets with 40 coins each. Player A selects 2 targets and "
                + "Player B selects 7 targets. Player A will receive 80 coins while Player B "
                + "receives 280 coins.\nExample 2: There are again 10 total targets with 40 coins "
                + "each. Player A selects 4 targets and Player B selects 7 targets. As 11 targets "
                + "total have been selected, half the targets run away and 5 remain. Player A "
                + "goes first and receives 160 coins. Player B is only able to pickpocket 1 "
                + "target and receives 40 coins.\n\nWill you capitalize on other's cautious "
                + "selections, or overcommit and receive nothing?\n\n-# *Ties are split as evenly "
                + "as possible with any remainder lost. Example 3: There are 10 targets with 60 "
                + "coins each. Players A and B both select 6 and Player C selects 8. Too many "
                + "total targets are selected, so half the targets run away leaving 5 total "
                + "targets. Having selected the same value, Player A and B each get to "
                + "pickpocket 2 targets and each receive 120 coins. Player C gets 0 coins as no "
                + "targets remain.";
        }

        Queue<HBMain.EmbedResponse.InlineBlock> displayCurrentState() {
            StringBuilder builderOne = new StringBuilder();
            StringBuilder builderTwo = new StringBuilder();
            if (participants.isEmpty()) {
                builderOne.append("[Empty]");
                builderTwo.append("[None]");
            }
            for (PickParticipant participant : participants) {
                if (builderOne.length() != 0) {
                    builderOne.append('\n');
                    builderTwo.append('\n');
                }
                builderOne.append(participant.nickname);
                builderTwo.append("`??`");
            }
            builderOne.append("\n**Total Targets:**");
            builderTwo.append("\n`");
            builderTwo.append(details.totalTargets);
            builderTwo.append('`');
            return new LinkedList<>(Arrays.asList(
                new HBMain.EmbedResponse.InlineBlock("Participants:", builderOne.toString()),
                new HBMain.EmbedResponse.InlineBlock("Targets:", builderTwo.toString()),
                EMPTY_INLINE_BLOCK,
                new HBMain.EmbedResponse.InlineBlock("Coins per target:",
                    currentCoinsPerTarget() + "\n(Increases as players join)"),
                new HBMain.EmbedResponse.InlineBlock("Payout Multiplier:",
                    "+" + ONE_DECIMAL.format(getPayoutBonusPercent()))));
        }

        @Override
        HBMain.EmbedResponse createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character,
                long selection) {
            if (selection == DEFAULT_SELECTION_FILLER) {
                return createErrorResponse(INVALID_SELECTION_PREFIX + "Manually enter number of "
                    + "targets (autocomplete cannot be used for this event type)");
            } else if (selection < 1 || selection > 99) {
                return createErrorResponse(INVALID_SELECTION_PREFIX
                    + "Number of targets must be 1-99");
            }
            PickParticipant participant = new PickParticipant(user.getUid(), user.getNickname(),
                character.getId(), character.getCharacterStats().getStat(type.assocatedStat),
                (int)selection, participants.size());
            participants.add(participant);

            StringBuilder builder = new StringBuilder();
            builder.append(user.getNickname());
            builder.append(" joined with ");
            builder.append(character.getDisplayName());
            builder.append(' ');
            builder.append(character.getCharacterStats().printStat(type.assocatedStat));
            builder.append(".\nEach target now carries ");
            builder.append(currentCoinsPerTarget());
            builder.append(" coins. The payout multiplier is now +");
            builder.append(ONE_DECIMAL.format(getPayoutBonusPercent()));
            return createEmbedResponse(builder.toString(), displayCurrentState())
                .setFooter(JOIN_COMMAND_PROMPT);
        }

        @Override
        HBMain.EmbedResponse createPublicUserRejoinMessage(Casino.User user,
                Gacha.GachaCharacter character, long selection) {
            // TODO
            return null;
        }

        @Override
        HBMain.EmbedResponse createReminderMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Ending in ");
            builder.append(EVENT_ENDING_REMINDER_WINDOW.toMinutes());
            builder.append(" minutes!\n\nCurrent participants:");
            return createEmbedResponse(builder.toString(), displayCurrentState())
                .setFooter(JOIN_COMMAND_PROMPT);
        }

        @Override
        Queue<HBMain.EmbedResponse> createResolutionMessages() {
            Queue<HBMain.EmbedResponse> messageFrames = new LinkedList<>();
            createResolutionCountdown(messageFrames);

            if (participants.isEmpty()) {
                messageFrames.add(createEmbedResponse(
                    "Nobody joined the event. All the targets' wallets and purses are safe for now."));
                return messageFrames;
            }

            int totalTargetsSelected = 0;
            int coinsPerTarget = currentCoinsPerTarget();
            // If too many total targets are picked, payout in order of targets picked,
            // starting with participants who picked fewest
            Map<Integer, List<PickParticipant>> payoutOrder = new TreeMap<>();

            String description = "Total targets: " + details.totalTargets + "\nCoins per target: "
                + coinsPerTarget;
            StringBuilder builderOne = new StringBuilder();
            for (PickParticipant participant : participants) {
                if (builderOne.length() != 0) {
                    builderOne.append('\n');
                }
                builderOne.append(participant.nickname);
                if (!payoutOrder.containsKey(participant.targets)) {
                    payoutOrder.put(participant.targets, new ArrayList<>());
                }
                payoutOrder.get(participant.targets).add(participant);
            }
            builderOne.append("\n**Total:**");
            String userTargets = Casino.repeatString("`??`\n", participants.size());
            HBMain.EmbedResponse.InlineBlock column1
                = new HBMain.EmbedResponse.InlineBlock("Participants:", builderOne.toString());
            HBMain.EmbedResponse.InlineBlock column2
                = new HBMain.EmbedResponse.InlineBlock("Targets Hit:",
                    userTargets + '`' + totalTargetsSelected + '`');
            HBMain.EmbedResponse.InlineBlock column3
                = new HBMain.EmbedResponse.InlineBlock("Payout:", "");

            // Reveal everyone's targets in order smallest to largest
            messageFrames.add(createEmbedResponse(description, new LinkedList<>(Arrays.asList(
                column1, column2, column3)), true));
            for (Map.Entry<Integer, List<PickParticipant>> entry : payoutOrder.entrySet()) {
                for (PickParticipant participant : entry.getValue()) {
                    totalTargetsSelected += participant.targets;
                    // userTargets is blocks of `??`\n
                    userTargets = userTargets.substring(0, (participant.joinOrder * 5) + 1)
                        + TWO_DIGITS.format(participant.targets)
                        + userTargets.substring((participant.joinOrder * 5) + 3);
                    if (totalTargetsSelected > details.totalTargets) {
                        description = "Total targets: ~~" + details.totalTargets + "~~ "
                            + (details.totalTargets / 2) + ". Too many pockets picked! Half the "
                            + "targets ran away\nCoins per target: " + coinsPerTarget;
                        details.totalTargets /= 2;
                    }
                    column2 = new HBMain.EmbedResponse.InlineBlock("Targets Hit:",
                        userTargets + '`' + totalTargetsSelected + '`');
                    messageFrames.add(createEmbedResponse(description,
                        new LinkedList<>(Arrays.asList(column1, column2, column3)), true));
                }
            }

            // Payout participants in increasing order of targets selected
            int targetsPaid = 0;
            String column3text = Casino.repeatString("\n", participants.size());
            for (Map.Entry<Integer, List<PickParticipant>> entry : payoutOrder.entrySet()) {
                int targetsToPay;
                if (targetsPaid > details.totalTargets) {
                    targetsToPay = 0;
                } else if (targetsPaid + (entry.getKey() * entry.getValue().size())
                        > details.totalTargets) {
                    targetsToPay = (details.totalTargets - targetsPaid) / entry.getValue().size();
                    // Remainder is dropped
                    targetsPaid = details.totalTargets;
                } else {
                    targetsToPay = entry.getKey();
                    targetsPaid += targetsToPay;
                }

                for (PickParticipant participant : entry.getValue()) {
                    participant.successfulTargets = targetsToPay;
                    participant.payout = targetsToPay * (long)coinsPerTarget;

                    int index = 0;
                    for (int i = 0; i < participant.joinOrder; i++) {
                        index = column3text.indexOf('\n', index) + 1;
                    }
                    if (index < 0) {
                        // This shouldn't happen as there should always be an equal number
                        // of newlines and participants
                        System.out.println("Invalid index encountered while updating pick payout "
                            + "text. Text is:\n" + column3text);
                        continue;
                    }
                    column3text = column3text.substring(0, index) + participant.payout
                        + column3text.substring(index);
                    column3 = new HBMain.EmbedResponse.InlineBlock("Payout:", column3text);
                    messageFrames.add(createEmbedResponse(description,
                        new LinkedList<>(Arrays.asList(column1, column2, column3)), true));
                }
            }

            // Modify payout to include multiplier
            StringBuilder intermediatePayoutBuilder = new StringBuilder();
            StringBuilder finalPayoutBuilder = new StringBuilder();
            for (Map.Entry<Integer, List<PickParticipant>> entry : payoutOrder.entrySet()) {
                for (PickParticipant participant : entry.getValue()) {
                    String line = "";
                    if (intermediatePayoutBuilder.length() != 0) {
                        line = "\n";
                    }
                    line = line + participant.payout + " x "
                        + Stats.twoDecimals.format(getPayoutMultiplier());
                    intermediatePayoutBuilder.append(line);
                    finalPayoutBuilder.append(line);
                    participant.payout *= getPayoutMultiplier();
                    finalPayoutBuilder.append(" = ");
                    finalPayoutBuilder.append(participant.payout);

                    // TODO: Pay coins and log result
                }
            }
            column3 = new HBMain.EmbedResponse.InlineBlock("Payout:",
                intermediatePayoutBuilder.toString());
            messageFrames.add(createEmbedResponse(description,
                new LinkedList<>(Arrays.asList(column1, column2, column3)), true));
            column3 = new HBMain.EmbedResponse.InlineBlock("Payout:",
                finalPayoutBuilder.toString());
            messageFrames.add(createEmbedResponse(description,
                new LinkedList<>(Arrays.asList(column1, column2, column3)), true));

            // TODO: Log event completion

            return messageFrames;
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

    static PickEvent.PickEventDetails fetchPickEventDetails(long seed) {
        return new PickEvent.PickEventDetails(PickEvent.AVERAGE_PICK_TARGETS, "Test Land");
    }


}