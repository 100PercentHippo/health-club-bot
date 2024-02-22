package com.c2t2s.hb;

import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;

class ButtonRows {

    // Hide default constructor
    private ButtonRows() {}

    static ActionRow OVERUNDER_BUTTONS = ActionRow.of(Button.secondary("overunder.over", "Over"),
        Button.secondary("overunder.under", "Under"),
        Button.secondary("overunder.same", "Same"));

    static ActionRow BLACKJACK_BUTTONS = ActionRow.of(Button.secondary("blackjack.hit", "Hit"),
        Button.secondary("blackjack.stand", "Stand"));
    static ActionRow makeBlackJackSplit(long wager) {
        return ActionRow.of(Button.secondary("blackjack.hit", "Hit"),
            Button.secondary("blackjack.stand", "Stand"),
            Button.secondary("blackjack.split", "Split (Costs " + wager + " coin" + Casino.getPluralSuffix(wager) + ")"));
    }

    static ActionRow makeAllOrNothingUnclaimable(AllOrNothing.ActiveGame activeGame) {
        return ActionRow.of(Button.secondary("allornothing.claim|" + activeGame.difficulty.rollsToDouble, "Claim " + activeGame.getPotentialPayout()),
            Button.success("allornothing.roll|" + activeGame.difficulty.rollsToDouble, "Roll for " + activeGame.getNextRollPayout()
                + " (" + activeGame.difficulty.description + ")"));
    }

    static ActionRow makeAllOrNothingClaimable(AllOrNothing.ActiveGame activeGame) {
        return ActionRow.of(Button.success("allornothing.claim|" + activeGame.difficulty.rollsToDouble, "Claim " + activeGame.getPotentialPayout()),
            Button.success("allornothing.roll|" + activeGame.difficulty.rollsToDouble, "Roll for " + activeGame.getNextRollPayout()
                + " (" + activeGame.difficulty.description + ")"));
    }

    static ActionRow makeAllOrNothing(AllOrNothing.ActiveGame activeGame) {
        return activeGame.isClaimable() ? makeAllOrNothingClaimable(activeGame) : makeAllOrNothingUnclaimable(activeGame);
    }

    static ActionRow makeDeathroll(int max) {
        return ActionRow.of(Button.success("roll.deathroll|" + max, "Roll 1-" + max));
    }

    static ActionRow WORKOUT_OFFER_VOLUNTARY_BREAK = ActionRow.of(Button.secondary("workout.break", "Manually Break Streak"));
    static ActionRow WORKOUT_OFFER_VOLUNTARY_RESTORE = ActionRow.of(Button.secondary("workout.restore", "Manually Restore Streak"));
    static ActionRow WORKOUT_UNDO_BREAK = ActionRow.of(Button.secondary("workout.restore", "Undo"));
    static ActionRow WORKOUT_UNDO_RESTORE = ActionRow.of(Button.secondary("workout.break", "Undo"));

}
