package com.c2t2s.hb;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

class Casino {

    // Hide default constructor
    private Casino() {}

    private static class User {
        private int work;
        private int fish;
        private int pick;
        private int rob;
        private long balance;
        private boolean inJail;
        private Timestamp timer;
        private Timestamp timer2;

        private User(int w, int f, int p, int r, long b, boolean jail, Timestamp time, Timestamp time2) {
            work = w;
            fish = f;
            pick = p;
            rob = r;
            balance = b;
            inJail = jail;
            timer = time;
            timer2 = time2;
        }

        private int getMorality() {
            return (2 * work) + fish - pick - (2 * rob);
        }

        private long getBalance() {
            return balance;
        }

        private boolean isJailed() {
            return inJail;
        }

        private Timestamp getTimer() {
            return timer;
        }

        private Timestamp getTimer2() {
            return timer2;
        }
    }

    private static class OverUnderGame {
        private int round;
        private long wager;
        private int target;

        private OverUnderGame(int round, long wager, int target) {
            this.round = round;
            this.wager = wager;
            this.target = target;
        }

        private int getRound() {
            return round;
        }

        private long getWager() {
            return wager;
        }

        private int getTarget() {
            return target;
        }
    }

    private static final long MONEY_MACHINE_UID = -1;
    static final int PREDICTION_OVER = 0;
    static final int PREDICTION_UNDER = 1;
    static final int PREDICTION_SAME = 2;
    static final String USER_NOT_FOUND_MESSAGE = "Unable to fetch user. If you are new run `/claim` to start";
    static final String PLACEHOLDER_NEWLINE_STRING = "\n:black_small_square:";

    // Returns 's' as needed to write the English version
    // of value, unless value == 1
    static String getPluralSuffix(long value) {
        return (value != 1 ? "s" : "");
    }

    static String formatTime(long time) {
        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) - (24 * days);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) - (1440 * days) - (60 * hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) - (86400 * days) - (3600 * hours) - (60 * minutes);
        StringBuilder output = new StringBuilder();
        if (days > 0) {
            output.append(days);
            output.append(" day");
            output.append(getPluralSuffix(days));
            output.append(", ");
        }
        if (hours > 0) {
            output.append(hours);
            output.append(" hour");
            output.append(getPluralSuffix(hours));
            output.append(", ");
        }
        if (minutes > 0) {
            output.append(minutes);
            output.append(" minute");
            output.append(getPluralSuffix(minutes));
            if (hours > 0) { output.append(','); }
            output.append(" and ");
        }
        output.append(seconds);
        output.append(" second");
        output.append(getPluralSuffix(seconds));
        return output.toString();
    }

    // Emulates String.repeat() but for versions before Java 11
    static String repeatString(String phrase, int times) {
        StringBuilder output = new StringBuilder();
        for (int i = times; i > 0; --i) {
            output.append(phrase);
        }
        return output.toString();
    }

    private static String stillTiredMessage(long remainingTime) {
        return "You are still tired. Try again in " + formatTime(remainingTime) + ".";
    }

    private static String insuffientBalanceMessage(long balance) {
        return "Your balance of " + balance + " is not enough to cover that!";
    }

// Payout:
// 25% 150
//     250 instead (50% if high morality)
// 25% 200
//     300 instead (50% if high morality)
// 25% 200
// 25% 250

    static String handleWork(long uid) {
        User user = getUser(uid);
        if (user == null) {
            return USER_NOT_FOUND_MESSAGE;
        }
        long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            if (user.isJailed()) {
                return "You are still in jail! Your sentence ends in " + formatTime(remainingTime) + ".";
            } else {
                return stillTiredMessage(remainingTime);
            }
        }
        String bonus = Events.checkRobBonus(uid, "`/work`");
        String output = "";
        int roll = HBMain.RNG_SOURCE.nextInt(100);
        if (roll < 25) {
            if (user.getMorality() > 5 && HBMain.RNG_SOURCE.nextInt(2) == 0) {
                output = ":scientist: You use your connections and work in The Lab for 2 hours and make 250 coins! Your new balance is " + logWork(uid, 250);
            } else {
                output = ":mechanic: You work as a mechanic for 2 hours and make 150 coins. Your new balance is " + logWork(uid, 150);
            }
        } else if (roll < 50) {
            if (user.getMorality() > 5 && HBMain.RNG_SOURCE.nextInt(2) == 0) {
                output = ":firefighter: You use your connections and put out fires and save kittens for 2 hours and make 300 coins! Your new balance is " + logWork(uid, 300);
            } else {
                output = ":farmer: You work hard in a field for 2 hours and make 200 coins. It ain't much, but it's honest work. Your new balance is " + logWork(uid, 200);
            }
        } else if (roll < 70) {
            output = ":cook: You work as a chef for 2 hours and make 200 coins. Your new balance is " + logWork(uid, 200);
        } else if (roll < 75) {
            output = ":detective: You work as a detective trying to find a missing satellite. You're unable to find it after 2 hours, but are still paid 200 coins. Your new balance is " + logWork(uid, 200);
        } else if (roll < 80) {
            output = ":drum: You play your drum in the local park. People smile as they pass by, and you make a total of 250 coins in tips! Your new balance is " + logWork(uid, 250);
        } else if (roll < 85) {
            output = ":potable_water: You win an internet contest and get to work a job job for 250 coins. `LET` `IT` `RIP` `!` `!` `!` Your new balance is " + logWork(uid, 250);
        } else {
            output = ":artist: You make an artistic masterpiece and sell it for 250 coins! Your new balance is " + logWork(uid, 250);
        }
        return output + bonus;
    }

