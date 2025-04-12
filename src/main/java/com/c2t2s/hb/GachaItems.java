package com.c2t2s.hb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class GachaItems {

    // Hide default constructor
    private GachaItems() { }

    private static final int WORK_STAT_INDEX = 0;
    private static final int FISH_STAT_INDEX = 1;
    private static final int PICK_STAT_INDEX = 2;
    private static final int ROB_STAT_INDEX  = 3;
    private static final int MISC_STAT_INDEX = 4;

    enum ITEM_STAT {
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

        static String format(int amount) {
            return (amount >= 0 ? "+" : "") + amount;
        }
    }

    // This could be adjusted to be doubles in more
    //  granular control if wanted in the future
    static class StatArray {
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

        StatArray addArray(StatArray array) {
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                addStat(stat, array.getStat(stat));
            }
            return this;
        }

        @Override
        public String toString() {
            // e.g. [-1W +4F +11P +2R -5M]
            StringBuilder builder = new StringBuilder();
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                builder.append(builder.length() == 0 ? '[' : ' ');
                if (stats[stat.index] > 0) { builder.append('+'); }
                builder.append(stats[stat.index]);
                builder.append(stat.getLetter());
            }
            builder.append(']');
            return builder.toString();
        }

        String formatForDB() {
            StringBuilder builder = new StringBuilder();
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                if (builder.length() != 0) {
                    builder.append(", ");
                }
                builder.append(stats[stat.index]);
            }
            return builder.toString();
        }

        String printStat(ITEM_STAT stat) {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            if (stats[stat.index] > 0) { builder.append('+'); }
            builder.append(stats[stat.index]);
            builder.append(stat.getLetter());
            builder.append(']');
            return builder.toString();
        }
    }

    private static final int INITIAL_BONUS_INDEX = 0;
    private static final int GEM_BONUS_INDEX = 1;

    abstract static class Item {
        int generatorVersion;
        long itemId;
        long itemUid;
        // { StatArray of inital stats, StatArray of gem stats }
        StatArray[] bonuses;
        List<GachaGems.AppliedGem> gems;
        // Indicates gem stats are included in the item's base stats to save DB fetches
        int abstractedGems = 0;

        // Stat selected is more likely to be rolled when adding stats
        ITEM_STAT positiveTendency;
        // Stat selected is more likely to be rolled when subtracting stats
        ITEM_STAT negativeTendency;
        ITEM_STAT bonusStat;

        int additions = 0;
        int subtractions = 0;

        int gemSlots;
        int enhancementLevel = 0;

        abstract StatRollResult rollStat(boolean isPositive);
        abstract void addRandomStat(int amount, boolean initial);
        abstract void subtractRandomStat(int amount, boolean initial);

        abstract int getModifier(ITEM_STAT stat);
        abstract StatArray getModifiers();
        abstract double getBonusModifier();
        abstract int getBonusModifierContribution(int baseModifier);
        abstract int getTier();
        abstract String getName();

        private static final String ITEM_ID_SEPARATOR = "|";

        long getItemId() { return itemId; }

        String getItemIdString() { return itemId + ITEM_ID_SEPARATOR + getName(); }

        static long parseItemIdString(String itemIdString) {
            try {
                int index = itemIdString.indexOf(ITEM_ID_SEPARATOR);
                return Long.parseLong(itemIdString.substring(0, index));
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                System.out.println("Failed to parse item id string: " + itemIdString);
                return -1;
            }
        }

        long getItemUid() { return itemUid; }

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

        StatArray getGemModifiers() {
            return bonuses[GEM_BONUS_INDEX];
        }

        StatArray getBaseModifiers() {
            return bonuses[INITIAL_BONUS_INDEX];
        }

        List<String> applyGem(GachaGems.Gem gem) {
            GachaGems.GemApplicationResult applicationResult = gem.apply(this);
            gems.add(applicationResult.result);
            bonuses[GEM_BONUS_INDEX].addArray(applicationResult.result.getModifiedStats());
            additions += applicationResult.result.getAdditions();
            subtractions += applicationResult.result.getSubtractions();
            gemSlots += applicationResult.result.getAddedGemSlots();
            logAppliedGem(itemUid, itemId, gem.id, applicationResult.result.getModifiedStats(),
                applicationResult.result.getAdditions(),
                applicationResult.result.getSubtractions(),
                applicationResult.result.getAddedGemSlots());
            return applicationResult.output;
        }

        int getRemainingGemSlots() {
            return gemSlots - gems.size();
        }

        int removeAllGems() {
            int removedGems = gems.size();
            for (GachaGems.AppliedGem gem : gems) {
                logGemRemoval(gem.getGemId());
            }
            gems.clear();
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                // This won't be exactly right, but it'll accurately
                // calculate the item's tier. The DB will contain
                // correct values
                additions -= bonuses[GEM_BONUS_INDEX].getStat(stat);
            }
            bonuses[GEM_BONUS_INDEX] = new StatArray();
            return removedGems;
        }

        public String getAutoCompleteDescription() {
            return getModifiers().toString() + " " + getName();
        }

        public String getBriefDescription() {
            return getName() + "\n\t" + getModifiers().toString() + "\n\t" + gemSlots + " gem slots";
        }

        public String getItemListDescription() {
            return getName() + "\n\t" + getModifiers().toString() + " "
                + getAppliedGemCount() + "/" + gemSlots + " gems";
        }

        public int getAppliedGemCount() {
            return Math.max(gems.size(), abstractedGems);
        }

        public String getFullDescription() {
            StringBuilder builder = new StringBuilder();
            builder.append(getName());
            builder.append("\n\n");
            builder.append(positiveTendency.getPositiveAdjective());
            builder.append(": Randomly rolled positive stats are more likely to go into ");
            builder.append(positiveTendency.getStatName());
            builder.append('\n');
            builder.append(negativeTendency.getNegativeAdjective());
            builder.append(": Randomly rolled negative stats are more likely to go into ");
            builder.append(negativeTendency.getStatName());
            builder.append('\n');
            builder.append(bonusStat.getItemName(enhancementLevel));
            builder.append(": x");
            builder.append(Stats.twoDecimals.format(getBonusModifier() + 1));
            builder.append(" to ");
            builder.append(bonusStat.getStatName());
            builder.append(" (if positive)\n");
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                int initialBonus = bonuses[INITIAL_BONUS_INDEX].getStat(stat);
                int gemBonus = bonuses[GEM_BONUS_INDEX].getStat(stat);
                builder.append('\n');
                builder.append(stat.getStatName());
                builder.append(": ");
                builder.append(ITEM_STAT.format(getModifier(stat)));
                builder.append(" (");
                builder.append(ITEM_STAT.format(initialBonus));
                builder.append(" Base");
                if (gemBonus != 0) {
                    builder.append(", ");
                    builder.append(ITEM_STAT.format(gemBonus));
                    builder.append(" from Gems");
                }
                if (stat == bonusStat) {
                    int bonusContribution = getBonusModifierContribution(initialBonus + gemBonus);
                    if (bonusContribution != 0) {
                        builder.append(", ");
                        builder.append(ITEM_STAT.format(bonusContribution));
                        builder.append(" from Item Type");
                    }
                }
                builder.append(')');
            }
            builder.append("\n\nGems:");
            for (GachaGems.AppliedGem gem : gems) {
                builder.append("\n  ");
                builder.append(gem.getDescription());
            }
            for (int i = 0; i < gemSlots - gems.size(); ++i) {
                builder.append("\n  Empty Gem Slot");
            }
            return builder.toString();
        }

        void awardTo(long uid) {
            logAwardItem(uid, this, bonuses[INITIAL_BONUS_INDEX]);
        }

        static Item getItem(FetchedItem item, List<GachaGems.AppliedGem> gems) {
            StatArray gemBonuses = new StatArray();
            int appliedGemSlots = 0;

            for (GachaGems.AppliedGem gem : gems) {
                gemBonuses.addArray(gem.getModifiedStats());
                item.additions += gem.getAdditions();
                item.subtractions += gem.getSubtractions();
                appliedGemSlots += gem.getAddedGemSlots();
            }

            StatArray[] bonuses = { item.stats, gemBonuses };

            // In the future use the generator version to pick the right item generation
            return new G0Item(item.generator, item.uid, item.iid, bonuses, gems,
                ITEM_STAT.fromIndex(item.positiveTendency),
                ITEM_STAT.fromIndex(item.negativeTendency), ITEM_STAT.fromIndex(item.bonusStat),
                item.additions, item.subtractions, item.enhancementLevel,
                item.gemSlots + appliedGemSlots);
        }

        static Item getItem(FetchedItem item, StatArray gemStats, int gemAdditions,
                int gemSubtractions, int slotsFromGems, int gemCount) {
            StatArray[] bonuses = { item.stats, gemStats };

            return new G0Item(item.generator, item.uid, item.iid, bonuses, new ArrayList<>(),
                ITEM_STAT.fromIndex(item.positiveTendency),
                ITEM_STAT.fromIndex(item.negativeTendency), ITEM_STAT.fromIndex(item.bonusStat),
                item.additions + gemAdditions, item.subtractions + gemSubtractions,
                item.enhancementLevel, item.gemSlots + slotsFromGems, gemCount);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Item)) {
                return false;
            }
            return ((Item)other).itemId == itemId;
        }

        @Override
        public int hashCode() {
            return (int)itemId;
        }
    }

    static class StatRollResult {
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

        private static final int GENERATOR_VERSION = 4; // 0.04
        private static final double LN_ONE_HALF = Math.log(0.5);
        private static final int AVERAGE_ADDITIONS = 50;
        private static final int AVERAGE_SUBTRACTIONS = 50;
        private static final int ADDITIONS_STDDEV = 25;
        private static final int BASE_STAT_AMOUNT = 10;
        private static final int BASE_GEM_SLOTS = 2;
        private static final double TENDENCY_CHANCE = 0.1;
        private static final double UNENHANCED_BONUS_STAT_MODIFIER = 0.25;
        private static final double ENHANCED_BONUS_STAT_MODIFIER = 0.5;

        private static int getActivityCount(Casino.User user, ITEM_STAT stat) {
            switch (stat) {
                case WORK:
                    return user.getWork();
                case FISH:
                    return user.getFish();
                case PICK:
                    return user.getPick();
                case ROB:
                    return user.getRob();
                case MISC:
                    // Average of all
                    return (user.getWork() + user.getFish() + user.getPick() + user.getRob()) / 4;
                default:
                    return 0;
                }
        }

        static G0Item generate(long uid) {
            // Repeating 50% chance per additional gem slots
            Double extraGemSlots = Math.log(HBMain.RNG_SOURCE.nextDouble()) / LN_ONE_HALF;
            int gemSlots = BASE_GEM_SLOTS + extraGemSlots.intValue();

            ITEM_STAT positiveTendency = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));
            ITEM_STAT negativeTendency = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));
            ITEM_STAT bonusStat        = ITEM_STAT.fromIndex(HBMain.RNG_SOURCE.nextInt(5));

            Casino.User user = Casino.getUser(uid);
            int enhancementLevel = 0;
            if (user != null) {
                // Chance of an enhanced item is 10% * log(base 4)(1 + # of participations in relevant activity)
                double enhancementChance = 0.1 * Math.log1p(getActivityCount(user, bonusStat)) / Math.log(4.0);
                if (HBMain.RNG_SOURCE.nextDouble() < enhancementChance) {
                    enhancementLevel = 1;
                }
            }

            G0Item item = new G0Item(positiveTendency, negativeTendency, bonusStat, gemSlots, enhancementLevel);

            // Roll for initial additions and subtractions
            int additions    = BASE_STAT_AMOUNT + HBMain.generateBoundedNormal(AVERAGE_ADDITIONS, ADDITIONS_STDDEV, 0);
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
                int gemSlots, int enhancmentLevel) {
            generatorVersion = GENERATOR_VERSION;
            bonuses = new StatArray[] { new StatArray(), new StatArray() };
            this.gems             = new ArrayList<>();
            this.positiveTendency = positiveTendency;
            this.negativeTendency = negativeTendency;
            this.bonusStat        = bonusStat;
            this.gemSlots         = gemSlots;
            this.enhancementLevel = enhancmentLevel;
        }

        G0Item(int generatorVersion, long itemUid, long itemId, StatArray[] bonuses,
                List<GachaGems.AppliedGem> gems, ITEM_STAT positiveTendency,
                ITEM_STAT negativeTendency, ITEM_STAT bonusStat, int additions,
                int subtractions, int enhancementLevel, int gemSlots) {
            this(generatorVersion, itemUid, itemId, bonuses, gems, positiveTendency, negativeTendency,
                bonusStat, additions, subtractions, enhancementLevel, gemSlots, 0);
        }

        G0Item(int generatorVersion, long itemUid, long itemId, StatArray[] bonuses,
                List<GachaGems.AppliedGem> gems, ITEM_STAT positiveTendency,
                ITEM_STAT negativeTendency, ITEM_STAT bonusStat, int additions,
                int subtractions, int enhancementLevel, int gemSlots, int abstractedGems) {
            this.generatorVersion = generatorVersion;
            this.itemUid          = itemUid;
            this.itemId           = itemId;
            this.bonuses          = bonuses;
            this.gems             = gems;
            this.positiveTendency = positiveTendency;
            this.negativeTendency = negativeTendency;
            this.bonusStat        = bonusStat;
            this.additions        = additions;
            this.subtractions     = subtractions;
            this.enhancementLevel = enhancementLevel;
            this.gemSlots         = gemSlots;
            this.abstractedGems   = abstractedGems;
        }

        @Override
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

        @Override
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

        @Override
        void addRandomStat(int amount, boolean initial) {
            int index = initial ? INITIAL_BONUS_INDEX : GEM_BONUS_INDEX;
            StatRollResult rollResult = rollStat(true);
            bonuses[index].addStat(rollResult.stat, amount);
            additions += amount;
        }

        @Override
        void subtractRandomStat(int amount, boolean initial) {
            int index = initial ? INITIAL_BONUS_INDEX : GEM_BONUS_INDEX;
            StatRollResult rollResult = rollStat(false);
            bonuses[index].subtractStat(rollResult.stat, amount);
            subtractions += amount;
        }

        @Override
        int getModifier(ITEM_STAT stat) {
            int modifier = bonuses[INITIAL_BONUS_INDEX].getStat(stat)
                + bonuses[GEM_BONUS_INDEX].getStat(stat);
            return modifier + (stat == bonusStat ? getBonusModifierContribution(modifier) : 0);
        }

        @Override
        StatArray getModifiers() {
            return new StatArray(getModifier(ITEM_STAT.fromIndex(0)),
                getModifier(ITEM_STAT.fromIndex(1)), getModifier(ITEM_STAT.fromIndex(2)),
                getModifier(ITEM_STAT.fromIndex(3)), getModifier(ITEM_STAT.fromIndex(4)));
        }

        @Override
        double getBonusModifier() {
            if (enhancementLevel > 0) {
                return ENHANCED_BONUS_STAT_MODIFIER;
            } else {
                return UNENHANCED_BONUS_STAT_MODIFIER;
            }
        }

        @Override
        int getBonusModifierContribution(int baseModifier) {
            if (baseModifier <= 0) { return 0; }
            Double contribution = baseModifier * getBonusModifier();
            return contribution.intValue();
        }

        @Override
        int getTier() {
            return (additions - subtractions) / 10;
        }
    }

    static Item generateItem(long uid) {
        return G0Item.generate(uid);
    }

    static String handleItemInfo(long uid, String iidString) {
        long iid = Item.parseItemIdString(iidString);
        if (iid < 0) {
            return "Unable to parse item id";
        }
        Item item = fetchItem(uid, iid);
        if (item == null) {
            return "Failed to fetch item " + iid;
        }
        return item.getFullDescription();
    }

    static String handleItemList(long uid) {
        Map<Item, String> items = fetchItemList(uid);
        if (items.isEmpty()) {
            return "No items found";
        } else if (items.containsKey(null)) {
            return "Unable to parse items";
        }

        StringBuilder output = new StringBuilder("Your items:");
        for (Entry<Item, String> entry : items.entrySet()) {
            output.append("\n");
            output.append(entry.getKey().getItemListDescription());
            if (!entry.getValue().isEmpty()) {
                output.append("\n\tEquipped by ");
                output.append(entry.getValue());
            }
        }
        return output.toString();
    }

    static String handleAwardGem(long uid, GachaGems.Gem gem) {
        if (logAwardGem(uid, gem.getId())) {
            return gem.getAwardMessage();
        }
        return "Failed to award gem " + gem.getId();
    }

    static HBMain.MultistepResponse handleApplyGem(long uid, String gemIdString, String iidString) {
        List<String> results = new ArrayList<>();
        GachaGems.Gem gem;
        int gemId = UnappliedGem.parseGemIdString(gemIdString);
        if (gemId < 0) {
            results.add("Unable to parse gem id");
            return new HBMain.MultistepResponse(results);
        }
        try {
             gem = GachaGems.Gem.fromId(gemId);
        } catch (IllegalArgumentException e) {
            results.add("Unable to apply gem: Unable to fetch gem " + gemId);
            return new HBMain.MultistepResponse(results);
        }
        int quantity = fetchGemQuantity(uid, gem.getId());
        if (quantity < 1) {
            results.add("Unable to apply gem: You don't have any " + gem.getName());
            return new HBMain.MultistepResponse(results);
        }
        long iid = Item.parseItemIdString(iidString);
        if (iid < 0) {
            results.add("Unable to parse item id");
            return new HBMain.MultistepResponse(results);
        }
        Item item = fetchItem(uid, iid);
        if (item == null) {
            results.add("Unable to apply gem: Item " + iid + " not found");
            return new HBMain.MultistepResponse(results);
        }

        if (!gem.isEligible(item)) {
            results.add(gem.getIneligibilityReason(item));
            return new HBMain.MultistepResponse(results);
        }

        String oldStats = item.getModifiers().toString();
        int oldTier = item.getTier();
        String oldName = item.getName();
        List<String> steps = item.applyGem(gem);
        logGemConsumed(uid, gem.getId());
        if (!oldName.equals(item.getName())) {
            logName(item);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Applying ");
        builder.append(gem.getName());
        builder.append(':');
        results.add(builder.toString());
        for (String step : steps) {
            builder.append("\n  ");
            builder.append(step);
            results.add(builder.toString());
        }
        builder.append("\n\nResult:\n");
        builder.append(oldStats);
        builder.append(" -> ");
        builder.append(item.getModifiers().toString());
        if (oldTier != item.getTier()) {
            builder.append("\nTier ");
            builder.append(oldTier);
            builder.append(" -> ");
            builder.append(item.getTier());
        }
        builder.append('\n');
        builder.append(item.getRemainingGemSlots());
        builder.append(" gem slots remaining");

        results.add(builder.toString());

        return new HBMain.MultistepResponse(results);
    }

    private static final int MINIMUM_GEM_REMOVAL_COST = 1000;
    private static final int GEM_REMOVAL_COST_PER_TIER = 1000;

    private static int getGemRemovalCost(Item item) {
        return Math.max(MINIMUM_GEM_REMOVAL_COST, GEM_REMOVAL_COST_PER_TIER * item.getTier());
    }

    static HBMain.SingleResponse handleGemRemoveInitial(long uid, String iidString) {
        long iid = Item.parseItemIdString(iidString);
        if (iid < 0) {
            return new HBMain.SingleResponse("Unable to parse item id");
        }
        Item item = fetchItem(uid, iid);
        if (item == null) {
            return new HBMain.SingleResponse("Unable to remove gems: Item " + iid + " not found");
        }
        int gemCount = item.getAppliedGemCount();
        if (gemCount < 1) {
            return new HBMain.SingleResponse("Unable to remove gems: Specified item has no gems");
        }

        int cost = getGemRemovalCost(item);
        StringBuilder builder = new StringBuilder();
        builder.append("Removing ").append(gemCount).append(" gem")
            .append(Casino.getPluralSuffix(gemCount)).append(" from ").append(item.getName())
            .append(" will cost ").append(cost).append(" coins\nThe stats will change from ")
            .append(item.getModifiers()).append(" to ").append(item.getBaseModifiers());
        return new HBMain.SingleResponse(builder.toString(),
            ButtonRows.makeRemoveGem(cost, iidString));
    }

    static String handleGemRemoveFollowup(long uid, String iidString) {
        long iid = Item.parseItemIdString(iidString);
        if (iid < 0) {
            return "Unable to parse item id";
        }
        Item item = fetchItem(uid, iid);
        if (item == null) {
            return "Unable to remove gems: Item " + iid + " not found";
        } else if (item.getAppliedGemCount() < 1) {
            return "Unable to remove gems: Specified item has no gems";
        }

        int cost = getGemRemovalCost(item);
        long balance = Casino.checkBalance(uid);
        if (balance < 0) {
            return "Unable to remove gems: Balance check failed or was negative (" + balance +")";
        } else if (balance < cost) {
            return "Unbale to remove gems: Your current balance of " + balance
                + " is not enough to cover that";
        }

        balance = Casino.takeMoney(uid, cost);
        StringBuilder builder = new StringBuilder();
        builder.append("Spent ").append(cost).append(" coins to remove gems:\n\n")
            .append(item.getItemListDescription()).append("\nIs now:\n");
        item.removeAllGems();
        // RemoveAllGems doesn't perfectly update the stats (per a comment)
        // but it does update the db
        item = fetchItem(uid, iid);
        if (item == null) {
            return "Removed gems, but encountered a DB error formatting output";
        }
        builder.append(item.getItemListDescription()).append("\n\nYour balance is now ")
            .append(balance);
        return builder.toString();
    }

    static String handleGemRemoveCancel() {
        return "Successfully did nothing";
    }

    static List<HBMain.AutocompleteStringOption> handleItemAutocomplete(long uid, String partialName) {
        List<Item> items;
        if (partialName.isEmpty()) {
            items = fetchItems(uid);
        } else {
            items = fetchItems(uid, partialName);
        }
        if (items == null) { return new ArrayList<>(); }

        List<HBMain.AutocompleteStringOption> output = new ArrayList<>(items.size());
        items.forEach(i -> output.add(new HBMain.AutocompleteStringOption(
            i.getItemIdString(), i.getAutoCompleteDescription())));
        return output;
    }

    private static class UnappliedGem {
        int gemId;
        int quantityOwned;
        String name;

        UnappliedGem(int gemId, int quantityOwned, String name) {
            this.gemId = gemId;
            this.quantityOwned = quantityOwned;
            this.name = name;
        }

        int getGemId() { return gemId; }

        private static final String GEM_ID_SEPARATOR = "|";

        String getGemIdString() {
            return gemId + GEM_ID_SEPARATOR + name;
        }

        static int parseGemIdString(String idString) {
            try {
                int index = idString.indexOf(GEM_ID_SEPARATOR);
                return Integer.parseInt(idString.substring(0, index));
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                System.out.println("Failed to parse gem id string: " + idString);
                return -1;
            }
        }

        @Override
        public String toString() {
            GachaGems.Gem gem = GachaGems.Gem.fromId(gemId);
            StringBuilder builder = new StringBuilder();
            builder.append(gem.getName());
            builder.append(" (");
            builder.append(quantityOwned);
            builder.append(" owned): ");
            builder.append(gem.getDescription());
            return builder.toString();
        }
    }

    static List<HBMain.AutocompleteStringOption> handleGemAutocomplete(long uid, String substring) {
        List<UnappliedGem> gems = queryGems(uid, substring);
        if (gems == null) { return new ArrayList<>(); }

        List<HBMain.AutocompleteStringOption> output = new ArrayList<>(gems.size());
        gems.forEach(g -> output.add(new HBMain.AutocompleteStringOption(g.getGemIdString(), g.toString())));
        return output;
    }

    static String handleListGems(long uid) {
        List<UnappliedGem> gems = queryGems(uid);
        if (gems.isEmpty()) {
            return "You currently have no gems";
        }
        StringBuilder commonGems = new StringBuilder();
        StringBuilder uncommonGems = new StringBuilder();
        StringBuilder rareGems = new StringBuilder();
        for (UnappliedGem gem : gems) {
            String line = "\n  " + gem.toString();
            if (GachaGems.RARE_GEM_SET.contains(gem.getGemId())) {
                rareGems.append(line);
            } else if (GachaGems.UNCOMMON_GEM_SET.contains(gem.getGemId())) {
                uncommonGems.append(line);
            } else {
                commonGems.append(line);
            }
        }
        StringBuilder output = new StringBuilder();
        if (commonGems.length() != 0) {
            output.append("Common Gems:");
            output.append(commonGems.toString());
        }
        if (uncommonGems.length() != 0) {
            if (output.length() != 0) {
                output.append("\n\n");
            }
            output.append("Uncommon Gems:");
            output.append(uncommonGems.toString());
        }
        if (rareGems.length() != 0) {
            if (output.length() != 0) {
                output.append("\n\n");
            }
            output.append("Rare Gems:");
            output.append(rareGems.toString());
        }
        return output.toString();
    }

    static HBMain.MultistepResponse handleRerollItems(long uid, String item1String,
                                                      String item2String, String item3String) {
        long iid1 = Item.parseItemIdString(item1String);
        long iid2 = Item.parseItemIdString(item2String);
        long iid3 = Item.parseItemIdString(item3String);
        if (iid1 < 0 || iid2 < 0 || iid3 < 0) {
            return new HBMain.MultistepResponse(Arrays.asList(
                "Unable to reroll items: Failed to parse item id(s)"));
        } else if (iid1 == iid2 || iid1 == iid3 || iid2 == iid3) {
            return new HBMain.MultistepResponse(Arrays.asList(
                "Unable to reroll items: The same item was provided multiple times"
            ));
        }
        List<Item> items = fetchItems(uid, "", Arrays.asList(iid1, iid2, iid3));
        if (items == null || items.size() != 3) {
            return new HBMain.MultistepResponse(Arrays.asList(
                "Unable to reroll items: Unable to fetch items"));
        }
        Item item1 = items.get(0);
        Item item2 = items.get(1);
        Item item3 = items.get(2);
        if (item1 == null || item2 == null || item3 == null) {
            return new HBMain.MultistepResponse(Arrays.asList(
                "Unable to reroll items: Item(s) not found"));
        }

        ITEM_STAT positiveTendency = null;
        ITEM_STAT negativeTendency = null;
        ITEM_STAT bonusStat = null;
        int enhancementLevel = -1;
        if (item1.positiveTendency == item2.positiveTendency
                && item1.positiveTendency == item3.positiveTendency) {
            positiveTendency = item1.positiveTendency;
        }
        if (item1.negativeTendency == item2.negativeTendency
                && item1.negativeTendency == item3.negativeTendency) {
            negativeTendency = item1.negativeTendency;
        }
        if (item1.bonusStat == item2.bonusStat && item1.bonusStat == item3.bonusStat
                && item1.enhancementLevel == item2.enhancementLevel
                && item1.enhancementLevel == item3.enhancementLevel) {
            bonusStat = item1.bonusStat;
            enhancementLevel = item1.enhancementLevel;
        }
        if (positiveTendency == null && negativeTendency == null
                && bonusStat == null) {
            return new HBMain.MultistepResponse(Arrays.asList(
                "Unable to reroll items: Items do not share a common trait "
                + "(positive tendency, negative tendency, or item type)"));
        }

        // TODO: Adjust this to be 1 query
        for (Item item: items) {
            Gacha.GachaCharacter character = Gacha.getCharacterByItem(uid, item.itemId);
            if (character != null) {
                return new HBMain.MultistepResponse(Arrays.asList(
                    "Unable to reroll items: " + item.getName() + " is equipped by "
                    + character.getDisplayName()));
            }
        }

        Item newItem = generateItem(uid);
        if (positiveTendency != null) { newItem.positiveTendency = positiveTendency; }
        if (negativeTendency != null) { newItem.negativeTendency = negativeTendency; }
        if (bonusStat != null) {
            newItem.bonusStat = bonusStat;
            newItem.enhancementLevel = enhancementLevel;
        }
        newItem.awardTo(uid);
        destroyItems(uid, Arrays.asList(iid1, iid2, iid3));

        List<String> results = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        result.append("Destroyed ");
        result.append(item1.getName());
        result.append(" (");
        result.append(item1.getModifiers().toString());
        result.append(")");
        results.add(result.toString());
        result.append("\nDestroyed ");
        result.append(item2.getName());
        result.append(" (");
        result.append(item2.getModifiers().toString());
        result.append(")");
        results.add(result.toString());
        result.append("\nDestroyed ");
        result.append(item3.getName());
        result.append(" (");
        result.append(item3.getModifiers().toString());
        result.append(")");
        results.add(result.toString());
        results.add(":package::black_large_square::package::black_large_square::package:");
        results.add(":black_large_square::package::black_large_square::package::black_large_square:");
        results.add(":black_large_square::black_large_square::package::black_large_square::black_large_square:");
        results.add(":black_large_square::black_large_square::black_large_square::black_large_square::black_large_square:");
        results.add(":black_large_square::black_large_square::confetti_ball::black_large_square::black_large_square:");
        result.append("\n\nCreated ");
        result.append(newItem.getBriefDescription());
        results.add(result.toString());
        return new HBMain.MultistepResponse(results);
    }

    // CREATE TABLE IF NOT EXISTS gacha_item (
    //  iid SERIAL,
    //  uid bigint NOT NULL,
    //  generator int NOT NULL,
    //  enhancement_level integer NOT NULL,
    //  gem_slots integer NOT NULL,
    //  positive_tendency integer NOT NULL,
    //  negative_tendency integer NOT NULL,
    //  bonus_stat integer NOT NULL,
    //  initial_additions integer NOT NULL,
    //  initial_subtractions integer NOT NULL,
    //  initial_work integer NOT NULL,
    //  initial_fish integer NOT NULL,
    //  initial_pick integer NOT NULL,
    //  initial_rob integer NOT NULL,
    //  initial_misc integer NOT NULL,
    //  name varchar(100) NOT NULL,
    //  destroyed boolean NOT NULL DEFAULT false,
    //  PRIMARY KEY(iid, uid),
    //  CONSTRAINT gacha_item_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE INDEX gacha_item_lowercase_name ON gacha_item (LOWER(name));

    // CREATE TABLE IF NOT EXISTS gacha_item_gem (
    //  gid SERIAL PRIMARY KEY,
    //  uid bigint NOT NULL,
    //  iid bigint NOT NULL,
    //  gem_type integer NOT NULL,
    //  work_modifier integer NOT NULL DEFAULT 0,
    //  fish_modifier integer NOT NULL DEFAULT 0,
    //  pick_modifier integer NOT NULL DEFAULT 0,
    //  rob_modifier integer NOT NULL DEFAULT 0,
    //  misc_modifier integer NOT NULL DEFAULT 0,
    //  additions integer NOT NULL DEFAULT 0,
    //  subtractions integer NOT NULL DEFAULT 0,
    //  gem_slots_added integer NOT NULL DEFAULT 0,
    //  negated boolean NOT NULL DEFAULT false,
    //  CONSTRAINT gacha_item_gem_iid FOREIGN KEY(uid, iid) REFERENCES gacha_item(uid, iid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_user_gem (
    //  gid int NOT NULL,
    //  uid bigint NOT NULL,
    //  quantity int NOT NULL DEFAULT 0,
    //  PRIMARY KEY(gid, uid),
    //  CONSTRAINT gacha_user_gem_uid FOREIGN KEY(uid) REFERENCES money_user(uid),
    //  CONSTRAINT gacha_user_gem_gid FOREIGN KEY(gid) REFERENCES gacha_gem(gid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_gem (
    //  gid int PRIMARY KEY,
    //  name varchar(100) NOT NULL
    // );

    // CREATE INDEX gacha_gem_lowercase_name ON gacha_gem (LOWER(name));

    static boolean logAppliedGem(long uid, long itemId, int gemId, StatArray stats, int additions,
            int subtractions, int addedGemSlots) {
        String query = "INSERT INTO gacha_item_gem (uid, iid, gem_type, work_modifier, fish_modifier, pick_modifier, "
            + "rob_modifier, misc_modifier, additions, subtractions, gem_slots_added) VALUES (" + uid + ","
            + itemId + ", " + gemId + ", " + stats.formatForDB() + ", " + additions + ", " + subtractions
            + ", " + addedGemSlots + ");";
        return CasinoDB.executeUpdate(query);
    }

    static boolean logName(Item item) {
        String query = "UPDATE gacha_item SET name = '" + item.getName().toLowerCase()
            + "' WHERE iid = " + item.getItemId() + " AND uid = " + item.getItemUid() + ";";
        return CasinoDB.executeUpdate(query);
    }

    static void logGemConsumed(long uid, int gemId) {
        String query = "UPDATE gacha_user_gem SET quantity = quantity - 1 WHERE uid = "
            + uid + " AND gid = " + gemId + ";";
        CasinoDB.executeUpdate(query);
    }

    static boolean logGemRemoval(long gemId) {
        String query = "UPDATE gacha_item_gem SET negated = true WHERE negated = false AND gid = " + gemId + ";";
        return CasinoDB.executeUpdate(query);
    }

    static boolean logAwardItem(long uid, Item item, StatArray initialStats) {
        String query = "INSERT INTO gacha_item (iid, uid, generator, enhancement_level, gem_slots, positive_tendency, "
            + "negative_tendency, bonus_stat, initial_additions, initial_subtractions, name, initial_work, initial_fish, "
            + "initial_pick, initial_rob, initial_misc) VALUES (DEFAULT, " + uid + ", " + item.generatorVersion + ", "
            + item.enhancementLevel + "," + item.gemSlots + ", " + item.positiveTendency.getIndex() + ", "
            + item.negativeTendency.getIndex() + ", " + item.bonusStat.getIndex() + ", " + item.additions + ", "
            + item.subtractions + ", '" + item.getName().toLowerCase() + "', " + initialStats.formatForDB()
            + ") ON CONFLICT DO NOTHING;";
        return CasinoDB.executeUpdate(query);
    }

    static boolean logAwardGem(long uid, int gid) {
        String query = "INSERT INTO gacha_user_gem (gid, uid, quantity) VALUES (" + gid + ", " + uid
            + ", 1) ON CONFLICT (gid, uid) DO UPDATE SET quantity = gacha_user_gem.quantity + 1;";
        return CasinoDB.executeUpdate(query);
    }

    static int fetchGemQuantity(long uid, int gid) {
        String query = "SELECT quantity FROM gacha_user_gem WHERE uid = " + uid + " AND gid = " + gid + ";";
        return CasinoDB.executeIntQuery(query);
    }

    private static class FetchedItem {
        int generator;
        long uid;
        long iid;
        StatArray stats;
        int positiveTendency;
        int negativeTendency;
        int bonusStat;
        int additions;
        int subtractions;
        int enhancementLevel;
        int gemSlots;

        FetchedItem(int generator, long uid, long iid, int work, int fish, int pick, int rob, int misc,
                int positiveTendency, int negativeTendency, int bonusStat, int additions,
                int subtractions, int enhancementLevel, int gemSlots) {
            this.generator = generator;
            this.uid = uid;
            this.iid = iid;
            this.stats = new StatArray(work, fish, pick, rob, misc);
            this.positiveTendency = positiveTendency;
            this.negativeTendency = negativeTendency;
            this.bonusStat = bonusStat;
            this.additions = additions;
            this.subtractions = subtractions;
            this.enhancementLevel = enhancementLevel;
            this.gemSlots = gemSlots;
        }
    }

    static List<GachaGems.AppliedGem> fetchGems(long uid, long iid) {
        String query = "SELECT gem_type, work_modifier, fish_modifier, pick_modifier, rob_modifier, misc_modifier, "
            + "gem_slots_added, additions, subtractions, gid FROM gacha_item_gem WHERE negated = false AND iid = "
            + iid + "AND uid = " + uid + ";";
        List<GachaGems.AppliedGem> gems = new ArrayList<>();
        return CasinoDB.executeQueryWithReturn(query, results -> {
            while (results.next()) {
                StatArray stats = new StatArray(results.getInt(2), results.getInt(3),
                    results.getInt(4), results.getInt(5), results.getInt(6));
                gems.add(new GachaGems.AppliedGem(results.getInt(1), stats, results.getInt(7),
                    results.getInt(8), results.getInt(9), results.getInt(10)));
            }
            return gems;
        }, gems);
    }

    static Item fetchItem(long uid, long iid) {
        List<GachaGems.AppliedGem> gems = fetchGems(uid, iid);
        String query = "SELECT generator, uid, iid, initial_work, initial_fish, initial_pick, initial_rob, "
            + "initial_misc, positive_tendency, negative_tendency, bonus_stat, initial_additions, "
            + "initial_subtractions, enhancement_level, gem_slots FROM gacha_item WHERE iid = " + iid
            + " AND uid = " + uid + ";";
        FetchedItem item = CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return new FetchedItem(results.getInt(1), results.getLong(2), results.getLong(3),
                    results.getInt(4), results.getInt(5), results.getInt(6), results.getInt(7),
                    results.getInt(8), results.getInt(9), results.getInt(10), results.getInt(11),
                    results.getInt(12), results.getInt(13), results.getInt(14), results.getInt(15));
            }
            return null;
        }, null);
        return item == null ? null : Item.getItem(item, gems);
    }

    static String createIidQuery(List<Long> iids) {
        StringBuilder iidsBuilder = new StringBuilder();
        for (long iid : iids) {
            if (iidsBuilder.length() != 0) { iidsBuilder.append(", "); }
            iidsBuilder.append(iid);
        }
        return iidsBuilder.toString();
    }

    static List<Item> fetchItems(long uid) {
        return fetchItems(uid, "");
    }

    static List<Item> fetchItems(long uid, String partialName) {
        partialName = '%' + partialName + '%';
        List<Item> items = new ArrayList<>();
        String iidQuery = "SELECT iid FROM gacha_item WHERE uid = " + uid + " AND LOWER(name) LIKE LOWER(?)";
        String query =  "WITH gem_stats AS (SELECT iid, SUM(work_modifier) AS gem_work, SUM(fish_modifier) AS gem_fish, "
            + "SUM(pick_modifier) AS gem_pick, SUM(rob_modifier) AS gem_rob, SUM(misc_modifier) AS gem_misc, SUM(additions) AS "
            + "gem_additions, SUM(subtractions) AS gem_subtractions, SUM(gem_slots_added) AS gem_granted_slots, "
            + "COUNT(*) AS gem_count FROM gacha_item_gem WHERE negated = false AND iid IN (" + iidQuery + ") "
            + "GROUP BY iid) SELECT generator, gacha_item.uid, gacha_item.iid, initial_work, initial_fish, initial_pick, initial_rob, "
            + "initial_misc, positive_tendency, negative_tendency, bonus_stat, initial_additions, initial_subtractions, "
            + "enhancement_level, gem_slots, gem_work, gem_fish, gem_pick, gem_rob, gem_misc, gem_additions, "
            + "gem_subtractions, gem_granted_slots, gem_count FROM gem_stats RIGHT OUTER JOIN gacha_item ON "
            + "gem_stats.iid = gacha_item.iid WHERE uid = " + uid + " AND LOWER(name) LIKE LOWER(?) AND destroyed = false "
            + "ORDER BY (initial_additions + coalesce(gem_additions, 0) - initial_subtractions - coalesce(gem_subtractions, 0)) DESC LIMIT 25;";
        return CasinoDB.executeValidatedQueryWithReturn(query, results -> {
            while (results.next()) {
                items.add(Item.getItem(new FetchedItem(results.getInt(1), results.getLong(2), results.getLong(3),
                    results.getInt(4), results.getInt(5), results.getInt(6), results.getInt(7),
                    results.getInt(8), results.getInt(9), results.getInt(10), results.getInt(11),
                    results.getInt(12), results.getInt(13), results.getInt(14), results.getInt(15)),
                    new StatArray(results.getInt(16), results.getInt(17), results.getInt(18),
                    results.getInt(19), results.getInt(20)), results.getInt(21), results.getInt(22),
                    results.getInt(23), results.getInt(24)));
            }
            return items;
        }, items, partialName, partialName);
    }

    static List<Item> fetchItems(long uid, String partialName, List<Long> iids) {
        partialName = '%' + partialName + '%';
        List<Item> items = new ArrayList<>();
        String iidQuery = "0";
        if (iids != null && !iids.isEmpty()) {
            iidQuery = createIidQuery(iids);
        }
        String query =  "WITH gem_stats AS (SELECT iid, SUM(work_modifier) AS gem_work, SUM(fish_modifier) AS gem_fish, "
            + "SUM(pick_modifier) AS gem_pick, SUM(rob_modifier) AS gem_rob, SUM(misc_modifier) AS gem_misc, SUM(additions) AS "
            + "gem_additions, SUM(subtractions) AS gem_subtractions, SUM(gem_slots_added) AS gem_granted_slots, "
            + "COUNT(*) AS gem_count FROM gacha_item_gem WHERE negated = false AND iid IN (" + iidQuery + ") "
            + "GROUP BY iid) SELECT generator, gacha_item.uid, gacha_item.iid, initial_work, initial_fish, initial_pick, initial_rob, "
            + "initial_misc, positive_tendency, negative_tendency, bonus_stat, initial_additions, initial_subtractions, "
            + "enhancement_level, gem_slots, gem_work, gem_fish, gem_pick, gem_rob, gem_misc, gem_additions, "
            + "gem_subtractions, gem_granted_slots, gem_count FROM gem_stats RIGHT OUTER JOIN gacha_item ON "
            + "gem_stats.iid = gacha_item.iid WHERE uid = " + uid
            + " AND LOWER(name) LIKE LOWER(?) AND destroyed = false AND gacha_item.iid IN (" + iidQuery + ") ORDER BY iid DESC LIMIT 10;";
        return CasinoDB.executeValidatedQueryWithReturn(query, results -> {
            while (results.next()) {
                items.add(Item.getItem(new FetchedItem(results.getInt(1), results.getLong(2), results.getLong(3),
                    results.getInt(4), results.getInt(5), results.getInt(6), results.getInt(7),
                    results.getInt(8), results.getInt(9), results.getInt(10), results.getInt(11),
                    results.getInt(12), results.getInt(13), results.getInt(14), results.getInt(15)),
                    new StatArray(results.getInt(16), results.getInt(17), results.getInt(18),
                    results.getInt(19), results.getInt(20)), results.getInt(21), results.getInt(22),
                    results.getInt(23), results.getInt(24)));
            }
            return items;
        }, items, partialName);
    }

    static Map<Item, String> fetchItemList(long uid) {
        Map<Item, String> items = new LinkedHashMap<>();
        for (Item item : fetchItems(uid)) {
            items.put(item, "");
        }
        List<Gacha.GachaCharacter> characters = Gacha.queryCharacters(uid);
        for (Gacha.GachaCharacter character : characters) {
            if (character.getItem() != null) {
                items.put(character.getItem(), character.getDisplayName());
            }
        }
        return items;
    }

    static void destroyItems(long uid, List<Long> iids) {
        String query = "UPDATE gacha_item SET destroyed = true WHERE uid = " + uid
            + " AND iid IN (" + createIidQuery(iids) + ");";
        CasinoDB.executeUpdate(query);
    }

    static List<UnappliedGem> queryGems(long uid) {
        return queryGems(uid, "");
    }

    static List<UnappliedGem> queryGems(long uid, String substring) {
        substring = '%' + substring + '%';
        String query = "SELECT gid, quantity, name FROM gacha_user_gem NATURAL JOIN gacha_gem "
            + "WHERE quantity > 0 AND uid = " + uid + " AND LOWER(name) LIKE LOWER(?);";
        List<UnappliedGem> unappliedGems = new ArrayList<>();
        return CasinoDB.executeValidatedQueryWithReturn(query, results -> {
            while (results.next()) {
                unappliedGems.add(new UnappliedGem(results.getInt(1), results.getInt(2),
                    results.getString(3)));
            }
            return unappliedGems;
        }, unappliedGems, substring);
    }

}
