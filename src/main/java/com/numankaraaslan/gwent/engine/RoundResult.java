package com.numankaraaslan.gwent.engine;

/**
 * Outcome of a finished round.
 *
 * @param round  1-based round number
 * @param score0 final score of player 0 (human)
 * @param score1 final score of player 1 (computer)
 * @param winner 0, 1, or {@link GameState#DRAW}
 */
public record RoundResult(int round, int score0, int score1, int winner) {

    public int score(int player) {
        return player == 0 ? score0 : score1;
    }
}
