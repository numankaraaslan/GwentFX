package com.numankaraaslan.gwent.engine.snapshot;

import com.numankaraaslan.gwent.engine.GamePhase;
import com.numankaraaslan.gwent.engine.RoundResult;
import com.numankaraaslan.gwent.model.RowType;
import java.util.List;
import java.util.Set;

/**
 * Immutable picture of the whole game, safe to read from the JavaFX thread.
 *
 * @param round         1-based round number (0 before the first round)
 * @param currentPlayer whose turn it is
 * @param firstPlayer   who started (or starts) the current round
 * @param phase         coarse game phase
 * @param players       index 0 = human, 1 = computer
 * @param weather       weather cards in the weather zone
 * @param weatherRows   row types currently hit by bad weather (both sides)
 * @param results       finished rounds
 * @param winner        game winner (0 / 1), GameState.DRAW, or GameState.UNDECIDED
 */
public record GameSnapshot(int round, int currentPlayer, int firstPlayer, GamePhase phase, List<PlayerSnapshot> players, List<CardSnapshot> weather, Set<RowType> weatherRows, List<RoundResult> results, int winner) {

    public PlayerSnapshot player(int index) {
        return players.get(index);
    }

    public PlayerSnapshot human() {
        return players.get(0);
    }

    public PlayerSnapshot ai() {
        return players.get(1);
    }

    /** Finds a card anywhere visible in this snapshot (board, special slots, weather, discard piles, viewer's hand), or null. */
    public CardSnapshot findCard(int id) {
        for (CardSnapshot card : weather) {
            if (card.id() == id) {
                return card;
            }
        }
        for (PlayerSnapshot player : players) {
            for (RowSnapshot row : player.rows()) {
                for (CardSnapshot card : row.cards()) {
                    if (card.id() == id) {
                        return card;
                    }
                }
                if (row.special() != null && row.special().id() == id) {
                    return row.special();
                }
            }
            for (CardSnapshot card : player.hand()) {
                if (card.id() == id) {
                    return card;
                }
            }
            for (CardSnapshot card : player.discard()) {
                if (card.id() == id) {
                    return card;
                }
            }
        }
        return null;
    }
}
