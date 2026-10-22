package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.LeaderAbility;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Read-only rule queries: strength calculation, legal moves, leader usability, scorch targets.
 * Nothing here modifies the state.
 */
public final class Rules {

    /** Row scorch (Villentretenmerth, leader scorches...) needs at least this much strength in the enemy row. */
    public static final int SCORCH_ROW_THRESHOLD = 10;
    /** Cards drawn when playing a spy. */
    public static final int SPY_DRAW_COUNT = 2;
    /** Cards the Skellige perk brings back at the start of round 3. */
    public static final int SKELLIGE_RETURN_COUNT = 2;
    /** Random hand cards Emhyr: Emperor of Nilfgaard reveals. */
    public static final int LOOK_AT_HAND_COUNT = 3;

    private Rules() {
    }

    public static int opponent(int player) {
        return 1 - player;
    }

    // ------------------------------------------------------------------ weather & passives

    /** True if bad weather currently hits this row type (on both sides). */
    public static boolean isWeatherActive(GameState state, RowType row) {
        for (CardInstance card : state.weather()) {
            if (card.def().special().affectedRows().contains(row)) {
                return true;
            }
        }
        return false;
    }

    public static Set<RowType> weatherRows(GameState state) {
        Set<RowType> rows = EnumSet.noneOf(RowType.class);
        for (RowType row : RowType.values()) {
            if (isWeatherActive(state, row)) {
                rows.add(row);
            }
        }
        return rows;
    }

    /** True if either player has this (passive) leader ability and it is not cancelled. */
    public static boolean isPassiveActive(GameState state, LeaderAbility ability) {
        return state.player(0).leaderActive(ability) || state.player(1).leaderActive(ability);
    }

    // ------------------------------------------------------------------ row effects

    /** True if anything doubles this row: a Commander's Horn card, a leader horn or a horn unit (Dandelion...). */
    public static boolean hasHorn(RowState row) {
        return hornAppliesTo(row, null);
    }

    /** True if berserkers in this row transform (Mardroeme in the slot, or a Mardroeme unit like Ermion in the row). */
    public static boolean rowHasMardroeme(RowState row) {
        if (row.special() != null && row.special().def().special() == SpecialType.MARDROEME) {
            return true;
        }
        for (CardInstance card : row.cards()) {
            if (card.def().isUnit() && card.def().ability() == Ability.MARDROEME) {
                return true;
            }
        }
        return false;
    }

