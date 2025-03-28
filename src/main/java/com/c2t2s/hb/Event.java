package com.c2t2s.hb;

import static java.util.Map.entry;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;

import com.c2t2s.hb.Gacha.GachaCharacter;
import com.c2t2s.hb.HBMain.EmbedResponse;
import com.c2t2s.hb.HBMain.EmbedResponse.InlineBlock;

abstract class Event {

    static class EventFactory {

        // Hide default constructor
        private EventFactory() {}

        static Event createEvent(long server, EventType type) {
            // if (type == EventType.FISH) {
            //     return new FishEvent(server, Duration.ofMinutes(2));
            // } else if (type == EventType.PICKPOCKET) {
            //     return new PickEvent(server, Duration.ofMinutes(2));
            // } else if (type == EventType.ROB) {
            //     return new RobEvent(server, Duration.ofMinutes(2));
            // } else if (type == EventType.SUPER_SLOTS) {
            //     return new SlotsEvent(server, Duration.ofMinutes(2));
            // }
            return new RobEvent(server, Duration.ofMinutes(2));
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
    static final int EVENTTYPE_ID_GIVEAWAY = 7;
    static final Color MISC_EVENT_COLOR = new Color(255, 120, 17); // Orange
    enum EventType {
        FISH(EVENTTYPE_ID_FISH, GachaItems.ITEM_STAT.FISH, new Color(91, 170, 255)), // Light Blue
        ROB(EVENTTYPE_ID_ROB, GachaItems.ITEM_STAT.ROB, new Color(255, 213, 0)), // Gold
        WORK(EVENTTYPE_ID_WORK, GachaItems.ITEM_STAT.WORK, new Color(91, 41, 3)), // Brown
        PICKPOCKET(EVENTTYPE_ID_PICKPOCKET, GachaItems.ITEM_STAT.PICK, new Color(0, 136, 50)), // Money Green
        AVERAGE(EVENTTYPE_ID_AVERAGE, GachaItems.ITEM_STAT.MISC, MISC_EVENT_COLOR),
        SUPER_GUESS(EVENTTYPE_ID_SUPER_GUESS, GachaItems.ITEM_STAT.MISC, MISC_EVENT_COLOR),
        SUPER_SLOTS(EVENTTYPE_ID_SUPER_SLOTS, GachaItems.ITEM_STAT.MISC, MISC_EVENT_COLOR),
        GIVEAWAY(EVENTTYPE_ID_GIVEAWAY, GachaItems.ITEM_STAT.MISC, MISC_EVENT_COLOR);

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

        String getNickname() { return nickname; }

        @Override
        public boolean equals(Object other) {
            return other instanceof Participant && ((Participant)other).uid == uid;
        }

        @Override
        public int hashCode() {
            return (int)uid;
        }
    }

    protected EventType type;
    protected long server;
    protected Duration timeUntilResolution;
    private Map<Long, String> joinSelections;
    private List<HBMain.AutocompleteIdOption> options = new ArrayList<>();

    protected Set<Long> participatingUsers = new HashSet<>();
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
    static final InlineBlock EMPTY_INLINE_BLOCK
        = new InlineBlock("\u200B", "\u200B");

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

    abstract EmbedResponse createInitialMessage();

    abstract String createAboutMessage();

    abstract EmbedResponse createPublicUserJoinMessage(Casino.User user,
        Gacha.GachaCharacter character, long selection);

    abstract EmbedResponse createPublicUserRejoinMessage(Casino.User user,
        Gacha.GachaCharacter character, long selection);

    abstract EmbedResponse createReminderMessage();

    abstract Queue<EmbedResponse> createResolutionMessages();

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

    EmbedResponse createEmbedResponse(String message) {
        return new EmbedResponse(type.embedColor, message, createEmbedTitle());
    }

    EmbedResponse createEmbedResponse(String message,
            Queue<InlineBlock> inlineBlocks) {
        return createEmbedResponse(message, inlineBlocks, false);
    }

    EmbedResponse createEmbedResponse(String message,
            Queue<InlineBlock> inlineBlocks, boolean shouldCopy) {
        return createEmbedResponse(message).setInlineBlocks(inlineBlocks, shouldCopy);
    }

    EmbedResponse createEmbedResponse(String message,
            Queue<InlineBlock> inlineBlocks, boolean shouldCopy, String footer) {
        return createEmbedResponse(message, inlineBlocks, shouldCopy).setFooter(footer);
    }

    EmbedResponse createErrorResponse(String message) {
        return new EmbedResponse(Color.RED, message);
    }

    Void initialize() {
        if (timeUntilResolution.compareTo(EVENT_ENDING_REMINDER_WINDOW) < 0) {
            CasinoServerManager.schedule(this::lockEvent, timeUntilResolution);
        } else {
            CasinoServerManager.schedule(this::postReminder,
                timeUntilResolution.minus(EVENT_ENDING_REMINDER_WINDOW));
        }

        EmbedResponse response = createInitialMessage();
        response.setFooter(JOIN_COMMAND_PROMPT);
        response.setButtons(createAboutButton());
        CasinoServerManager.sendEventMessage(server, response);
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

        Queue<EmbedResponse> messages = createResolutionMessages();
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
        } else if (getStat(character) < 0) {
            return "Unable to join event: Cannot join events with characters whose bonus is negative ("
                + character.getDisplayName() + " has a " + type.assocatedStat.getStatName()
                + " bonus of " + getStat(character);
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

        totalPayoutMultiplier += getStat(character);
        participatingUsers.add(uid);

        EmbedResponse joinMessage = createPublicUserJoinMessage(user, character, selection);
        if (joinMessage.getMessage().startsWith(INVALID_SELECTION_PREFIX)) {
            totalPayoutMultiplier -= getStat(character);
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

    void createResolutionCountdown(Queue<EmbedResponse> messageFrames) {
        for (int seconds = COUNTDOWN_SECONDS; seconds > 0; seconds--) {
            messageFrames.add(createEmbedResponse("Starting in " + seconds
                + " seconds"));
        }
    }

    ActionRow createAboutButton() {
        return ActionRow.of(Button.secondary(HBMain.GACHA_EVENT_PREFIX + ".about", "How do "
            + type.name().replace('_', ' ').toLowerCase() + " events work?"));
    }

    void appendJoinMessage(StringBuilder builder, Casino.User user,
            Gacha.GachaCharacter character) {
        appendJoinMessage(builder, user, character, false);
    }

    void appendJoinMessage(StringBuilder builder, Casino.User user,
            Gacha.GachaCharacter character,boolean rejoining) {
        builder.append(user.getNickname());
        builder.append(" ");
        if (rejoining) {
            builder.append("re");
        }
        builder.append("joined with ");
        builder.append(character.getDisplayName());
        builder.append(' ');
        builder.append(character.getCharacterStats().printStat(type.assocatedStat));
        builder.append(".\nTotal payout bonus is now +");
        builder.append(ONE_DECIMAL.format(getPayoutBonusPercent()));
        builder.append('%');
    }

    int getStat(GachaCharacter character) {
        return character.getCharacterStats().getStat(type.assocatedStat);
    }

    private static class FishEvent extends Event {
        private static final long BOAT_1_VALUE = 1;
        private static final long BOAT_2_VALUE = 2;
        private static final long BOAT_3_VALUE = 3;
        private static final int BASE_EASY_ROLL_REQUIREMENT = 50;
        private static final int BASE_HARD_ROLL_REQUIREMENT = 100;
        private static final int ROLL_REDUCTION_PER_PARTICIPANT = 10;
        private static final int BASE_COMMON_FISH_VALUE = 200;
        private static final int BASE_UNCOMMON_FISH_VALUE = 500;
        private static final int BASE_RARE_FISH_VALUE = 1000;

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
        EmbedResponse createInitialMessage() {
            EmbedResponse response = createEmbedResponse(
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

        Queue<InlineBlock> displayCurrentState() {
            return new LinkedList<>(Arrays.asList(
                new InlineBlock("Boat 1",
                    printCurrentState(boat1Users, false)),
                new InlineBlock("Boat 2",
                    printCurrentState(boat2Users, false)),
                new InlineBlock("Boat 3",
                    printCurrentState(boat3Users, true)))
            );
        }

        String printCurrentState(List<FishParticipant> participants, boolean deep) {
            int easyFishValue = deep ? BASE_UNCOMMON_FISH_VALUE : BASE_COMMON_FISH_VALUE;
            int hardFishValue = deep ? BASE_RARE_FISH_VALUE : BASE_UNCOMMON_FISH_VALUE;
            String easyFishRarity = deep ? "Uncommon" : "Common";
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
            builder.append(easyFishRarity);
            builder.append(" (");
            builder.append(easyFishValue);
            builder.append(") on ");
            builder.append(getRequiredRoll(participants.size(), true));
            builder.append("+\n");
            builder.append(hardFishRarity);
            builder.append(" (");
            builder.append(hardFishValue);
            builder.append(") on ");
            int highRoll = getRequiredRoll(participants.size(), false);
            builder.append(highRoll);
            if (highRoll < 100) {
                builder.append('+');
            }
            return builder.toString();
        }

        @Override
        EmbedResponse createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character,
                long selection) {
            FishParticipant participant = new FishParticipant(user.getUid(), user.getNickname(),
                character.getId(), getStat(character));
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
            appendJoinMessage(builder, user, character);
            builder.append("\n\nFishing fleet is now:");
            return createEmbedResponse(builder.toString(), displayCurrentState(), false,
                JOIN_COMMAND_PROMPT);
        }

        @Override
        EmbedResponse createPublicUserRejoinMessage(Casino.User user,
                Gacha.GachaCharacter character, long selection) {
            // TODO
            return null;
        }

        @Override
        EmbedResponse createReminderMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Ending in ");
            builder.append(EVENT_ENDING_REMINDER_WINDOW.toMinutes());
            builder.append(" minutes!\n\nCurrent fishing boat fleet:");
            return createEmbedResponse(builder.toString(), displayCurrentState(), false,
                JOIN_COMMAND_PROMPT);
        }

        @Override
        Queue<EmbedResponse> createResolutionMessages() {
            Queue<EmbedResponse> messageFrames = new LinkedList<>();
            createResolutionCountdown(messageFrames);

            long payout = 0;
            Deque<InlineBlock> inlineBlocks = new LinkedList<>();
            inlineBlocks.add(new InlineBlock("Boat 1:          ", ""));
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));
            payout += resolveBoat(boat1Users, inlineBlocks, messageFrames, false);
            inlineBlocks.add(new InlineBlock("Boat 2:          ", ""));
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));
            payout += resolveBoat(boat2Users, inlineBlocks, messageFrames, false);
            inlineBlocks.add(new InlineBlock("Boat 3:          ", ""));
            messageFrames.add(createEmbedResponse("", inlineBlocks, true));
            payout += resolveBoat(boat3Users, inlineBlocks, messageFrames, true);

            inlineBlocks.add(new InlineBlock("Payout:", ""));
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
                Deque<InlineBlock> displayBlocks,
                Queue<EmbedResponse> messageFrames, boolean deep) {
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
        static final int MIN_PICK_TARGETS = 15;
        static final int AVERAGE_PICK_TARGETS = 40;
        static final int PICK_TARGETS_STD_DEV = 10;
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
            super(EventType.PICKPOCKET, server, timeUntilResolution, new HashMap<>());
            supportsUserSelections = true;
            details = fetchPickEventDetails(seed);
        }

