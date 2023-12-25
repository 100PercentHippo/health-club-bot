package com.c2t2s.hb;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

class HealthClub {

    // Hide default constructor
    private HealthClub() {}

    static class WorkoutUser {
        int streak;
        int previousStreak;
        int totalWorkouts;
        Timestamp lastWorkout;
        int selectedReward;

        private WorkoutUser(int streak, int previousStreak, int totalWorkouts,
                Timestamp lastWorkout, int selectedReward) {
            this.streak = streak;
            this.previousStreak = previousStreak;
            this.totalWorkouts = totalWorkouts;
            this.lastWorkout = lastWorkout;
            this.selectedReward = selectedReward;
        }

        private String getTotalWorkoutString() {
            StringBuilder output = new StringBuilder(COMPLIMENT_LIST[HBMain.RNG_SOURCE.nextInt(COMPLIMENT_LIST.length)]);
            output.append(" Total Workouts completed: ");
            output.append(totalWorkouts);
            return output.toString();
        }

        private String getStreakString() {
            StringBuilder output = new StringBuilder();
            if (streak > LARGE_STREAK_BREAKPOINT) { output.append(":tada: "); }
            output.append(streak);
            output.append(" day streak");
            if (streak > MEDIUM_STREAK_BREAKPOINT) { output.append('!'); }
            if (streak > LARGE_STREAK_BREAKPOINT) { output.append(" :tada:"); }
            return output.toString();
        }
    }

    private static final String[] COMPLIMENT_LIST = {"Good Job!", "Nice!", "Well done!", "Yay!"};
    private static final long MINIMUM_SEPARATION = TimeUnit.HOURS.toMillis(12);
    private static final long MAXIMUM_SEPARATION = TimeUnit.HOURS.toMillis(48);
    private static final int MEDIUM_STREAK_BREAKPOINT = 10;
    private static final int LARGE_STREAK_BREAKPOINT = 20;
    private static final String STREAK_BROKEN_STRING = "Streak broken, streak is now 1 day";
    static final int PULL_REWARD_ID = 1;
    static final int COIN_REWARD_ID = 2;
    private static final int PULL_REWARD_AMOUNT = 1;
    private static final long COIN_REWARD_AMOUNT = 100;

    static HBMain.SingleResponse handleWorkout(long uid) {
        WorkoutUser user = getUser(uid);
        if (user == null) {
            return new HBMain.SingleResponse(Casino.USER_NOT_FOUND_MESSAGE);
        }

        long elapsedTime = System.currentTimeMillis() - user.lastWorkout.getTime();
        boolean streakBroken = false;
        if (elapsedTime < MINIMUM_SEPARATION) {
            return new HBMain.SingleResponse("Unable to report workout: Too soon after last workout. Next workout available in "
                + Casino.formatTime(MINIMUM_SEPARATION - elapsedTime));
        } else if (elapsedTime > MAXIMUM_SEPARATION && user.streak > 0) {
            streakBroken = true;
            user = addWorkoutAndBreakStreak(uid);
        } else {
            user = addWorkout(uid);
        }

        if (user == null) {
            return new HBMain.SingleResponse("Unable to report workout: Updated user was null");
        }
        StringBuilder response = new StringBuilder();
        response.append(user.getTotalWorkoutString());
        response.append('\n');
        response.append(streakBroken ? STREAK_BROKEN_STRING : user.getStreakString());
        response.append('\n');
        response.append(awardReward(uid, user.selectedReward));
        return new HBMain.SingleResponse(response.toString(),
            streakBroken ? ButtonRows.WORKOUT_OFFER_VOLUNTARY_RESTORE : ButtonRows.WORKOUT_OFFER_VOLUNTARY_BREAK);
    }

    static HBMain.SingleResponse handleBreakStreak(long uid) {
        WorkoutUser user = getUser(uid);
        if (user == null) {
            return new HBMain.SingleResponse(Casino.USER_NOT_FOUND_MESSAGE);
        }

        user = breakStreak(uid);
        if (user == null) {
            return new HBMain.SingleResponse("Unable to break streak: Updated user was null");
        }
        return new HBMain.SingleResponse("Streak broken, streak is now "
                + user.streak + " day" + Casino.getPluralSuffix(user.streak),
            ButtonRows.WORKOUT_UNDO_BREAK);
    }

    static HBMain.SingleResponse handleRestoreStreak(long uid) {
        WorkoutUser user = getUser(uid);
        if (user == null) {
            return new HBMain.SingleResponse(Casino.USER_NOT_FOUND_MESSAGE);
        }

        if (user.previousStreak <= 0) {
            return new HBMain.SingleResponse("Unable to restore streak, no previous streak found to restore from ("
                + user.previousStreak + ")");
        } else if (user.streak != 1) {
            return new HBMain.SingleResponse("Unable to restore streak, expected current streak to be 1 day, was "
                + user.streak + " day" + Casino.getPluralSuffix(user.streak) + " instead");
        }
        user = restoreStreak(uid);
        if (user == null) {
            return new HBMain.SingleResponse("Unable to restore streak: Updated user was null");
        }
        return new HBMain.SingleResponse("Restored streak, streak is now "
                + user.streak + " day" + Casino.getPluralSuffix(user.streak),
            ButtonRows.WORKOUT_UNDO_RESTORE);
    }

