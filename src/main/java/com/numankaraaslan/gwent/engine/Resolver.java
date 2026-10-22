package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.Snapshots;
import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderAbility;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Applies moves and game-flow steps to a {@link GameState}. This is the only class that changes the state during play.
 * The same code runs the real game (with UI / AI deciders and an event sink feeding the UI) and the AI's look-ahead
 * (on a copied state with simulated deciders and {@link EventSink#NONE}).
 */
public final class Resolver {

    private final GameState state;
    private final Decider[] deciders;
    private final EventSink sink;

    public Resolver(GameState state, Decider decider0, Decider decider1, EventSink sink) {
        this.state = state;
        this.deciders = new Decider[] {decider0, decider1};
        this.sink = sink;
    }

    public GameState state() {
        return state;
    }

    // ================================================================== turn moves

    /** Applies a move that was taken from {@link Rules#legalMoves}. */
    public void applyMove(int player, Move move) {
        switch (move.type()) {
            case PASS -> pass(player, false);
            case LEADER -> useLeader(player);
            case PLAY -> playFromHand(player, move);
        }
    }

    /** Marks the player as passed. {@code forced} is true when they had nothing left to do. */
    public void pass(int player, boolean forced) {
        PlayerState me = state.player(player);
        me.setPassed(true);
        String text = forced ? me.name() + " had no moves left and passed." : me.name() + " passed.";
        emit(EventType.PLAYER_PASSED, player, List.of(), null, text);
    }

    private void playFromHand(int player, Move move) {
        PlayerState me = state.player(player);
        CardInstance card = me.findInHand(move.cardId());
        if (card == null) {
            throw new IllegalArgumentException("Card " + move.cardId() + " is not in the hand of player " + player);
        }
        me.hand().remove(card);
        card.setPlayedBy(player);
        CardDef def = card.def();
        if (def.isUnit()) {
            playUnit(player, card, move.row(), EventType.CARD_PLAYED);
            return;
        }
        if (def.isWeather()) {
            playWeather(player, card);
            return;
        }
        switch (def.special()) {
            case COMMANDERS_HORN -> playHorn(player, card, move.row());
            case MARDROEME -> playMardroeme(player, card, move.row());
            case DECOY -> playDecoy(player, card, move.row(), move.targetId());
            case SCORCH -> playScorch(player, card);
            default -> throw new IllegalStateException("Unsupported special card " + def.id());
        }
    }

    // ================================================================== units

    /** Puts a unit on the battlefield (spies go to the opponent's side) and resolves its ability. */
    private void playUnit(int player, CardInstance card, RowType requestedRow, EventType eventType) {
        CardDef def = card.def();
        int side = def.isSpy() ? Rules.opponent(player) : player;
        RowType row = requestedRow != null && def.rows().contains(requestedRow) ? requestedRow : def.defaultRow();
        card.setPlayedBy(player);
        place(side, card, row);
        String verb = eventType == EventType.CARD_REVIVED ? " revived " : " played ";
        String where = def.isSpy() ? " as a spy (" + row.displayName() + ")." : " (" + row.displayName() + ").";
        emit(eventType, player, List.of(card.id()), row, state.player(player).name() + verb + def.name() + where);
        checkBerserker(side, card);
        resolveAbility(player, side, card);
    }

    /** Adds a card to a row, next to cards with the same name so bonded cards stand together. */
    private void place(int side, CardInstance card, RowType rowType) {
        RowState row = state.player(side).row(rowType);
        card.setRow(rowType);
        int insertAt = row.cards().size();
        for (int i = 0; i < row.cards().size(); i++) {
            if (row.cards().get(i).def().name().equals(card.def().name())) {
                insertAt = i + 1;
            }
        }
        row.cards().add(insertAt, card);
    }

    private void resolveAbility(int player, int side, CardInstance card) {
        CardDef def = card.def();
        switch (def.ability()) {
            case SPY -> draw(player, Rules.SPY_DRAW_COUNT);
            case MEDIC -> medic(player);
            case MUSTER -> muster(player, card);
            case SCORCH -> scorchBattlefield(player, def.name());
            case SCORCH_CLOSE, SCORCH_RANGED, SCORCH_SIEGE -> scorchRow(player, Rules.opponent(player), def.ability().scorchRow(), def.name());
            case MARDROEME -> transformBerserkers(side, card.row());
            default -> {
            }
        }
    }

    private void medic(int player) {
        PlayerState me = state.player(player);
        List<CardInstance> candidates = Rules.revivableUnits(me.discard());
        if (candidates.isEmpty()) {
            emit(EventType.MESSAGE, player, List.of(), null, "Medic: there is no unit to revive.");
            return;
        }
        CardInstance chosen;
        if (Rules.isPassiveActive(state, LeaderAbility.RANDOM_MEDIC)) {
            chosen = candidates.get(state.random().nextInt(candidates.size()));
        } else {
            chosen = chooseOne(player, ChoiceKind.MEDIC, "Medic: choose a unit to revive", candidates);
        }
        me.discard().remove(chosen);
        int side = chosen.def().isSpy() ? Rules.opponent(player) : player;
        playUnit(player, chosen, autoRow(side, chosen), EventType.CARD_REVIVED);
    }

    /** Row for a unit placed by an effect: agile units go where they are stronger (ties are random), as in the original. */
    private RowType autoRow(int side, CardInstance card) {
        if (!card.def().isAgile()) {
            return card.def().defaultRow();
        }
        PlayerState player = state.player(side);
        RowType best = null;
        int bestValue = Integer.MIN_VALUE;
        int ties = 0;
        for (RowType row : RowType.values()) {
            if (!card.def().rows().contains(row)) {
                continue;
            }
            int value = Rules.strength(state, player, player.row(row), card);
            if (value > bestValue) {
                best = row;
                bestValue = value;
                ties = 1;
            } else if (value == bestValue) {
                ties++;
                if (state.random().nextInt(ties) == 0) {
                    best = row;
                }
            }
        }
        return best;
    }

    /** Muster: every card of the summoned group in the deck AND the hand joins the battlefield (as in the original game). */
    private void muster(int player, CardInstance source) {
        String tag = source.def().summons();
        if (tag.isEmpty()) {
            return;
        }
        PlayerState me = state.player(player);
        List<CardInstance> pulled = new ArrayList<>();
        for (CardInstance card : me.deck()) {
            if (tag.equals(card.def().group())) {
                pulled.add(card);
            }
        }
        for (CardInstance card : me.hand()) {
            if (tag.equals(card.def().group())) {
                pulled.add(card);
            }
        }
        if (pulled.isEmpty()) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (CardInstance card : pulled) {
            me.deck().remove(card);
            me.hand().remove(card);
            card.setPlayedBy(player);
            int side = card.def().isSpy() ? Rules.opponent(player) : player;
            place(side, card, Rules.bestRow(state, side, card));
            ids.add(card.id());
        }
        emit(EventType.CARDS_SUMMONED, player, ids, null, source.def().name() + " mustered " + describe(pulled) + ".");
        for (CardInstance card : pulled) {
            checkBerserker(Rules.sideOf(state, card), card);
        }
    }

    private void checkBerserker(int side, CardInstance card) {
        if (side < 0 || card.row() == null || card.def().ability() != Ability.BERSERKER) {
            return;
        }
        if (Rules.rowHasMardroeme(state.player(side).row(card.row()))) {
            transform(side, card);
        }
    }

    private void transformBerserkers(int side, RowType rowType) {
        List<CardInstance> berserkers = new ArrayList<>();
        for (CardInstance card : state.player(side).row(rowType).cards()) {
            if (card.def().isUnit() && card.def().ability() == Ability.BERSERKER) {
                berserkers.add(card);
            }
        }
        for (CardInstance card : berserkers) {
            transform(side, card);
        }
    }

    private void transform(int side, CardInstance card) {
        CardDef target = state.db().findCard(card.def().transformsTo());
        if (target == null) {
            return;
        }
        String before = card.def().name();
        card.setDef(target);
        emit(EventType.CARD_TRANSFORMED, side, List.of(card.id()), card.row(), before + " transformed into " + target.name() + "!");
    }

    // ================================================================== scorch

    private void scorchBattlefield(int player, String source) {
        List<CardInstance> victims = Rules.scorchVictims(state);
        if (victims.isEmpty()) {
            emit(EventType.MESSAGE, player, List.of(), null, source + ": nothing to burn.");
            return;
        }
        destroy(victims, source);
    }

    private void scorchRow(int player, int targetSide, RowType row, String source) {
        List<CardInstance> victims = Rules.rowScorchVictims(state, targetSide, row);
        if (victims.isEmpty()) {
            emit(EventType.MESSAGE, player, List.of(), row, source + ": the enemy " + row.displayName() + " row is not strong enough (needs " + Rules.SCORCH_ROW_THRESHOLD + "+).");
            return;
        }
        destroy(victims, source);
    }

    private void destroy(List<CardInstance> victims, String source) {
        List<PendingSummon> avengers = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        for (CardInstance victim : victims) {
            int side = Rules.sideOf(state, victim);
            if (side < 0) {
                continue;
            }
            state.player(side).row(victim.row()).cards().remove(victim);
            leaveBattlefield(victim);
            state.player(side).discard().add(victim);
            ids.add(victim.id());
            if (victim.def().ability() == Ability.AVENGER) {
                avengers.add(new PendingSummon(side, victim));
            }
        }
        emit(EventType.CARDS_DESTROYED, -1, ids, null, source + " destroyed " + describe(victims) + ".");
        for (PendingSummon pending : avengers) {
            summonAvenger(pending.side(), pending.card());
        }
    }

    /** A card leaves the battlefield: it loses its row, and a transformed berserker turns back into the berserker card. */
    private void leaveBattlefield(CardInstance card) {
        card.setRow(null);
        if (card.def().token()) {
            CardDef original = Rules.transformationSource(state.db(), card.def());
            if (original != null) {
                card.setDef(original);
            }
        }
    }

    private void summonAvenger(int side, CardInstance source) {
        CardDef summoned = state.db().findCard(source.def().avengerSummons());
        if (summoned == null) {
            return;
        }
        CardInstance token = new CardInstance(state.newCardId(), summoned, side);
        place(side, token, summoned.defaultRow());
        emit(EventType.CARDS_SUMMONED, side, List.of(token.id()), token.row(), source.def().name() + " summoned " + summoned.name() + "!");
        checkBerserker(side, token);
    }

    // ================================================================== special & weather cards

    private void playWeather(int player, CardInstance card) {
        PlayerState me = state.player(player);
        SpecialType type = card.def().special();
        if (type == SpecialType.CLEAR_WEATHER) {
            me.discard().add(card);
            clearWeather(player, card.def().name(), card);
            return;
        }
        for (CardInstance existing : state.weather()) {
            if (existing.def().special() == type) {
                me.discard().add(card);
                emit(EventType.WEATHER_PLAYED, player, List.of(card.id()), null, me.name() + " played " + card.def().name() + ", but it was already in effect.");
                return;
            }
        }
        state.weather().add(card);
        emit(EventType.WEATHER_PLAYED, player, List.of(card.id()), null, me.name() + " played " + card.def().name() + ".");
    }

    private void clearWeather(int player, String source, CardInstance clearCard) {
        List<CardInstance> removed = new ArrayList<>(state.weather());
        state.weather().clear();
        List<Integer> ids = new ArrayList<>();
        for (CardInstance weather : removed) {
            state.player(weather.playedBy()).discard().add(weather);
            ids.add(weather.id());
        }
        if (clearCard != null) {
            ids.add(clearCard.id());
        }
        emit(EventType.WEATHER_CLEARED, player, ids, null, source + ": the skies are clear.");
    }

    private void playHorn(int player, CardInstance card, RowType row) {
        RowState target = state.player(player).row(row);
        target.setSpecial(card);
        card.setRow(row);
        emit(EventType.SPECIAL_PLAYED, player, List.of(card.id()), row, state.player(player).name() + " played Commander's Horn on " + row.displayName() + ".");
    }

    private void playMardroeme(int player, CardInstance card, RowType row) {
        RowState target = state.player(player).row(row);
        target.setSpecial(card);
        card.setRow(row);
        emit(EventType.SPECIAL_PLAYED, player, List.of(card.id()), row, state.player(player).name() + " played Mardroeme on " + row.displayName() + ".");
        transformBerserkers(player, row);
    }

    private void playDecoy(int player, CardInstance decoy, RowType row, int targetId) {
        PlayerState me = state.player(player);
        RowState target = me.row(row);
        int index = -1;
        for (int i = 0; i < target.cards().size(); i++) {
            if (target.cards().get(i).id() == targetId) {
                index = i;
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException("Decoy target " + targetId + " is not in row " + row);
        }
        CardInstance taken = target.cards().get(index);
        target.cards().set(index, decoy);
        decoy.setRow(row);
        leaveBattlefield(taken);
        me.hand().add(taken);
        emit(EventType.CARD_RETURNED, player, List.of(decoy.id(), taken.id()), row, me.name() + " used a Decoy to take " + taken.def().name() + " back.");
    }

    private void playScorch(int player, CardInstance card) {
        state.player(player).discard().add(card);
        emit(EventType.SPECIAL_PLAYED, player, List.of(card.id()), null, state.player(player).name() + " played Scorch.");
        scorchBattlefield(player, "Scorch");
    }

    // ================================================================== leaders

    private void useLeader(int player) {
        PlayerState me = state.player(player);
        LeaderDef leader = me.leader();
        LeaderAbility ability = leader.ability();
        me.setLeaderUsed(true);
        emit(EventType.LEADER_USED, player, List.of(), null, me.name() + " used " + leader.name() + ".");
        switch (ability) {
            case FOG_FROM_DECK, RAIN_FROM_DECK, FROST_FROM_DECK -> playWeatherFromDeck(player, ability.weatherFromDeck());
            case WEATHER_FROM_DECK -> chooseWeatherFromDeck(player);
            case CLEAR_WEATHER -> clearWeather(player, leader.name(), null);
            case HORN_CLOSE, HORN_RANGED, HORN_SIEGE -> leaderHorn(player, ability.hornRow(), leader);
            case SCORCH_CLOSE, SCORCH_RANGED, SCORCH_SIEGE -> scorchRow(player, Rules.opponent(player), ability.scorchRow(), leader.name());
            case LOOK_AT_HAND -> lookAtHand(player);
            case DRAW_FROM_OPPONENT_DISCARD -> takeFromDiscard(player, Rules.opponent(player), ChoiceKind.DRAW_FROM_OPPONENT_DISCARD, "Choose a card from your opponent's discard pile");
            case RESTORE_FROM_DISCARD -> takeFromDiscard(player, player, ChoiceKind.RESTORE_FROM_DISCARD, "Choose a card from your discard pile to return to your hand");
            case DISCARD_2_DRAW_1 -> discardTwoDrawOne(player);
            case OPTIMIZE_AGILE -> optimizeAgile(player);
            case SHUFFLE_GRAVEYARDS -> shuffleGraveyards(player);
            default -> {
            }
        }
    }

    private void playWeatherFromDeck(int player, SpecialType type) {
        PlayerState me = state.player(player);
        CardInstance card = Rules.findSpecial(me.deck(), type);
        if (card == null) {
            emit(EventType.MESSAGE, player, List.of(), null, "No " + type.displayName() + " card left in the deck.");
            return;
        }
        me.deck().remove(card);
        card.setPlayedBy(player);
        playWeather(player, card);
    }

    private void chooseWeatherFromDeck(int player) {
        PlayerState me = state.player(player);
        List<CardInstance> options = new ArrayList<>();
        List<SpecialType> seen = new ArrayList<>();
        for (CardInstance card : me.deck()) {
            if (Rules.weatherWouldChange(state, card.def()) && !seen.contains(card.def().special())) {
                seen.add(card.def().special());
                options.add(card);
            }
        }
        if (options.isEmpty()) {
            emit(EventType.MESSAGE, player, List.of(), null, "No weather card left in the deck.");
            return;
        }
        CardInstance chosen = chooseOne(player, ChoiceKind.WEATHER_FROM_DECK, "Choose a weather card to play", options);
        me.deck().remove(chosen);
        chosen.setPlayedBy(player);
        playWeather(player, chosen);
    }

    private void leaderHorn(int player, RowType row, LeaderDef leader) {
        state.player(player).row(row).setLeaderHorn(true);
        emit(EventType.LEADER_HORN, player, List.of(), row, leader.name() + " doubles the " + row.displayName() + " row.");
    }

    private void lookAtHand(int player) {
        List<CardInstance> hand = new ArrayList<>(state.opponentOf(player).hand());
        Collections.shuffle(hand, state.random());
        List<CardInstance> shown = hand.subList(0, Math.min(Rules.LOOK_AT_HAND_COUNT, hand.size()));
        emit(EventType.CARDS_REVEALED, player, List.of(), null, state.player(player).name() + " looked at " + shown.size() + " random cards of the other hand.");
        deciders[player].reveal(state, player, "Cards in your opponent's hand", Snapshots.cards(shown));
    }

    /** Bringer of Death / The Relentless: like the original, only non-hero units can be taken. */
    private void takeFromDiscard(int player, int fromPlayer, ChoiceKind kind, String title) {
        List<CardInstance> options = Rules.revivableUnits(state.player(fromPlayer).discard());
        if (options.isEmpty()) {
            return;
        }
        PlayerState from = state.player(fromPlayer);
        CardInstance chosen = chooseOne(player, kind, title, options);
        from.discard().remove(chosen);
        state.player(player).hand().add(chosen);
        emit(EventType.CARD_TAKEN, player, List.of(chosen.id()), null, state.player(player).name() + " took " + chosen.def().name() + " from a discard pile into the hand.");
    }

    private void discardTwoDrawOne(int player) {
        PlayerState me = state.player(player);
        if (me.hand().size() < 2 || me.deck().isEmpty()) {
            return;
        }
        List<CardInstance> discarded = chooseMany(player, ChoiceKind.DISCARD_FROM_HAND, "Choose 2 cards to discard", new ArrayList<>(me.hand()), 2, 2);
        List<Integer> ids = new ArrayList<>();
        for (CardInstance card : discarded) {
            me.hand().remove(card);
            me.discard().add(card);
            ids.add(card.id());
        }
        emit(EventType.CARDS_DISCARDED, player, ids, null, me.name() + " discarded " + describe(discarded) + ".");
        List<CardInstance> deckOptions = new ArrayList<>(me.deck());
        Collections.sort(deckOptions, new DeckChoiceOrder());
        CardInstance chosen = chooseOne(player, ChoiceKind.DRAW_FROM_DECK, "Choose a card from your deck", deckOptions);
        me.deck().remove(chosen);
        me.hand().add(chosen);
        emit(EventType.CARD_TAKEN, player, List.of(chosen.id()), null, me.name() + " took a card from the deck.");
    }

    private void optimizeAgile(int player) {
        PlayerState me = state.player(player);
        List<CardInstance> moved = new ArrayList<>();
        for (CardInstance card : Rules.agileImprovements(state, player)) {
            RowType from = card.row();
            RowType to = Rules.otherAgileRow(card.def(), from);
            RowState fromRow = me.row(from);
            RowState toRow = me.row(to);
            if (Rules.strength(state, me, toRow, card) > Rules.strength(state, me, fromRow, card)) {
                fromRow.cards().remove(card);
                place(player, card, to);
                moved.add(card);
            }
        }
        List<Integer> ids = new ArrayList<>();
        for (CardInstance card : moved) {
            ids.add(card.id());
        }
        String text = moved.isEmpty() ? "No agile unit needed to move." : "Moved " + describe(moved) + " to stronger rows.";
        emit(EventType.AGILE_MOVED, player, ids, null, text);
    }

    private void shuffleGraveyards(int player) {
        for (int p = 0; p < 2; p++) {
            PlayerState side = state.player(p);
            for (CardInstance card : side.discard()) {
                if (!card.def().token()) {
                    side.deck().add(card);
                }
            }
            side.discard().clear();
            Collections.shuffle(side.deck(), state.random());
        }
        emit(EventType.DECKS_SHUFFLED, player, List.of(), null, "Both discard piles were shuffled back into the decks.");
    }

    // ================================================================== drawing

    /** Moves up to {@code count} cards from the top of the deck into the hand and reports it. */
    public List<CardInstance> draw(int player, int count) {
        List<CardInstance> drawn = takeFromDeck(player, count);
        if (!drawn.isEmpty()) {
            emit(EventType.CARDS_DRAWN, player, ids(drawn), null, state.player(player).name() + " drew " + drawn.size() + (drawn.size() == 1 ? " card." : " cards."));
        }
        return drawn;
    }

    private List<CardInstance> takeFromDeck(int player, int count) {
        PlayerState me = state.player(player);
        List<CardInstance> drawn = new ArrayList<>();
        for (int i = 0; i < count && !me.deck().isEmpty(); i++) {
            CardInstance card = me.deck().remove(0);
            me.hand().add(card);
            drawn.add(card);
        }
        return drawn;
    }

    // ================================================================== round flow

    /** Skellige perk at the start of round 3: two random non-hero units from the discard pile return to the battlefield. */
    public void skelligeReturn(int player) {
        PlayerState me = state.player(player);
        List<CardInstance> candidates = Rules.revivableUnits(me.discard());
        if (candidates.isEmpty()) {
            emit(EventType.FACTION_ABILITY, player, List.of(), null, "Skellige: the graveyard is empty, nobody returns.");
            return;
        }
        Collections.shuffle(candidates, state.random());
        List<CardInstance> returning = new ArrayList<>(candidates.subList(0, Math.min(Rules.SKELLIGE_RETURN_COUNT, candidates.size())));
        for (CardInstance card : returning) {
            me.discard().remove(card);
            place(player, card, autoRow(player, card));
        }
        emit(EventType.FACTION_ABILITY, player, ids(returning), null, "Skellige: " + describe(returning) + " returned from the graveyard.");
        for (CardInstance card : returning) {
            checkBerserker(player, card);
        }
    }

    /**
     * Ends the current round: scores it, removes gems, applies the Nilfgaard / Northern Realms / Monsters perks,
     * clears the battlefield and summons avengers. Sets the phase to GAME_OVER (with the winner) when a player has no gems left.
     */
    public void endRound() {
        PlayerState p0 = state.player(0);
        PlayerState p1 = state.player(1);
        int score0 = Rules.score(state, 0);
        int score1 = Rules.score(state, 1);
        int winner = GameState.DRAW;
        if (score0 > score1) {
            winner = 0;
        } else if (score1 > score0) {
            winner = 1;
        }
        boolean nilfgaardTie = false;
        if (winner == GameState.DRAW) {
            boolean ng0 = p0.faction() == Faction.NILFGAARD;
            boolean ng1 = p1.faction() == Faction.NILFGAARD;
            if (ng0 != ng1) {
                winner = ng0 ? 0 : 1;
                nilfgaardTie = true;
            }
        }
        if (winner == GameState.DRAW) {
            p0.loseGem();
            p1.loseGem();
        } else {
            state.player(Rules.opponent(winner)).loseGem();
        }
        state.results().add(new RoundResult(state.round(), score0, score1, winner));
        state.setPhase(GamePhase.ROUND_OVER);
        if (nilfgaardTie) {
            emit(EventType.FACTION_ABILITY, winner, List.of(), null, "Nilfgaardian Empire: " + state.player(winner).name() + " won the tied round.");
        }
        String scoreText = " (" + score0 + " : " + score1 + ")";
        String text = winner == GameState.DRAW ? "Round " + state.round() + " is a draw" + scoreText + "." : state.player(winner).name() + " won round " + state.round() + scoreText + ".";
        emit(EventType.ROUND_ENDED, winner, List.of(), null, text);
        boolean gameOver = p0.gems() == 0 || p1.gems() == 0;
        if (!gameOver && winner != GameState.DRAW && state.player(winner).faction() == Faction.NORTHERN_REALMS) {
            List<CardInstance> drawn = takeFromDeck(winner, 1);
            if (!drawn.isEmpty()) {
                emit(EventType.FACTION_ABILITY, winner, ids(drawn), null, "Northern Realms: " + state.player(winner).name() + " drew a card for winning the round.");
            }
        }
        clearBoard(gameOver);
        if (gameOver) {
            int gameWinner = GameState.DRAW;
            if (p0.gems() > 0) {
                gameWinner = 0;
            } else if (p1.gems() > 0) {
                gameWinner = 1;
            }
            state.setWinner(gameWinner);
            state.setPhase(GamePhase.GAME_OVER);
        }
    }

    private void clearBoard(boolean gameOver) {
        CardInstance[] kept = new CardInstance[2];
        if (!gameOver) {
            for (int p = 0; p < 2; p++) {
                PlayerState side = state.player(p);
                if (side.faction() == Faction.MONSTERS) {
                    List<CardInstance> units = Rules.nonHeroUnits(side.boardCards());
                    if (!units.isEmpty()) {
                        kept[p] = units.get(state.random().nextInt(units.size()));
                    }
                }
            }
        }
        List<PendingSummon> avengers = new ArrayList<>();
        for (int p = 0; p < 2; p++) {
            PlayerState side = state.player(p);
            for (RowState row : side.rows()) {
                List<CardInstance> leaving = new ArrayList<>(row.cards());
                row.cards().clear();
                for (CardInstance card : leaving) {
                    if (card == kept[p]) {
                        row.cards().add(card);
                        continue;
                    }
                    leaveBattlefield(card);
                    side.discard().add(card);
                    if (card.def().ability() == Ability.AVENGER) {
                        avengers.add(new PendingSummon(p, card));
                    }
                }
                if (row.special() != null) {
                    CardInstance special = row.special();
                    special.setRow(null);
                    side.discard().add(special);
                    row.setSpecial(null);
                }
                row.setLeaderHorn(false);
            }
        }
        for (CardInstance weather : state.weather()) {
            state.player(weather.playedBy()).discard().add(weather);
        }
        state.weather().clear();
        emit(EventType.BOARD_CLEARED, -1, List.of(), null, "The battlefield was cleared.");
        for (int p = 0; p < 2; p++) {
            if (kept[p] != null) {
                emit(EventType.FACTION_ABILITY, p, List.of(kept[p].id()), kept[p].row(), "Monsters: " + kept[p].def().name() + " stays on the battlefield.");
            }
        }
        if (!gameOver) {
            for (PendingSummon pending : avengers) {
                summonAvenger(pending.side(), pending.card());
            }
        }
    }

    // ================================================================== choices

    private CardInstance chooseOne(int player, ChoiceKind kind, String title, List<CardInstance> options) {
        return chooseMany(player, kind, title, options, 1, 1).get(0);
    }

    /** Asks the player's decider and repairs invalid answers (unknown ids, duplicates, too few / too many). */
    private List<CardInstance> chooseMany(int player, ChoiceKind kind, String title, List<CardInstance> options, int min, int max) {
        List<CardSnapshot> snapshots = Snapshots.cardsWithStrength(state, options);
        ChoiceRequest request = new ChoiceRequest(kind, title, snapshots, min, max);
        List<Integer> answer = deciders[player].chooseCards(state, player, request);
        List<CardInstance> chosen = new ArrayList<>();
        if (answer != null) {
            for (Integer id : answer) {
                CardInstance card = findById(options, id);
                if (card != null && !chosen.contains(card) && chosen.size() < max) {
                    chosen.add(card);
                }
            }
        }
        for (int i = 0; i < options.size() && chosen.size() < min; i++) {
            if (!chosen.contains(options.get(i))) {
                chosen.add(options.get(i));
            }
        }
        return chosen;
    }

    // ================================================================== helpers

    private void emit(EventType type, int player, List<Integer> cardIds, RowType row, String text) {
        sink.emit(new GameEvent(type, player, cardIds, row, text));
    }

    private static CardInstance findById(List<CardInstance> cards, Integer id) {
        if (id == null) {
            return null;
        }
        for (CardInstance card : cards) {
            if (card.id() == id) {
                return card;
            }
        }
        return null;
    }

    private static List<Integer> ids(List<CardInstance> cards) {
        List<Integer> ids = new ArrayList<>();
        for (CardInstance card : cards) {
            ids.add(card.id());
        }
        return ids;
    }

    /** "A", "A and B", "A, B and C" */
    private static String describe(List<CardInstance> cards) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) {
                text.append(i == cards.size() - 1 ? " and " : ", ");
            }
            text.append(cards.get(i).def().name());
        }
        return text.toString();
    }

    /** An avenger that left the battlefield and still has to summon its replacement. */
    private record PendingSummon(int side, CardInstance card) {
    }

    /**
     * Order in which deck cards are offered for a choice: special and weather cards first, then units from strongest to
     * weakest. Sorting (instead of deck order) keeps the order of the deck secret.
     */
    private static final class DeckChoiceOrder implements Comparator<CardInstance> {
        @Override
        public int compare(CardInstance a, CardInstance b) {
            int groupA = a.def().isUnit() ? 1 : 0;
            int groupB = b.def().isUnit() ? 1 : 0;
            if (groupA != groupB) {
                return Integer.compare(groupA, groupB);
            }
            int byStrength = Integer.compare(b.def().strength(), a.def().strength());
            if (byStrength != 0) {
                return byStrength;
            }
            int byName = a.def().name().compareTo(b.def().name());
            return byName != 0 ? byName : Integer.compare(a.id(), b.id());
        }
    }
}