        int currentCoinsPerTarget() {
            int participantCount = participants.isEmpty() ? 1 : participants.size();
            return participantCount * POT_PER_PLAYER / details.totalTargets;
        }

        @Override
        String createEmbedTitle() {
            return "Pickpocketing Event at " + details.destination;
        }

        @Override
        EmbedResponse createInitialMessage() {
            EmbedResponse response = createEmbedResponse(
                "A new Pickpocketing event is starting, destination: " + details.destination);
            response.addInlineBlock("Total targets:", Integer.toString(details.totalTargets));
            response.addInlineBlock("Coins per target:",
                currentCoinsPerTarget() + "\n(Increases as players join)");
            return response;
        }

        @Override
        String createAboutMessage() {
            return "During a pickpocketing event there are a number of unsuspecting targets at the "
                + "given location. The amount of coins each target is carrying increases with the "
                + "number of event participants. Each player secretly picks a number of targets "
                + "to pickpocket, and receives coins from each target once the event ends. "
                + "However, if the event participants collectively pick more pockets than the "
                + "number of targets, the targets realize what is happening, and half of them run "
                + "away! If there aren't enough targets for everyone's selection, the "
                + "participants who selected the fewest targets go first†, and receive coins from "
                + "the targets they're able to pickpocket, while targets remain.\n\nExample 1: "
                + "There are 10 total targets with 40 coins each. Player A selects 2 targets and "
                + "Player B selects 7 targets. Player A will receive 80 coins while Player B "
                + "receives 280 coins.\nExample 2: There are again 10 total targets with 40 coins "
                + "each. Player A selects 4 targets and Player B selects 7 targets. As 11 targets "
                + "total have been selected, half the targets run away and 5 remain. Player A "
                + "goes first and receives 160 coins. Player B is only able to pickpocket 1 "
                + "target and receives 40 coins.\n\nWill you capitalize on other's cautious "
                + "selections, or overcommit and receive nothing?\n\n-# †Ties are split as evenly "
                + "as possible with any remainder lost. Imagine there are 10 targets with 60 "
                + "coins each. Players A and B both select 6 and Player C selects 9. Too many "
                + "total targets (21) are selected, so half the targets run away leaving 5 total "
                + "targets. Having selected the same value, Player A and B each get to "
                + "pickpocket 2 targets and each receive 120 coins. Player C gets 0 coins as no "
                + "targets remain.";
        }

