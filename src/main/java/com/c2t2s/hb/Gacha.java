package com.c2t2s.hb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.MalformedURLException;
import java.net.URL;

class Gacha {

    // Hide default constructor
    private Gacha() {}

    private static class GachaUser {
        int primaryPity;
        int secondaryPity;
        int tertiaryPity;
        int bannerFlops;
        int pulls;
        int timesPulled;

        private GachaUser(int primaryPity, int secondaryPity, int tertiaryPity,
                int bannerFlops, int pulls, int timesPulled) {
            this.primaryPity = primaryPity;
            this.secondaryPity = secondaryPity;
            this.tertiaryPity = tertiaryPity;
            this.bannerFlops = bannerFlops;
            this.pulls = pulls;
            this.timesPulled = timesPulled;
        }

        // Returns chance to pull a 3 Star
        private double getThreeStarChance() {
            if (primaryPity >= (MAX_3STAR_PITY)) {
                return 1.0;
            }
            int averagePulls = (int)(1.0 / BASE_3STAR_CHANCE);
            if (primaryPity > averagePulls) {
                return BASE_3STAR_CHANCE
                    + 0.25 * (((double)primaryPity - averagePulls) / (MAX_3STAR_PITY - averagePulls));
            } else {
                return BASE_3STAR_CHANCE;
            }
        }

        // Returns chance to pull a 2 Star
        private double getTwoStarChance() {
            if (secondaryPity >= (MAX_2STAR_PITY)) {
                return 1.0;
            }
            int averagePulls = (int)(1.0 / BASE_2STAR_CHANCE);
            if (secondaryPity > averagePulls) {
                return BASE_2STAR_CHANCE
                    + 0.25 * (((double)secondaryPity - averagePulls) / (MAX_2STAR_PITY - averagePulls));
            } else {
                return BASE_2STAR_CHANCE;
            }
        }

        // Returns chance to pull a 1 Star
        private double getOneStarChance() {
            if (tertiaryPity >= (MAX_1STAR_PITY)) {
                return 1.0;
            } else {
                return BASE_1STAR_CHANCE;
            }
        }

        private String getPityString() {
            return "3 Star Pity: " + primaryPity + "/" + (MAX_3STAR_PITY + 1)
                + "\n2 Star Pity: " + secondaryPity + "/" + (MAX_2STAR_PITY + 1)
                + "\n1 Star Pity: " + tertiaryPity + "/" + (MAX_1STAR_PITY + 1)
                + "\nAvailable Pulls: " + pulls;
        }

        private int getBannerFlops() {
            return bannerFlops;
        }

        private int getTimesPulled() {
            return timesPulled;
        }
    }

    private static class GachaCharacter {

        private String name;
        private int rarity;
        private int foil;
        private String type;
        private int level;
        private int xp;
        private int duplicates;
        private String description;
        private String pictureUrl;
        private String shinyUrl;

        private GachaCharacter(String name, int rarity, int foil, String type, int level,
                int xp, int duplicates, String description, String pictureUrl, String shinyUrl) {
            this.name = name;
            this.rarity = rarity;
            this.foil = foil;
            this.type = type;
            this.level = level;
            this.xp = xp;
            this.duplicates = duplicates;
            this.description = description;
            this.pictureUrl = pictureUrl;
            this.shinyUrl = shinyUrl;

            if (duplicates > MAX_CHARACTER_DUPLICATES) {
                duplicates = MAX_CHARACTER_DUPLICATES;
            }
            if (level > MAX_CHARACTER_LEVEL) {
                level = MAX_CHARACTER_LEVEL;
            }
        }

        private String getDisplayName() {
            return (foil == 1 ? "Shiny " : "") + name;
        }

        private String getPictureLink() {
            return (foil == 1 ? shinyUrl : pictureUrl);
        }

        private String toAbbreviatedString() {
            return getDisplayName() + (duplicates > 0 ? " +" + duplicates : "")
                    + " (" + rarity + " Star " + type + ") - Level " + level
                    + (level < MAX_CHARACTER_LEVEL ? " [" + xp + "/" + getXpToLevel() + "]" : " [Max Level]")
                    + " - +" + getBuffPercent() + "% Bonus";
        }

