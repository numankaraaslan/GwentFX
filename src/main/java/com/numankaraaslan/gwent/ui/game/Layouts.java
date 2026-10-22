package com.numankaraaslan.gwent.ui.game;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/** Small helpers for the absolutely positioned layout of the fixed 1920 x 1080 board. */
final class Layouts {

    private Layouts() {
    }

    /** Fixes a region's size and puts it at (x, y) inside a plain Pane. */
    static <T extends Region> T place(T region, double x, double y, double width, double height) {
        fix(region, width, height);
        region.relocate(x, y);
        return region;
    }

    /** Fixes a region's min, pref and max size. */
    static <T extends Region> T fix(T region, double width, double height) {
        region.setMinSize(width, height);
        region.setPrefSize(width, height);
        region.setMaxSize(width, height);
        return region;
    }

    /** A label with style classes. */
    static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    /** A themed button that runs {@code action} when pressed. */
    static Button button(String text, boolean primary, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("gw-button");
        if (primary) {
            button.getStyleClass().add("gw-button-primary");
        }
        button.setOnAction(new RunAction(action));
        return button;
    }

    /** A themed button with an icon. */
    static Button button(String text, Node icon, boolean primary, Runnable action) {
        Button button = button(text, primary, action);
        button.setGraphic(icon);
        button.setGraphicTextGap(12);
        return button;
    }

    /** Adapts a Runnable to a button action. */
    private static final class RunAction implements EventHandler<ActionEvent> {

        private final Runnable action;

        RunAction(Runnable action) {
            this.action = action;
        }

        @Override
        public void handle(ActionEvent event) {
            action.run();
        }
    }
}
