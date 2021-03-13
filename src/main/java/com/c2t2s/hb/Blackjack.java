package com.c2t2s.hb;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*; //TODO: Remove the *
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class Blackjack {
	
	public static class BlackJackGame {
		private String hand;
		private int sum;
		private boolean contains_ace;
		private char dealer_hand;
		private int wager;
		
		public BlackJackGame(String hand, int sum, boolean ace, char dealer, int wager) {
			this.hand = hand;
			this.sum = sum;
			this.contains_ace = ace;
			this.dealer_hand = dealer;
			this.wager = wager;
		}
		
		public String getHand() {
			return hand;
		}
		
		public int getsum() {
			return sum;
		}
		
		public boolean hasAce() {
			return contains_ace;
		}
		
		public char getDealerHand() {
			return dealer_hand;
		}
		
		public int getWager() {
			return wager;
		}
	}
	
	private static char cardIntToChar(int card, boolean hasAce) {
		if (card == 1) {
			if (hasAce) {
				return '1';
			} else {
				return 'A';
			}
		} else if (card == 11) {
			return 'J';
		} else if (card == 12) {
			return 'Q';
		} else if (card == 13) {
			return 'K';
		} else {
			return ' '; //TODO: Change
		}
	}
	
	public static String handleBlackjack(long uid, int wager) {
		return "";
	}
	
	public static String handleStand(long uid) {
		return "";
	}
	
	public static String handleHit(long uid) {
		BlackJackGame game = getBlackjackGame(uid);
		if (game == null) {
			return "No active game found. Type `+blackjack <amount>` to start a new game";
		}
		Random random = new Random();
		int card = random.nextInt(13) + 1;
		
		return "";
	}
	
	/////////////////////////////////////////////////////////////////////////////
	
	public static long blackjackBust(long uid, int bet) {
		return 0;
	}
	
	public static long blackjackLoss(long uid, int bet) {
		return 0;
	}
	
	public static long blackjackWin(long uid, int bet) {
		return 0;
	}
	
	public static BlackJackGame getBlackjackGame(long uid) {
		return null;
	}
	
	public static void newBlackjackGame(long uid, int wager) {
		
	}
	
	public static void updateBlackjackGame(long uid, int sum, boolean contain_ace, char dealer_hand) {
		
	}

}
