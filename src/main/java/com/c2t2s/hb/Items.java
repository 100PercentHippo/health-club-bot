package com.c2t2s.hb;

public class Items {

    private static final int WORK_STAT_INDEX = 0;
    private static final int FISH_STAT_INDEX = 1;
    private static final int PICK_STAT_INDEX = 2;
    private static final int ROB_STAT_INDEX  = 3;
    private static final int MISC_STAT_INDEX = 4;

    // TODO: Remove debug letters
    private enum ITEM_STAT {
        WORK(WORK_STAT_INDEX, 'W'),
        FISH(FISH_STAT_INDEX, 'F'),
        PICK(PICK_STAT_INDEX, 'P'),
        ROB(ROB_STAT_INDEX, 'R'),
        MISC(MISC_STAT_INDEX, 'M');

        private int index;
        private char debugLetter;
        ITEM_STAT(int id, char letter) {
            index = id;
            debugLetter = letter;
        }
        int getIndex() { return index; }
        char getDebugLetter() { return debugLetter; }

        static ITEM_STAT fromIndex(int id) {
            switch (id) {
                case WORK_STAT_INDEX:
                    return ITEM_STAT.WORK;
                case FISH_STAT_INDEX:
                    return ITEM_STAT.FISH;
                case PICK_STAT_INDEX:
                    return ITEM_STAT.PICK;
                case ROB_STAT_INDEX:
                    return ITEM_STAT.ROB;
                case MISC_STAT_INDEX:
                default:
                    return ITEM_STAT.MISC;
            }
        }
    }

    private static class Item {
        // [Tier #][Quality description from initial + and -][Adjective from positive affinity][Item name from bonus]
        String name = "";
        String description = "";
        double generatorVersion = GENERATOR_VERSION;

        int[] bonuses;

        // Stat selected is more likely to be rolled when adding stats
        ITEM_STAT positiveTendency;
        // Stat selected is more likely to be rolled when subtracting stats
        ITEM_STAT negativeTendency;
        ITEM_STAT bonusStat;

        int initialAdditions;
        int initialSubtractions;

        int tier;
        int gemSlots;

        Item(int baseStats, int tier, int gemSlots, int initialAdditions, int initialSubtractions) {
            bonuses = new int[]{baseStats, baseStats, baseStats, baseStats, baseStats};
            positiveTendency = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));
            negativeTendency = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));
            bonusStat        = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));

            this.gemSlots            = gemSlots;
            this.tier                = tier;
            this.initialAdditions    = initialAdditions;
            this.initialSubtractions = initialSubtractions;
        }

        void addRandomStats(int amount, int repetitions) {
            for (int i = 0; i < repetitions; i++) {
                if (HBMain.RNG_SOURCE.nextInt(10) == 0) {
                    bonuses[positiveTendency.getIndex()] += amount;
                } else {
                    bonuses[HBMain.RNG_SOURCE.nextInt(5)] += amount;
                }
            }
        }

        void subtractRandomStats(int amount, int repetitions) {
            for (int i = 0; i < repetitions; i++) {
                if (HBMain.RNG_SOURCE.nextInt(10) == 0) {
                    bonuses[negativeTendency.getIndex()] -= amount;
                } else {
                    bonuses[HBMain.RNG_SOURCE.nextInt(5)] -= amount;
                }
            }
        }

        double getModifier(ITEM_STAT stat) {
            int modifier = bonuses[stat.getIndex()];
            if (stat == bonusStat) {
                // Add 50%, using int math as a floor
                modifier = modifier + modifier / 2;
            }
            return modifier / 10.0;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (ITEM_STAT stat: ITEM_STAT.values()) {
                builder.append('[');
                builder.append(Stats.oneDecimal.format(getModifier(stat)));
                builder.append(']');
            }
            builder.append("\nT");
            builder.append(tier);
            builder.append(' ');
            builder.append(initialAdditions);
            builder.append('/');
            builder.append(initialSubtractions);
            builder.append(' ');
            builder.append(positiveTendency.getDebugLetter());
            builder.append("+ ");
            builder.append(negativeTendency.getDebugLetter());
            builder.append("- ");
            builder.append(bonusStat.getDebugLetter());
            builder.append("* G");
            builder.append(gemSlots);
            return builder.toString();
        }
    }

    private static final double GENERATOR_VERSION = 0.2;
    private static final int[] TIER_STATS = {-10, -7, -5, -3, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12};
    private static final double LN_ONE_HALF = Math.log(0.5);
    private static final double TIER_OFFSET = 8.0;
    private static final int TIER_0_INDEX = 5;
    private static final double MAXIMUM_TIER = 3.0;
    private static final int BASE_GEM_SLOTS = 2;

    static Item generateItem() {
        // Roll for tier
        Double tier = Math.floor(HBMain.RNG_SOURCE.nextGaussian() * 4) + TIER_OFFSET;
        if (tier < 0.0) {
            tier = 0.0;
        } else if (tier > MAXIMUM_TIER) {
            tier = MAXIMUM_TIER;
        }

        // Repeating 50% chance per additional gem slots
        Double extraGemSlots = Math.log(HBMain.RNG_SOURCE.nextDouble()) / LN_ONE_HALF;
        int gemSlots = BASE_GEM_SLOTS + extraGemSlots.intValue();

        // Roll for initial additions and subtractions
        int additions = HBMain.RNG_SOURCE.nextInt(100);
        int subtractions = HBMain.RNG_SOURCE.nextInt(100);

        Item item = new Item(TIER_STATS[tier.intValue()], tier.intValue() - TIER_0_INDEX, gemSlots, additions, subtractions);

        // Award 1/3 of the stats 3 at a time, 1/3 2 at a time, and the last 1/3 1 at a time
        // Some rounding will occur, but that's fine
        int allocation = additions / 3;
        item.addRandomStats(3, allocation / 3);
        item.addRandomStats(2, allocation / 2);
        item.addRandomStats(1, allocation);
        allocation = subtractions / 3;
        item.subtractRandomStats(3, allocation / 3);
        item.subtractRandomStats(2, allocation / 2);
        item.subtractRandomStats(1, allocation);

        return item;
    }

    static HBMain.SingleResponse handleTest() {
        StringBuilder builder = new StringBuilder();
        builder.append(GENERATOR_VERSION);
        builder.append('\n');
        for (int i = 0; i < 10; i++) {
            builder.append(generateItem().toString());
            builder.append('\n');
        }
        return new HBMain.SingleResponse(builder.toString());
    }
}
