package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.model.RowType;
import java.util.ArrayList;
import java.util.List;

/** One combat row of one player: the cards in it plus its special slot (Commander's Horn / Mardroeme). */
public final class RowState {

    private final RowType type;
    private final List<CardInstance> cards = new ArrayList<>();
    private CardInstance special;
    private boolean leaderHorn;

    public RowState(RowType type) {
        this.type = type;
    }

    public RowType type() {
        return type;
    }

    /** Unit cards (and decoys) in display order. Mutable. */
    public List<CardInstance> cards() {
        return cards;
    }

    /** Commander's Horn or Mardroeme card in the special slot, or null. */
    public CardInstance special() {
        return special;
    }

    public void setSpecial(CardInstance special) {
        this.special = special;
    }

    /** True when a leader ability (e.g. Foltest: The Siegemaster) doubles this row for the round. */
    public boolean leaderHorn() {
        return leaderHorn;
    }

    public void setLeaderHorn(boolean leaderHorn) {
        this.leaderHorn = leaderHorn;
    }

    /** The special slot can take a Commander's Horn / Mardroeme / leader horn. */
    public boolean isSlotFree() {
        return special == null && !leaderHorn;
    }

    public RowState copy() {
        RowState copy = new RowState(type);
        for (CardInstance card : cards) {
            copy.cards.add(card.copy());
        }
        copy.special = special == null ? null : special.copy();
        copy.leaderHorn = leaderHorn;
        return copy;
    }
}
