package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.RowType;

/** Mouse input coming from the board views (hover and clicks). All methods run on the JavaFX thread. */
interface BoardInput {

    /** The mouse is over a card: show it in the info panel. */
    void showCard(CardSnapshot card);

    /** The mouse is over a leader card. */
    void showLeader(int side);

    /** Primary click on a card in the hand. */
    void handCardClicked(int cardId);

    /**
     * Primary click on a card on the battlefield.
     *
     * @return true if the click was used (then it is not passed on to the row)
     */
    boolean boardCardClicked(int side, RowType row, int cardId);

    /** Primary click on a row (its card area or background). */
    void rowClicked(int side, RowType row);

    /** Primary click on a row's special slot (Commander's Horn / Mardroeme). */
    void slotClicked(int side, RowType row);

    /** Primary click on the weather zone. */
    void weatherClicked();

    /** Primary click on a leader card. */
    void leaderClicked(int side);

    /** Primary click on a discard pile. */
    void discardClicked(int side);

    /** The Pass button. */
    void passClicked();

    /** The Menu button. */
    void menuClicked();
}