// Payout:
//  :fish:              80% 40/50/60
//  :satellite_orbital:  5% 75
//  :octopus:            5% 75
//  :crab:               5% 100
//  :ring:               5% 250 (400 if high morality)

    static String handleFish(long uid) {
        User user = getUser(uid);
        if (user == null) {
            return USER_NOT_FOUND_MESSAGE;
        }
        long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            if (user.isJailed()) {
                return "There's no fishing pool in jail! Your sentence ends in " + formatTime(remainingTime) + ".";
            } else {
                return stillTiredMessage(remainingTime);
            }
        }
        String bonus = Events.checkPickBonus(uid, "`/fish`");
        String output = "";
        int roll = HBMain.RNG_SOURCE.nextInt(100);
        if (roll < 80) {
            int fish = (HBMain.RNG_SOURCE.nextInt(3) + 4);
            output = ":fish: You fish for 30 minutes and catch " + fish
                + " fish. You sell them for " + (fish * 10) + " coins. Your new balance is "
                + logFish(uid, false, fish * 10);
        } else if (roll < 85) {
            output = ":satellite_orbital: You fish up a satellite??? You're not sure how it got there, but you turn it into The Lab, and they pay you 75 coins. Your new balance is "
                + logFish(uid, false, 75);
        } else if (roll < 90) {
            output = ":blowfish: You fish up a pufferfish! You feed it a carrot and it thanks you with 75 coins. Your new balance is "
//          output = ":octopus: You fish up an octopus, and cook it into delicious sushi worth 75 coins. Your new balance is "
                + logFish(uid, false, 75);
        } else if (roll < 95) {
            output = ":crocodile: You fish up a baby crocodile! You take it back to someone who may know about it and they exchange it for 100 coins of grey items and some fishing hooks. Your new balance is "
//          output = ":crab: You fish up crab. It pays you 100 coins to let it return to its dance party. Your new balance is "
                + logFish(uid, false, 100);
        } else {
            if (user.getMorality() > 5) {
                output = ":ring: You fish up a ring. Since you're a good person you return it to its rightful owner and are rewarded with 400 coins! Your new balance is "
                    + logFish(uid, true, 400);
            } else {
                output = ":ring: You fish up a ring, and sell it for 250 coins! Your new balance is "
                    + logFish(uid, true, 250);
            }
        }
        return output + bonus;
    }

// Payout:
//  :books:             5% -10
//   (:slot_machine:)       500 (replaces books if bad)
//  :motorway:          10% 0
//  :house_adandoned:   10% 5
//  :house:             25% 200
//  :convenience_store: 25% 250
//  :bank:              25% 350

    static String handleRob(long uid) {
        User user = getUser(uid);
        if (user == null) {
            return USER_NOT_FOUND_MESSAGE;
        }
        long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            if (user.isJailed()) {
                return "The guard gives you a funny look. You're still in jail for " + formatTime(remainingTime) + ".";
            } else {
                return stillTiredMessage(remainingTime);
            }
        }
        String bonus = Events.checkRobBonus(uid, "`/rob`");
        String output = "";
        if (HBMain.RNG_SOURCE.nextInt(2) == 0) {
            robFailed(uid);
            output = "You were caught! You are dragged off to jail for 2 hours.";
            return output + bonus;
        }
        int roll = HBMain.RNG_SOURCE.nextInt(100);
        if (roll < 5) {
            if (user.getMorality() < -10) {
                output = ":slot_machine: You use your criminal knowledge and rob the slot machine of 500 coins! Your new balance is "
                    + logRob(uid, true, 500) + "\nWait! Get away from that!";
            } else if (user.getBalance() > 10) {
                output = ":book: You rob The Bank! Wait, that's not The Bank, that's The Library. You pay the late fee of 10 coins for your overdue books and leave before the cops arrive. Your new balance is "
                    + logRob(uid, false, -10);
            } else {
                logRob(uid, false, 0);
                output = ":book: You rob The Bank! Wait, that's not The Bank, that's The Library. You quickly leave before the cops arrive.";
            }
        } else if (roll < 15) {
            logRob(uid, false, 0);
            output = ":motorway: You attempt a highway robbery, but your horse and six shooter are no match for modern automobiles.";
        } else if (roll < 25) {
            output = ":house_abandoned: You rob a house, but find it empty and abandoned. Except for 5 coins and a dead rat. Though is it still a rat if it is dead? You pick up the 5 coins and leave pondering the question. Your new balance is "
                + logRob(uid, false, 5);
        } else if (roll < 50) {
            output = ":house: You rob a rich looking house and get away with 200 coins. Your new balance is "
                + logRob(uid, false, 200);
        } else if (roll < 75) {
            output = ":convenience_store: You rob a convenience store and grab 250 coins from the register! Your new balance is "
                + logRob(uid, true, 250);
        } else if (roll < 80) {
            output = ":full_moon: With the help of some funny friends in overalls you steal THE MOON. The UN pays you 350 coins in ransom. Your new balance is "
                + logRob(uid, true, 350);
        } else {
            output = ":bank: You rob The Bank and grab 350 coins worth of diamonds! Your new balance is "
                + logRob(uid, true, 350);
        }
        return output + bonus;
    }

