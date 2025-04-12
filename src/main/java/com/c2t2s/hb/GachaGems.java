package com.c2t2s.hb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final int GEM_OF_CHANCE_ID = 5;
    private static final int RECKLESS_GEM_ID = 6;
    private static final int HARDWORKING_GEM_ID = 7;
    private static final int PATIENT_GEM_ID = 8;
    private static final int SHADOWY_GEM_ID = 9;
    private static final int INTIMIDATING_GEM_ID = 10;
    private static final int VERSATILE_GEM_ID = 11;
    private static final int PUTRID_GEM_ID = 12;
    private static final int FORGETFUL_GEM_ID = 13; // No longer used
    private static final int EQUALIZING_GEM_ID = 14;
    private static final int CHAOTIC_GEM_ID = 15;
    private static final int INVERTED_GEM_ID = 16;
    private static final int COMPOUNDING_GEM_ID = 17;
    private static final int FRACTAL_GEM_ID = 18;
    private static final int GAMBLERS_GEM_ID = 19;

    static final int COMMON_GEM_RARITY = 0;
    static final int UNCOMMON_GEM_RARITY = 1;
    static final int RARE_GEM_RARITY = 2;
    private static final Integer[] COMMON_GEMS
        = {FRACTURED_GEM_ID, PURE_GEM_ID, UNSTABLE_GEM_ID, BOLSTERING_GEM_ID, BALANCED_GEM_ID,
           GEM_OF_CHANCE_ID, RECKLESS_GEM_ID, GAMBLERS_GEM_ID};
    private static final Integer[] UNCOMMON_GEMS
        = {HARDWORKING_GEM_ID, PATIENT_GEM_ID, SHADOWY_GEM_ID,
           INTIMIDATING_GEM_ID, VERSATILE_GEM_ID, PUTRID_GEM_ID, EQUALIZING_GEM_ID};
    private static final Integer[] RARE_GEMS
        = {CHAOTIC_GEM_ID, INVERTED_GEM_ID, COMPOUNDING_GEM_ID, FRACTAL_GEM_ID};
    static final Set<Integer> COMMON_GEM_SET = Set.of(COMMON_GEMS);
    static final Set<Integer> UNCOMMON_GEM_SET = Set.of(UNCOMMON_GEMS);
    static final Set<Integer> RARE_GEM_SET = Set.of(RARE_GEMS);

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
            if (!isEligible(item)) {
                return "Selected item has no empty gem slots";
            }
            return "";
        }

        String getAwardMessage() {
            return ":gem: " + name + " (" + description + ")";
        }

        static GachaItems.StatArray getInitialStats(GachaItems.Item item) {
            GachaItems.StatArray initialStats = new GachaItems.StatArray();
            for (GachaItems.StatArray statArray : item.bonuses) {
                for (GachaItems.ITEM_STAT stat : GachaItems.ITEM_STAT.values()) {
                    initialStats.addStat(stat, statArray.getStat(stat));
                }
            }
            return initialStats;
        }

        static GachaItems.ITEM_STAT getLowestStat(GachaItems.Item item) {
            GachaItems.StatArray initialStats = getInitialStats(item);
            List<GachaItems.ITEM_STAT> lowestStat = new ArrayList<>();
            int lowestValue = initialStats.getStat(GachaItems.ITEM_STAT.fromIndex(0));
            lowestStat.add(GachaItems.ITEM_STAT.fromIndex(0));
            for (int i = 1; i < GachaItems.ITEM_STAT.values().length; ++i) {
                int value = initialStats.getStat(GachaItems.ITEM_STAT.fromIndex(i));
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
            return selectedStat;
        }

        static GachaItems.ITEM_STAT getHighestStat(GachaItems.Item item) {
            List<GachaItems.ITEM_STAT> highestStats = new ArrayList<>();
            GachaItems.StatArray initialStats = getInitialStats(item);
            int highestModifier = initialStats.getStat(GachaItems.ITEM_STAT.fromIndex(0));
            highestStats.add(GachaItems.ITEM_STAT.fromIndex(0));
            for (int i = 1; i < GachaItems.ITEM_STAT.values().length; ++i) {
                int modifier = initialStats.getStat(GachaItems.ITEM_STAT.fromIndex(i));
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
            return selectedStat;
        }

        static Gem fromId(int id) throws IllegalArgumentException {
            switch (id) {
                case FRACTURED_GEM_ID:
                    return new BasicGem(FRACTURED_GEM_ID, "Fractured Gem",
                        "+2 to a stat and -1 to a stat, duplicate rolls permitted", new int[]{2, -1});
                case PURE_GEM_ID:
                    return new BasicGem(PURE_GEM_ID, "Pure Gem", "+1 to one random stat",
                            new int[]{1});
                case UNSTABLE_GEM_ID:
                    return new BasicGem(UNSTABLE_GEM_ID, "Unstable Gem",
                        "+3 to a random stat and -1 to two random stats, duplicate rolls permitted", new int[]{3, -1, -1});
                case BOLSTERING_GEM_ID:
                    return new BolsteringGem();
                case BALANCED_GEM_ID:
                    return new BalancedGem();
                case GEM_OF_CHANCE_ID:
                    return new GemOfChance();
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
                case EQUALIZING_GEM_ID:
                    return new EqualizingGem();
                case CHAOTIC_GEM_ID:
                    return new ChaoticGem();
                case INVERTED_GEM_ID:
                    return new InvertedGem();
                case COMPOUNDING_GEM_ID:
                    return new CompoundingGem();
                case FRACTAL_GEM_ID:
                    return new FractalGem();
                case GAMBLERS_GEM_ID:
                    return new GamblersGem();
                default:
                    throw new IllegalArgumentException("Encountered unexpected gem id: " + id);
            }
        }

        static Gem getRandomGem(int rarity) {
            switch (rarity) {
                default:
                    new IllegalArgumentException("Unexpected gem rarity encountered: " + rarity).printStackTrace();
                    // Fall through and give common gem
                case COMMON_GEM_RARITY:
                    return fromId(COMMON_GEMS[HBMain.RNG_SOURCE.nextInt(COMMON_GEMS.length)]);
                case UNCOMMON_GEM_RARITY:
                    return fromId(UNCOMMON_GEMS[HBMain.RNG_SOURCE.nextInt(UNCOMMON_GEMS.length)]);
                case RARE_GEM_RARITY:
                    return fromId(RARE_GEMS[HBMain.RNG_SOURCE.nextInt(RARE_GEMS.length)]);
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
            String result = stat.getStatName() + (amount >= 0 ? " +" : " ") + amount;
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
                result.subtractions += -1 * amount;
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
            this.description = "+3 to lowest stat (ties broken randomly)";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            applicationResult.addStat(getLowestStat(item), 3);
            return applicationResult;
        }
    }

    private static class BalancedGem extends Gem {
        BalancedGem() {
            this.id = BALANCED_GEM_ID;
            this.name = "Balanced Gem";
            this.description = "+2, +1, -1, and -2 to random unique stats";
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

    private static class GemOfChance extends Gem {
        private static double jackpotChance = 0.2;

        GemOfChance() {
            id = GEM_OF_CHANCE_ID;
            name = "Gem of Chance";
            description = "20% chance of +4 to highest stat, -1 to highest stat otherwise";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            GachaItems.ITEM_STAT selectedStat = getHighestStat(item);
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
            description = "Either +1 or -1 to a stat (50%/50% odds)";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            boolean isPositive = HBMain.RNG_SOURCE.nextBoolean();
            GachaItems.StatRollResult roll = item.rollStat(isPositive);
            applicationResult.addStat(roll, isPositive ? 1 : -1);
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
            this.description = "+1 to " + target.getStatName() + ", -3 to "
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
            description = "+2 to Misc, -2 to all other stats";
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
            description = "-1 to all stats";
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
            description = "Remove all previous applied gems (this one remains socketed)";
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
                return "Selected item has no gems to remove";
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

    private abstract static class StatShuffleGem extends Gem {
        GachaItems.StatArray initialStats = new GachaItems.StatArray();
        GachaItems.StatArray targetStats = new GachaItems.StatArray();

        abstract String getApplicationTendencyString(GachaItems.ITEM_STAT stat);

        GemApplicationResult applyToTarget() {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            for (GachaItems.ITEM_STAT stat : GachaItems.ITEM_STAT.values()) {
                String tendencyString = getApplicationTendencyString(stat);
                applicationResult.addStat(stat,
                    targetStats.getStat(stat) - initialStats.getStat(stat),
                    !tendencyString.isEmpty(), tendencyString);
            }
            return applicationResult;
        }
    }

    private static class EqualizingGem extends StatShuffleGem {
        GachaItems.StatRollResult firstStat;

        EqualizingGem() {
            id = EQUALIZING_GEM_ID;
            name = "Equalizing Gem";
            description = "Average all stat bonuses (ties broken randomly)";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            initialStats = getInitialStats(item);

            int totalStats = 0;
            for (GachaItems.ITEM_STAT stat : GachaItems.ITEM_STAT.values()) {
                totalStats += initialStats.getStat(stat);
            }

            int averageStat = totalStats / 5;
            int remainder = totalStats % 5;
            if (remainder < 0) {
                // totalStats was negative, adjust so remainder are added instead of subtracted
                averageStat += 1;
                remainder += 5;
            }
            targetStats
                = new GachaItems.StatArray(averageStat, averageStat, averageStat, averageStat,
                    averageStat);

            // Assign remaining stats randomly, considering tendency weight for first roll
            List<GachaItems.ITEM_STAT> remainingStats
                = new ArrayList<>(Arrays.asList(GachaItems.ITEM_STAT.values()));
            firstStat = item.rollStat(true);
            if (remainder > 0) {
                targetStats.addStat(firstStat.stat, 1);
                remainingStats.remove(firstStat.stat);
            }
            // Start at 1 as we've already awarded one above
            for (int i = 1; i < remainder; ++i) {
                int nextStat = HBMain.RNG_SOURCE.nextInt(remainingStats.size());
                targetStats.addStat(remainingStats.get(nextStat), 1);
                remainingStats.remove(nextStat);
            }

            // Now award enough stats to reach targets
            return applyToTarget();
        }

        @Override
        String getApplicationTendencyString(GachaItems.ITEM_STAT stat) {
            if (stat == firstStat.stat && firstStat.isTendency) {
                return stat.getPositiveAdjective();
            }
            return "";
        }
    }

    private static class ChaoticGem extends StatShuffleGem {
        GachaItems.StatRollResult highestStatRoll;
        GachaItems.StatRollResult lowestStatRoll;

        ChaoticGem() {
            id = CHAOTIC_GEM_ID;
            name = "Chaotic Gem";
            description = "Shuffle all stats";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            initialStats = getInitialStats(item);

            // Track the initial stats that still need a destination
            List<GachaItems.ITEM_STAT> remainingStats
                = new ArrayList<>(Arrays.asList(GachaItems.ITEM_STAT.values()));

            // Figure out highest and lowest so we can use weighted rolls for them
            int highestIndex = 0;
            int highest = initialStats.getStat(GachaItems.ITEM_STAT.fromIndex(highestIndex));
            int lowestIndex = highestIndex;
            int lowest = highest;
            for (int i = 1; i < GachaItems.ITEM_STAT.values().length; ++i) {
                int value = initialStats.getStat(GachaItems.ITEM_STAT.fromIndex(i));
                if (value < lowest) {
                    lowest = value;
                    lowestIndex = i;
                } else if (value > highest) {
                    highest = value;
                    highestIndex = i;
                }
            }

            // Award highest and lowest using tendency weighted rolls
            GachaItems.ITEM_STAT highestStat = GachaItems.ITEM_STAT.fromIndex(highestIndex);
            highestStatRoll = item.rollStat(true);
            targetStats.setStat(highestStatRoll.stat, initialStats.getStat(highestStat));
            remainingStats.remove(highestStat);
            GachaItems.ITEM_STAT lowestStat = GachaItems.ITEM_STAT.fromIndex(lowestIndex);
            GachaItems.StatRollResult roll = item.rollStat(false);
            if (roll.stat != highestStatRoll.stat) {
                lowestStatRoll = roll;
                targetStats.setStat(lowestStatRoll.stat, initialStats.getStat(lowestStat));
                remainingStats.remove(lowestStat);
            }

            // Now shuffle and award the 3 remaining stats without tendencies
            Collections.shuffle(remainingStats);
            int index = 0;
            for (GachaItems.ITEM_STAT stat : GachaItems.ITEM_STAT.values()) {
                if (targetStats.getStat(stat) != 0) {
                    // This was awarded the previous high or low
                    continue;
                }
                targetStats.setStat(stat, initialStats.getStat(remainingStats.get(index)));
                index++;
            }

            return applyToTarget();
        }

        @Override
        String getApplicationTendencyString(GachaItems.ITEM_STAT stat) {
            if (stat == highestStatRoll.stat && highestStatRoll.isTendency) {
                return stat.getPositiveAdjective();
            } else if (lowestStatRoll != null && stat == lowestStatRoll.stat
                    && lowestStatRoll.isTendency) {
                return stat.getNegativeAdjective();
            }
            return "";
        }
    }

    private static class InvertedGem extends StatShuffleGem {
        InvertedGem() {
            id = INVERTED_GEM_ID;
            name = "Inverted Gem";
            description = "Invert all bonuses";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            initialStats = getInitialStats(item);
            for (GachaItems.ITEM_STAT stat : GachaItems.ITEM_STAT.values()) {
                targetStats.setStat(stat, -1 * initialStats.getStat(stat));
            }
            return applyToTarget();
        }

        @Override
        String getApplicationTendencyString(GachaItems.ITEM_STAT stat) {
            return "";
        }
    }

    private static class CompoundingGem extends Gem {
        CompoundingGem() {
            id = COMPOUNDING_GEM_ID;
            name = "Compounding Gem";
            description = "+2 to highest stat (ties broken randomly)";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            applicationResult.addStat(getHighestStat(item), 2);
            return applicationResult;
        }
    }

    private static class FractalGem extends Gem {
        FractalGem() {
            id = FRACTAL_GEM_ID;
            name = "Fractal Gem";
            description = "Grants +3 Gem Slots once socketed";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            GemApplicationResult applicationResult = new GemApplicationResult(id);
            applicationResult.result.addedGemSlots = 3;
            applicationResult.output.add("Added 3 Gem Slots");
            return applicationResult;
        }
    }

    private static class GamblersGem extends Gem {
        GamblersGem() {
            id = GAMBLERS_GEM_ID;
            name = "Gambler's Gem";
            description = "Mimics another random gem";
        }

        @Override
        GemApplicationResult apply(GachaItems.Item item) {
            int totalGems = COMMON_GEMS.length + UNCOMMON_GEMS.length + RARE_GEMS.length;
            Gem mimickedGem = Gem.fromId(HBMain.RNG_SOURCE.nextInt(totalGems));
            GemApplicationResult applicationResult = mimickedGem.apply(item);

            // Update the stored gem id to this
            applicationResult.result.gemId = id;

            // Add a new prefix to each output line
            applicationResult.output.add(0, "Mimicked " + mimickedGem.name + ": "
                + mimickedGem.description);

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

        // e.g. "Cursed Gem: Work +1, Fish -3"
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
                builder.append(modifiedStats.getStat(stat));
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
