package com.c2t2s.hb;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AllOrNothing {

    static final int ROLLS_TO_DOUBLE_SEVENTY = 2;
    static final int ROLLS_TO_DOUBLE_EIGHTY = 3;
    static final int ROLLS_TO_DOUBLE_NINETY = 7;

    private static enum Difficulty {
        SEVENTY(ROLLS_TO_DOUBLE_SEVENTY, "70%", 0.3),
        EIGHTY(ROLLS_TO_DOUBLE_EIGHTY, "80%", 0.2),
        NINETY(ROLLS_TO_DOUBLE_NINETY, "90%", 0.1);

        private final int rollsToDouble;
        private final String description;
        private final double minimumSuccessfulRoll;

        Difficulty(int rollsToDouble, String description, double minimumSuccessfulRoll) {
            this.rollsToDouble = rollsToDouble;
            this.description = description;
            this.minimumSuccessfulRoll = minimumSuccessfulRoll;
        }

        static Difficulty getConstant(int rollsToDouble) {
            switch (rollsToDouble) {
                case ROLLS_TO_DOUBLE_SEVENTY:
                    return SEVENTY;
                case ROLLS_TO_DOUBLE_EIGHTY:
                    return EIGHTY;
                case ROLLS_TO_DOUBLE_NINETY:
                    return NINETY;
                default:
                    return null;
            }
        }
    }

    private static class RecordEntry {
        int rolls;
        long pot;
        long cashout;

        RecordEntry(int rolls, long pot, long cashout) {
            this.rolls = rolls;
            this.pot = pot;
            this.cashout = cashout;
        }
    }

    private static class RecordCache {
        HashMap<Long, RecordEntry> personalBests;
        int globalRollRecord;
        long globalPotRecord;
        long globalCashoutRecord;

        RecordCache(HashMap<Long, RecordEntry> personalBests, int rollRecord, long potRecord, long cashoutRecord) {
            this.personalBests = personalBests;
            globalRollRecord = rollRecord;
            globalPotRecord = potRecord;
            globalCashoutRecord = cashoutRecord;
        }

        String checkPotRecord(long uid, ActiveGame activeGame) {
            RecordEntry entry = personalBests.get(uid);
            if (entry == null) {
                return "Unable to check for record, entry absent from the cache";
            }

            String response = "";
            long potentialPayout = activeGame.getPotentialPayout();
            if (potentialPayout > globalPotRecord) {
                globalPotRecord = potentialPayout;
                entry.pot = potentialPayout;
                response = ":tada: New Global " + activeGame.difficulty.description
                    + " Pot Record: " + potentialPayout + "!";
            } else if (potentialPayout > entry.pot) {
                entry.pot = potentialPayout;
                response = ":tada: New Personal " + activeGame.difficulty.description
                    + " Best Pot: " + potentialPayout + "!";
            }

            if (activeGame.rolls > globalRollRecord) {
                globalRollRecord = activeGame.rolls;
                entry.rolls = activeGame.rolls;
                response += (response.isEmpty() ? "" : "\n") + ":tada: New Global "
                    + activeGame.difficulty.description + " Multiplier Record: "
                    + payoutPercentFormat.format(activeGame.getPayoutMultiplier()) + "!";
            } else if (activeGame.rolls > entry.rolls) {
                entry.rolls = activeGame.rolls;
                response += (response.isEmpty() ? "" : "\n") + ":tada: New Personal "
                    + activeGame.difficulty.description + " Best Multiplier: "
                    + payoutPercentFormat.format(activeGame.getPayoutMultiplier()) + "!";
            }
            return response;
        }

        String checkCashoutRecord(long uid, ActiveGame activeGame) {
            RecordEntry entry = personalBests.get(uid);
            if (entry == null) {
                return "Unable to check for record, entry absent from the cache";
            }

            long payout = activeGame.getPotentialPayout();
            if (payout > globalCashoutRecord) {
                globalCashoutRecord = payout;
                entry.cashout = payout;
                return ":tada: New Global " + activeGame.difficulty.description
                    + " Payout Record: " + payout + "!";
            } else if (payout > entry.cashout) {
                entry.cashout = payout;
                return ":tada: New Personal " + activeGame.difficulty.description
                    + " Best Payout: " + payout + "!";
            }
            return "";
        }
    }

    private static class ActiveGame {
        int rolls;
        long wager;
        Difficulty difficulty;

        ActiveGame(int rolls, long wager, Difficulty difficulty) {
            this.rolls = rolls;
            this.wager = wager;
            this.difficulty = difficulty;
        }

        double getMinimumSuccessfulRoll() {
            return difficulty.minimumSuccessfulRoll;
        }

        long getPotentialPayout() {
            return (long)(wager * getPayoutMultiplier());
        }

        double getPayoutMultiplier() {
            return Math.pow(2, rolls / difficulty.rollsToDouble);
        }
    }

    private static Map<Difficulty, RecordCache> records = initializeRecords();

    private static Map<Difficulty, RecordCache> initializeRecords() {
        Map<Difficulty, RecordCache> result = new HashMap<>();
        for (Difficulty difficulty: Difficulty.values()) {
            result.put(difficulty, null);
        }
        return result;
    }

    private static RecordCache getRecordCache(Difficulty difficulty) {
        if (!records.containsKey(difficulty)) {
            return null;
        }
        RecordCache cache = records.get(difficulty);
        if (cache == null) {
            cache = populateRecordCache(difficulty);
            records.replace(difficulty, null, cache);
        }
        return cache;
    }

    private static DecimalFormat payoutPercentFormat = new DecimalFormat("x##########0.00");
    private static DecimalFormat rollFormat = new DecimalFormat("00.000");
    private static DecimalFormat rollTargetFormat = new DecimalFormat("00");

    // Hide default constructor
    private AllOrNothing() {}

    static List<String> handleNew(long uid, long rollsToDouble, long wager) {
        Difficulty difficulty = Difficulty.getConstant((int)rollsToDouble);
        if (difficulty == null) {
            List<String> response = new ArrayList<>();
            response.add("Unrecognized odds (" + rollsToDouble + "), supported values: " + Difficulty.values().toString());
            return response;
        }

        ActiveGame activeGame = fetchActiveGame(uid, difficulty);
        if (activeGame.rolls >= 0) {
            List<String> response = new ArrayList<>();
            response.add("Existing game found:"
                + "Current payout: " + activeGame.getPotentialPayout()
                + "\nCurrent multiplier: " + payoutPercentFormat.format(activeGame.getPayoutMultiplier()));
            return response;
            // TODO: Add buttons
        }

        long balance = Casino.checkBalance(uid);
        if (balance < 0) {
            List<String> response = new ArrayList<>();
            response.add("Unable to start new game. Balance check failed or was negative (" + balance + ")");
            return response;
        } else if (balance < wager) {
            List<String> response = new ArrayList<>();
            response.add("Your current balance of " + balance + " is not enough to cover that");
            return response;
        }

        activeGame = logNewGame(uid, difficulty, wager);
        return handleRoll(uid, activeGame);
        // TODO: Add buttons
    }

    static List<String> handleRoll(long uid, long rollsToDouble) {
        Difficulty difficulty = Difficulty.getConstant((int)rollsToDouble);
        if (difficulty == null) {
            List<String> response = new ArrayList<>();
            response.add("Unrecognized odds (" + rollsToDouble + "), supported values: " + Difficulty.values().toString());
            return response;
        }

        ActiveGame activeGame = fetchActiveGame(uid, difficulty);
        if (activeGame.rolls < 0) {
            List<String> response = new ArrayList<>();
            response.add("No active game found. Use `/allornothing new` to start a new game");
            return response;
        }

        return handleRoll(uid, activeGame);
        // TODO: Add buttons
    }

    private static List<String> handleRoll(long uid, ActiveGame activeGame) {
        List<String> response = new ArrayList<>();
        double roll = HBMain.RNG_SOURCE.nextDouble() * 100;
        String rollString = rollFormat.format(roll);
        String targetRollString = rollTargetFormat.format(activeGame.getMinimumSuccessfulRoll());

        String obscuredRoll = "Roll: `??.???";
        String initialSuffix = "` (Target: " + targetRollString + ")"
            + "\nCurrent payout: " + activeGame.getPotentialPayout()
            + "\nCurrent multiplier: " + payoutPercentFormat.format(activeGame.getPayoutMultiplier()
            + Casino.PLACEHOLDER_NEWLINE_STRING);

        for (int i = 1; i < 7; i += (i != 4 ? 1 : 2)) {
            response.add(obscuredRoll.substring(obscuredRoll.length() - i)
                + rollString.substring(0, i) + initialSuffix);
        }

        if (roll < activeGame.getMinimumSuccessfulRoll()) {
            long balance = logBust(uid, activeGame.difficulty);
            response.add("Roll: `" + rollString + "` (Target: " + targetRollString + ")"
                + "\nCurrent payout: 0"
                + "\nCurrent multiplier: 0"
                + "\nBust! Your new balance is " + balance);
        } else {
            activeGame = logRoll(uid, activeGame.difficulty);
            String recordString = getRecordCache(activeGame.difficulty).checkPotRecord(uid, activeGame);
            response.add("Roll: `" + rollString + "` (Target: " + targetRollString + ")"
                + "\nCurrent payout: " + activeGame.getPotentialPayout()
                + "\nCurrent multiplier: " + payoutPercentFormat.format(activeGame.getPayoutMultiplier())
                + "\n" + recordString);
        }
        return response;
    }

    static String handleCashOut(long uid, long rollsToDouble) {
        Difficulty difficulty = Difficulty.getConstant((int)rollsToDouble);
        if (difficulty == null) {
            return "Unrecognized odds (" + rollsToDouble + "), supported values: " + Difficulty.values().toString();
        }

        ActiveGame activeGame = fetchActiveGame(uid, difficulty);
        if (activeGame.rolls < 0) {
            return "No active game found. Use `/allornothing new` to start a new game";
        }

        long balance = logCashout(uid, activeGame.difficulty);
        String recordString = getRecordCache(activeGame.difficulty).checkCashoutRecord(uid, activeGame);
        return "Cashed out for " + activeGame.getPotentialPayout() + ". Your new balance is " + balance
            + "\n" + recordString;
    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS allornothing_user (
    //  uid bigint,
    //  rolls_to_double integer,
    //  games_played integer DEFAULT 0,
    //  times_rolled bigint DEFAULT 0,
    //  busts integer DEFAULT 0,
    //  times_cashed_out integer DEFAULT 0,
    //  spent bigint DEFAULT 0,
    //  winnings bigint DEFAULT 0,
    //  record_rolls integer DEFAULT 0,
    //  record_pot bigint DEFAULT 0,
    //  record_cashout bigint DEFAULT 0,
    //  current_wager bigint DEFAULT -1,
    //  current_rolls integer DEFAULT -1,
    //  CONSTRAINT allornothing_primary_key PRIMARY_KEY(uid, rolls_to_double),
    //  CONSTRAINT allornothing_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    static RecordCache populateRecordCache(Difficulty difficulty) {
        return null;
    }

    static ActiveGame fetchActiveGame(long uid, Difficulty difficulty) {
        return null;
    }

    static ActiveGame logNewGame(long uid, Difficulty difficulty, long wager) {
        return null;
    }

    static long logBust(long uid, Difficulty difficulty) {
        return 0;
    }

    static ActiveGame logRoll(long uid, Difficulty difficulty) {
        return null;
    }

    static long logCashout(long uid, Difficulty difficulty) {
        return 0;
    }
}
