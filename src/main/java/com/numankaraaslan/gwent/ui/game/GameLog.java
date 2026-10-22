package com.numankaraaslan.gwent.ui.game;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Compact, scrollable list of the latest event texts (newest at the bottom). */
final class GameLog extends VBox {

    private static final int MAX_ENTRIES = 120;

    private final VBox entries = new VBox(4);
    private final ScrollPane scroll = new ScrollPane(entries);

    GameLog() {
        getStyleClass().addAll("gw-panel", "gm-game-log");
        setSpacing(4);
        entries.getStyleClass().add("gm-game-log-entries");
        entries.setFillWidth(true);
        scroll.getStyleClass().add("gm-game-log-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        entries.heightProperty().addListener(new StickToBottom());
        getChildren().addAll(Layouts.label("Game log", "gm-zone-caption"), scroll);
    }

    /** Appends a line; {@code player} 0 / 1 colours it as yours / the opponent's, anything else neutral. */
    void add(String text, int player) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Label entry = Layouts.label(text, "gm-game-log-entry", player == 0 ? "gm-log-own" : player == 1 ? "gm-log-enemy" : "gm-log-neutral");
        entry.setWrapText(true);
        entry.setMaxWidth(Double.MAX_VALUE);
        entries.getChildren().add(entry);
        while (entries.getChildren().size() > MAX_ENTRIES) {
            entries.getChildren().remove(0);
        }
    }

    /** Keeps the newest line visible. */
    private final class StickToBottom implements ChangeListener<Number> {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            scroll.setVvalue(scroll.getVmax());
        }
    }
}