// Payout:
//  :paperclip:         10% 0
//  :satellite_orbital:  5% 0
//  :lungs:              5% 0 (150 if low morality)
//  :moneybag:          50% 30/50/70
//  :computer:          15% 100
//  :medal:             10% 125
//  :gem:                5% 250

    static String handlePickpocket(long uid) {
        User user = getUser(uid);
        if (user == null) {
            return USER_NOT_FOUND_MESSAGE;
        }
        long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            if (user.isJailed()) {
                return "The guard gives you a funny look. You're still in jail for " + formatTime(remainingTime) + ".";
            } else {
                return stillTiredMessage(remainingTime);
            }
        }
        String bonus = Events.checkPickBonus(uid, "`/pickpocket`");
        String output = "";
        if (HBMain.RNG_SOURCE.nextInt(2) == 0) {
            pickFailed(uid);
            output = "You were caught! You are dragged off to jail for 30 minutes.";
            return output + bonus;
        }
        int roll = HBMain.RNG_SOURCE.nextInt(100);
        if (roll < 10) {
            logPick(uid, false, 0);
            output = ":paperclip: You steal a paperclip, which you use to bundle together your wanted posters you took down.";
        } else if (roll < 15) {
            if (user.getMorality() < -10) {
                output = ":lungs: You pickpocket a pair of lungs. Using your criminal connections you find a buyer who pays 150 coins. Your new balance is "
                    + logPick(uid, true, 150);
            } else {
                logPick(uid, false, 0);
                output = ":lungs: You pickpocket a pair of lungs???? This was supposed to be a petty theft! You drop them on the ground and quickly run away.";
            }
        } else if (roll < 20) {
            logPick(uid, false, 0);
            output = ":satellite_orbital: You pickpocket an orbital satellite???? Unsure what to do with it you ditch it in a nearby lake.";
        } else if (roll < 70) {
            int haul = 30 + (HBMain.RNG_SOURCE.nextInt(3) * 20);
            output = ":moneybag: You successfully pickpocket " + haul + " coins. Your new balance is "
                + logPick(uid, false, haul);
        } else if (roll < 85) {
            output = ":computer: You pickpocket a laptop computer! You sell it for 100 coins, and your new balance is "
                + logPick(uid, false, 100);
        } else if (roll < 95) {
//          output = ":medal: You pickpocket a medal of pure gold! You sell it for 125 coins, and your new balance is "
            output = ":credit_card: You pickpocket mom's credit card! You note down the 3 wacky numbers on the back and purchase 125 coins. Your new balance is "
                + logPick(uid, false, 125);
        } else {
            output = ":gem: You grab a large diamond worth 250 coins!! Your new balance is "
                + logPick(uid, true, 250);
        }
        return output + bonus;
    }

    static String handleClaim(long uid, String name) {
        User user = getUser(uid);
        String response = "";
        if (user == null) {
            boolean error = CasinoDB.addUser(uid, name);
            if (!error) {
                response += "Welcome! You have been given an initial balance of 1000 coins";
            } else {
                response += "Unable to add new user. Something may have gone wrong :slight_frown:";
            }
        }
        return response + "\nTo earn money, use one of the following commands:"
            + "\n\t`/work` Work for 2 hours. This is a lawful pursuit"
            + "\n\t`/fish` Fish for 30 minutes. This is a lawful pursuit"
            + "\n\t`/pickpocket` Attempt to pickpocket. This is a criminal pursuit and risks 30 minutes in jail"
            + "\n\t`/rob` Attempt a robbery. This is a crimal pursuit and risks 2 hours of jail time"
            + "\nIf you get a particularly lawful or particularly crimal record, you may get unique options."
            + "You can also gamble with `/guess`, `/bigguess`, `/hugeguess`, `/slots`, `/minislots`, `/moneymachine`, `/overunder`, or `/blackjack`";
    }

// Big Guess Payout:
//  Correct:        1/10  10:1

    static String handleGuess(long uid, long guess, long amount) {
        User user = getUser(uid);
        if (user == null) {
            return USER_NOT_FOUND_MESSAGE;
        }
        long balance = user.getBalance();
        if (balance < amount) {
            return insuffientBalanceMessage(balance);
        }
        int correct = HBMain.RNG_SOURCE.nextInt(10) + 1;
        StringBuilder output = new StringBuilder("Guessed " + guess + "\n");
        if (guess == correct) {
            guessWin(uid, amount, 9 * amount);
            output.append("Correct! You win " + (10 * amount) + "! New balance is " + addMoney(uid, 9 * amount));
        } else {
            guessLoss(uid, amount);
            output.append("The correct value was " + correct + ". Your new balance is " + takeMoney(uid, amount));
        }
        return output.toString();
    }

