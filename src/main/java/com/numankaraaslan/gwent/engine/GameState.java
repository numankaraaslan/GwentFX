package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardPool;
import com.numankaraaslan.gwent.model.GameSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Complete mutable state of one game. Owned by the engine thread; the UI only ever sees snapshots.
 * Player 0 is always the human (bottom of the screen), player 1 the computer (top).
 */
public final class GameState {

    public static final int HUMAN = 0;
    public static final int AI = 1;
    /** Winner value for a drawn round / game. */
    public static final int DRAW = -1;
    /** Winner value while the game is still running. */
    public static final int UNDECIDED = -2;

    private final CardDatabase db;
    private final GameSettings settings;
    private final CardPool pool;
    private final PlayerState[] players;
    private final List<CardInstance> weather = new ArrayList<>();
    private final List<RoundResult> results = new ArrayList<>();
    private final Random random;
    private int round;
    private int currentPlayer;
    private int firstPlayer;
    private int nextCardId = 1;
    private GamePhase phase = GamePhase.SETUP;
    private int winner = UNDECIDED;

    public GameState(CardDatabase db, GameSettings settings, CardPool pool, PlayerState player0, PlayerState player1, Random random) {
        this.db = db;
        this.settings = settings;
        this.pool = pool;
        this.players = new PlayerState[] {player0, player1};
        this.random = random;
    }

    public CardDatabase db() {
        return db;
    }

    public GameSettings settings() {
        return settings;
    }

    /** The cards both decks were built from. */
    public CardPool pool() {
        return pool;
    }

    public PlayerState player(int index) {
        return players[index];
    }

    public PlayerState opponentOf(int index) {
        return players[1 - index];
    }

    /** Weather cards currently in the weather zone (they affect both sides). */
    public List<CardInstance> weather() {
        return weather;
    }

    public List<RoundResult> results() {
        return results;
    }

    public Random random() {
        return random;
    }

    /** 1-based number of the current round (0 before the first round starts). */
    public int round() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    /** Player whose turn it is. */
    public int currentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    /** Player who starts the current (or next) round. */
    public int firstPlayer() {
        return firstPlayer;
    }

    public void setFirstPlayer(int firstPlayer) {
        this.firstPlayer = firstPlayer;
    }

    public int newCardId() {
        return nextCardId++;
    }

    public GamePhase phase() {
        return phase;
    }

    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }

    /** 0, 1, DRAW, or UNDECIDED while the game runs. */
    public int winner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }

    /** Winner of the last finished round, DRAW, or UNDECIDED if no round finished yet. */
    public int lastRoundWinner() {
        if (results.isEmpty()) {
            return UNDECIDED;
        }
        return results.get(results.size() - 1).winner();
    }

    /** Deep copy for AI look-ahead. The copy gets its own Random so simulations never disturb the real game. */
    public GameState copy() {
        GameState copy = new GameState(db, settings, pool, players[0].copy(), players[1].copy(), new Random());
        for (CardInstance card : weather) {
            copy.weather.add(card.copy());
        }
        copy.results.addAll(results);
        copy.round = round;
        copy.currentPlayer = currentPlayer;
        copy.firstPlayer = firstPlayer;
        copy.nextCardId = nextCardId;
        copy.phase = phase;
        copy.winner = winner;
        return copy;
    }
}
