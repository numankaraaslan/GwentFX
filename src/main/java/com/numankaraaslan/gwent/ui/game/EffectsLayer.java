package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.ui.card.CardBackView;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Transparent layer above the board for everything that moves: ghost cards flying between zones, pop-ins, glows,
 * row flashes, banners and toasts. Its coordinates are the 1920 x 1080 canvas. It never takes mouse input.
 */
final class EffectsLayer extends Pane {

    private static final int MAX_TOASTS = 4;
    private static final double CENTER_Y = BoardView.DIVIDER_Y + BoardView.DIVIDER_H / 2;

    private final VBox toasts = new VBox(8);

    EffectsLayer() {
        getStyleClass().add("gm-effects-layer");
        setMouseTransparent(true);
        setPickOnBounds(false);
        toasts.getStyleClass().add("gm-toast-stack");
        toasts.setAlignment(Pos.BOTTOM_CENTER);
        toasts.setFillWidth(false);
        toasts.setManaged(false);
        toasts.setMouseTransparent(true);
        toasts.resizeRelocate(BoardView.CENTER_X, CENTER_Y - 330, BoardView.CENTER_W, 300);
        getChildren().add(toasts);
    }

    /** A new, empty animation step on this layer. */
    FxBatch batch() {
        return new FxBatch(this);
    }

    /** The node's box in this layer's coordinates, or null for no node. */
    Bounds boundsOf(Node node) {
        if (node == null) {
            return null;
        }
        Bounds scene = node.localToScene(node.getLayoutBounds());
        return sceneToLocal(scene);
    }

    // ------------------------------------------------------------------ cards

    /** A ghost card flies from {@code from} (layer coordinates) to {@code target}, which stays hidden until the step ends. */
    void fly(FxBatch batch, CardSnapshot card, CardSize size, Bounds from, Node target, double delay, double millis) {
        Bounds to = boundsOf(target);
        if (to == null) {
            return;
        }
        if (from == null || from.getWidth() <= 0) {
            pop(batch, card, size, target, delay, millis);
            return;
        }
        CardView ghost = cardGhost(card, size, to);
        double startScale = from.getWidth() / size.width();
        travel(batch, ghost, from, to, startScale, 1, false, delay, millis);
        batch.hide(target);
    }

    /** A ghost card pops into the place of {@code target} (scale 1.35 -> 1, fade in); the target is hidden meanwhile. */
    void pop(FxBatch batch, CardSnapshot card, CardSize size, Node target, double delay, double millis) {
        Bounds to = boundsOf(target);
        if (to == null) {
            return;
        }
        CardView ghost = cardGhost(card, size, to);
        ghost.setOpacity(0);
        ghost.setScaleX(1.35);
        ghost.setScaleY(1.35);
        batch.addTransient(ghost);
        batch.hide(target);
        batch.add(new ParallelTransition(Animations.fade(ghost, 0, 1, millis * 0.6), Animations.scale(ghost, 1.35, 1, millis)), delay);
    }

    /** A copy of a card leaves {@code from} towards {@code to} (both layer coordinates), shrinking and fading out. */
    void flyAway(FxBatch batch, CardSnapshot card, CardSize size, Bounds from, Bounds to, double delay, double millis) {
        if (from == null || to == null) {
            return;
        }
        CardView ghost = cardGhost(card, size, from);
        travel(batch, ghost, from, to, 1, 0.45, true, delay, millis);
    }

    /** A card back travels from {@code from} to {@code to} (opponent draws, decks), fading out at the end. */
    void flyBack(FxBatch batch, Faction faction, Bounds from, Bounds to, double delay, double millis) {
        if (from == null || to == null) {
            return;
        }
        CardBackView ghost = new CardBackView(faction, CardSize.BOARD);
        ghost.setMouseTransparent(true);
        ghost.resizeRelocate(from.getCenterX() - CardSize.BOARD.width() / 2, from.getCenterY() - CardSize.BOARD.height() / 2, CardSize.BOARD.width(), CardSize.BOARD.height());
        travel(batch, ghost, from, to, from.getWidth() / CardSize.BOARD.width(), 0.5, true, delay, millis);
    }

