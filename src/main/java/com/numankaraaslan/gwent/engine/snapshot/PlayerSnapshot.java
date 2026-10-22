package com.numankaraaslan.gwent.engine.snapshot;

import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.model.RowType;
import java.util.List;

/**
 * Immutable view of one player.
 *
 * @param index          0 = human (bottom), 1 = computer (top)
 * @param name           "You" / "Opponent"
 * @param faction        the player's faction
 * @param leader         the player's leader card
 * @param leaderUsed     the leader ability was already used
 * @param leaderDisabled the leader was cancelled by Emhyr: The White Flame
 * @param leaderUsable   viewer: the leader ability could be activated right now (ignores whose turn it is); opponent: the
 *                       ability is still available (not used, cancelled or passive)
 * @param gems           remaining round lives (2 at start)
 * @param passed         passed in the current round
 * @param handCount      number of cards in hand
 * @param hand           the hand cards, only for the viewing player (empty list for the opponent)
 * @param deckCount      cards left in the deck
 * @param discard        discard pile, oldest first
 * @param rows           rows in CLOSE, RANGED, SIEGE order
 * @param score          total current strength on this side
 */
public record PlayerSnapshot(int index, String name, Faction faction, LeaderDef leader, boolean leaderUsed, boolean leaderDisabled, boolean leaderUsable, int gems, boolean passed, int handCount, List<CardSnapshot> hand, int deckCount, List<CardSnapshot> discard, List<RowSnapshot> rows, int score) {

    public RowSnapshot row(RowType type) {
        return rows.get(type.ordinal());
    }
}
