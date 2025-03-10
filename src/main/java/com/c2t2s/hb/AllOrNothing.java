package com.c2t2s.hb;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AllOrNothing {

    static final int ROLLS_TO_DOUBLE_SEVENTY = 2;
    static final int ROLLS_TO_DOUBLE_EIGHTY = 3;
    static final int ROLLS_TO_DOUBLE_NINETY = 7;

    enum Difficulty {
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

    static class RecordCache {
        HashMap<Long, RecordEntry> personalBests;

        RecordCache(HashMap<Long, RecordEntry> personalBests) {
            this.personalBests = personalBests;
        }

        String checkActiveGameRecords(long server, long uid, ActiveGame activeGame) {
            RecordEntry entry = personalBests.get(uid);
            if (entry == null) {
                return "Unable to check for record, entry absent from the cache";
            }

            StringBuilder response = new StringBuilder();
            response.append(checkRollRecord(server, entry, activeGame));
            response.append(checkPotRecord(server, entry, activeGame));
            if (response.length() > 0) {
                response.insert(0, '\n');
            }
            return response.toString();
        }

        String checkRollRecord(long server, RecordEntry entry, ActiveGame activeGame) {
            int rolls = activeGame.rolls;
            if (rolls < entry.rolls) {
                return "";
            }
            StringBuilder response = new StringBuilder();
            long globalRollRecord = fetchGlobalRollRecord(activeGame.difficulty, server);
            if (rolls > globalRollRecord) {
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

        String checkPotRecord(long server, RecordEntry entry, ActiveGame activeGame) {
            long pot = activeGame.getPotentialPayout();
            if (pot < entry.pot) {
                return "";
            }
            StringBuilder response = new StringBuilder();
            long globalPotRecord = fetchGlobalPotRecord(activeGame.difficulty, server);
            if (pot > globalPotRecord) {
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

        String checkCashoutRecord(long server, long uid, ActiveGame activeGame) {
            RecordEntry entry = personalBests.get(uid);
            if (entry == null) {
                return "Unable to check for record, entry absent from the cache";
            }

            long payout = activeGame.getPotentialPayout();
            if (payout < entry.cashout) {
                return "";
            }
            StringBuilder response = new StringBuilder();
            long globalCashoutRecord = fetchGlobalCashoutRecord(activeGame.difficulty, server);
            if (payout > globalCashoutRecord) {
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
        Map<Difficulty, RecordCache> result = new EnumMap<>(Difficulty.class);
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

    static DecimalFormat payoutPercentFormat = new DecimalFormat("x##########0.00");
    private static DecimalFormat rollFormat = new DecimalFormat("00.000");
    private static DecimalFormat rollTargetFormat = new DecimalFormat("00");

    // Hide default constructor
    private AllOrNothing() {}

    static HBMain.MultistepResponse handleNew(long server, long uid, long rollsToDouble, long wager) {
        Difficulty difficulty = Difficulty.getConstant((int)rollsToDouble);
        if (difficulty == null) {
            return new HBMain.MultistepResponse("Unrecognized odds (" + rollsToDouble
                + "), supported values: " + Arrays.toString(Difficulty.values()));
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
        return handleRoll(server, uid, activeGame);
    }

    static HBMain.MultistepResponse handleRoll(long server, long uid, long rollsToDouble) {
        Difficulty difficulty = Difficulty.getConstant((int)rollsToDouble);
        if (difficulty == null) {
            return new HBMain.MultistepResponse("Unrecognized odds (" + rollsToDouble
                + "), supported values: " + Arrays.toString(Difficulty.values()));
        }

        ActiveGame activeGame = fetchActiveGame(uid, difficulty);
        if (activeGame.rolls < 0) {
            return new HBMain.MultistepResponse("No active game found. Use `/allornothing` to start a new game");
        }

        return handleRoll(server, uid, activeGame);
    }

    private static HBMain.MultistepResponse handleRoll(long server, long uid, ActiveGame activeGame) {
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
            // Ensure cache is populated before we register this entry
            RecordCache cache = getRecordCache(activeGame.difficulty);
            ++activeGame.rolls;
            String recordString;
            if (cache != null) {
                recordString = cache.checkActiveGameRecords(server, uid, activeGame);
            } else {
                recordString = "Unable to populate records: Cache unavailable";
            }
            activeGame = logRoll(uid, activeGame.difficulty, activeGame.getPotentialPayout());
            response.add("Roll: `" + rollString + "` (Target: " + targetRollString + " or higher)"
                + "\nCurrent payout: " + activeGame.getPotentialPayout()
                + recordString);
            return new HBMain.MultistepResponse(response, ButtonRows.makeAllOrNothing(activeGame));
        }
    }

    static String handleCashOut(long server, long uid, long rollsToDouble) {
        Difficulty difficulty = Difficulty.getConstant((int)rollsToDouble);
        if (difficulty == null) {
            return "Unrecognized odds (" + rollsToDouble + "), supported values: " + Arrays.toString(Difficulty.values());
        }

        ActiveGame activeGame = fetchActiveGame(uid, difficulty);
        if (activeGame.rolls < 0) {
            return "No active game found. Use `/allornothing new` to start a new game";
        }

        if (activeGame.rolls < activeGame.difficulty.rollsToDouble) {
            int rollsUntilClaimable = activeGame.difficulty.rollsToDouble - activeGame.rolls;
            return "Unable to claim until multiplier reaches x2.0 (currently "
                + payoutPercentFormat.format(activeGame.getPayoutMultiplier()) + "). "
                + rollsUntilClaimable + " more roll" + Casino.getPluralSuffix(rollsUntilClaimable) + " needed.";
        }

        // Ensure cache is populated before we register this entry
        RecordCache cache = getRecordCache(activeGame.difficulty);
        String recordString;
        if (cache != null) {
            recordString = cache.checkCashoutRecord(server, uid, activeGame);
        } else {
            recordString = "Unable to populate records: Cache unavailable";
        }
        long balance = logCashout(uid, activeGame.difficulty, activeGame.getPotentialPayout());
        return "Cashed out for " + activeGame.getPotentialPayout() + ". Your new balance is " + balance
            + recordString;
    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS allornothing_user (
    //  uid bigint,
    //  rolls_to_double integer,
    //  games_played integer NOT NULL DEFAULT 0,
    //  times_rolled bigint NOT NULL DEFAULT 0,
    //  busts integer NOT NULL DEFAULT 0,
    //  times_cashed_out integer NOT NULL DEFAULT 0,
    //  spent bigint NOT NULL DEFAULT 0,
    //  winnings bigint NOT NULL DEFAULT 0,
    //  record_rolls integer NOT NULL DEFAULT 0,
    //  record_pot bigint NOT NULL DEFAULT 0,
    //  record_cashout bigint NOT NULL DEFAULT 0,
    //  current_wager bigint NOT NULL DEFAULT 500,
    //  current_rolls integer NOT NULL DEFAULT -1,
    //  PRIMARY KEY(uid, rolls_to_double),
    //  CONSTRAINT allornothing_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    static RecordCache populateRecordCache(Difficulty difficulty) {
        String query = "SELECT uid, record_rolls, record_pot, record_cashout FROM allornothing_user WHERE rolls_to_double = " + difficulty.rollsToDouble + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            HashMap<Long, RecordEntry> personalBests = new HashMap<>();
            while (results.next()) {
                personalBests.put(results.getLong(1),
                    new RecordEntry(results.getInt(2), results.getLong(3), results.getLong(4)));
            }
            return new RecordCache(personalBests);
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

    static long fetchGlobalRollRecord(Difficulty difficulty, long server) {
        return fetchGlobalRecord(difficulty, server, "record_rolls");
    }

    static long fetchGlobalPotRecord(Difficulty difficulty, long server) {
        return fetchGlobalRecord(difficulty, server, "record_pot");
    }

    static long fetchGlobalCashoutRecord(Difficulty difficulty, long server) {
        return fetchGlobalRecord(difficulty, server, "record_cashout");
    }

    static long fetchGlobalRecord(Difficulty difficulty, long server, String column) {
        return CasinoDB.executeLongQuery("SELECT MAX(" + column
            + ") FILTER (WHERE uid IN (SELECT uid FROM casino_server_user WHERE server_id = "
            + server + ")) FROM allornothing_user WHERE rolls_to_double = "
            + difficulty.rollsToDouble + ";");
    }
}
