package com.c2t2s.hb;

import java.sql.*; //TODO: Remove the *
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;
import java.util.HashMap;
import java.net.URISyntaxException;

public class Gacha {
    
    private static class GachaUser {
    	public int primary_pity;
    	public int secondary_pity;
    	public int tertiary_pity;
    	public int banner_flops;
    	public int pulls;
    	public int times_pulled;
    	
    	public GachaUser(int primary_pity, int secondary_pity, int tertiary_pity,
    			int banner_flops, int pulls, int times_pulled) {
    		this.primary_pity = primary_pity;
    		this.secondary_pity = secondary_pity;
    		this.tertiary_pity = tertiary_pity;
    		this.banner_flops = banner_flops;
    		this.pulls = pulls;
    		this.times_pulled = times_pulled;
    	}
    	
    	// Returns chance to pull a 3 Star
    	public double getThreeStarChance() {
    		// TODO: Scale this to be larger at high pity
    		if (primary_pity >= (MAX_3STAR_PITY)) {
    			return 1.0;
    		} else {
        		return BASE_3STAR_CHANCE;
    		}
    	}
    	
    	// Returns chance to pull a 2 Star
    	public double getTwoStarChance() {
    		// TODO: Scale this to be larger at high pity
    		if (secondary_pity >= (MAX_2STAR_PITY)) {
    			return 1.0;
    		} else {
        		return BASE_2STAR_CHANCE;
    		}
    	}
    	
    	// Returns chance to pull a 1 Star
    	public double getOneStarChance() {
    		// TODO: Scale this to be larger at high pity
    		if (tertiary_pity >= (MAX_1STAR_PITY)) {
    			return 1.0;
    		} else {
        		return BASE_1STAR_CHANCE;
    		}
    	}
    	
    	protected String getPityString() {
    		return "3 Star Pity: " + primary_pity + "/" + (MAX_3STAR_PITY + 1)
    			+ "\n2 Star Pity: " + secondary_pity + "/" + (MAX_2STAR_PITY + 1)
    			+ "\n1 Star Pity: " + tertiary_pity + "/" + (MAX_1STAR_PITY + 1)
    			+ "\nAvailable Pulls: " + pulls;
    	}
    }
    
    private static class GachaCharacter {
    	
    	public String name;
    	public int rarity;
    	public int foil;
    	public String type;
    	public int level;
    	public int xp;
    	public int duplicates;
    	public String description;
    	public String picture_url;
    	public String shiny_url;
    	
    	public GachaCharacter(String name, int rarity, int foil, String type, int level,
    			int xp, int duplicates, String description, String picture_url, String shiny_url) {
    		this.name = name;
    		this.rarity = rarity;
    		this.foil = foil;
    		this.type = type;
    		this.level = level;
    		this.xp = xp;
    		this.duplicates = duplicates;
    		this.description = description;
    		this.picture_url = picture_url;
    		this.shiny_url = shiny_url;
    		
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
    	
    	public String toAbbreviatedString() {
    		return getDisplayName() + (duplicates > 0 ? " +" + duplicates : "")
    				+ " (" + rarity + " Star " + type + ") - Level " + level
    				+ (level < MAX_CHARACTER_LEVEL ? " [" + xp + "/" + getXpToLevel() + "]" : " [Max Level]")
    				+ " - +" + getBuffPercent() + "% Bonus";
    	}
    	
    	public String toFullString() {
    		return getDisplayName() + (duplicates > 0 ? " +" + duplicates : "")
    				+ "\n\t" + rarity + " Star " + type
    				+ "\n\tLevel " + level + (level < MAX_CHARACTER_LEVEL ? " [" + xp + "/" + getXpToLevel() + "]" : " [Max Level]")
    				+ "\n\t+" + getBuffPercent() + "% Bonus"
    				+ "\n" + (description.isEmpty() ? "" : description)
    				+ "\n" + (foil == 1 ? shiny_url : picture_url);
    	}
    	
    	public String generateAwardText(boolean useBriefResponse) {
    		String star = (foil == 1 ? ":star2:" : ":star:");
    		String stars = "";
    		if (rarity > 0) {
    			stars = Casino.repeatString(star, rarity);
    		}
    		String duplicateString = "";
    		if (duplicates > 0) {
    			duplicateString = "\n" + (useBriefResponse ? "\t" : "") + "Upgraded " + getDisplayName()
                    + (duplicates > 1 ? " +" + (duplicates - 1) : "")
    				+ " -> " + getDisplayName() + " +" + duplicates;
    		}
            if (useBriefResponse) {
                return stars + " " + getDisplayName() + " " + stars + " (" + rarity + " Star " + type + ")"
                    + duplicateString
                    + (!description.isEmpty() ? "\n\tThis character has a unique ability" : "")
    				+ "\n\t" + (foil == 1 ? shiny_url : picture_url);
            } else {
                return stars + " " + getDisplayName() + " " + stars
                    + "\n" + rarity + " Star " + type
                    + duplicateString
                    + (!description.isEmpty() ? "\n\tUnique Ability:" + description : "")
                    + "\n" + (foil == 1 ? shiny_url : picture_url);
            }
    	}
    	
    	public int getXpToLevel() {
    		// Level 0 -> Level 1: 100 xp
    		// Every level beyond doubles previous level
    		return 50 * (int)Math.pow(2, level + 1);
    	}
    	
    	public double getBuffPercent() {
    		return 2.5 + (rarity * 2.5) + (duplicates * 1.0) + (level * 1.0);
    	}
    	
    	public double getBuffDecimal() {
    		return getBuffPercent() / 100;
    	}
    }
    
