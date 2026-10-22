package com.numankaraaslan.gwent.engine;

import java.util.List;

/**
 * Brain of one player (human UI or computer). All methods are called on the engine thread and may block.
 * Implementations may read {@code state} but must never modify it.
 */
public interface PlayerController extends Decider {

    /**
     * Redraw phase: called repeatedly while redraws are left. Return the id of a hand card to swap for a random card
     * from the deck, or -1 to keep the hand.
     */
    int chooseRedraw(GameState state, int player, int redrawsLeft);

    /** Scoia'tael perk: return true to go first in round 1, false to let the opponent start. */
    boolean chooseGoFirst(GameState state, int player);

    /** The turn decision. Must return one of {@code legalMoves} (it always contains PASS). */
    Move chooseMove(GameState state, int player, List<Move> legalMoves);
}