// Huge Guess Payout:
//  Correct:    1/100  100:1

    static String handleHugeGuess(long uid, long guess, long amount) {
        User user = getUser(uid);
        if (user == null) {
            return USER_NOT_FOUND_MESSAGE;
        }
        long balance = user.getBalance();
        if (balance < amount) {
            return insuffientBalanceMessage(balance);
        }
        int correct = HBMain.RNG_SOURCE.nextInt(100) + 1;
        StringBuilder output = new StringBuilder("Guessed " + guess + "\n");
        if (guess == correct) {
            hugeGuessWin(uid, amount, 99 * amount);
            output.append("Correct! You win " + (100 * amount) + "! New balance is " + addMoney(uid, 99 * amount));
        } else {
            hugeGuessLoss(uid, amount);
            output.append("The correct value was " + correct + ". Your new balance is " + takeMoney(uid, amount));
        }
        return output.toString();
    }

    static String handleFeed(long uid, Long amount) {
        User user = getUser(uid);
        if (user == null) {
            return USER_NOT_FOUND_MESSAGE;
        }
        if (user.getBalance() < amount) {
            return insuffientBalanceMessage(user.getBalance());
        }
        long remainingTime = user.getTimer2().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            return "You have recently fed the money machine. Try again in " + formatTime(remainingTime) + ".";
        }
        User moneyMachine = getUser(MONEY_MACHINE_UID);
        if (moneyMachine == null) {
            return "A database error occurred. The money machine is nowhere to be found.";
        }
        long pot = moneyMachine.getBalance() + amount;
        double winChance = 0.25;
        if (pot < 20000) {
            winChance = 0.05 + (0.2 * ((double)pot / 20000));
        }
        StringBuilder output = new StringBuilder("Fed " + amount + " coins to the Money Machine\n");
        if (HBMain.RNG_SOURCE.nextDouble() < winChance && (amount >= 100 || HBMain.RNG_SOURCE.nextInt(100) < amount)) { // Win
            long winnings = (long)(pot * 0.75);
            long newPot = pot - winnings;
            output.append("The money machine is satisfied! :dollar: You win "
                + winnings + "! Your new balance is " + moneyMachineWin(uid, winnings, winnings - amount, newPot)
                + ". The pot is now " + newPot + ".");
        } else { // Lose
            output.append("Even with a current pot of " + pot + " the money machine is still hungry. Your new balance is "
                + moneyMachineLoss(uid, amount));
        }
        return output.toString();
    }

    static String handlePot() {
        User moneyMachine = getUser(MONEY_MACHINE_UID);
        if (moneyMachine == null) {
            return "A database error occurred. The money machine is nowhere to be found.";
        }
        return "The current Money Machine pot is " + moneyMachine.getBalance();
    }

// Slots Payout:
//  5 of a kind: 1/625              30:1
//  4 of a kind: 4/125              10:1
//  3 of a kind: 32/125             1.5:1
//  Fruit Salad: 24/125             2:1
//  1 diamond:   1/20               1:1
//  2 diamonds:  1/1000             10:1
//  3 diamonds:  1/100 000          100:1
//  4 diamonds:  1/20 000 000       1000:1
//  5 diamonds:  1/10 000 000 000   10000:1

    static HBMain.MultistepResponse handleSlots(long uid, long amount) {
        User user = getUser(uid);
        if (user == null) {
            return new HBMain.MultistepResponse(USER_NOT_FOUND_MESSAGE);
        }
        long balance = user.getBalance();
        if (balance < amount) {
            return new HBMain.MultistepResponse(insuffientBalanceMessage(balance));
        }
        int cherries = 0;
        int oranges = 0;
        int lemons = 0;
        int blueberries = 0;
        int grapes = 0;
        int diamonds = 0;
        StringBuilder output = new StringBuilder("Bid " + amount + " on slots\n");
        String placeholder = ":blue_square:";
        int winnings = 0;
        List<String> responseSteps = new ArrayList<>();
        responseSteps.add(output + repeatString(placeholder, 5) + PLACEHOLDER_NEWLINE_STRING);
        for (int i = 0; i < 5; ++i) {
            switch (HBMain.RNG_SOURCE.nextInt(5)) {
                case 0:
                    output.append(":cherries:");
                    cherries++;
                    break;
                case 1:
                    output.append(":tangerine:");
                    oranges++;
                    break;
                case 2:
                    output.append(":lemon:");
                    lemons++;
                    break;
                case 3:
                    output.append(":blueberries:");
                    blueberries++;
                    break;
                case 4:
                    if (HBMain.RNG_SOURCE.nextInt(20) == 10) {
                        output.append(":gem:");
                        diamonds++;
                    } else {
                        output.append(":grapes:");
                        grapes++;
                    }
                    break;
                default:
                    System.out.println("Out of range value encountered when generating slots");
            }
            responseSteps.add(output.toString() + repeatString(placeholder, 4 - i)
                + PLACEHOLDER_NEWLINE_STRING);
        }
        output.append("\n");
        int winCondition = 0;
        if (cherries == 5 || oranges == 5 || lemons == 5 || blueberries == 5 || grapes == 5) {
            output.append(":moneybag::moneybag: 5 OF A KIND!!! :moneybag::moneybag:");
            winnings += 30 * amount;
            winCondition = 5;
        } else if (cherries == 4 || oranges == 4 || lemons == 4 || blueberries == 4 || grapes == 4) {
            output.append(":moneybag: 4 of a kind!! :moneybag: ");
            winnings += 10 * amount;
            winCondition = 4;
        } else if (cherries == 3 || oranges == 3 || lemons == 3 || blueberries == 3 || grapes == 3) {
            output.append("3 of a kind. ");
            winnings += (int)(1.5 * amount);
            winCondition = 3;
        } else if (cherries == 1 && oranges == 1 && lemons == 1 && blueberries == 1 && grapes == 1) {
            output.append("Fruit salad! ");
            winnings += 2 * amount;
            winCondition = 1;
        }
        if (diamonds > 0) {
            output.append(":gem: " + diamonds + " diamond" + (diamonds == 1 ? "" : "s") + "! :gem: ");
            if (diamonds > 3) { output.append("Jackpot!!! "); }
            winnings += amount * (int)Math.pow(10, (diamonds - 1));
        }
        if (amount > winnings) {
            balance = takeMoney(uid, amount - winnings);
        } else {
            balance = addMoney(uid, winnings - amount);
        }
        if (winnings > 0) {
            output.append("Winnings: " + (winnings) + " ");
        }
        output.append("New balance: " + balance);
        logSlots(uid, amount, winnings, diamonds, winCondition);
        responseSteps.add(output.toString());
        return new HBMain.MultistepResponse(responseSteps);
    }

