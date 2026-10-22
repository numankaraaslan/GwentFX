package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The weather zone in the middle of the left column. Highlighted (and clickable) while a weather card is selected. */
final class WeatherZone extends VBox {

    static final double WIDTH = 360;
    static final double HEIGHT = 152;
    private static final PseudoClass TARGET = PseudoClass.getPseudoClass("target");

    private final BoardInput input;
    private final CardRowPane cards = new CardRowPane(CardSize.TINY, 8);
    private final Label emptyLabel = Layouts.label("Clear skies", "gm-zone-empty");
    private final Map<Integer, CardView> views = new LinkedHashMap<>();
    private final Map<Integer, CardSnapshot> shown = new HashMap<>();

    WeatherZone(BoardInput input) {
        this.input = input;
        getStyleClass().addAll("gw-panel", "gm-weather-zone");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(4);
        Layouts.fix(this, WIDTH, HEIGHT);
        Label caption = Layouts.label("Weather", "gm-zone-caption");
        StackPane area = new StackPane(emptyLabel, cards);
        area.getStyleClass().add("gm-weather-cards");
        Layouts.fix(area, WIDTH - 24, CardSize.TINY.height() + 16);
        Layouts.fix(cards, WIDTH - 24, CardSize.TINY.height() + 16);
        getChildren().addAll(caption, area);
        addEventHandler(MouseEvent.MOUSE_CLICKED, new ZoneClick());
    }

    /** Shows the weather cards in play. */
    void update(List<CardSnapshot> weather) {
        Map<Integer, CardView> next = new LinkedHashMap<>();
        List<Node> ordered = new ArrayList<>();
        for (CardSnapshot card : weather) {
            CardView view = views.get(card.id());
            if (view == null) {
                view = new CardView(card, CardSize.TINY);
                view.addEventHandler(MouseEvent.MOUSE_ENTERED, new Hover(card.id()));
            }
            next.put(card.id(), view);
            ordered.add(view);
        }
        views.clear();
        views.putAll(next);
        shown.clear();
        for (CardSnapshot card : weather) {
            shown.put(card.id(), card);
        }
        cards.getChildren().setAll(ordered);
        emptyLabel.setVisible(weather.isEmpty());
    }

    /** The node of a weather card, or null. */
    CardView view(int cardId) {
        return views.get(cardId);
    }

    /** The latest snapshot of a weather card, or null. */
    CardSnapshot card(int cardId) {
        return shown.get(cardId);
    }

    /** All weather card nodes. */
    List<CardView> views() {
        return new ArrayList<>(views.values());
    }

    /** Highlights the zone as the place to play the selected weather card. */
    void setTarget(boolean target) {
        pseudoClassStateChanged(TARGET, target);
    }

    private final class Hover implements EventHandler<MouseEvent> {

        private final int cardId;

        Hover(int cardId) {
            this.cardId = cardId;
        }

        @Override
        public void handle(MouseEvent event) {
            CardSnapshot card = shown.get(cardId);
            if (card != null) {
                input.showCard(card);
            }
        }
    }

    private final class ZoneClick implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY) {
                event.consume();
                input.weatherClicked();
            }
        }
    }
}
