package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.ui.card.CardSize;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Lays out equally sized cards in one horizontal line in child order, centred. When they do not fit, the cards overlap
 * evenly so the line never gets wider than the pane.
 */
final class CardRowPane extends Pane {

    private final double cardWidth;
    private final double cardHeight;
    private final double gap;
    private double offsetY;

    CardRowPane(CardSize size, double gap) {
        this.cardWidth = size.width();
        this.cardHeight = size.height();
        this.gap = gap;
        getStyleClass().add("gm-card-row-pane");
    }

    /** Moves the cards down by {@code offsetY} from the vertical centre (room for lifted cards in the hand). */
    void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        List<Node> cards = getManagedChildren();
        int count = cards.size();
        if (count == 0) {
            return;
        }
        double width = getWidth();
        double step = cardWidth + gap;
        double total = count * cardWidth + (count - 1) * gap;
        if (total > width && count > 1) {
            step = (width - cardWidth) / (count - 1);
            total = width;
        }
        double x = Math.max(0, (width - total) / 2);
        double y = Math.max(0, (getHeight() - cardHeight) / 2 + offsetY);
        for (Node card : cards) {
            card.resizeRelocate(snapPositionX(x), snapPositionY(y), cardWidth, cardHeight);
            x += step;
        }
    }
}
