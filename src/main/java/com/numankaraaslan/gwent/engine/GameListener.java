package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.engine.snapshot.GameSnapshot;

/**
 * Receives everything that happens in a game. Called on the engine thread.
 * The UI implementation blocks in {@link #onEvent} until the event has been animated, which keeps the
 * engine and the screen in step.
 */
public interface GameListener {

    /** An event happened; {@code snapshot} is the state right after it. */
    void onEvent(GameEvent event, GameSnapshot snapshot);

    /** The engine crashed with an unexpected exception. */
    void onError(Throwable error);
}