    /** Horn effect for one card: a horn unit doesn't double itself, but it does double everything else in its row. */
    private static boolean hornAppliesTo(RowState row, CardInstance card) {
        if (row.leaderHorn()) {
            return true;
        }
        if (row.special() != null && row.special().def().special() == SpecialType.COMMANDERS_HORN) {
            return true;
        }
        for (CardInstance other : row.cards()) {
            if (other != card && other.def().isUnit() && other.def().ability() == Ability.HORN) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ strength

    /**
     * Current strength of {@code card} in {@code row} of {@code side}. Works for cards that are not (yet) in that row too,
     * which gives "what would it be worth there". Order: weather, tight bond, morale boost, horn, spy doubling.
     * Heroes always keep their printed strength. Non-unit cards (decoys) are worth 0.
     */
    public static int strength(GameState state, PlayerState side, RowState row, CardInstance card) {
        CardDef def = card.def();
        if (!def.isUnit()) {
            return 0;
        }
        if (def.hero()) {
            return def.strength();
        }
        int value = def.strength();
        if (isWeatherActive(state, row.type())) {
            if (side.leaderActive(LeaderAbility.HALF_WEATHER)) {
                value = value - value / 2;
            } else {
                value = Math.min(value, 1);
            }
        }
        if (def.ability() == Ability.TIGHT_BOND) {
            int bonded = 1;
            for (CardInstance other : row.cards()) {
                if (other != card && other.def().isUnit() && other.def().ability() == Ability.TIGHT_BOND && other.def().name().equals(def.name())) {
                    bonded++;
                }
            }
            value = value * bonded;
        }
        for (CardInstance other : row.cards()) {
            if (other != card && other.def().isUnit() && other.def().ability() == Ability.MORALE_BOOST) {
                value++;
            }
        }
        if (hornAppliesTo(row, card)) {
            value = value * 2;
        }
        if (def.ability() == Ability.SPY && isPassiveActive(state, LeaderAbility.DOUBLE_SPIES)) {
            value = value * 2;
        }
        return value;
    }

    public static int rowScore(GameState state, PlayerState side, RowState row) {
        int total = 0;
        for (CardInstance card : row.cards()) {
            total += strength(state, side, row, card);
        }
        return total;
    }

    /** Total strength on a player's side of the battlefield. */
    public static int score(GameState state, int player) {
        PlayerState side = state.player(player);
        int total = 0;
        for (RowState row : side.rows()) {
            total += rowScore(state, side, row);
        }
        return total;
    }

    /** Row an agile card is worth most in (ties go to the first row); the only row for other units. */
    public static RowType bestRow(GameState state, int side, CardInstance card) {
        CardDef def = card.def();
        if (!def.isAgile()) {
            return def.defaultRow();
        }
        PlayerState player = state.player(side);
        RowType best = def.defaultRow();
        int bestValue = Integer.MIN_VALUE;
        for (RowType row : RowType.values()) {
            if (def.rows().contains(row)) {
                int value = strength(state, player, player.row(row), card);
                if (value > bestValue) {
                    best = row;
                    bestValue = value;
                }
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ scorch

    /**
     * Units the Scorch card would kill right now: the strongest non-hero unit(s) on the whole battlefield. Like the
     * original, this also burns 0-strength units when nothing stronger is on the board.
     */
    public static List<CardInstance> scorchVictims(GameState state) {
        int max = Integer.MIN_VALUE;
        for (int p = 0; p < 2; p++) {
            PlayerState side = state.player(p);
            for (RowState row : side.rows()) {
                for (CardInstance card : row.cards()) {
                    if (card.def().isUnit() && !card.def().hero()) {
                        max = Math.max(max, strength(state, side, row, card));
                    }
                }
            }
        }
        List<CardInstance> victims = new ArrayList<>();
        if (max == Integer.MIN_VALUE) {
            return victims;
        }
        for (int p = 0; p < 2; p++) {
            PlayerState side = state.player(p);
            for (RowState row : side.rows()) {
                for (CardInstance card : row.cards()) {
                    if (card.def().isUnit() && !card.def().hero() && strength(state, side, row, card) == max) {
                        victims.add(card);
                    }
                }
            }
        }
        return victims;
    }

    /**
     * Units a row scorch would kill: the strongest non-hero unit(s) of {@code targetSide}'s row, but only if that row's
     * total strength (heroes included) is at least {@link #SCORCH_ROW_THRESHOLD}.
     */
    public static List<CardInstance> rowScorchVictims(GameState state, int targetSide, RowType rowType) {
        PlayerState side = state.player(targetSide);
        RowState row = side.row(rowType);
        List<CardInstance> victims = new ArrayList<>();
        if (rowScore(state, side, row) < SCORCH_ROW_THRESHOLD) {
            return victims;
        }
        int max = Integer.MIN_VALUE;
        for (CardInstance card : row.cards()) {
            if (card.def().isUnit() && !card.def().hero()) {
                max = Math.max(max, strength(state, side, row, card));
            }
        }
        if (max == Integer.MIN_VALUE) {
            return victims;
        }
        for (CardInstance card : row.cards()) {
            if (card.def().isUnit() && !card.def().hero() && strength(state, side, row, card) == max) {
                victims.add(card);
            }
        }
        return victims;
    }

    /** The non-hero unit cards in {@code cards} (e.g. what the Monsters perk can keep on the battlefield). */
    public static List<CardInstance> nonHeroUnits(List<CardInstance> cards) {
        List<CardInstance> result = new ArrayList<>();
        for (CardInstance card : cards) {
            if (card.def().isUnit() && !card.def().hero()) {
                result.add(card);
            }
        }
        return result;
    }

    /**
     * The non-hero, non-token unit cards in {@code cards}: what Medic, Skellige and the discard-pile leaders can bring back.
     * Tokens (e.g. Bovine Defense Force) only exist on the battlefield.
     */
    public static List<CardInstance> revivableUnits(List<CardInstance> cards) {
        List<CardInstance> result = new ArrayList<>();
        for (CardInstance card : cards) {
            if (card.def().isUnit() && !card.def().hero() && !card.def().token()) {
                result.add(card);
            }
        }
        return result;
    }

    /** The berserker a transformed card came from, or null if {@code def} is not a transformed card. */
    public static CardDef transformationSource(CardDatabase db, CardDef def) {
        for (CardDef candidate : db.cards()) {
            if (def.id().equals(candidate.transformsTo())) {
                return candidate;
            }
        }
        return null;
    }

    /** True if playing this weather card now would change anything (a new bad weather, or clearing active weather). */
    public static boolean weatherWouldChange(GameState state, CardDef def) {
        if (!def.isWeather()) {
            return false;
        }
        if (def.special() == SpecialType.CLEAR_WEATHER) {
            return !state.weather().isEmpty();
        }
        return findSpecial(state.weather(), def.special()) == null;
    }

    /** Which side's rows (or special slots) hold this card, or -1. */
    public static int sideOf(GameState state, CardInstance card) {
        for (int p = 0; p < 2; p++) {
            for (RowState row : state.player(p).rows()) {
                if (row.special() == card || row.cards().contains(card)) {
                    return p;
                }
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ agile

    /** Agile units of {@code player} that would get stronger in their other row (Francesca: Hope of the Aen Seidhe). */
    public static List<CardInstance> agileImprovements(GameState state, int player) {
        PlayerState side = state.player(player);
        List<CardInstance> result = new ArrayList<>();
        for (RowState row : side.rows()) {
            for (CardInstance card : row.cards()) {
                CardDef def = card.def();
                if (!def.isUnit() || def.hero() || !def.isAgile()) {
                    continue;
                }
                RowType other = otherAgileRow(def, row.type());
                if (other == null) {
                    continue;
                }
                int now = strength(state, side, row, card);
                int moved = strength(state, side, side.row(other), card);
                if (moved > now) {
                    result.add(card);
                }
            }
        }
        return result;
    }

    /** The other row an agile card may stand in, or null. */
    public static RowType otherAgileRow(CardDef def, RowType current) {
        for (RowType row : RowType.values()) {
            if (row != current && def.rows().contains(row)) {
                return row;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ moves

    /** Every legal move for {@code player} right now. Always ends with PASS. */
    public static List<Move> legalMoves(GameState state, int player) {
        List<Move> moves = new ArrayList<>();
        PlayerState me = state.player(player);
        for (CardInstance card : me.hand()) {
            moves.addAll(movesForCard(me, card));
        }
        if (isLeaderUsable(state, player)) {
            moves.add(Move.leader());
        }
        moves.add(Move.pass());
        return moves;
    }

    /** Legal ways to play one hand card. */
    public static List<Move> movesForCard(PlayerState me, CardInstance card) {
        List<Move> moves = new ArrayList<>();
        CardDef def = card.def();
        if (def.isUnit()) {
            for (RowType row : RowType.values()) {
                if (def.rows().contains(row)) {
                    moves.add(Move.play(card.id(), row));
                }
            }
            return moves;
        }
        if (def.isWeather()) {
            moves.add(Move.play(card.id(), null));
            return moves;
        }
        switch (def.special()) {
            case COMMANDERS_HORN, MARDROEME -> {
                for (RowState row : me.rows()) {
                    if (row.isSlotFree()) {
                        moves.add(Move.play(card.id(), row.type()));
                    }
                }
            }
            case DECOY -> {
                for (RowState row : me.rows()) {
                    for (CardInstance target : row.cards()) {
                        if (target.def().isUnit() && !target.def().hero()) {
                            moves.add(Move.decoy(card.id(), row.type(), target.id()));
                        }
                    }
                }
            }
            case SCORCH -> moves.add(Move.play(card.id(), null));
            default -> {
            }
        }
        return moves;
    }

    /** True if the player's leader ability can be activated now and would actually do something. */
    public static boolean isLeaderUsable(GameState state, int player) {
        PlayerState me = state.player(player);
        PlayerState opponent = state.opponentOf(player);
        LeaderAbility ability = me.leader().ability();
        if (me.leaderUsed() || me.leaderDisabled() || ability.isPassive()) {
            return false;
        }
        switch (ability) {
            case FOG_FROM_DECK, RAIN_FROM_DECK, FROST_FROM_DECK -> {
                CardInstance card = findSpecial(me.deck(), ability.weatherFromDeck());
                return card != null && weatherWouldChange(state, card.def());
            }
            case WEATHER_FROM_DECK -> {
                for (CardInstance card : me.deck()) {
                    if (weatherWouldChange(state, card.def())) {
                        return true;
                    }
                }
                return false;
            }
            case CLEAR_WEATHER -> {
                return !state.weather().isEmpty();
            }
            case HORN_CLOSE, HORN_RANGED, HORN_SIEGE -> {
                return me.row(ability.hornRow()).isSlotFree();
            }
            case SCORCH_CLOSE, SCORCH_RANGED, SCORCH_SIEGE -> {
                return !rowScorchVictims(state, opponent(player), ability.scorchRow()).isEmpty();
            }
            case LOOK_AT_HAND -> {
                return !opponent.hand().isEmpty();
            }
            case DRAW_FROM_OPPONENT_DISCARD -> {
                return !revivableUnits(opponent.discard()).isEmpty();
            }
            case RESTORE_FROM_DISCARD -> {
                return !revivableUnits(me.discard()).isEmpty();
            }
            case DISCARD_2_DRAW_1 -> {
                return me.hand().size() >= 2 && !me.deck().isEmpty();
            }
            case OPTIMIZE_AGILE -> {
                return !agileImprovements(state, player).isEmpty();
            }
            case SHUFFLE_GRAVEYARDS -> {
                return !me.discard().isEmpty() || !opponent.discard().isEmpty();
            }
            default -> {
                return false;
            }
        }
    }

    /** First card with the given special effect in {@code cards}, or null. */
    public static CardInstance findSpecial(List<CardInstance> cards, SpecialType type) {
        for (CardInstance card : cards) {
            if (card.def().special() == type) {
                return card;
            }
        }
        return null;
    }
}
