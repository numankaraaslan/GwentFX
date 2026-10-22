package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.RowType;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a controller so that a bug in it (e.g. in the AI) can't crash the game: unexpected exceptions are logged
 * and replaced by a safe default answer. {@link GameAbortedException} is passed through untouched.
 */
public final class SafeController implements PlayerController {

    private final PlayerController delegate;

    public SafeController(PlayerController delegate) {
        this.delegate = delegate;
    }

    @Override
    public int chooseRedraw(GameState state, int player, int redrawsLeft) {
        try {
            return delegate.chooseRedraw(state, player, redrawsLeft);
        } catch (GameAbortedException e) {
            throw e;
        } catch (RuntimeException e) {
            log("chooseRedraw", e);
            return -1;
        }
    }

    @Override
    public boolean chooseGoFirst(GameState state, int player) {
        try {
            return delegate.chooseGoFirst(state, player);
        } catch (GameAbortedException e) {
            throw e;
        } catch (RuntimeException e) {
            log("chooseGoFirst", e);
            return false;
        }
    }

    @Override
    public Move chooseMove(GameState state, int player, List<Move> legalMoves) {
        try {
            return delegate.chooseMove(state, player, legalMoves);
        } catch (GameAbortedException e) {
            throw e;
        } catch (RuntimeException e) {
            log("chooseMove", e);
            return Move.pass();
        }
    }

    @Override
    public List<Integer> chooseCards(GameState state, int player, ChoiceRequest request) {
        try {
            return delegate.chooseCards(state, player, request);
        } catch (GameAbortedException e) {
            throw e;
        } catch (RuntimeException e) {
            log("chooseCards", e);
            List<Integer> ids = new ArrayList<>();
            for (int i = 0; i < request.min() && i < request.options().size(); i++) {
                ids.add(request.options().get(i).id());
            }
            return ids;
        }
    }

    @Override
    public RowType chooseRow(GameState state, int player, CardSnapshot card, List<RowType> options) {
        try {
            return delegate.chooseRow(state, player, card, options);
        } catch (GameAbortedException e) {
            throw e;
        } catch (RuntimeException e) {
            log("chooseRow", e);
            return options.get(0);
        }
    }

    @Override
    public void reveal(GameState state, int player, String title, List<CardSnapshot> cards) {
        try {
            delegate.reveal(state, player, title, cards);
        } catch (GameAbortedException e) {
            throw e;
        } catch (RuntimeException e) {
            log("reveal", e);
        }
    }

    private static void log(String method, RuntimeException e) {
        System.err.println("Controller error in " + method + " - using a safe default instead: " + e);
        e.printStackTrace();
    }
}