// Minislots Payout
//  3 of a kind: 1/25      5:1
//  2 of a kind: 12/25     1.6:1
//  1 diamond:   3/100     0.4:1
//  2 diamonds:  3/10000   10:1
//  3 diamonds:  1/1000000 100:1

    static HBMain.MultistepResponse handleMinislots(long uid, long amount) {
        User user = getUser(uid);
        if (user == null) {
            return new HBMain.MultistepResponse(USER_NOT_FOUND_MESSAGE);
        }
        long balance = user.getBalance();
        if (balance < amount) {
            return new HBMain.MultistepResponse(insuffientBalanceMessage(balance));
        }
        int cherries = 0;
        int oranges = 0;
        int lemons = 0;
        int blueberries = 0;
        int grapes = 0;
        int diamonds = 0;
        StringBuilder output = new StringBuilder("Bid " + amount + " on mini slots\n");
        String placeholder = ":blue_square:";
        int winnings = 0;
        List<String> responseSteps = new ArrayList<>();
        responseSteps.add(output + repeatString(placeholder, 3) + PLACEHOLDER_NEWLINE_STRING);
        for (int i = 0; i < 3; i++) {
            switch (HBMain.RNG_SOURCE.nextInt(5)) {
            case 0:
                output.append(":cherries:");
                cherries++;
                break;
            case 1:
                output.append(":tangerine:");
                oranges++;
                break;
            case 2:
                output.append(":lemon:");
                lemons++;
                break;
            case 3:
                output.append(":blueberries:");
                blueberries++;
                break;
            case 4:
                if (HBMain.RNG_SOURCE.nextInt(20) == 10) {
                    output.append(":gem:");
                    diamonds++;
                } else {
                    output.append(":grapes:");
                    grapes++;
                }
                break;
            default:
                System.out.println("Out of range value encountered when generating slots");
            }
            responseSteps.add(output.toString() + repeatString(placeholder, 2 - i)
                + PLACEHOLDER_NEWLINE_STRING);
        }
        output.append("\n");
        if (cherries == 3 || oranges == 3 || lemons == 3 || blueberries == 3 || grapes == 3) {
            output.append(":moneybag: 3 of a kind! :moneybag: ");
            winnings += 5 * amount;
        } else if (cherries == 2 || oranges == 2 || lemons == 2 || blueberries == 2 || grapes == 2) {
            output.append("2 of a kind. ");
            winnings += (int)(1.6 * amount);
        }
        if (diamonds == 1) {
            output.append("1 bonus diamond :gem:");
            winnings += (int)(0.4 * amount);
        } else if (diamonds > 1) {
            output.append(":gem: " + diamonds + " diamond" + (diamonds == 1 ? "" : "s") + "! :gem: Jackpot!!! ");
            winnings += amount * (int)Math.pow(10, (diamonds - 1));
        }
        if (amount > winnings) {
            balance = takeMoney(uid, amount - winnings);
        } else {
            balance = addMoney(uid, winnings - amount);
        }
        if (winnings > 0) {
            output.append("Winnings: " + (winnings) + " ");
        }
        output.append("New balance: " + balance);
        logMinislots(uid, amount, winnings, diamonds);
        responseSteps.add(output.toString());
        return new HBMain.MultistepResponse(responseSteps);
    }

