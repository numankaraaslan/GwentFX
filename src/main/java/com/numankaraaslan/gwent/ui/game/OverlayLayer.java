package com.numankaraaslan.gwent.ui.game;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Top layer of the game screen holding the modal overlays (prompts, dialogs, menus), newest on top.
 * Board-sized overlays leave the right column (card info panel, piles, menu button) usable, so the player can still read
 * cards while a prompt is open; full-size overlays cover everything.
 */
final class OverlayLayer extends Pane {

    OverlayLayer() {
        getStyleClass().add("gm-overlay-layer");
        setPickOnBounds(false);
    }

    /** Shows {@code content} centred on a veil that covers the board (left and centre columns). */
    Overlay showOverBoard(Node content) {
        return show(content, BoardView.RIGHT_COLUMN_X);
    }

    /** Shows {@code content} centred on a veil that covers the whole screen. */
    Overlay showFull(Node content) {
        return show(content, BoardView.WIDTH);
    }

    /** True while at least one overlay is open. */
    boolean hasOpenOverlay() {
        for (Node child : getChildren()) {
            if (child instanceof Overlay overlay && overlay.isOpen()) {
                return true;
            }
        }
        return false;
    }

    private Overlay show(Node content, double width) {
        Overlay overlay = new Overlay(content);
        overlay.setManaged(false);
        getChildren().add(overlay);
        overlay.resizeRelocate(0, 0, width, BoardView.HEIGHT);
        overlay.playIntro();
        return overlay;
    }
}
