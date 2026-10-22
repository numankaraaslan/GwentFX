package com.numankaraaslan.gwent.ui.menu;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A card or leader with a caption (and an optional second, dimmer line) underneath, used by the card gallery and the
 * leader choice. It glows on hover; {@link #setSelected} marks the chosen tile. The captions have a fixed width so
 * all tiles of a row are equally wide.
 */
final class CardTile extends VBox {

    /**
     * @param card          the card node (CardView, LeaderView, ...)
     * @param captionWidth  width of the caption lines (at least the card width)
     * @param caption       first caption line
     * @param detail        second caption line, or null
     * @param captionStyles extra style classes of the first caption line
     */
    CardTile(Node card, double captionWidth, String caption, String detail, String... captionStyles) {
        getStyleClass().add("menu-tile");
        getChildren().addAll(card, captionLabel(caption, captionWidth, "menu-tile-caption", captionStyles));
        if (detail != null && !detail.isEmpty()) {
            getChildren().add(captionLabel(detail, captionWidth, "menu-tile-detail"));
        }
    }

    /** Marks this tile as the chosen one (gold frame). */
    void setSelected(boolean selected) {
        pseudoClassStateChanged(MenuUi.SELECTED, selected);
    }

    private static Label captionLabel(String text, double width, String style, String... extraStyles) {
        Label label = MenuUi.wrapping(text, style);
        label.getStyleClass().addAll(extraStyles);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        return label;
    }
}
