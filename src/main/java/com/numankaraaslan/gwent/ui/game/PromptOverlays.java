package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.ChoiceRequest;
import com.numankaraaslan.gwent.engine.Move;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import com.numankaraaslan.gwent.ui.card.Icons;
import java.util.ArrayList;
import java.util.List;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

/**
 * Shows the human player's prompts as modal overlays over the board (the board stays visible behind a dark veil,
 * the right column with the card info panel stays usable) and answers the engine's {@link Reply}s.
 * The board-based turn decision is delegated to {@link TurnInput}.
 */
final class PromptOverlays implements PromptHost {

    private final OverlayLayer overlays;
    private final TurnInput input;
    private RedrawOverlay redraw;
    private Overlay redrawOverlay;
    private Reply<Integer> redrawReply;
    private boolean disposed;

    PromptOverlays(OverlayLayer overlays, TurnInput input) {
        this.overlays = overlays;
        this.input = input;
    }

    @Override
    public void promptRedraw(List<CardSnapshot> hand, int redrawsLeft, Reply<Integer> reply) {
        if (disposed) {
            return;
        }
        redrawReply = reply;
        if (redraw == null) {
            redraw = new RedrawOverlay(input, new RedrawOverlay.Listener() {
                @Override
                public void swap(int cardId) {
                    answerRedraw(cardId);
                }

                @Override
                public void finish() {
                    answerRedraw(-1);
                }
            });
            redraw.setHand(hand, -1);
            redrawOverlay = overlays.showOverBoard(redraw);
        } else {
            redraw.setHand(hand, redraw.highlightId());
        }
        redraw.setRedrawsLeft(redrawsLeft);
        redraw.setEnabled(true);
    }

    private void answerRedraw(int cardId) {
        Reply<Integer> reply = redrawReply;
        redrawReply = null;
        if (reply != null) {
            reply.answer(cardId);
        }
    }

    /** The open redraw overlay, or null. */
    RedrawOverlay redrawOverlay() {
        return redraw;
    }

    /** Closes the redraw overlay (the redraw phase is over). */
    void closeRedraw() {
        if (redrawOverlay != null) {
            redrawOverlay.close();
        }
        redraw = null;
        redrawOverlay = null;
        redrawReply = null;
    }

    @Override
    public void promptGoFirst(Reply<Boolean> reply) {
        if (disposed) {
            return;
        }
        TurnInput.OverlayRef ref = new TurnInput.OverlayRef();
        List<Button> buttons = new ArrayList<>();
        buttons.add(Layouts.button("I go first", true, new Runnable() {
            @Override
            public void run() {
                ref.close();
                reply.answer(Boolean.TRUE);
            }
        }));
        buttons.add(Layouts.button("Opponent goes first", false, new Runnable() {
            @Override
            public void run() {
                ref.close();
                reply.answer(Boolean.FALSE);
            }
        }));
        ref.set(overlays.showOverBoard(Dialogs.panel("Who goes first?", "Scoia'tael perk: you decide who starts the first round.", Icons.faction(Faction.SCOIATAEL, 72), buttons)));
    }

    @Override
    public void promptMove(List<Move> legalMoves, Reply<Move> reply) {
        if (disposed) {
            return;
        }
        input.begin(legalMoves, reply);
    }

    @Override
    public void promptCards(ChoiceRequest request, Reply<List<Integer>> reply) {
        if (disposed) {
            return;
        }
        TurnInput.OverlayRef ref = new TurnInput.OverlayRef();
        String subtitle = request.min() == 1 && request.max() == 1 ? "Click a card to choose it." : "";
        CardGridOverlay grid = new CardGridOverlay(request.title(), subtitle, request.options(), request.min(), request.max(), "Confirm", input, new CardGridOverlay.Callback() {
            @Override
            public void done(List<Integer> cardIds) {
                ref.close();
                reply.answer(cardIds);
            }
        });
        ref.set(overlays.showOverBoard(grid));
    }

    @Override
    public void promptRow(CardSnapshot card, List<RowType> options, Reply<RowType> reply) {
        if (disposed) {
            return;
        }
        TurnInput.OverlayRef ref = new TurnInput.OverlayRef();
        List<Button> buttons = new ArrayList<>();
        for (RowType row : options) {
            buttons.add(Layouts.button(row.displayName(), Icons.row(row, 26), buttons.isEmpty(), new Runnable() {
                @Override
                public void run() {
                    ref.close();
                    reply.answer(row);
                }
            }));
        }
        CardView view = new CardView(card, CardSize.MEDIUM);
        view.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                input.showCard(card);
            }
        });
        ref.set(overlays.showOverBoard(Dialogs.panel("Choose a row", "Where should " + card.def().name() + " be placed?", view, buttons)));
    }

    @Override
    public void promptReveal(String title, List<CardSnapshot> cards, Reply<Boolean> reply) {
        if (disposed) {
            return;
        }
        TurnInput.OverlayRef ref = new TurnInput.OverlayRef();
        String subtitle = cards.isEmpty() ? "There was nothing to see." : "A look at the opponent's hand.";
        CardGridOverlay grid = new CardGridOverlay(title, subtitle, cards, 0, 0, "OK", input, new CardGridOverlay.Callback() {
            @Override
            public void done(List<Integer> cardIds) {
                ref.close();
                reply.answer(Boolean.TRUE);
            }
        });
        ref.set(overlays.showOverBoard(grid));
    }

    /** The screen is being left: ignore later prompts (the waiting engine thread is released elsewhere). */
    void dispose() {
        disposed = true;
        redrawReply = null;
    }
}
