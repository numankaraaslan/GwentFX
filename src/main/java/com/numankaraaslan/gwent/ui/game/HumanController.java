package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.ChoiceRequest;
import com.numankaraaslan.gwent.engine.GameAbortedException;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.Move;
import com.numankaraaslan.gwent.engine.PlayerController;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.Snapshots;
import com.numankaraaslan.gwent.model.RowType;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;

/**
 * The human player. Every method runs on the engine thread: it copies what the screen needs into immutable values
 * (snapshots, move records), shows a prompt on the JavaFX thread through the {@link PromptHost} and blocks until the
 * answer arrives through a {@link Reply}. {@link #abort()} (screen left) releases a waiting engine thread with a
 * {@link GameAbortedException}; interrupting the engine thread does the same.
 */
final class HumanController implements PlayerController {

    private final PromptHost host;
    private volatile boolean aborted;
    private volatile Reply<?> pending;

    HumanController(PromptHost host) {
        this.host = host;
    }

    @Override
    public int chooseRedraw(GameState state, int player, int redrawsLeft) {
        List<CardSnapshot> hand = Snapshots.of(state, player).player(player).hand();
        Reply<Integer> reply = new Reply<>();
        Integer answer = ask(reply, new Prompt<Integer>(reply, -1) {
            @Override
            void show() {
                host.promptRedraw(hand, redrawsLeft, reply);
            }
        });
        return answer == null ? -1 : answer;
    }

    @Override
    public boolean chooseGoFirst(GameState state, int player) {
        Reply<Boolean> reply = new Reply<>();
        Boolean answer = ask(reply, new Prompt<Boolean>(reply, Boolean.TRUE) {
            @Override
            void show() {
                host.promptGoFirst(reply);
            }
        });
        return answer == null || answer;
    }

    @Override
    public Move chooseMove(GameState state, int player, List<Move> legalMoves) {
        List<Move> moves = List.copyOf(legalMoves);
        Reply<Move> reply = new Reply<>();
        Move answer = ask(reply, new Prompt<Move>(reply, Move.pass()) {
            @Override
            void show() {
                host.promptMove(moves, reply);
            }
        });
        return answer == null ? Move.pass() : answer;
    }

    @Override
    public List<Integer> chooseCards(GameState state, int player, ChoiceRequest request) {
        Reply<List<Integer>> reply = new Reply<>();
        List<Integer> answer = ask(reply, new Prompt<List<Integer>>(reply, new ArrayList<>()) {
            @Override
            void show() {
                host.promptCards(request, reply);
            }
        });
        return answer == null ? new ArrayList<>() : answer;
    }

    @Override
    public RowType chooseRow(GameState state, int player, CardSnapshot card, List<RowType> options) {
        List<RowType> rows = List.copyOf(options);
        Reply<RowType> reply = new Reply<>();
        RowType answer = ask(reply, new Prompt<RowType>(reply, rows.get(0)) {
            @Override
            void show() {
                host.promptRow(card, rows, reply);
            }
        });
        return answer == null ? rows.get(0) : answer;
    }

    @Override
    public void reveal(GameState state, int player, String title, List<CardSnapshot> cards) {
        List<CardSnapshot> shown = List.copyOf(cards);
        Reply<Boolean> reply = new Reply<>();
        ask(reply, new Prompt<Boolean>(reply, Boolean.TRUE) {
            @Override
            void show() {
                host.promptReveal(title, shown, reply);
            }
        });
    }

    /** Releases a waiting engine thread and makes every later question fail fast. Safe to call from any thread. */
    void abort() {
        aborted = true;
        Reply<?> reply = pending;
        if (reply != null) {
            reply.abort();
        }
    }

    /**
     * A prompt to show on the FX thread. If showing it fails, the error is printed and a safe default answer is given,
     * so a UI bug can never leave the engine thread waiting forever.
     */
    private abstract static class Prompt<T> implements Runnable {

        private final Reply<T> reply;
        private final T fallback;

        Prompt(Reply<T> reply, T fallback) {
            this.reply = reply;
            this.fallback = fallback;
        }

        @Override
        public final void run() {
            try {
                show();
            } catch (RuntimeException e) {
                e.printStackTrace();
                reply.answer(fallback);
            }
        }

        /** Shows the prompt (FX thread). */
        abstract void show();
    }

    /** Posts the prompt to the FX thread and blocks until it is answered (engine thread). */
    private <T> T ask(Reply<T> reply, Runnable prompt) {
        if (aborted) {
            throw new GameAbortedException();
        }
        pending = reply;
        try {
            if (aborted) {
                reply.abort();
            } else {
                Platform.runLater(prompt);
            }
            return reply.await();
        } finally {
            pending = null;
        }
    }
}
