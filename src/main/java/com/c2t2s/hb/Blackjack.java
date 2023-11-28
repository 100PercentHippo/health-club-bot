package com.c2t2s.hb;

import java.util.List;
import java.util.ArrayList;

class Blackjack {

    // Hide public constructor
    private Blackjack() {}

    private static String[] cardLetters = {"", "[A]", "[2]", "[3]", "[4]", "[5]", "[6]", "[7]", "[8]", "[9]", "[10]", "[J]", "[Q]", "[K]"};
    private static int[] cardValues = {0, 11, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10};

    private static class BlackJackGame {
        private String hand;
        private int sum;
        private boolean containsAce;
        private int dealerHand;
        private long wager;

        private BlackJackGame(String hand, int sum, boolean ace, int dealer, long wager) {
            this.hand = hand;
            this.sum = sum;
            this.containsAce = ace;
            this.dealerHand = dealer;
            this.wager = wager;
        }

        private String getHand() {
            return hand;
        }

        private int getSum() {
            return sum;
        }

        private boolean hasAce() {
            return containsAce;
        }

        private int getDealerHand() {
            return dealerHand;
        }

        private long getWager() {
            return wager;
        }
    }

    private static String displayGame(String hand, int dealer, String dealerCardTwo) {
        return "Your hand:    " + hand + "\nDealer's hand: " + cardLetters[dealer] + dealerCardTwo;
    }

