package com.c2t2s.hb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.c2t2s.hb.GachaItems.Item;

import java.net.MalformedURLException;
import java.net.URL;

class Gacha {

    // Hide default constructor
    private Gacha() {}

    private static class GachaUser {
        int timesPulled;
        int oneStarPity;
        int twoStarPity;
        int threeStarPity;
        int fourStarPity;
        int fiveStarPity;
    }

    private static final int SHINY_TYPE_NORMAL = 0;
    private static final int SHINY_TYPE_SHINY = 1;
    private static final int SHINY_TYPE_PRISMATIC = 2;

    enum SHINY_TYPE {
        NORMAL(SHINY_TYPE_NORMAL, ""),
        SHINY(SHINY_TYPE_SHINY, "Foil"),
        PRISMATIC(SHINY_TYPE_PRISMATIC, "Prismatic");

        private int typeId;
        private String adjective;
        SHINY_TYPE(int id, String adjective) {
            typeId = id;
            this.adjective = adjective;
        }
        int getId() { return typeId; }
        String getAdjective() { return adjective; }

        static SHINY_TYPE fromId(int id) {
            switch (id) {
                case SHINY_TYPE_SHINY:
                    return SHINY_TYPE.SHINY;
                case SHINY_TYPE_PRISMATIC:
                    return SHINY_TYPE.PRISMATIC;
                case SHINY_TYPE_NORMAL:
                default:
                    return SHINY_TYPE.NORMAL;
            }
        }
    }

    static class GachaCharacter {

        private long id;
        private String name;
        private int rarity;
        private SHINY_TYPE shiny;
        private String type;
        private int level;
        private int xp;
        private int duplicates;
        private String description;
        private String pictureUrl;
        private String shinyUrl;
        private String prismaticUrl;

        private double workBonus;
        private double fishBonus;
        private double pickBonus;
        private double robBonus;
        private double miscBonus;
        private GachaItems.Item item;

        private static final String PICTURE_REPLACEMENT = ".";
        private static final String UNIQUE_ID_SEPARATOR = "|";

        private GachaCharacter(long id, String name, int rarity, SHINY_TYPE shiny, String type, int level,
                int xp, int duplicates, String description, String pictureUrl, String shinyUrl,
                String prismaticUrl, double workBonus, double fishBonus, double pickBonus, double robBonus,
                double miscBonus, GachaItems.Item item) {
            this.id = id;
            this.name = name;
            this.rarity = rarity;
            this.shiny = shiny;
            this.type = type;
            this.level = level;
            this.xp = xp;
            this.duplicates = duplicates;
            this.description = description;
            this.pictureUrl = pictureUrl;
            this.shinyUrl = shinyUrl;
            this.prismaticUrl = prismaticUrl;

            this.workBonus = workBonus;
            this.fishBonus = fishBonus;
            this.pickBonus = pickBonus;
            this.robBonus = robBonus;
            this.miscBonus = miscBonus;
            this.item = item;

            if (duplicates > MAX_CHARACTER_DUPLICATES) {
                duplicates = MAX_CHARACTER_DUPLICATES;
            }
            if (level > MAX_CHARACTER_LEVEL) {
                level = MAX_CHARACTER_LEVEL;
            }
        }

        String getDisplayName() {
            if (shiny == SHINY_TYPE.NORMAL) {
                return name;
            }
            return shiny.getAdjective() + " " + name;
        }

        private String getPictureLink() {
            switch (shiny.getId()) {
                case SHINY_TYPE_SHINY:
                    return shinyUrl;
                case SHINY_TYPE_PRISMATIC:
                    return prismaticUrl;
                case SHINY_TYPE_NORMAL:
                default:
                    return pictureUrl;
            }
        }

        GachaItems.Item getItem() { return item; }

        @Override
        public String toString() {
            return toString(false);
        }

        private String toString(boolean compact) {
            StringBuilder display = new StringBuilder();
            display.append(getDisplayName());
            if (duplicates > 0) {
                display.append(" +");
                display.append(duplicates);
            }
            display.append(" (");
            display.append(rarity);
            display.append(" Star ");
            display.append(type);
            display.append(')');
            if (compact) {
                display.append(" - ");
            } else {
                display.append('[');
                display.append(PICTURE_REPLACEMENT);
                display.append("](");
                display.append(getPictureLink());
                display.append(")\n\t");
            }
            display.append("Level ");
            display.append(level);
            if (level >= MAX_CHARACTER_LEVEL) {
                display.append(" [Max Level]");
            } else {
                display.append(" [");
                display.append(xp);
                display.append('/');
                display.append(getXpToLevel());
                display.append(']');
            }
            display.append("\n\t");
            if (item != null) {
                GachaItems.StatArray stats = item.getModifiers();
                String itemStatString = stats.toString();
                stats.addArray(getCharacterStats());
                display.append(stats.toString());
                display.append(" (");
                display.append(itemStatString);
                display.append(" from item)");
            } else {
                display.append(getCharacterStats().toString());
                if (!compact) {
                    display.append("\n  No item equipped");
                }
            }
            if (!compact && !description.isEmpty()) {
                display.append("\n\t\"");
                display.append(description);
                display.append('\"');
            }
            return display.toString();
        }

        private String generateAwardText(boolean alreadyMaxed) {
            String star = (shiny.getId() > 0 ? ":star2:" : ":star:");
            String stars = "";
            if (rarity > 0) {
                stars = Casino.repeatString(star, rarity);
            }
            String duplicateString = "";
            if (alreadyMaxed) {
                long coinEquivalent = getMaxedCharacterCoinEquivalent();
                duplicateString = "\n\t" + getDisplayName() + " +" + duplicates + " already maxed, converted to "
                    + coinEquivalent + " coin" + Casino.getPluralSuffix(coinEquivalent);
            } else if (duplicates > 0) {
                duplicateString = "\n\tUpgraded " + getDisplayName()
                    + (duplicates > 1 ? " +" + (duplicates - 1) : "")
                    + " -> " + getDisplayName() + " +" + duplicates;
            }
            return stars + " " + getDisplayName() + " " + stars + " (" + rarity + " Star " + type + ")["
                + PICTURE_REPLACEMENT + "](" + getPictureLink() + ")" + duplicateString;
        }

        private int getXpToLevel() {
            // Level 0 -> Level 1: 100 xp
            // Every level beyond doubles previous level
            return 50 * (int)Math.pow(2, level + 1);
        }

        private int getBaseBuffAmount() {
            return 25 + (rarity * 25)
                + ((duplicates > MAX_CHARACTER_DUPLICATES ? MAX_CHARACTER_DUPLICATES : duplicates) * 10)
                + (level * 10);
        }

        GachaItems.StatArray getCharacterStats() {
            int baseAmount = getBaseBuffAmount();
            return new GachaItems.StatArray((int)workBonus * baseAmount, (int)fishBonus * baseAmount,
                (int)pickBonus * baseAmount, (int)robBonus * baseAmount, (int)miscBonus * baseAmount);
        }

        private GachaItems.StatArray getTotalStatArray() {
            if (item == null) {
                return getCharacterStats();
            } else {
                return item.getModifiers().addArray(getCharacterStats());
            }
        }

        private long getMaxedCharacterCoinEquivalent() {
            if (rarity < MAXED_CHARACTER_COIN_VALUES.size()) {
                return MAXED_CHARACTER_COIN_VALUES.get(rarity);
            }
            return 100L;
        }

        private String getUniqueId() {
            return id + UNIQUE_ID_SEPARATOR + shiny.getId() + UNIQUE_ID_SEPARATOR
                + getDisplayName();
        }

        long getId() { return id; }

        private static SHINY_TYPE parseUniqueIdShiny(String uniqueId) {
            try {
                int index = uniqueId.indexOf(UNIQUE_ID_SEPARATOR);
                int secondIndex = uniqueId.indexOf(UNIQUE_ID_SEPARATOR, index + 1);
                return SHINY_TYPE.fromId(Integer.parseInt(
                    uniqueId.substring(index + 1, secondIndex)));
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                System.out.println("Failed to parse Shiny type from character unique id string: "
                    + uniqueId);
                return SHINY_TYPE.NORMAL;
            }
        }

