package com.c2t2s.hb;

import java.util.Random;

public class RandomRolls {

    public static Random random = new Random();

    public static int deathroll(int max) {
        return random.nextInt(max) + 1;
    }

    public static int rollDice(String diceArgs, String textOut, boolean negative) {
        if (!diceArgs.contains("d")) {
            int roll = Integer.parseInt(diceArgs);
            textOut += (negative ? "- " : "+ ");
            return (negative ? -1 : 1) * roll;
        }
        String[] splitArgs = diceArgs.split("d");
        // If a NumberFormatException occurs, pass it up, don't catch
        int numDice = Integer.parseInt(splitArgs[0]);
        int diceSize = Integer.parseInt(splitArgs[1]);
        int total = 0;
        String text = "";
        for (int i = 0; i < numDice; ++i) {
            int roll = random.nextInt(diceSize) + 1;
            total += (roll * (negative ? -1 : 1));
            text += (negative ? "- " : "+ ") + "`" + roll + "`";
        }
        text = text.substring(2, textOut.length());
        textOut += text;
        return total;
    }
}
