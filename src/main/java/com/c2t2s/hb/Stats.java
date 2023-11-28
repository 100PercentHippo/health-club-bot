package com.c2t2s.hb;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

class Stats {

    static final NumberFormat oneDecimal = new DecimalFormat("#.#");
    static final NumberFormat twoDecimals = new DecimalFormat("#.##");
    static final NumberFormat twoDecimalPercent = new DecimalFormat("#.##%");

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
            + "\n\tAverage Payout: 200 coins (225 if high morality)"
            + "\n\n" + getWorkStats(uid);
    }

    static String formatWorkStats(int workCount, long workProfit, int morality) {
        return "`/work` stats:"
            + "\n\tTimes worked: " + workCount
            + (workCount > 0 ? "\n\tAverage payout: " + oneDecimal.format((double)workProfit / workCount) : "")
            + "\n\tTotal work profit: " + workProfit
            + "\n\tMorality: " + morality;
    }

    static String handleFishStats(long uid) {
        return "`/fish` odds:"
            + "\n\tPayout chance: 100%"
            + "\n\tAverage Payout: 65 coins (72.5 if high morality)"
            + "\n\n" + getFishStats(uid);
    }

    static String formatFishStats(int fishCount, long fishProfit, int fishJackpots, int morality) {
        return "`/fish` stats:"
            + "\n\tTimes fished: " + fishCount
            + "\n\tFish jackpots: " + fishJackpots
            + (fishCount > 0 ? "\n\tAverage payout: " + oneDecimal.format((double)fishProfit / fishCount) : "")
            + "\n\tTotal fish profit: " + fishProfit
            + "\n\tMorality: " + morality;
    }

    static String handleRobStats(long uid) {
        return "`/rob` odds:"
            + "\n\tPayout chance: 50%"
            + "\n\tAverage Payout: 200 coins (225 if low morality)"
            + "\n\n" + getRobStats(uid);
    }

    static String formatRobStats(int robCount, int robFails, int robJackpots, long robProfit, long jailTime, int morality) {
        return "`/rob` stats:"
            + "\n\tTimes robbed: " + robCount
            + (robCount > 0 ? "\n\tSuccess rate: " + twoDecimalPercent.format((double)(robCount - robFails) / robCount) : "")
            + "\n\tRob jackpots: " + robJackpots
            + (robCount - robFails > 0 ? "\n\tAverage payout: " + oneDecimal.format((double)robProfit / (robCount - robFails)): "")
            + "\n\tTotal jail time: " + jailTime + " minutes"
            + "\n\tMorality: " + morality;
    }

    static String handlePickpocketStats(long uid) {
        return "`/pickpocket` odds:"
            + "\n\tPayout chance: 50%"
            + "\n\tAverage Payout: 65 coins (72.5 if high morality)"
            + "\n\n" + getPickStats(uid);
    }

    static String formatPickpocketStats(int pickCount, int pickFails, int pickJackpots, long pickProfit, long jailTime, int morality) {
        return "`/pickpocket` stats:"
            + "\n\tTimes pickpocketed: " + pickCount
            + (pickCount > 0 ? "\n\tSuccess rate: " + twoDecimalPercent.format((double)(pickCount - pickFails) / pickCount) : "")
            + "\n\tPickpocket jackpots: " + pickJackpots
            + (pickCount - pickFails > 0 ? "\n\tAverage payout: " + oneDecimal.format((double)pickProfit / (pickCount - pickFails)) : "")
            + "\n\tTotal jail time: " + jailTime + " minutes"
            + "\n\tMorality: " + morality;
    }

    static String handleGuessStats(long uid) {
        return "`/guess` odds:"
            + "\n\tCorrect guess: 10:1 (10%)"
            + "\n\n" + getGuessStats(uid);
    }

    static String handleHugeguessStats(long uid) {
        return "`/hugeguess` odds:"
            + "\n\tCorrect guess: 100:1 (1%)"
            + "\n\n" + getHugeguessStats(uid);
    }

    static String formatGuessStats(String gameName, int guesses, int correct, long spent, long winnings) {
        return "`/" + gameName + "` stats:"
            + "\n\tGames played: " + guesses
            + (guesses > 0 ? "\n\tSuccess rate: " + twoDecimalPercent.format((double)correct / guesses) : "")
            + "\n\tTotal wagered: " + spent
            + "\n\tTotal won: " + winnings;
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
            + "\n```"
            + "\n\n" + getSlotstStats(uid);
    }

    static String formatSlotsStats(int pulls, int diamonds, long spent, long winnings, int threes, int fours, int fives, int fruitSalads) {
        return "`/slots` stats:"
            + "\n\tGames played: " + pulls
            + "\n\t5 of a kinds: " + fives
            + "\n\t4 of a kinds: " + fours
            + "\n\t3 of a kinds: " + threes
            + "\n\tFruit salads: " + fruitSalads
            + "\n\tTotal diamonds: " + diamonds
            + "\n\tTotal wagered: " + spent
            + "\n\tTotal won: " + winnings
            + (pulls > 0 ? "\n\tAverage payout amount: " + twoDecimals.format((30 * fives + 10 * fours + 1.5 * threes + 2 * fruitSalads + diamonds) / pulls) + "x" : "");
    }

    static String handleMinislotsStats(long uid) {
        return "`/minislots` odds:"
            + "\n```"
            + "\n\t3 of a kind:   5:1 (1/25)"
            + "\n\t2 of a kind: 1.6:1 (12/25)"
            + "\n\t1 diamond:   0.4:1 (3/100)"
            + "\n\t2 diamonds:   10:1 (3/10 000)"
            + "\n\t3 diamonds:  100:1 (1/1 000 0000)"
            + "\n```"
            + "\n\n" + getMinislotsStats(uid);
    }

    static String formatMinislotsStats(int pulls, int diamonds, long spent, long winnings, int threes, int twos) {
        return "`/minislots` stats:"
            + "\n\tGames played: " + pulls
            + "\n\t3 of a kinds: " + threes
            + "\n\t2 of a kinds: " + twos
            + "\n\tTotal diamonds: " + diamonds
            + "\n\tTotal wagered: " + spent
            + "\n\tTotal wong: " + winnings
            + (pulls > 0 ? "\n\tAverage payout amount: "  + twoDecimals.format((5 * threes + 1.6 * twos + 0.4 * diamonds) / pulls) + "x" : "");
    }

    static String handleFeedStats(long uid) {
        return "Money Machine odds:"
            + "\n\tBase payout chance 5%"
            + "\n\tIncreases up to 25% based on current pot size"
            + "\n\tReduced for overly small feed amounts"
            + "\n\n" + getFeedStats(uid);
    }

    static String formatFeedStats(int feeds, int wins, long spent, long winnings) {
        return "Money Machine stats:"
            + "\n\tTimes fed: " + feeds
            + "\n\tTimes won: " + wins
            + "\n\tTotal coins fed: " + spent
            + "\n\tTotal coins won: " + winnings;
    }

    static String handleOverunderStats(long uid) {
        return "`/overunder` odds:"
            + "\n\t2 correct answers: 1:1 (~15%)"
            + "\n\t3 correct answers: 2.5:1 (~33%)"
            + "\n\n" + getOverunderStats(uid);
    }

    static String formatOverunderStats(int played, int consolations, int wins, long spent, long winnings) {
        return "`/overunder` stats:"
            + "\n\tGames played: " + played
            + (played > 0 ?
                "\n\t2 correct: " + consolations + " (" + twoDecimalPercent.format((double)consolations / played) + ")"
                + "\n\t3 correct: " + wins + " (" + twoDecimalPercent.format((double)wins / played) + ")"
                : "")
            + "\n\tTotal wagered: " + spent
            + "\n\tTotal won: " + winnings
            + (played > 0 ? "\n\tAverage payout amount: " + twoDecimals.format((consolations + 2.5 * wins) / played) + "x" : "");
    }

    static String handleBlackjackStats(long uid) {
        return "`/blackjack` odds:"
            + "\n\tCorrect: 2:1 (Bit under 1/2 probably)"
            + "\n\tTie:           1:1 (Rare)"
            + "\n\n" + getBlackjackStats(uid);
    }

    static String formatBlackjackStats(int hands, int busts, int ties, int wins, long spent, long winnings) {
        return "`/blackjack` stats:"
            + "\n\tHands played: " + hands
            + "\n\tGames busted: " + busts
            + "\n\tGames tied: " + ties
            + "\n\tGames won: " + wins
            + "\n\tTotal wagered: " + wins
            + "\n\tTotal won: " + winnings
            + (hands > 0 ? "\n\tAverage payout amount: " + twoDecimals.format((ties + 2 * (double)wins) / hands) + "x" : "");
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

    private static final String MORALITY_ARG = "2 * work_count + fish_count - pick_count - 2 * rob_count AS morality";

    private static String getWorkStats(long uid) {
        String query = "SELECT work_count, work_profit, " + MORALITY_ARG + " FROM job_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int workCount = results.getInt(1);
                long workProfit = results.getLong(2);
                int morality = results.getInt(3);
                return formatWorkStats(workCount, workProfit, morality);
            }
            return "\nUnable to fetch personalized work stats: no rows returned";
        }, "\nUnable to fetch personalized work stats");
    }

    private static String getFishStats(long uid) {
        String query = "SELECT fish_count, fish_profit, fish_jackpots, " + MORALITY_ARG + " FROM job_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int fishCount = results.getInt(1);
                long fishProfit = results.getLong(2);
                int fishJackpots = results.getInt(3);
                int morality = results.getInt(4);
                return formatFishStats(fishCount, fishProfit, fishJackpots, morality);
            }
            return "\nUnable to fetch personalized fish stats";
        }, "\nUnable to fetch personalized fish stats");
    }

    private static String getPickStats(long uid) {
        String query = "SELECT pick_count, pick_fails, pick_jackpots, pick_profit, jail_time, " + MORALITY_ARG + " FROM job_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int pickCount = results.getInt(1);
                int pickFails = results.getInt(2);
                int pickJackpots = results.getInt(3);
                long pickProfit = results.getLong(4);
                long jailTime = results.getLong(5);
                int morality = results.getInt(6);
                return formatPickpocketStats(pickCount, pickFails, pickJackpots, pickProfit, jailTime, morality);
            }
            return "\nUnable to fetch personalized pickpocket stats";
        }, "\nUnable to fetch personalized pickpocket stats");
    }

    private static String getRobStats(long uid) {
        String query = "SELECT rob_count, rob_fails, rob_jackpots, rob_profit, jail_time, " + MORALITY_ARG + " FROM job_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int robCount = results.getInt(1);
                int robFails = results.getInt(2);
                int robJackpots = results.getInt(3);
                long robProfit = results.getLong(4);
                long jailTime = results.getLong(5);
                int morality = results.getInt(6);
                return formatRobStats(robCount, robFails, robJackpots, robProfit, jailTime, morality);
            }
            return "\nUnable to fetch personalized rob stats";
        }, "\nUnable to fetch personalized rob stats");
    }

    private static String getGuessStats(long uid) {
        String query = "SELECT guesses, correct, spent, winnings FROM guess_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int guesses = results.getInt(1);
                int correct = results.getInt(2);
                long spent = results.getLong(3);
                long winnings = results.getLong(4);
                return formatGuessStats("guess", guesses, correct, spent, winnings);
            }
            return "\nUnable to fetch personalized guess stats";
        }, "\nUnable to fetch personalized guess stats");
    }

    private static String getHugeguessStats(long uid) {
        String query = "SELECT guesses, correct, spent, winnings FROM hugeguess_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int guesses = results.getInt(1);
                int correct = results.getInt(2);
                long spent = results.getLong(3);
                long winnings = results.getLong(4);
                return formatGuessStats("hugeguess", guesses, correct, spent, winnings);
            }
            return "\nUnable to fetch personalized hugeguess stats";
        }, "\nUnable to fetch personalized hugeguess stats");
    }

    private static String getSlotstStats(long uid) {
        String query = "SELECT pulls, diamonds, spent, winnings, threes, fours, fives, fruitsalads FROM slots_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int pulls = results.getInt(1);
                int diamonds = results.getInt(2);
                long spent = results.getLong(3);
                long winnings = results.getLong(4);
                int threes = results.getInt(5);
                int fours = results.getInt(6);
                int fives = results.getInt(7);
                int fruitSalads = results.getInt(8);
                return formatSlotsStats(pulls, diamonds, spent, winnings, threes, fours, fives, fruitSalads);
            }
            return "\nUnable to fetch personalized slots stats";
        }, "\nUnable to fetch personalized slots stats");
    }

    private static String getMinislotsStats(long uid) {
        String query = "SELECT pulls, diamonds, spent, winnings, threes, twos FROM minislots_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int pulls = results.getInt(1);
                int diamonds = results.getInt(2);
                long spent = results.getLong(3);
                long winnings = results.getLong(4);
                int threes = results.getInt(5);
                int twos = results.getInt(6);
                return formatMinislotsStats(pulls, diamonds, spent, winnings, threes, twos);
            }
            return "\nUnable to fetch personalized minislots stats";
        }, "\nUnable to fetch personalized minislots stats");
    }

    private static String getFeedStats(long uid) {
        String query = "SELECT feeds, wins, spent, winnings FROM moneymachine_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int feeds = results.getInt(1);
                int wings = results.getInt(2);
                long spent = results.getLong(3);
                long winnings = results.getLong(4);
                return formatFeedStats(feeds, wings, spent, winnings);
            }
            return "\nUnable to fetch personalized money machine stats";
        }, "\nUnable to fetch personalized money machine stats");
    }

    private static String getOverunderStats(long uid) {
        String query = "SELECT played, consolations, wins, spent, winnings FROM overunder_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int played = results.getInt(1);
                int consolations = results.getInt(2);
                int wins = results.getInt(3);
                long spent = results.getLong(4);
                long winnings = results.getLong(5);
                return formatOverunderStats(played, consolations, wins, spent, winnings);
            }
            return "\nUnable to fetch personalized overunder stats";
        }, "\nUnable to fetch personalized overunder stats");
    }

    private static String getBlackjackStats(long uid) {
        String query = "SELECT hands, busts, ties, wins, spent, winnings FROM blackjack_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int hands = results.getInt(1);
                int busts = results.getInt(2);
                int ties = results.getInt(3);
                int wins = results.getInt(4);
                long spent = results.getLong(5);
                long winnings = results.getLong(6);
                return formatBlackjackStats(hands, busts, ties, wins, spent, winnings);
            }
            return "\nUnable to fetch personalized blackjack stats";
        }, "\nUnable to fetch personalized blackjack stats");
    }

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
