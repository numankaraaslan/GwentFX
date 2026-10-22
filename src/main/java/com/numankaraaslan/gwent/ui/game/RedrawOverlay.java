package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Redraw phase: the whole hand in large cards (two rows when needed, smaller cards for very big hands).
 * Clicking a card swaps it; "Finish" keeps the hand. The new card of a swap is highlighted.
 */
final class RedrawOverlay extends VBox {

    /** What the player decided. */
    interface Listener {

        /** Swap this card for a random one from the deck. */
        void swap(int cardId);

        /** Keep the hand. */
        void finish();
    }

    private static final PseudoClass PICKABLE = PseudoClass.getPseudoClass("pickable");
    private static final double GAP = 14;
    private static final double MAX_ROW_WIDTH = 1420;

    private final BoardInput input;
    private final Listener listener;
    private final Label subtitle = Layouts.label("", "gw-body", "gm-dialog-text");
    private final VBox rows = new VBox(GAP);
    private final Button finishButton;
    private final Map<Integer, StackPane> cells = new HashMap<>();
    private List<CardSnapshot> shownHand = List.of();
    private int highlightId = -1;
    private boolean enabled;

    RedrawOverlay(BoardInput input, Listener listener) {
        this.input = input;
        this.listener = listener;
        getStyleClass().addAll("gw-panel", "gm-game-dialog", "gm-redraw-dialog");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(16);
        rows.setAlignment(Pos.CENTER);
        finishButton = Layouts.button("Finish redrawing", true, new Runnable() {
            @Override
            public void run() {
                if (enabled) {
                    setEnabled(false);
                    listener.finish();
                }
            }
        });
        getChildren().addAll(Layouts.label("Redraw", "gw-heading", "gm-dialog-title"), subtitle, rows, finishButton);
    }

    /** Shows the hand; {@code newCardId} (the card just drawn) is highlighted, -1 for none. Unchanged hands are kept. */
    void setHand(List<CardSnapshot> hand, int newCardId) {
        if (hand.equals(shownHand) && newCardId == highlightId && !cells.isEmpty()) {
            return;
        }
        shownHand = List.copyOf(hand);
        highlightId = newCardId;
        cells.clear();
        rows.getChildren().clear();
        int count = hand.size();
        CardSize size = CardSize.LARGE;
        int columns = count <= 6 ? count : (count + 1) / 2;
        if (columns * (size.width() + GAP) - GAP > MAX_ROW_WIDTH) {
            size = CardSize.MEDIUM;
            columns = Math.max(1, Math.min(count, (int) ((MAX_ROW_WIDTH + GAP) / (size.width() + GAP))));
            columns = Math.min(columns, Math.max((count + 1) / 2, 1));
        }
        columns = Math.max(1, columns);
        HBox line = null;
        for (int i = 0; i < count; i++) {
            if (i % columns == 0) {
                line = new HBox(GAP);
                line.setAlignment(Pos.CENTER);
                rows.getChildren().add(line);
            }
            CardSnapshot card = hand.get(i);
            CardView view = new CardView(card, size);
            view.setHighlighted(card.id() == highlightId);
            StackPane cell = new StackPane(view);
            cell.getStyleClass().add("gm-choice-card");
            cell.pseudoClassStateChanged(PICKABLE, enabled);
            Layouts.fix(cell, size.width(), size.height());
            cell.addEventHandler(MouseEvent.MOUSE_ENTERED, new Hover(card));
            cell.addEventHandler(MouseEvent.MOUSE_CLICKED, new Click(card.id()));
            cells.put(card.id(), cell);
            line.getChildren().add(cell);
        }
    }

    /** The highlighted (newly drawn) card, or -1. */
    int highlightId() {
        return highlightId;
    }

    /** Updates the counter text. */
    void setRedrawsLeft(int left) {
        subtitle.setText("Click a card to swap it for a random card from your deck (" + left + (left == 1 ? " redraw" : " redraws") + " left).");
    }

    /** Clicks are only accepted while the engine is waiting for a decision. */
    void setEnabled(boolean enabled) {
        this.enabled = enabled;
        finishButton.setDisable(!enabled);
        for (StackPane cell : cells.values()) {
            cell.pseudoClassStateChanged(PICKABLE, enabled);
        }
    }

    /** The node of a card in the overlay, or null. */
    Node node(int cardId) {
        return cells.get(cardId);
    }

    private final class Hover implements EventHandler<MouseEvent> {

        private final CardSnapshot card;

        Hover(CardSnapshot card) {
            this.card = card;
        }

        @Override
        public void handle(MouseEvent event) {
            input.showCard(card);
        }
    }

    private final class Click implements EventHandler<MouseEvent> {

        private final int cardId;

        Click(int cardId) {
            this.cardId = cardId;
        }

        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() != MouseButton.PRIMARY || !enabled) {
                return;
            }
            event.consume();
            setEnabled(false);
            listener.swap(cardId);
        }
    }
}
