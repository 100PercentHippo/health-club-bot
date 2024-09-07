package com.c2t2s.hb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GachaItems {

    private static final int WORK_STAT_INDEX = 0;
    private static final int FISH_STAT_INDEX = 1;
    private static final int PICK_STAT_INDEX = 2;
    private static final int ROB_STAT_INDEX  = 3;
    private static final int MISC_STAT_INDEX = 4;
    // Used by item add/subtract to indicate tendency procced
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

    private static class StatArray {
        private static final int[] EMPTY_STAT_ARRAY = {0, 0, 0, 0, 0};

        private int[] stats = EMPTY_STAT_ARRAY.clone();

        StatArray() { }

        StatArray(int work, int fish, int pick, int rob, int misc) {
            setStat(ITEM_STAT.WORK, work);
            setStat(ITEM_STAT.FISH, fish);
            setStat(ITEM_STAT.PICK, pick);
            setStat(ITEM_STAT.ROB, rob);
            setStat(ITEM_STAT.MISC, misc);
        }

        void setStat(ITEM_STAT stat, int value) {
            stats[stat.index] = value;
        }

        int getStat(ITEM_STAT stat) {
            return stats[stat.index];
        }

        void addStat(ITEM_STAT stat, int amount) {
            stats[stat.index] += amount;
        }

        void subtractStat(ITEM_STAT stat, int amount) {
            stats[stat.index] -= amount;
        }

        void addArray(StatArray array) {
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                addStat(stat, array.getStat(stat));
            }
        }
    }

    private static final int FRACTURED_GEM_ID = 0;
    private static final int PURE_GEM_ID = 1;
    private static final int UNSTABLE_GEM_ID = 2;
    private static final int BOLSTERING_GEM_ID = 3;
    private static final int BALANCED_GEM_ID = 4;
    private static final int RECKLESS_GEM_ID = 5;
    private static final int HARDWORKING_GEM_ID = 6;
    private static final int PATIENT_GEM_ID = 7;
    private static final int SHADOWY_GEM_ID = 8;
    private static final int INTIMIDATING_GEM_ID = 9;
    private static final int PUTRID_GEM_ID = 10;
    private static final int FORGETFUL_GEM_ID = 11;
    private static final int EQUALIZING_GEM_ID = 12;
    private static final int CHAOTIC_GEM_ID = 13;
    private static final int INVERTED_GEM_ID = 14;
    private static final int COMPOUNDING_GEM_ID = 15;
    private static final int FRACTAL_GEM_ID = 16;

    static final int COMMON_GEM_RARITY = 0;
    static final int UNCOMMON_GEM_RARITY = 1;
    static final int RARE_GEM_RARITY = 2;
    private static final int[] COMMON_GEMS
        = {FRACTURED_GEM_ID, PURE_GEM_ID, UNSTABLE_GEM_ID, BOLSTERING_GEM_ID, BALANCED_GEM_ID};
    private static final int[] UNCOMMON_GEMS
        = {RECKLESS_GEM_ID, HARDWORKING_GEM_ID, PATIENT_GEM_ID, SHADOWY_GEM_ID,
           INTIMIDATING_GEM_ID, PUTRID_GEM_ID, FORGETFUL_GEM_ID, EQUALIZING_GEM_ID};
    private static final int[] RARE_GEMS
        = {CHAOTIC_GEM_ID, INVERTED_GEM_ID, COMPOUNDING_GEM_ID, FRACTAL_GEM_ID};

    abstract static class Gem {
        int id;
        String name;
        String description;

        abstract GemApplicationResult apply(Item item);

        int getId() { return id; }
        String getName() { return name; }
        String getDescription() { return description; }

        static Gem fromId(int id) {
            switch (id) {
                case FRACTURED_GEM_ID:
                    return new BasicGem(FRACTURED_GEM_ID, "Fractured Gem", "+0.2 x1, -0.1 x1, duplicate rolls permitted",
                            new int[]{2, -1});
                case PURE_GEM_ID:
                    return new BasicGem(PURE_GEM_ID, "Pure Gem", "+0.1 x1",
                            new int[]{1});
                case UNSTABLE_GEM_ID:
                    return new BasicGem(UNSTABLE_GEM_ID, "Unstable Gem", "+0.3 x1, -0.1 x2, duplicate rolls permitted",
                            new int[]{3, -1, -1});
                case BOLSTERING_GEM_ID:
                    return new BolsteringGem();
                case BALANCED_GEM_ID:
                    return new BalancedGem();
                default:
                    throw new IllegalArgumentException("Encountered unexpected gem id: " + id);
            }
        }

        static Gem getRandomGem(int rarity) {
            switch (rarity) {
                default:
                    System.out.println("Unexpected gem rarity encountered: " + rarity);
                case COMMON_GEM_RARITY:
                    return fromId(COMMON_GEMS[HBMain.RNG_SOURCE.nextInt(COMMON_GEMS.length)]);
                case UNCOMMON_GEM_RARITY:
                    return fromId(UNCOMMON_GEMS[HBMain.RNG_SOURCE.nextInt(UNCOMMON_GEMS.length)]);
                case RARE_GEM_RARITY:
                    return fromId(RARE_GEMS[HBMain.RNG_SOURCE.nextInt(RARE_GEMS.length)]);
            }
        }
    }

    private static class GemApplicationResult {
        AppliedGem result;
        List<String> output;

        GemApplicationResult(int gemType) {
            result = new AppliedGem(gemType);
            output = new ArrayList<>();
        }

        static String formatStatApplication(ITEM_STAT stat, int amount, boolean isTendency,
                String tendencyAdjective) {
            String result = stat.statName + (amount >= 0 ? " +" : " ")
                + Stats.oneDecimal.format(amount / 10.0);
            if (isTendency) {
                result += " (selected with " + tendencyAdjective + ")";
            }
            return result;
        }

        void addStat(ITEM_STAT stat, int amount) {
            addStat(stat, amount, false, "");
        }

        void addStat(StatRollResult roll, int amount) {
            addStat(roll.stat, amount, roll.isTendency, roll.tendencyAdjective);
        }

        void addStat(ITEM_STAT stat, int amount, boolean isTendency, String tendencyAdjective) {
            result.modifiedStats.addStat(stat, amount);
            output.add(formatStatApplication(stat, amount, isTendency, tendencyAdjective));
        }
    }

    private static class BasicGem extends Gem {
        int[] statChanges;

        BasicGem(int id, String name, String description, int[] statChanges) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.statChanges = statChanges;
        }

        GemApplicationResult apply(Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            for (int change : statChanges) {
                StatRollResult roll = item.rollStat(change > 0);
                applicationResult.addStat(roll, change);
            }
            return applicationResult;
        }
    }

    private static class BolsteringGem extends Gem {
        BolsteringGem() {
            this.id = BOLSTERING_GEM_ID;
            this.name = "Bolstering Gem";
            this.description = "+0.3 to lowest stat (ties broken randomly)";
        }

        GemApplicationResult apply(Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            List<ITEM_STAT> lowestStat = new ArrayList<>();
            double lowestValue = item.getModifier(ITEM_STAT.fromIndex(0));
            for (int i = 1; i < ITEM_STAT.values().length; ++i) {
                double value = item.getModifier(ITEM_STAT.fromIndex(i));
                if (value < lowestValue) {
                    lowestValue = value;
                    lowestStat.clear();
                    lowestStat.add(ITEM_STAT.fromIndex(i));
                } else if (value == lowestValue) {
                    lowestStat.add(ITEM_STAT.fromIndex(i));
                }
            }
            ITEM_STAT selectedStat;
            if (lowestStat.size() > 1) {
                selectedStat = lowestStat.get(HBMain.RNG_SOURCE.nextInt(lowestStat.size()));
            } else {
                selectedStat = lowestStat.get(0);
            }
            applicationResult.addStat(selectedStat, 3);
            return applicationResult;
        }
    }

    private static class BalancedGem extends Gem {
        BalancedGem() {
            this.id = BALANCED_GEM_ID;
            this.name = "Balanced Gem";
            this.description = "+0.2 x1, -0.2 x1, +0.1 x1, -0.1 x1 to unique stats";
        }

        GemApplicationResult apply(Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            // There's probably a more efficient implementation, but it was done
            // this way to include biases for tendencies
            Set<ITEM_STAT> selectedStats = new HashSet<>();
            StatRollResult roll;
            for (int change : new int[]{2, -2, 1, -1}) {
                boolean found = false;
                do {
                    roll = item.rollStat(change > 0);
                    if (!selectedStats.contains(roll.stat)) { found = true; }
                } while (!found);
                applicationResult.addStat(roll, change);
                selectedStats.add(roll.stat);
            }
            return applicationResult;
        }
    }

    private static class AppliedGem {
        private int gemType;
        private StatArray modifiedStats;

        // e.g. "Cursed Gem: Work +0.1, Fish -0.3"
        String getDescription() {
            StringBuilder builder = new StringBuilder();
            builder.append(Gem.fromId(gemType).getName());
            builder.append(": ");
            int loggedStats = 0;
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                if (modifiedStats.getStat(stat) == 0) { continue; }
                if (loggedStats > 0) { builder.append(", "); }
                builder.append(stat.getStatName());
                builder.append(modifiedStats.getStat(stat) > 0 ? " +" : " ");
                builder.append(Stats.oneDecimal.format(modifiedStats.getStat(stat) / 10.0));
                ++loggedStats;
            }
            if (loggedStats == 0) {
                builder.append("No stats changed");
            }
            return builder.toString();
        }

        AppliedGem(int gemType) {
            this.gemType = gemType;
            modifiedStats = new StatArray();
        }

        AppliedGem(int gemType, StatArray modifiedStats) {
            this.gemType = gemType;
            this.modifiedStats = modifiedStats;
        }
    }

    private static final int INITIAL_BONUS_INDEX = 0;
    private static final int GEM_BONUS_INDEX = 1;

    private abstract static class Item {
        double generatorVersion;
        long itemId;
        // { StatArray of inital stats, StatArray of gem stats }
        StatArray[] bonuses;
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

        abstract StatRollResult rollStat(boolean isPositive);
        abstract void addRandomStat(int amount, boolean initial);
        abstract void subtractRandomStat(int amount, boolean initial);

        abstract double getModifier(ITEM_STAT stat);
        abstract double getBonusModifierContribution(int baseModifier);
        abstract int getTier();

        String getPositiveTendencyAdjective() {
            return positiveTendency.getPositiveAdjective();
        }

        String getNegativeTendencyAdjective() {
            return negativeTendency.getNegativeAdjective();
        }

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

        List<String> applyGem(Gem gem) {
            GemApplicationResult applicationResult = gem.apply(this);
            gems.add(applicationResult.result);
            bonuses[GEM_BONUS_INDEX].addArray(applicationResult.result.modifiedStats);
            logAppliedGem(itemId, gem.id, applicationResult.result.modifiedStats);
            return applicationResult.output;
        }

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
                int initialBonus = bonuses[INITIAL_BONUS_INDEX].getStat(stat);
                int gemBonus = bonuses[GEM_BONUS_INDEX].getStat(stat);
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

        static Item getItem(double generatorVersion, long itemId, StatArray[] bonuses,
                ITEM_STAT positiveTendency, ITEM_STAT negativeTendency, ITEM_STAT bonusStat,
                int initialAdditions, int appliedAdditions, int initialSubtractions,
                int appliedSubtractiions, int enhancementLevel, int gemSlots) {
            // In the future use the generator version to pick the right item generation
            return new G0Item(generatorVersion, itemId, bonuses, positiveTendency,
                negativeTendency, bonusStat, initialAdditions, appliedAdditions,
                initialSubtractions, appliedSubtractiions, enhancementLevel, gemSlots);
        }
    }

    private static class StatRollResult {
        ITEM_STAT stat;
        boolean isTendency;
        String tendencyAdjective;

        StatRollResult(ITEM_STAT stat, boolean isTendency, String tendencyAdjective) {
            this.stat = stat;
            this.isTendency = isTendency;
            this.tendencyAdjective = tendencyAdjective;
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
            bonuses = new StatArray[] {
                    new StatArray(BASE_STAT_AMOUNT, BASE_STAT_AMOUNT, BASE_STAT_AMOUNT,
                        BASE_STAT_AMOUNT, BASE_STAT_AMOUNT),
                    new StatArray()
                };
            this.positiveTendency = positiveTendency;
            this.negativeTendency = negativeTendency;
            this.bonusStat        = bonusStat;
            this.gemSlots         = gemSlots;
        }

        G0Item(double generatorVersion, long itemId, StatArray[] bonuses, ITEM_STAT positiveTendency,
                ITEM_STAT negativeTendency, ITEM_STAT bonusStat, int initialAdditions,
                int appliedAdditions, int initialSubtractions, int appliedSubtractiions,
                int enhancementLevel, int gemSlots) {
            this.generatorVersion    = generatorVersion;
            this.itemId              = itemId;
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

        StatRollResult rollStat(boolean isPositive) {
            if (HBMain.RNG_SOURCE.nextDouble() < TENDENCY_CHANCE) {
                return new StatRollResult(isPositive ? positiveTendency : negativeTendency, true,
                    isPositive ? positiveTendency.positiveAdjective : negativeTendency.negativeAdjective);
            } else {
                return new StatRollResult(
                    ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(ITEM_STAT.values().length)), false,
                    isPositive ? positiveTendency.positiveAdjective : negativeTendency.negativeAdjective);
            }
        }

        void addRandomStat(int amount, boolean initial) {
            int index = initial ? INITIAL_BONUS_INDEX : GEM_BONUS_INDEX;
            StatRollResult rollResult = rollStat(true);
            bonuses[index].addStat(rollResult.stat, amount);
            if (initial) {
                initialAdditions += amount;
            } else {
                appliedAdditions += amount;
            }
        }

        void subtractRandomStat(int amount, boolean initial) {
            int index = initial ? INITIAL_BONUS_INDEX : GEM_BONUS_INDEX;
            StatRollResult rollResult = rollStat(false);
            bonuses[index].subtractStat(rollResult.stat, amount);
            if (initial) {
                initialSubtractions += amount;
            } else {
                appliedSubtractions += amount;
            }
        }

        double getModifier(ITEM_STAT stat) {
            int modifier = bonuses[INITIAL_BONUS_INDEX].getStat(stat)
                + bonuses[GEM_BONUS_INDEX].getStat(stat);
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

    static void logAppliedGem(long itemId, int gemId, StatArray stats) {

    }
}
