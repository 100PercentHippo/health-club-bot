package com.c2t2s.hb;

import java.util.List;
import java.util.ArrayList;
import org.javacord.api.entity.message.component.ActionRow;

class Blackjack {

    // Hide public constructor
    private Blackjack() {}

    private static final char CARD_LETTER_ACE   = 'A';
    private static final char CARD_LETTER_TWO   = '2';
    private static final char CARD_LETTER_THREE = '3';
    private static final char CARD_LETTER_FOUR  = '4';
    private static final char CARD_LETTER_FIVE  = '5';
    private static final char CARD_LETTER_SIX   = '6';
    private static final char CARD_LETTER_SEVEN = '7';
    private static final char CARD_LETTER_EIGHT = '8';
    private static final char CARD_LETTER_NINE  = '9';
    private static final char CARD_LETTER_TEN   = '0';
    private static final char CARD_LETTER_JACK  = 'J';
    private static final char CARD_LETTER_QUEEN = 'Q';
    private static final char CARD_LETTER_KING  = 'K';
    private static final char CARD_LETTER_DEALER = '?';
    private static final String NO_ACTIVE_GAME_MESSAGE = "No active game found. Use `/blackjack new` to start a new game";

    private enum BLACKJACK_CARD {
        ACE  (CARD_LETTER_ACE,   11),
        TWO  (CARD_LETTER_TWO,    2),
        THREE(CARD_LETTER_THREE,  3),
        FOUR (CARD_LETTER_FOUR,   4),
        FIVE (CARD_LETTER_FIVE,   5),
        SIX  (CARD_LETTER_SIX,    6),
        SEVEN(CARD_LETTER_SEVEN,  7),
        EIGHT(CARD_LETTER_EIGHT,  8),
        NINE (CARD_LETTER_NINE,   9),
        TEN  (CARD_LETTER_TEN,   10),
        JACK (CARD_LETTER_JACK,  10),
        QUEEN(CARD_LETTER_QUEEN, 10),
        KING (CARD_LETTER_KING,  10),
        DEALER(CARD_LETTER_DEALER, 0);

        private char letter;
        private int value;

        BLACKJACK_CARD(char letter, int value) {
            this.letter = letter;
            this.value = value;
        }

        char getChar() { return letter; }
        int getValue() { return value; }
        public String toString() {
            StringBuilder output = new StringBuilder(4);
            output.append('[');
            if (letter == CARD_LETTER_TEN) {
                output.append("10");
            } else {
                output.append(letter);
            }
            output.append(']');
            return output.toString();
        }

        static BLACKJACK_CARD fromChar(char letter) {
            switch (letter) {
                case CARD_LETTER_ACE:
                    return BLACKJACK_CARD.ACE;
                case CARD_LETTER_TWO:
                    return BLACKJACK_CARD.TWO;
                case CARD_LETTER_THREE:
                    return BLACKJACK_CARD.THREE;
                case CARD_LETTER_FOUR:
                    return BLACKJACK_CARD.FOUR;
                case CARD_LETTER_FIVE:
                    return BLACKJACK_CARD.FIVE;
                case CARD_LETTER_SIX:
                    return BLACKJACK_CARD.SIX;
                case CARD_LETTER_SEVEN:
                    return BLACKJACK_CARD.SEVEN;
                case CARD_LETTER_EIGHT:
                    return BLACKJACK_CARD.EIGHT;
                case CARD_LETTER_NINE:
                    return BLACKJACK_CARD.NINE;
                case CARD_LETTER_TEN:
                    return BLACKJACK_CARD.TEN;
                case CARD_LETTER_JACK:
                    return BLACKJACK_CARD.JACK;
                case CARD_LETTER_QUEEN:
                    return BLACKJACK_CARD.QUEEN;
                case CARD_LETTER_KING:
                    return BLACKJACK_CARD.KING;
                default:
                    return BLACKJACK_CARD.DEALER;
            }
        }

        static BLACKJACK_CARD newCard() {
            return BLACKJACK_CARD.class.getEnumConstants()[HBMain.RNG_SOURCE.nextInt(13)];
        }
    }

    private static class BlackJackHand {
        private List<BLACKJACK_CARD> hand;
        private int sum;
        private boolean containsAce;

        private BlackJackHand() {
            hand = new ArrayList<>();
        }

        private BlackJackHand(char card) {
            this.hand = new ArrayList<>();
            addCard(BLACKJACK_CARD.fromChar(card));
        }

