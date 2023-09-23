package com.c2t2s.hb;

import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;

class ButtonRows {

    static ActionRow OVERUNDER_BUTTONS = ActionRow.of(Button.secondary("overunder.over", "Over"),
        Button.secondary("overunder.under", "Under"),
        Button.secondary("overunder.same", "Same"));

    static ActionRow BLACKJACK_BUTTONS = ActionRow.of(Button.secondary("blackjack.hit", "Hit"),
        Button.secondary("blackjack.stand", "Stand"));

}