    static HBMain.SingleResponse handleBlackjack(long uid, long wager) {
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null) {
            return new HBMain.SingleResponse("Unable to fetch game from the database. Did you `+claim`?");
        } else if (game.getWager() != -1) {
            return new HBMain.SingleResponse("You have a currently active game:\n"
                + displayGame(game.getHand(), game.getDealerHand(), "[?]")
                + "\nUse `+hit` or `+stand` to play it out", ButtonRows.BLACKJACK_BUTTONS);
        }
        long balance = Casino.checkBalance(uid);
        if (balance < 0) {
            return new HBMain.SingleResponse("Unable to start game. Balance check failed or was negative (" + balance +")");
        } else if (balance < wager) {
            return new HBMain.SingleResponse("Your current balance of " + balance + " is not enough to cover that");
        }
        int dealerCard = HBMain.RNG_SOURCE.nextInt(13) + 1;
        StringBuilder hand = new StringBuilder();
        int value = 0;
        boolean hasAce = false;
        for (int i = 0; i < 2; i++) {
            int card = HBMain.RNG_SOURCE.nextInt(13) + 1;
            hand.append(cardLetters[card]);
            if (card == 1) {
                if (hasAce) {
                    value += 1;
                } else {
                    hasAce = true;
                    value += 11;
                }
            } else {
                value += cardValues[card];
            }
        }
        String completeHand = hand.toString();
        Casino.takeMoney(uid, wager);
        newBlackjackGame(uid, completeHand, value, hasAce, dealerCard, wager);
        return new HBMain.SingleResponse("Bid " + wager + " on Blackjack\n"
            + displayGame(completeHand, dealerCard, "[?]"), ButtonRows.BLACKJACK_BUTTONS);
    }

    static HBMain.MultistepResponse handleStand(long uid) {
        List<String> response = new ArrayList<>();
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null || game.getWager() == -1) {
            return new HBMain.MultistepResponse("No active game found. Use `/blackjack new` to start a new game");
        }
        String playerHand = "Your hand:    " + game.getHand();
        StringBuilder dealerHand = new StringBuilder("\nDealer's hand: " + cardLetters[game.getDealerHand()]);
        response.add(playerHand + dealerHand.toString() + "[?]" + Casino.PLACEHOLDER_NEWLINE_STRING);
        int dealerTotal = cardValues[game.getDealerHand()];
        boolean dealerAce = (game.getDealerHand() == 1);
        while ((!dealerAce && dealerTotal < 17) || (dealerAce && dealerTotal < 17)
                || (dealerAce && dealerTotal > 21 && dealerTotal - 10 < 17)) {
            int card = HBMain.RNG_SOURCE.nextInt(13) + 1;
            dealerHand.append(cardLetters[card]);
            if (card == 1) {
                if (dealerAce) {
                    dealerTotal++;
                } else {
                    dealerTotal += 11;
                    dealerAce = true;
                }
            } else {
                dealerTotal += cardValues[card];
            }
            response.add(playerHand + dealerHand.toString() + Casino.PLACEHOLDER_NEWLINE_STRING);
        }
        if (dealerAce && dealerTotal > 21) {
            dealerTotal -= 10;
        }
        int playerTotal = game.getSum();
        if (game.hasAce() && playerTotal > 21) {
            playerTotal -= 10;
        }
        String resolution = "";
        if (dealerTotal > 21) {
            resolution = "\nDealer bust! You win " + (2 * game.getWager())
                    + "! Your new balance is " + blackjackWin(uid, game.getWager(), true);
        } else if (dealerTotal > playerTotal) {
            blackjackLoss(uid);
            resolution = "\nDealer wins. Your new balance is " + Casino.checkBalance(uid);
        } else if (dealerTotal < playerTotal) {
            resolution = "\nYou win " + (2 * game.getWager())
                    + "! Your new balance is " + blackjackWin(uid, game.getWager(), false);
        } else { // Tie
            resolution = "\nTie. You get " + game.getWager() + " back. Your new balance is "
                    + blackjackTie(uid, game.getWager());
        }
        response.add(playerHand + dealerHand + resolution);
        return new HBMain.MultistepResponse(response);
    }

    static HBMain.SingleResponse handleHit(long uid) {
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null || game.getWager() == -1) {
            return new HBMain.SingleResponse("No active game found. Type `/blackjack new` to start a new game");
        }
        boolean hasAce = game.hasAce();
        int value = game.getSum();
        int card = HBMain.RNG_SOURCE.nextInt(13) + 1;
        String hand = game.getHand() + cardLetters[card];
        if (card == 1) {
            if (hasAce) {
                value++;
            } else {
                value += 11;
                hasAce = true;
            }
        } else {
            value += cardValues[card];
        }
        if ((hasAce && value > 31) || (!hasAce && value > 21)) {
            return new HBMain.SingleResponse(displayGame(hand, game.getDealerHand(), cardLetters[HBMain.RNG_SOURCE.nextInt(13) + 1])
                + "\nBust! Your new balance is " + blackjackBust(uid));
        } else {
            updateBlackjackGame(uid, hand, value, hasAce);
            return new HBMain.SingleResponse(displayGame(hand, game.getDealerHand(), "[?]"),
                ButtonRows.BLACKJACK_BUTTONS);
        }
    }

    /////////////////////////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS blackjack_user (
    //  uid bigint PRIMARY KEY,
    //  hands integer NOT NULL DEFAULT 0,
    //  busts integer NOT NULL DEFAULT 0,
    //  dealer_busts NOT NULL integer DEFAULT 0,
    //  ties integer NOT NULL DEFAULT 0,
    //  wins integer NOT NULL DEFAULT 0,
    //  spent bigint NOT NULL DEFAULT 0,
    //  winnings bigint NOT NULL DEFAULT 0,
    //  hand varchar(63) NOT NULL DEFAULT '',
    //  sum integer NOT NULL DEFAULT -1,
    //  ace boolean NOT NULL DEFAULT false,
    //  dealer_hand integer NOT NULL DEFAULT -1,
    //  wager bigint NOT NULL DEFAULT -1,
    //  CONSTRAINT blackjack_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    private static void newBlackjackGame(long uid, String hand, int sum, boolean hasAce, int dealerHand, long wager) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (hands, spent, hand, sum, ace, dealer_hand, wager) = (hands + 1, spent + "
            + wager + ", '" + hand + "', " + sum + ", " + hasAce + ", " + dealerHand + ", "
            + wager + ") WHERE uid = " + uid + ";");
    }

    private static void updateBlackjackGame(long uid, String hand, int sum, boolean containsAce) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (hand, sum, ace) = ('" + hand
            + "', " + sum + ", " + containsAce + ") WHERE uid = " + uid + ";");
    }

    private static long blackjackBust(long uid) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (busts, hand, sum, ace, dealer_hand, wager) = (busts + 1, '', -1, false, -1, -1) WHERE uid = "
            + uid + ";");
        return Casino.checkBalance(uid);
    }

    private static long blackjackLoss(long uid) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (hand, sum, ace, dealer_hand, wager) = ('', -1, false, -1, -1) WHERE uid = "
            + uid +";");
        return Casino.checkBalance(uid);
    }

    private static long blackjackTie(long uid, long winnings) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (ties, winnings, hand, sum, ace, dealer_hand, wager) = (ties + 1, winnings + "
            + winnings + ",'', -1, false, -1, -1) WHERE uid = " + uid +";");
        return Casino.addMoney(uid, winnings);
    }

    private static long blackjackWin(long uid, long winnings, boolean dealerBust) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (dealer_busts, wins, winnings, hand, sum, ace, dealer_hand, wager) = (dealer_busts + "
            + (dealerBust ? 1 : 0) + ", wins + " + (dealerBust ? 0 : 1) + ", winnings + "
            + winnings + ", '', -1, false, -1, -1) WHERE uid = " + uid + ";");
        return Casino.addMoney(uid, 2 * winnings);
    }

    private static BlackJackGame getBlackjackGame(long uid) {
        String query = "SELECT hand, sum, ace, dealer_hand, wager FROM blackjack_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                String hand = results.getString(1);
                int sum = results.getInt(2);
                boolean ace = results.getBoolean(3);
                int dealer = results.getInt(4);
                int wager = results.getInt(5);
                return new Blackjack.BlackJackGame(hand, sum, ace, dealer, wager);
            }
            return null;
        }, null);
    }

}
