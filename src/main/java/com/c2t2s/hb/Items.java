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

        int[] bonuses;

        // Stat selected is more likely to be rolled when adding stats
        ITEM_STAT positiveTendency;
        // Stat selected is more likely to be rolled when subtracting stats
        ITEM_STAT negativeTendency;
        ITEM_STAT bonusStat;

        int initialAdditions;
        int initialSubtractions;

        int gemSlots;

        Item(int baseStats, int gemSlots, int initialAdditions, int initialSubtractions) {
            bonuses = new int[]{baseStats, baseStats, baseStats, baseStats, baseStats};
            positiveTendency = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));
            negativeTendency = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));
            bonusStat        = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));
            this.gemSlots    = gemSlots;

            addRandomStats(initialAdditions);
            subtractRandomStats(initialSubtractions);
            this.initialAdditions = initialAdditions;
            this.initialSubtractions = initialSubtractions;
        }

        void addRandomStats(int amount) {
            for (int i = 0; i < amount; i++) {
                if (HBMain.RNG_SOURCE.nextInt(10) == 0) {
                    bonuses[positiveTendency.getIndex()]++;
                } else {
                    bonuses[HBMain.RNG_SOURCE.nextInt(5)]++;
                }
            }
        }

        void subtractRandomStats(int amount) {
            for (int i = 0; i < amount; i++) {
                if (HBMain.RNG_SOURCE.nextInt(10) == 0) {
                    bonuses[negativeTendency.getIndex()]--;
                } else {
                    bonuses[HBMain.RNG_SOURCE.nextInt(5)]--;
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
            builder.append('\n');
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

    // Eventually make this {-20, -10, 0, 5, 10, 20}
    private static final int[] TIER_STATS = {-10, 0, 5, 10};
    private static final double LN_ONE_HALF = Math.log(0.5);
    private static final double TIER_OFFSET = 2.0;
    private static final double MAXIMUM_TIER = 3.0;
    private static final int BASE_GEM_SLOTS = 2;

    static Item generateItem() {
        // Roll for tier
        Double tier = Math.floor(HBMain.RNG_SOURCE.nextGaussian()) + TIER_OFFSET;
        if (tier < 0.0) {
            tier = 0.0;
        } else if (tier > MAXIMUM_TIER) {
            tier = MAXIMUM_TIER;
        }

        // Roll for initial additions and subtractions
        int additions = HBMain.RNG_SOURCE.nextInt(100);
        int subtractions = HBMain.RNG_SOURCE.nextInt(100);

        // Repeating 50% chance per additional gem slots
        Double extraGemSlots = Math.log(HBMain.RNG_SOURCE.nextDouble()) / LN_ONE_HALF;
        int gemSlots = BASE_GEM_SLOTS + extraGemSlots.intValue();

        return new Item(TIER_STATS[tier.intValue()], gemSlots, additions, subtractions);
    }

    static HBMain.SingleResponse handleTest() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append(generateItem().toString());
            builder.append('\n');
        }
        return new HBMain.SingleResponse(builder.toString());
    }
}
