package com.c2t2s.hb;

import java.net.URISyntaxException;
import java.sql.*; //TODO: Remove the *
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Casino {

    public static class User {
        private int work;
        private int fish;
        private int pick;
        private int rob;
        private long balance;
        private boolean inJail;
        private Timestamp timer;
        private Timestamp timer2;

        public User(int w, int f, int p, int r, long b, boolean jail, Timestamp time, Timestamp time2) {
            work = w;
            fish = f;
            pick = p;
            rob = r;
            balance = b;
            inJail = jail;
            timer = time;
            timer2 = time2;
        }

        public int getMorality() {
            return (2 * work) + fish - pick - (2 * rob);
        }

        public long getBalance() {
            return balance;
        }

        public boolean isJailed() {
            return inJail;
        }

        public Timestamp getTimer() {
            return timer;
        }

        public Timestamp getTimer2() {
            return timer2;
        }
    }

    public static class OverUnderGame {
        private int round;
        private int wager;
        private int target;

        public OverUnderGame(int round, int wager, int target) {
            this.round = round;
            this.wager = wager;
            this.target = target;
        }

        public int getRound() {
            return round;
        }

        public int getWager() {
            return wager;
        }

        public int getTarget() {
            return target;
        }
    }

    public static final long MONEY_MACHINE_UID = -1;
    public static final int PREDICTION_OVER = 0;
    public static final int PREDICTION_UNDER = 1;
    public static final int PREDICTION_SAME = 2;

    public static String formatTime(long time) {
        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) - (24 * days);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) - (1440 * days) - (60 * hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) - (86400 * days) - (3600 * hours) - (60 * minutes);
        return ((days > 0) ? (days + " day" + (days == 1 ? "" : "s") + ", ") : "")
            + ((hours > 0) ? (hours + " hour" + (hours == 1 ? "" : "s") + ", ") : "")
            + ((minutes > 0) ? (minutes + " minute" + (minutes == 1 ? "" : "s") + " and ") : "")
            + seconds + " second" + (seconds == 1 ? "" : "s") + ".";
    }
    
    // Emulates String.repeat() but for versions before Java 11
    private static String repeatString(String phrase, int times) {
    	String output = "";
    	for (int i = times; i > 0; --i) {
    		output += phrase;
    	}
    	return output;
    }

// Payout:
//  :mechanic:     25% 150
//   (:scientist:)     250 (50% if high morality)
//  :farmer:       25% 250
//   (:firefighter:)   250 (50% if high morality)
//  :chef:         20% 200
//  :detective:     5% 200
//  :artist:       25% 250

    public static String handleWork(long uid) {
        User user = getUser(uid);
        if (user == null) {
            return "Unable to fetch user. If you are new run `/claim` to start";
        }
        long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            if (user.isJailed()) {
                return "You are still in jail! Your sentence ends in " + formatTime(remainingTime);
            } else {
                return "You are still tired. Try again in " + formatTime(remainingTime);
            }
        }
        Random random = new Random();
        int roll = random.nextInt(100);
        if (roll < 25) {
            if (user.getMorality() > 5 && random.nextInt(2) == 0) {
                return ":scientist: You use your connections and work in The Lab for 2 hours and make 250 coins! Your new balance is " + logWork(uid, 250);
            } else {
                return ":mechanic: You work as a mechanic for 2 hours and make 150 coins. Your new balance is " + logWork(uid, 150);
            }
        } else if (roll < 50) {
            if (user.getMorality() > 5 && random.nextInt(2) == 0) {
                return ":firefighter: You use your connections and put out fires and save kittens for 2 hours and make 250 coins! Your new balance is " + logWork(uid, 250);
            } else {
                return ":farmer: You work hard in a field for 2 hours and make 150 coins. It ain't much, but it's honest work. Your new balance is " + logWork(uid, 150);
            }
        } else if (roll < 70) {
            return ":cook: You work as a chef for 2 hours and make 200 coins. Your new balance is " + logWork(uid, 200);
        } else if (roll < 75) {
            return ":detective: You work as a detective trying to find a missing satellite. You're unable to find it after 2 hours, but are still paid 200 coins. Your new balance is " + logWork(uid, 200);
        } else if (roll < 80) {
            return ":drum: You play your drum in the local park. People smile as they pass by, and you make a total of 250 coins in tips! Your new balance is " + logWork(uid, 250);
        } else if (roll < 85) {
            return ":potable_water: You win an internet contest and get to work a job job for 250 coins. `LET` `IT` `RIP` `!` `!` `!` Your new balance is " + logWork(uid, 250);
        } else {
            return ":artist: You make an artistic masterpiece and sell it for 250 coins! Your new balance is " + logWork(uid, 250);
        }
    }