    protected static class GachaResponse {
        public String partialMessage = "";
        public List<String> messageParts = new ArrayList<String>();
        public Map<Integer, String> images = new HashMap<Integer, String>();

        public long coinsAwarded = 0;
        public long coinBalance = 0;
        public long chocolateCoinsAwarded = 0;
        public long chocolateCoinBalance = 0;
        public int highestCharacterAwarded = 0;

        public void addMessagePart(String message) {
            partialMessage += (!partialMessage.isEmpty() ? "\n" : "") + message;
            messageParts.add(partialMessage);
        }

        public void addCharacterMessagePart(String message, int rarity) {
            if (rarity > highestCharacterAwarded) {
                highestCharacterAwarded = rarity;
            }
            addMessagePart(message);
        }

        public void addAnimation(List<String> frames) {
            messageParts.addAll(0, frames);
        }
    }

	public static final int MAX_CHARACTER_LEVEL = 5;
	public static final int MAX_CHARACTER_DUPLICATES = 5;
	public static final int MAX_3STAR_PITY = 191;
	public static final int MAX_2STAR_PITY = 47;
	public static final int MAX_1STAR_PITY = 11;
	public static final int MAX_BANNER_FLOPS = 2;
	public static final double BASE_3STAR_CHANCE = 0.0078125;
	public static final double BASE_2STAR_CHANCE = 0.03125;
	public static final double BASE_1STAR_CHANCE = 0.125;
	public static final double BANNER_3STAR_CHANCE = 0.5;
	public static final double BANNER_2STAR_CHANCE = 0.6;
	public static final double SHINY_CHANCE = 0.05;

