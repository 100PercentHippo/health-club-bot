package com.c2t2s.hb;

import java.util.ArrayList;
import java.util.List;

import org.postgresql.translation.messages_bg;

import com.c2t2s.hb.AllOrNothing.ActiveGame;

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
    static final String ALLORNOTHING70_STRING = "allornothing|70";
    static final String ALLORNOTHING80_STRING = "allornothing|80";
    static final String ALLORNOTHING90_STRING = "allornothing|90";

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
        ALLORNOTHING70(ALLORNOTHING70_STRING, "/allornothing 70%"),
        ALLORNOTHING80(ALLORNOTHING80_STRING, "/allornothing 80%"),
        ALLORNOTHING90(ALLORNOTHING90_STRING, "/allornothing 90%");

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
            case ALLORNOTHING70_STRING:
                return handleAllOrNothingStats(uid, 2);
            case ALLORNOTHING80_STRING:
                return handleAllOrNothingStats(uid, 3);
            case ALLORNOTHING90_STRING:
                return handleAllOrNothingStats(uid, 7);
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

    static String handleAllOrNothingStats(long uid, int rollsToDouble) {
        StringBuilder output = new StringBuilder("`/allornothing` odds:");
        String description = "";
        switch (rollsToDouble) {
            case 2:
                output.append("\n\t70%: Payout increases by x1.41 per roll (70% chance)");
                description = "70%";
                break;
            case 3:
                output.append("\n\t80%: Payout increases by x1.26 per roll (80% chance)");
                description = "80%";
                break;
            case 7:
                output.append("\n\t90%: Payout increases by x1.10 per roll (90% chance)");
                description = "90%";
                break;
        }
        output.append("\n\nWorld Record holders for the " + description + " bracket:");
        output.append(getAllOrNothingWorldRecordRolls(rollsToDouble));
        output.append(getAllOrNothingWorldRecordPot(rollsToDouble));
        output.append(getAllOrNothingWorldRecordCashout(rollsToDouble));
        output.append("\n\nPersonal Bests for the " + description + " bracket:");
        output.append(getAllOrNothingPersonalBests(rollsToDouble, uid));
        return output.toString();
    }

    static String formatNames(List<String> names) {
        String formattedNames = "";
        for(String name: names) {
            formattedNames += name + ",";
        }
        if (formattedNames.length() > 1) {
            formattedNames = formattedNames.substring(0, formattedNames.length() - 1);
        }
        return formattedNames;
    }

    static String formatWorldRecordRolls(int rolls, int rollsToDouble, List<String> names) {
        // For calculating multiplier
        AllOrNothing.ActiveGame game = new AllOrNothing.ActiveGame(rolls, 1, AllOrNothing.Difficulty.getConstant(rollsToDouble));
        return "\n\tWorld Record Multiplier: " + AllOrNothing.payoutPercentFormat.format(game.getPayoutMultiplier())
            + " (" + formatNames(names) + ")";
    }

    static String formatWorldRecordPot(long pot, List<String> names) {
        return "\n\tWorld Record Potential Payout: " + pot + " (" + formatNames(names) + ")";
    }

    static String formatWorldRecordCashout(long cashout, List<String> names) {
        return "\n\tWorld Record Payout: " + cashout + " (" + formatNames(names) + ")";
    }

    static String formatPersonalBests(int rolls, long pot, long payout, int rollsToDouble) {
        // For calculating multiplier
        AllOrNothing.ActiveGame game = new AllOrNothing.ActiveGame(rolls, 1, AllOrNothing.Difficulty.getConstant(rollsToDouble));
        return "\n\tPersonal Best Multiplier: " + AllOrNothing.payoutPercentFormat.format(game.getPayoutMultiplier())
            + "\n\tPersonal Best Potential Payout: " + pot
            + "\n\tPersonal best Payout: " + payout;
    }

    //////////////////////////////////////////////////////////

    private static String getAllOrNothingWorldRecordRolls(int rollsToDouble) {
        String query = "SELECT record_rolls, nickname FROM allornothing_user NATURAL JOIN money_user WHERE record_rolls = "
            + "(SELECT MAX(record_rolls) FROM allornothing_user WHERE rolls_to_double = " + rollsToDouble + ") AND rolls_to_double = "
            + rollsToDouble + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            int rollRecord = 0;
            List<String> recordHolders = new ArrayList<>();
            while (results.next()) {
                rollRecord = results.getInt(1);
                recordHolders.add(results.getString(2));
            }
            return formatWorldRecordRolls(rollRecord, rollsToDouble, recordHolders);
        }, "Unable to fetch roll record holder(s)");
    }

    private static String getAllOrNothingWorldRecordPot(int rollsToDouble) {
        String query = "SELECT record_pot, nickname FROM allornothing_user NATURAL JOIN money_user WHERE record_pot = "
            + "(SELECT MAX(record_pot) FROM allornothing_user WHERE rolls_to_double = " + rollsToDouble + ") AND rolls_to_double = "
            + rollsToDouble + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            long potRecord = 0;
            List<String> recordHolders = new ArrayList<>();
            while (results.next()) {
                potRecord = results.getLong(1);
                recordHolders.add(results.getString(2));
            }
            return formatWorldRecordPot(potRecord, recordHolders);
        }, "Unable to fetch pot record holder(s)");
    }

    private static String getAllOrNothingWorldRecordCashout(int rollsToDouble) {
        String query = "SELECT record_cashout, nickname FROM allornothing_user NATURAL JOIN money_user WHERE record_cashout = "
            + "(SELECT MAX(record_cashout) FROM allornothing_user WHERE rolls_to_double = " + rollsToDouble + ") AND rolls_to_double = "
            + rollsToDouble + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            long cashoutRecord = 0;
            List<String> recordHolders = new ArrayList<>();
            while (results.next()) {
                cashoutRecord = results.getLong(1);
                recordHolders.add(results.getString(2));
            }
            return formatWorldRecordCashout(cashoutRecord, recordHolders);
        }, "Unable to fetch cashout record holder(s)");
    }

    private static String getAllOrNothingPersonalBests(int rollsToDouble, long uid) {
        String query = "SELECT record_rolls, record_pot, record_cashout FROM allornothing_user WHERE uid = " + uid
            + " AND rolls_to_double = " + rollsToDouble + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return formatPersonalBests(results.getInt(1), results.getLong(2), results.getLong(3), rollsToDouble);
            }
            return "Unable to fetch personal bests";
        }, "Unable to fetch personal bests");
    }
}