    /**
     * Adds an already placed ghost to the step and moves its centre from the centre of {@code from} to the centre of
     * {@code to}, scaling it and optionally fading it out at the end.
     */
    private void travel(FxBatch batch, Node ghost, Bounds from, Bounds to, double startScale, double endScale, boolean fadeOut, double delay, double millis) {
        double homeX = ghost.getLayoutX() + ghost.getLayoutBounds().getWidth() / 2;
        double homeY = ghost.getLayoutY() + ghost.getLayoutBounds().getHeight() / 2;
        double startX = from.getCenterX() - homeX;
        double startY = from.getCenterY() - homeY;
        double endX = to.getCenterX() - homeX;
        double endY = to.getCenterY() - homeY;
        ghost.setTranslateX(startX);
        ghost.setTranslateY(startY);
        ghost.setScaleX(startScale);
        ghost.setScaleY(startScale);
        batch.addTransient(ghost);
        ParallelTransition flight = new ParallelTransition(Animations.move(ghost, startX, startY, endX, endY, millis), Animations.scale(ghost, startScale, endScale, millis));
        if (fadeOut) {
            flight.getChildren().add(Animations.delayed(Animations.fade(ghost, 1, 0, millis * 0.35), millis * 0.65));
        }
        batch.add(flight, delay);
    }

    /** A big copy of a card or leader shown in the middle of the board for a moment. */
    void showcase(FxBatch batch, Node card, double width, double height, double millis) {
        card.setMouseTransparent(true);
        card.resizeRelocate(BoardView.CENTER_X + BoardView.CENTER_W / 2 - width / 2, CENTER_Y - height / 2, width, height);
        card.setOpacity(0);
        batch.addTransient(card);
        ParallelTransition in = new ParallelTransition(Animations.fade(card, 0, 1, 180), Animations.scale(card, 0.6, 1.04, 240));
        ParallelTransition out = new ParallelTransition(Animations.fade(card, 1, 0, 260), Animations.scale(card, 1.04, 1.18, 260));
        batch.add(new SequentialTransition(in, Animations.pause(Math.max(1, millis - 500)), out));
    }

    /** A big card in the middle of the board (Scorch). */
    void showcase(FxBatch batch, CardSnapshot card, double millis) {
        showcase(batch, new CardView(card, CardSize.LARGE), CardSize.LARGE.width(), CardSize.LARGE.height(), millis);
    }

    private CardView cardGhost(CardSnapshot card, CardSize size, Bounds at) {
        CardView ghost = new CardView(card, size);
        ghost.setMouseTransparent(true);
        ghost.resizeRelocate(at.getCenterX() - size.width() / 2, at.getCenterY() - size.height() / 2, size.width(), size.height());
        return ghost;
    }

    // ------------------------------------------------------------------ glows & flashes

