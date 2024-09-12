package com.c2t2s.hb;

import java.util.ArrayList;
import java.util.List;

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
            return (amount > 0 ? "+" : "") + Stats.oneDecimal.format(amount / 10.0);
        }
    }

    // This could be adjusted to be doubles in more granular control
    // is wanted in the future
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

        void addArray(StatArray array) {
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                addStat(stat, array.getStat(stat));
            }
        }

        @Override
        public String toString() {
            // e.g. [-0.1W +0.4F +1.1P +0.2R -0.5M]
            StringBuilder builder = new StringBuilder();
            for (ITEM_STAT stat : ITEM_STAT.values()) {
                builder.append(builder.length() == 0 ? '[' : ' ');
                if (stats[stat.index] > 0) { builder.append('+'); }
                builder.append(Stats.oneDecimal.format(stats[stat.index] / 10.0));
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
    }

    private static final int INITIAL_BONUS_INDEX = 0;
    private static final int GEM_BONUS_INDEX = 1;

    abstract static class Item {
        int generatorVersion;
        long itemId;
        // { StatArray of inital stats, StatArray of gem stats }
        StatArray[] bonuses;
        List<GachaGems.AppliedGem> gems;

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

        List<String> applyGem(GachaGems.Gem gem) {
            // TODO: Move this to command handling
            if (!gem.isEligible(this)) {
                List<String> result = new ArrayList<>();
                result.add(gem.getIneligibilityReason(this));
                return result;
            }

            GachaGems.GemApplicationResult applicationResult = gem.apply(this);
            gems.add(applicationResult.result);
            bonuses[GEM_BONUS_INDEX].addArray(applicationResult.result.getModifiedStats());
            logAppliedGem(itemId, gem.id, applicationResult.result.getModifiedStats(),
                applicationResult.result.getAdditions(),
                applicationResult.result.getSubtractions(),
                applicationResult.result.getAddedGemSlots());
            return applicationResult.output;
        }

        int removeAllGems() {
            int removedGems = gems.size();
            for (GachaGems.AppliedGem gem : gems) {
                logGemRemoval(gem.getGemId());
            }
            gems.clear();
            return removedGems;
        }

        public String getBriefDescription() {
            return getName() + '\n' + getModifiers().toString() + '\n' + gemSlots + " gem slots";
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
                builder.append("% (");
                builder.append(ITEM_STAT.format(initialBonus));
                builder.append("% Base");
                if (gemBonus != 0) {
                    builder.append(", ");
                    builder.append(ITEM_STAT.format(gemBonus));
                    builder.append(" from Gems");
                }
                if (stat == bonusStat) {
                    int bonusContribution = getBonusModifierContribution(initialBonus + gemBonus);
                    if (bonusContribution != 0.0) {
                        builder.append(", ");
                        builder.append(ITEM_STAT.format(bonusContribution));
                        builder.append("% from Item Type");
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
                for (ITEM_STAT stat : ITEM_STAT.values()) {
                    int value = gem.getModifiedStats().getStat(stat);
                    // Most values will be 0
                    if (value != 0) {
                        gemBonuses.addStat(stat, value);
                        if (value > 0) {
                            item.additions += value;
                        } else {
                            item.subtractions += value;
                        }
                    }
                }
                appliedGemSlots += gem.getAddedGemSlots();
            }

            StatArray[] bonuses = { item.stats, gemBonuses };

            // In the future use the generator version to pick the right item generation
            return new G0Item(item.generator, item.iid, bonuses, gems,
                ITEM_STAT.fromIndex(item.positiveTendency),
                ITEM_STAT.fromIndex(item.negativeTendency), ITEM_STAT.fromIndex(item.bonusStat),
                item.additions, item.subtractions, item.enhancementLevel,
                item.gemSlots + appliedGemSlots);
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

        private static final int GENERATOR_VERSION = 3; // 0.03
        private static final double LN_ONE_HALF = Math.log(0.5);
        private static final int AVERAGE_ADDITIONS = 50;
        private static final int AVERAGE_SUBTRACTIONS = 50;
        private static final int ADDITIONS_STDDEV = 25;
        private static final int BASE_STAT_AMOUNT = 2;
        private static final int BASE_GEM_SLOTS = 2;
        private static final double TENDENCY_CHANCE = 0.1;
        private static final double UNENHANCED_BONUS_STAT_MODIFIER = 0.25;
        private static final double ENHANCED_BONUS_STAT_MODIFIER = 0.5;

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
            this.gems             = new ArrayList<>();
            this.positiveTendency = positiveTendency;
            this.negativeTendency = negativeTendency;
            this.bonusStat        = bonusStat;
            this.gemSlots         = gemSlots;
        }

        G0Item(int generatorVersion, long itemId, StatArray[] bonuses,
                List<GachaGems.AppliedGem> gems, ITEM_STAT positiveTendency,
                ITEM_STAT negativeTendency, ITEM_STAT bonusStat, int additions,
                int subtractions, int enhancementLevel, int gemSlots) {
            this.generatorVersion = generatorVersion;
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

    static String handleTest(long uid) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1; i++) {
            Item item = G0Item.generate();
            builder.append(item.getBriefDescription());
            builder.append("\n");
            item.awardTo(uid);
        }
        for (int i = 0; i < 1; ++i) {
            builder.append('\n');
            builder.append(handleAwardGem(uid, GachaGems.Gem.getRandomGem(0)));
        }
        return builder.toString();
    }

    static String handleItemInfo(long uid, long iid) {
        Item item = fetchItem(uid, iid);
        if (item == null) {
            return "Failed to fetch item " + iid;
        }
        return item.getFullDescription();
    }

    static String handleAwardGem(long uid, GachaGems.Gem gem) {
        if (logAwardGem(uid, gem.getId())) {
            return gem.getAwardMessage();
        }
        return "Failed to award gem " + gem.getId();
    }

    static HBMain.MultistepResponse handleApplyGem(long uid, long gemId, long iid) {
        int gid = (int)gemId;
        List<String> results = new ArrayList<>();
        int quantity = fetchGemQuantity(uid, gid);
        if (quantity < 1) {
            results.add("Unable to apply gem: You don't have any " + GachaGems.Gem.fromId(gid).getName());
            return new HBMain.MultistepResponse(results);
        }
        Item item = fetchItem(uid, iid);
        if (item == null) {
            results.add("Unable to apply gem: Item " + iid + " not found");
            return new HBMain.MultistepResponse(results);
        }
        return new HBMain.MultistepResponse(item.applyGem(GachaGems.Gem.fromId(gid)));
    }

    // CREATE TABLE IF NOT EXISTS gacha_item (
    //  iid SERIAL PRIMARY KEY,
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
    //  CONSTRAINT gacha_item_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_item_gem (
    //  gid SERIAL PRIMARY KEY,
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
    //  CONSTRAINT gacha_item_gem_iid FOREIGN KEY(iid) REFERENCES gacha_item(iid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_user_gem (
    //  gid int NOT NULL,
    //  uid bigint NOT NULL,
    //  quantity int NOT NULL DEFAULT 0,
    //  PRIMARY KEY(gid, uid),
    //  CONSTRAINT gacha_user_gem_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    static boolean logAppliedGem(long itemId, int gemId, StatArray stats, int additions,
            int subtractions, int addedGemSlots) {
        String query = "INSERT INTO gacha_item_gem (iid, gem_type, work_modifier, fish_modifier, pick_modifier, "
            + "rob_modifier, misc_modifier, additions, subtractions, gem_slots_added) VALUES (" + itemId + ", "
            + gemId + ", " + stats.formatForDB() + ", " + additions + ", " + subtractions + ", "+ addedGemSlots + ";";
        return CasinoDB.executeUpdate(query);
    }

    static boolean logGemRemoval(long gemId) {
        String query = "UPDATE gacha_item_gem SET negated = true WHERE negated = false AND gid = " + gemId + ";";
        return CasinoDB.executeUpdate(query);
    }

    static boolean logAwardItem(long uid, Item item, StatArray initialStats) {
        String query = "INSERT INTO gacha_item (uid, generator, enhancement_level, gem_slots, positive_tendency, "
            + "negative_tendency, bonus_stat, initial_additions, initial_subtractions, initial_work, initial_fish, "
            + "initial_pick, initial_rob, initial_misc) VALUES (" + uid + ", " + item.generatorVersion + ", 0,"
            + item.gemSlots + ", " + item.positiveTendency.getIndex() + ", " + item.negativeTendency.getIndex() + ", "
            + item.bonusStat.getIndex() + ", " + item.additions + ", " + item.subtractions + ", "
            + initialStats.formatForDB() + ") ON CONFLICT DO NOTHING;";
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
        long iid;
        StatArray stats;
        int positiveTendency;
        int negativeTendency;
        int bonusStat;
        int additions;
        int subtractions;
        int enhancementLevel;
        int gemSlots;

        FetchedItem(int generator, long iid, int work, int fish, int pick, int rob, int misc,
                int positiveTendency, int negativeTendency, int bonusStat, int additions,
                int subtractions, int enhancementLevel, int gemSlots) {
            this.generator = generator;
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

    static List<GachaGems.AppliedGem> fetchGems(long iid) {
        String query = "SELECT gem_type, work_modifier, fish_modifier, pick_modifier, rob_modifier, misc_modifier, "
            + "gem_slots_added, additions, subtractions, gid FROM gacha_item_gem WHERE iid = " + iid + ";";
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
        List<GachaGems.AppliedGem> gems = fetchGems(iid);
        String query = "SELECT generator, iid, initial_work, initial_fish, initial_pick, initial_rob, "
            + "initial_misc, positive_tendency, negative_tendency, bonus_stat, initial_additions, "
            + "initial_subtractions, enhancement_level, gem_slots FROM gacha_item WHERE iid = " + iid
            + " AND uid = " + uid + ";";
        FetchedItem item = CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return new FetchedItem(results.getInt(1), results.getLong(2), results.getInt(3),
                    results.getInt(4), results.getInt(5), results.getInt(6), results.getInt(7),
                    results.getInt(8), results.getInt(9), results.getInt(10), results.getInt(11),
                    results.getInt(12), results.getInt(13), results.getInt(14));
            }
            return null;
        }, null);
        return Item.getItem(item, gems);
    }
}