        Queue<InlineBlock> displayCurrentState() {
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
                new InlineBlock("Participants:", builderOne.toString()),
                new InlineBlock("Targets:", builderTwo.toString()),
                EMPTY_INLINE_BLOCK,
                new InlineBlock("Coins per target:",
                    currentCoinsPerTarget() + "\n(Increases as players join)"),
                new InlineBlock("Payout Multiplier:",
                    "+" + ONE_DECIMAL.format(getPayoutBonusPercent()) + "%")));
        }

        @Override
        EmbedResponse createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character,
                long selection) {
            if (selection < 1 || selection > 99) {
                return createErrorResponse(INVALID_SELECTION_PREFIX
                    + "Number of targets must be 1-99");
            }
            PickParticipant participant = new PickParticipant(user.getUid(), user.getNickname(),
                character.getId(), getStat(character), (int)selection, participants.size());
            participants.add(participant);

            StringBuilder builder = new StringBuilder();
            appendJoinMessage(builder, user, character);
            return createEmbedResponse(builder.toString(), displayCurrentState(), false,
                JOIN_COMMAND_PROMPT);
        }

        @Override
        EmbedResponse createPublicUserRejoinMessage(Casino.User user,
                Gacha.GachaCharacter character, long selection) {
            // TODO
            return null;
        }

        @Override
        EmbedResponse createReminderMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Ending in ");
            builder.append(EVENT_ENDING_REMINDER_WINDOW.toMinutes());
            builder.append(" minutes!\n\nCurrent participants:");
            return createEmbedResponse(builder.toString(), displayCurrentState(), false,
                JOIN_COMMAND_PROMPT);
        }

        @Override
        Queue<EmbedResponse> createResolutionMessages() {
            Queue<EmbedResponse> messageFrames = new LinkedList<>();
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
            InlineBlock column1
                = new InlineBlock("Participants:", builderOne.toString());
            InlineBlock column2
                = new InlineBlock("Targets Hit:",
                    userTargets + '`' + totalTargetsSelected + '`');
            InlineBlock column3
                = new InlineBlock("Payout:", "");

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
                    column2 = new InlineBlock("Targets Hit:",
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
                    column3 = new InlineBlock("Payout:", column3text);
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
            column3 = new InlineBlock("Payout:",
                intermediatePayoutBuilder.toString());
            messageFrames.add(createEmbedResponse(description,
                new LinkedList<>(Arrays.asList(column1, column2, column3)), true));
            column3 = new InlineBlock("Payout:",
                finalPayoutBuilder.toString());
            messageFrames.add(createEmbedResponse(description,
                new LinkedList<>(Arrays.asList(column1, column2, column3)), true));

            // TODO: Log event completion

            return messageFrames;
        }
    }

    static class RobEvent extends Event {
        private static final int POT_PER_PLAYER = 175;
        private static final int TOO_FEW_PLAYERS_PAYOUT = 150;
        private static final double LOUD_PLAYER_PORTION = 0.4;
        private static final double ALL_QUIET_BONUS = 0.2;
        private static final int MINIMUM_PARTICIPANTS = 3;
        private static final long QUIET_SELECTION_ID = 0;
        private static final long LOUD_SELECTION_ID = 1;

        static class RobEventDetails {
            String destination;
            String target;

            RobEventDetails(String destination, String target) {
                this.destination = destination;
                this.target = target;
            }
        }

        static class RobParticipant extends Participant {
            boolean isQuiet;

            RobParticipant(long uid, String nickname, long cid, int characterMultiplier,
                    boolean isQuiet) {
                super(uid, nickname, cid, characterMultiplier);
                this.isQuiet = isQuiet;
            }
        }

        RobEventDetails details;
        List<RobParticipant> participants = new ArrayList<>();

        RobEvent(long server, Duration timeUntilResolution) {
            super(EventType.ROB, server, timeUntilResolution,
                Map.ofEntries(entry(QUIET_SELECTION_ID,"Quiet: Stick to the plan - receive a "
                    + "bonus if everybody else is quiet as well"), entry(LOUD_SELECTION_ID,
                    "Loud: Betray the team to grab loot early and run - earn bonus coins so long "
                    + "as nobody else goes loud")));
            details = fetchRobEventDetails(seed);
            // TODO: Remove Test Players after testing
            participants.add(new RobParticipant(0, "Test Player 1", 0, 10, true));
            participants.add(new RobParticipant(0, "Test Player 2", 0, 10, true));
        }

        @Override
        String createEmbedTitle() {
            return "Rob Event to steal " + details.target + " from " + details.destination;
        }

        @Override
        EmbedResponse createInitialMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Robert is putting together a crew to steal ");
            builder.append(details.target);
            builder.append(" from ");
            builder.append(details.destination);
            builder.append(". He has the perfect plan to get in, grab every valuable, and get ");
            builder.append("out all without being seen, and is offering you a bonus if ");
            builder.append("everything goes according to plan (for once). However, you're ");
            builder.append("pretty sure if you go loud you can catch everyone by surprise, grab ");
            builder.append("a few valuables for yourself, and make a break for it before the ");
            builder.append("crew knows what happened. There won't be much to grab if multiple ");
            builder.append("people go loud, and the bonus is pretty tempting, so this time ");
            builder.append("you're sticking to the plan.\n\n-# unless?\n\n");

            EmbedResponse response = createEmbedResponse(builder.toString());
            return response;
        }

        @Override
        String createAboutMessage() {
            return "When joining a Rob Event, participants choose to either stay quiet and stick "
                + "to Robert's plan or go loud and betray the team. If the entire crew stays "
                + "quiet the take is split evenly and Robert gives everyone a "
                + TWO_DIGITS.format(100 * ALL_QUIET_BONUS)
                + "% bonus for the successful heist. If you choose to go loud, you steal "
                + TWO_DIGITS.format(100 * LOUD_PLAYER_PORTION) + "% of the total take, leaving "
                + "the rest to be split among the participants that were quiet. If more than "
                + "one participant chooses to go loud, they split the "
                + TWO_DIGITS.format(100 * LOUD_PLAYER_PORTION) + "%, likely resulting in less "
                + "profit than staying stealthy.\n\nThe total take increases as participants "
                + "join the crew (more hands to carry loot). At least " + MINIMUM_PARTICIPANTS
                + " participants are needed to start the heist, but Robert will pay a reduced "
                + "amount (" + TOO_FEW_PLAYERS_PAYOUT + " coins) to participants if not enough "
                + "join.";
        }

        int getHeistTake() {
            int participantCount = participants.size();
            if (participantCount == 0) { participantCount = 1; }
            return (int)(POT_PER_PLAYER * participantCount * getPayoutMultiplier());
        }

        Queue<InlineBlock> printParticipants() {
            StringBuilder builder = new StringBuilder();
            if (participants.isEmpty()) {
                builder.append("[Empty]");
            } else {
                for (Participant participant : participants) {
                    if (builder.length() != 0) { builder.append('\n'); }
                    builder.append(participant.nickname);
                }
            }
            Queue<InlineBlock> output = new LinkedList<>();
            output.add(new InlineBlock("Heist Crew:", builder.toString()));
            return output;
        }

        void addMoreParticipantsNeededMessage(StringBuilder builder) {
            if (participants.size() >= MINIMUM_PARTICIPANTS) {
                return;
            }
            int participantsNeeded = MINIMUM_PARTICIPANTS - participants.size();
            builder.append("\n\nRobert needs at least ");
            builder.append(participantsNeeded);
            builder.append(" more participant");
            builder.append(Casino.getPluralSuffix(participantsNeeded));
            builder.append(" to pull off the heist");
        }

        @Override
        EmbedResponse createPublicUserJoinMessage(Casino.User user,
                Gacha.GachaCharacter character, long selection) {
            if (!(selection == QUIET_SELECTION_ID || selection == LOUD_SELECTION_ID)) {
                return createErrorResponse(INVALID_SELECTION_PREFIX + "Unrecognized selection");
            }
            RobParticipant participant = new RobParticipant(user.getUid(), user.getNickname(),
                character.getId(), getStat(character), selection == QUIET_SELECTION_ID);
            participants.add(participant);

            StringBuilder builder = new StringBuilder();
            appendJoinMessage(builder, user, character);
            builder.append("\n\nTotal heist value is now: ");
            builder.append(getHeistTake());
            addMoreParticipantsNeededMessage(builder);
            return createEmbedResponse(builder.toString(), printParticipants(), false,
                JOIN_COMMAND_PROMPT);
        }

        @Override
        EmbedResponse createPublicUserRejoinMessage(Casino.User user,
                Gacha.GachaCharacter character, long selection) {
            RobParticipant newParticipant = new RobParticipant(user.getUid(), user.getNickname(),
                character.getId(), getStat(character), selection == QUIET_SELECTION_ID);
            if (!(selection == QUIET_SELECTION_ID || selection == LOUD_SELECTION_ID)) {
                return createErrorResponse(INVALID_SELECTION_PREFIX + "Unrecognized selection");
            } else if (!participants.contains(newParticipant)) {
                return createErrorResponse(INVALID_SELECTION_PREFIX
                    + "Unable to find previous entry");
            }
            RobParticipant oldParticipant = participants.get(participants.indexOf(newParticipant));
            if (oldParticipant.isQuiet == newParticipant.isQuiet
                    && oldParticipant.cid == newParticipant.cid) {
                return createErrorResponse(INVALID_SELECTION_PREFIX
                    + "You already joined with that character");
            }

            participants.remove(oldParticipant);
            participants.add(newParticipant);

            StringBuilder builder = new StringBuilder();
            if (oldParticipant.cid == newParticipant.cid) {
                builder.append(user.getNickname());
                builder.append(" changed their selection :face_with_raised_eyebrow:");
            } else {
                appendJoinMessage(builder, user, character, true);
            }
            addMoreParticipantsNeededMessage(builder);
            builder.append("\n\nTotal heist value is now: ");
            builder.append(getHeistTake());
            return createEmbedResponse(builder.toString(), printParticipants(), false,
                JOIN_COMMAND_PROMPT);
        }

        @Override
        EmbedResponse createReminderMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("The heist is starting in ");
            builder.append(EVENT_ENDING_REMINDER_WINDOW.toMinutes());
            builder.append(" minutes!\n\nThe total heist value is currently: ");
            builder.append(getHeistTake());
            addMoreParticipantsNeededMessage(builder);
            return createEmbedResponse(builder.toString(), printParticipants(), false,
                JOIN_COMMAND_PROMPT);
        }

        @Override
        Queue<EmbedResponse> createResolutionMessages() {
            Queue<EmbedResponse> messageFrames = new LinkedList<>();
            StringBuilder builder = new StringBuilder();
            String robert = "Robert is monitoring the heist :neutral_face:";
            createResolutionCountdown(messageFrames);

            if (participants.size() < MINIMUM_PARTICIPANTS) {
                builder.append("Robert was not able to assemble a full crew, but he pays those ");
                builder.append("who tried to join ");
                builder.append(TOO_FEW_PLAYERS_PAYOUT);
                builder.append(" coins.");
                robert = "Robert is sad :slight_frown:";

                // TODO: Log event and payout

                Queue<InlineBlock> crewBlock = printParticipants();
                messageFrames.add(createEmbedResponse(builder.toString(), crewBlock, true)
                    .addInlineBlock("Payout:", "").setFooter(robert));
                StringBuilder payoutString = new StringBuilder(
                    Integer.toString(TOO_FEW_PLAYERS_PAYOUT));
                messageFrames.add(createEmbedResponse(builder.toString(), crewBlock, true)
                    .addInlineBlock("Payout:", Casino.repeatString(payoutString.toString(),
                        participants.size())).setFooter(robert));
                payoutString.append(" x ");
                payoutString.append(Stats.twoDecimals.format(getPayoutMultiplier()));
                messageFrames.add(createEmbedResponse(builder.toString(), crewBlock, true)
                    .addInlineBlock("Payout:", Casino.repeatString(payoutString.toString(),
                        participants.size())).setFooter(robert));
                payoutString.append(" = ");
                payoutString.append((int)(TOO_FEW_PLAYERS_PAYOUT * getPayoutMultiplier()));
                messageFrames.add(createEmbedResponse(builder.toString(), crewBlock, true)
                    .addInlineBlock("Payout:", Casino.repeatString(payoutString.toString(),
                        participants.size())).setFooter(robert));
                return messageFrames;
            }

            List<RobParticipant> quietParticipants = new ArrayList<>();
            List<RobParticipant> loudParticipants = new ArrayList<>();
            for (RobParticipant participant : participants) {
                if (participant.isQuiet) {
                    quietParticipants.add(participant);
                } else {
                    loudParticipants.add(participant);
                }
            }

            builder.append("Total heist take: ");
            messageFrames.add(createEmbedResponse(builder.toString()).setFooter(robert));
            builder.append(POT_PER_PLAYER * participants.size());
            messageFrames.add(createEmbedResponse(builder.toString()).setFooter(robert));
            builder.append(" x ");
            builder.append(Stats.twoDecimals.format(getPayoutMultiplier()));
            messageFrames.add(createEmbedResponse(builder.toString()).setFooter(robert));
            builder.append(" = ");
            int totalPayout = getHeistTake();
            builder.append(totalPayout);
            String description = builder.toString();

            InlineBlock quietBlock = new InlineBlock("Quiet Crew:", "");
            InlineBlock quietPayout = new InlineBlock("Quiet Payout: " + totalPayout, "");
            builder = new StringBuilder();
            Queue<InlineBlock> blocks = new LinkedList<>(Arrays.asList(quietBlock, quietPayout,
                EMPTY_INLINE_BLOCK));
            messageFrames.add(createEmbedResponse(description, blocks, true, robert));

            if (quietParticipants.isEmpty()) {
                robert = "Robert is furious :rage:";
                quietBlock.setBody("[Empty]");
                messageFrames.add(createEmbedResponse(description, blocks, true, robert));
            } else {
                for (RobParticipant participant : quietParticipants) {
                    if (builder.length() != 0) { builder.append('\n'); }
                    builder.append(participant.getNickname());
                    quietBlock.setBody(builder.toString());
                    messageFrames.add(createEmbedResponse(description, blocks, true,
                        robert));
                }
            }

            InlineBlock loudBlock = new InlineBlock("Loud Crew:", "");
            InlineBlock loudPayout = new InlineBlock("Loud Payout: ", "");
            builder = new StringBuilder();
            blocks.add(loudBlock);
            blocks.add(loudPayout);
            blocks.add(EMPTY_INLINE_BLOCK);
            int loudCut = 0;
            messageFrames.add(createEmbedResponse(description, blocks, true, robert));

            if (loudParticipants.isEmpty()) {
                robert = "Robert is pleased :slight_smile:";
                loudBlock.setBody("[Empty]");
                loudPayout.setTitle("Loud Payout: 0");
                messageFrames.add(createEmbedResponse(description, blocks, true, robert));
            } else {
                if (!quietParticipants.isEmpty()) {
                    robert = "Robert is upset :angry:";
                }
                loudCut = (int)(totalPayout * LOUD_PLAYER_PORTION);
                totalPayout -= loudCut;
                quietPayout.setTitle("Total Payout: " + totalPayout);
                loudPayout.setTitle("Loud Payout: " + loudCut);
                for (RobParticipant participant : loudParticipants) {
                    if (builder.length() != 0) { builder.append('\n'); }
                    builder.append(participant.getNickname());
                    loudBlock.setBody(builder.toString());
                    messageFrames.add(createEmbedResponse(description, blocks, true,
                        robert));
                }
            }

            if (!quietParticipants.isEmpty()) {
                int quietCut = totalPayout / quietParticipants.size();
                builder = new StringBuilder(Integer.toString(quietCut));
                quietPayout.setBody(Casino.repeatString(builder.toString() + '\n',
                    quietParticipants.size()));
                messageFrames.add(createEmbedResponse(description, blocks, true, robert));
                builder.append(" x ");
                builder.append(Stats.twoDecimals.format(getPayoutMultiplier()));
                quietPayout.setBody(Casino.repeatString(builder.toString() + '\n',
                    quietParticipants.size()));
                messageFrames.add(createEmbedResponse(description, blocks, true, robert));
                quietCut *= getPayoutMultiplier();
                builder.append(" = ");
                builder.append(quietCut);
                quietPayout.setBody(Casino.repeatString(builder.toString() + '\n',
                    quietParticipants.size()));
                messageFrames.add(createEmbedResponse(description, blocks, true, robert));

                if (loudParticipants.isEmpty()) {
                    // TODO: Add 20%
                }

                // TODO: Log player outcomes
            }

            if (!loudParticipants.isEmpty()) {
                loudCut = loudCut / loudParticipants.size();
                builder = new StringBuilder(Integer.toString(loudCut));
                loudPayout.setBody(Casino.repeatString(builder.toString() + '\n',
                    loudParticipants.size()));
                messageFrames.add(createEmbedResponse(description, blocks, true, robert));
                builder.append(" x ");
                builder.append(Stats.twoDecimals.format(getPayoutMultiplier()));
                loudPayout.setBody(Casino.repeatString(builder.toString() + '\n',
                    loudParticipants.size()));
                messageFrames.add(createEmbedResponse(description, blocks, true, robert));
                loudCut *= getPayoutMultiplier();
                builder.append(" = ");
                builder.append(loudCut);
                loudPayout.setBody(Casino.repeatString(builder.toString() + '\n',
                    loudParticipants.size()));
                messageFrames.add(createEmbedResponse(description, blocks, true, robert));

                // TODO: Log player outcomes
            }

            // TODO: Log event

            return messageFrames;
        }
    }

    static class SlotsEvent extends Event {
        private static final int ROWS = 10;
        private static final int COLUMNS = 10;
        private static final int COINS_PER_FRUIT = 5;
        private static final int COINS_PER_GROUP = 10;
        private static final double DIAMOND_CHANCE = 0.01;
        private static final double FRUIT_CHANCE = 0.2;

        private static final long CHERRY_ID = 0;
        private static final long ORANGE_ID = 1;
        private static final long LEMON_ID = 2;
        private static final long BLUEBERRY_ID = 3;
        private static final long GRAPE_ID = 4;
        private static final long DIAMOND_ID = 5;
        private static final String TEAM_CHERRY_NAME = "Team Cherry";
        private static final String TEAM_ORANGE_NAME = "Team Orange";
        private static final String TEAM_LEMON_NAME = "Team Lemon";
        private static final String TEAM_BLUEBERRY_NAME = "Team Blueberry";
        private static final String TEAM_GRAPE_NAME = "Team Grape";
        private static final String PLACEHOLDER = ":black_large_square:";
        private static final String EMPTY_ROW = "\n" + Casino.repeatString(PLACEHOLDER, COLUMNS);

        private Map<Long, SlotsTeam> teams = Map.ofEntries(
            entry(CHERRY_ID, new SlotsTeam(TEAM_CHERRY_NAME, ":cherries:")),
            entry(ORANGE_ID, new SlotsTeam(TEAM_ORANGE_NAME, ":tangerine:")),
            entry(LEMON_ID, new SlotsTeam(TEAM_LEMON_NAME, ":lemon:")),
            entry(BLUEBERRY_ID, new SlotsTeam(TEAM_BLUEBERRY_NAME, ":blueberries:")),
            entry(GRAPE_ID, new SlotsTeam(TEAM_GRAPE_NAME, ":grapes:")));

        static class SlotsTeam {
            String teamName;
            String emote;
            List<Participant> members = new ArrayList<>();
            long payout = 0;

            SlotsTeam(String teamName, String emote) {
                this.teamName = teamName;
                this.emote = emote;
            }

            String getDisplayName() {
                return emote + " " + teamName;
            }
        }

        SlotsEvent(long server, Duration timeUntilResolution) {
            super(EventType.SUPER_SLOTS, server, timeUntilResolution,
                Map.ofEntries(entry(CHERRY_ID, TEAM_CHERRY_NAME),
                              entry(ORANGE_ID, TEAM_ORANGE_NAME),
                              entry(LEMON_ID, TEAM_LEMON_NAME),
                              entry(BLUEBERRY_ID, TEAM_BLUEBERRY_NAME),
                              entry(GRAPE_ID, TEAM_GRAPE_NAME)));
        }

        @Override
        String createEmbedTitle() {
            return "Super Slots!";
        }

        @Override
        EmbedResponse createInitialMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("A new Miscellaneous event is starting: Super Slots!\n\nPick a team, ");
            builder.append("and earn coins when your fruit shows up on the giant 10x10 slot ");
            builder.append("machine! The teams are:");
            for (Map.Entry<Long, SlotsTeam> entry : teams.entrySet()) {
                builder.append('\n');
                builder.append(entry.getValue().getDisplayName());
            }
            return createEmbedResponse(builder.toString());
        }

        @Override
        String createAboutMessage() {
            return "At the end of the event a 10x10 board will be filled with the same fruits "
                + "from `/slots`: :cherries:, :tangerine:, :lemon:, :blueberries:, and :grapes:. "
                + "Each participant picks a team, and receives 1 coin every time that team's "
                + "fruit appears on the slots board. Bonus coins are awarded for groups of fruit "
                + "- multiple adjacent copies of the team's fruit. Diamonds also appear rarely, "
                + "and award 1 coin to all teams. There's no limit to the number of participants "
                + "that can join a given team, but whichever team *you* join is decidedly "
                + "cooler than the others and is definitely going to earn more coins.";
        }

        Queue<InlineBlock> displayCurrentState() {
            return displayCurrentState(false);
        }

        Queue<InlineBlock> displayCurrentState(boolean resolving) {
            Queue<InlineBlock> blocks = new LinkedList<>();
            for (Map.Entry<Long, SlotsTeam> entry : teams.entrySet()) {
                SlotsTeam team = entry.getValue();
                blocks.add(new InlineBlock(team.getDisplayName() + ":"
                        + (resolving ? " " + team.payout : ""),
                    team.members.isEmpty() ? "[Empty]" : team.members.stream()
                        .map(Participant::getNickname).collect(Collectors.joining("\n"))));
            }
            blocks.add(EMPTY_INLINE_BLOCK);
            return blocks;
        }

        @Override
        EmbedResponse createPublicUserJoinMessage(Casino.User user,
                Gacha.GachaCharacter character, long selection) {
            if (!teams.containsKey(selection)) {
                return createErrorResponse(INVALID_SELECTION_PREFIX + "Unrecognized selection");
            }
            SlotsTeam team = teams.get(selection);
            team.members.add(new Participant(user.getUid(), user.getNickname(),
                character.getId(), getStat(character)));

            StringBuilder builder = new StringBuilder();
            builder.append(user.getNickname());
            builder.append(" brought ");
            builder.append(character.getDisplayName());
            builder.append(" and joined ");
            builder.append(team.teamName);
            builder.append("!\nTotal payout bonus is now +");
            builder.append(ONE_DECIMAL.format(getPayoutBonusPercent()));
            builder.append("%");
            return createEmbedResponse(builder.toString(), displayCurrentState())
                .setFooter(JOIN_COMMAND_PROMPT);
        }

        @Override
        EmbedResponse createPublicUserRejoinMessage(Casino.User user,
                Gacha.GachaCharacter character, long selection) {
            if (!teams.containsKey(selection)) {
                return createErrorResponse(INVALID_SELECTION_PREFIX + "Unrecognized selection");
            }

            Participant oldParticipant = null;
            long oldTeam = -1;
            Participant newParticipant = new Participant(user.getUid(), user.getNickname(),
                character.getId(), getStat(character));
            for (Map.Entry<Long, SlotsTeam> entry : teams.entrySet()) {
                List<Participant> members = entry.getValue().members;
                if (members.contains(newParticipant)) {
                    oldTeam = entry.getKey();
                    oldParticipant = members.get(members.indexOf(newParticipant));
                    break;
                }
            }

            if (oldTeam == -1 || oldParticipant == null) {
                return createErrorResponse(INVALID_SELECTION_PREFIX
                    + "Unable to find previous entry");
            } else if (oldTeam == selection && character.getId() == oldParticipant.cid) {
                return createErrorResponse(INVALID_SELECTION_PREFIX
                    + "You already joined that team with that character");
            }

            teams.get(oldTeam).members.remove(oldParticipant);
            teams.get(selection).members.add(newParticipant);

            StringBuilder builder = new StringBuilder();
            if (oldTeam == selection) {
                appendJoinMessage(builder, user, character, true);
            } else {
                builder.append(user.getNickname());
                if (oldParticipant.cid != newParticipant.cid) {
                    builder.append(" brought ");
                    builder.append(character.getDisplayName());
                    builder.append(" and ");
                }
                builder.append(" defected from ");
                builder.append(teams.get(oldTeam).teamName);
                builder.append(" to ");
                builder.append(teams.get(selection).teamName);
                if (oldParticipant.cid != newParticipant.cid) {
                    builder.append(". Total payout bonus is now +");
                    builder.append(ONE_DECIMAL.format(getPayoutBonusPercent()));
                    builder.append('%');
                }
            }
            return createEmbedResponse(builder.toString(), displayCurrentState())
                .setFooter(JOIN_COMMAND_PROMPT);
        }

        @Override
        EmbedResponse createReminderMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Ending in ");
            builder.append(EVENT_ENDING_REMINDER_WINDOW.toMinutes());
            builder.append(" minutes!\n\nCurrent teams:");
            return createEmbedResponse(builder.toString(), displayCurrentState())
                .setFooter(JOIN_COMMAND_PROMPT);
        }

        private long generateFruit() {
            double roll = HBMain.RNG_SOURCE.nextDouble();
            if (roll < DIAMOND_CHANCE) {
                return DIAMOND_ID;
            }
            // Dividing the roll by 0.2 converts it to
            // evenly distributed IDs 0-4
            return (long)(roll / FRUIT_CHANCE);
        }

        @Override
        Queue<EmbedResponse> createResolutionMessages() {
            Queue<EmbedResponse> messageFrames = new LinkedList<>();
            createResolutionCountdown(messageFrames);

            long[][] fruit = new long[ROWS][COLUMNS];
            StringBuilder builder = new StringBuilder();
            StringBuilder reverseBuilder;

            for (int i = 0; i < ROWS; i++) {
                reverseBuilder = new StringBuilder();
                // Insert elements 2 at a time, one left and one right
                for (int k = 0; k < COLUMNS / 2; k++) {
                    for (int j : Arrays.asList(k, COLUMNS - k - 1)) {
                        fruit[i][j] = generateFruit();

                        int value = COINS_PER_FRUIT;
                        if (i > 0 && fruit[i-1][j] == fruit[i][j]) { value += COINS_PER_GROUP; }
                        // Since we're filling from either side, check for grouping
                        // to the left if we're on the left side or the last entry
                        // of the right side, and check to the right if we're on the
                        // right side
                        if (j > 0 && (j == k || j == k + 1) && fruit[i][j-1] == fruit[i][j]) {
                            value += COINS_PER_GROUP;
                        }
                        if (j != k && j < COLUMNS - 1 && fruit[i][j+1] == fruit[i][j]) {
                            value += COINS_PER_GROUP;
                        }

                        if (fruit[i][j] == DIAMOND_ID) {
                            for (Map.Entry<Long, SlotsTeam> entries : teams.entrySet()) {
                                entries.getValue().payout += value;
                            }
                            if (j == k) {
                                builder.append(":gem:");
                            } else {
                                reverseBuilder.insert(0, ":gem:");
                            }
                        } else {
                            teams.get(fruit[i][j]).payout += value;
                            if (j == k) {
                                builder.append(teams.get(fruit[i][j]).emote);
                            } else {
                                reverseBuilder.insert(0, teams.get(fruit[i][j]).emote);
                            }
                        }
                    }

                    messageFrames.add(createEmbedResponse(builder.toString()
                        + Casino.repeatString(PLACEHOLDER, 8 - 2 * k) + reverseBuilder.toString()
                        + Casino.repeatString(EMPTY_ROW, 9 - i), displayCurrentState(true)));
                }
                builder.append(reverseBuilder.toString());
                builder.append('\n');
            }

            builder.append("\nPayout multiplier: x");
            builder.append(Stats.twoDecimals.format(getPayoutMultiplier()));
            for (Map.Entry<Long, SlotsTeam> entries : teams.entrySet()) {
                entries.getValue().payout *= getPayoutMultiplier();
            }
            messageFrames.add(createEmbedResponse(builder.toString(), displayCurrentState(true)));

            // TODO: Log results and pay coins

            return messageFrames;
        }
    }

    private static class GiveawayEvent extends Event {
        static final int COIN_AMOUNT_1 = 250;
        static final int COIN_AMOUNT_2 = 251;
        static final int GIVEAWAY_CHARACTER_CID = 0; // TODO
        static final int GIVEAWAY_GEM_GID = 19;
        static final int GIVEAWAY_GEM_DUPLICATES = 2;
        static final long COIN_1_SELECTION_ID = 0;
        static final long COIN_2_SELECTION_ID = 1;
        static final long ITEM_SELECTION_ID = 2;
        static final long GEM_SELECTION_ID = 3;
        static final long CHARACTER_SELECTION_ID = 4;

        enum GiveawayPrize {
            COIN_1(COIN_1_SELECTION_ID, Integer.toString(COIN_AMOUNT_1) + " coins"),
            COIN_2(COIN_2_SELECTION_ID, Integer.toString(COIN_AMOUNT_2) + " coins"),
            ITEM(ITEM_SELECTION_ID, "A Random High Quality Item"),
            GAMBLERS_GEM(GEM_SELECTION_ID, Integer.toString(GIVEAWAY_GEM_DUPLICATES)
                + " Gambler's Gems"),
            CHARACTER(CHARACTER_SELECTION_ID, "Placeholder (2 Star Misc)");

            long id;
            String description;
            GiveawayPrize(long id, String description) {
                this.id = id;
                this.description = description;
            }

            static GiveawayPrize fromId(long id) {
                switch ((int)id) {
                    default:
                    case (int)COIN_1_SELECTION_ID:
                        return COIN_1;
                    case (int)COIN_2_SELECTION_ID:
                        return COIN_2;
                    case (int)ITEM_SELECTION_ID:
                        return ITEM;
                    case (int)GEM_SELECTION_ID:
                        return GAMBLERS_GEM;
                    case (int)CHARACTER_SELECTION_ID:
                        return CHARACTER;
                }
            }
        }

        static class GiveawayParticipant extends Participant {
            long selection;
            boolean won = false;
            int roll = -1;
            String rollString = "";

            GiveawayParticipant(long uid, String nickname, long cid, int characterMultiplier,
                    long selection) {
                super(uid, nickname, cid, characterMultiplier);
                this.selection = selection;
            }
        }

        static class GiveawayEventDetails {
            Gacha.SHINY_TYPE shiny;

            GiveawayEventDetails(Gacha.SHINY_TYPE shiny) {
                this.shiny = shiny;
            }

            String getShinyAdjective() {
                if (shiny == Gacha.SHINY_TYPE.NORMAL) { return ""; }
                return shiny.getAdjective() + ' ';
            }
        }

        GiveawayEventDetails details;
        Map<Long, List<GiveawayParticipant>> prizeSelections = new HashMap<>();

        static Map<Long, String> getSelections() {
            Map<Long, String> output = new TreeMap<>();
            for (GiveawayPrize prize : GiveawayPrize.values()) {
                output.put(prize.id, prize.description);
            }
            return output;
        }

        GiveawayEvent(long server, Duration timeUntilResolution) {
            super(EventType.GIVEAWAY, server, timeUntilResolution, getSelections());
            for (GiveawayPrize prize : GiveawayPrize.values()) {
                prizeSelections.put(prize.id, new ArrayList<>());
            }
            details = fetchGiveawayEventDetails(seed);
        }

        @Override
        String createEmbedTitle() {
            return "Giveaway Event";
        }

        @Override
        EmbedResponse createInitialMessage() {
            EmbedResponse response = createEmbedResponse("A new Giveaway event is starting!");
            StringBuilder builder = new StringBuilder();
            for (GiveawayPrize prize : GiveawayPrize.values()) {
                if (builder.length() != 0) { builder.append('\n'); }
                if (prize == GiveawayPrize.CHARACTER) {
                    builder.append(details.getShinyAdjective());
                }
                builder.append(prize.description);
            }
            response.addInlineBlock("Prizes:", builder.toString());
            return response;
        }

        @Override
        String createAboutMessage() {
            return "Several prizes are being given away! When joining, select the prize you want "
                + "to receive. If multiple people select the same prize the winner will be "
                + "determined by rolling 1-100.\n\nWill you pick a prize you know nobody else "
                + "will pick, or trust your dice to secure what you want most?";
        }

        @Override
        EmbedResponse createPublicUserJoinMessage(Casino.User user, Gacha.GachaCharacter character,
                long selection) {
            if (!prizeSelections.containsKey(selection)) {
                return createErrorResponse(INVALID_SELECTION_PREFIX);
            }

            prizeSelections.get(selection).add(new GiveawayParticipant(user.getUid(),
                user.getNickname(), character.getId(), getStat(character), selection));

            return createEmbedResponse(user.getNickname() + " joined with "
                + character.getDisplayName() + ". There are now " + participatingUsers.size()
                + " players participating in the giveaway.").setFooter(JOIN_COMMAND_PROMPT);
        }

        @Override
        EmbedResponse createPublicUserRejoinMessage(Casino.User user,
                Gacha.GachaCharacter character, long selection) {
            if (!prizeSelections.containsKey(selection)) {
                return createErrorResponse(INVALID_SELECTION_PREFIX);
            }

            GiveawayParticipant oldParticipant = null;
            long oldSelection = -1;
            GiveawayParticipant newParticipant = new GiveawayParticipant(user.getUid(),
                user.getNickname(), character.getId(), getStat(character), selection);
            for (Map.Entry<Long, List<GiveawayParticipant>> entry : prizeSelections.entrySet()) {
                if (entry.getValue().contains(newParticipant)) {
                    oldSelection = entry.getKey();
                    oldParticipant = entry.getValue().get(
                        entry.getValue().indexOf(newParticipant));
                    break;
                }
            }

            if (oldSelection == -1 || oldParticipant == null) {
                return createErrorResponse(INVALID_SELECTION_PREFIX
                    + "Unable to find previous entry");
            } else if (oldSelection == selection && character.getId() == oldParticipant.cid) {
                return createErrorResponse(INVALID_SELECTION_PREFIX
                    + "You already joined that team with that character");
            }

            prizeSelections.get(oldSelection).remove(oldParticipant);
            prizeSelections.get(selection).add(newParticipant);

            StringBuilder builder = new StringBuilder();
            builder.append(user.getNickname());
            if (newParticipant.cid != oldParticipant.cid) {
                builder.append(" brought ").append(character.getDisplayName()).append(" instead");
            }
            if (newParticipant.cid != oldParticipant.cid
                    && newParticipant.selection != oldParticipant.selection) {
                builder.append(" and");
            }
            if (newParticipant.selection != oldParticipant.selection) {
                builder.append(" changed their selection");
            }
            return createEmbedResponse(builder.toString()).setFooter(JOIN_COMMAND_PROMPT);
        }

        @Override
        EmbedResponse createReminderMessage() {
            StringBuilder builder = new StringBuilder();
            builder.append("Ending in ").append(EVENT_ENDING_REMINDER_WINDOW.toMinutes())
                .append(" minutes! There are currently ").append(participatingUsers.size())
                .append(" participants");
            if (details.shiny != Gacha.SHINY_TYPE.NORMAL) {
                builder.append("\n\nRemember the character in this giveaway is ")
                    .append(details.shiny.getAdjective()).append('!');
            }
            return createEmbedResponse(builder.toString()).setFooter(JOIN_COMMAND_PROMPT);
        }

        String givePrize(long uid, GiveawayPrize prize) {
            switch (prize) {
                case COIN_1:
                    Casino.addMoney(uid, COIN_AMOUNT_1);
                    return prize.description;
                case COIN_2:
                    Casino.addMoney(uid, COIN_AMOUNT_2);
                    return prize.description;
                case ITEM:
                    GachaItems.Item item = GachaItems.generateItem(uid);
                    item.enhancementLevel = 1;
                    item.awardTo(uid);
                    return item.getAutoCompleteDescription();
                case GAMBLERS_GEM:
                    GachaGems.Gem gem = GachaGems.Gem.fromId(GIVEAWAY_GEM_GID);
                    for (int i = 0; i < GIVEAWAY_GEM_DUPLICATES; ++i) {
                        GachaItems.handleAwardGem(uid, gem);
                    }
                    return prize.description;
                case CHARACTER:
                    Gacha.awardCharacter(uid, uid, details.shiny);
                    return details.getShinyAdjective() + prize.description;
                default:
                    return "";
            }
        }

        private String getRollString(List<GiveawayParticipant> participants) {
            StringBuilder builder = new StringBuilder();
            for (GiveawayParticipant participant : participants) {
                if (builder.length() != 0) { builder.append('\n'); }
                builder.append(participant.rollString);
            }
            return builder.toString();
        }

        @Override
        Queue<EmbedResponse> createResolutionMessages() {
            Queue<EmbedResponse> messageFrames = new LinkedList<>();
            createResolutionCountdown(messageFrames);

            String description = "The giveaway winners are:";
            Queue<InlineBlock> blocks = new LinkedList<>();
            messageFrames.add(createEmbedResponse(description, blocks, true));

            for (Map.Entry<Long, List<GiveawayParticipant>> entry : prizeSelections.entrySet()) {
                List<GiveawayParticipant> participants = entry.getValue();
                GiveawayPrize prize = GiveawayPrize.fromId(entry.getKey());
                String title = prize.description;
                if (prize == GiveawayPrize.CHARACTER) {
                    title = details.getShinyAdjective() + title;
                }
                InlineBlock mainBlock = new InlineBlock(title, "");
                StringBuilder participantNames = new StringBuilder();
                InlineBlock rollBlock = new InlineBlock("Rolls", "");
                blocks.add(mainBlock);
                blocks.add(rollBlock);
                blocks.add(EMPTY_INLINE_BLOCK);
                messageFrames.add(createEmbedResponse(description, blocks, true));

                if (participants.isEmpty()) {
                    mainBlock.setBody("[Empty]");
                    messageFrames.add(createEmbedResponse(description, blocks, true));
                    continue;
                }

                List<Integer> eligibleWinners = new ArrayList<>();
                int nameIndex = 0;
                for (GiveawayParticipant participant : participants) {
                    if (participantNames.length() != 0) { participantNames.append('\n'); }
                    participantNames.append(participant.nickname);
                    mainBlock.setBody(participantNames.toString());
                    messageFrames.add(createEmbedResponse(description, blocks, true));
                    eligibleWinners.add(nameIndex);
                    nameIndex++;
                }

                do {
                    int highRoll = 0;
                    List<Integer> winners = new ArrayList<>();
                    for (int index : eligibleWinners) {
                        GiveawayParticipant participant = participants.get(index);
                        participant.roll = HBMain.RNG_SOURCE.nextInt(100) + 1;
                        participant.rollString += '`' + participant.roll + '`';
                        if (participant.roll > highRoll) {
                            highRoll = participant.roll;
                            winners.clear();
                            winners.add(index);
                        } else if (participant.roll == highRoll) {
                            winners.add(index);
                        }
                        rollBlock.setBody(getRollString(participants));
                        messageFrames.add(createEmbedResponse(description, blocks, true));
                    }

                    eligibleWinners = winners;
                    if (eligibleWinners.size() > 1) {
                        for (int index : eligibleWinners) {
                            participants.get(index).rollString += " - Tied!";
                        }
                        messageFrames.add(createEmbedResponse(description, blocks, true));
                        // Remove "Tied!" suffix
                        for (int index : eligibleWinners) {
                            String rollString = participants.get(index).rollString;
                            participants.get(index).rollString
                                = rollString.substring(0, rollString.length() - 5);
                        }
                    } else if (eligibleWinners.isEmpty()) {
                        System.out.println("Impossible state: encounted empty winner list while "
                            + "distributing " + prize.description);
                    } else {
                        // 1 Winner
                        GiveawayParticipant participant = participants.get(eligibleWinners.get(0));
                        participant.rollString += " - Winner!";
                        participant.won = true;
                        givePrize(participant.uid, prize);
                        messageFrames.add(createEmbedResponse(description, blocks, true));
                    }

                    // TODO: Log all participants to DB
                } while (eligibleWinners.size() > 1);
            }

            // TODO: Log event result to DB

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
        return new PickEvent.PickEventDetails(HBMain.generateBoundedNormal(
            PickEvent.AVERAGE_PICK_TARGETS, PickEvent.PICK_TARGETS_STD_DEV,
            PickEvent.MIN_PICK_TARGETS), "Test Land");
    }

    static RobEvent.RobEventDetails fetchRobEventDetails(long seed) {
        return new RobEvent.RobEventDetails("Test Land", "Test Prize");
    }

    static GiveawayEvent.GiveawayEventDetails fetchGiveawayEventDetails(long seed) {
        Gacha.SHINY_TYPE shiny = Gacha.getGachaBanner(0).generateShinyType();
        return new GiveawayEvent.GiveawayEventDetails(shiny);
    }
}