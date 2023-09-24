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

    static ActionRow makeAllOrNothingUnclaimable(AllOrNothing.ActiveGame activeGame) {
        return ActionRow.of(Button.secondary("allornothing.prematureclaim", "Claim " + activeGame.getPotentialPayout()),
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

}
