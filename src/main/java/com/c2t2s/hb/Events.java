package com.c2t2s.hb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

class Events {

    // Hide default constructor
    private Events() {}

    static class EventUser {
        private int eventsToday = 0;
        private int robsToday = 0;
        private int picksToday = 0;
        private Timestamp reset;

        private EventUser(long uid, int robs, int picks, int events, Timestamp nextReset) {
            if (nextReset.getTime() - System.currentTimeMillis() < 0) {
                this.reset = resetEventUserDailyLimits(uid);
            } else {
                this.reset = nextReset;
                this.eventsToday = events;
                this.robsToday = robs;
                this.picksToday = picks;
            }
        }

        String getAvailablePullSources() {
            long timeRemaining = reset.getTime() - System.currentTimeMillis();
            if (timeRemaining < 1000) { timeRemaining = 1000; }
            String remainingTime = Casino.formatTime(timeRemaining);
            if (robsToday < 1 || picksToday < 1 /* || events_today < MAX_DAILY_EVENT_PULLS */) {
                String output = "You can still earn pulls today through the following means:";
                if (robsToday < 1) {
                    output += "\n\t`/rob` or `/work` once today";
                }
                if (picksToday < 1) {
                    output += "\n\t`/fish` or `/pickpocket` once today";
                }
                // TODO: Readd event check
                //int remaining_events = MAX_DAILY_EVENT_PULLS - events_today;
                //if (remaining_events > 0) {
                //    output += "\n\tJoin events today - earn pulls up to " + remaining_events + " more time" + (remaining_events == 1 ? "" : "s");
                //}

                output += "\nYour next reset occurs in " + remainingTime;
                return output;
            } else {
                return "Return in " + remainingTime + " to get more!";
            }
        }
    }

    private static final int MAX_DAILY_EVENT_PULLS = 3;

    static String checkRobBonus(long uid, String command) {
        EventUser user = getEventUser(uid);
        if (user == null) {
            return "\nUnable to fetch user for daily bonus. If you are new run `/claim` to start";
        }

        if (user.robsToday > 0) {
            return "";
        }

        long pulls = awardRobBonus(uid);

        return "\nFirst " + command + " of the day: Received 2 Gacha pulls. You now have "
            + pulls + " pull" + (pulls != 1 ? "s" : "");
    }

    static String checkPickBonus(long uid, String command) {
        EventUser user = getEventUser(uid);
        if (user == null) {
            return "\nUnable to fetch user for daily bonus. If you are new run `/claim` to start";
        }

        if (user.picksToday > 0) {
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
    //   next_reset timestamp DEFAULT '2021-01-01 00:00:00',
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

    static EventUser getEventUser(long uid) {
        String query = "SELECT robs_today, picks_today, events_today, next_reset FROM event_user WHERE uid = " + uid + ";";
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
        } catch (SQLException e) {
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

    private static long awardRobBonus(long uid) {
        Casino.executeUpdate("UPDATE event_user SET (robs_today) = (robs_today + 1) WHERE uid = " + uid + ";");
        return Gacha.addPulls(uid, 2);
    }

    private static long awardPickBonus(long uid) {
        Casino.executeUpdate("UPDATE event_user SET (picks_today) = (picks_today + 1) WHERE uid = " + uid + ";");
        return Gacha.addPulls(uid, 2);
    }

    private static Timestamp resetEventUserDailyLimits(long uid) {
        return Casino.executeTimestampQuery("UPDATE event_user SET (robs_today, picks_today, next_reset) = (0, 0, NOW() + INTERVAL '22 hours') WHERE uid = "
                + uid + " RETURNING next_reset;");
    }
}
