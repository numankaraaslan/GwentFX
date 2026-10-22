package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.model.CardDef;
import java.util.Collections;

/**
 * Builds a player's deck: every card of the faction plus every neutral card, each as many times as it exists in the
 * original game's collection, shuffled. There is no deck building - the starting hand is simply drawn from this pool.
 */
public final class DeckBuilder {

    private DeckBuilder() {
    }

    public static void fill(GameState state, int player) {
        PlayerState side = state.player(player);
        for (CardDef def : state.db().deckPool(side.faction())) {
            for (int i = 0; i < def.copies(); i++) {
                side.deck().add(new CardInstance(state.newCardId(), def, player));
            }
        }
        Collections.shuffle(side.deck(), state.random());
    }
}
