package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.ChoiceRequest;
import com.numankaraaslan.gwent.engine.Move;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.RowType;
import java.util.List;

/**
 * FX side of the human player's decisions. {@link HumanController} calls these methods on the JavaFX thread
 * (through {@code Platform.runLater}); each one shows its prompt and answers the given {@link Reply} once the player decided.
 */
interface PromptHost {

    /** Redraw phase: show the hand; answer the id of a card to swap, or -1 to keep the hand. */
    void promptRedraw(List<CardSnapshot> hand, int redrawsLeft, Reply<Integer> reply);

    /** Scoia'tael perk: answer true to go first, false to let the opponent start. */
    void promptGoFirst(Reply<Boolean> reply);

    /** The human's turn: enable the board input; answer one of {@code legalMoves}. */
    void promptMove(List<Move> legalMoves, Reply<Move> reply);

    /** Pick between {@code request.min()} and {@code request.max()} of the offered cards; answer their ids. */
    void promptCards(ChoiceRequest request, Reply<List<Integer>> reply);

    /** Pick the row for an agile unit that an effect puts on the battlefield. */
    void promptRow(CardSnapshot card, List<RowType> options, Reply<RowType> reply);

    /** Show some cards; answer anything once the player acknowledged them. */
    void promptReveal(String title, List<CardSnapshot> cards, Reply<Boolean> reply);
}