        private BlackJackHand(String hand) {
            this.hand = new ArrayList<>();
            for (int i = 0; i < hand.length(); i++) {
                addCard(BLACKJACK_CARD.fromChar(hand.charAt(i)));
            }
        }

        // Getters

        private int getSum() {
            if (sum > 21 && containsAce) {
                return sum - 10;
            }
            return sum;
        }

        private boolean isBust() {
            return sum > (containsAce ? 31 : 21);
        }

        private boolean wouldDealerHit() {
            return sum < 17 || (containsAce && sum > 21 && sum < 27);
        }

        private boolean canSplit() {
            return hand.size() == 2 && hand.get(0) == hand.get(1);
        }

        private String getRawHand() {
            StringBuilder output = new StringBuilder();
            for (BLACKJACK_CARD card: hand) {
                output.append(card.getChar());
            }
            return output.toString();
        }

        // Modify hand state

        private void addCard() {
            addCard(BLACKJACK_CARD.newCard());
        }

        private void addCard(BLACKJACK_CARD card) {
            hand.add(card);
            if (card == BLACKJACK_CARD.ACE) {
                if (containsAce) {
                    sum += 1;
                } else {
                    containsAce = true;
                    sum += 11;
                }
            } else {
                sum += card.getValue();
            }
        }

        private void split() {
            BLACKJACK_CARD firstCard = hand.get(0);
            hand = new ArrayList<>();
            addCard(firstCard);
            addCard();
        }

        // Output formatting

        private String displayHand() {
            StringBuilder output = new StringBuilder();
            for (BLACKJACK_CARD card: hand) {
                output.append(card.toString());
            }
            return output.toString();
        }

        private static String displayDormantHand(String cards) {
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < cards.length(); i++) {
                output.append(BLACKJACK_CARD.fromChar(cards.charAt(i)));
            }
            return output.toString();
        }

        private static String displayDealerHand(char card) {
            return BLACKJACK_CARD.fromChar(card).toString() + BLACKJACK_CARD.DEALER.toString();
        }