    public static GachaResponse handleGachaPull(long uid, boolean on_banner, long pulls) {
        GachaResponse response = new GachaResponse();
    	GachaUser user = getGachaUser(uid);
    	if (user == null) {
    		response.addMessagePart("Unable to fetch user. If you are new run `/claim` to start");
    		return response;
    	}

		Events.EventUser eventUser = Events.getEventUser(uid);
		if (eventUser == null) {
			response.addMessagePart("Unable to pull. Insufficient pulls and unable to fetch EventUser.");
			return response;
		}

    	if (user.pulls < 1) {
    		response.addMessagePart("No pulls remaining. " + eventUser.getAvailablePullSources());
    		return response;
    	} else if (user.pulls < pulls) {
            response.addMessagePart("Insufficient pulls, you have " + user.pulls + " pulls remaining. "
                + eventUser.getAvailablePullSources());
            return response;
        }

    	Random random = new Random();
        boolean useBriefResponse = (pulls != 1);
        GachaUser updatedUser;

        for (int i = 0; i < pulls; ++i) {
            if (random.nextDouble() <= user.getThreeStarChance()) {
                updatedUser = awardThreeStar(uid, on_banner, user.banner_flops, useBriefResponse, random, response);
            } else if (random.nextDouble() <= user.getTwoStarChance()) {
                updatedUser = awardTwoStar(uid, on_banner, useBriefResponse, random, response);
            // First roll always gives at least 1 star
            } else if (random.nextDouble() <= user.getOneStarChance() || user.times_pulled == 0) { 
                updatedUser = awardOneStar(uid, on_banner, useBriefResponse, random, response);
            } else {
                updatedUser = awardFiller(uid, on_banner, random, response);
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

        return response;
    }

    private static GachaUser awardOneStar(long uid, boolean from_banner, boolean useBriefResponse,
            Random random, GachaResponse response) {
    	List<Long> cids = null;
    	boolean shiny = random.nextDouble() < SHINY_CHANCE;
    	cids = getEligibleCharacters(uid, 1, shiny);
    	if (cids.isEmpty()) {
			// Player has maxed all relevant 1 Stars, pick one at random to 'award'
			cids = getAllCharacters(1);
		}
    	
    	if (cids.isEmpty()) {
    		response.addMessagePart("Unable to award 1 star: No eligible character ids found");
            return null;
    	}
    	
    	long cid = cids.get(random.nextInt(cids.size()));
    	return awardCharacter(uid, cid, shiny, from_banner, false, useBriefResponse, response);
    }
    
    private static GachaUser awardTwoStar(long uid, boolean from_banner, boolean useBriefResponse,
            Random random, GachaResponse response) {
    	List<Long> cids = null;
    	boolean shiny = random.nextDouble() < SHINY_CHANCE;
    	if (from_banner && (random.nextDouble() < BANNER_2STAR_CHANCE)) {
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
    	
    	long cid = cids.get(random.nextInt(cids.size()));
        return awardCharacter(uid, cid, shiny, from_banner, false, useBriefResponse, response);
    }
    
    private static GachaUser awardThreeStar(long uid, boolean from_banner, int banner_flops,
            boolean useBriefResponse, Random random, GachaResponse response) {
    	List<Long> cids = null;
    	boolean shiny = random.nextDouble() < SHINY_CHANCE;
    	if (from_banner && (random.nextDouble() < BANNER_3STAR_CHANCE)) {
    		// Award from banner
    		cids = getEligibleBanner3Stars(uid, shiny);
    	}
    	// If there were no eligible banner characters, fall through and pull from all options
    	if (cids == null || cids.isEmpty()){
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
    	
    	long cid = cids.get(random.nextInt(cids.size()));
        return awardCharacter(uid, cid, shiny, from_banner, false, useBriefResponse, response);
    }

    private static GachaUser awardFiller(long uid, boolean from_banner, Random random, GachaResponse response) {
        int roll = random.nextInt(100);
        // TODO: Add filler other than coins
        if (roll < 99) {
            int coins = (int)(random.nextGaussian() * 20) + 50;
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
        return logFillerPull(uid, from_banner);
    }

    private static GachaUser awardCharacter(long uid, long cid, boolean shiny, boolean from_banner,
            boolean is_banner_flop, boolean useBriefResponse, GachaResponse response) {
    	int duplicates = checkCharacterDuplicates(uid, cid, shiny);
    	if (duplicates < 0) {
    		awardNewCharacter(uid, cid, shiny);
    	} else if (duplicates < MAX_CHARACTER_DUPLICATES) {
    		awardCharacterDuplicate(uid, cid, shiny);
    	}

    	GachaCharacter character = getCharacter(uid, cid, shiny);
        response.addCharacterMessagePart(character.generateAwardText(useBriefResponse), character.rarity);
        return logCharacterAward(uid, character.rarity, from_banner, is_banner_flop);
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
    	return new ArrayList<String>(Arrays.asList(
    			":sparkles::black_large_square::black_large_square::black_large_square::black_large_square:",
    			":black_large_square::sparkles::black_large_square::black_large_square::black_large_square:",
    			":black_large_square::black_large_square::sparkles::black_large_square::black_large_square:",
    			":black_large_square::black_large_square::black_large_square::sparkles::black_large_square:",
    			":black_large_square::black_large_square::black_large_square::black_large_square::sparkles:",
    			":black_large_square::black_large_square::black_large_square::black_large_square::black_large_square:"));
    }
    
    // TODO
    public static List<String> provideCharacterPreview(long uid) {
    	// Query characters
    	
    	// Format text
    	
    	return new ArrayList<String>(Arrays.asList(""));
    }
    
    public static String handleCharacterList(long uid) {
    	List<GachaCharacter> characters = getCharacters(uid);
    	if (characters.isEmpty()) {
    		GachaUser user = getGachaUser(uid);
    		if (user == null) {
                return "Unable to fetch user. If you are new run `/claim` to start";
    		} else {
    			return "No characters found, but you do have " + user.pulls + " pulls available. Use `/pull` to spend them";
    		}
    	}
    	
    	String output = "Your characters:";
    	for (GachaCharacter character : characters) {
    		output += "\n" + character.toAbbreviatedString();
    	}
    	return output;
    }
    
    public static String handleCharacterDetails(long uid, long cid, int rarity) {
    	GachaCharacter character = getCharacter(uid, cid, rarity == 1);
    	if (character == null) {
    		GachaUser user = getGachaUser(uid);
    		if (user == null) {
                return "Unable to fetch user. If you are new run `/claim` to start";
    		} else {
    			return "Unable to fetch details for provided character";
    		}
    	}
    	
    	return character.toFullString();
    }
    
    protected static String handlePity(long uid) {
    	GachaUser user = getGachaUser(uid);
    	if (user == null) {
    		return "Unable to fetch user. If you are new run `/claim` to start";
    	}
    	return user.getPityString();
    }
    
    protected static String handlePulls(long uid) {
    	GachaUser user = getGachaUser(uid);
    	if (user == null) {
    		return "Unable to fetch user. If you are new run `/claim` to start";
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
        return getGachaUser(uid, query);
    }
    
    private static GachaUser getGachaUser(long uid, String query) {
        Connection connection = null;
        Statement statement = null;
        GachaUser user = null;
        try {
            connection = Casino.getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
            	int primary = results.getInt(1);
            	int secondary = results.getInt(2);
            	int tertiary = results.getInt(3);
            	int banner_flops = results.getInt(4);
            	int pulls = results.getInt(5);
            	int times_pulled = results.getInt(6);
            	
                user = new Gacha.GachaUser(primary, secondary, tertiary,
                		banner_flops, pulls, times_pulled);
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return user;
    }
    
    private static GachaCharacter getCharacter(long uid, long cid, boolean shiny) {
    	String query = "SELECT name, rarity, foil, type, level, xp, duplicates, description, picture_url, shiny_picture_url FROM "
    			+ "gacha_user_character NATURAL JOIN gacha_character WHERE uid = " + uid + " AND cid = " + cid
    			+ " AND foil = " + (shiny ? 1 : 0) + ";";
        Connection connection = null;
        Statement statement = null;
        GachaCharacter character = null;
        try {
            connection = Casino.getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
            	character = new GachaCharacter(results.getString(1), results.getInt(2), results.getInt(3),
            			results.getString(4), results.getInt(5), results.getInt(6), results.getInt(7),
            			results.getString(8), results.getString(9), results.getString(10));
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return character;
    }

    private static List<GachaCharacter> getCharacters(long uid) {
    	String query = "SELECT name, rarity, foil, type, level, xp, duplicates, description, picture_url, shiny_picture_url FROM " +
    			"gacha_user_character NATURAL JOIN gacha_character WHERE uid = " + uid + " ORDER BY rarity DESC;";
        Connection connection = null;
        Statement statement = null;
        List<GachaCharacter> characters = new ArrayList<GachaCharacter>();
        try {
            connection = Casino.getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            while (results.next()) {
            	characters.add(new GachaCharacter(results.getString(1), results.getInt(2), results.getInt(3),
            			results.getString(4), results.getInt(5), results.getInt(6), results.getInt(7),
            			results.getString(8), results.getString(9), results.getString(10)));
            }
            statement.close();
            connection.close();
        } catch (URISyntaxException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return characters;
    }
    
    private static List<Long> getEligibleBanner3Stars(long uid, boolean shiny) {
    	// SELECT cid FROM (SELECT slot1 AS cid FROM gacha_banner ORDER BY start DESC LIMIT 1) AS current WHERE NOT
    	// EXISTS(SELECT 1 FROM gacha_user_character WHERE gacha_user_character.cid = current.cid AND duplicates >= 5 AND foil = 0);
    	return Casino.executeListQuery("SELECT cid FROM (SELECT slot1 AS cid FROM gacha_banner ORDER BY start DESC LIMIT 1) "
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
    	return Casino.executeListQuery(first2Star + " UNION " + second2Star + ";");
    	
    }
    
    private static List<Long> getEligibleCharacters(long uid, int rarity, boolean shiny) {
    	return Casino.executeListQuery("SELECT cid FROM gacha_character WHERE rarity = " + rarity
    			+ " AND enabled = TRUE AND NOT EXISTS(SELECT 1 FROM gacha_user_character "
    			+ "WHERE gacha_user_character.cid = gacha_character.cid AND duplicates >= "
    			+ MAX_CHARACTER_DUPLICATES + " AND foil = " + (shiny ? 1 : 0) + " AND uid = " + uid + ");");
    }
    
    private static List<Long> getAllCharacters(int rarity) {
    	return Casino.executeListQuery("SELECT cid FROM gacha_character WHERE rarity = "
    			+ rarity + " AND enabled = TRUE;");
    }
    
    private static int checkCharacterDuplicates(long uid, long cid, boolean shiny) {
    	int isOwned = Casino.executeIntQuery("SELECT COUNT(uid) FROM gacha_user_character WHERE uid = "
    			+ uid + " AND cid = " + cid + "AND foil = " + (shiny ? 1 : 0) + ";");
    	// Shouldn't ever be negative, but doesn't hurt to catch it
    	if (isOwned <= 0) {
    		return -1;
    	} else {
    		return Casino.executeIntQuery("SELECT duplicates FROM gacha_user_character WHERE uid = "
    				+ uid + " AND cid = " + cid + ";");
    	}
    }
    
    private static void awardNewCharacter(long uid, long cid, boolean shiny) {
    	Casino.executeUpdate("INSERT INTO gacha_user_character(uid, cid, foil) VALUES ("
    			+ uid + ", " + cid + ", " + (shiny ? 1 : 0)
    			+ ") ON CONFLICT (uid, cid, foil) DO NOTHING;");
    }
    
    private static void awardCharacterDuplicate(long uid, long cid, boolean shiny) {
    	Casino.executeUpdate("UPDATE gacha_user_character SET (duplicates) = (duplicates + 1) WHERE uid = "
    			+ uid + " AND cid = " + cid + " AND foil = " + (shiny ? 1 : 0) + ";");
    }
    
    private static long awardCoinFiller(long uid, long coin_amount) {
    	return Casino.addMoneyDirect(uid, coin_amount);
    }

    protected static int addPulls(long uid, int amount) {
    	return Casino.executeIntQuery("UPDATE gacha_user SET (pulls) = (pulls + " + amount
    			+ ") WHERE uid = " + uid + " RETURNING pulls;");
    }
    
    private static GachaUser logFillerPull(long uid, boolean on_banner) {
    	String pullType = (on_banner ? "times_pulled_banner" : "times_pulled_nonbanner");
    	return getGachaUser(uid, "UPDATE gacha_user SET (pulls, " + pullType + ", primary_pity, secondary_pity, "
    			+ "tertiary_pity) = (pulls - 1, " + pullType + " + 1, primary_pity + 1, secondary_pity + 1, "
    			+ "tertiary_pity + 1) WHERE uid = " + uid + " RETURNING " + GACHA_USER_COLUMNS + ";");
    }
    
    private static GachaUser logCharacterAward(long uid, int rarity, boolean from_banner, boolean is_banner_flop) {
    	String pullType = (from_banner ? "times_pulled_banner" : "times_pulled_nonbanner");
    	String secondary_pity = "secondary_pity + 1";
    	if (rarity > 2) {
    		secondary_pity = "secondary_pity";
    	} else if (rarity == 2) {
    		secondary_pity = "0";
    	}
    	String tertiary_pity = "tertiary_pity + 1";
    	if (rarity > 1) {
    		tertiary_pity = "tertiary_pity";
    	} else if (rarity == 1) {
    		tertiary_pity = "0";
    	}
    	return getGachaUser(uid, "UPDATE gacha_user SET (pulls, " + pullType + ", primary_pity, secondary_pity, "
    			+ "tertiary_pity, banner_flops) = (pulls - 1, " + pullType + " + 1, " + (rarity == 3 ? "0" : "primary_pity + 1") + ", "
    			+ secondary_pity + ", " + tertiary_pity + ", " + (is_banner_flop ? "banner_flops + 1" : "0")
    			+ ") WHERE uid = " + uid + " RETURNING " + GACHA_USER_COLUMNS + ";");
    }

}
