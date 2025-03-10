package com.c2t2s.hb;

import java.sql.Timestamp;

class EventUser {
    private int eventsToday = 0;
    private int robsToday = 0;
    private int picksToday = 0;
    private int dailyGamesToday = 0;
    private Timestamp reset;

    enum DailyGame {
        GUESS, HUGEGUESS, SLOTS, MINISLOTS, OVERUNDER, BLACKJACK, ALLORNOTHING;

        static DailyGame getDailyGame(long resetTime) {
            switch ((int)resetTime % 7) {
                case 0:
                    return GUESS;
                case 1:
                    return HUGEGUESS;
                default:
                case 2:
                    return SLOTS;
                case 3:
                    return MINISLOTS;
                case 4:
                    return OVERUNDER;
                case 5:
                    return BLACKJACK;
                case 6:
                    return ALLORNOTHING;
            }
        }

        @Override
        public String toString() {
            return "`/" + name().toLowerCase() + '`';
        }
    }

    private EventUser(long uid, int robsToday, int picksToday, int dailyGamesToday,
                      int eventsToday, Timestamp reset) {
        if (reset.getTime() - System.currentTimeMillis() < 0) {
            this.reset = resetEventUserDailyLimits(uid);
        } else {
            this.reset = reset;
            this.eventsToday = eventsToday;
            this.robsToday = robsToday;
            this.picksToday = picksToday;
            this.dailyGamesToday = dailyGamesToday;
        }
    }

    String getAvailablePullSources() {
        long timeRemaining = reset.getTime() - System.currentTimeMillis();
        if (timeRemaining < 1000) { timeRemaining = 1000; }
        String remainingTime = Casino.formatTime(timeRemaining);
        if (robsToday < 1 || picksToday < 1 || dailyGamesToday < 1 /* || events_today < MAX_DAILY_EVENT_PULLS */) {
            StringBuilder output = new StringBuilder("You can still earn pulls today through the following means:");
            if (robsToday < 1) {
                output.append("\n\t`/rob` or `/work` once today");
            }
            if (picksToday < 1) {
                output.append("\n\t`/fish` or `/pickpocket` once today");
            }
            if (dailyGamesToday < 1) {
                output.append("\n\tPlay ");
                output.append(DailyGame.getDailyGame(reset.getTime()).toString());
                output.append(" once today");
            }
            // TODO: Readd event check
            //int remaining_events = MAX_DAILY_EVENT_PULLS - events_today;
            //if (remaining_events > 0) {
            //    output += "\n\tJoin events today - earn pulls up to " + remaining_events + " more time" + (remaining_events == 1 ? "" : "s");
            //}

            output.append("\nYour next reset occurs in ");
            output.append(remainingTime);
            return output.toString();
        } else {
            return "Return in " + remainingTime + " to get more!";
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

    static String checkGameBonus(long uid, DailyGame game) {
        EventUser user = getEventUser(uid);
        if (user == null) {
            return "\nUnable to fetch user for daily bonus. If you are new run `/claim` to start";
        }

        if (user.dailyGamesToday > 0 || game != DailyGame.getDailyGame(user.reset.getTime())) {
            return "";
        }

        long pulls = awardDailyGameBonus(uid);

        return "\nFirst " + game.toString() + " of the day: Received 1 Gacha pull. You now have "
            + pulls + " pull" + (pulls != 1 ? "s" : "");

    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS event_user (
    //   uid bigint PRIMARY KEY,
    //   robs_today integer DEFAULT 0,
    //   picks_today integer DEFAULT 0,
    //   events_today integer DEFAULT 0,
    //   daily_games_today integer DEFAULT 0,
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
        String query = "SELECT robs_today, picks_today, events_today, daily_games_today, next_reset FROM event_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int robs = results.getInt(1);
                int picks = results.getInt(2);
                int events = results.getInt(3);
                int dailyGames = results.getInt(4);
                Timestamp reset = results.getTimestamp(4);

                return new EventUser(uid, robs, picks, dailyGames, events, reset);
            }
            return null;
        }, null);
    }

    private static long awardRobBonus(long uid) {
        CasinoDB.executeUpdate("UPDATE event_user SET (robs_today) = (robs_today + 1) WHERE uid = " + uid + ";");
        return Gacha.addPulls(uid, 2);
    }

    private static long awardPickBonus(long uid) {
        CasinoDB.executeUpdate("UPDATE event_user SET (picks_today) = (picks_today + 1) WHERE uid = " + uid + ";");
        return Gacha.addPulls(uid, 2);
    }

    private static long awardDailyGameBonus(long uid) {
        CasinoDB.executeUpdate("UPDATE event_user SET (daily_games_today) = (daily_games_today + 1) WHERE uid = " + uid + ";");
        return Gacha.addPulls(uid, 1);
    }

    private static Timestamp resetEventUserDailyLimits(long uid) {
        return CasinoDB.executeTimestampQuery("UPDATE event_user SET (robs_today, picks_today, daily_games_today, next_reset) = (0, 0, 0, NOW() + INTERVAL '22 hours') WHERE uid = "
                + uid + " RETURNING next_reset;");
    }
}
