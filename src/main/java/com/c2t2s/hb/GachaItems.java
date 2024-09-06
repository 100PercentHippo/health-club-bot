package com.c2t2s.hb;

import java.util.List;

public class GachaItems {

    private static final int WORK_STAT_INDEX = 0;
    private static final int FISH_STAT_INDEX = 1;
    private static final int PICK_STAT_INDEX = 2;
    private static final int ROB_STAT_INDEX  = 3;
    private static final int MISC_STAT_INDEX = 4;
    private static final int STAT_SIZE = 5;
    private static final int TENDENCY_STAT_INDEX = 5;

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

    private static final int FRACTURED_GEM_ID = 0;
    private static final int PURE_GEM_ID = 1;
    private static final int UNSTABLE_GEM_ID = 2;

    private abstract static class Gem {
        int id;
        String name;
        String description;

        abstract List<String> apply(Item item);

        int getId() { return id; }
        String getName() { return name; }
        String getDescription() { return description; }

        static Gem fromId(int id) {
            switch (id) {
                case FRACTURED_GEM_ID:
                    return new BasicGem(FRACTURED_GEM_ID, "Fractured Gem", "+0.2 to 1 stat & -0.1 to 1 stat",
                            new int[]{2}, new int[]{1});
                case PURE_GEM_ID:
                    return new BasicGem(PURE_GEM_ID, "Pure Gem", "+0.1 to 1 stat",
                            new int[]{1}, new int[]{});
                case UNSTABLE_GEM_ID:
                    return new BasicGem(UNSTABLE_GEM_ID, "Unstable Gem", "+0.3 to 1 stat & -0.1 to 1 stat & -0.1 to 1 stat",
                            new int[]{3}, new int[]{1, 1});
                default:
                    throw new IllegalArgumentException("Encountered unexpected gem id: " + id);
            }
        }
    }

    private static class BasicGem extends Gem {
        int[] additions;
        int[] subtractions;

        BasicGem(int id, String name, String description, int[] additions, int[] subtractions) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.additions = additions;
            this.subtractions = subtractions;
        }

        List<String> apply(Item item) {

        }
    }

    private static class AppliedGem {
        private int gemType;
        private int[] modifiedStats;

        String getDescription() {
            StringBuilder builder = new StringBuilder();
            builder.append(GEM.fromId(gemType).)
        }

        AppliedGem(int gemType, int[] modifiedStats) {
            this.gemType = gemType;
            this.modifiedStats = modifiedStats;
        }
    }

    private static final int INITIAL_BONUS_INDEX = 0;
    private static final int GEM_BONUS_INDEX = 1;

    private abstract static class Item {
        double generatorVersion;
        // { {5 element initial bonus array}
        //   {5 element gem bonus array} }
        int[][] bonuses;
        List<AppliedGem> gems;

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

        abstract int addRandomStat(int amount, boolean initial);
        abstract int subtractRandomStat(int amount, boolean initial);

        abstract double getModifier(ITEM_STAT stat);
        abstract double getBonusModifierContribution(int baseModifier);
        abstract int getTier();

        void addRandomStats(int amount, boolean initial, int repetitions) {
            for (int i = 0; i < repetitions; ++i) {
                addRandomStat(amount, initial);
            }
        }

        void subtractRandomStats(int amount, boolean initial, int repetitions) {
            for (int i = 0; i < repetitions; ++i) {
                subtractRandomStat(amount, initial);
            }
        }

        AppliedGem applyGem(Gem)

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
            for (AppliedGem gem : gems) {
                builder.append("\n  ");
                builder.append(gem.getDescription());
            }
            for (int i = 0; i < gemSlots - gems.size(); ++i) {
                builder.append("\n  Empty Gem Slot");
            }
            return builder.toString();
        }

        static Item getItem(double generatorVersion, int[][] bonuses, ITEM_STAT positiveTendency,
                ITEM_STAT negativeTendency, ITEM_STAT bonusStat, int initialAdditions,
                int appliedAdditions, int initialSubtractions, int appliedSubtractiions,
                int enhancementLevel, int gemSlots) {
            // In the future use the generator version to pick the right item generation
            return new G0Item(generatorVersion, bonuses, positiveTendency, negativeTendency,
                bonusStat, initialAdditions, appliedAdditions, initialSubtractions,
                appliedSubtractiions, enhancementLevel, gemSlots);
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
            item.addRandomStats(3, true, allocation / 3);
            item.addRandomStats(2, true, allocation / 2);
            item.addRandomStats(1, true, allocation);
            allocation = subtractions / 3;
            item.subtractRandomStats(3, true, allocation / 3);
            item.subtractRandomStats(2, true, allocation / 2);
            item.subtractRandomStats(1, true, allocation);

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

        int addRandomStat(int amount, boolean initial) {
            int index = initial ? INITIAL_BONUS_INDEX : GEM_BONUS_INDEX;
            int stat;
            if (HBMain.RNG_SOURCE.nextDouble() < TENDENCY_CHANCE) {
                stat = TENDENCY_STAT_INDEX;
                bonuses[index][positiveTendency.getIndex()] += amount;
            } else {
                stat = HBMain.RNG_SOURCE.nextInt(STAT_SIZE);
                bonuses[index][stat] += amount;
            }
            if (initial) {
                initialAdditions += amount;
            } else {
                appliedAdditions += amount;
            }
            return stat;
        }

        int subtractRandomStat(int amount, boolean initial) {
            int index = initial ? INITIAL_BONUS_INDEX : GEM_BONUS_INDEX;
            int stat;
            if (HBMain.RNG_SOURCE.nextDouble() < TENDENCY_CHANCE) {
                stat = TENDENCY_STAT_INDEX;
                bonuses[index][negativeTendency.getIndex()] -= amount;
            } else {
                stat = HBMain.RNG_SOURCE.nextInt(STAT_SIZE);
                bonuses[index][stat] -= amount;
            }
            if (initial) {
                initialSubtractions += amount;
            } else {
                appliedSubtractions += amount;
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

    // CREATE TABLE IF NOT EXISTS gacha_item (
    //  iid SERIAL PRIMARY KEY,
    //  uid bigint NOT NULL,
    //  generator double NOT NULL,
    //  enhancement_level integer NOT NULL,
    //  gem_slots integer NOT NULL,
    //  positive_tendency integer NOT NULL,
    //  negative_tendency integer NOT NULL,
    //  bonusStat integer NOT NULL,
    //  initial_additions integer NOT NULL,
    //  initial_subtractions integer NOT NULL,
    //  initial_work integer NOT NULL,
    //  initial_fish integer NOT NULL,
    //  initial_pick integer NOT NULL,
    //  initial_rob integer NOT NULL,
    //  initial_misc integer NOT NULL,
    //  CONSTRAINT gacha_item_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );
