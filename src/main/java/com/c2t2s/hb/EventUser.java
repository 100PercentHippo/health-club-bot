package com.c2t2s.hb;

import java.sql.Timestamp;

class EventUser {
    private int eventsToday = 0;
    private int dailyGamesToday = 0;
    private Timestamp reset;

    enum DailyGame {
        GUESS, HUGEGUESS, SLOTS, MINISLOTS, OVERUNDER, BLACKJACK, ALLORNOTHING;

        static DailyGame getDailyGame(long resetTime) {
            System.out.println("[DEBUG] resetTime: " + resetTime + " becomes: " + (int)resetTime % 7);
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

    private EventUser(long uid, int dailyGamesToday, int eventsToday, Timestamp reset) {
        if (reset.getTime() - System.currentTimeMillis() < 0) {
            this.reset = resetEventUserDailyLimits(uid);
        } else {
            this.reset = reset;
            this.eventsToday = eventsToday;
            this.dailyGamesToday = dailyGamesToday;
        }
    }

    String getAvailablePullSources() {
        long timeRemaining = reset.getTime() - System.currentTimeMillis();
        if (timeRemaining < 1000) { timeRemaining = 1000; }
        String remainingTime = Casino.formatTime(timeRemaining);
        if (dailyGamesToday < 1 || eventsToday < MAX_DAILY_EVENT_PULLS) {
            StringBuilder output = new StringBuilder("You can still earn pulls today through the following means:");
            if (dailyGamesToday < 1) {
                output.append("\n\tPlay ");
                output.append(DailyGame.getDailyGame(reset.getTime()).toString());
                output.append(" once today");
            }
            int remainingEvents = MAX_DAILY_EVENT_PULLS - eventsToday;
            if (remainingEvents > 0) {
                output.append("\n\tJoin gacha events (up to ");
                output.append(remainingEvents);
                output.append(" more time");
                output.append(Casino.getPluralSuffix(remainingEvents));
                output.append(" today)");
            }

            output.append("\nYour next reset occurs in ");
            output.append(remainingTime);
            return output.toString();
        } else {
            return "Return in " + remainingTime + " to get more!";
        }
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

    private static final int MAX_DAILY_EVENT_PULLS = 2;
    private static final int FIRST_EVENT_PULL_REWARD = 2;
    private static final int SUBSEQUENT_EVENT_PULL_REWARD = 1;

    static void logEventBonus(StringBuilder builder, String name, long uid) {
        EventUser user = getEventUser(uid);
        if (user == null) {
            System.out.println("Unable to find EventUser for " + name + " (" + uid
                + ") when paying out event");
            return;
        }

        int pullsAwarded = user.eventsToday == 0 ? FIRST_EVENT_PULL_REWARD
            : SUBSEQUENT_EVENT_PULL_REWARD;
        long totalPulls = awardEventBonus(uid, pullsAwarded);

        builder.append('\n');
        builder.append(name);
        builder.append("'s ");
        if (user.eventsToday == 0) {
            builder.append("1st");
        } else if (user.eventsToday == 1) {
            builder.append("2nd");
        } else if (user.eventsToday == 2) {
            builder.append("3rd");
        } else {
            // Assumes no user will participant in 10 or more events in one day
            builder.append(user.eventsToday + 1);
            builder.append("th");
        }
        builder.append(" event of the day: +");
        builder.append(pullsAwarded);
        builder.append(" pull");
        builder.append(Casino.getPluralSuffix(pullsAwarded));
        builder.append(", new balance ");
        builder.append(totalPulls);
        builder.append(" pull");
        builder.append(Casino.getPluralSuffix(totalPulls));
    }

    //////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS event_user (
    //   uid bigint PRIMARY KEY,
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
        String query = "SELECT daily_games_today, events_today, next_reset FROM event_user WHERE uid = " + uid + ";";
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                return new EventUser(uid, results.getInt(1), results.getInt(2),
                    results.getTimestamp(3));
            }
            return null;
        }, null);
    }

    private static long awardDailyGameBonus(long uid) {
        CasinoDB.executeUpdate("UPDATE event_user SET daily_games_today = daily_games_today + 1 WHERE uid = " + uid + ";");
        return Gacha.addPulls(uid, 1);
    }

    private static long awardEventBonus(long uid, int pulls) {
        CasinoDB.executeUpdate("UPDATE event_user SET events_today = events_today + 1 WHERE uid = " + uid + ";");
        return Gacha.addPulls(uid, pulls);
    }

    private static Timestamp resetEventUserDailyLimits(long uid) {
        return CasinoDB.executeTimestampQuery("UPDATE event_user SET (events_today, daily_games_today, next_reset) = (0, 0, NOW() + INTERVAL '22 hours') WHERE uid = "
                + uid + " RETURNING next_reset;");
    }
}
