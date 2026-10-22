package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.GameAbortedException;
import com.numankaraaslan.gwent.engine.GameEvent;
import com.numankaraaslan.gwent.engine.GameListener;
import com.numankaraaslan.gwent.engine.snapshot.GameSnapshot;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;

/**
 * Bridges engine events to the screen. {@link #onEvent} runs on the engine thread: it hands the event to the
 * {@link EventPlayer} on the JavaFX thread and blocks until the animation released the latch, which keeps the engine
 * and the screen in step. A generous safety timeout makes sure a broken animation can never hang the game forever.
 */
final class FxGameListener implements GameListener {

    /** Upper bound for one event animation; normal events take well under two seconds. */
    private static final long SAFETY_TIMEOUT_MILLIS = 20000;
    private static final long POLL_MILLIS = 250;

    private final EventPlayer player;
    private volatile boolean aborted;
    private volatile CountDownLatch pending;

    FxGameListener(EventPlayer player) {
        this.player = player;
    }

    @Override
    public void onEvent(GameEvent event, GameSnapshot snapshot) {
        if (aborted) {
            throw new GameAbortedException();
        }
        CountDownLatch latch = new CountDownLatch(1);
        pending = latch;
        try {
            if (!aborted) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        player.playEvent(event, snapshot, new LatchRelease(latch));
                    }
                });
                awaitAnimation(latch);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GameAbortedException(e);
        } finally {
            pending = null;
        }
        if (aborted) {
            throw new GameAbortedException();
        }
    }

    @Override
    public void onError(Throwable error) {
        if (aborted) {
            return;
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                player.showError(error);
            }
        });
    }

    /**
     * Waits until the event animation released the latch. Gives up after the safety timeout, which does not count while the
     * screen holds events back on purpose (pause menu).
     */
    private void awaitAnimation(CountDownLatch latch) throws InterruptedException {
        long waited = 0;
        while (!latch.await(POLL_MILLIS, TimeUnit.MILLISECONDS)) {
            if (aborted) {
                return;
            }
            if (!player.isHolding()) {
                waited += POLL_MILLIS;
            }
            if (waited >= SAFETY_TIMEOUT_MILLIS) {
                return;
            }
        }
    }

    /** Releases a waiting engine thread; later events fail fast. Safe to call from any thread. */
    void abort() {
        aborted = true;
        CountDownLatch latch = pending;
        if (latch != null) {
            latch.countDown();
        }
    }

    /** Counts the latch down; harmless when run more than once. */
    private static final class LatchRelease implements Runnable {

        private final CountDownLatch latch;

        LatchRelease(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            latch.countDown();
        }
    }
}
