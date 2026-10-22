package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.PlayerSnapshot;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.ui.card.CardBackView;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import java.util.List;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Discard pile and deck of one player (right column). The discard pile shows its top card and size; clicking it opens
 * the whole pile. The deck shows a card back and the number of cards left.
 */
final class PileView extends HBox {

    private final int side;
    private final BoardInput input;
    private final StackPane discardSlot = new StackPane();
    private final StackPane deckSlot = new StackPane();
    private final Label discardCount = Layouts.label("0", "gm-pile-count");
    private final Label deckCount = Layouts.label("0", "gm-pile-count");
    private final Label discardEmpty = Layouts.label("Empty", "gm-pile-empty");
    private final Label deckEmpty = Layouts.label("Empty", "gm-pile-empty");
    private CardView topView;
    private CardSnapshot topCard;
    private CardBackView back;
    private Faction faction;
    private int discardSize = -1;
    private int deckSize = -1;

    PileView(int side, BoardInput input) {
        this.side = side;
        this.input = input;
        getStyleClass().add("gm-pile-view");
        setSpacing(18);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(pile("Discard", discardSlot, discardCount, discardEmpty), pile("Deck", deckSlot, deckCount, deckEmpty));
        discardSlot.getStyleClass().add("gm-discard-slot");
        discardSlot.addEventHandler(MouseEvent.MOUSE_CLICKED, new DiscardClick());
        discardSlot.addEventHandler(MouseEvent.MOUSE_ENTERED, new DiscardHover());
    }

    private VBox pile(String caption, StackPane slot, Label count, Label empty) {
        slot.getStyleClass().add("gm-pile-slot");
        Layouts.fix(slot, CardSize.TINY.width(), CardSize.TINY.height());
        StackPane.setAlignment(count, Pos.BOTTOM_CENTER);
        count.setMouseTransparent(true);
        empty.setMouseTransparent(true);
        slot.getChildren().addAll(empty, count);
        VBox box = new VBox(4, Layouts.label(caption, "gm-pile-caption"), slot);
        box.setAlignment(Pos.TOP_CENTER);
        return box;
    }

    /** Shows the player's deck and discard pile. */
    void update(PlayerSnapshot player) {
        if (faction != player.faction()) {
            faction = player.faction();
            if (back != null) {
                deckSlot.getChildren().remove(back);
            }
            back = new CardBackView(faction, CardSize.TINY);
            back.setMouseTransparent(true);
            deckSlot.getChildren().add(1, back);
        }
        if (deckSize != player.deckCount()) {
            deckSize = player.deckCount();
            deckCount.setText(String.valueOf(deckSize));
            back.setVisible(deckSize > 0);
            deckEmpty.setVisible(deckSize == 0);
            deckCount.setVisible(deckSize > 0);
        }
        List<CardSnapshot> discard = player.discard();
        CardSnapshot top = discard.isEmpty() ? null : discard.get(discard.size() - 1);
        if (top == null) {
            if (topView != null) {
                discardSlot.getChildren().remove(topView);
                topView = null;
            }
        } else if (topCard == null || topView == null || top.id() != topCard.id()) {
            if (topView != null) {
                discardSlot.getChildren().remove(topView);
            }
            topView = new CardView(top, CardSize.TINY);
            topView.setMouseTransparent(true);
            discardSlot.getChildren().add(1, topView);
        }
        topCard = top;
        if (discardSize != discard.size()) {
            discardSize = discard.size();
            discardCount.setText(String.valueOf(discardSize));
        }
        discardEmpty.setVisible(discard.isEmpty());
        discardCount.setVisible(!discard.isEmpty());
    }

    /** The deck pile node (where drawn cards come from). */
    Node deckNode() {
        return deckSlot;
    }

    /** The discard pile node. */
    Node discardNode() {
        return discardSlot;
    }

    private final class DiscardClick implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY) {
                event.consume();
                input.discardClicked(side);
            }
        }
    }

    private final class DiscardHover implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (topCard != null) {
                input.showCard(topCard);
            }
        }
    }
}
