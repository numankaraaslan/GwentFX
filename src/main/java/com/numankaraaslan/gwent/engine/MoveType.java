package com.numankaraaslan.gwent.engine;

/** What a player does on their turn. */
public enum MoveType {
    /** Play a card from the hand. */
    PLAY,
    /** Activate the leader ability (takes the turn). */
    LEADER,
    /** Pass for the rest of the round. */
    PASS
}
