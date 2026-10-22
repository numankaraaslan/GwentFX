package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.RowType;

/**
 * One physical card in a game. The id is unique for the whole game and never changes,
 * even when the card moves between zones or a berserker transforms (only {@link #def()} changes then).
 */
public final class CardInstance {

    private final int id;
    private final int owner;
    private CardDef def;
    private RowType row;
    private int playedBy;

    public CardInstance(int id, CardDef def, int owner) {
        this.id = id;
        this.def = def;
        this.owner = owner;
        this.playedBy = owner;
    }

    public int id() {
        return id;
    }

    /** Index of the player whose deck this card was built into. */
    public int owner() {
        return owner;
    }

    public CardDef def() {
        return def;
    }

    /** Used by berserker transformation. */
    public void setDef(CardDef def) {
        this.def = def;
    }

    /** Row the card sits in while on the battlefield (also for horn/mardroeme in a row's special slot), else null. */
    public RowType row() {
        return row;
    }

    public void setRow(RowType row) {
        this.row = row;
    }

    /** Player who last played the card (used to return weather cards to the right discard pile). */
    public int playedBy() {
        return playedBy;
    }

    public void setPlayedBy(int playedBy) {
        this.playedBy = playedBy;
    }

    public CardInstance copy() {
        CardInstance copy = new CardInstance(id, def, owner);
        copy.row = row;
        copy.playedBy = playedBy;
        return copy;
    }

    @Override
    public String toString() {
        return def.name() + "#" + id;
    }
}
