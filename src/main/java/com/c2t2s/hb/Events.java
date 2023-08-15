package com.c2t2s.hb;

import java.sql.*;
import java.util.Arrays;
import java.util.List; //TODO: Remove the *
import java.util.Random;
import java.net.URISyntaxException;

public class Events {
	
	protected static class EventUser {    	
    	public int events_today;
    	public int robs_today;
    	public int picks_today;
    	public Timestamp reset;
    	
    	public EventUser(long uid, int robs, int picks, int events, Timestamp lastReset) {
    		if (System.currentTimeMillis() - lastReset.getTime() > DAILY_RESET_MS) {
    			this.reset = resetEventUserDailyLimits(uid);
    		}
    		this.events_today = events;
    		this.robs_today = robs;
    		this.picks_today = picks;
    		this.reset = lastReset;
    	}
    	
    	protected String getAvailablePullSources() {
        	if (robs_today < 1 || picks_today < 1 /* || events_today < MAX_DAILY_EVENT_PULLS */) {
        		String output = "No pulls remaining. You can still earn pulls today through the following means:";
        		if (robs_today < 1) {
        			output += "\n\t`/rob` or `/work` once today";
        		}
        		if (picks_today < 1) {
        			output += "\n\t`/fish` or `/pickpocket` once today";
        		}
        		// TODO: Readd event check
        		//int remaining_events = MAX_DAILY_EVENT_PULLS - events_today;
        		//if (remaining_events > 0) {
        		//	output += "\n\tJoin events today - earn pulls up to " + remaining_events + " more time" + (remaining_events == 1 ? "" : "s");
        		//}
	        	return output;
        	} else {
        		long timeRemaining = DAILY_RESET_MS - (System.currentTimeMillis() - reset.getTime());
        		if (timeRemaining < 1000) { timeRemaining = 1000; }
        		return "No pulls remaining. Return in " + Casino.formatTime(timeRemaining) + " to get more!";
        	}
    	}
    }
	
	private static final int MAX_DAILY_EVENT_PULLS = 3;
	protected static final int DAILY_RESET_MS = 22 * 60 * 60 * 1000;

	public static String checkRobBonus(long uid, String command) {
    	EventUser user = getEventUser(uid);
    	if (user == null) {
            return "\nUnable to fetch user for daily bonus. If you are new run `/claim` to start";
        }
    	
    	if (user.robs_today > 0) {
    		return "";
    	}
    	
    	long pulls = awardRobBonus(uid);
    	
    	return "\nFirst " + command + " of the day: Received 2 Gacha pulls. You now have "
    		+ pulls + " pull" + (pulls != 1 ? "s" : "");
    }

    public static String checkPickBonus(long uid, String command) {
    	EventUser user = getEventUser(uid);
    	if (user == null) {
            return "\nUnable to fetch user for daily bonus. If you are new run `/claim` to start";
        }
    	
    	if (user.picks_today > 0) {
    		return "";
    	}
    	
    	long pulls = awardPickBonus(uid);
    	
    	return "\nFirst " + command + " of the day: Received 2 Gacha pulls. You now have "
    		+ pulls + " pull" + (pulls != 1 ? "s" : "");
    }
    
    //////////////////////////////////////////////////////////
    
    // CREATE TABLE IF NOT EXISTS event_user (
    //   uid bigint PRIMARY KEY,
    //   robs_today integer DEFAULT 0,
    //   picks_today integer DEFAULT 0,
    //   events_today integer DEFAULT 0,
    //   last_reset timestamp DEFAULT '2021-01-01 00:00:00',
    //   robs_joined bigint DEFAULT 0,
    //   rob_successes bigint DEFAULT 0,
    //   rob_profit bigint DEFAULT 0,
    //   picks_joined bigint DEFAULT 0,
    //   pick_successes bigint DEFAULT 0,
    //   pick_profit bigint DEFAULT 0,
    //   works_joined bigint DEFAULT 0,
    //   work_profit bigint DEFAULT 0,
    //   fishes_joined bigint DEFAULT 0,
    //   fish_caught bigint DEFAULT 0,
    //   fish_profit bigint DEFAULT 0,
    //   CONSTRAINT event_user_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );
    
    // CREATE TABLE IF NOT EXISTS event_participants (
    //   uid bigint,
    //   cid bigint,
    //   notes varchar(40) DEFAULT '',
    //   PRIMARY KEY(uid, cid),
    //   CONSTRAINT event_participants_uid FOREIGN KEY(uid, cid) REFERENCES gacha_user_characters(uid, cid)
    // );
    
    protected static EventUser getEventUser(long uid) {
        String query = "SELECT robs_today, picks_today, events_today, last_reset FROM event_user WHERE uid = " + uid + ";";
        Connection connection = null;
        Statement statement = null;
        EventUser user = null;
        try {
            connection = Casino.getConnection();
            statement = connection.createStatement();
            ResultSet results = statement.executeQuery(query);
            if (results.next()) {
            	int robs = results.getInt(1);
            	int picks = results.getInt(2);
            	int events = results.getInt(3);
            	Timestamp reset = results.getTimestamp(4);
            	
                user = new EventUser(uid, robs, picks, events, reset);
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

    public static long awardRobBonus(long uid) {
    	Casino.executeUpdate("UPDATE event_user SET (robs_today) = (robs_today + 1) WHERE uid = " + uid + ";");
    	return addPulls(uid, 2);
    }
    
    public static long awardPickBonus(long uid) {
    	Casino.executeUpdate("UPDATE event_user SET (picks_today) = (picks_today + 1) WHERE uid = " + uid + ";");
    	return addPulls(uid, 2);
    }

    private static int addPulls(long uid, int amount) {
    	return Casino.executeIntQuery("UPDATE gacha_user SET (pulls) = (pulls + " + amount
    			+ ") WHERE uid = " + uid + " RETURNING pulls;");
    }
    
    private static Timestamp resetEventUserDailyLimits(long uid) {
    	return Casino.executeTimestampQuery("UPDATE event_user SET (robs_today, picks_today, last_reset) = (0, 0, NOW()) WHERE uid = "
    			+ uid + " RETURNING last_reset;");
    }
}