    static String handleSelectReward(long uid, long selection) {
        int parsedSelect = (int)selection;
        if (parsedSelect != COIN_REWARD_ID && parsedSelect != PULL_REWARD_AMOUNT) {
            return "Unable to update workout reward: Unknown value " + parsedSelect;
        }
        WorkoutUser user = getUser(uid);
        if (user == null) {
            return Casino.USER_NOT_FOUND_MESSAGE;
        }
        if (parsedSelect == user.selectedReward) {
            return "Your workout reward is now " + getRewardDescription(parsedSelect)
                + ". (It is also what you already had selected)";
        }

        int updatedSelection = updateReward(uid, parsedSelect);
        return "Your workout reward is now " + getRewardDescription(updatedSelection);
    }

    private static String awardReward(long uid, int selection) {
        StringBuilder response = new StringBuilder("Received ");
        response.append(getRewardDescription(selection));
        long balance;
        switch (selection) {
            case PULL_REWARD_ID:
                balance = Gacha.addPulls(uid, PULL_REWARD_AMOUNT);
                response.append(". You now have ");
                response.append(balance);
                response.append(" pull");
                response.append(Casino.getPluralSuffix(balance));
                return response.toString();
            case COIN_REWARD_ID:
            default:
                balance = Casino.addMoney(uid, COIN_REWARD_AMOUNT);
                response.append(". Your new balance is ");
                response.append(balance);
                return response.toString();
        }
    }

    static String getRewardDescription(int selection) {
        switch (selection) {
            case PULL_REWARD_ID:
                return PULL_REWARD_AMOUNT + " pull";
            case COIN_REWARD_ID:
            default:
                return COIN_REWARD_AMOUNT + " coins";
        }
    }

    /////////////////////////////////////////////////////////////////////////////

    // CREATE TABLE IF NOT EXISTS workout_user (
    //   uid bigint PRIMARY KEY,
    //   streak integer NOT NULL DEFAULT 0,
    //   previous_streak integer NOT NULL DEFAULT 0,
    //   longest_streak integer NOT NULL DEFAULT 0,
    //   total_workouts integer NOT NULL DEFAULT 0,
    //   last_workout timestamp NOT NULL DEFAULT '2021-01-01 00:00:00',
    //   reward integer NOT NULL DEFAULT 1,
    //   CONSTRAINT workout_uid FOREIGN KEY(uid) REFERENCES money_user(uid)
    // );

    private static WorkoutUser executeWorkoutUserQuery(String query) {
        return CasinoDB.executeQueryWithReturn(query, results -> {
            if (results.next()) {
                int streak = results.getInt(1);
                int previousStreak = results.getInt(2);
                int totalWorkouts = results.getInt(3);
                Timestamp lastWorkout = results.getTimestamp(4);
                int selectedReward = results.getInt(5);
                return new WorkoutUser(streak, previousStreak, totalWorkouts, lastWorkout, selectedReward);
            }
            return null;
        }, null);
    }

    private static final String WORKOUT_USER_FIELDS = "streak, previous_streak, total_workouts, last_workout, reward";

    private static WorkoutUser getUser(long uid) {
        return executeWorkoutUserQuery("SELECT " + WORKOUT_USER_FIELDS + " FROM workout_user WHERE uid = "
            + uid + ";");
    }

    private static WorkoutUser addWorkout(long uid, boolean streakBroken) {
        return streakBroken ? addWorkoutAndBreakStreak(uid) : addWorkout(uid);
    }

    private static WorkoutUser addWorkout(long uid) {
        return executeWorkoutUserQuery("UPDATE workout_user SET (streak, total_workouts, last_workout, longest_streak) "
            + "= (streak + 1, total_workouts + 1, NOW(), GREATEST(longest_streak, streak + 1)) WHERE uid = "
            + uid + " RETURNING " + WORKOUT_USER_FIELDS + ";");
    }

    private static WorkoutUser addWorkoutAndBreakStreak(long uid) {
        return executeWorkoutUserQuery("UPDATE workout_user SET (previous_streak, streak, total_workouts, last_workout, longest_streak) "
            + "= (streak, 1, total_workouts + 1, NOW(), GREATEST(longest_streak, streak - 1)) WHERE uid = "
            + uid + " RETURNING " + WORKOUT_USER_FIELDS + ";");
    }

    private static WorkoutUser breakStreak(long uid) {
        return executeWorkoutUserQuery("UPDATE workout_user SET (previous_streak, streak, longest_streak) = (streak - 1, 1, GREATEST(longest_streak, streak - 1)) WHERE uid = "
            + uid + " RETURNING " + WORKOUT_USER_FIELDS + ";");
    }

    private static WorkoutUser restoreStreak(long uid) {
        return executeWorkoutUserQuery("UPDATE workout_user SET (streak, previous_streak) = (previous_streak + streak, 0) WHERE uid = "
            + uid + " RETURNING " + WORKOUT_USER_FIELDS + ";");
    }

    private static int updateReward(long uid, int reward) {
        return CasinoDB.executeIntQuery("UPDATE workout_user SET reward = " + reward + " WHERE uid = " + uid + " RETURNING reward;");
    }
}
