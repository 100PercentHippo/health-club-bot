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
        SEVENTY(ROLLS_TO_DOUBLE_SEVENTY, "70%", 30),
        EIGHTY(ROLLS_TO_DOUBLE_EIGHTY, "80%", 20),
        NINETY(ROLLS_TO_DOUBLE_NINETY, "90%", 10);

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

        String checkActiveGameRecords(long uid, ActiveGame activeGame) {
            RecordEntry entry = personalBests.get(uid);
            if (entry == null) {
                return "Unable to check for record, entry absent from the cache";
            }

            StringBuilder response = new StringBuilder();
            response.append(checkRollRecord(uid, entry, activeGame));
            response.append(checkPotRecord(uid, entry, activeGame));
            if (response.length() > 0) {
                response.insert(0, '\n');
            }
            return response.toString();
        }

        String checkRollRecord(long uid, RecordEntry entry, ActiveGame activeGame) {
            int rolls = activeGame.rolls;
            if (rolls < entry.rolls) {
                return "";
            }
            int validatedRecord = verifyRollRecord(uid, activeGame.difficulty);
            if (validatedRecord != rolls) {
                entry.rolls = validatedRecord;
                return "";
            }
            StringBuilder response = new StringBuilder();
            if (rolls > globalRollRecord) {
                globalRollRecord = rolls;
                response.append("\n:tada: New World Record Multiplier in the "
                    + activeGame.difficulty.description + " bracket: "
                    + payoutPercentFormat.format(activeGame.getPayoutMultiplier()) + "!");
            } else if (rolls == globalRollRecord) {
                response.append("\nTied for World Record Multiplier in the "
                    + activeGame.difficulty.description + " bracket: "
                    + payoutPercentFormat.format(activeGame.getPayoutMultiplier()));
            }
            if (rolls > entry.rolls) {
                entry.rolls = rolls;
                response.append("\n:tada: New Personal Best Multiplier in the "
                    + activeGame.difficulty.description + " bracket: "
                    + payoutPercentFormat.format(activeGame.getPayoutMultiplier()) + "!");
            } else if (rolls == entry.rolls) {
                response.append("\nTied for Personal Best Multiplier in the "
                    + activeGame.difficulty.description + " bracket: "
                    + payoutPercentFormat.format(activeGame.getPayoutMultiplier()));
            }
            return response.toString();
        }

        String checkPotRecord(long uid, RecordEntry entry, ActiveGame activeGame) {
            long pot = activeGame.getPotentialPayout();
            if (pot < entry.pot) {
                return "";
            }
            long validatedRecord = verifyPotRecord(uid, activeGame.difficulty);
            if (validatedRecord != pot) {
                entry.pot = validatedRecord;
                return "";
            }
            StringBuilder response = new StringBuilder();
            if (pot > globalPotRecord) {
                globalPotRecord = pot;
                response.append("\n:tada: New World Record Potential Payout in the "
                    + activeGame.difficulty.description + " bracket: " + pot + "!");
            } else if (pot == globalPotRecord) {
                response.append("\nTied for World Record Potential Payout in the "
                    + activeGame.difficulty.description + " bracket: " + pot);
            }
            if (pot > entry.pot) {
                entry.pot = pot;
                response.append("\n:tada: New Personal Best Potential Payout in the "
                    + activeGame.difficulty.description + " bracket: " + pot + "!");
            } else if (pot == entry.pot) {
                response.append("\nTied for Personal Best Potential Payout in the "
                    + activeGame.difficulty.description + " bracket: " + pot);
            }
            return response.toString();
        }

        String checkCashoutRecord(long uid, ActiveGame activeGame) {
            RecordEntry entry = personalBests.get(uid);
            if (entry == null) {
                return "Unable to check for record, entry absent from the cache";
            }

            long payout = activeGame.getPotentialPayout();
            if (payout < entry.cashout) {
                return "";
            }
            long validatedRecord = verifyCashoutRecord(uid, activeGame.difficulty);
            if (validatedRecord != payout) {
                entry.cashout = validatedRecord;
                return "";
            }
            StringBuilder response = new StringBuilder();
            if (payout > globalCashoutRecord) {
                globalCashoutRecord = payout;
                response.append("\n:tada: New World Record Payout in the "
                    + activeGame.difficulty.description + " bracket: " + payout);
            } else if (payout == globalCashoutRecord) {
                response.append("\nTied for World Record Payout in the "
                    + activeGame.difficulty.description + " bracket: " + payout);
            }
            if (payout > entry.cashout) {
                entry.cashout = payout;
                response.append("\n:tada: New Personal Best Payout in the "
                    + activeGame.difficulty.description + " bracket: " + payout);
            } else if (payout == entry.cashout) {
                response.append("\nTied for Personal Best Payout in the "
                    + activeGame.difficulty.description + " bracket: " + payout);
            }
            return response.toString();
        }
    }

    static class ActiveGame {
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
            return (long)(wager * getPayoutMultiplier(rolls));
        }

        long getNextRollPayout() {
            return (long)(wager * getPayoutMultiplier(rolls + 1));
        }

        double getPayoutMultiplier() {
            return getPayoutMultiplier(rolls);
        }

        private double getPayoutMultiplier(int rolls) {
            return Math.pow(2, ((double)rolls) / difficulty.rollsToDouble);
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
                + "\nCurrent payout: " + activeGame.getPotentialPayout(),
                ButtonRows.makeAllOrNothing(activeGame));
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
            return new HBMain.MultistepResponse("No active game found. Use `/allornothing` to start a new game");
        }

        return handleRoll(uid, activeGame);
    }

    private static HBMain.MultistepResponse handleRoll(long uid, ActiveGame activeGame) {
        List<String> response = new ArrayList<>();
        double roll = HBMain.RNG_SOURCE.nextDouble() * 100;
        String rollString = rollFormat.format(roll);
        String targetRollString = rollTargetFormat.format(activeGame.getMinimumSuccessfulRoll());

        String obscuredRoll = "Roll: `  .   ";
        String initialSuffix = "` (Target: " + targetRollString + " or higher)"
            + "\nCurrent payout: " + activeGame.getPotentialPayout()
            + Casino.PLACEHOLDER_NEWLINE_STRING;

        for (int i = 0; i < 7; i += (i != 3 ? 1 : 2)) {
            response.add(obscuredRoll.substring(0, obscuredRoll.length() - i)
                + rollString.substring(rollString.length() - i) + initialSuffix);
        }

        if (roll < activeGame.getMinimumSuccessfulRoll()) {
            long balance = logBust(uid, activeGame.difficulty);
            response.add("Roll: `" + rollString + "` (Target: " + targetRollString + " or higher)"
                + "\nCurrent payout: 0"
                + "\nBust! Your new balance is " + balance);
            return new HBMain.MultistepResponse(response);
        } else {
            // Ensure cache is populated before we register this entry, but check for records after updating the DB
            RecordCache cache = getRecordCache(activeGame.difficulty);
            ++activeGame.rolls;
            activeGame = logRoll(uid, activeGame.difficulty, activeGame.getPotentialPayout());
            String recordString = cache.checkActiveGameRecords(uid, activeGame);
            response.add("Roll: `" + rollString + "` (Target: " + targetRollString + " or higher)"
                + "\nCurrent payout: " + activeGame.getPotentialPayout()
                + recordString);
            return new HBMain.MultistepResponse(response, ButtonRows.makeAllOrNothing(activeGame));
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

        if (activeGame.rolls < activeGame.difficulty.rollsToDouble) {
            int rollsUntilClaimable = activeGame.difficulty.rollsToDouble - activeGame.rolls;
            return "Unable to claim until multiplier reaches x2.0 (currently " + activeGame.getPayoutMultiplier()
                + "). " + rollsUntilClaimable + " more roll" + Casino.getPluralSuffix(rollsUntilClaimable) + " needed.";
        }

        // Ensure cache is populated before we register this entry, but check for records after updating the DB
        RecordCache cache = getRecordCache(activeGame.difficulty);
        long balance = logCashout(uid, activeGame.difficulty, activeGame.getPotentialPayout());
        String recordString = cache.checkCashoutRecord(uid, activeGame);
        return "Cashed out for " + activeGame.getPotentialPayout() + ". Your new balance is " + balance
            + recordString;
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
    //  current_wager bigint DEFAULT 500,
    //  current_rolls integer DEFAULT -1,
    //  PRIMARY KEY(uid, rolls_to_double),
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

    static ActiveGame logRoll(long uid, Difficulty difficulty, long pot) {
        String query = "UPDATE allornothing_user SET (times_rolled, current_rolls, record_rolls, record_pot) = (times_rolled + 1, current_rolls + 1, "
            + "GREATEST(record_rolls, current_rolls + 1), GREATEST(record_pot, " + pot + ")) WHERE uid = "
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
        String query = "UPDATE allornothing_user SET (times_cashed_out, winnings, record_cashout, current_rolls) = (times_cashed_out + 1, winnings + "
            + winnings + ", GREATEST(" + winnings + ", record_cashout), -1) WHERE uid = " + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + ";";
        CasinoDB.executeUpdate(query);
        return Casino.addMoney(uid, winnings);
    }

    static int verifyRollRecord(long uid, Difficulty difficulty) {
        String query = "SELECT record_rolls FROM allornothing_user WHERE uid = "
            + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + ";";
        return CasinoDB.executeIntQuery(query);
    }

    static long verifyPotRecord(long uid, Difficulty difficulty) {
        String query = "SELECT record_pot FROM allornothing_user WHERE uid = "
            + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + ";";
        return CasinoDB.executeLongQuery(query);
    }

    static long verifyCashoutRecord(long uid, Difficulty difficulty) {
        String query = "SELECT record_cashout FROM allornothing_user WHERE uid = "
            + uid + " AND rolls_to_double = " + difficulty.rollsToDouble + ";";
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
