package com.c2t2s.hb;

class Stats {

    // Hide default constructor
    private Stats() {}

    static String POSSIBLE_ARGUMENTS = "work, fish, rob, pickpocket";

    static String handleStats(String argument, long uid) {
        switch (argument) {
            case "work":
                return handleWorkStats(uid);
            case "fish":
                return handleFishStats(uid);
            case "rob":
                return handleRobStats(uid);
            case "pickpocket":
                return handlePickpocketStats(uid);
            default:
                return "Unrecognized Game argument. Supported values are the following:\n\t" + POSSIBLE_ARGUMENTS;
        }

    }

    static String handleWorkStats(long uid) {
        return "`/work` stats:"
            + "\n\tPayout chance: 100%"
            + "\n\tAverage Payout: 200 coins (225 if high morality)";
    }

    static String handleFishStats(long uid) {
        return "`/fish` stats:"
            + "\n\tPayout chance: 100%"
            + "\n\tAverage Payout: 65 coins (72.5 if high morality)";
    }

    static String handleRobStats(long uid) {
        return "`/rob` stats:"
            + "\n\tPayout chance: 50%"
            + "\n\tAverage Payout: 200 coins (225 if low morality)";
    }

    static String handlePickpocketStats(long uid) {
        return "`/pickpocket` stats:"
            + "\n\tPayout chance: 50%"
            + "\n\tAverage Payout: 65 coins (72.5 if high morality)";
    }

    static String handleGuessStats(long uid) {
        return "";
    }

    static String handleHugeguessStats(long uid) {
        return "";
    }

    static String handleSlotsStats(long uid) {
        return "";
    }

    static String handleMinislotsStats(long uid) {
        return "";
    }

    static String handleFeedStats(long uid) {
        return "";
    }

    static String handleOverunderStats(long uid) {
        return "";
    }

    static String handleBlackjackStats(long uid) {
        return "";
    }

    static String handleGachaStats(long uid) {
        return "";
    }

}
