package com.c2t2s.hb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GachaGems {

    // Hide default constructor
    private GachaGems() { }

    private static final int FRACTURED_GEM_ID = 0;
    private static final int PURE_GEM_ID = 1;
    private static final int UNSTABLE_GEM_ID = 2;
    private static final int BOLSTERING_GEM_ID = 3;
    private static final int BALANCED_GEM_ID = 4;
    private static final int GAMBLERS_GEM_ID = 5;
    private static final int RECKLESS_GEM_ID = 6;
    private static final int HARDWORKING_GEM_ID = 7;
    private static final int PATIENT_GEM_ID = 8;
    private static final int SHADOWY_GEM_ID = 9;
    private static final int INTIMIDATING_GEM_ID = 10;
    private static final int VERSATILE_GEM_ID = 11;
    private static final int PUTRID_GEM_ID = 12;
    private static final int FORGETFUL_GEM_ID = 13;
    private static final int EQUALIZING_GEM_ID = 14;
    private static final int CHAOTIC_GEM_ID = 15;
    private static final int INVERTED_GEM_ID = 16;
    private static final int COMPOUNDING_GEM_ID = 17;
    private static final int FRACTAL_GEM_ID = 18;


    static final int COMMON_GEM_RARITY = 0;
    static final int UNCOMMON_GEM_RARITY = 1;
    static final int RARE_GEM_RARITY = 2;
    private static final int[] COMMON_GEMS
        = {FRACTURED_GEM_ID, PURE_GEM_ID, UNSTABLE_GEM_ID, BOLSTERING_GEM_ID, BALANCED_GEM_ID,
           GAMBLERS_GEM_ID, RECKLESS_GEM_ID};
    private static final int[] UNCOMMON_GEMS
        = {HARDWORKING_GEM_ID, PATIENT_GEM_ID, SHADOWY_GEM_ID,
           INTIMIDATING_GEM_ID, VERSATILE_GEM_ID, PUTRID_GEM_ID, FORGETFUL_GEM_ID,
           EQUALIZING_GEM_ID};
    private static final int[] RARE_GEMS
        = {CHAOTIC_GEM_ID, INVERTED_GEM_ID, COMPOUNDING_GEM_ID, FRACTAL_GEM_ID};

    abstract static class Gem {
        protected int id;
        protected String name;
        protected String description;

        abstract GemApplicationResult apply(GachaItems.Item item);

        int getId() { return id; }
        String getName() { return name; }
        String getDescription() { return description; }

        boolean isEligible(GachaItems.Item item) {
            return item.gems.size() < item.gemSlots;
        }

        String getIneligibilityReason(GachaItems.Item item) {
            if (item.gems.size() < item.gemSlots) {
                return "Selected item has no gem slots";
            }
            return "";
        }

        String getAwardMessage() {
            return ":gem: " + name + " (" + description + ")";
        }

        static Gem fromId(int id) {
            switch (id) {
                case FRACTURED_GEM_ID:
                    return new BasicGem(FRACTURED_GEM_ID, "Fractured Gem",
                        "+0.2 x1, -0.1 x1, duplicate rolls permitted", new int[]{2, -1});
                case PURE_GEM_ID:
                    return new BasicGem(PURE_GEM_ID, "Pure Gem", "+0.1 x1",
                            new int[]{1});
                case UNSTABLE_GEM_ID:
                    return new BasicGem(UNSTABLE_GEM_ID, "Unstable Gem",
                        "+0.3 x1, -0.1 x2, duplicate rolls permitted", new int[]{3, -1, -1});
                case BOLSTERING_GEM_ID:
                    return new BolsteringGem();
                case BALANCED_GEM_ID:
                    return new BalancedGem();
                case GAMBLERS_GEM_ID:
                    return new GamblersGem();
                case RECKLESS_GEM_ID:
                    return new RecklessGem();
                case HARDWORKING_GEM_ID:
                    return new TargetedGem(HARDWORKING_GEM_ID, "Hardworking Gem",
                        GachaItems.ITEM_STAT.WORK, GachaItems.ITEM_STAT.ROB);
                case PATIENT_GEM_ID:
                    return new TargetedGem(PATIENT_GEM_ID, "Patient Gem",
                        GachaItems.ITEM_STAT.FISH, GachaItems.ITEM_STAT.PICK);
                case SHADOWY_GEM_ID:
                    return new TargetedGem(SHADOWY_GEM_ID, "Shadowy Gem",
                        GachaItems.ITEM_STAT.PICK, GachaItems.ITEM_STAT.FISH);
                case INTIMIDATING_GEM_ID:
                    return new TargetedGem(INTIMIDATING_GEM_ID, "Intimidating Gem",
                        GachaItems.ITEM_STAT.ROB, GachaItems.ITEM_STAT.WORK);
                case VERSATILE_GEM_ID:
                    return new VersatileGem();
                case PUTRID_GEM_ID:
                    return new PutridGem();
                case FORGETFUL_GEM_ID:
                    return new ForgetfulGem();
                default:
                    throw new IllegalArgumentException("Encountered unexpected gem id: " + id);
            }
        }

        static Gem getRandomGem(int rarity) {
            switch (rarity) {
                case COMMON_GEM_RARITY:
                    return fromId(COMMON_GEMS[HBMain.RNG_SOURCE.nextInt(COMMON_GEMS.length)]);
                case UNCOMMON_GEM_RARITY:
                    return fromId(UNCOMMON_GEMS[HBMain.RNG_SOURCE.nextInt(UNCOMMON_GEMS.length)]);
                case RARE_GEM_RARITY:
                    return fromId(RARE_GEMS[HBMain.RNG_SOURCE.nextInt(RARE_GEMS.length)]);
                default:
                    throw new IllegalArgumentException("Unexpected gem rarity encountered: " + rarity);
            }
        }
    }

    static class GemApplicationResult {
        AppliedGem result;
        List<String> output;

        GemApplicationResult(int gemType) {
            result = new AppliedGem(gemType);
            output = new ArrayList<>();
        }

        static String formatStatApplication(GachaItems.ITEM_STAT stat, int amount,
                boolean isTendency, String tendencyAdjective) {
            String result = stat.getStatName() + (amount >= 0 ? " +" : " ")
                + Stats.oneDecimal.format(amount / 10.0);
            if (isTendency) {
                result += " (selected with " + tendencyAdjective + ")";
            }
            return result;
        }

        void addStat(GachaItems.ITEM_STAT stat, int amount) {
            addStat(stat, amount, false, "");
        }

        void addStat(GachaItems.StatRollResult roll, int amount) {
            addStat(roll.stat, amount, roll.isTendency, roll.tendencyAdjective);
        }

        void addStat(GachaItems.ITEM_STAT stat, int amount, boolean isTendency, String tendencyAdjective) {
            result.modifiedStats.addStat(stat, amount);
            if (amount > 0) {
                result.additions += amount;
            } else {
                result.subtractions += amount;
            }
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

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            for (int change : statChanges) {
                GachaItems.StatRollResult roll = item.rollStat(change > 0);
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

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            List<GachaItems.ITEM_STAT> lowestStat = new ArrayList<>();
            int lowestValue = item.getModifier(GachaItems.ITEM_STAT.fromIndex(0));
            for (int i = 1; i < GachaItems.ITEM_STAT.values().length; ++i) {
                int value = item.getModifier(GachaItems.ITEM_STAT.fromIndex(i));
                if (value < lowestValue) {
                    lowestValue = value;
                    lowestStat.clear();
                    lowestStat.add(GachaItems.ITEM_STAT.fromIndex(i));
                } else if (value == lowestValue) {
                    lowestStat.add(GachaItems.ITEM_STAT.fromIndex(i));
                }
            }
            GachaItems.ITEM_STAT selectedStat;
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

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            // There's probably a more efficient implementation, but it was done
            // this way to include biases for tendencies
            Set<GachaItems.ITEM_STAT> selectedStats = new HashSet<>();
            GachaItems.StatRollResult roll;
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

    private static class GamblersGem extends Gem {
        private static double jackpotChance = 0.2;

        GamblersGem() {
            id = GAMBLERS_GEM_ID;
            name = "Gambler's Gem";
            description = "20% chance of +0.4 to highest stat, -0.1 to highest stat otherwise";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            List<GachaItems.ITEM_STAT> highestStats = new ArrayList<>();
            int highestModifier = item.getModifier(GachaItems.ITEM_STAT.fromIndex(0));
            highestStats.add(GachaItems.ITEM_STAT.fromIndex(0));
            for (int i = 1; i < GachaItems.ITEM_STAT.values().length; ++i) {
                int modifier = item.getModifier(GachaItems.ITEM_STAT.fromIndex(i));
                if (modifier > highestModifier) {
                    highestModifier = modifier;
                    highestStats.clear();
                    highestStats.add(GachaItems.ITEM_STAT.fromIndex(i));
                } else if (modifier == highestModifier) {
                    highestStats.add(GachaItems.ITEM_STAT.fromIndex(i));
                }
            }
            GachaItems.ITEM_STAT selectedStat;
            if (highestStats.size() == 1) {
                selectedStat = highestStats.get(0);
            } else {
                selectedStat = highestStats.get(HBMain.RNG_SOURCE.nextInt(highestStats.size()));
            }
            if (HBMain.RNG_SOURCE.nextDouble() < jackpotChance) {
                applicationResult.addStat(selectedStat, 4);
            } else {
                applicationResult.addStat(selectedStat, -1);
            }
            return applicationResult;
        }
    }

    private static class RecklessGem extends Gem {
        RecklessGem() {
            id = RECKLESS_GEM_ID;
            name = "Reckless Gem";
            description = "Either +0.1 x1 or -0.1 x1 (50%/50% odds respectively)";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            boolean isPositive = HBMain.RNG_SOURCE.nextBoolean();
            GachaItems.StatRollResult roll = item.rollStat(isPositive);
            applicationResult.addStat(roll, 1);
            return applicationResult;
        }
    }

    private static class TargetedGem extends Gem {
        GachaItems.ITEM_STAT target;
        GachaItems.ITEM_STAT inverse;

        TargetedGem(int id, String name, GachaItems.ITEM_STAT target,
                GachaItems.ITEM_STAT inverse) {
            this.id = id;
            this.name = name;
            this.description = "+0.1 to " + target.getStatName() + ", -0.3 to "
                + inverse.getStatName();
            this.target = target;
            this.inverse = inverse;
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            applicationResult.addStat(target, 1);
            applicationResult.addStat(inverse, -3);
            return applicationResult;
        }
    }

    private static class VersatileGem extends Gem {
        VersatileGem() {
            id = VERSATILE_GEM_ID;
            name = "Versatile Gem";
            description = "+0.2 to Misc, -0.2 to all other stats";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            for (GachaItems.ITEM_STAT stat : GachaItems.ITEM_STAT.values()) {
                if (stat == GachaItems.ITEM_STAT.MISC) {
                    applicationResult.addStat(stat, 2);
                } else {
                    applicationResult.addStat(stat, -2);
                }
            }
            return applicationResult;
        }
    }

    private static class PutridGem extends Gem {
        PutridGem() {
            id = PUTRID_GEM_ID;
            name = "Putrid Gem";
            description = "-0.1 to all stats";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            for (GachaItems.ITEM_STAT stat : GachaItems.ITEM_STAT.values()) {
                applicationResult.addStat(stat, -1);
            }
            return applicationResult;
        }
    }

    private static class ForgetfulGem extends Gem {
        ForgetfulGem() {
            id = FORGETFUL_GEM_ID;
            name = "Forgetful Gem";
            description = "Negate all existing gem bonuses and refund their gem slots";
        }

        @Override
        boolean isEligible(GachaItems.Item item) {
            if (item.gems.isEmpty()) {
                return false;
            }
            return super.isEligible(item);
        }

        @Override
        String getIneligibilityReason(GachaItems.Item item) {
            if (item.gems.isEmpty()) {
                return "No gems to remove";
            }
            return super.getIneligibilityReason(item);
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            int removedGems = item.removeAllGems();
            applicationResult.output.add(removedGems + " gems removed");
            return applicationResult;
        }
    }

    static class AppliedGem {
        private int gemType;
        private GachaItems.StatArray modifiedStats;
        private int addedGemSlots;
        private int additions;
        private int subtractions;
        private int gemId;

        int getGemType() { return gemType; }
        GachaItems.StatArray getModifiedStats() { return modifiedStats; }
        int getAddedGemSlots() { return addedGemSlots; }
        int getAdditions() { return additions; }
        int getSubtractions() { return subtractions; }
        int getGemId() { return gemId; }

        // e.g. "Cursed Gem: Work +0.1, Fish -0.3"
        String getDescription() {
            StringBuilder builder = new StringBuilder();
            builder.append(Gem.fromId(gemType).getName());
            builder.append(": ");
            int loggedStats = 0;
            for (GachaItems.ITEM_STAT stat : GachaItems.ITEM_STAT.values()) {
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
            modifiedStats = new GachaItems.StatArray();
        }

        AppliedGem(int gemType, GachaItems.StatArray modifiedStats, int addedGemSlots,
                int additions, int subtractions, int gemId) {
            this.gemType = gemType;
            this.modifiedStats = modifiedStats;
            this.addedGemSlots = addedGemSlots;
            this.additions = additions;
            this.subtractions = subtractions;
            this.gemId = gemId;
        }
    }
}