        private String toFullString() {
            return getDisplayName() + (duplicates > 0 ? " +" + duplicates : "")
                    + "\n\t" + rarity + " Star " + type
                    + "\n\tLevel " + level + (level < MAX_CHARACTER_LEVEL ? " [" + xp + "/" + getXpToLevel() + "]" : " [Max Level]")
                    + getPictureLink()
                    + "\n\t+" + getBuffPercent() + "% Bonus"
                    + (description.isEmpty() ? "" : "\n\t" + description);
        }

        private String generateAwardText(boolean useBriefResponse, boolean alreadyMaxed) {
            String star = (foil == 1 ? ":star2:" : ":star:");
            String stars = "";
            if (rarity > 0) {
                stars = Casino.repeatString(star, rarity);
            }
            String duplicateString = "";
            if (alreadyMaxed) {
                long coinEquivalent = getMaxedCharacterCoinEquivalent();
                duplicateString = "\n" + (useBriefResponse ? "\t" : "") + getDisplayName() + " +" + duplicates
                    + " already maxed, converted to " + coinEquivalent + " coin" + Casino.getPluralSuffix(coinEquivalent);
            } else if (duplicates > 0) {
                duplicateString = "\n" + (useBriefResponse ? "\t" : "") + "Upgraded " + getDisplayName()
                    + (duplicates > 1 ? " +" + (duplicates - 1) : "")
                    + " -> " + getDisplayName() + " +" + duplicates;
            }
            if (useBriefResponse) {
                return stars + " " + getDisplayName() + " " + stars + " (" + rarity + " Star " + type + ")"
                    + duplicateString //+ getPictureLink()
                    + (!description.isEmpty() ? "\n\tThis character has a unique ability" : "");
            } else {
                return stars + " " + getDisplayName() + " " + stars
                    + "\n" + rarity + " Star " + type
                    + duplicateString // + getPictureLink()
                    + (!description.isEmpty() ? "\n\tUnique Ability:" + description : "");
            }
        }

        private int getXpToLevel() {
            // Level 0 -> Level 1: 100 xp
            // Every level beyond doubles previous level
            return 50 * (int)Math.pow(2, level + 1);
        }

        private double getBuffPercent() {
            return 2.5 + (rarity * 2.5) + (duplicates * 1.0) + (level * 1.0);
        }

        private double getBuffDecimal() {
            return getBuffPercent() / 100;
        }

        private long getMaxedCharacterCoinEquivalent() {
            if (rarity < MAXED_CHARACTER_COIN_VALUES.size()) {
                return MAXED_CHARACTER_COIN_VALUES.get(rarity);
            }
            return 100L;
        }
    }

    static class GachaResponse {
        private String partialMessage = "";
        private HBMain.MultistepResponse messages = new HBMain.MultistepResponse();

        private long coinsAwarded = 0;
        private long coinBalance = 0;
        private long chocolateCoinsAwarded = 0;
        private long chocolateCoinBalance = 0;
        private int highestCharacterAwarded = 0;

        private void addMessagePart(String message) {
            partialMessage += (!partialMessage.isEmpty() ? "\n" : "") + message;
            messages.addMessage(partialMessage);
        }

        private void addCharacterMessagePart(String message, int rarity, String pictureUrl) {
            if (rarity > highestCharacterAwarded) {
                highestCharacterAwarded = rarity;
            }
            try {
                messages.images.put(messages.messages.size(), new URL(pictureUrl));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            addMessagePart(message);
        }

        private void addAnimation(List<String> frames) {
            int size = frames.size();
            Map<Integer, URL> adjustedImages = new HashMap<>();
            for (int key: messages.images.keySet()) {
                adjustedImages.put(key + size, messages.images.get(key));
            }
            messages.images = adjustedImages;
            messages.addAllToStart(frames);
        }
    }

