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

    static ActionRow makeAllOrNothingUnclaimable(AllOrNothing.Difficulty difficulty) {
        return ActionRow.of(Button.secondary("allornothing.prematureclaim", "Claim"),
            Button.success("allornothing.roll|" + difficulty.rollsToDouble, "Roll (" + difficulty.description + ")"));
    }

    static ActionRow makeAllOrNothingClaimable(AllOrNothing.Difficulty difficulty) {
        return ActionRow.of(Button.success("allornothing.claim|" + difficulty.rollsToDouble, "Claim"),
            Button.success("allornothing.roll|" + difficulty.rollsToDouble, "Roll (" + difficulty.description + ")"));
    }

    static ActionRow makeAllOrNothing(boolean claimable, AllOrNothing.Difficulty difficulty) {
        return claimable ? makeAllOrNothingClaimable(difficulty) : makeAllOrNothingUnclaimable(difficulty);
    }

}