        private static String displayDealerHandAfterBust(char card) {
            return BLACKJACK_CARD.fromChar(card).toString() + BLACKJACK_CARD.newCard().toString();
        }
    }

    private static class BlackJackGame {
        private String rawCards;
        private BlackJackHand activeHand;
        private int splitIndex;
        private boolean resolvingPrimaryHand = true;
        private char dealerCard;
        private long wager;

        private BlackJackGame(long wager) {
            this.wager = wager;
            activeHand = new BlackJackHand();
            activeHand.addCard();
            activeHand.addCard();
            rawCards = activeHand.getRawHand();
        }

        private BlackJackGame(String hand, char dealer, long wager) {
            this.dealerCard = dealer;
            this.wager = wager;
            rawCards = hand;
            splitIndex = rawCards.indexOf('<');
            if (splitIndex != -1) {
                this.activeHand = new BlackJackHand(rawCards.substring(0, splitIndex));
                return;
            }
            splitIndex = rawCards.indexOf('>');
            if (splitIndex != -1) {
                this.activeHand = new BlackJackHand(rawCards.substring(splitIndex + 1));
                resolvingPrimaryHand = false;
            } else {
                this.activeHand = new BlackJackHand(hand);
            }
        }

        // Getters

        private char getDealerCard() {
            return dealerCard;
        }

        private long getWager() {
            return wager;
        }

        private String getHand() {
            return rawCards;
        }

        private int getSum() {
            return activeHand.getSum();
        }

        private boolean isGameSplit() {
            return splitIndex > 0;
        }

        private boolean canSplit() {
            return rawCards.length() > 0 && activeHand.canSplit() && !isGameSplit();
        }

        private boolean isCurrentHandBust() {
            return activeHand.isBust();
        }

        private boolean resolvingFirstSplitHand() {
            return isGameSplit() && resolvingPrimaryHand;
        }

        private boolean resolvingSecondSplitHand() {
            return isGameSplit() && !resolvingPrimaryHand;
        }

        private BlackJackHand getFirstSplitHand() {
            if (!isGameSplit()) {
                return activeHand;
            }
            return new BlackJackHand(rawCards.substring(splitIndex));
        }

        private BlackJackHand getSecondSplitHand() {
            return activeHand;
        }

        // Modifies the game state

        private void split() {
            activeHand.split();
            rawCards = new StringBuilder(5).append(activeHand.getRawHand())
                .append('<').append(rawCards.charAt(0)).append(BLACKJACK_CARD.newCard().getChar())
                .toString();
            splitIndex = 3;
        }

        private void addCard() {
            activeHand.addCard();
            if (!isGameSplit()) {
                rawCards = activeHand.getRawHand();
            } else if (resolvingPrimaryHand) {
                String firstHand = activeHand.getRawHand();
                rawCards = firstHand + rawCards.substring(splitIndex);
                splitIndex = firstHand.length();
            } else {
                rawCards = rawCards.substring(0, splitIndex + 1) + activeHand.getRawHand();
            }
        }

        private boolean advanceToSecondSplitHand() {
            if (!isGameSplit() || !resolvingPrimaryHand || rawCards.charAt(splitIndex) != '<') {
                return false;
            }
            rawCards.replace('<', '>');
            resolvingPrimaryHand = false;
            activeHand = new BlackJackHand(rawCards.substring(splitIndex + 1));
            return true;
        }

        // Output formatting

        private String displayGame() {
            return displayGame(BlackJackHand.displayDealerHand(dealerCard), false);
        }

        private String displayBustGame() {
            return displayGame(BlackJackHand.displayDealerHandAfterBust(dealerCard), true);
        }

        private String displayWithoutIndicators() {
            return displayGame(BlackJackHand.displayDealerHand(dealerCard), true);
        }

        private String displayGameBeingResolved(BlackJackHand dealerHand) {
            return displayGame(dealerHand.displayHand(), true);
        }

        private String displayGame(String dealerCards, boolean resolvingDealer) {
            StringBuilder output = new StringBuilder("Dealer's hand: ");
            output.append(dealerCards);
            if (!isGameSplit()) {
                output.append("\nYour hand:        ");
                output.append(activeHand.displayHand());
            } else if (resolvingPrimaryHand) {
                output.append("\nYour hands:\n-> ");
                output.append(activeHand.displayHand());
                output.append("\n     ");
                output.append(BlackJackHand.displayDormantHand(rawCards.substring(splitIndex + 1)));
                output.append("\nPlaying first hand");
            } else if (!resolvingDealer) {
                output.append("\nYour hands:\n     ");
                output.append(BlackJackHand.displayDormantHand(rawCards.substring(0, splitIndex)));
                output.append("\n-> ");
                output.append(activeHand.displayHand());
                output.append("\nPlaying second hand");
            } else {
                output.append("\nYour hands:\n     ");
                output.append(BlackJackHand.displayDormantHand(rawCards.substring(0, splitIndex)));
                output.append("\n     ");
                output.append(activeHand.displayHand());
            }
            return output.toString();
        }
    }

    static ActionRow getButtons(BlackJackGame game) {
        return game.canSplit() ? ButtonRows.makeBlackJackSplit(game.getWager()) : ButtonRows.BLACKJACK_BUTTONS;
    }

    static HBMain.SingleResponse handleBlackjack(long uid, long wager) {
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null) {
            return new HBMain.SingleResponse("Unable to fetch your blackjack information from the database. Did you run `/claim`?");
        } else if (game.getWager() != -1) {
            return new HBMain.SingleResponse("You have a currently active game:\n" + game.displayGame(), getButtons(game));
        }
        long balance = Casino.checkBalance(uid);
        if (balance < 0) {
            return new HBMain.SingleResponse("Unable to start game. Balance check failed or was negative (" + balance +")");
        } else if (balance < wager) {
            return new HBMain.SingleResponse("Your current balance of " + balance + " is not enough to cover that");
        }

        game = new BlackJackGame(wager);
        Casino.takeMoney(uid, wager);
        storeNewBlackjackGame(uid, game);
        return new HBMain.SingleResponse("Bid " + wager + " on Blackjack\n" + game.displayGame(), getButtons(game));
    }

    static HBMain.SingleResponse handleSplit(long uid) {
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null || game.getWager() == -1) {
            return new HBMain.SingleResponse(NO_ACTIVE_GAME_MESSAGE);
        }
        long balance = Casino.checkBalance(uid);
        if (balance < 0) {
            return new HBMain.SingleResponse("Unable to split game: Balance check failed or was negative (" + balance +")");
        } else if (balance < game.getWager()) {
            return new HBMain.SingleResponse("Unable to split: Your current balance of " + balance + " is not enough to cover the "
                + game.getWager() + " required");
        } else if (!game.canSplit()) {
            return new HBMain.SingleResponse("Current game is not eligible for splitting:\n" + game.displayGame(),
                ButtonRows.BLACKJACK_BUTTONS);
        }

        game.split();
        Casino.takeMoney(uid, game.getWager());
        blackjackSplit(uid, game);
        return new HBMain.SingleResponse(game.displayGame(), ButtonRows.BLACKJACK_BUTTONS);
    }

    static HBMain.MultistepResponse handleStand(long uid) {
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null || game.getWager() == -1) {
            return new HBMain.MultistepResponse(NO_ACTIVE_GAME_MESSAGE);
        }

        if (game.resolvingFirstSplitHand()) {
            game.advanceToSecondSplitHand();
            updateBlackjackGame(uid, game);
            return new HBMain.MultistepResponse(game.displayGame(), ButtonRows.BLACKJACK_BUTTONS);
        }

        return new HBMain.MultistepResponse(resolveDealerHand(uid, game));
    }

    static HBMain.MultistepResponse handleHit(long uid) {
        BlackJackGame game = getBlackjackGame(uid);
        if (game == null || game.getWager() == -1) {
            return new HBMain.MultistepResponse(NO_ACTIVE_GAME_MESSAGE);
        }

        game.addCard();
        if (game.isCurrentHandBust()) {
            if (game.resolvingFirstSplitHand()) {
                game.advanceToSecondSplitHand();
                updateBlackjackGame(uid, game);
                return new HBMain.MultistepResponse(
                    game.displayGame().replace("Playing second hand", "First hand bust, playing second hand"),
                    ButtonRows.BLACKJACK_BUTTONS);
            } else if (game.resolvingSecondSplitHand()) {
                if (game.getFirstSplitHand().isBust()) {
                    return new HBMain.MultistepResponse(game.displayBustGame() + "\nBoth hands bust! Your new balance is "
                        + blackjackBust(uid, true));
                }
                List<String> response = resolveDealerHand(uid, game);
                response.add(0, game.displayWithoutIndicators());
                for (int i = 0; i < response.size() - 1; i++) {
                    response.set(i, response.get(i) + "\nSecond hand bust, resolving first hand");
                }
                return new HBMain.MultistepResponse(response);
            } else {
                return new HBMain.MultistepResponse(game.displayBustGame()
                    + "\nBust! Your new balance is " + blackjackBust(uid, false));
            }
        }

        updateBlackjackGame(uid, game);
        return new HBMain.MultistepResponse(game.displayGame(), ButtonRows.BLACKJACK_BUTTONS);
    }

    private static List<String> resolveDealerHand(long uid, BlackJackGame game) {
        List<String> response = new ArrayList<>();
        response.add(game.displayWithoutIndicators());
        BlackJackHand dealerHand = new BlackJackHand(game.getDealerCard());
        while (dealerHand.wouldDealerHit()) {
            dealerHand.addCard();
            response.add(game.displayGameBeingResolved(dealerHand));
        }

        if (game.isGameSplit()) {
            StringBuilder resolution = new StringBuilder('\n');
            int busts = 0;
            int dealerBusts = 0;
            int wins = 0;
            int ties = 0;

            for (int i = 1; i < 3; i++) {
                BlackJackHand hand = null;
                if (i == 1) {
                    resolution.append("First hand ");
                    hand = game.getFirstSplitHand();
                } else {
                    resolution.append("\nSecond hand ");
                    hand = game.getSecondSplitHand();
                }

                if (hand.isBust()) {
                    busts++;
                    resolution.append("bust");
                } else if (dealerHand.isBust()) {
                    dealerBusts++;
                    wins++;
                    resolution.append("dealer busts! You win ");
                    resolution.append(2 * game.getWager());
                    resolution.append('!');
                } else if (dealerHand.getSum() < hand.getSum()) {
                    wins++;
                    resolution.append("wins! You win ");
                    resolution.append(2 * game.getWager());
                    resolution.append('!');
                } else if (dealerHand.getSum() > hand.getSum()) {
                    resolution.append("dealer wins");
                } else {
                    ties++;
                    resolution.append("tie. You get ");
                    resolution.append(game.getWager());
                    resolution.append(" back");
                }
            }

            resolution.append("\nYour new balance is ");
            resolution.append(completeSplitGame(uid, (2 * wins + ties) * game.getWager(), busts, dealerBusts, wins, ties));
            response.add(game.displayGameBeingResolved(dealerHand) + resolution.toString());
            return response;
        } else {
            String resolution = "";
            if (dealerHand.isBust()) {
                resolution = "\nDealer bust! You win " + (2 * game.getWager())
                        + "! Your new balance is " + blackjackWin(uid, 2 * game.getWager(), true);
            } else if (dealerHand.getSum() < game.getSum()) {
                resolution = "\nYou win " + (2 * game.getWager())
                        + "! Your new balance is " + blackjackWin(uid, 2 * game.getWager(), false);
            } else if (dealerHand.getSum() > game.getSum()) {
                resolution = "\nDealer wins. Your new balance is " + blackjackLoss(uid);
            } else {
                resolution = "\nTie. You get " + game.getWager() + " back. Your new balance is "
                        + blackjackTie(uid, game.getWager());
            }
            response.add(game.displayGameBeingResolved(dealerHand) + resolution);
            return response;
        }
    }

    /////////////////////////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS blackjack_user (
    //  uid bigint PRIMARY KEY,
    //  hands integer NOT NULL DEFAULT 0,
    //  times_split integer NOT NULL DEFAULT 0,
    //  busts integer NOT NULL DEFAULT 0,
    //  dealer_busts NOT NULL integer DEFAULT 0,
    //  ties integer NOT NULL DEFAULT 0,
    //  wins integer NOT NULL DEFAULT 0,
    //  spent bigint NOT NULL DEFAULT 0,
    //  winnings bigint NOT NULL DEFAULT 0,
    //  hand varchar(44) NOT NULL DEFAULT '',
    //  dealer_hand varchar(1) NOT NULL DEFAULT -1,
    //  wager bigint NOT NULL DEFAULT -1,
    //  CONSTRAINT blackjack_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    private static void storeNewBlackjackGame(long uid, BlackJackGame game) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (hands, spent, hand, dealer_hand, wager) = (hands + 1, spent + "
            + game.getWager() + ", '" + game.getHand() + "', '" + game.getDealerCard()
            + "', " + game.getWager() + ") WHERE uid = " + uid + ";");
    }

    private static void updateBlackjackGame(long uid, BlackJackGame game) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (hand) = ('" + game.getHand()
            + "') WHERE uid = " + uid + ";");
    }

    private static void blackjackSplit(long uid, BlackJackGame game) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (hands, spent, hand) = (hands + 1, spent + "
            + game.getWager() + ", '" + game.getHand() + "') WHERE uid = " + uid + ";");
    }

    private static long blackjackBust(long uid, boolean isGameSplit) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (busts, hand, dealer_hand, wager) = (busts + "
            + (isGameSplit ? 2 : 1) + ", '', '', -1) WHERE uid = " + uid + ";");
        return Casino.checkBalance(uid);
    }

    private static long blackjackLoss(long uid) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (hand, dealer_hand, wager) = ('', '', -1) WHERE uid = " + uid +";");
        return Casino.checkBalance(uid);
    }

    private static long blackjackTie(long uid, long winnings) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (ties, winnings, hand, dealer_hand, wager) = (ties + 1, winnings + "
            + winnings + ", '', '', -1) WHERE uid = " + uid +";");
        return Casino.addMoney(uid, winnings);
    }

    private static long blackjackWin(long uid, long winnings, boolean dealerBust) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (dealer_busts, wins, winnings, hand, dealer_hand, wager) = (dealer_busts + "
            + (dealerBust ? 1 : 0) + ", wins + 1, winnings + "
            + winnings + ", '', '', -1) WHERE uid = " + uid + ";");
        return Casino.addMoney(uid, winnings);
    }

    private static long completeSplitGame(long uid, long winnings, int busts, int dealerBusts, int wins, int ties) {
        CasinoDB.executeUpdate("UPDATE blackjack_user SET (busts, dealer_busts, wins, ties, winnings, hand, dealer_hand, wager) = (busts + "
            + busts + ", dealer_busts + " + dealerBusts + ", wins + " + wins + ", ties + " + ties + ", winnings + " + winnings
            + ", '', '', -1) WHERE uid = " + uid + ";");
        if (winnings > 0) {
            return Casino.addMoney(uid, winnings);
        } else {
            return Casino.checkBalance(uid);
        }
    }

    private static BlackJackGame getBlackjackGame(long uid) {
        String query = "SELECT hand, dealer_hand, wager FROM blackjack_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                String hand = results.getString(1);
                String dealer = results.getString(2);
                char dealerCard = ' ';
                if (dealer.length() > 0) {
                    dealerCard = dealer.charAt(0);
                }
                long wager = results.getLong(3);
                return new Blackjack.BlackJackGame(hand, dealerCard, wager);
            }
            return null;
        }, null);
    }

}