// Payout:
//  :fish:              80% 40/50/60
//  :satellite_orbital:  5% 75
//  :octopus:            5% 75
//  :crab:               5% 100
//  :ring:               5% 250 (400 if high morality)

    public static String handleFish(long uid) {
        User user = getUser(uid);
        if (user == null) {
            return "Unable to fetch user. If you are new run `/claim` to start";
        }
        long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            if (user.isJailed()) {
                return "There's no fishing pool in jail! Your sentence ends in " + formatTime(remainingTime);
            } else {
                return "You are still tired. Try again in " + formatTime(remainingTime);
            }
        }
        Random random = new Random();
        int roll = random.nextInt(100);
        if (roll < 80) {
            int fish = (random.nextInt(3) + 4);
            return ":fish: You fish for 30 minutes and catch " + fish
                + " fish. You sell them for " + (fish * 10) + " coins. Your new balance is "
                + logFish(uid, false, fish * 10);
        } else if (roll < 85) {
            return ":satellite_orbital: You fish up a satellite??? You're not sure how it got there, but you turn it into The Lab, and they pay you 75 coins. Your new balance is "
                + logFish(uid, false, 75);
        } else if (roll < 90) {
            return ":blowfish: You fish up a pufferfish! You feed it a carrot and it thanks you with 75 coins. Your new balance is "
//          return ":octopus: You fish up an octopus, and cook it into delicious sushi worth 75 coins. Your new balance is "
                + logFish(uid, false, 75);
        } else if (roll < 95) {
            return ":crocodile: You fish up a baby crocodile! You take it back to someone who may know about it and they exchange it for 100 coins of grey items and some fishing hooks. Your new balance is "
//          return ":crab: You fish up crab. It pays you 100 coins to let it return to its dance party. Your new balance is "
                + logFish(uid, false, 100);
        } else {
            if (user.getMorality() > 5) {
                return ":ring: You fish up a ring. Since you're a good person you return it to its rightful owner and are rewarded with 400 coins! Your new balance is "
                    + logFish(uid, true, 400);
            } else {
                return ":ring: You fish up a ring, and sell it for 250 coins! Your new balance is "
                    + logFish(uid, true, 250);
            }
        }
    }

// Payout:
//  :books:             5% -10
//   (:slot_machine:)       400 (replaces books if bad)
//  :motorway:          10% 0
//  :house_adandoned:   10% 5
//  :house:             25% 200
//  :convenience_store: 25% 300
//  :bank:              25% 350

    public static String handleRob(long uid) {
        User user = getUser(uid);
        if (user == null) {
            return "Unable to fetch user. If you are new run `/claim` to start";
        }
        long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            if (user.isJailed()) {
                return "The guard gives you a funny look. You're still in jail for " + formatTime(remainingTime);
            } else {
                return "You are still tired. Try again in " + formatTime(remainingTime);
            }
        }
        Random random = new Random();
        if (random.nextInt(2) == 0) {
            robFailed(uid);
            return "You were caught! You are dragged off to jail for 2 hours.";
        }
        int roll = random.nextInt(100);
        if (roll < 5) {
            if (user.getMorality() < -10) {
                return ":slot_machine: You use your criminal knowledge and rob the slot machine of 400 coins! Your new balance is "
                    + logRob(uid, true, 400) + "\nWait! Get away from that!";
            } else if (user.getBalance() > 10) {
                return ":book: You rob The Bank! Wait, that's not The Bank, that's The Library. You pay the late fee of 10 coins for your overdue books and leave before the cops arrive. Your new balance is "
                    + logRob(uid, false, -10);
            } else {
                logRob(uid, false, 0);
                return ":book: You rob The Bank! Wait, that's not The Bank, that's The Library. You quickly leave before the cops arrive.";
            }
        } else if (roll < 15) {
            logRob(uid, false, 0);
            return ":motorway: You attempt a highway robbery, but your horse and six shooter are no match for modern automobiles.";
        } else if (roll < 25) {
            return ":house_abandoned: You rob a house, but find it empty and abandoned. Except for 5 coins and a dead rat. Though is it still a rat if it is dead? You pick up the 5 coins and leave pondering the question. Your new balance is "
                + logRob(uid, false, 5);
        } else if (roll < 50) {
            return ":house: You rob a rich looking house and get away with 200 coins. Your new balance is "
                + logRob(uid, false, 200);
        } else if (roll < 75) {
            return ":convenience_store: You rob a convenience store and grab 300 coins from the register! Your new balance is "
                + logRob(uid, true, 300);
        } else if (roll < 80) {
            return ":full_moon: With the help of some funny friends in overalls you steal THE MOON. The UN pays you 350 coins in ransom. Your new balance is "
                + logRob(uid, true, 350);
        } else {
            return ":bank: You rob The Bank and grab 350 coins worth of diamonds! Your new balance is "
                + logRob(uid, true, 350);
        }
    }

