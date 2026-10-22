package com.numankaraaslan.gwent.engine.snapshot;

import com.numankaraaslan.gwent.engine.CardInstance;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.PlayerState;
import com.numankaraaslan.gwent.engine.RowState;
import com.numankaraaslan.gwent.engine.Rules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Builds immutable snapshots from the engine's mutable state. Call on the engine thread only. */
public final class Snapshots {

    private Snapshots() {
    }

    /** Snapshot as seen by {@code viewer}: the other player's hand is hidden (only its size is given). */
    public static GameSnapshot of(GameState state, int viewer) {
        List<PlayerSnapshot> players = new ArrayList<>();
        players.add(player(state, 0, viewer));
        players.add(player(state, 1, viewer));
        return new GameSnapshot(state.round(), state.currentPlayer(), state.firstPlayer(), state.phase(), List.copyOf(players), cards(state.weather()), Collections.unmodifiableSet(Rules.weatherRows(state)), List.copyOf(state.results()), state.winner());
    }

    /** A card outside the battlefield (hand, deck, discard, weather zone): strength = printed strength. */
    public static CardSnapshot card(CardInstance card) {
        return new CardSnapshot(card.id(), card.def(), card.def().isUnit() ? card.def().strength() : 0, card.owner());
    }

    public static List<CardSnapshot> cards(List<CardInstance> cards) {
        List<CardSnapshot> result = new ArrayList<>();
        for (CardInstance card : cards) {
            result.add(card(card));
        }
        return List.copyOf(result);
    }

    /** Cards that are possibly on the battlefield get their current strength; others their printed strength. */
    public static List<CardSnapshot> cardsWithStrength(GameState state, List<CardInstance> cards) {
        List<CardSnapshot> result = new ArrayList<>();
        for (CardInstance card : cards) {
            result.add(cardWithStrength(state, card));
        }
        return List.copyOf(result);
    }

    public static CardSnapshot cardWithStrength(GameState state, CardInstance card) {
        int side = Rules.sideOf(state, card);
        if (side < 0 || card.row() == null) {
            return card(card);
        }
        PlayerState player = state.player(side);
        RowState row = player.row(card.row());
        return new CardSnapshot(card.id(), card.def(), Rules.strength(state, player, row, card), card.owner());
    }

    private static PlayerSnapshot player(GameState state, int index, int viewer) {
        PlayerState player = state.player(index);
        List<CardSnapshot> hand = List.of();
        if (index == viewer) {
            List<CardSnapshot> sorted = new ArrayList<>(cards(player.hand()));
            Collections.sort(sorted, new HandOrder());
            hand = List.copyOf(sorted);
        }
        List<RowSnapshot> rows = new ArrayList<>();
        for (RowState row : player.rows()) {
            rows.add(row(state, player, row));
        }
        return new PlayerSnapshot(index, player.name(), player.faction(), player.leader(), player.leaderUsed(), player.leaderDisabled(), leaderUsable(state, index, viewer), player.gems(), player.passed(), player.hand().size(), hand, player.deck().size(), cards(player.discard()), List.copyOf(rows), Rules.score(state, index));
    }

    /**
     * For the viewer: whether the leader ability can be activated right now. For the other player only whether it is still
     * available at all (the exact answer would reveal the contents of their deck, e.g. "has a Fog card").
     */
    private static boolean leaderUsable(GameState state, int index, int viewer) {
        PlayerState player = state.player(index);
        if (index == viewer) {
            return Rules.isLeaderUsable(state, index);
        }
        return !player.leaderUsed() && !player.leaderDisabled() && !player.leader().isPassive();
    }

    private static RowSnapshot row(GameState state, PlayerState player, RowState row) {
        List<CardSnapshot> cards = new ArrayList<>();
        for (CardInstance card : row.cards()) {
            cards.add(new CardSnapshot(card.id(), card.def(), Rules.strength(state, player, row, card), card.owner()));
        }
        CardSnapshot special = row.special() == null ? null : card(row.special());
        return new RowSnapshot(row.type(), List.copyOf(cards), special, row.leaderHorn(), Rules.hasHorn(row), Rules.rowHasMardroeme(row), Rules.isWeatherActive(state, row.type()), Rules.rowScore(state, player, row));
    }

    /** Hand order: special and weather cards first, then units by strength, then by name. */
    private static final class HandOrder implements Comparator<CardSnapshot> {
        @Override
        public int compare(CardSnapshot a, CardSnapshot b) {
            int groupA = a.def().isUnit() ? 1 : 0;
            int groupB = b.def().isUnit() ? 1 : 0;
            if (groupA != groupB) {
                return Integer.compare(groupA, groupB);
            }
            if (groupA == 0) {
                int bySpecial = Integer.compare(a.def().special().ordinal(), b.def().special().ordinal());
                return bySpecial != 0 ? bySpecial : Integer.compare(a.id(), b.id());
            }
            int byStrength = Integer.compare(a.def().strength(), b.def().strength());
            if (byStrength != 0) {
                return byStrength;
            }
            int byName = a.def().name().compareTo(b.def().name());
            return byName != 0 ? byName : Integer.compare(a.id(), b.id());
        }
    }
}
