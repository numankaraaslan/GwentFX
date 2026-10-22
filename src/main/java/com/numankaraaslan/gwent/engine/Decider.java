package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.RowType;
import java.util.List;

/**
 * Decisions a player has to make while a move is being resolved. All methods are called on the engine thread
 * (never on the JavaFX thread) and may block until the player has decided. They may read {@code state} but must not modify it.
 */
public interface Decider {

    /** Pick between request.min() and request.max() cards; return their ids. Invalid answers are corrected by the engine. */
    List<Integer> chooseCards(GameState state, int player, ChoiceRequest request);

    /**
     * Pick the row for an agile unit that is put on the battlefield by an effect. Not called by the current rules:
     * like the original game, the engine places such units in their stronger row automatically. Kept for rule variants.
     */
    RowType chooseRow(GameState state, int player, CardSnapshot card, List<RowType> options);

    /** Show the player some cards (Emhyr: Emperor of Nilfgaard looks at the opponent's hand). Return once acknowledged. */
    void reveal(GameState state, int player, String title, List<CardSnapshot> cards);
}