        private static long parseUniqueIdCid(String uniqueId) {
            try {
                int index = uniqueId.indexOf(UNIQUE_ID_SEPARATOR);
                return Long.parseLong(uniqueId.substring(0, index));
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                System.out.println("Failed to parse cid from character unique id string: "
                    + uniqueId);
                return -1;
            }
        }

        static GachaCharacter fromUniqueId(long uid, String uniqueId)
                throws IllegalArgumentException {
            long cid = GachaCharacter.parseUniqueIdCid(uniqueId);
            SHINY_TYPE shiny = GachaCharacter.parseUniqueIdShiny(uniqueId);
            if (cid < 0) {
                throw new IllegalArgumentException("Unrecognized character id");
            }
            GachaCharacter character = getCharacter(uid, cid, shiny);
            if (character == null) {
                long pullBalance = getPullCount(uid);
                if (pullBalance < 0) {
                    throw new IllegalArgumentException(Casino.USER_NOT_FOUND_MESSAGE);
                }
                throw new IllegalArgumentException("Specified character not found");
            }
            return character;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof GachaCharacter)) {
                return false;
            }
            GachaCharacter otherCharacter = (GachaCharacter)other;
            return hashCode() == otherCharacter.hashCode();
        }

        @Override
        public int hashCode() {
            return 17 * (int)id + 13 * shiny.getId();
        }
    }

    static class GachaBanner {
        int bannerId;
        double oneStarChance;
        double twoStarChance;
        double threeStarChance;
        double fourStarChance;
        double fiveStarChance;
        double shinyChance;
        double prismaticChance;
        double scalingOneStarBonus;
        double scalingTwoStarBonus;
        double scalingThreeStarBonus;
        double scalingFourStarBonus;
        double scalingFiveStarBonus;
        int maxOneStarPity;
        int maxTwoStarPity;
        int maxThreeStarPity;
        int maxFourStarPity;
        int maxFiveStarPity;
        boolean enabled;
        String name;
        String description;

        private double getOneStarChance(int pity) {
            return getPullChance(oneStarChance, pity, maxOneStarPity, scalingOneStarBonus);
        }

        private double getTwoStarChance(int pity) {
            return getPullChance(twoStarChance, pity, maxTwoStarPity, scalingTwoStarBonus);
        }

        private double getThreeStarChance(int pity) {
            return getPullChance(threeStarChance, pity, maxThreeStarPity, scalingThreeStarBonus);
        }

        private double getFourStarChance(int pity) {
            return getPullChance(fourStarChance, pity, maxFourStarPity, scalingFourStarBonus);
        }

        private double getFiveStarChance(int pity) {
            return getPullChance(fiveStarChance, pity, maxFiveStarPity, scalingFiveStarBonus);
        }

        private double getPullChance(double baseChance, int currentPity, int maxPity, double scalingBonus) {
            if (currentPity >= maxPity) {
                return 1.0;
            }
            int avgPulls = (int)(1 / baseChance);
            if (currentPity <= avgPulls) {
                return baseChance;
            }
            double bonus = (currentPity - avgPulls) / (maxPity - avgPulls) * scalingBonus;
            return baseChance + bonus;
        }

        private String getPityString(GachaUser user, long pullBalance) {
            StringBuilder output = new StringBuilder();
            output.append("Pity for ");
            output.append(name);
            output.append('\n');
            if (fiveStarChance > 0) {
                output.append("5 Star Pity: ");
                output.append(user.fiveStarPity);
                output.append('/');
                output.append(maxFiveStarPity + 1);
                output.append('\n');
            }
            if (fourStarChance > 0) {
                output.append("4 Star Pity: ");
                output.append(user.fourStarPity);
                output.append('/');
                output.append(maxFourStarPity + 1);
                output.append('\n');
            }
            if (threeStarChance > 0) {
                output.append("3 Star Pity: ");
                output.append(user.threeStarPity);
                output.append('/');
                output.append(maxThreeStarPity + 1);
                output.append('\n');
            }
            if (twoStarChance > 0) {
                output.append("2 Star Pity: ");
                output.append(user.twoStarPity);
                output.append('/');
                output.append(maxTwoStarPity + 1);
                output.append('\n');
            }
            if (oneStarChance > 0) {
                output.append("1 Star Pity: ");
                output.append(user.oneStarPity);
                output.append('/');
                output.append(maxOneStarPity + 1);
                output.append('\n');
            }
            output.append("Available Pulls: ");
            output.append(pullBalance);
            return output.toString();
        }

        SHINY_TYPE generateShinyType() {
            if (HBMain.RNG_SOURCE.nextDouble() <= prismaticChance) {
                return SHINY_TYPE.PRISMATIC;
            } else if (HBMain.RNG_SOURCE.nextDouble() <= shinyChance) {
                return SHINY_TYPE.SHINY;
            }
            return SHINY_TYPE.NORMAL;
        }

        private String getInfoString(long server, long uid) {
            StringBuilder output = new StringBuilder();
            output.append(name);
            output.append(":");
            if (!description.isEmpty()) {
                output.append('\n');
                output.append(description);
                output.append('\n');
            }

            List<GachaBannerCharacter> characters = getBannerCharacters(bannerId, server, uid);
            int currentRarity = 6;
            for (int i = 0; i < characters.size(); i++) {
                GachaBannerCharacter c = characters.get(i);
                if (currentRarity > c.getRarity()) {
                    currentRarity = c.getRarity();
                    output.append('\n');
                    output.append(currentRarity);
                    output.append(" Stars:");
                }
                output.append("\n  ");
                output.append(c.getDisplayString());
                output.append(": ");
                if (c.getDuplicates() < 0) {
                    output.append("Not owned");
                    continue;
                }
                boolean multipleOwned = false;
                for (SHINY_TYPE shiny: SHINY_TYPE.values()) {
                    if (c.getShiny() != shiny.getId()) {
                        continue;
                    }
                    if (multipleOwned) {
                        output.append(", ");
                    }
                    output.append(c.getOwnedString());
                    if (characters.size() > i + 1 && characters.get(i + 1).getName().equals(c.getName())) {
                        multipleOwned = true;
                        i++;
                        c = characters.get(i);
                    }
                }
            }

            return output.toString();
        }
    }

    private static class GachaBannerCharacter {
        private String name;
        private int rarity;
        private String type;
        private int shiny = -1;
        private int duplicates = -1;

        GachaBannerCharacter(String name, int rarity, String type) {
            this.name = name;
            this.rarity = rarity;
            this.type = type;
        }

        GachaBannerCharacter(String name, int rarity, String type, int shiny, int duplicates) {
            this.name = name;
            this.rarity = rarity;
            this.type = type;
            this.shiny = shiny;
            this.duplicates = duplicates;
        }

        String getName() {
            return name;
        }

        int getRarity() {
            return rarity;
        }

        int getShiny() {
            return shiny;
        }

        int getDuplicates() {
            return duplicates;
        }

        String getDisplayString() {
            return name + " - " + rarity + " Star " + type;
        }

        String getOwnedString() {
            StringBuilder result = new StringBuilder();
            if (shiny > 0) {
                result.append(SHINY_TYPE.fromId(shiny).getAdjective());
                result.append(' ');
            }
            result.append("Owned");
            if (duplicates > 0) {
                result.append(" +");
                result.append(duplicates > MAX_CHARACTER_DUPLICATES ? MAX_CHARACTER_DUPLICATES : duplicates);
            }
            return result.toString();
        }
    }

    static class GachaBannerStats {
        String name;
        int totalCharacters;
        int ownedCharacters;
        int maxedCharacters;

        GachaBannerStats(String name, int totalCharacters, int ownedCharacters, int maxedCharacters) {
            this.name = name;
            this.totalCharacters = totalCharacters;
            this.ownedCharacters = ownedCharacters;
            this.maxedCharacters = maxedCharacters;
        }

        String getDisplayString() {
            return name + ": " + ownedCharacters + "/" + totalCharacters + " owned, "
                + maxedCharacters + "/" + totalCharacters + " maxed";
        }
    }

    static class GachaResponse {
        private String partialMessage = "";
        private HBMain.MultistepResponse messages = new HBMain.MultistepResponse();

        private long coinsAwarded = 0;
        private long coinBalance = 0;
        private long chocolateCoinsAwarded = 0;
        private long chocolateCoinBalance = 0;
        private long pullBalance;
        private int highestCharacterAwarded = 0;

        GachaResponse(long pullBalance) {
            this.pullBalance = pullBalance;
        }

        private void addMessagePart(String message) {
            partialMessage += (!partialMessage.isEmpty() ? "\n" : "") + message;
            messages.addMessage(partialMessage);
        }

        private void addCharacterMessagePart(String message, int rarity, String pictureUrl) {
            if (rarity > highestCharacterAwarded) {
                highestCharacterAwarded = rarity;
            }
            //try {
            //    messages.images.put(messages.messages.size(), new URL(pictureUrl));
            //} catch (MalformedURLException e) {
            //    e.printStackTrace();
            //}
            addMessagePart(message);
        }

        private void addAnimation(List<String> frames) {
            //int size = frames.size();
            //Map<Integer, URL> adjustedImages = new HashMap<>();
            //for (int key: messages.images.keySet()) {
            //    adjustedImages.put(key + size, messages.images.get(key));
            //}
            //messages.images = adjustedImages;
            messages.addAllToStart(frames);
        }
    }

    private static final int MAX_CHARACTER_LEVEL = 5;
    private static final int MAX_CHARACTER_DUPLICATES = 5;
    private static final List<Long> MAXED_CHARACTER_COIN_VALUES = Arrays.asList(0L, 50L, 100L, 1000L);

    static HBMain.MultistepResponse handleGachaPull(long uid, long bannerId, long pulls) {
        GachaUser user = getGachaUser(uid, bannerId);
        if (user == null) {
            return new HBMain.MultistepResponse("Unable to pull: User or Banner not found. If you are new run `/claim` to start");
        }

        GachaBanner banner = getGachaBanner(bannerId);
        if (banner == null) {
            return new HBMain.MultistepResponse("Unable to pull: Banner " + bannerId + " not found.");
        } else if (!banner.enabled) {
            return new HBMain.MultistepResponse("Unbale to pull: Banner '" + banner.name + "' is not currently active.");
        }

        long availablePulls = getPullCount(uid);
        if (availablePulls < 1 || availablePulls < pulls) {
            EventUser eventUser = EventUser.getEventUser(uid);
            if (eventUser == null) {
                return new HBMain.MultistepResponse("Unable to pull. Insufficient pulls and unable to fetch EventUser.");
            }

            if (availablePulls < 1) {
                return new HBMain.MultistepResponse("No pulls remaining. " + eventUser.getAvailablePullSources());
            } else { // availablePulls < pulls
                return new HBMain.MultistepResponse("Insufficient pulls, you have " + availablePulls + " pulls remaining. "
                    + eventUser.getAvailablePullSources());
            }
        }
        availablePulls = removePulls(uid, (int)pulls);

        GachaUser updatedUser;
        GachaResponse response = new GachaResponse(availablePulls);
        boolean veryFirstPull = false;
        if (user.timesPulled == 0) {
            long totalTimesPulled = getTotalTimesPulled(uid);
            veryFirstPull = (totalTimesPulled == 0);
        }

        for (int i = 0; i < pulls; ++i) {
            int rarity = 0;
            if (banner.fiveStarChance > 0 && HBMain.RNG_SOURCE.nextDouble() <= banner.getFiveStarChance(user.fiveStarPity)) {
                rarity = 5;
            } else if (banner.fourStarChance > 0 && HBMain.RNG_SOURCE.nextDouble() <= banner.getFourStarChance(user.fourStarPity)) {
                rarity = 4;
            } else if (banner.threeStarChance > 0 && HBMain.RNG_SOURCE.nextDouble() <= banner.getThreeStarChance(user.threeStarPity)) {
                rarity = 3;
            } else if (banner.twoStarChance > 0 && HBMain.RNG_SOURCE.nextDouble() <= banner.getTwoStarChance(user.twoStarPity)) {
                rarity = 2;
            // First roll always gives at least 1 star
            } else if (banner.oneStarChance > 0 && (HBMain.RNG_SOURCE.nextDouble() <= banner.getOneStarChance(user.oneStarPity) || veryFirstPull)) {
                rarity = 1;
                veryFirstPull = false;
            }
            if (rarity > 0) {
                updatedUser = awardCharacter(uid, banner, rarity, response);
            } else {
                updatedUser = awardFiller(uid, banner.bannerId, response);
            }
            if (updatedUser != null) {
                user = updatedUser;
            }
        }

        List<String> animation;
        switch (response.highestCharacterAwarded) {
            case 5:
            case 4:
            case 3:
                animation = createThreeStarAnimation();
                break;
            case 2:
                animation = createTwoStarAnimation();
                break;
            case 1:
                animation = createOneStarAnimation();
                break;
            default:
                animation = createAnimation();
                break;
        }
        response.addAnimation(animation);

        String balances = "";
        if (response.coinsAwarded > 0) {
            balances += "\nYour new balance is " + response.coinBalance;
        }
        if (response.chocolateCoinsAwarded > 0) {
            balances += "\nYour new chocolate coin balance is " + response.chocolateCoinBalance;
        }
        balances += "\nYou have " + response.pullBalance + " pull"
            + Casino.getPluralSuffix(response.pullBalance) + " remaining";
        response.addMessagePart(balances);

        return response.messages;
    }

    // Item Chance: 1/8
    // Common Gem Chance: 1/6
    // Uncommon Gem Chance: 1/25
    // Rare Gem Chance: 1/100
    // Chocolate Coin Chance: 1/10
    // Extra Pulls Chance : 1/1000
    // Then adjust for the fact filler makes up ~83.5% of pulls
    private static final double FILLER_ADJUSTMENT = 1.198;
    private static final double ITEM_CHANCE = 0.125 * FILLER_ADJUSTMENT;
    private static final double COMMON_GEM_CHANCE = 0.167 * FILLER_ADJUSTMENT;
    private static final double UNCOMMON_GEM_CHANCE = 0.04 * FILLER_ADJUSTMENT;
    private static final double RARE_GEM_CHANCE = 0.01 * FILLER_ADJUSTMENT;
    private static final double CHOCOLATE_COIN_CHANCE = 0.1 * FILLER_ADJUSTMENT;
    private static final double EXTRA_PULLS_CHANCE = 0.001 * FILLER_ADJUSTMENT;

    private static final double ITEM_ROLL_LIMIT = ITEM_CHANCE;
    private static final double COMMON_GEM_ROLL_LIMIT = ITEM_ROLL_LIMIT + COMMON_GEM_CHANCE;
    private static final double UNCOMMON_GEM_ROLL_LIMIT = COMMON_GEM_ROLL_LIMIT + UNCOMMON_GEM_CHANCE;
    private static final double RARE_GEM_ROLL_LIMIT = UNCOMMON_GEM_ROLL_LIMIT + RARE_GEM_CHANCE;
    private static final double CHOCOLATE_COIN_ROLL_LIMIT = RARE_GEM_ROLL_LIMIT + CHOCOLATE_COIN_CHANCE;
    private static final double EXTRA_PULLS_ROLL_LIMIT = CHOCOLATE_COIN_ROLL_LIMIT + EXTRA_PULLS_CHANCE;

    private static GachaUser awardFiller(long uid, long bannerId, GachaResponse response) {
        double roll = HBMain.RNG_SOURCE.nextDouble();
        if (roll < ITEM_ROLL_LIMIT) {
            GachaItems.Item item = GachaItems.generateItem(uid);
            item.awardTo(uid);
            if (item.enhancementLevel > 0) {
                response.addMessagePart(":tools: You pull a rare item! " + item.getBriefDescription());
            } else {
                response.addMessagePart(":hammer: You pull an item: " + item.getBriefDescription());
            }
            return logItemFiller(uid, bannerId);
        } else if (roll < COMMON_GEM_ROLL_LIMIT) {
            GachaGems.Gem gem = GachaGems.Gem.getRandomGem(GachaGems.COMMON_GEM_RARITY);
            GachaItems.logAwardGem(uid, gem.getId());
            response.addMessagePart(":small_blue_diamond: You pull a common gem: " + gem.getName()
                + "\n\t" + gem.getDescription());
            return logCommonGemFiller(uid, bannerId);
        } else if (roll < UNCOMMON_GEM_ROLL_LIMIT) {
            GachaGems.Gem gem = GachaGems.Gem.getRandomGem(GachaGems.UNCOMMON_GEM_RARITY);
            GachaItems.logAwardGem(uid, gem.getId());
            response.addMessagePart(":large_blue_diamond: You pull an uncommon gem: " + gem.getName()
                + "\n\t" + gem.getDescription());
            return logUncommonGemFiller(uid, bannerId);
        } else if (roll < RARE_GEM_ROLL_LIMIT) {
            GachaGems.Gem gem = GachaGems.Gem.getRandomGem(GachaGems.RARE_GEM_RARITY);
            GachaItems.logAwardGem(uid, gem.getId());
            response.addMessagePart(":diamond_shape_with_a_dot_inside: You pull a rare gem! " + gem.getName()
                + "\n\t" + gem.getDescription());
            return logRareGemFiller(uid, bannerId);
        } else if (roll < CHOCOLATE_COIN_ROLL_LIMIT) {
            int coins = HBMain.generateBoundedNormal(100, 50, 2);
            response.chocolateCoinBalance = Casino.addChocolateCoins(uid, coins);
            response.chocolateCoinsAwarded += coins;
            response.addMessagePart(":chocolate_bar: You pull " + coins + " chocolate coins.");
            return logChocolateCoinFiller(uid, bannerId, coins);
        } else if (roll < EXTRA_PULLS_ROLL_LIMIT) {
            final long PULLS_AWARDED = 5;
            response.pullBalance = addPulls(uid, PULLS_AWARDED);
            response.addMessagePart(":stars: You pull " + PULLS_AWARDED + " pulls! Sweet!");
            return logPullFiller(uid, bannerId, PULLS_AWARDED);
        } else { // coins
            int coins = HBMain.generateBoundedNormal(50, 20, 2);
            response.coinBalance = Casino.addMoney(uid, coins);
            response.coinsAwarded += coins;
            response.addMessagePart(":coin: You pull " + coins + " coins.");
            return logCoinFiller(uid, bannerId, coins);
        }
    }

    private static GachaUser awardCharacter(long uid, GachaBanner banner, int rarity,
            GachaResponse response) {
        SHINY_TYPE shiny = banner.generateShinyType();

        List<Long> cids = getEligibleCharacters(uid, banner.bannerId, rarity, shiny);
        if (cids.isEmpty()) {
            // Player has maxed all relevant characters, pick one at random to 'award'
            cids = getAllCharacters(rarity, banner.bannerId);
        }

        if (cids.isEmpty()) {
            response.addMessagePart("Unable to award character: No eligible character ids found. Values {"
                + uid + ", " + banner.bannerId + ", " + rarity + ", " + shiny.getId() + "}");
            return null;
        }

        long cid = cids.get(HBMain.RNG_SOURCE.nextInt(cids.size()));

        boolean alreadyMaxed = awardCharacter(uid, cid, shiny);

        GachaCharacter character = getCharacter(uid, cid, shiny);
        if (character == null) {
            System.out.println("Failed to award character: " + uid + ", " + cid + ", " + shiny);
            response.addMessagePart("Failed to award character. Error: (" + cid + ", " + shiny + ")");
            return null;
        }
        if (alreadyMaxed) {
            response.coinBalance = awardMaxedCharacter(uid, character.getMaxedCharacterCoinEquivalent());
            response.coinsAwarded += character.getMaxedCharacterCoinEquivalent();
        }
        response.addCharacterMessagePart(character.generateAwardText(alreadyMaxed), character.rarity,
            character.getPictureLink());
        return logCharacterAward(uid, banner.bannerId, character.rarity);
    }

    static boolean awardCharacter(long uid, long cid, Gacha.SHINY_TYPE shiny) {
        int duplicates = checkCharacterDuplicates(uid, cid, shiny);
        boolean alreadyMaxed = false;
        if (duplicates < 0) {
            awardNewCharacter(uid, cid, shiny);
        } else if (duplicates < MAX_CHARACTER_DUPLICATES) {
            awardCharacterDuplicate(uid, cid, shiny);
        } else {
            alreadyMaxed = true;
            // Award coins once we have the character object to check value
        }
        return alreadyMaxed;
    }

    private static List<String> createThreeStarAnimation() {
        List<String> frames = baseAnimation();
        frames.add(":black_large_square::black_large_square::star::black_large_square::black_large_square:");
        frames.add(":black_large_square::star::black_large_square::star::black_large_square:");
        frames.add(":black_large_square::star::star::star::black_large_square:");
        frames.add(":black_large_square::star2::star2::star2::black_large_square:");
        frames.add(":black_large_square::star::star::star::black_large_square:");
        frames.add(":black_large_square::star2::star2::star2::black_large_square:");
        frames.add(":black_large_square::star::star::star::black_large_square:");
        frames.add(":black_medium_small_square::star::star::star::black_medium_small_square:");
        frames.add(":black_small_square::star::star::star::black_small_square:");
        return frames;
    }

    private static List<String> createTwoStarAnimation() {
        List<String> frames = baseAnimation();
        frames.add(":black_large_square::black_large_square::star::black_large_square::black_large_square:");
        frames.add(":black_large_square::star::black_large_square::star::black_large_square:");
        frames.add(":black_large_square::star2::black_large_square::star2::black_large_square:");
        frames.add(":black_large_square::star::black_large_square::star::black_large_square:");
        frames.add(":black_medium_small_square::star::black_medium_small_square::star::black_medium_small_square:");
        frames.add(":black_small_square::star::black_small_square::star::black_small_square:");
        return frames;
    }

    private static List<String> createOneStarAnimation() {
        List<String> frames = baseAnimation();
        frames.add(":black_large_square::black_large_square::star::black_large_square::black_large_square:");
        frames.add(":black_medium_small_square::black_medium_small_square::star::black_medium_small_square::black_medium_small_square:");
        frames.add(":black_small_square::black_small_square::star::black_small_square::black_small_square:");
        return frames;
    }

    private static List<String> createAnimation() {
        List<String> frames = baseAnimation();
        frames.add(":black_medium_small_square::black_medium_small_square::black_medium_small_square::black_medium_small_square::black_medium_small_square:");
        frames.add(":black_small_square::black_small_square::black_small_square::black_small_square::black_small_square:");
        return frames;
    }

    private static List<String> baseAnimation() {
        return new ArrayList<>(Arrays.asList(
                ":sparkles::black_large_square::black_large_square::black_large_square::black_large_square:",
                ":black_large_square::sparkles::black_large_square::black_large_square::black_large_square:",
                ":black_large_square::black_large_square::sparkles::black_large_square::black_large_square:",
                ":black_large_square::black_large_square::black_large_square::sparkles::black_large_square:",
                ":black_large_square::black_large_square::black_large_square::black_large_square::sparkles:",
                ":black_large_square::black_large_square::black_large_square::black_large_square::black_large_square:"));
    }

    static String handleCharacterList(long uid) {
        List<GachaCharacter> characters = queryCharacters(uid);
        if (characters.isEmpty()) {
            long pullBalance = getPullCount(uid);
            if (pullBalance < 0) {
                return Casino.USER_NOT_FOUND_MESSAGE;
            } else {
                return "No characters found, but you do have " + pullBalance + " pulls available. Use `/pull` to spend them";
            }
        }

        StringBuilder output = new StringBuilder("Your characters:");
        for (GachaCharacter character : characters) {
            output.append("\n" + character.toString(true));
        }
        return output.toString();
    }

    static String handleCharacterInfo(long uid, String uniqueId) {
        try {
            GachaCharacter character = GachaCharacter.fromUniqueId(uid, uniqueId);
            return character.toString();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    static String handleBannerList(long uid) {
        List<GachaBannerStats> bannerStats = getBannerStats(uid);
        StringBuilder output = new StringBuilder();
        output.append("Available Banners:");
        for (GachaBannerStats banner: bannerStats) {
            output.append('\n');
            output.append(banner.getDisplayString());
        }
        return output.toString();
    }

    static String handleBannerInfo(long server, long uid, long bannerId) {
        GachaBanner banner = getGachaBanner(bannerId);
        if (banner == null) {
            return "Specified banner was not found";
        }
        return banner.getInfoString(server, uid);
    }

    static String handlePity(long uid, long bannerId) {
        long pullBalance = getPullCount(uid);
        GachaUser user = getGachaUser(uid, bannerId);
        if (pullBalance < 0 || user == null) {
            return Casino.USER_NOT_FOUND_MESSAGE;
        }
        GachaBanner banner = getGachaBanner(bannerId);
        if (banner == null) {
            return "Unable to fetch pity: Specified banner was not found";
        }
        return banner.getPityString(user, pullBalance);
    }

    static String handlePulls(long uid) {
        long pullBalance = getPullCount(uid);
        if (pullBalance < 0) {
            return Casino.USER_NOT_FOUND_MESSAGE;
        }
        EventUser eventUser = EventUser.getEventUser(uid);
        if (eventUser == null) {
            return "Unable to fetch EventUser. Potentially bad DB state";
        }
        if (pullBalance > 0) {
            return "You currently have " + pullBalance + " available pull"
                + Casino.getPluralSuffix(pullBalance) + ".\n"
                + eventUser.getAvailablePullSources();
        } else {
            return "No pulls remaining. " + eventUser.getAvailablePullSources();
        }
    }

    static List<HBMain.AutocompleteIdOption> getBanners() {
        return queryBanners();
    }

    static List<HBMain.AutocompleteStringOption> getCharacters(long uid, String substring) {
        return getCharacters(uid, substring, false);
    }

    static List<HBMain.AutocompleteStringOption> getCharacters(long uid, String substring,
            Event.EventType eventType) {
        // TODO: Sort by event stat
        return getCharacters(uid, substring, false);
    }

    static List<HBMain.AutocompleteStringOption> getCharacters(long uid, String substring, boolean withItems) {
        int shiny = -1;
        if (substring.toLowerCase().startsWith(SHINY_TYPE.SHINY.getAdjective().toLowerCase())) {
            shiny = 1;
            substring = substring.substring(SHINY_TYPE.SHINY.getAdjective().length());
        } else if (substring.toLowerCase().startsWith(SHINY_TYPE.PRISMATIC.getAdjective().toLowerCase())) {
            shiny = 2;
            substring = substring.substring(SHINY_TYPE.PRISMATIC.getAdjective().length());
        }
        // Remove leading space so you can use "Foil Test"
        if (substring.startsWith(" ")) { substring = substring.substring(1); }
        List<GachaCharacter> characters = queryCharacters(uid, substring, withItems, shiny);
        List<HBMain.AutocompleteStringOption> output = new ArrayList<>(characters.size());
        characters.forEach(c -> output.add(new HBMain.AutocompleteStringOption(c.getUniqueId(), c.getDisplayName())));
        return output;
    }

    static String handleGiveItem(long uid, String iidString, String uniqueId) {
        long iid = Item.parseItemIdString(iidString);
        if (iid < 0) {
            return "Unable to give item: Unrecognized item id";
        }
        GachaItems.Item item = GachaItems.fetchItem(uid, iid);
        if (item == null) {
            return "Unable to give item: Specified item not found";
        }

        GachaCharacter character;
        try {
            character = GachaCharacter.fromUniqueId(uid, uniqueId);
        } catch (IllegalArgumentException e) {
            return "Unable to give item: " + e.getMessage();
        }

        StringBuilder response = new StringBuilder();
        GachaItems.Item oldItem = character.item;
        if (oldItem != null && oldItem.itemId == item.itemId) {
            return character.name + " is already using " + item.getName();
        }
        GachaCharacter oldCharacter = getCharacterByItem(uid, iid);

        if (oldCharacter != null && !oldCharacter.equals(character)) {
            moveItem(uid, oldCharacter.id, oldCharacter.shiny.getId(), character.id,
                character.shiny.getId(), iid);
        } else {
            giveItem(uid, character.id, character.shiny.getId(), iid);
        }
        character.item = item;

        if (oldCharacter != null) {
            response.append("Unequipped ");
            response.append(item.getName());
            response.append(" from ");
            response.append(oldCharacter.name);
            response.append(".\n");
        }
        if (oldItem != null) {
            response.append("Unequipped ");
            response.append(oldItem.getName());
            response.append(" from ");
            response.append(character.getDisplayName());
            response.append(".\n");
        }
        response.append("Gave ");
        response.append(item.getName());
        response.append(" to ");
        response.append(character.getDisplayName());
        response.append(".\nTheir stats are now: ");
        response.append(character.getTotalStatArray());
        return response.toString();
    }

    static String handleRemoveItem(long uid, String uniqueId) {
        GachaCharacter character;
        try {
            character = GachaCharacter.fromUniqueId(uid, uniqueId);
        } catch (IllegalArgumentException e) {
            return "Unable to remove item: " + e.getMessage();
        }

        if (character.item == null) {
            return "Unable to remove item: Character has no item equipped";
        }

        String itemName = character.item.getName();
        removeItem(uid, character.id, character.shiny.getId());
        return "Unequipped " + itemName + " from " + character.getDisplayName();
    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS gacha_user (
    //   uid bigint PRIMARY KEY,
    //   pulls integer NOT NULL DEFAULT 10,
    //   CONSTRAINT gacha_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_banner (
    //   banner_id SERIAL PRIMARY KEY,
    //   banner_name varchar(40) NOT NULL,
    //   description varchar(100) NOT NULL DEFAULT '',
    //   one_star_chance float8 NOT NULL DEFAULT 0.125,
    //   two_star_chance float8 NOT NULL DEFAULT 0.03125,
    //   three_star_chance float8 NOT NULL DEFAULT 0.0078125,
    //   four_star_chance float8 NOT NULL DEFAULT 0.0,
    //   five_star_chance float8 NOT NULL DEFAULT 0.0,
    //   scaling_one_star_bonus float8 NOT NULL DEFAULT 0.0,
    //   scaling_two_star_bonus float8 NOT NULL DEFAULT 0.21875,
    //   scaling_three_star_bonus float8 NOT NULL DEFAULT 0.2421875,
    //   scaling_four_star_bonus float8 NOT NULL DEFAULT 0.0,
    //   scaling five_star_bonus float8 NOT NULL DEFAULT 0.0,
    //   max_one_star_pity integer NOT NULL DEFAULT 11,
    //   max_two_star_pity integer NOT NULL DEFAULT 47,
    //   max_three_star_pity integer NOT NULL DEFAULT 191,
    //   max_four_star_pity integer NOT NUL DEFAULT 767,
    //   max_five_star_pity integer NOT NULL DEFAULT 3071,
    //   shiny_chance float8 NOT NULL DEFAULT 0.05,
    //   prismatic_chance float8 NOT NULL DEFAULT 0.0,
    //   enabled boolean NOT NULL DEFAULT true
    // );

    // CREATE TABLE IF NOT EXISTS gacha_user_banner (
    //   uid bigint,
    //   banner_id bigint,
    //   times_pulled bigint NOT NULL DEFAULT 0,
    //   one_star_pity integer NOT NULL DEFAULT 0,
    //   two_star_pity integer NOT NULL DEFAULT 0,
    //   three_star_pity integer NOT NULL DEFAULT 0,
    //   four_star_pity integer NOT NULL DEFAULT 0,
    //   five_star_pity integer NOT NULL DEFAULT 0,
    //   one_stars_pulled integer NOT NULL DEFAULT 0,
    //   two_stars_pulled integer NOT NULL DEFAULT 0,
    //   three_stars_pulled integer NOT NULL DEFAULT 0,
    //   four_stars_pulled integer NOT NULL DEFAULT 0,
    //   five_stars_pulled integer NOT NULL DEFAULT 0,
    //   items_pulled integer NOT NULL DEFAULT 0,
    //   common_gems_pulled integer NOT NULL DEFAULT 0,
    //   uncommon_gems_pulled integer NOT NULL DEFAULT 0,
    //   rare_gems_pulled integer NOT NULL DEFAULT 0,
    //   coins_pulled bigint NOT NULL DEFAULT 0,
    //   chocolate_coins_pulled bigint NOT NULL DEFAULT 0,
    //   pulls_pulled integer NOT NULL DEFAULT 0,
    //   PRIMARY KEY(uid, banner_id),
    //   CONSTRAINT gacha_user_banner_uid FOREIGN KEY(uid) REFERENCES gacha_user(uid),
    //   CONSTRAINT gacha_user_banner_banner_id FOREIGN KEY(banner_id) REFERENCES gacha_banner(banner_id)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_character (
    //   cid SERIAL PRIMARY KEY,
    //   name varchar(40) NOT NULL,
    //   rarity integer NOT NULL DEFAULT 1,
    //   type varchar(40) NOT NULL,
    //   rob_bonus float8 NOT NULL DEFAULT 1.0,
    //   pick_bonus float8 NOT NULL DEFAULT 1.0,
    //   fish_bonus float8 NOT NULL DEFAULT 1.0,
    //   work_bonus float8 NOT NULL DEFAULT 1.0,
    //   misc_bonus float8 NOT NULL DEFAULT 1.0,
    //   description varchar(200) NOT NULL DEFAULT '',
    //   picture_url varchar(100) NOT NULL,
    //   shiny_picture_url varchar(100) NOT NULL,
    //   prismatic_picture_url varchar(100) NOT NULL DEFAULT '',
    //   enabled boolean NOT NULL DEFAULT true
    // );

    // CREATE INDEX gacha_character_lowercase_name ON gacha_character (LOWER(name));

    // CREATE TABLE IF NOT EXISTS gacha_alternate_art (
    //   cid bigint PRIMARY KEY,
    //   alternate_picture_url varchar(100) DEFAULT NULL,
    //   alternate_shiny_picture_url varchar(100) DEFAULT NULL,
    //   alternate_prismatic_picture_url varchar(100) DEFAULT NULL,
    //   CONSTRAINT gacha_alternate_art_cid FOREIGN KEY(cid) REFERENCES gacha_character(cid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_user_alternate_art (
    //   uid bigint,
    //   cid bigint,
    //   alternate_picture boolean NOT NULL DEFAULT false,
    //   alternate_shiny_picture boolean NOT NULL DEFAULT false,
    //   alternate_prismatic_picture boolean NOT NULL DEFAULT false,
    //   PRIMARY KEY(uid, cid),
    //   CONSTRAINT gacha_user_alternate_art_cid FOREIGN KEY(cid) REFERENCES gacha_character(cid),
    //   CONSTRAINT gacha_user_alternate_uid FOREIGN KEY(uid) REFERENCES gacha_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_character_banner (
    //   cid bigint,
    //   banner_id bigint,
    //   PRIMARY KEY(cid, banner_id),
    //   CONSTRAINT gacha_character_banner_cid FOREIGN KEY(cid) REFERENCES gacha_character(cid),
    //   CONSTRAINT gacha_character_banner_banner_id FOREIGN KEY(banner_id) REFERENCES gacha_banner(banner_id)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_user_character (
    //   uid bigint,
    //   cid bigint,
    //   foil integer DEFAULT 0,
    //   duplicates integer NOT NULL DEFAULT 0,
    //   level integer NOT NULL DEFAULT 0,
    //   xp integer NOT NULL DEFAULT 0,
    //   iid bigint DEFAULT NULL,
    //   PRIMARY KEY(uid, cid, foil),
    //   CONSTRAINT gacha_user_character_uid FOREIGN KEY(uid) REFERENCES gacha_user(uid),
    //   CONSTRAINT gacha_user_character_cid FOREIGN KEY(cid) REFERENCES gacha_character(cid),
    //   CONSTRAINT gacha_user_character_iid FOREIGN KEY(uid, iid) REFERENCES gacha_item(uid, iid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_gem (
    //   gid SERIAL PRIMARY KEY,
    //   name varchar(40) NOT NULL,
    //   adjective varchar(40) NOT NULL,
    //   effect1 float NOT NULL DEFAULT 0.0,
    //   effect2 float NOT NULL DEFAULT 0.0,
    //   effect3 float NOT NULL DEFAULT 0.0,
    //   effect4 float NOT NULL DEFAULT 0.0,
    //   effect5 float NOT NULL DEFAULT 0.0,
    //   is_special boolean NOT NULL DEFAULT false,
    // );

    // CREATE TABLE IF NOT EXISTS gacha_user_gem (
    //   uid bigint,
    //   gid bigint,
    //   owned integer NOT NULL DEFAULT 0,
    //   used integer NOT NULL DEFAULT 0,
    //   PRIMARY_KEY(uid, gid),
    //   CONSTRAINT gacha_user_gem_uid FOREIGN KEY(uid) REFERENCES gacha_user(uid),
    //   CONSTRAINT gacha_user_gem_gid FOREIGN KEY(gid) REFERENCES gacha_gem(gid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_user_character_gem (
    //   uid bigint,
    //   cid bigint,
    //   gid bigint,
    //   times_used integer NOT NULL DEFAULT 0,
    //   PRIMARY_KEY(uid, cid, gid),
    //   CONSTRAINT gacha_user_character_gem_uid FOREIGN KEY(uid) REFERENCES gacha_user(uid),
    //   CONSTRAINT gacha_user_character_gem_cid FOREIGN KEY(cid) REFERENCES gacha_character(cid),
    //   CONSTRAINT gacha_user_character_gem_gid FOREIGN KEY(gid) REFERENCES gacha_gem(gid)
    // );

    static final String GACHA_USER_BANNER_COLUMNS = "times_pulled, one_star_pity, two_star_pity, three_star_pity, four_star_pity, five_star_pity";

    private static String getPartialCharacterQuery(long uid) {
        return "SELECT cid, name, rarity, foil, type, level, xp, duplicates, description, "
        + "COALESCE(alternate_art, picture_url) AS picture_url, COALESCE(alternate_shiny_art, shiny_picture_url) AS shiny_picture_url, "
        + "COALESCE(alternate_prismatic_art, prismatic_picture_url) AS prismatic_picture_url, work_bonus, fish_bonus, pick_bonus, "
        + "rob_bonus, misc_bonus, iid FROM gacha_user_character NATURAL JOIN gacha_character NATURAL LEFT JOIN (SELECT cid, CASE "
        + "WHEN alternate_picture THEN alternate_picture_url ELSE NULL END AS alternate_art, CASE WHEN alternate_shiny_picture THEN "
        + "alternate_shiny_picture_url ELSE NULL END AS alternate_shiny_art, CASE WHEN alternate_prismatic_picture THEN "
        + "alternate_prismatic_picture_url ELSE NULL END AS alternate_prismatic_art FROM gacha_alternate_art NATURAL LEFT JOIN "
        + "gacha_user_alternate_art WHERE uid = " + uid + ") AS art";
    }

    private static GachaUser getGachaUser(long uid, long bannerId) {
        String query = "SELECT " + GACHA_USER_BANNER_COLUMNS + " FROM gacha_user NATURAL JOIN gacha_user_banner WHERE uid = "
            + uid + " AND banner_id = " + bannerId + ";";
        GachaUser result = getGachaUser(query);
        if (result == null) {
            String insertQuery = "INSERT INTO gacha_user_banner (uid, banner_id) VALUES ("
                + uid + ", " + bannerId + ") ON CONFLICT (uid, banner_id) DO NOTHING RETURNING "
                + GACHA_USER_BANNER_COLUMNS + ";";
            return getGachaUser(insertQuery);
        }
        return result;
    }

    private static GachaUser getGachaUser(String query) {
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                GachaUser user = new GachaUser();
                user.timesPulled = results.getInt(1);
                user.oneStarPity = results.getInt(2);
                user.twoStarPity = results.getInt(3);
                user.threeStarPity = results.getInt(4);
                user.fourStarPity = results.getInt(5);
                user.fiveStarPity = results.getInt(6);
                return user;
            }
            return null;
        }, null);
    }

    static GachaBanner getGachaBanner(long bannerId) {
        String query = "SELECT banner_id, one_star_chance, two_star_chance, three_star_chance, four_star_chance, "
            + "five_star_chance, shiny_chance, prismatic_chance, scaling_one_star_bonus, scaling_two_star_bonus, "
            + "scaling_three_star_bonus, scaling_four_star_bonus, scaling_five_star_bonus, max_one_star_pity, "
            + "max_two_star_pity, max_three_star_pity, max_four_star_pity, max_five_star_pity, enabled, banner_name, description "
            + "FROM gacha_banner WHERE banner_id = " + bannerId + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                GachaBanner banner = new GachaBanner();
                banner.bannerId = results.getInt(1);
                banner.oneStarChance = results.getDouble(2);
                banner.twoStarChance = results.getDouble(3);
                banner.threeStarChance = results.getDouble(4);
                banner.fourStarChance = results.getDouble(5);
                banner.fiveStarChance = results.getDouble(6);
                banner.shinyChance = results.getDouble(7);
                banner.prismaticChance = results.getDouble(8);
                banner.scalingOneStarBonus = results.getDouble(9);
                banner.scalingTwoStarBonus = results.getDouble(10);
                banner.scalingThreeStarBonus = results.getDouble(11);
                banner.scalingFourStarBonus = results.getDouble(12);
                banner.scalingFiveStarBonus = results.getDouble(13);
                banner.maxOneStarPity = results.getInt(14);
                banner.maxTwoStarPity = results.getInt(15);
                banner.maxThreeStarPity = results.getInt(16);
                banner.maxFourStarPity = results.getInt(17);
                banner.maxFiveStarPity = results.getInt(18);
                banner.enabled = results.getBoolean(19);
                banner.name = results.getString(20);
                banner.description = results.getString(21);
                return banner;
            }
            return null;
        }, null);
    }

    private static GachaCharacter getCharacter(long uid, long cid, SHINY_TYPE shiny) {
        String query = getPartialCharacterQuery(uid) + " WHERE uid = " + uid + " AND cid = " + cid
                + " AND foil = " + shiny.getId() + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                GachaItems.Item item = null;
                if (results.getString(18) != null) {
                    item = GachaItems.fetchItem(uid, results.getLong(18));
                }
                return new GachaCharacter(results.getLong(1), results.getString(2), results.getInt(3), SHINY_TYPE.fromId(results.getInt(4)),
                        results.getString(5), results.getInt(6), results.getInt(7), results.getInt(8),
                        results.getString(9), results.getString(10), results.getString(11), results.getString(12),
                        results.getLong(13), results.getLong(14), results.getLong(15), results.getLong(16),
                        results.getLong(17), item);
            }
            return null;
        }, null);
    }

    static GachaCharacter getCharacterByItem(long uid, long iid) {
        String query = getPartialCharacterQuery(uid) + " WHERE uid = " + uid + " AND iid = " + iid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                GachaItems.Item item = null;
                if (results.getString(18) != null) {
                    item = GachaItems.fetchItem(uid, results.getLong(18));
                }
                return new GachaCharacter(results.getLong(1), results.getString(2), results.getInt(3), SHINY_TYPE.fromId(results.getInt(4)),
                        results.getString(5), results.getInt(6), results.getInt(7), results.getInt(8),
                        results.getString(9), results.getString(10), results.getString(11), results.getString(12),
                        results.getLong(13), results.getLong(14), results.getLong(15), results.getLong(16),
                        results.getLong(17), item);
            }
            return null;
        }, null);
    }

    static List<GachaCharacter> queryCharacters(long uid) {
        return queryCharacters(uid, "");
    }

    private static List<GachaCharacter> queryCharacters(long uid, String substring) {
        return queryCharacters(uid, substring, false, -1);
    }

    private static List<GachaCharacter> queryCharacters(long uid, String substring, boolean withItems, int foil) {
        if (substring == null) { substring = ""; }
        substring = '%' + substring + '%';
        String query = getPartialCharacterQuery(uid) + " WHERE uid = " + uid;
        if (withItems) { query += " AND iid IS NOT NULL"; }
        if (foil >= 0) { query += " AND foil = " + foil; }
        query += " AND LOWER(name) LIKE LOWER(?) ORDER BY rarity DESC, foil DESC, name ASC;";
        return CasinoDB.executeValidatedQueryWithReturn(query, results -> {
            List<GachaCharacter> output = new ArrayList<>();
            while (results.next()) {
                // This is inefficient and should be refactored to involve fewer queries
                GachaItems.Item item = null;
                if (results.getString(18) != null) {
                    item = GachaItems.fetchItem(uid, results.getLong(18));
                }
                output.add(new GachaCharacter(results.getLong(1), results.getString(2), results.getInt(3), SHINY_TYPE.fromId(results.getInt(4)),
                        results.getString(5), results.getInt(6), results.getInt(7), results.getInt(8),
                        results.getString(9), results.getString(10), results.getString(11), results.getString(12),
                        results.getLong(13), results.getLong(14), results.getLong(15), results.getLong(16),
                        results.getLong(17), item));
            }
            return output;
        }, new ArrayList<>(), substring);
    }

    private static List<Long> getEligibleCharacters(long uid, long bannerId, int rarity, SHINY_TYPE shiny) {
        return CasinoDB.executeListQuery("SELECT cid FROM gacha_character NATURAL JOIN gacha_character_banner "
                + "WHERE rarity = " + rarity + " AND enabled = TRUE and banner_id = " + bannerId
                + "AND NOT EXISTS(SELECT 1 FROM gacha_user_character "
                + "WHERE gacha_user_character.cid = gacha_character.cid AND duplicates >= "
                + MAX_CHARACTER_DUPLICATES + " AND foil = " + shiny.getId() + " AND uid = " + uid + ");");
    }

    private static List<Long> getAllCharacters(int rarity, long bannerId) {
        return CasinoDB.executeListQuery("SELECT cid FROM gacha_character NATURAL JOIN gacha_character_banner "
            + "WHERE banner_id = " + bannerId + " AND rarity = " + rarity + " AND enabled = TRUE;");
    }

    private static int checkCharacterDuplicates(long uid, long cid, SHINY_TYPE shiny) {
        int isOwned = CasinoDB.executeIntQuery("SELECT COUNT(uid) FROM gacha_user_character WHERE uid = "
                + uid + " AND cid = " + cid + "AND foil = " + shiny.getId() + ";");
        // Shouldn't ever be negative, but doesn't hurt to catch it
        if (isOwned <= 0) {
            return -1;
        } else {
            return CasinoDB.executeIntQuery("SELECT duplicates FROM gacha_user_character WHERE uid = "
                    + uid + " AND cid = " + cid + "AND foil = " + shiny.getId() + ";");
        }
    }

    private static void awardNewCharacter(long uid, long cid, SHINY_TYPE shiny) {
        CasinoDB.executeUpdate("INSERT INTO gacha_user_character(uid, cid, foil) VALUES ("
                + uid + ", " + cid + ", " + shiny.getId()
                + ") ON CONFLICT (uid, cid, foil) DO NOTHING;");
    }

    private static void awardCharacterDuplicate(long uid, long cid, SHINY_TYPE shiny) {
        CasinoDB.executeUpdate("UPDATE gacha_user_character SET (duplicates) = (duplicates + 1) WHERE uid = "
                + uid + " AND cid = " + cid + " AND foil = " + shiny.getId() + ";");
    }

    private static long awardMaxedCharacter(long uid, long coinEquivalent) {
        return Casino.addMoney(uid, coinEquivalent);
    }

    static long getPullCount(long uid) {
        return CasinoDB.executeLongQuery("SELECT pulls FROM gacha_user WHERE uid = " + uid + ";");
    }

    static long getTotalTimesPulled(long uid) {
        return CasinoDB.executeLongQuery("SELECT SUM(times_pulled) FROM gacha_user_banner WHERE uid = " + uid + ";");
    }

    static long addPulls(long uid, long amount) {
        return CasinoDB.executeLongQuery("UPDATE gacha_user SET (pulls) = (pulls + " + amount
                + ") WHERE uid = " + uid + " RETURNING pulls;");
    }

    static long removePulls(long uid, long amount) {
        return addPulls(uid, -1 * amount);
    }

    private static GachaUser logItemFiller(long uid, long bannerId) {
        return logFillerPull(uid, bannerId, "items_pulled", 1);
    }

    private static GachaUser logCommonGemFiller(long uid, long bannerId) {
        return logFillerPull(uid, bannerId, "common_gems_pulled", 1);
    }

    private static GachaUser logUncommonGemFiller(long uid, long bannerId) {
        return logFillerPull(uid, bannerId, "uncommon_gems_pulled", 1);
    }

    private static GachaUser logRareGemFiller(long uid, long bannerId) {
        return logFillerPull(uid, bannerId, "rare_gems_pulled", 1);
    }

    private static GachaUser logChocolateCoinFiller(long uid, long bannerId, long coins) {
        return logFillerPull(uid, bannerId, "chocolate_coins_pulled", coins);
    }

    private static GachaUser logPullFiller(long uid, long bannerId, long pulls) {
        return logFillerPull(uid, bannerId, "pulls_pulled", pulls);
    }

    private static GachaUser logCoinFiller(long uid, long bannerId, long coins) {
        return logFillerPull(uid, bannerId, "coins_pulled", coins);
    }

    private static GachaUser logFillerPull(long uid, long bannerId, String column, long amount) {
        return getGachaUser("UPDATE gacha_user_banner SET (times_pulled, one_star_pity, two_star_pity, three_star_pity, "
            + "four_star_pity, five_star_pity, " + column + ") = (times_pulled + 1, one_star_pity + 1, two_star_pity + 1, "
            + "three_star_pity + 1, four_star_pity + 1, five_star_pity + 1, " + column + " + " + amount + ") WHERE uid = "
            + uid + " AND banner_id = " + bannerId + " RETURNING " + GACHA_USER_BANNER_COLUMNS + ";");
    }

    private static GachaUser logCharacterAward(long uid, long bannerId, int rarity) {
        String fiveStarPityResult = (rarity == 5 ? "0" : "five_star_pity + 1");
        String fourStarPityResult = (rarity == 4 ? "0" : (rarity > 4 ? "four_star_pity" : "four_star_pity + 1"));
        String threeStarPityResult = (rarity == 3 ? "0" : (rarity > 3 ? "three_star_pity" : "three_star_pity + 1"));
        String twoStarPityResult = (rarity == 2 ? "0" : (rarity > 2 ? "two_star_pity" : "two_star_pity + 1"));
        String oneStarPityResult = (rarity == 1 ? "0" : (rarity > 1 ? "one_star_pity" : "one_star_pity + 1"));
        String rarityPulled = "";
        switch (rarity) {
            case 5:
                rarityPulled = "five_stars_pulled";
                break;
            case 4:
                rarityPulled = "four_stars_pulled";
                break;
            case 3:
                rarityPulled = "three_stars_pulled";
                break;
            case 2:
                rarityPulled = "two_stars_pulled";
                break;
            case 1:
            default:
                rarityPulled = "one_stars_pulled";
                break;
        }
        return getGachaUser("UPDATE gacha_user_banner SET (times_pulled, one_star_pity, two_star_pity, three_star_pity, "
            + " four_star_pity, five_star_pity, " + rarityPulled + ") = (times_pulled + 1, " + oneStarPityResult + ", "
            + twoStarPityResult + ", " + threeStarPityResult + ", " + fourStarPityResult + ", " + fiveStarPityResult + ", "
            + rarityPulled + " + 1) WHERE uid = " + uid + " AND banner_id = " + bannerId + " RETURNING "
            + GACHA_USER_BANNER_COLUMNS + ";");
    }

    private static List<HBMain.AutocompleteIdOption> queryBanners() {
        return CasinoDB.executeAutocompleteIdQuery("SELECT banner_id, banner_name FROM gacha_banner WHERE enabled = true;");
    }

    private static List<GachaBannerCharacter> getBannerCharacters(long bannerId, long server, long uid) {
        String query = "WITH server_characters AS (SELECT cid FROM gacha_user_character WHERE uid IN "
            + "(SELECT uid FROM casino_server_user WHERE server_id = " + server + ")) "
            + "SELECT CASE WHEN cid IN (SELECT cid FROM server_characters) THEN name ELSE '?????' END, "
            + "rarity, type, foil, duplicates FROM (SELECT cid, foil, duplicates FROM gacha_user_character "
            + "WHERE uid = " + uid + ") AS u NATURAL RIGHT JOIN gacha_character_banner NATURAL JOIN "
            + "gacha_character WHERE banner_id = " + bannerId + " ORDER BY rarity DESC, name ASC, foil ASC;";
        return CasinoDB.executeQueryWithReturn(query, results -> {
                List<GachaBannerCharacter> output = new ArrayList<>();
                while (results.next()) {
                    // Have to call this for wasNull() to work
                    results.getInt(4);
                    if (results.wasNull()) {
                        output.add(new GachaBannerCharacter(results.getString(1), results.getInt(2), results.getString(3)));
                    } else {
                        output.add(new GachaBannerCharacter(results.getString(1), results.getInt(2), results.getString(3),
                            results.getInt(4), results.getInt(5)));
                    }
                }
                return output;
            }, new ArrayList<>());
    }

    private static List<GachaBannerStats> getBannerStats(long uid) {
        return CasinoDB.executeQueryWithReturn("SELECT banner_name, "
            + "(SELECT COUNT(*) FROM gacha_character_banner WHERE gacha_character_banner.banner_id = gacha_banner.banner_id) AS banner_characters, "
            + "(SELECT COUNT(DISTINCT cid) FROM gacha_user_character NATURAL JOIN gacha_character_banner WHERE banner_id = gacha_banner.banner_id AND uid = "
                + uid + ") AS owned, "
            + "(SELECT COUNT(DISTINCT cid) FROM gacha_user_character NATURAL JOIN gacha_character_banner WHERE banner_id = gacha_banner.banner_id AND uid = "
                + uid + " AND duplicates >= " + MAX_CHARACTER_DUPLICATES + ") AS maxed "
            + "FROM gacha_banner WHERE enabled = true;", results -> {
                List<GachaBannerStats> output = new ArrayList<>();
                while (results.next()) {
                    output.add(new GachaBannerStats(results.getString(1), results.getInt(2), results.getInt(3), results.getInt(4)));
                }
                return output;
            }, new ArrayList<>());
    }

    private static void giveItem(long uid, long cid, int foil, long iid) {
        CasinoDB.executeUpdate("UPDATE gacha_user_character SET iid = " + iid + " WHERE cid = " + cid
            + " AND foil = " + foil + " AND uid = " + uid + ";");
    }

    private static void moveItem(long uid, long oldCid, int oldFoil, long newCid, int newFoil, long iid) {
        CasinoDB.executeUpdate("UPDATE gacha_user_character AS g SET iid = c.iid FROM (VALUES (" + newCid + ", " + newFoil
            + ", " + iid + "), (" + oldCid + ", " + oldFoil + ", NULL)) AS c(cid, foil, iid) "
            + "WHERE g.cid = c.cid AND g.foil = c.foil AND g.uid = " + uid + ";");
    }

    private static void removeItem(long uid, long cid, int foil) {
        CasinoDB.executeUpdate("UPDATE gacha_user_character SET iid = NULL WHERE uid = "
            + uid + " AND cid = " + cid + " AND foil = " + foil + ";");
    }
}
