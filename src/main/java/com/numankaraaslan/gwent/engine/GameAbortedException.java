package com.numankaraaslan.gwent.engine;

/** Thrown on the engine thread when the game is abandoned (e.g. the player returns to the main menu). */
public final class GameAbortedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public GameAbortedException() {
        super("Game aborted");
    }

    public GameAbortedException(Throwable cause) {
        super("Game aborted", cause);
    }
}
