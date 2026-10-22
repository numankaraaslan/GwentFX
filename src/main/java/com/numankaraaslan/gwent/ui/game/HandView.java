package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.TranslateTransition;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * The human player's hand along the bottom of the board. Cards are centred and overlap when crowded; the hovered card
 * rises and comes to the front, the selected card rises higher. Each card sits in a small, unmoving wrapper that takes
 * the mouse input while the card inside it is lifted, so the card's own styling is never touched.
 */
final class HandView extends StackPane {

    private static final double HOVER_LIFT = -14;
    private static final double SELECTED_LIFT = -28;
    private static final PseudoClass INACTIVE = PseudoClass.getPseudoClass("inactive");

    private final BoardInput input;
    private final CardRowPane row = new CardRowPane(CardSize.HAND, 6);
    private final Map<Integer, Slot> slots = new LinkedHashMap<>();
    private int selectedId = -1;

    HandView(BoardInput input) {
        this.input = input;
        getStyleClass().add("gm-hand-view");
        row.setOffsetY(12);
        getChildren().add(row);
        pseudoClassStateChanged(INACTIVE, true);
    }

    /** Shows the hand: nodes of cards that are still there are reused, new cards get new nodes. */
    void update(List<CardSnapshot> hand) {
        Map<Integer, Slot> next = new LinkedHashMap<>();
        List<Node> ordered = new ArrayList<>();
        for (CardSnapshot card : hand) {
            Slot slot = slots.get(card.id());
            if (slot == null) {
                slot = new Slot(card);
            } else {
                slot.update(card);
            }
            next.put(card.id(), slot);
            ordered.add(slot);
        }
        slots.clear();
        slots.putAll(next);
        row.getChildren().setAll(ordered);
        if (!slots.containsKey(selectedId)) {
            selectedId = -1;
        }
        for (Slot slot : slots.values()) {
            slot.applyLift(false);
        }
    }

    /** The on-screen node of a hand card (the card inside its wrapper, so its bounds include the lift), or null. */
    Node node(int cardId) {
        Slot slot = slots.get(cardId);
        return slot == null ? null : slot.view;
    }

    /** The latest snapshot of a hand card, or null. */
    CardSnapshot card(int cardId) {
        Slot slot = slots.get(cardId);
        return slot == null ? null : slot.card;
    }

    /** Ids of the cards in hand, in display order. */
    List<Integer> ids() {
        return new ArrayList<>(slots.keySet());
    }

    /** Selects one card (lifted + highlighted), or none with -1. */
    void setSelected(int cardId) {
        selectedId = slots.containsKey(cardId) ? cardId : -1;
        for (Slot slot : slots.values()) {
            slot.view.setSelected(slot.id == selectedId);
            slot.applyLift(true);
        }
    }

    /** Dims every card that is not in {@code playable}; null un-dims all cards. */
    void setPlayable(Set<Integer> playable) {
        for (Slot slot : slots.values()) {
            slot.view.setDimmed(playable != null && !playable.contains(slot.id));
        }
    }

    /** Input enabled (your turn) or not; an inactive hand is slightly dimmed as a whole. */
    void setActive(boolean active) {
        pseudoClassStateChanged(INACTIVE, !active);
    }

    /**
     * One card of the hand with its hover / selection lift. The wrapper itself never moves (only the card inside it is
     * lifted), so its hover area stays put and a card hovered near its bottom edge does not bounce up and down.
     */
    private final class Slot extends StackPane {

        private final int id;
        private final CardView view;
        private final TranslateTransition lift;
        private CardSnapshot card;
        private boolean hovered;

        Slot(CardSnapshot card) {
            this.id = card.id();
            this.card = card;
            this.view = new CardView(card, CardSize.HAND);
            this.lift = new TranslateTransition(Duration.millis(110), view);
            getStyleClass().add("gm-hand-slot");
            Layouts.fix(this, CardSize.HAND.width(), CardSize.HAND.height());
            getChildren().add(view);
            addEventHandler(MouseEvent.MOUSE_ENTERED, new Hover(true));
            addEventHandler(MouseEvent.MOUSE_EXITED, new Hover(false));
            addEventHandler(MouseEvent.MOUSE_CLICKED, new Click());
        }

        void update(CardSnapshot fresh) {
            if (!fresh.equals(card)) {
                card = fresh;
                view.update(fresh);
            }
        }

        /** Moves the card to its rest / hover / selected height. */
        void applyLift(boolean animate) {
            double target = id == selectedId ? SELECTED_LIFT : hovered ? HOVER_LIFT : 0;
            setViewOrder(hovered ? -2 : id == selectedId ? -1 : 0);
            lift.stop();
            if (!animate) {
                view.setTranslateY(target);
                return;
            }
            lift.setFromY(view.getTranslateY());
            lift.setToY(target);
            lift.play();
        }

        private final class Hover implements EventHandler<MouseEvent> {

            private final boolean entered;

            Hover(boolean entered) {
                this.entered = entered;
            }

            @Override
            public void handle(MouseEvent event) {
                hovered = entered;
                applyLift(true);
                if (entered) {
                    input.showCard(card);
                }
            }
        }

        private final class Click implements EventHandler<MouseEvent> {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    event.consume();
                    input.handCardClicked(id);
                }
            }
        }
    }
}
