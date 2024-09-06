package com.c2t2s.hb;

import java.util.List;

public class GachaItems {

    private static final int WORK_STAT_INDEX = 0;
    private static final int FISH_STAT_INDEX = 1;
    private static final int PICK_STAT_INDEX = 2;
    private static final int ROB_STAT_INDEX  = 3;
    private static final int MISC_STAT_INDEX = 4;
    private static final int INITIAL_BONUS_INDEX = 0;
    private static final int GEM_BONUS_INDEX = 1;

    private enum ITEM_STAT {
        WORK(WORK_STAT_INDEX, 'W', "Work", "Hardworking", "Lazy", "Tie", "Suit"),
        FISH(FISH_STAT_INDEX, 'F', "Fish", "Patient", "Impatient", "Reel", "Tacklebox"),
        PICK(PICK_STAT_INDEX, 'P', "Pick", "Inconspicuous", "Conspicuous", "Gloves", "Mage Hand"),
        ROB(ROB_STAT_INDEX, 'R', "Rob", "Cautious", "Hasty", "Crowbar", "Gun"),
        MISC(MISC_STAT_INDEX, 'M', "Misc", "Flexible", "Inflexible", "Credit Card", "Loaded Die");

        private int index;
        private char letter;
        private String statName;
        private String positiveAdjective;
        private String negativeAdjective;
        private String baseItemName;
        private String enhancedItemName;
        ITEM_STAT(int index, char letter, String statName, String positiveAdjective,
                String negativeAdjective, String baseItemName, String enhancedItemName) {
            this.index = index;
            this.letter = letter;
            this.statName = statName;
            this.positiveAdjective = positiveAdjective;
            this.negativeAdjective = negativeAdjective;
            this.baseItemName = baseItemName;
            this.enhancedItemName = enhancedItemName;
        }
        int getIndex() { return index; }
        char getLetter() { return letter; }
        String getStatName() { return statName; }
        String getPositiveAdjective() { return positiveAdjective; }
        String getNegativeAdjective() { return negativeAdjective; }
        String getItemName(int enhancementLevel) {
            if (enhancementLevel == 0) {
                return baseItemName;
            } else {
                return enhancedItemName;
            }
        }

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

    private abstract static class Gem {
        abstract String getDescription();
    }

    private abstract static class Item {
        double generatorVersion;
        // { {5 element initial bonus array}
        //   {5 element gem bonus array} }
        int[][] bonuses;
        List<Gem> gems;

        // Stat selected is more likely to be rolled when adding stats
        ITEM_STAT positiveTendency;
        // Stat selected is more likely to be rolled when subtracting stats
        ITEM_STAT negativeTendency;
        ITEM_STAT bonusStat;

        int initialAdditions = 0;
        int appliedAdditions = 0;
        int initialSubtractions = 0;
        int appliedSubtractions = 0;

        int gemSlots;
        int enhancementLevel = 0;

        abstract void addRandomStats(int amount, int repetitions, boolean initial);
        abstract void subtractRandomStats(int amount, int repetitions, boolean initial);

        abstract double getModifier(ITEM_STAT stat);
        abstract double getBonusModifierContribution(int baseModifier);
        abstract int getTier();

        public String getName() {
            // [Tier #][Adjective from positive affinity][Adjective from negative affinity][Item name from bonus]
            StringBuilder builder = new StringBuilder();
            builder.append("Tier ");
            builder.append(getTier());
            builder.append(' ');
            builder.append(positiveTendency.getPositiveAdjective());
            builder.append(' ');
            builder.append(negativeTendency.getNegativeAdjective());
            builder.append(' ');
            builder.append(bonusStat.getItemName(enhancementLevel));
            return builder.toString();
        }

        public String getBriefDescription() {
            // e.g. [-0.1W][0.4F][1.1P][0.2R][-0.5M]
            StringBuilder builder = new StringBuilder();
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                builder.append('[');
                builder.append(Stats.oneDecimal.format(getModifier(stat)));
                builder.append(stat.getLetter());
                builder.append(']');
            }
            return builder.toString();
        }

