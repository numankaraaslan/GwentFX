package com.numankaraaslan.gwent.ui.game;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.Animation;
import javafx.animation.ParallelTransition;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * A group of animations played together as one step of an event. Temporary nodes (ghost cards, halos, banners) are
 * added to the effects layer and removed when the step ends; real nodes hidden while a ghost flies to them are shown
 * again at the end. The step always finishes, even when it contains no animation.
 */
final class FxBatch {

    private final Pane layer;
    private final List<Animation> parts = new ArrayList<>();
    private final List<Node> transients = new ArrayList<>();
    private final List<Node> hidden = new ArrayList<>();

    FxBatch(Pane layer) {
        this.layer = layer;
    }

    /** Adds an animation that starts with the step. */
    void add(Animation animation) {
        parts.add(animation);
    }

    /** Adds an animation that starts {@code delayMillis} after the step. */
    void add(Animation animation, double delayMillis) {
        parts.add(Animations.delayed(animation, delayMillis));
    }

    /** Puts a temporary node on the effects layer; it is removed when the step ends. */
    void addTransient(Node node) {
        node.setManaged(false);
        layer.getChildren().add(node);
        transients.add(node);
    }

    /** Hides a real node until the step ends (a ghost is flying to its place). */
    void hide(Node target) {
        if (target != null && target.isVisible()) {
            target.setVisible(false);
            hidden.add(target);
        }
    }

    /** Plays every part in parallel, cleans up, then runs {@code next}. */
    void play(Runnable next) {
        ParallelTransition all = new ParallelTransition();
        all.getChildren().addAll(parts);
        all.getChildren().add(Animations.pause(1));
        Animations.play(all, new Runnable() {
            @Override
            public void run() {
                cleanup();
                next.run();
            }
        });
    }

    /** Removes the temporary nodes and shows the hidden ones. Safe to call more than once. */
    void cleanup() {
        layer.getChildren().removeAll(transients);
        transients.clear();
        for (Node node : hidden) {
            node.setVisible(true);
        }
        hidden.clear();
    }
}