    /** A coloured glow around a node that fades in and out. */
    void halo(FxBatch batch, Node target, Color color, double delay, double millis) {
        Bounds box = boundsOf(target);
        if (box == null) {
            return;
        }
        Rectangle rect = new Rectangle(box.getWidth() + 10, box.getHeight() + 10);
        rect.setArcWidth(14);
        rect.setArcHeight(14);
        rect.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.28));
        rect.setStroke(color);
        rect.setStrokeWidth(3);
        rect.setEffect(new DropShadow(BlurType.GAUSSIAN, color, 26, 0.45, 0, 0));
        rect.setMouseTransparent(true);
        rect.setOpacity(0);
        batch.addTransient(rect);
        rect.relocate(box.getMinX() - 5, box.getMinY() - 5);
        batch.add(new SequentialTransition(Animations.fade(rect, 0, 1, millis * 0.3), Animations.pause(millis * 0.3), Animations.fade(rect, 1, 0, millis * 0.4)), delay);
    }

    /** A band of light sweeping along an area (horn / mardroeme on a row). */
    void sweep(FxBatch batch, Node area, Color color, double delay, double millis) {
        Bounds box = boundsOf(area);
        if (box == null) {
            return;
        }
        Pane holder = flashHolder(box);
        Rectangle tint = new Rectangle(box.getWidth(), box.getHeight(), Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.20));
        double bandWidth = Math.min(280, box.getWidth() / 2);
        Rectangle band = new Rectangle(bandWidth, box.getHeight());
        band.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, new Stop(0, Color.TRANSPARENT), new Stop(0.5, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.65)), new Stop(1, Color.TRANSPARENT)));
        band.setTranslateX(-bandWidth);
        holder.getChildren().addAll(tint, band);
        batch.addTransient(holder);
        double travel = Math.max(1, millis - 200);
        ParallelTransition body = new ParallelTransition(Animations.move(band, -bandWidth, 0, box.getWidth(), 0, travel));
        batch.add(new SequentialTransition(Animations.fade(holder, 0, 1, 100), body, Animations.fade(holder, 1, 0, 100)), delay);
    }

    /** Flickering fire over an area (scorch). */
    void fire(FxBatch batch, Node area, double delay, double millis) {
        Bounds box = boundsOf(area);
        if (box == null) {
            return;
        }
        Pane holder = flashHolder(box);
        Rectangle flames = new Rectangle(box.getWidth(), box.getHeight());
        flames.setFill(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE, new Stop(0, Color.color(1.0, 0.45, 0.05, 0.75)), new Stop(0.55, Color.color(0.95, 0.2, 0.05, 0.35)), new Stop(1, Color.TRANSPARENT)));
        holder.getChildren().add(flames);
        batch.addTransient(holder);
        Timeline flicker = new Timeline();
        flicker.getKeyFrames().add(new KeyFrame(Duration.ZERO, new KeyValue(holder.opacityProperty(), 0)));
        flicker.getKeyFrames().add(new KeyFrame(Duration.millis(millis * 0.15), new KeyValue(holder.opacityProperty(), 0.95)));
        flicker.getKeyFrames().add(new KeyFrame(Duration.millis(millis * 0.3), new KeyValue(holder.opacityProperty(), 0.55)));
        flicker.getKeyFrames().add(new KeyFrame(Duration.millis(millis * 0.45), new KeyValue(holder.opacityProperty(), 1.0)));
        flicker.getKeyFrames().add(new KeyFrame(Duration.millis(millis * 0.65), new KeyValue(holder.opacityProperty(), 0.6)));
        flicker.getKeyFrames().add(new KeyFrame(Duration.millis(millis), new KeyValue(holder.opacityProperty(), 0)));
        batch.add(flicker, delay);
    }

    private Pane flashHolder(Bounds box) {
        Pane holder = new Pane();
        holder.setMouseTransparent(true);
        holder.setOpacity(0);
        holder.resizeRelocate(box.getMinX(), box.getMinY(), box.getWidth(), box.getHeight());
        holder.setClip(new Rectangle(box.getWidth(), box.getHeight()));
        return holder;
    }

    // ------------------------------------------------------------------ texts

    /** A wide banner across the middle of the board: fade in, hold, fade out. */
    void banner(FxBatch batch, String title, String subtitle, String variant, double holdMillis) {
        VBox band = new VBox(4);
        band.getStyleClass().add("gm-banner");
        if (variant != null && !variant.isEmpty()) {
            band.getStyleClass().add(variant);
        }
        band.setAlignment(Pos.CENTER);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("gm-banner-title");
        band.getChildren().add(titleLabel);
        if (subtitle != null && !subtitle.isEmpty()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("gm-banner-subtitle");
            subtitleLabel.setWrapText(true);
            band.getChildren().add(subtitleLabel);
        }
        band.setMouseTransparent(true);
        band.setOpacity(0);
        band.resizeRelocate(BoardView.CENTER_X, CENTER_Y - 75, BoardView.CENTER_W, 150);
        batch.addTransient(band);
        ParallelTransition in = new ParallelTransition(Animations.fade(band, 0, 1, 220), Animations.scale(band, 0.92, 1, 260));
        batch.add(new SequentialTransition(in, Animations.pause(holdMillis), Animations.fade(band, 1, 0, 280)));
    }

    /** A short message above the middle of the board; it fades out on its own and never blocks the game. */
    void toast(String text, String variant) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Label label = new Label(text);
        label.getStyleClass().add("gm-toast");
        if (variant != null && !variant.isEmpty()) {
            label.getStyleClass().add(variant);
        }
        label.setWrapText(true);
        label.setMaxWidth(BoardView.CENTER_W - 200);
        label.setOpacity(0);
        toasts.getChildren().add(label);
        while (toasts.getChildren().size() > MAX_TOASTS) {
            toasts.getChildren().remove(0);
        }
        SequentialTransition life = new SequentialTransition(Animations.fade(label, 0, 1, 160), Animations.pause(2400), Animations.fade(label, 1, 0, 450));
        Animations.play(life, new Animations.RemoveNode(label));
    }
}