// OverUnder Payout:
//  2 correct then 1 wrong: ~2/11 1:1
//  3 correct:              ~3/11 3:1

    static HBMain.SingleResponse handleOverUnderInitial(long uid, long amount) {
        OverUnderGame game = getOverUnderRound(uid);
        if (game != null && game.getRound() != -1) {
            return new HBMain.SingleResponse("You already have an active game: Round " + game.getRound()
                + " with the current value " + game.getTarget()
                + ".\nUse `over`, `under`, or `same` to predict which the next value will be",
                ButtonRows.OVERUNDER_BUTTONS);
        }
        long balance = checkBalance(uid);
        if (balance < 0) {
            return new HBMain.SingleResponse("Unable to start game. Balance check failed or was negative (" + balance +")");
        } else if (balance < amount) {
            return new HBMain.SingleResponse("Your current balance of " + balance + " is not enough to cover that");
        }
        int target = HBMain.RNG_SOURCE.nextInt(10) + 1;
        logInitialOverUnder(uid, amount, target);
        return new HBMain.SingleResponse("Bid " + amount + " on overunder\nYour initial value is " + target
            + ". Predict if the next value (1-10) will be `over`, `under`, or the `same`", ButtonRows.OVERUNDER_BUTTONS);
    }

    static HBMain.SingleResponse handleOverUnderFollowup(long uid, int prediction) {
        OverUnderGame game = getOverUnderRound(uid);
        if (game == null || game.getRound() == -1) {
            return new HBMain.SingleResponse("No active game found. Use `/overunder new` to start a new game");
        }
        int target = HBMain.RNG_SOURCE.nextInt(10) + 1;
        if ((prediction == PREDICTION_OVER && target > game.getTarget())
                || (prediction == PREDICTION_UNDER && target < game.getTarget())
                || (prediction == PREDICTION_SAME && target == game.getTarget())) { // Correct
            if (game.getRound() == 3) {
                return new HBMain.SingleResponse("Correct! The value was " + target + ". You win " + (3 * game.getWager())
                    + "!\nYour new balance is " + logOverUnderWin(uid, 3 * game.getWager(), true, game.getWager()));
            } else {
                logOverUnderProgress(uid, game.getRound() + 1, target);
                return new HBMain.SingleResponse("Correct! Your new value for the " + ((game.getRound() + 1) == 2 ? "second" : "third")
                    + " round is " + target, ButtonRows.OVERUNDER_BUTTONS); 
            }
        } else { // Loss
            String correct = "";
            if (target > game.getTarget()) {
                correct = "over";
            } else if (target < game.getTarget()) {
                correct = "under";
            } else {
                correct = "same";
            }
            String response = "The answer was " + correct + ": " + target + ".";
            if (game.getRound() == 3) {
                return new HBMain.SingleResponse(response + " With 2 correct your " + game.getWager()
                    + " coins are returned. Your new balance is " + logOverUnderWin(uid, game.getWager(), false, game.getWager()));
            } else {
                logOverUnderLoss(uid);
                return new HBMain.SingleResponse(response + " Your current balance is " + checkBalance(uid));
            }
        }
    }

    static String handleBalance(long uid) {
        User user = getUser(uid);
        if (user == null) {
            return USER_NOT_FOUND_MESSAGE;
        }
        return "Your current balance is " + user.getBalance() + " coin" + getPluralSuffix(user.getBalance()) + ".";
    }

    static String handleGive(long donorUid, long recipientUid, long amount) {
        if (amount <= 0) {
            return "Can't give someone a negative number of coins. Try asking them nicely if you want money.";
        }
        long donorBalance = checkBalance(donorUid);
        if (donorBalance < 0) {
            return "Unable to give money. Balance check failed or was negative (" + donorBalance +")";
        } else if (donorBalance < amount) {
            return insuffientBalanceMessage(donorBalance);
        }
        if (donorUid == recipientUid) {
            return "You give yourself " + amount + ". Your balance is unchanged for some reason.";
        }
        long recipientBalance = checkBalance(recipientUid);
        if (recipientBalance == -1) {
            return "Unable to give money. Has that user run `/claim`?";
        }
        donorBalance = takeMoney(donorUid, amount);
        recipientBalance = addMoney(recipientUid, amount);
        if (donorBalance < 0) {
            return "Unable to process transaction";
        } else {
            return "Gave " + amount + " coins to <@" + recipientUid + ">\nYour new balance is " + donorBalance
                + "\nTheir new balance is " + recipientBalance;
        }
    }

    static String handleLeaderboard(long entries) {
        if (entries > 10) {
            entries = 10;
        }
        return parseLeaderboard(entries);
    }

    //////////////////////////////////////////////////////////

    //CREATE TABLE IF NOT EXISTS money_user (
    //  uid bigint PRIMARY KEY,
    //  name varchar(40) NOT NULL DEFAULT '',
    //  nickname varchar(40) NOT NULL DEFAULT '',
    //  balance bigint NOT NULL DEFAULT 0,
    //  in_jail boolean NOT NULL DEFAULT false,
    //  last_claim timestamp NOT NULL DEFAULT '2021-01-01 00:00:00',
    //  timestamp2 timestamp NOT NULL DEFAULT '2021-01-01 00:00:00'
    //);

    // CREATE TABLE IF NOT EXISTS job_user (
    //  uid bigint PRIMARY KEY,
    //  work_count integer NOT NULL DEFAULT 0,
    //  work_profit bigint NOT NULL DEFAULT 0,
    //  fish_count integer NOT NULL DEFAULT 0,
    //  fish_jackpots integer NOT NULL DEFAULT 0,
    //  fish_profit bigint NOT NULL DEFAULT 0,
    //  pick_count integer NOT NULL DEFAULT 0,
    //  pick_fails integer NOT NULL DEFAULT 0,
    //  pick_jackpots integer NOT NULL DEFAULT 0,
    //  pick_profit bigint NOT NULL DEFAULT 0,
    //  rob_count integer NOT NULL DEFAULT 0,
    //  rob_fails integer NOT NULL DEFAULT 0,
    //  rob_jackpots integer NOT NULL DEFAULT 0,
    //  rob_profit bigint NOT NULL DEFAULT 0,
    //  jail_time bigint NOT NULL DEFAULT 0,
    //  CONSTRAINT jobs_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS slots_user (
    //  uid bigint PRIMARY KEY,
    //  pulls integer NOT NULL DEFAULT 0,
    //  diamonds integer NOT NULL DEFAULT 0,
    //  spent bigint NOT NULL DEFAULT 0,
    //  winnings bigint NOT NULL DEFAULT 0,
    //  threes integer NOT NULL DEFAULT 0,
    //  fours integer NOT NULL DEFAULT 0,
    //  fives integer NOT NULL DEFAULT 0,
    //  fruitsalads integer NOT NULL DEFAULT 0,
    //  CONSTRAINT slots_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS guess_user (
    //  uid bigint PRIMARY KEY,
    //  guesses integer NOT NULL DEFAULT 0,
    //  correct integer NOT NULL DEFAULT 0,
    //  spent bigint NOT NULL DEFAULT 0,
    //  winnings bigint NOT NULL DEFAULT 0,
    //  CONSTRAINT guess_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS hugeguess_user (
    //  uid bigint PRIMARY KEY,
    //  guesses integer NOT NULL DEFAULT 0,
    //  correct integer NOT NULL DEFAULT 0,
    //  spent bigint NOT NULL DEFAULT 0,
    //  winnings bigint NOT NULL DEFAULT 0,
    //  CONSTRAINT hugeguess_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS minislots_user (
    //  uid bigint PRIMARY KEY,
    //  pulls integer NOT NULL DEFAULT 0,
    //  diamonds integer NOT NULL DEFAULT 0,
    //  spent integer NOT NULL DEFAULT 0,
    //  winnings integer NOT NULL DEFAULT 0,
    //  twos integer NOT NULL DEFAULT 0,
    //  threes integer NOT NULL DEFAULT 0,
    //  CONSTRAINT minislots_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS moneymachine_user (
    //  uid bigint PRIMARY KEY,
    //  chocolate_coins bigint NOT NULL DEFAULT 0,
    //  chocolate_spent bigint NOT NULL DEFAULT 0,
    //  feeds integer NOT NULL DEFAULT 0,
    //  wins integer NOT NULL DEFAULT 0,
    //  spent integer NOT NULL DEFAULT 0,
    //  winnings integer NOT NULL DEFAULT 0,
    //  CONSTRAINT moneymachine_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS overunder_user (
    //  uid bigint PRIMARY KEY,
    //  played integer NOT NULL DEFAULT 0,
    //  consolations integer NOT NULL DEFAULT 0,
    //  wins integer NOT NULL DEFAULT 0,
    //  spent bigint NOT NULL DEFAULT 0,
    //  winnings bigint NOT NULL DEFAULT 0,
    //  bet integer NOT NULL DEFAULT -1,
    //  round integer NOT NULL DEFAULT -1,
    //  target integer NOT NULL DEFAULT -1,
    //  CONSTRAINT overunder_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    static long checkBalance(long uid) {
        return CasinoDB.executeLongQuery("SELECT balance FROM money_user WHERE uid = " + uid + ";");
    }

    static long takeMoney(long uid, long amount) {
        return CasinoDB.executeLongQuery("UPDATE money_user SET balance = balance - "
            + amount + " WHERE uid = " + uid + " RETURNING balance;");
    }

    static long addMoney(long uid, long amount) {
        return CasinoDB.executeLongQuery("UPDATE money_user SET balance = balance + "
            + amount + " WHERE uid = " + uid + " RETURNING balance;");
    }

    private static long addWorkMoney(long uid, int amount, String delay) {
        return CasinoDB.executeLongQuery("UPDATE money_user SET (in_jail, balance, last_claim) = (false, balance + "
            + amount + ", NOW() + INTERVAL '" + delay + "') WHERE uid = " + uid + " RETURNING balance;");
    }

    private static void setJailTime(long uid, String interval) {
        CasinoDB.executeUpdate("UPDATE money_user SET (in_jail, last_claim) = (true, NOW() + INTERVAL '"
            + interval + "') WHERE uid = " + uid + ";");
    }

    private static void setTimer2Time(long uid, String interval) {
        CasinoDB.executeUpdate("UPDATE money_user SET timestamp2 = NOW() + INTERVAL '"
            + interval + "' WHERE uid = " + uid + ";");
    }

    private static String parseLeaderboard(long entries) {
        String query = "SELECT name, balance FROM money_user ORDER BY balance DESC LIMIT " + entries + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            StringBuilder leaderboard = new StringBuilder();
            int place = 1;
            while (results.next()) {
                leaderboard.append("#" + place++ + " ");
                String name = results.getString(1);
                if (name.contains("#")) {
                    name = name.substring(0, name.indexOf('#'));
                }
                leaderboard.append(name + " " + results.getLong(2) + "\n");
            }
            return leaderboard.toString();
        }, "");
    }

    private static void hugeGuessWin(long uid, long spent, long winnings) {
        CasinoDB.executeUpdate("UPDATE hugeguess_user SET (guesses, correct, spent, winnings) = (guesses + 1, correct + 1, spent + "
            + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
    }

    private static void hugeGuessLoss(long uid, long spent) {
        CasinoDB.executeUpdate("UPDATE hugeguess_user SET (guesses, spent) = (guesses + 1, spent + "
                + spent + ") WHERE uid = " + uid + ";");
    }

    private static void guessWin(long uid, long spent, long winnings) {
        CasinoDB.executeUpdate("UPDATE guess_user SET (guesses, correct, spent, winnings) = (guesses + 1, correct + 1, spent + "
            + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
    }

    private static void guessLoss(long uid, long spent) {
        CasinoDB.executeUpdate("UPDATE guess_user SET (guesses, spent) = (guesses + 1, spent + "
            + spent + ") WHERE uid = " + uid + ";");
    }

    private static void logSlots(long uid, long spent, long winnings, int diamonds, int winCondition) {
        CasinoDB.executeUpdate("UPDATE slots_user SET (pulls, diamonds, spent, winnings, threes, fours, fives, fruitsalads) = (pulls + 1, diamonds + "
            + diamonds + ", spent + " + spent + ", winnings + " + winnings + ", threes + "
            + (winCondition == 3 ? 1 : 0) + ", fours + " + (winCondition == 4 ? 1 : 0) + ", fives + "
            + (winCondition == 5 ? 1 : 0) + ", fruitsalads + " + (winCondition == 1 ? 1 : 0) + ") WHERE uid = " + uid + ";");
    }

    private static void logMinislots(long uid, long spent, long winnings, int diamonds) {
        CasinoDB.executeUpdate("UPDATE minislots_user SET (pulls, diamonds, spent, winnings) = (pulls + 1, diamonds + "
            + diamonds + ", spent + " + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
    }

    private static User getUser(long uid) {
        String query = "SELECT work_count, fish_count, pick_count, rob_count, balance, in_jail, last_claim, timestamp2 FROM money_user NATURAL JOIN job_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int work = results.getInt(1);
                int fish = results.getInt(2);
                int pick = results.getInt(3);
                int rob = results.getInt(4);
                long balance = results.getLong(5);
                boolean isJail = results.getBoolean(6);
                Timestamp time = results.getTimestamp(7);
                Timestamp time2 = results.getTimestamp(8);
                return new Casino.User(work, fish, pick, rob, balance, isJail, time, time2);
            }
            return null;
        }, null);
    }

    private static long logWork(long uid, int income) {
        long balance = addWorkMoney(uid, income, "2 hours");
        CasinoDB.executeUpdate("UPDATE job_user SET (work_count, work_profit) = (work_count + 1, "
            + "work_profit + " + income + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static long logFish(long uid, boolean rare, int income) {
        long balance = addWorkMoney(uid, income, "30 minutes");
        CasinoDB.executeUpdate("UPDATE job_user SET (fish_count, fish_jackpots, fish_profit) = (fish_count + 1, fish_jackpots + " 
            + (rare ? 1 : 0) + ", fish_profit + " + income + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static void pickFailed(long uid) {
        setJailTime(uid, "30 minutes");
        CasinoDB.executeUpdate("UPDATE job_user SET (pick_count, pick_fails, jail_time) = (pick_count + 1, pick_fails + 1, jail_time + 30) WHERE uid = "
            + uid + ";");
    }

    private static long logPick(long uid, boolean rare, int income) {
        long balance = addMoney(uid, income);
        CasinoDB.executeUpdate("UPDATE job_user SET (pick_count, pick_jackpots, pick_profit) = (pick_count + 1, pick_jackpots + "
            + (rare ? 1 : 0) + ", pick_profit + " + income + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static void robFailed(long uid) {
        setJailTime(uid, "2 hours");
        CasinoDB.executeUpdate("UPDATE job_user SET (rob_count, rob_fails, jail_time) = (rob_count + 1, rob_fails + 1, jail_time + 120) WHERE uid = "
                + uid + ";");
    }

    private static long logRob(long uid, boolean rare, int income) {
        long balance = addMoney(uid, income);
        CasinoDB.executeUpdate("UPDATE job_user SET (rob_count, rob_jackpots, rob_profit) = (rob_count + 1, rob_jackpots + "
            + (rare ? 1 : 0) + ", rob_profit + " + income + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static long moneyMachineWin(long uid, long winnings, long profit, long newPot) {
        long balance = addMoney(uid, profit);
        CasinoDB.executeUpdate("UPDATE money_user SET balance = " + newPot + " WHERE uid = " + MONEY_MACHINE_UID + ";");
        setTimer2Time(uid, "1 minute");
        CasinoDB.executeUpdate("UPDATE moneymachine_user SET (feeds, wins, winnings) = (feeds + 1, wins + 1, winnings + "
            + winnings + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static long moneyMachineLoss(long uid, long bet) {
        long balance = takeMoney(uid, bet);
        addMoney(MONEY_MACHINE_UID, bet);
        setTimer2Time(uid, "1 minute");
        CasinoDB.executeUpdate("UPDATE moneymachine_user SET (feeds, spent) = (feeds + 1, spent + "
            + bet + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static void logInitialOverUnder(long uid, long bet, int target) {
        takeMoney(uid, bet);
        CasinoDB.executeUpdate("UPDATE overunder_user SET (round, played, spent, bet, target) = (1, played + 1, spent + "
            + bet + ", " + bet + ", " + target + ") WHERE uid = " + uid + ";");
    }

    private static void logOverUnderProgress(long uid, int round, int target) {
        CasinoDB.executeUpdate("UPDATE overunder_user SET (round, target) = ("
            + round + ", " + target + ") WHERE uid = " + uid + ";");
    }

    private static void logOverUnderLoss(long uid) {
        CasinoDB.executeUpdate("UPDATE overunder_user SET (bet, round, target) = (-1, -1, -1) WHERE uid = "
            + uid + ";");
    }

    private static long logOverUnderWin(long uid, long winnings, boolean thirdRound, long wager) {
        long balance = addMoney(uid, winnings);
        CasinoDB.executeUpdate("UPDATE overunder_user SET (bet, round, target, consolations, wins, winnings) = (-1, -1, -1, consolations + "
            + (thirdRound ? 0 : 1) + ", wins + " + (thirdRound ? 1 : 0) + ", winnings + "
            + (winnings - wager) + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static OverUnderGame getOverUnderRound(long uid) {
        String query = "SELECT round, bet, target FROM overunder_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int round = results.getInt(1);
                int wager = results.getInt(2);
                int target = results.getInt(3);
                return new Casino.OverUnderGame(round, wager, target);
            }
            return null;
        }, null);
    }

}
