package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.engine.snapshot.Snapshots;
import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardPool;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.GameSettings;
import com.numankaraaslan.gwent.model.LeaderAbility;
import com.numankaraaslan.gwent.model.LeaderDef;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Runs one game of Gwent on its own thread: deal, redraw, coin toss, three rounds of turns, game over.
 * Talks to the two {@link PlayerController}s for decisions and reports every step to the {@link GameListener}.
 *
 * <p>Typical use from the UI: {@code new GameEngine(settings, pool, humanController, aiController, listener).start()},
 * and {@link #stop()} when leaving the game screen. Both decks are built from the cards in {@code pool}.</p>
 */
public final class GameEngine implements Runnable {

    private final GameState state;
    private final PlayerController[] controllers;
    private final GameListener listener;
    private final Resolver resolver;
    private final EventSink sink;
    private volatile boolean stopped;
    private Thread thread;
    /** Player who made the last move of the current round (decides who starts after a drawn round). */
    private int lastMover;

    public GameEngine(GameSettings settings, CardPool pool, PlayerController human, PlayerController ai, GameListener listener) {
        CardDatabase db = CardDatabase.get();
        Random random = new Random();
        LeaderDef humanLeader = settings.playerLeader() != null ? settings.playerLeader() : randomLeader(db, settings.playerFaction(), random);
        Faction aiFaction = settings.aiFaction() != null ? settings.aiFaction() : randomFaction(random);
        LeaderDef aiLeader = settings.aiLeader() != null && settings.aiLeader().faction() == aiFaction ? settings.aiLeader() : randomLeader(db, aiFaction, random);
        PlayerState player0 = new PlayerState(GameState.HUMAN, "You", settings.playerFaction(), humanLeader);
        PlayerState player1 = new PlayerState(GameState.AI, "Opponent", aiFaction, aiLeader);
        this.state = new GameState(db, settings, pool, player0, player1, random);
        PlayerController safeAi = new SafeController(ai);
        this.controllers = new PlayerController[] {human, safeAi};
        this.listener = listener;
        this.sink = new ListenerSink();
        this.resolver = new Resolver(state, human, safeAi, sink);
        DeckBuilder.fill(state, GameState.HUMAN);
        DeckBuilder.fill(state, GameState.AI);
    }

    /** Starts the game loop on a daemon thread. */
    public synchronized void start() {
        if (thread != null) {
            throw new IllegalStateException("Game already started");
        }
        thread = new Thread(this, "gwent-engine");
        thread.setDaemon(true);
        thread.start();
    }

    /** Abandons the game: the engine thread is interrupted and ends quietly. Safe to call from any thread, more than once. */
    public synchronized void stop() {
        stopped = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void run() {
        try {
            playGame();
        } catch (GameAbortedException e) {
            // the player left the game
        } catch (RuntimeException e) {
            if (!stopped) {
                listener.onError(e);
            }
        }
    }

    // ------------------------------------------------------------------ game flow

    private void playGame() {
        PlayerState human = state.player(GameState.HUMAN);
        PlayerState ai = state.player(GameState.AI);
        emit(EventType.GAME_STARTED, -1, List.of(), "New game: " + human.faction().displayName() + " (" + human.leader().name() + ") vs " + ai.faction().displayName() + " (" + ai.leader().name() + ").");
        applyLeaderCancel();
        state.setPhase(GamePhase.REDRAW);
        deal(GameState.HUMAN);
        deal(GameState.AI);
        redraw(GameState.HUMAN);
        redraw(GameState.AI);
        chooseFirstPlayer();
        while (state.phase() != GamePhase.GAME_OVER) {
            playRound();
        }
        int winner = state.winner();
        String text = winner == GameState.HUMAN ? "Victory! You won the game." : winner == GameState.AI ? "Defeat. The opponent won the game." : "The game ended in a draw.";
        emit(EventType.GAME_ENDED, winner, List.of(), text);
    }

    /** Emhyr: The White Flame cancels the other leader before anything else happens. */
    private void applyLeaderCancel() {
        boolean cancel0 = state.player(0).leader().ability() == LeaderAbility.CANCEL_LEADER;
        boolean cancel1 = state.player(1).leader().ability() == LeaderAbility.CANCEL_LEADER;
        if (cancel0) {
            state.player(1).setLeaderDisabled(true);
            emit(EventType.LEADER_CANCELLED, 1, List.of(), state.player(0).leader().name() + " cancelled the opponent's leader ability.");
        }
        if (cancel1) {
            state.player(0).setLeaderDisabled(true);
            emit(EventType.LEADER_CANCELLED, 0, List.of(), state.player(1).leader().name() + " cancelled your leader ability.");
        }
    }

    private void deal(int player) {
        PlayerState side = state.player(player);
        int count = state.settings().handSize();
        boolean extra = side.leaderActive(LeaderAbility.EXTRA_CARD);
        if (extra) {
            count++;
        }
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < count && !side.deck().isEmpty(); i++) {
            CardInstance card = side.deck().remove(0);
            side.hand().add(card);
            ids.add(card.id());
        }
        String text = side.name() + " drew " + ids.size() + " cards" + (extra ? " (" + side.leader().name() + ": one extra card)." : ".");
        emit(EventType.HAND_DEALT, player, ids, text);
    }

    private void redraw(int player) {
        PlayerState side = state.player(player);
        int left = state.settings().redraws();
        while (left > 0 && !side.deck().isEmpty() && !side.hand().isEmpty()) {
            int cardId = controllers[player].chooseRedraw(state, player, left);
            checkStopped();
            if (cardId < 0) {
                break;
            }
            CardInstance old = side.findInHand(cardId);
            if (old == null) {
                break;
            }
            int index = side.hand().indexOf(old);
            CardInstance fresh = side.deck().remove(0);
            side.hand().set(index, fresh);
            side.deck().add(old);
            left--;
            emit(EventType.CARD_REDRAWN, player, List.of(old.id(), fresh.id()), side.name() + " redrew a card.");
        }
        emit(EventType.REDRAW_FINISHED, player, List.of(), "");
    }

    private void chooseFirstPlayer() {
        boolean scoiatael0 = state.player(0).faction() == Faction.SCOIATAEL;
        boolean scoiatael1 = state.player(1).faction() == Faction.SCOIATAEL;
        int first;
        String text;
        if (scoiatael0 != scoiatael1) {
            int chooser = scoiatael0 ? 0 : 1;
            boolean goFirst = controllers[chooser].chooseGoFirst(state, chooser);
            checkStopped();
            first = goFirst ? chooser : Rules.opponent(chooser);
            text = "Scoia'tael perk: " + state.player(chooser).name() + " decided that " + (first == GameState.HUMAN ? "you go first." : "the opponent goes first.");
        } else {
            first = state.random().nextInt(2);
            text = "Coin toss: " + (first == GameState.HUMAN ? "you go first." : "the opponent goes first.");
        }
        state.setFirstPlayer(first);
        state.setCurrentPlayer(first);
        emit(EventType.FIRST_PLAYER_CHOSEN, first, List.of(), text);
    }

    private void playRound() {
        state.setRound(state.round() + 1);
        state.setPhase(GamePhase.PLAYING);
        state.player(0).setPassed(false);
        state.player(1).setPassed(false);
        int current = state.firstPlayer();
        state.setCurrentPlayer(current);
        emit(EventType.ROUND_STARTED, current, List.of(), "Round " + state.round() + " begins. " + (current == GameState.HUMAN ? "You start." : "The opponent starts."));
        if (state.round() == 3) {
            for (int p = 0; p < 2; p++) {
                if (state.player(p).faction() == Faction.SKELLIGE) {
                    resolver.skelligeReturn(p);
                }
            }
        }
        for (int p = 0; p < 2; p++) {
            if (state.player(p).hand().isEmpty()) {
                resolver.pass(p, true);
                lastMover = p;
            }
        }
        while (!(state.player(0).passed() && state.player(1).passed())) {
            if (state.player(current).passed()) {
                current = Rules.opponent(current);
                continue;
            }
            state.setCurrentPlayer(current);
            List<Move> legal = Rules.legalMoves(state, current);
            if (legal.size() == 1) {
                resolver.pass(current, true);
            } else {
                emit(EventType.TURN_STARTED, current, List.of(), "");
                Move move = controllers[current].chooseMove(state, current, legal);
                checkStopped();
                if (move == null || !legal.contains(move)) {
                    move = Move.pass();
                }
                resolver.applyMove(current, move);
            }
            lastMover = current;
            int other = Rules.opponent(current);
            if (!state.player(other).passed()) {
                current = other;
            }
        }
        resolver.endRound();
        if (state.phase() != GamePhase.GAME_OVER) {
            state.setFirstPlayer(nextRoundFirstPlayer());
        }
    }

    /**
     * Who starts the next round, as in the original game: the winner of the previous round (a Nilfgaard tie win counts);
     * after a drawn round the player who made the last move. If that player has no cards left, the other one starts.
     */
    private int nextRoundFirstPlayer() {
        int lastWinner = state.lastRoundWinner();
        int first = lastWinner == 0 || lastWinner == 1 ? lastWinner : lastMover;
        int other = Rules.opponent(first);
        if (state.player(first).hand().isEmpty() && !state.player(other).hand().isEmpty()) {
            return other;
        }
        return first;
    }

    // ------------------------------------------------------------------ events

    private void emit(EventType type, int player, List<Integer> cardIds, String text) {
        sink.emit(new GameEvent(type, player, cardIds, null, text));
    }

    private void checkStopped() {
        if (stopped || Thread.currentThread().isInterrupted()) {
            throw new GameAbortedException();
        }
    }

    /** Forwards events to the listener together with a fresh snapshot (as seen by the human). */
    private final class ListenerSink implements EventSink {
        @Override
        public void emit(GameEvent event) {
            checkStopped();
            listener.onEvent(event, Snapshots.of(state, GameState.HUMAN));
            checkStopped();
        }
    }

    // ------------------------------------------------------------------ setup helpers

    private static LeaderDef randomLeader(CardDatabase db, Faction faction, Random random) {
        List<LeaderDef> leaders = db.leaders(faction);
        if (leaders.isEmpty()) {
            throw new IllegalStateException("No leaders for faction " + faction);
        }
        return leaders.get(random.nextInt(leaders.size()));
    }

    private static Faction randomFaction(Random random) {
        List<Faction> factions = Faction.playable();
        return factions.get(random.nextInt(factions.size()));
    }
}
