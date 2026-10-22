package com.numankaraaslan.gwent.ui.game;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/** Builds the content panels of the small modal dialogs (confirmations, menus, simple choices). */
final class Dialogs {

    private static final double TEXT_WIDTH = 600;

    private Dialogs() {
    }

    /**
     * A dialog panel: title, optional text, optional extra content and a row of buttons.
     *
     * @param text  body text, or null / empty for none
     * @param extra extra node shown under the text (a card, a table...), or null
     */
    static VBox panel(String title, String text, Node extra, List<Button> buttons) {
        VBox box = new VBox(20);
        box.getStyleClass().addAll("gw-panel", "gm-game-dialog");
        box.setAlignment(Pos.CENTER);
        Label titleLabel = Layouts.label(title, "gw-heading", "gm-dialog-title");
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setMaxWidth(TEXT_WIDTH + 100);
        box.getChildren().add(titleLabel);
        if (text != null && !text.isEmpty()) {
            box.getChildren().add(text(text));
        }
        if (extra != null) {
            box.getChildren().add(extra);
        }
        if (!buttons.isEmpty()) {
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER);
            row.getChildren().addAll(buttons);
            box.getChildren().add(row);
        }
        return box;
    }

    /** A wrapping, centred body text of fixed width. */
    static Label text(String text) {
        Label body = Layouts.label(text, "gw-body", "gm-dialog-text");
        body.setWrapText(true);
        body.setTextAlignment(TextAlignment.CENTER);
        body.setAlignment(Pos.CENTER);
        body.setPrefWidth(TEXT_WIDTH);
        body.setMaxWidth(TEXT_WIDTH);
        return body;
    }
}
