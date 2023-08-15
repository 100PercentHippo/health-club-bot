package com.c2t2s.hb;

import java.net.URISyntaxException;
import java.sql.*; //TODO: Remove the *
import java.util.List;
import java.util.Random;
import java.util.ArrayList;

public class Blackjack {

    private static String[] cardLetters = {"", "[A]", "[2]", "[3]", "[4]", "[5]", "[6]", "[7]", "[8]", "[9]", "[10]", "[J]", "[Q]", "[K]"};
    private static int[] cardValues = {0, 11, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10};

    public static class BlackJackGame {
        private String hand;
        private int sum;
        private boolean contains_ace;
        private int dealer_hand;
        private int wager;

        public BlackJackGame(String hand, int sum, boolean ace, int dealer, int wager) {
            this.hand = hand;
            this.sum = sum;
            this.contains_ace = ace;
            this.dealer_hand = dealer;
            this.wager = wager;
        }

        public String getHand() {
            return hand;
        }

        public int getSum() {
            return sum;
        }

        public boolean hasAce() {
            return contains_ace;
        }

        public int getDealerHand() {
            return dealer_hand;
        }

        public int getWager() {
            return wager;
        }
    }

    private static String displayGame(String hand, int dealer, String dealerCardTwo) {
        return "Your hand:    " + hand + "\nDealer's hand: " + cardLetters[dealer] + dealerCardTwo;
    }

    public static String handleBlackjack(long uid, long wager) {
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null) {
            return "Unable to fetch game from the database. Did you `+claim`?";
        } else if (game.getWager() != -1) {
            return "You have a currently active game:\n" + displayGame(game.getHand(), game.getDealerHand(), "[?]")
                + "\nUse `+hit` or `+stand` to play it out";
        }
        long balance = Casino.checkBalance(uid);
        if (balance < 0) {
            return "Unable to start game. Balance check failed or was negative (" + balance +")";
        } else if (balance < wager) {
            return "Your current balance of " + balance + " is not enough to cover that";
        }
        Random random = new Random();
        int dealerCard = random.nextInt(13) + 1;
        String hand = "";
        int value = 0;
        boolean hasAce = false;
        for (int i = 0; i < 2; i++) {
            int card = random.nextInt(13) + 1;
            hand += cardLetters[card];
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
        Casino.takeMoneyDirect(uid, wager);
        newBlackjackGame(uid, hand, value, hasAce, dealerCard, wager);
        return "Bid " + wager + " on Blackjack\n" + displayGame(hand, dealerCard, "[?]");
    }