// Payout:
//  :paperclip:         10% 0
//  :satellite_orbital:  5% 0
//  :lungs:              5% 0 (150 if low morality)
//  :moneybag:          50% 50/70/90
//  :computer:          15% 100
//  :medal:             10% 125
//  :gem:                5% 250

    public static String handlePickpocket(long uid) {
        User user = getUser(uid);
        if (user == null) {
            return "Unable to fetch user. If you are new run `/claim` to start";
        }
        long remainingTime = user.getTimer().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            if (user.isJailed()) {
                return "The guard gives you a funny look. You're still in jail for " + formatTime(remainingTime);
            } else {
                return "You are still tired. Try again in " + formatTime(remainingTime);
            }
        }
        Random random = new Random();
        if (random.nextInt(2) == 0) {
            pickFailed(uid);
            return "You were caught! You are dragged off to jail for 30 minutes.";
        }
        int roll = random.nextInt(100);
        if (roll < 10) {
            logPick(uid, false, 0);
            return ":paperclip: You steal a paperclip, which you use to bundle together your wanted posters you took down.";
        } else if (roll < 15) {
            if (user.getMorality() < -10) {
                return ":lungs: You pickpocket a pair of lungs. Using your criminal connections you find a buyer who pays 150 coins. Your new balance is "
                    + logPick(uid, true, 150);
            } else {
                logPick(uid, false, 0);
                return ":lungs: You pickpocket a pair of lungs???? This was supposed to be a petty theft! You drop them on the ground and quickly run away.";
            }
        } else if (roll < 20) {
            logPick(uid, false, 0);
            return ":satellite_orbital: You pickpocket an orbital satellite???? Unsure what to do with it you ditch it in a nearby lake.";
        } else if (roll < 70) {
            int haul = 50 + (random.nextInt(3) * 20);
            return ":moneybag: You successfully pickpocket " + haul + " coins. Your new balance is "
                + logPick(uid, false, haul);
        } else if (roll < 85) {
            return ":computer: You pickpocket a laptop computer! You sell it for 100 coins, and your new balance is "
                + logPick(uid, false, 100);
        } else if (roll < 95) {
//            return ":medal: You pickpocket a medal of pure gold! You sell it for 125 coins, and your new balance is "
            return ":credit_card: You pickpocket mom's credit card! You note down the 3 wacky numbers on the back and purchase 125 coins. Your new balance is "
                + logPick(uid, false, 125);
        } else {
            return ":gem: You grab a large diamond worth 250 coins!! Your new balance is "
                + logPick(uid, true, 250);
        }
    }

    public static String handleClaim(long uid, String name) {
        User user = getUser(uid);
        String response = "";
        if (user == null) {
            boolean error = addUser(uid, name);
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

// Guess Payout:
//  Correct:        1/10  6:1
//  Close:          1/5   2:1
//  Dealer mistake: 1/200 2.5:1

    // public static String handleGuess(long uid, int guess, int amount) {
    //  long balance = checkBalance(uid);
    //  if (balance < 0) {
    //      return "Unable to guess. Balance check failed or was negative (" + balance +")";
    //  } else if (balance < amount) {
    //      return "Your balance of " + balance + " is not enough to cover that!";
    //  }
    //  Random random = new Random();
    //  int correct = random.nextInt(10) + 1;
    //  if (guess == correct) {
    //      if (guess == 1 || guess == 10) {
    //          guessWin(uid, amount, 6 * amount);
    //          return "Correct!! Big win of " + (6 * amount) + "! New balance is "
    //              + addWinnings(uid, 6 * amount);
    //      }
    //      guessWin(uid, amount, 5 * amount);
    //      return "Correct! You win " + (5 * amount) + "! New balance is "
    //          + addWinnings(uid, 5 * amount);
    //  } else if (guess + 1 == correct || guess - 1 == correct) {
    //      guessClose(uid, amount, 1 * amount);
    //      return "Very close. The value was " + correct + ". You get " + (amount)
    //          + " as a consolation prize. New balance is " + addWinnings(uid, amount);
    //  } else {
    //      if (random.nextInt(140) == 0) {
    //          guessMistake(uid, amount, ((int)2.5 * amount));
    //          return "The correct value was " + (random.nextInt(5) + 11)
    //              + ". Wait, that isn't right. Here, take " + ((int)2.5 * amount)
    //              + " to pretend that never happened. New balance is "
    //                  + addWinnings(uid, ((int)2.5 * amount));
    //      }
    //      guessLoss(uid, amount);
    //      return "The correct value was " + correct + ". Your new balance is "
    //          + takeLosses(uid, amount);
    //  }
    // }

// Big Guess Payout:
//  Correct:        1/10  10:1

    public static String handleGuess(long uid, long guess, long amount) {
        long balance = checkBalance(uid);
        if (balance < 0) {
            return "Unable to guess. Balance check failed or was negative (" + balance +")"
                + "\nIf you're new, use `/claim` to get set up with an initial balance.";
        } else if (balance < amount) {
            return "Your balance of " + balance + " is not enough to cover that!";
        }
        Random random = new Random();
        int correct = random.nextInt(10) + 1;
        if (guess == correct) {
            guessWin(uid, amount, 9 * amount);
            return "Correct! You win " + (10 * amount) + "! New balance is " + addWinnings(uid, 9 * amount);
        } else {
            guessLoss(uid, amount);
            return "The correct value was " + correct + ". Your new balance is " + takeLosses(uid, amount);
        }
    }

// Huge Guess Payout:
//  Correct:    1/100  100:1

    public static String handleHugeGuess(long uid, long guess, long amount) {
        long balance = checkBalance(uid);
        if (balance < 0) {
            return "Unable to guess. Balance check failed or was negative (" + balance +")"
                + "\nIf you're new, use `/claim` to get set up with an initial balance.";
        } else if (balance < amount) {
            return "Your balance of " + balance + " is not enough to cover that!";
        }
        Random random = new Random();
        int correct = random.nextInt(100) + 1;
        if (guess == correct) {
            hugeGuessWin(uid, amount, 99 * amount);
            return "Correct! You win " + (100 * amount) + "! New balance is " + addWinnings(uid, 99 * amount);
        } else {
            hugeGuessLoss(uid, amount);
            return "The correct value was " + correct + ". Your new balance is " + takeLosses(uid, amount);
        }
    }

    public static String handleFeed(long uid, Long amount) {
        User user = getUser(uid);
        if (user == null) {
            return "Unable to fetch user. If you are new run `/claim` to start";
        }
        if (user.getBalance() < amount) {
            return "Your balance of " + user.getBalance() + " is not enough to cover that!";
        }
        long remainingTime = user.getTimer2().getTime() - System.currentTimeMillis();
        if (remainingTime > 0) {
            return "You have recently fed the money machine. Try again in " + formatTime(remainingTime);
        }
        User moneyMachine = getUser(MONEY_MACHINE_UID);
        if (moneyMachine == null) {
            return "A database error occurred. The money machine is nowhere to be found.";
        }
        Random random = new Random();
        long pot = moneyMachine.getBalance() + amount;
        double winChance = 0.25;
        if (pot < 20000) {
            winChance = 0.05 + (0.2 * (pot / 20000));
        }
        if (random.nextDouble() < winChance && (amount >= 100 || random.nextInt(100) < amount)) { // Win
        	long winnings = (long)(pot * 0.75);
        	long newPot = pot - winnings;
            return "The money machine is satisfied! :dollar: You win "
                + winnings + "! Your new balance is " + moneyMachineWin(uid, winnings, newPot)
                + ". The pot is now " + newPot + ".";
        } else { // Lose
            return "Even with a current pot of " + pot + " the money machine is still hungry. Your new balance is "
                + moneyMachineLoss(uid, amount);
        }
    }

    public static String handlePot() {
    	User moneyMachine = getUser(MONEY_MACHINE_UID);
        if (moneyMachine == null) {
            return "A database error occurred. The money machine is nowhere to be found.";
        }
        return "The current pot is " + moneyMachine.getBalance();
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

    public static List<String> handleSlots(long uid, long amount) {
        List<String> responseSteps = new ArrayList<String>();
        long balance = checkBalance(uid);
        if (balance < 0) {
            responseSteps.add("Unable to guess. Balance check failed or was negative (" + balance +")"
            + "\nIf you're new, use `/claim` to get set up with an initial balance.");
            return responseSteps;
        } else if (balance < amount) {
            responseSteps.add("Your balance of " + balance + " is not enough to cover that!");
            return responseSteps;
        }
        Random random = new Random();
        int cherries = 0, oranges = 0, lemons = 0, blueberries = 0, grapes = 0, diamonds = 0;
        String output = "Bid " + amount + " on slots\n", placeholder = ":blue_square:";
        int winnings = 0;
        responseSteps.add(new String(output + repeatString(placeholder, 5)));
        for (int i = 0; i < 5; ++i) {
            switch (random.nextInt(5)) {
                case 0:
                    output += ":cherries:";
                    cherries++;
                    break;
                case 1:
                    output += ":tangerine:";
                    oranges++;
                    break;
                case 2:
                    output += ":lemon:";
                    lemons++;
                    break;
                case 3:
                    output += ":blueberries:";
                    blueberries++;
                    break;
                case 4:
                    if (random.nextInt(20) == 10) {
                        output += ":gem:";
                        diamonds++;
                    } else {
                        output += ":grapes:";
                        grapes++;
                    }
                    break;
            }
            responseSteps.add(new String(output + repeatString(placeholder, 4 - i)));
        }
        output += "\n";
        int win_condition = 0;
        if (cherries == 5 || oranges == 5 || lemons == 5 || blueberries == 5 || grapes == 5) {
            output += ":moneybag::moneybag: 5 OF A KIND!!! :moneybag::moneybag:";
            winnings += 30 * amount;
            win_condition = 5;
        } else if (cherries == 4 || oranges == 4 || lemons == 4 || blueberries == 4 || grapes == 4) {
            output += ":moneybag: 4 of a kind!! :moneybag: ";
            winnings += 10 * amount;
            win_condition = 4;
        } else if (cherries == 3 || oranges == 3 || lemons == 3 || blueberries == 3 || grapes == 3) {
            output += "3 of a kind. ";
            winnings += (int)(1.5 * amount);
            win_condition = 3;
        } else if (cherries == 1 && oranges == 1 && lemons == 1 && blueberries == 1 && grapes == 1) {
            output += "Fruit salad! ";
            winnings += 2 * amount;
            win_condition = 1;
        }
        if (diamonds > 0) {
            output += ":gem: " + diamonds + " diamond" + (diamonds == 1 ? "" : "s") + "! :gem: ";
            if (diamonds > 3) { output += "Jackpot!!! "; }
            winnings += amount * (int)Math.pow(10, diamonds - 1);
        }
        if (amount > winnings) {
            balance = takeLosses(uid, amount - winnings);
        } else {
            balance = addWinnings(uid, winnings - amount);
        }
        if (winnings > 0) {
            output += "Winnings: " + (winnings) + " ";
        }
        output += "New balance: " + balance;
        logSlots(uid, amount, winnings, diamonds, win_condition);
        responseSteps.add(new String(output));
        return responseSteps;
    }

// Minislots Payout
//  3 of a kind: 1/25      5:1
//  2 of a kind: 12/25     1.6:1
//  1 diamond:   3/100     0.4:1
//  2 diamonds:  3/10000   10:1
//  3 diamonds:  1/1000000 100:1

    public static List<String> handleMinislots(long uid, long amount) {
        List<String> responseSteps = new ArrayList<String>();
        long balance = checkBalance(uid);
        if (balance < 0) {
            responseSteps.add("Unable to guess. Balance check failed or was negative (" + balance +")"
            + "\nIf you're new, use `/claim` to get set up with an initial balance.");
            return responseSteps;
        } else if (balance < amount) {
            responseSteps.add("Your balance of " + balance + " is not enough to cover that!");
            return responseSteps;
        }
        Random random = new Random();
        int cherries = 0, oranges = 0, lemons = 0, blueberries = 0, grapes = 0, diamonds = 0;
        String output = "Bid " + amount + " on mini slots\n", placeholder = ":blue_square:";
        int winnings = 0;
        responseSteps.add(new String(output + repeatString(placeholder, 3)));
        for (int i = 0; i < 3; i++) {
            switch (random.nextInt(5)) {
            case 0:
                output += ":cherries:";
                cherries++;
                break;
            case 1:
                output += ":tangerine:";
                oranges++;
                break;
            case 2:
                output += ":lemon:";
                lemons++;
                break;
            case 3:
                output += ":blueberries:";
                blueberries++;
                break;
            case 4:
                if (random.nextInt(20) == 10) {
                    output += ":gem:";
                    diamonds++;
                } else {
                    output += ":grapes:";
                    grapes++;
                }
                break;
            }
            responseSteps.add(new String(output + repeatString(placeholder, 2 - i)));
        }
        output += "\n";
        if (cherries == 3 || oranges == 3 || lemons == 3 || blueberries == 3 || grapes == 3) {
            output += ":moneybag: 3 of a kind! :moneybag: ";
            winnings += 5 * amount;
        } else if (cherries == 2 || oranges == 2 || lemons == 2 || blueberries == 2 || grapes == 2) {
            output += "2 of a kind. ";
            winnings += (int)(1.6 * amount);
        }
        if (diamonds == 1) {
            output += "1 bonus diamond :gem:";
            winnings += (int)(0.4 * amount);
        } else if (diamonds > 1) {
            output += ":gem: " + diamonds + " diamond" + (diamonds == 1 ? "" : "s") + "! :gem: Jackpot!!! ";
            winnings += amount * (int)Math.pow(10, diamonds - 1);
        }
        if (amount > winnings) {
            balance = takeLosses(uid, amount - winnings);
        } else {
            balance = addWinnings(uid, winnings - amount);
        }
        if (winnings > 0) {
            output += "Winnings: " + (winnings) + " ";
        }
        output += "New balance: " + balance;
        logMinislots(uid, amount, winnings, diamonds);
        responseSteps.add(new String(output));
        return responseSteps;
    }

// OverUnder Payout:
//  2 correct then 1 wrong: ~2/11 1:1
//  3 correct:              ~3/11 3:1

    public static String handleOverUnderInitial(long uid, long amount) {
        OverUnderGame game = getOverUnderRound(uid);
        if (game != null && game.getRound() != -1) {
            return "You already have an active game: Round " + game.getRound() + " with the current value "
                + game.getTarget() + ".\nUse `over`, `under`, or `same` to predict which the next value will be";
        }
        long balance = checkBalance(uid);
        if (balance < 0) {
            return "Unable to start game. Balance check failed or was negative (" + balance +")";
        } else if (balance < amount) {
            return "Your current balance of " + balance + " is not enough to cover that";
        }
        Random random = new Random();
        int target = random.nextInt(10) + 1;
        logInitialOverUnder(uid, amount, target);
        return "Bid " + amount + " on overunder.\nYour initial value is " + target
            + ". Predict if the next value (1-10) will be `over`, `under`, or the `same`";
    }

    public static String handleOverUnderFollowup(long uid, int prediction) {
        OverUnderGame game = getOverUnderRound(uid);
        if (game == null || game.getRound() == -1) {
            return "No active game found. Use `/overunder new` to start a new game";
        }
        Random random = new Random();
        int target = random.nextInt(10) + 1;
        if ((prediction == PREDICTION_OVER && target > game.getTarget())
                || (prediction == PREDICTION_UNDER && target < game.getTarget())
                || (prediction == PREDICTION_SAME && target == game.getTarget())) { // Correct
            if (game.getRound() == 3) {
                return "Correct! The value was " + target + ". You win " + (3 * game.getWager())
                    + "!\nYour new balance is " + logOverUnderWin(uid, 3 * game.getWager(), true, game.getWager());
            } else {
                logOverUnderProgress(uid, game.getRound() + 1, target);
                return "Correct! Your new value for the " + ((game.getRound() + 1) == 2 ? "second" : "third")
                    + " round is " + target; 
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
                return response + " With 2 correct your " + game.getWager()
                    + " coins are returned. Your new balance is " + logOverUnderWin(uid, game.getWager(), false, game.getWager());
            } else {
                logOverUnderLoss(uid, game.getWager());
                return response + " Your current balance is " + checkBalance(uid);
            }
        }
    }

    public static String handleBalance(long uid) {
        long balance = checkBalance(uid);
        if (balance < 0) {
            return "There was an issue checking your balance, value returned was " + balance
            		+ "\nIf you're new, use `/claim` to get set up with an initial balance.";
        } else {
            return "Your current balance is " + balance + " coin" + (balance == 1 ? "" : "s") + ".";
        }
    }

    public static String handleGive(long donorUid, long recipientUid, long amount) {
        if (amount <= 0) {
            return "Can't give someone a negative number of coins. Try asking them nicely if you want money.";
        }
        long donorBalance = checkBalance(donorUid);
        if (donorBalance < 0) {
            return "Unable to give money. Balance check failed or was negative (" + donorBalance +")";
        } else if (donorBalance < amount) {
            return "Your balance of " + donorBalance + " is not enough to cover that!";
        }
        if (donorUid == recipientUid) {
            return "You give yourself " + amount + ". Your balance is unchanged for some reason.";
        }
        long recipientBalance = checkBalance(recipientUid);
        if (recipientBalance == -1) {
            return "Unable to give money. Has that user run `/claim`?";
        }
        donorBalance = takeMoneyDirect(donorUid, amount);
        addMoneyDirect(recipientUid, amount);
        if (donorBalance < 0) {
            return "Unable to process transaction";
        } else {
            return "Gave " + amount + ", your new balance is " + donorBalance;
        }
    }

    public static String handleLeaderboard(long entries) {
    	if (entries > 10) {
    		entries = 10;
    	}
        return parseLeaderboard(entries);
    }



    //////////////////////////////////////////////////////////

    public static Connection getConnection() throws URISyntaxException, SQLException {
        return DriverManager.getConnection(System.getenv("JDBC_DATABASE_URL"),
            System.getenv("JDBC_USERNAME"), System.getenv("JDBC_PASSWORD"));
    }

    //CREATE TABLE IF NOT EXISTS money_user (
    //  uid bigint PRIMARY KEY,
    //  name varchar(40) DEFAULT '',
    //  balance bigint DEFAULT 0,
    //  in_jail boolean DEFAULT false,
    //  last_claim timestamp DEFAULT '2021-01-01 00:00:00',
    //  timestamp2 timestamp DEFAULT '2021-01-01 00:00:00'
    //);
    
    // CREATE TABLE IF NOT EXISTS job_user (
    //  uid bigint PRIMARY KEY,
    //  work_count integer DEFAULT 0,
    //  work_profit bigint DEFAULT 0,
    //  fish_count integer DEFAULT 0,
    //  fish_jackpots integer DEFAULT 0,
    //  fish_profit bigint DEFAULT 0,
    //  pick_count integer DEFAULT 0,
    //  pick_fails integer DEFAULT 0,
    //  pick_jackpots integer DEFAULT 0,
    //  pick_profit bigint DEFAULT 0,
    //  rob_count integer DEFAULT 0,
    //  rob_fails integer DEFAULT 0,
    //  rob_jackpots integer DEFAULT 0,
    //  rob_profit bigint DEFAULT 0,
    //  jail_time bigint DEFAULT 0,
    //  CONSTRAINT jobs_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );
    
    // CREATE TABLE IF NOT EXISTS slots_user (
    //  uid bigint PRIMARY KEY,
    //  pulls integer DEFAULT 0,
    //  diamonds integer DEFAULT 0,
    //  spent bigint DEFAULT 0,
    //  winnings bigint DEFAULT 0,
    //  threes integer DEFAULT 0,
    //  fours integer DEFAULT 0,
    //  fives integer DEFAULT 0,
    //  fruitsalads integer DEFAULT 0,
    //  CONSTRAINT slots_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );
    
    // CREATE TABLE IF NOT EXISTS guess_user (
    //  uid bigint PRIMARY KEY,
    //  guesses integer DEFAULT 0,
    //  correct integer DEFAULT 0,
    //  close integer DEFAULT 0,
    //  mistake integer DEFAULT 0,
    //  spent bigint DEFAULT 0,
    //  winnings bigint DEFAULT 0,
    //  CONSTRAINT guess_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );
    
    // CREATE TABLE IF NOT EXISTS hugeguess_user (
    //  uid bigint PRIMARY KEY,
    //  guesses integer DEFAULT 0,
    //  correct integer DEFAULT 0,
    //  spent bigint DEFAULT 0,
    //  winnings bigint DEFAULT 0,
    //  CONSTRAINT hugeguess_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );
    
    // CREATE TABLE IF NOT EXISTS minislots_user (
    //  uid bigint PRIMARY KEY,
    //  pulls integer DEFAULT 0,
    //  diamonds integer DEFAULT 0,
    //  spent integer DEFAULT 0,
    //  winnings integer DEFAULT 0,
    //  CONSTRAINT minislots_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );
    
    // CREATE TABLE IF NOT EXISTS moneymachine_user (
    //  uid bigint PRIMARY KEY,
    //  feeds integer DEFAULT 0,
    //  wins integer DEFAULT 0,
    //  spent integer DEFAULT 0,
    //  winnings integer DEFAULT 0,
    //  CONSTRAINT moneymachine_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );
    
    // CREATE TABLE IF NOT EXISTS overunder_user (
    //  uid bigint PRIMARY KEY,
    //  played integer DEFAULT 0,
    //  consolations integer DEFAULT 0,
    //  wins integer DEFAULT 0,
    //  spent bigint DEFAULT 0,
    //  winnings bigint DEFAULT 0,
    //  bet integer DEFAULT -1,
    //  round integer DEFAULT -1,
    //  target integer DEFAULT -1,
    //  CONSTRAINT overunder_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    public static long checkBalance(long uid) {
        return executeBalanceQuery("SELECT balance FROM money_user WHERE uid = " + uid + ";");
    }

    public static long takeMoneyDirect(long uid, long amount) {
        return executeBalanceQuery("UPDATE money_user SET balance = balance - "
            + amount + " WHERE uid = " + uid + " RETURNING balance;");
    }

    public static long takeLosses(long uid, long amount) {
        return executeBalanceQuery("UPDATE money_user SET balance = balance - "
            + amount + " WHERE uid = " + uid + " RETURNING balance;");
    }

    public static long addMoneyDirect(long uid, long amount) {
        return executeBalanceQuery("UPDATE money_user SET balance = balance + "
            + amount + " WHERE uid = " + uid + " RETURNING balance;");
    }

    public static long addWinnings(long uid, long amount) {
        return addWinnings(uid, amount, amount);
    }

    public static long addWinnings(long uid, long amount, long profit) {
        return executeBalanceQuery("UPDATE money_user SET balance = balance + "
            + amount + " WHERE uid = " + uid + " RETURNING balance;");
    }

    private static long addWorkMoney(long uid, int amount, String delay) {
        return executeBalanceQuery("UPDATE money_user SET (in_jail, balance, last_claim) = (false, balance + "
            + amount + ", NOW() + INTERVAL '" + delay + "') WHERE uid = " + uid + " RETURNING balance;");
    }

    private static void setJailTime(long uid, String interval) {
        executeUpdate("UPDATE money_user SET (in_jail, last_claim) = (true, NOW() + INTERVAL '"
            + interval + "') WHERE uid = " + uid + ";");
    }

    private static void setTimer2Time(long uid, String interval) {
        executeUpdate("UPDATE money_user SET timestamp2 = NOW() + INTERVAL '"
            + interval + "' WHERE uid = " + uid + ";");
    }

    private static boolean addUser(long uid, String name) {
        boolean error = false;
        String query = "INSERT INTO money_user (uid, name, balance) VALUES(" + uid + ", '" + name +"', 1000) ON CONFLICT (uid) DO NOTHING;";
        String job = "INSERT INTO job_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String slots = "INSERT INTO slots_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String guess = "INSERT INTO guess_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String minislots = "INSERT INTO minislots_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String hugeguess = "INSERT INTO hugeguess_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String monemachine = "INSERT INTO moneymachine_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String overunder = "INSERT INTO overunder_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        String blackjac = "INSERT INTO blackjack_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        //String wagers = "INSERT INTO wager_user (uid) VALUES (" + uid + ") ON CONFLICT (uid) DO NOTHING;";
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            error = statement.executeUpdate(query) < 1;
            if (!error) {
                statement.executeUpdate(job);
                statement.executeUpdate(slots);
                statement.executeUpdate(guess);
                statement.executeUpdate(minislots);
                statement.executeUpdate(hugeguess);
                statement.executeUpdate(monemachine);
                statement.executeUpdate(overunder);
                statement.executeUpdate(blackjac);
                //statement.executeUpdate(wagers);
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return error;
    }

    private static String parseLeaderboard(long entries) {
        String query = "SELECT name, balance FROM money_user ORDER BY balance DESC LIMIT " + entries + ";";
        Connection connection = null;
        Statement statement = null;
        String leaderboard = "";
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            int place = 1;
            while (results.next()) {
                leaderboard += "#" + place++ + " ";
                String name = results.getString(1);
                if (name.contains("#")) {
                    name = name.substring(0, name.indexOf('#'));
                }
                leaderboard += name + " " + results.getLong(2) + "\n";
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return leaderboard;
    }

    private static void hugeGuessWin(long uid, long spent, long winnings) {
        executeUpdate("UPDATE hugeguess_user SET (guesses, correct, spent, winnings) = (guesses + 1, correct + 1, spent + "
            + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
    }

    private static void hugeGuessLoss(long uid, long spent) {
        executeUpdate("UPDATE hugeguess_user SET (guesses, spent) = (guesses + 1, spent + "
                + spent + ") WHERE uid = " + uid + ";");
    }

    private static void guessWin(long uid, long spent, long winnings) {
        executeUpdate("UPDATE guess_user SET (guesses, correct, spent, winnings) = (guesses + 1, correct + 1, spent + "
            + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
    }

    // private static void guessClose(long uid, int spent, int winnings) {
    //     executeUpdate("UPDATE guess_user SET (guesses, close, spent, winnings) = (guesses + 1, close + 1, spent + "
    //         + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
    // }

    private static void guessLoss(long uid, long spent) {
        executeUpdate("UPDATE guess_user SET (guesses, spent) = (guesses + 1, spent + "
            + spent + ") WHERE uid = " + uid + ";");
    }

    // private static void guessMistake(long uid, int spent, int winnings) {
    //     executeUpdate("UPDATE guess_user SET (guesses, mistake, spent, winnings) = (guesses + 1, mistake + 1, spent + "
    //         + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
    // }

    private static void logSlots(long uid, long spent, long winnings, int diamonds, int winCondition) {
        executeUpdate("UPDATE slots_user SET (pulls, diamonds, spent, winnings, threes, fours, fives, fruitsalads) = (pulls + 1, diamonds + "
            + diamonds + ", spent + " + spent + ", winnings + " + winnings + ", threes + "
            + (winCondition == 3 ? 1 : 0) + ", fours + " + (winCondition == 4 ? 1 : 0) + ", fives + "
            + (winCondition == 5 ? 1 : 0) + ", fruitsalads + " + (winCondition == 1 ? 1 : 0) + ") WHERE uid = " + uid + ";");
    }

    private static void logMinislots(long uid, long spent, long winnings, int diamonds) {
        executeUpdate("UPDATE minislots_user SET (pulls, diamonds, spent, winnings) = (pulls + 1, diamonds + "
            + diamonds + ", spent + " + spent + ", winnings + " + winnings + ") WHERE uid = " + uid + ";");
    }

    private static User getUser(long uid) {
        String query = "SELECT work_count, fish_count, pick_count, rob_count, balance, in_jail, last_claim, timestamp2 FROM money_user NATURAL JOIN job_user WHERE uid = " + uid + ";";
        Connection connection = null;
        Statement statement = null;
        User user = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
                int work = results.getInt(1);
                int fish = results.getInt(2);
                int pick = results.getInt(3);
                int rob = results.getInt(4);
                long balance = results.getLong(5);
                boolean isJail = results.getBoolean(6);
                Timestamp time = results.getTimestamp(7);
                Timestamp time2 = results.getTimestamp(8);
                user = new Casino.User(work, fish, pick, rob, balance, isJail, time, time2);
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return user;
    }

    private static long logWork(long uid, int income) {
        long balance = addWorkMoney(uid, income, "2 hours");
        executeUpdate("UPDATE job_user SET (work_count, work_profit) = (work_count + 1, "
            + "work_profit + " + income + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static long logFish(long uid, boolean rare, int income) {
        long balance = addWorkMoney(uid, income, "30 minutes");
        executeUpdate("UPDATE job_user SET (fish_count, fish_jackpots, fish_profit) = (fish_count + 1, fish_jackpots + " 
            + (rare ? 1 : 0) + ", fish_profit + " + income + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static void pickFailed(long uid) {
        setJailTime(uid, "30 minutes");
        executeUpdate("UPDATE job_user SET (pick_count, pick_fails, jail_time) = (pick_count + 1, pick_fails + 1, jail_time + 30) WHERE uid = "
            + uid + ";");
    }

    private static long logPick(long uid, boolean rare, int income) {
        long balance = addMoneyDirect(uid, income);
        executeUpdate("UPDATE job_user SET (pick_count, pick_jackpots, pick_profit) = (pick_count + 1, pick_jackpots + "
            + (rare ? 1 : 0) + ", pick_profit + " + income + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static void robFailed(long uid) {
        setJailTime(uid, "2 hours");
        executeUpdate("UPDATE job_user SET (rob_count, rob_fails, jail_time) = (rob_count + 1, rob_fails + 1, jail_time + 120) WHERE uid = "
                + uid + ";");
    }

    private static long logRob(long uid, boolean rare, int income) {
        long balance = addMoneyDirect(uid, income);
        executeUpdate("UPDATE job_user SET (rob_count, rob_jackpots, rob_profit) = (rob_count + 1, rob_jackpots + "
            + (rare ? 1 : 0) + ", rob_profit + " + income + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static long moneyMachineWin(long uid, long winnings, long newPot) {
        long balance = addMoneyDirect(uid, winnings);
        executeUpdate("UPDATE money_user SET balance = " + newPot + " WHERE uid = " + MONEY_MACHINE_UID + ";");
        setTimer2Time(uid, "1 minute");
        executeUpdate("UPDATE moneymachine_user SET (feeds, wins, winnings) = (feeds + 1, wins + 1, winnings + "
            + winnings + ") WHERE uid = " + uid + ";");
        return balance;
    }

    private static long moneyMachineLoss(long uid, long bet) {
        long balance = takeMoneyDirect(uid, bet);
        addMoneyDirect(MONEY_MACHINE_UID, bet);
        setTimer2Time(uid, "1 minute");
        executeUpdate("UPDATE moneymachine_user SET (feeds, spent) = (feeds + 1, spent + "
            + bet + ") WHERE uid = " + uid + ";");
        return balance;
    }

    public static void logInitialOverUnder(long uid, long bet, int target) {
        takeMoneyDirect(uid, bet);
        executeUpdate("UPDATE overunder_user SET (round, played, spent, bet, target) = (1, played + 1, spent + "
            + bet + ", " + bet + ", " + target + ") WHERE uid = " + uid + ";");
    }

    public static void logOverUnderProgress(long uid, int round, int target) {
        executeUpdate("UPDATE overunder_user SET (round, target) = ("
            + round + ", " + target + ") WHERE uid = " + uid + ";");
    }

    public static void logOverUnderLoss(long uid, long bet) {
        executeUpdate("UPDATE overunder_user SET (bet, round, target) = (-1, -1, -1) WHERE uid = "
            + uid + ";");
    }

    public static long logOverUnderWin(long uid, long winnings, boolean thirdRound, long wager) {
        long balance = addWinnings(uid, winnings, winnings - wager);
        executeUpdate("UPDATE overunder_user SET (bet, round, target, consolations, wins, winnings) = (-1, -1, -1, consolations + "
            + (thirdRound ? 0 : 1) + ", wins + " + (thirdRound ? 1 : 0) + ", winnings + "
            + (winnings - wager) + ") WHERE uid = " + uid + ";");
        return balance;
    }

    public static OverUnderGame getOverUnderRound(long uid) {
        String query = "SELECT round, bet, target FROM overunder_user WHERE uid = " + uid + ";";
        Connection connection = null;
        Statement statement = null;
        OverUnderGame game = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
                int round = results.getInt(1);
                int wager = results.getInt(2);
                int target = results.getInt(3);
                game = new Casino.OverUnderGame(round, wager, target);
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return game;
    }

    public static void executeUpdate(String query) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            statement.executeUpdate(query);
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static long executeBalanceQuery(String query) {
        Connection connection = null;
        Statement statement = null;
        long balance = 0;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
                balance = results.getLong(1);
            } else {
                balance = -1;
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return balance;
    }

}