        public String getFullDescription() {
            StringBuilder builder = new StringBuilder();
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                int initialBonus = bonuses[INITIAL_BONUS_INDEX][stat.getIndex()];
                int gemBonus = bonuses[GEM_BONUS_INDEX][stat.getIndex()];
                if (!builder.isEmpty()) { builder.append('\n'); }
                builder.append(stat.getStatName());
                builder.append(": ");
                builder.append(Stats.oneDecimal.format(getModifier(stat)));
                builder.append(" (");
                builder.append(Stats.oneDecimal.format(initialBonus / 10.0));
                builder.append(" Base");
                if (gemBonus != 0) {
                    builder.append(", ");
                    builder.append(Stats.oneDecimal.format(gemBonus / 10.0));
                    builder.append(" from Gems");
                }
                if (stat == bonusStat) {
                    double bonusContribution = getBonusModifierContribution(initialBonus + gemBonus);
                    if (bonusContribution != 0.0) {
                        builder.append(", ");
                        builder.append(Stats.oneDecimal.format(bonusContribution));
                        builder.append(" from Item Type");
                    }
                }
                builder.append(')');
            }
            builder.append("\n\nGems:");
            for (Gem gem : gems) {
                builder.append("\n  ");
                builder.append(gem.getDescription());
            }
            for (int i = 0; i < gemSlots - gems.size(); ++i) {
                builder.append("\n  Empty Gem Slot");
            }
            return builder.toString();
        }
    }

    private static class G0Item extends Item {

        private static final double GENERATOR_VERSION = 0.3;
        private static final double LN_ONE_HALF = Math.log(0.5);
        private static final int AVERAGE_ADDITIONS = 50;
        private static final int AVERAGE_SUBTRACTIONS = 50;
        private static final int ADDITIONS_STDDEV = 25;
        private static final int BASE_STAT_AMOUNT = 2;
        private static final int BASE_GEM_SLOTS = 2;
        private static final double TENDENCY_CHANCE = 0.1;

        static G0Item generate() {
            // Repeating 50% chance per additional gem slots
            Double extraGemSlots = Math.log(HBMain.RNG_SOURCE.nextDouble()) / LN_ONE_HALF;
            int gemSlots = BASE_GEM_SLOTS + extraGemSlots.intValue();

            ITEM_STAT positiveTendency = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));
            ITEM_STAT negativeTendency = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));
            ITEM_STAT bonusStat        = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));

            G0Item item = new G0Item(positiveTendency, negativeTendency, bonusStat, gemSlots);

            // Roll for initial additions and subtractions
            int additions    = HBMain.generateBoundedNormal(AVERAGE_ADDITIONS, ADDITIONS_STDDEV, 0);
            int subtractions = HBMain.generateBoundedNormal(AVERAGE_SUBTRACTIONS, ADDITIONS_STDDEV, 0);

            // Award 1/3 of the stats 3 at a time, 1/3 2 at a time, and the last 1/3 1 at a time
            // Some rounding will occur, but that's fine
            int allocation = additions / 3;
            item.addRandomStats(3, allocation / 3, true);
            item.addRandomStats(2, allocation / 2, true);
            item.addRandomStats(1, allocation, true);
            allocation = subtractions / 3;
            item.subtractRandomStats(3, allocation / 3, true);
            item.subtractRandomStats(2, allocation / 2, true);
            item.subtractRandomStats(1, allocation, true);

            return item;
        }

        G0Item(ITEM_STAT positiveTendency, ITEM_STAT negativeTendency, ITEM_STAT bonusStat,
                int gemSlots) {
            generatorVersion = GENERATOR_VERSION;
            bonuses = new int[][] {
                    new int[]{BASE_STAT_AMOUNT, BASE_STAT_AMOUNT, BASE_STAT_AMOUNT,
                        BASE_STAT_AMOUNT, BASE_STAT_AMOUNT},
                    new int[]{0, 0, 0, 0, 0}
                };
            this.positiveTendency = positiveTendency;
            this.negativeTendency = negativeTendency;
            this.bonusStat        = bonusStat;
            this.gemSlots         = gemSlots;
        }

        G0Item(double generatorVersion, int[][] bonuses, ITEM_STAT positiveTendency,
                ITEM_STAT negativeTendency, ITEM_STAT bonusStat, int initialAdditions,
                int appliedAdditions, int initialSubtractions, int appliedSubtractiions,
                int enhancementLevel, int gemSlots) {
            this.generatorVersion    = generatorVersion;
            this.bonuses             = bonuses;
            this.positiveTendency    = positiveTendency;
            this.negativeTendency    = negativeTendency;
            this.bonusStat           = bonusStat;
            this.initialAdditions    = initialAdditions;
            this.appliedAdditions    = appliedAdditions;
            this.initialSubtractions = initialSubtractions;
            this.appliedSubtractions = appliedSubtractiions;
            this.enhancementLevel    = enhancementLevel;
            this.gemSlots            = gemSlots;
        }

        void addRandomStats(int amount, int repetitions, boolean initial) {
            int index = initial ? INITIAL_BONUS_INDEX : GEM_BONUS_INDEX;
            for (int i = 0; i < repetitions; i++) {
                if (HBMain.RNG_SOURCE.nextDouble() < TENDENCY_CHANCE) {
                    bonuses[index][positiveTendency.getIndex()] += amount;
                } else {
                    bonuses[index][HBMain.RNG_SOURCE.nextInt(5)] += amount;
                }
            }
            if (initial) {
                initialAdditions += amount * repetitions;
            } else {
                appliedAdditions += amount * repetitions;
            }
        }

        void subtractRandomStats(int amount, int repetitions, boolean initial) {
            int index = initial ? INITIAL_BONUS_INDEX : GEM_BONUS_INDEX;
            for (int i = 0; i < repetitions; i++) {
                if (HBMain.RNG_SOURCE.nextInt(10) == 0) {
                    bonuses[index][negativeTendency.getIndex()] -= amount;
                } else {
                    bonuses[index][HBMain.RNG_SOURCE.nextInt(5)] -= amount;
                }
            }
            if (initial) {
                initialSubtractions += amount * repetitions;
            } else {
                appliedSubtractions += amount * repetitions;
            }
        }

        double getModifier(ITEM_STAT stat) {
            int modifier = bonuses[INITIAL_BONUS_INDEX][stat.getIndex()]
                + bonuses[GEM_BONUS_INDEX][stat.getIndex()];
            return modifier / 10.0
                + (stat == bonusStat ? getBonusModifierContribution(modifier) : 0);
        }

        double getBonusModifierContribution(int baseModifier) {
            if (baseModifier <= 0) { return 0.0; }
            // Add 50%, using int math as a floor
            return (baseModifier / 2) / 10.0;
        }

        int getTier() {
            return ((initialAdditions + appliedAdditions) - (initialSubtractions + appliedSubtractions)) / 10;
        }
    }

    static String handleTest() {
        StringBuilder builder = new StringBuilder();
        builder.append(G0Item.GENERATOR_VERSION);
        builder.append('\n');
        for (int i = 0; i < 10; i++) {
            Item item = G0Item.generate();
            builder.append(item.getName());
            builder.append(item.getBriefDescription());
            builder.append('\n');
        }
        return builder.toString();
    }
}
