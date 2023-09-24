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

    static enum Difficulty {
        SEVENTY(ROLLS_TO_DOUBLE_SEVENTY, "70%", 0.3),
        EIGHTY(ROLLS_TO_DOUBLE_EIGHTY, "80%", 0.2),
        NINETY(ROLLS_TO_DOUBLE_NINETY, "90%", 0.1);

        final int rollsToDouble;
        final String description;
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
            if (potentialPayout > entry.pot) {
                long validatedRecord = logPotRecord(uid, activeGame.difficulty, potentialPayout);
                if (validatedRecord == potentialPayout) {
                    entry.pot = potentialPayout;
                    if (potentialPayout > globalPotRecord) {
                        globalPotRecord = potentialPayout;
                        response = ":tada: New Global " + activeGame.difficulty.description
                            + " Pot Record: " + potentialPayout + "!";
                    } else {
                        response = ":tada: New Personal " + activeGame.difficulty.description
                            + " Best Pot: " + potentialPayout + "!";
                    }
                }
            }

            if (activeGame.rolls > entry.rolls) {
                long validatedRecord = logRollRecord(uid, activeGame.difficulty, activeGame.rolls);
                if (validatedRecord == activeGame.rolls) {
                    entry.rolls = activeGame.rolls;
                    if (activeGame.rolls > globalRollRecord) {
                        globalRollRecord = activeGame.rolls;
                        response += (response.isEmpty() ? "" : "\n") + ":tada: New Global "
                            + activeGame.difficulty.description + " Multiplier Record: "
                            + payoutPercentFormat.format(activeGame.getPayoutMultiplier()) + "!";
                    } else {
                        response += (response.isEmpty() ? "" : "\n") + ":tada: New Personal "
                            + activeGame.difficulty.description + " Best Multiplier: "
                            + payoutPercentFormat.format(activeGame.getPayoutMultiplier()) + "!";
                    }
                }
            }

            return response;
        }

        String checkCashoutRecord(long uid, ActiveGame activeGame) {
            RecordEntry entry = personalBests.get(uid);
            if (entry == null) {
                return "Unable to check for record, entry absent from the cache";
            }

            long payout = activeGame.getPotentialPayout();
            if (payout > entry.cashout) {
                long validatedRecord = logCashoutRecord(uid, activeGame.difficulty, payout);
                if (validatedRecord == payout) {
                    entry.cashout = payout;
                    if (payout > globalCashoutRecord) {
                        globalCashoutRecord = payout;
                        return ":tada: New Global " + activeGame.difficulty.description
                            + " Payout Record: " + payout + "!";
                    } else {
                        return ":tada: New Personal " + activeGame.difficulty.description
                            + " Best Payout: " + payout + "!";
                    }
                }
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

        boolean isClaimable() {
            return rolls >= difficulty.rollsToDouble;
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

    static HBMain.MultistepResponse handleNew(long uid, long rollsToDouble, long wager) {
        Difficulty difficulty = Difficulty.getConstant((int)rollsToDouble);
        if (difficulty == null) {
            return new HBMain.MultistepResponse("Unrecognized odds (" + rollsToDouble
                + "), supported values: " + Difficulty.values().toString());
        }

        ActiveGame activeGame = fetchActiveGame(uid, difficulty);
        if (activeGame.rolls >= 0) {
            return new HBMain.MultistepResponse("Existing game found:"
                + "Current payout: " + activeGame.getPotentialPayout()
                + "\nCurrent multiplier: " + payoutPercentFormat.format(activeGame.getPayoutMultiplier()),
                ButtonRows.makeAllOrNothing(activeGame.isClaimable(), difficulty));
        }

        long balance = Casino.checkBalance(uid);
        if (balance < 0) {
            return new HBMain.MultistepResponse("Unable to start new game. Balance check failed or was negative (" + balance + ")");
        } else if (balance < wager) {
            return new HBMain.MultistepResponse("Your current balance of " + balance + " is not enough to cover that");
        }

        activeGame = logNewGame(uid, difficulty, wager);
        return handleRoll(uid, activeGame);
    }

    static HBMain.MultistepResponse handleRoll(long uid, long rollsToDouble) {
        Difficulty difficulty = Difficulty.getConstant((int)rollsToDouble);
        if (difficulty == null) {
            return new HBMain.MultistepResponse("Unrecognized odds (" + rollsToDouble
                + "), supported values: " + Difficulty.values().toString());
        }

        ActiveGame activeGame = fetchActiveGame(uid, difficulty);
        if (activeGame.rolls < 0) {
            return new HBMain.MultistepResponse("No active game found. Use `/allornothing new` to start a new game");
        }

        return handleRoll(uid, activeGame);
    }

    private static HBMain.MultistepResponse handleRoll(long uid, ActiveGame activeGame) {
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
            return new HBMain.MultistepResponse(response);
        } else {
            String recordString = getRecordCache(activeGame.difficulty).checkPotRecord(uid, activeGame);
            activeGame = logRoll(uid, activeGame.difficulty);
            response.add("Roll: `" + rollString + "` (Target: " + targetRollString + ")"
                + "\nCurrent payout: " + activeGame.getPotentialPayout()
                + "\nCurrent multiplier: " + payoutPercentFormat.format(activeGame.getPayoutMultiplier())
                + "\n" + recordString);
            return new HBMain.MultistepResponse(response, ButtonRows.makeAllOrNothing(activeGame.isClaimable(), activeGame.difficulty));
        }
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

        String recordString = getRecordCache(activeGame.difficulty).checkCashoutRecord(uid, activeGame);
        long balance = logCashout(uid, activeGame.difficulty, activeGame.getPotentialPayout());
        return "Cashed out for " + activeGame.getPotentialPayout() + ". Your new balance is " + balance
            + "\n" + recordString;
    }

    static String handlePrematureCashOut() {
        return "Unable to claim until multiplier reaches x2";
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
        String query = "SELECT uid, record_rolls, record_pot, record_cashout FROM allornothing_user WHERE rolls_to_double = " + difficulty.rollsToDouble + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            HashMap<Long, RecordEntry> personalBests = new HashMap<>();
            int rollRecord = 0;
            long potRecord = 0;
            long cashoutRecord = 0;
            long uid = 0;
            int personalRollRecord = 0;
            long personalPotRecord = 0;
            long personalCashoutRecord = 0;
            while (results.next()) {
                uid = results.getLong(1);
                personalRollRecord = results.getInt(2);
                personalPotRecord = results.getLong(3);
                personalCashoutRecord = results.getLong(4);

                if (personalRollRecord > rollRecord) {
                    rollRecord = personalRollRecord;
                }
                if (personalPotRecord > potRecord) {
                    potRecord = personalPotRecord;
                }
                if (personalCashoutRecord > cashoutRecord) {
                    cashoutRecord = personalCashoutRecord;
                }
                personalBests.put(uid,
                    new RecordEntry(personalRollRecord, personalPotRecord, personalCashoutRecord));
            }
            return new RecordCache(personalBests, rollRecord, potRecord, cashoutRecord);
        }, null);
    }

    static ActiveGame executeActiveGameQuery(String query, Difficulty difficulty) {
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int rolls = results.getInt(1);
                long wager = results.getLong(2);
                return new ActiveGame(rolls, wager, difficulty);
            }
            return null;
        }, null);
    }

    static ActiveGame fetchActiveGame(long uid, Difficulty difficulty) {
        String query = "SELECT current_rolls, current_wager FROM allornothing_user WHERE uid = " + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + ";";
        return executeActiveGameQuery(query, difficulty);
    }

    static ActiveGame logNewGame(long uid, Difficulty difficulty, long wager) {
        Casino.takeMoney(uid, wager);
        String query = "UPDATE allornothing_user SET (games_played, spent, current_rolls, current_wager) = (games_played + 1, spent + " + wager
            + ", 0, " + wager + ") WHERE uid = " + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + " RETURNING current_rolls, current_wager;";
        return executeActiveGameQuery(query, difficulty);
    }

    static ActiveGame logRoll(long uid, Difficulty difficulty) {
        String query = "UPDATE allornothing_user SET (times_rolled, current_rolls) = (timers_rolled + 1, current_rolls + 1) WHERE uid = "
            + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + " RETURNING current_rolls, current_wager;";
        return executeActiveGameQuery(query, difficulty);
    }

    static long logBust(long uid, Difficulty difficulty) {
        String query = "UPDATE allornothing_user SET (times_rolled, busts, current_rolls) = (times_rolled + 1, busts + 1, -1) WHERE uid = "
            + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + ";";
        CasinoDB.executeUpdate(query);
        return Casino.checkBalance(uid);
    }

    static long logCashout(long uid, Difficulty difficulty, long winnings) {
        String query = "UPDATE allornothing_user SET (times_cashed_out, winnings, current_rolls) = (times_cashed_out + 1, winnings "
            + winnings + ", -1) WHERE uid = " + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + ";";
        CasinoDB.executeUpdate(query);
        return Casino.addMoney(uid, winnings);
    }

    static long logRollRecord(long uid, Difficulty difficulty, int newRecord) {
        return logRecord(uid, difficulty, newRecord, "record_rolls");
    }

    static long logPotRecord(long uid, Difficulty difficulty, long newRecord) {
        return logRecord(uid, difficulty, newRecord, "record_pot");
    }

    static long logCashoutRecord(long uid, Difficulty difficulty, long newRecord) {
        return logRecord(uid, difficulty, newRecord, "record_cashout");
    }

    static long logRecord(long uid, Difficulty difficulty, long newRecord, String field) {
        String query = "UPDATE allornothing_user SET " + field + " = GREATEST(" + field + ", " + newRecord
            + ") WHERE uid = " + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + " RETURNING " + field + ";";
        return CasinoDB.executeLongQuery(query);
    }

    static void addUserToCache(long uid) {
        for (Difficulty difficulty: Difficulty.values()) {
            if (records != null && !records.get(difficulty).personalBests.containsKey(uid)) {
                records.get(difficulty).personalBests.put(uid, new RecordEntry(0, 0, 0));
            }
        }
    }
}