    public static List<String> handleStand(long uid) {
    	List<String> response = new ArrayList<>();
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null || game.getWager() == -1) {
        	response.add("No active game found. Use `/blackjack new` to start a new game");
            return response;
        }
        String playerHand = "Your hand:    " + game.getHand();
        String dealerHand = "\nDealer's hand: " + cardLetters[game.getDealerHand()];
        int dealerTotal = cardValues[game.getDealerHand()];
        boolean dealerAce = (game.getDealerHand() == 1);
        Random random = new Random();
        while ((!dealerAce && dealerTotal < 17) || (dealerAce && dealerTotal < 17)
                || (dealerAce && dealerTotal > 21 && dealerTotal - 10 < 17)) {
            int card = random.nextInt(13) + 1;
            dealerHand += cardLetters[card];
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
            response.add(new String(playerHand + dealerHand));
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
            blackjackLoss(uid, game.getWager());
            resolution = "\nDealer wins. Your new balance is " + Casino.checkBalance(uid);
        } else if (dealerTotal < playerTotal) {
        	resolution = "\nYou win " + (2 * game.getWager())
                    + "! Your new balance is " + blackjackWin(uid, game.getWager(), false);
        } else { // Tie
        	resolution = "\nTie. You get " + game.getWager() + " back. Your new balance is "
                    + blackjackTie(uid, game.getWager());
        }
        response.add(playerHand + dealerHand + resolution);
        return response;
    }

    public static String handleHit(long uid) {
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null || game.getWager() == -1) {
            return "No active game found. Type `/blackjack new` to start a new game";
        }
        Random random = new Random();
        boolean hasAce = game.hasAce();
        int value = game.getSum();
        int card = random.nextInt(13) + 1;
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
            return displayGame(hand, game.getDealerHand(), cardLetters[random.nextInt(13) + 1])
                + "\nBust! Your new balance is " + blackjackBust(uid, game.getWager());
        } else {
            updateBlackjackGame(uid, hand, value, hasAce);
            return displayGame(hand, game.getDealerHand(), "[?]");
        }
    }

    /////////////////////////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS blackjack_user (
    //  uid bigint PRIMARY KEY,
    //  hands integer DEFAULT 0,
    //  busts integer DEFAULT 0,
    //  dealer_busts integer DEFAULT 0,
    //  ties integer DEFAULT 0,
    //  wins integer DEFAULT 0,
    //  spent bigint DEFAULT 0,
    //  winnings bigint DEFAULT 0,
    //  hand varchar(63) DEFAULT '',
    //  sum integer DEFAULT -1,
    //  ace boolean DEFAULT false,
    //  dealer_hand integer DEFAULT -1,
    //  wager bigint DEFAULT -1,
    //  CONSTRAINT blackjack_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    public static void newBlackjackGame(long uid, String hand, int sum, boolean hasAce, int dealerHand, long wager) {
        Casino.executeUpdate("UPDATE blackjack_user SET (hands, spent, hand, sum, ace, dealer_hand, wager) = (hands + 1, spent + "
            + wager + ", '" + hand + "', " + sum + ", " + hasAce + ", " + dealerHand + ", "
            + wager + ") WHERE uid = " + uid + ";");
    }

    public static void updateBlackjackGame(long uid, String hand, int sum, boolean contains_ace) {
        Casino.executeUpdate("UPDATE blackjack_user SET (hand, sum, ace) = ('" + hand
            + "', " + sum + ", " + contains_ace + ") WHERE uid = " + uid + ";");
    }

    public static long blackjackBust(long uid, long amount) {
        Casino.executeUpdate("UPDATE blackjack_user SET (busts, hand, sum, ace, dealer_hand, wager) = (busts + 1, '', -1, false, -1, -1) WHERE uid = "
            + uid + ";");
        return Casino.checkBalance(uid);
    }

    public static long blackjackLoss(long uid, long amount) {
        Casino.executeUpdate("UPDATE blackjack_user SET (hand, sum, ace, dealer_hand, wager) = ('', -1, false, -1, -1) WHERE uid = "
            + uid +";");
        return Casino.checkBalance(uid);
    }

    public static long blackjackTie(long uid, long winnings) {
        Casino.executeUpdate("UPDATE blackjack_user SET (ties, winnings, hand, sum, ace, dealer_hand, wager) = (ties + 1, winnings + "
            + winnings + ",'', -1, false, -1, -1) WHERE uid = " + uid +";");
        return Casino.addMoneyDirect(uid, winnings);
    }

    public static long blackjackWin(long uid, int winnings, boolean dealerBust) {
        Casino.executeUpdate("UPDATE blackjack_user SET (dealer_busts, wins, winnings, hand, sum, ace, dealer_hand, wager) = (dealer_busts + "
            + (dealerBust ? 1 : 0) + ", wins + " + (dealerBust ? 0 : 1) + ", winnings + "
            + winnings + ", '', -1, false, -1, -1) WHERE uid = " + uid + ";");
        return Casino.addWinnings(uid, 2 * winnings, winnings);
    }

    public static BlackJackGame getBlackjackGame(long uid) {
        String query = "SELECT hand, sum, ace, dealer_hand, wager FROM blackjack_user WHERE uid = " + uid + ";";
        Connection connection = null;
        Statement statement = null;
        BlackJackGame game = null;
        try {
            connection = Casino.getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
                String hand = results.getString(1);
                int sum = results.getInt(2);
                boolean ace = results.getBoolean(3);
                int dealer = results.getInt(4);
                int wager = results.getInt(5);
                game = new Blackjack.BlackJackGame(hand, sum, ace, dealer, wager);
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

}
