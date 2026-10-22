package com.numankaraaslan.gwent.ui.game;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

/** Factory for the small transitions the board uses, plus safe chaining helpers. Use on the JavaFX thread only. */
final class Animations {

    private Animations() {
    }

    /** Plays {@code animation} and runs {@code next} once it has finished. */
    static void play(Animation animation, Runnable next) {
        animation.setOnFinished(new Then(next));
        animation.play();
    }

    /** A pause of at least one millisecond (zero-length animations are avoided on purpose). */
    static PauseTransition pause(double millis) {
        return new PauseTransition(Duration.millis(Math.max(1, millis)));
    }

    /** The animation preceded by a pause; returns the animation itself when there is no delay. */
    static Animation delayed(Animation animation, double delayMillis) {
        if (delayMillis <= 0) {
            return animation;
        }
        return new SequentialTransition(pause(delayMillis), animation);
    }

    /** Opacity from {@code from} to {@code to}. */
    static FadeTransition fade(Node node, double from, double to, double millis) {
        FadeTransition fade = new FadeTransition(Duration.millis(Math.max(1, millis)), node);
        fade.setFromValue(from);
        fade.setToValue(to);
        return fade;
    }

    /** Uniform scale from {@code from} to {@code to}. */
    static ScaleTransition scale(Node node, double from, double to, double millis) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(Math.max(1, millis)), node);
        scale.setFromX(from);
        scale.setFromY(from);
        scale.setToX(to);
        scale.setToY(to);
        scale.setInterpolator(Interpolator.EASE_OUT);
        return scale;
    }

    /** Horizontal-only scale (card flips). */
    static ScaleTransition scaleX(Node node, double from, double to, double millis) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(Math.max(1, millis)), node);
        scale.setFromX(from);
        scale.setToX(to);
        scale.setInterpolator(Interpolator.EASE_BOTH);
        return scale;
    }

    /** Translation from (fromX, fromY) to (toX, toY). */
    static TranslateTransition move(Node node, double fromX, double fromY, double toX, double toY, double millis) {
        TranslateTransition move = new TranslateTransition(Duration.millis(Math.max(1, millis)), node);
        move.setFromX(fromX);
        move.setFromY(fromY);
        move.setToX(toX);
        move.setToY(toY);
        move.setInterpolator(Interpolator.EASE_BOTH);
        return move;
    }

    /** Scale 1 -> {@code peak} -> 1. */
    static Animation pulse(Node node, double peak, double millis) {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(Math.max(1, millis / 2)), node);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(peak);
        pulse.setToY(peak);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(2);
        return pulse;
    }

    /** Left-right shake that ends where it started. */
    static Animation shake(Node node, double distance, int times, double millis) {
        Timeline shake = new Timeline();
        int steps = Math.max(1, times) * 2;
        double step = millis / (steps + 1);
        shake.getKeyFrames().add(new KeyFrame(Duration.ZERO, new KeyValue(node.translateXProperty(), 0)));
        for (int i = 0; i < steps; i++) {
            double offset = i % 2 == 0 ? distance : -distance;
            shake.getKeyFrames().add(new KeyFrame(Duration.millis(step * (i + 1)), new KeyValue(node.translateXProperty(), offset)));
        }
        shake.getKeyFrames().add(new KeyFrame(Duration.millis(millis), new KeyValue(node.translateXProperty(), 0)));
        return shake;
    }

    /** Runs {@code next} when an animation finishes. */
    private static final class Then implements EventHandler<ActionEvent> {

        private final Runnable next;

        Then(Runnable next) {
            this.next = next;
        }

        @Override
        public void handle(ActionEvent event) {
            next.run();
        }
    }

    /** Removes a node from its parent pane (used at the end of self-contained animations). */
    static final class RemoveNode implements Runnable {

        private final Node node;

        RemoveNode(Node node) {
            this.node = node;
        }

        @Override
        public void run() {
            Parent parent = node.getParent();
            if (parent instanceof Pane pane) {
                pane.getChildren().remove(node);
            }
        }
    }
}
