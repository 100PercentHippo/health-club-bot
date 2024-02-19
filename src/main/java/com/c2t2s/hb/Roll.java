package com.c2t2s.hb;

class Roll {

    // Hide default constructor
    private Roll() {}

    static String handleRoll(String args) {
        try {
            if (args.contains("d")) {
                //Dice rolling
                return handleTabletopRoll(args);
            } else {
                // Deathrolling
                return handleDeathroll(args);
            }
        } catch (NumberFormatException e) {
            // Unrecognized syntax
            return "Unrecognized roll syntax. Try `/roll 3` or `/roll 2d6`";
        }
    }

    private static String handleDeathroll(String args) throws NumberFormatException {
        int max = Integer.parseInt(args);
        if (max < 0) {
            return "Negative numbers make me sad :slight_frown:";
        } else if (max == 0) {
            return "Rolling 0-0\n0\n.-.";
        }
        int roll = HBMain.RNG_SOURCE.nextInt(max) + 1;
        return "Rolling 1-" + max + "\n" + roll
            + (roll == 1 ? "\nIt's been a pleasure doing business with you :slight_smile: :moneybag:" : "");
    }

    //TODO: This doesn't always behave as expected with multiple arguments
    private static String handleTabletopRoll(String args) throws NumberFormatException {
        StringBuilder message = new StringBuilder("Rolling `" + args + "`\n");
        args = args.replace("-\\s*-", "");
        args = args.replace("-", "+-");
        args = args.replace("\\s", "");
        String[] pieces = args.split("\\+");
        int total = 0;
        for (int i = 0; i < pieces.length; ++i) {
            boolean negative = false;
            if (pieces[i].startsWith("-")) {
                pieces[i] = pieces[i].substring(1);
                negative = true;
            }
            if (!pieces[i].contains("d")) {
                int roll = Integer.parseInt(pieces[i]);
                message.append((negative ? " - " : " + ") + roll);
                total += (negative ? -1 : 1) * roll;
                continue;
            }
            String[] splitArgs = pieces[i].split("d");
            // If a NumberFormatException occurs, pass it up, don't catch
            int numDice = Integer.parseInt(splitArgs[0]);
            int diceSize = Integer.parseInt(splitArgs[1]);
            String text = "";
            for (int j = 0; j < numDice; ++j) {
                int roll = HBMain.RNG_SOURCE.nextInt(diceSize) + 1;
                total += (roll * (negative ? -1 : 1));
                text += (negative ? " - " : " + ") + "`" + roll + "`";
            }
            text = text.substring(2, text.length());
            if (message.length() != 0) {
                message.append(negative ? " - " : " + ");
            }
            message.append(text);
        }
        return message.toString() + "\n`" + total + "`";
    }
}
