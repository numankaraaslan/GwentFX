package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.GameEvent;
import com.numankaraaslan.gwent.engine.snapshot.GameSnapshot;

/** FX side of the {@link FxGameListener}. Both methods are called on the JavaFX thread. */
interface EventPlayer {

    /**
     * Animates one event and renders its snapshot. {@code done} must be run exactly when the animation is over
     * (also when it is skipped or fails), because the engine thread waits for it.
     */
    void playEvent(GameEvent event, GameSnapshot snapshot, Runnable done);

    /** The engine crashed. */
    void showError(Throwable error);

    /**
     * True while the screen deliberately holds events back (pause menu open). Read on the engine thread; the listener's
     * safety timeout does not run meanwhile.
     */
    boolean isHolding();
}