    private static final int MAX_CHARACTER_LEVEL = 5;
    private static final int MAX_CHARACTER_DUPLICATES = 5;
    private static final int MAX_3STAR_PITY = 191;
    private static final int MAX_2STAR_PITY = 47;
    private static final int MAX_1STAR_PITY = 11;
    private static final int MAX_BANNER_FLOPS = 2;
    private static final double BASE_3STAR_CHANCE = 0.0078125;
    private static final double BASE_2STAR_CHANCE = 0.03125;
    private static final double BASE_1STAR_CHANCE = 0.125;
    private static final double BANNER_3STAR_CHANCE = 0.5;
    private static final double BANNER_2STAR_CHANCE = 0.6;
    private static final double SHINY_CHANCE = 0.05;
    private static final List<Long> MAXED_CHARACTER_COIN_VALUES = Arrays.asList(0L, 100L, 500L, 1000L);

    static HBMain.MultistepResponse handleGachaPull(long uid, boolean onBanner, long pulls) {
        GachaUser user = getGachaUser(uid);
        if (user == null) {
            return new HBMain.MultistepResponse(Casino.USER_NOT_FOUND_MESSAGE);
        }

        Events.EventUser eventUser = Events.getEventUser(uid);
        if (eventUser == null) {
            return new HBMain.MultistepResponse("Unable to pull. Insufficient pulls and unable to fetch EventUser.");
        }

        if (user.pulls < 1) {
            return new HBMain.MultistepResponse("No pulls remaining. " + eventUser.getAvailablePullSources());
        } else if (user.pulls < pulls) {
            return new HBMain.MultistepResponse("Insufficient pulls, you have " + user.pulls + " pulls remaining. "
                + eventUser.getAvailablePullSources());
        }

        boolean useBriefResponse = (pulls != 1);
        GachaUser updatedUser;
        GachaResponse response = new GachaResponse();

        for (int i = 0; i < pulls; ++i) {
            if (HBMain.RNG_SOURCE.nextDouble() <= user.getThreeStarChance()) {
                updatedUser = awardThreeStar(uid, onBanner, user.getBannerFlops(), useBriefResponse, response);
            } else if (HBMain.RNG_SOURCE.nextDouble() <= user.getTwoStarChance()) {
                updatedUser = awardTwoStar(uid, onBanner, useBriefResponse, response);
            // First roll always gives at least 1 star
            } else if (HBMain.RNG_SOURCE.nextDouble() <= user.getOneStarChance() || user.getTimesPulled() == 0) { 
                updatedUser = awardOneStar(uid, onBanner, useBriefResponse, response);
            } else {
                updatedUser = awardFiller(uid, onBanner, response);
            }
            if (updatedUser != null) {
                user = updatedUser;
            }
        }

        List<String> animation;
        switch (response.highestCharacterAwarded) {
            case 1:
                animation = createOneStarAnimation();
                break;
            case 2:
                animation = createTwoStarAnimation();
                break;
            case 3:
                animation = createThreeStarAnimation();
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
        balances += "\nYou have " + user.pulls + " pull"
            + (user.pulls != 1 ? "s" : "") + " remaining";
        response.addMessagePart(balances);

        return response.messages;
    }

    private static GachaUser awardOneStar(long uid, boolean fromBanner, boolean useBriefResponse,
            GachaResponse response) {
        List<Long> cids = null;
        boolean shiny = HBMain.RNG_SOURCE.nextDouble() < SHINY_CHANCE;
        cids = getEligibleCharacters(uid, 1, shiny);
        if (cids.isEmpty()) {
            // Player has maxed all relevant 1 Stars, pick one at random to 'award'
            cids = getAllCharacters(1);
        }

        if (cids.isEmpty()) {
            response.addMessagePart("Unable to award 1 star: No eligible character ids found");
            return null;
        }

        long cid = cids.get(HBMain.RNG_SOURCE.nextInt(cids.size()));
        return awardCharacter(uid, cid, shiny, fromBanner, false, useBriefResponse, response);
    }

    private static GachaUser awardTwoStar(long uid, boolean fromBanner, boolean useBriefResponse,
            GachaResponse response) {
        List<Long> cids = null;
        boolean shiny = HBMain.RNG_SOURCE.nextDouble() < SHINY_CHANCE;
        if (fromBanner && (HBMain.RNG_SOURCE.nextDouble() < BANNER_2STAR_CHANCE)) {
            // Award from banner
            cids = getEligibleBanner2Stars(uid, shiny);
        }
        // If there were no eligible banner characters, fall through and pull from all options
        if (cids == null || cids.isEmpty()){
            cids = getEligibleCharacters(uid, 2, shiny);
            if (cids.isEmpty()) {
                // Player has maxed all relevant 2 Stars, pick one at random to 'award'
                cids = getAllCharacters(2);
            }
        }

        if (cids.isEmpty()) {
            response.addMessagePart("Unable to award 2 star: No eligible character ids found");
            return null;
        }

        long cid = cids.get(HBMain.RNG_SOURCE.nextInt(cids.size()));
        return awardCharacter(uid, cid, shiny, fromBanner, false, useBriefResponse, response);
    }

    private static GachaUser awardThreeStar(long uid, boolean fromBanner, int bannerFlops,
            boolean useBriefResponse, GachaResponse response) {
        List<Long> cids = null;
        boolean shiny = HBMain.RNG_SOURCE.nextDouble() < SHINY_CHANCE;
        boolean isBannerFlop = false;
        if (fromBanner && (bannerFlops > 0 || HBMain.RNG_SOURCE.nextDouble() < BANNER_3STAR_CHANCE)) {
            // Award from banner
            cids = getEligibleBanner3Stars(uid, shiny);
        }
        // If there were no eligible banner characters, fall through and pull from all options
        if (cids == null || cids.isEmpty()) {
            if (fromBanner) { isBannerFlop = true; }
            cids = getEligibleCharacters(uid, 3, shiny);
            if (cids.isEmpty()) {
                // Player has maxed all relevant 2 Stars, pick one at random to 'award'
                cids = getAllCharacters(3);
            }
        }

        if (cids.isEmpty()) {
            response.addMessagePart("Unable to award 3 star: No eligible character ids found");
            return null;
        }

        long cid = cids.get(HBMain.RNG_SOURCE.nextInt(cids.size()));
        return awardCharacter(uid, cid, shiny, fromBanner, isBannerFlop, useBriefResponse, response);
    }

    private static GachaUser awardFiller(long uid, boolean fromBanner, GachaResponse response) {
        int roll = HBMain.RNG_SOURCE.nextInt(100);
        // TODO: Add filler other than coins
        if (roll < 99) {
            int coins = (int)(HBMain.RNG_SOURCE.nextGaussian() * 20) + 50;
            if (coins < 2) {
                coins = 2;
            }
            response.coinBalance = awardCoinFiller(uid, coins);
            response.coinsAwarded += coins;
            response.addMessagePart(":coin: You pull " + coins + " coins.");
        } else {
            addPulls(uid, 1);
            response.addMessagePart(":stars: You pull ... a pull. Neat.");
        }
        return logFillerPull(uid, fromBanner);
    }

    private static GachaUser awardCharacter(long uid, long cid, boolean shiny, boolean fromBanner,
            boolean isBannerFlop, boolean useBriefResponse, GachaResponse response) {
        int duplicates = checkCharacterDuplicates(uid, cid, shiny);
        boolean alreadyMaxed = false;
        if (duplicates < 0) {
            awardNewCharacter(uid, cid, shiny);
        } else if (duplicates < MAX_CHARACTER_DUPLICATES) {
            awardCharacterDuplicate(uid, cid, shiny);
        } else {
            alreadyMaxed = true;
            // Award coins once we have the character object to check rarity
        }

        GachaCharacter character = getCharacter(uid, cid, shiny);
        if (character == null) {
            System.out.println("Failed to award character: " + uid + ", " + cid + ", " + shiny);
            response.addMessagePart("Failed to award character. Error: (" + cid + ", " + shiny + ")");
        }
        if (alreadyMaxed) {
            awardMaxedCharacter(uid, character.getMaxedCharacterCoinEquivalent());
        }
        response.addCharacterMessagePart(character.generateAwardText(useBriefResponse, alreadyMaxed), character.rarity,
            character.getPictureLink());
        return logCharacterAward(uid, character.rarity, fromBanner, isBannerFlop);
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

    // TODO
    static List<String> provideCharacterPreview(long uid) {
        // Query characters

        // Format text

        return new ArrayList<>(Arrays.asList(""));
    }

    static String handleCharacterList(long uid) {
        List<GachaCharacter> characters = getCharacters(uid);
        if (characters.isEmpty()) {
            GachaUser user = getGachaUser(uid);
            if (user == null) {
                return Casino.USER_NOT_FOUND_MESSAGE;
            } else {
                return "No characters found, but you do have " + user.pulls + " pulls available. Use `/pull` to spend them";
            }
        }

        StringBuilder output = new StringBuilder("Your characters:");
        for (GachaCharacter character : characters) {
            output.append("\n" + character.toAbbreviatedString());
        }
        return output.toString();
    }

    static String handleCharacterDetails(long uid, long cid, int rarity) {
        GachaCharacter character = getCharacter(uid, cid, rarity == 1);
        if (character == null) {
            GachaUser user = getGachaUser(uid);
            if (user == null) {
                return Casino.USER_NOT_FOUND_MESSAGE;
            } else {
                return "Unable to fetch details for provided character";
            }
        }

        return character.toFullString();
    }

    static String handlePity(long uid) {
        GachaUser user = getGachaUser(uid);
        if (user == null) {
            return Casino.USER_NOT_FOUND_MESSAGE;
        }
        return user.getPityString();
    }

    static String handlePulls(long uid) {
        GachaUser user = getGachaUser(uid);
        if (user == null) {
            return Casino.USER_NOT_FOUND_MESSAGE;
        }
        Events.EventUser eventUser = Events.getEventUser(uid);
        if (eventUser == null) {
            return "Unable to fetch EventUser. Potentially bad DB state";
        }
        if (user.pulls > 0) {
            return "You currently have " + user.pulls + " available pulls.\n"
                + eventUser.getAvailablePullSources();
        } else {
            return "No pulls remaining. " + eventUser.getAvailablePullSources();
        }
    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS gacha_user (
    //   uid bigint PRIMARY KEY,
    //   pulls integer DEFAULT 10,
    //   feed_coins bigint DEFAULT 0,
    //   times_pulled_nonbanner bigint DEFAULT 0,
    //   times_pulled_banner bigint DEFAULT 0,
    //   primary_pity integer DEFAULT 0,
    //   secondary_pity integer DEFAULT 0,
    //   tertiary_pity integer DEFAULT 0,
    //   banner_flops integer DEFAULT 0,
    //   CONSTRAINT gacha_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_character (
    //   cid bigint PRIMARY KEY,
    //   name varchar(40),
    //   rarity integer DEFAULT 1,
    //   type varchar(40),
    //   rob_bonus float(6) DEFAULT 1.0,
    //   pick_bonus float(6) DEFAULT 1.0,
    //   fish_bonus float(6) DEFAULT 1.0,
    //   work_bonus float(6) DEFAULT 1.0,
    //   description varchar(200) DEFAULT '',
    //   picture_url varchar(100),
    //   shiny_picture_url varchar(100),
    //   shinier_picture_url varchar(100) DEFAULT '',
    //   enabled boolean DEFAULT true
    // );

    // CREATE TABLE IF NOT EXISTS gacha_user_character (
    //   uid bigint,
    //   cid bigint,
    //   foil integer DEFAULT 0,
    //   duplicates integer DEFAULT 0,
    //   level integer DEFAULT 0,
    //   xp integer DEFAULT 0,
    //   item_id bigint DEFAULT 0,
    //   PRIMARY KEY(uid, cid, foil),
    //   CONSTRAINT gacha_user_character_uid FOREIGN KEY(uid) REFERENCES gacha_user(uid),
    //   CONSTRAINT gacha_user_character_cid FOREIGN KEY(cid) REFERENCES gacha_character(cid)
    // );

    // CREATE TABLE IF NOT EXISTS gacha_banner (
    //   banner_id bigint PRIMARY KEY,
    //   slot1 bigint,
    //   slot2 bigint,
    //   slot3 bigint,
    //   slot4 bigint,
    //   start timestamp,
    //   CONSTRAINT gacha_banner_cid1 FOREIGN KEY(slot1) REFERENCES gacha_character(cid),
    //   CONSTRAINT gacha_banner_cid2 FOREIGN KEY(slot2) REFERENCES gacha_character(cid),
    //   CONSTRAINT gacha_banner_cid3 FOREIGN KEY(slot3) REFERENCES gacha_character(cid),
    //   CONSTRAINT gacha_banner_cid4 FOREIGN KEY(slot4) REFERENCES gacha_character(cid)
    // );

    static final String GACHA_USER_COLUMNS = "primary_pity, secondary_pity, tertiary_pity, banner_flops, pulls, times_pulled_nonbanner + times_pulled_banner AS times_pulled";

    private static GachaUser getGachaUser(long uid) {
        String query = "SELECT " + GACHA_USER_COLUMNS + " FROM gacha_user WHERE uid = " + uid + ";";
        return getGachaUser(query);
    }

    private static GachaUser getGachaUser(String query) {
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int primary = results.getInt(1);
                int secondary = results.getInt(2);
                int tertiary = results.getInt(3);
                int bannerFlops = results.getInt(4);
                int pulls = results.getInt(5);
                int timesPulled = results.getInt(6);

                return new Gacha.GachaUser(primary, secondary, tertiary,
                        bannerFlops, pulls, timesPulled);
            }
            return null;
        }, null);
    }

    private static GachaCharacter getCharacter(long uid, long cid, boolean shiny) {
        String query = "SELECT name, rarity, foil, type, level, xp, duplicates, description, picture_url, shiny_picture_url FROM "
                + "gacha_user_character NATURAL JOIN gacha_character WHERE uid = " + uid + " AND cid = " + cid
                + " AND foil = " + (shiny ? 1 : 0) + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return new GachaCharacter(results.getString(1), results.getInt(2), results.getInt(3),
                        results.getString(4), results.getInt(5), results.getInt(6), results.getInt(7),
                        results.getString(8), results.getString(9), results.getString(10));
            }
            return null;
        }, null);
    }

    private static List<GachaCharacter> getCharacters(long uid) {
        String query = "SELECT name, rarity, foil, type, level, xp, duplicates, description, picture_url, shiny_picture_url FROM " +
                "gacha_user_character NATURAL JOIN gacha_character WHERE uid = " + uid + " ORDER BY rarity DESC;";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            List<GachaCharacter> output = new ArrayList<>();
            while (results.next()) {
                output.add(new GachaCharacter(results.getString(1), results.getInt(2), results.getInt(3),
                        results.getString(4), results.getInt(5), results.getInt(6), results.getInt(7),
                        results.getString(8), results.getString(9), results.getString(10)));
            }
            return output;
        }, new ArrayList<>());
    }

    private static List<Long> getEligibleBanner3Stars(long uid, boolean shiny) {
        return CasinoDB.executeListQuery("SELECT cid FROM (SELECT slot1 AS cid FROM gacha_banner ORDER BY start DESC LIMIT 1) "
                + "AS current WHERE NOT EXISTS(SELECT 1 FROM gacha_user_character WHERE gacha_user_character.cid = current.cid AND duplicates >= "
                + MAX_CHARACTER_DUPLICATES + " AND foil = " + (shiny ? 1 : 0) + " AND uid = " + uid + ");");
    }

    private static List<Long> getEligibleBanner2Stars(long uid, boolean shiny) {
        String first2Star = "(SELECT cid FROM (SELECT slot2 AS cid FROM gacha_banner ORDER BY start DESC LIMIT 1) "
                + "AS current WHERE NOT EXISTS(SELECT 1 FROM gacha_user_character WHERE gacha_user_character.cid = current.cid AND duplicates >= "
                + MAX_CHARACTER_DUPLICATES + " AND foil = " + (shiny ? 1 : 0) + " AND uid = " + uid + "))";
        String second2Star = "(SELECT cid FROM (SELECT slot3 AS cid FROM gacha_banner ORDER BY start DESC LIMIT 1) "
                + "AS current WHERE NOT EXISTS(SELECT 1 FROM gacha_user_character WHERE gacha_user_character.cid = current.cid AND duplicates >= "
                + MAX_CHARACTER_DUPLICATES + " AND foil = " + (shiny ? 1 : 0) + " AND uid = " + uid + "))";
        return CasinoDB.executeListQuery(first2Star + " UNION " + second2Star + ";");

    }

    private static List<Long> getEligibleCharacters(long uid, int rarity, boolean shiny) {
        return CasinoDB.executeListQuery("SELECT cid FROM gacha_character WHERE rarity = " + rarity
                + " AND enabled = TRUE AND NOT EXISTS(SELECT 1 FROM gacha_user_character "
                + "WHERE gacha_user_character.cid = gacha_character.cid AND duplicates >= "
                + MAX_CHARACTER_DUPLICATES + " AND foil = " + (shiny ? 1 : 0) + " AND uid = " + uid + ");");
    }

    private static List<Long> getAllCharacters(int rarity) {
        return CasinoDB.executeListQuery("SELECT cid FROM gacha_character WHERE rarity = "
                + rarity + " AND enabled = TRUE;");
    }

    private static int checkCharacterDuplicates(long uid, long cid, boolean shiny) {
        int isOwned = CasinoDB.executeIntQuery("SELECT COUNT(uid) FROM gacha_user_character WHERE uid = "
                + uid + " AND cid = " + cid + "AND foil = " + (shiny ? 1 : 0) + ";");
        // Shouldn't ever be negative, but doesn't hurt to catch it
        if (isOwned <= 0) {
            return -1;
        } else {
            return CasinoDB.executeIntQuery("SELECT duplicates FROM gacha_user_character WHERE uid = "
                    + uid + " AND cid = " + cid + ";");
        }
    }

    private static void awardNewCharacter(long uid, long cid, boolean shiny) {
        CasinoDB.executeUpdate("INSERT INTO gacha_user_character(uid, cid, foil) VALUES ("
                + uid + ", " + cid + ", " + (shiny ? 1 : 0)
                + ") ON CONFLICT (uid, cid, foil) DO NOTHING;");
    }

    private static void awardCharacterDuplicate(long uid, long cid, boolean shiny) {
        CasinoDB.executeUpdate("UPDATE gacha_user_character SET (duplicates) = (duplicates + 1) WHERE uid = "
                + uid + " AND cid = " + cid + " AND foil = " + (shiny ? 1 : 0) + ";");
    }

    private static long awardMaxedCharacter(long uid, long coinEquivalent) {
        return Casino.addMoney(uid, coinEquivalent);
    }

    private static long awardCoinFiller(long uid, long coinAmount) {
        return Casino.addMoney(uid, coinAmount);
    }

    static int addPulls(long uid, int amount) {
        return CasinoDB.executeIntQuery("UPDATE gacha_user SET (pulls) = (pulls + " + amount
                + ") WHERE uid = " + uid + " RETURNING pulls;");
    }

    private static GachaUser logFillerPull(long uid, boolean onBanner) {
        String pullType = (onBanner ? "times_pulled_banner" : "times_pulled_nonbanner");
        return getGachaUser("UPDATE gacha_user SET (pulls, " + pullType + ", primary_pity, secondary_pity, "
                + "tertiary_pity) = (pulls - 1, " + pullType + " + 1, primary_pity + 1, secondary_pity + 1, "
                + "tertiary_pity + 1) WHERE uid = " + uid + " RETURNING " + GACHA_USER_COLUMNS + ";");
    }

    private static GachaUser logCharacterAward(long uid, int rarity, boolean fromBanner, boolean isBannerFlop) {
        String pullType = (fromBanner ? "times_pulled_banner" : "times_pulled_nonbanner");
        String secondaryPity = "secondary_pity + 1";
        if (rarity > 2) {
            secondaryPity = "secondary_pity";
        } else if (rarity == 2) {
            secondaryPity = "0";
        }
        String tertiaryPity = "tertiary_pity + 1";
        if (rarity > 1) {
            tertiaryPity = "tertiary_pity";
        } else if (rarity == 1) {
            tertiaryPity = "0";
        }
        return getGachaUser("UPDATE gacha_user SET (pulls, " + pullType + ", primary_pity, secondary_pity, "
                + "tertiary_pity, banner_flops) = (pulls - 1, " + pullType + " + 1, " + (rarity == 3 ? "0" : "primary_pity + 1") + ", "
                + secondaryPity + ", " + tertiaryPity + ", " + (isBannerFlop ? "banner_flops + 1" : "0")
                + ") WHERE uid = " + uid + " RETURNING " + GACHA_USER_COLUMNS + ";");
    }

}
