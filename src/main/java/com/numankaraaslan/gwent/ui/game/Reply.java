package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.GameAbortedException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hand-over point for one answer from the JavaFX thread to the blocked engine thread.
 * The FX side calls {@link #answer} once; the engine thread waits in {@link #await}.
 * {@link #abort} releases a waiting engine thread with a {@link GameAbortedException} (used when the screen is left).
 *
 * @param <T> type of the answer
 */
final class Reply<T> {

    private final BlockingQueue<Slot<T>> queue = new ArrayBlockingQueue<>(1);
    private final AtomicBoolean done = new AtomicBoolean(false);

    /**
     * FX side: delivers the answer. Only the first answer (or abort) counts.
     *
     * @return false if the reply was already answered or aborted
     */
    boolean answer(T value) {
        if (!done.compareAndSet(false, true)) {
            return false;
        }
        queue.offer(new Slot<>(value, false));
        return true;
    }

    /** Releases the waiting engine thread without an answer; it will throw {@link GameAbortedException}. */
    void abort() {
        if (done.compareAndSet(false, true)) {
            queue.offer(new Slot<T>(null, true));
        }
    }

    /** True once answered or aborted. */
    boolean isDone() {
        return done.get();
    }

    /** Engine side: blocks until the answer arrives. Throws {@link GameAbortedException} when aborted or interrupted. */
    T await() {
        Slot<T> slot;
        try {
            slot = queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GameAbortedException(e);
        }
        if (slot.aborted()) {
            throw new GameAbortedException();
        }
        return slot.value();
    }

    /** Queue element: the answer, or the abort marker. */
    private record Slot<T>(T value, boolean aborted) {
    }
}
