package com.numankaraaslan.gwent.ui.game;

import javafx.animation.Animation;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * One modal overlay: a dark veil that swallows every mouse event, with its content centred on it.
 * Created and stacked by {@link OverlayLayer}.
 */
final class Overlay extends StackPane {

    private Runnable onVeilClicked;
    private boolean open = true;
    private Animation intro;

    Overlay(Node content) {
        getStyleClass().add("gm-game-veil");
        setPickOnBounds(true);
        if (content instanceof Region region) {
            region.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }
        getChildren().add(content);
        addEventHandler(MouseEvent.MOUSE_CLICKED, new VeilClick());
    }

    /** Action for a primary click on the veil outside the content (e.g. close a read-only overlay). */
    void setOnVeilClicked(Runnable action) {
        this.onVeilClicked = action;
    }

    /** False once {@link #close()} was called. */
    boolean isOpen() {
        return open;
    }

    /** Fades the overlay in (called once it is on the layer). */
    void playIntro() {
        setOpacity(0);
        intro = Animations.fade(this, 0, 1, 170);
        intro.play();
    }

    /** Fades the overlay out and removes it. Safe to call more than once. */
    void close() {
        if (!open) {
            return;
        }
        open = false;
        setMouseTransparent(true);
        if (intro != null) {
            intro.stop();
        }
        Animations.play(Animations.fade(this, getOpacity(), 0, 140), new Animations.RemoveNode(this));
    }

    /** Clicks never reach the board below; a click on the veil itself may close the overlay. */
    private final class VeilClick implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (event.getTarget() == Overlay.this && event.getButton() == MouseButton.PRIMARY && onVeilClicked != null && open) {
                onVeilClicked.run();
            }
            event.consume();
        }
    }
}
