package com.c2t2s.hb;

class Stats {

    static final String WORK_STRING = "work";
    static final String FISH_STRING = "fish";
    static final String ROB_STRING = "rob";
    static final String PICKPOCKET_STRING = "pickpocket";
    static final String GUESS_STRING = "guess";
    static final String HUGEGUESS_STRING = "hugeguess";
    static final String SLOTS_STRING = "slots";
    static final String MINISLOTS_STRING = "minislots";
    static final String FEED_STRING = "feed";
    static final String OVERUNDER_STRING = "overunder";
    static final String BLACKJACK_STRING = "blackjack";
    static final String GACHA_STRING = "gacha";
    static final String ALLORNOTHING_STRING = "allornothing";

    static enum StatsOption {

        WORK(WORK_STRING, "/work"),
        FISH(FISH_STRING, "/fish"),
        ROB(ROB_STRING, "/rob"),
        PICKPOCKET(PICKPOCKET_STRING, "/pickpocket"),
        GUESS(GUESS_STRING, "/guess"),
        HUGEGUESS(HUGEGUESS_STRING, "/hugeguess"),
        SLOTS(SLOTS_STRING, "/slots"),
        MINISLOTS(MINISLOTS_STRING, "/minislots"),
        FEED(FEED_STRING, "Money Machine"),
        OVERUNDER(OVERUNDER_STRING, "/overunder"),
        BLACKJACK(BLACKJACK_STRING, "/blackjack"),
        GACHA(GACHA_STRING, "Gacha Pulls"),
        ALLORNOTHING(ALLORNOTHING_STRING, "/allornothing");

        final String optionName;
        final String description;

        StatsOption(String name, String description) {
            optionName = name;
            this.description = description;
        }

        String getName() {
            return optionName;
        }

        String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return optionName;
        }
    }

    // Hide default constructor
    private Stats() {}

    static String handleStats(String argument, long uid) {
        switch (argument) {
            case WORK_STRING:
                return handleWorkStats(uid);
            case FISH_STRING:
                return handleFishStats(uid);
            case ROB_STRING:
                return handleRobStats(uid);
            case PICKPOCKET_STRING:
                return handlePickpocketStats(uid);
            case GUESS_STRING:
                return handleGuessStats(uid);
            case HUGEGUESS_STRING:
                return handleHugeguessStats(uid);
            case SLOTS_STRING:
                return handleSlotsStats(uid);
            case MINISLOTS_STRING:
                return handleMinislotsStats(uid);
            case FEED_STRING:
                return handleFeedStats(uid);
            case OVERUNDER_STRING:
                return handleOverunderStats(uid);
            case BLACKJACK_STRING:
                return handleBlackjackStats(uid);
            case GACHA_STRING:
                return handleGachaStats(uid);
            case ALLORNOTHING_STRING:
                return handleAllOrNothingStats(uid);
            default:
                return "Unrecognized Game argument. Supported values are the following:\n\t" + StatsOption.values().toString();
        }

    }

    static String handleWorkStats(long uid) {
        return "`/work` odds:"
            + "\n\tPayout chance: 100%"
            + "\n\tAverage Payout: 200 coins (225 if high morality)";
    }

    static String handleFishStats(long uid) {
        return "`/fish` odds:"
            + "\n\tPayout chance: 100%"
            + "\n\tAverage Payout: 65 coins (72.5 if high morality)";
    }

    static String handleRobStats(long uid) {
        return "`/rob` odds:"
            + "\n\tPayout chance: 50%"
            + "\n\tAverage Payout: 200 coins (225 if low morality)";
    }

    static String handlePickpocketStats(long uid) {
        return "`/pickpocket` odds:"
            + "\n\tPayout chance: 50%"
            + "\n\tAverage Payout: 65 coins (72.5 if high morality)"
            + "\n\n`/pickpocket` stats for ";
    }

    static String handleGuessStats(long uid) {
        return "`/guess` odds:"
            + "\n\tCorrect guess: 10:1 (10%)";
    }

    static String handleHugeguessStats(long uid) {
        return "`/hugeguess` odds:"
            + "\n\tCorrect guess: 100:1 (1%)";
    }

    static String handleSlotsStats(long uid) {
        return "`/slots` odds:"
            + "\n```"
            + "\n\t5 of a kind:   30:1 (1/625)"
            + "\n\t4 of a kind:   10:1 (4/125)"
            + "\n\t3 of a kind:  1.5:1 (32/125)"
            + "\n\tFruit Salad:    2:1 (24/125)"
            + "\n\t1 diamond:      1:1 (1/20)"
            + "\n\t2 diamonds:    10:1 (1/1000)"
            + "\n\t3 diamonds:   100:1 (1/100 000)"
            + "\n\t4 diamonds:  1000:1 (1/20 000 000)"
            + "\n\t5 diamonds: 10000:1 (1/10 000 000 000)"
            + "\n```";
    }

    static String handleMinislotsStats(long uid) {
        return "`/minislots` odds:"
            + "\n```"
            + "\n\t3 of a kind:   5:1 (1/25)"
            + "\n\t2 of a kind: 1.6:1 (12/25)"
            + "\n\t1 diamond:   0.4:1 (3/100)"
            + "\n\t2 diamonds:   10:1 (3/10 000)"
            + "\n\t3 diamonds:  100:1 (1/1 000 0000)"
            + "\n```";
    }

    static String handleFeedStats(long uid) {
        return "Money Machine odds:"
            + "\n\tBase payout chance 5%"
            + "\n\tIncreases up to 25% based on current pot size"
            + "\n\tReduced for overly small feed amounts";
    }

    static String handleOverunderStats(long uid) {
        return "`/overunder` odds:"
            + "\n\t2 correct answers: 1:1 (~2/11)"
            + "\n\t3 correct answers: 3:1 (~3/11)";
    }

    static String handleBlackjackStats(long uid) {
        return "`/blackjack` odds:"
            + "\n\tCorrect: 2:1 (Bit under 1/2 probably)"
            + "\n\tTie:     1:1 (Rare)";
    }

    static String handleGachaStats(long uid) {
        return "`/gacha` odds:"
            + "\n\tBase 1 Star Chance: 1/8   (Max pity 12)"
            + "\n\tBase 2 Star Chance: 1/32  (Max pity 48)"
            + "\n\tBase 3 Star Chance: 1/128 (Max pity 192)"
            + "\n\tEach increases as you approach max pity"
            + "\n\tShiny Chance: 1/20 for any awarded character";
    }

    static String handleAllOrNothingStats(long uid) {
        return "`/allornothing` odds:"
            + "\n\t70%: Payout increases by x1.41 per roll (70% chance)"
            + "\n\t80%: Payout increases by x1.26 per roll (80% chance)"
            + "\n\t90%: Payout increases by x1.10 per roll (90% chance)";
    }

}
