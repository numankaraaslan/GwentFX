package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.RowSnapshot;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import com.numankaraaslan.gwent.ui.card.Icons;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

/**
 * One combat row of one player: row score, special slot (Commander's Horn / Mardroeme / leader horn), row icon and the
 * card area. The card area shows weather (frost, fog, rain; storm = fog + rain) as translucent layers that fade in and
 * out, a gold tint while the row is doubled and a green tint while Mardroeme is active. Clicking it plays the selected
 * card here when the row is a highlighted target.
 */
final class RowView extends Pane {

    static final double WIDTH = 1164;
    static final double HEIGHT = 134;
    private static final double CARDS_X = 190;
    private static final double CARDS_Y = 6;
    private static final double CARDS_W = WIDTH - CARDS_X - 8;
    private static final double CARDS_H = HEIGHT - 12;
    private static final PseudoClass TARGET = PseudoClass.getPseudoClass("target");
    private static final PseudoClass HORN = PseudoClass.getPseudoClass("horn");
    private static final PseudoClass MARDROEME = PseudoClass.getPseudoClass("mardroeme");

    private final int side;
    private final RowType type;
    private final BoardInput input;
    private final StackPane scoreBadge = new StackPane();
    private final Label scoreLabel = Layouts.label("0", "gm-row-score-label");
    private final StackPane slot = new StackPane();
    private final StackPane cardsArea = new StackPane();
    private final CardRowPane cards = new CardRowPane(CardSize.BOARD, 4);
    private final Pane frostLayer;
    private final Pane fogLayer;
    private final Pane rainLayer;
    private final FadeTransition[] fades = new FadeTransition[3];
    private final boolean[] weatherOn = new boolean[3];
    private final Map<Integer, CardView> views = new LinkedHashMap<>();
    private final Map<Integer, CardSnapshot> shown = new HashMap<>();
    private final List<CardView> raised = new ArrayList<>();
    private final List<CardView> lowered = new ArrayList<>();
    private CardView specialView;
    private CardSnapshot specialCard;
    private Node leaderHornIcon;
    private int score;

    RowView(int side, RowType type, BoardInput input) {
        this.side = side;
        this.type = type;
        this.input = input;
        getStyleClass().addAll("gm-row-view", side == 0 ? "gm-row-own" : "gm-row-enemy");
        Layouts.fix(this, WIDTH, HEIGHT);
        scoreBadge.getStyleClass().add("gm-row-score");
        scoreBadge.getChildren().add(scoreLabel);
        Layouts.place(scoreBadge, 8, (HEIGHT - 52) / 2, 52, 52);
        slot.getStyleClass().add("gm-special-slot");
        Layouts.place(slot, 70, (HEIGHT - 100) / 2, 72, 100);
        slot.addEventHandler(MouseEvent.MOUSE_CLICKED, new SlotClick());
        StackPane icon = new StackPane(Icons.row(type, 30));
        icon.getStyleClass().add("gm-row-icon");
        icon.setMouseTransparent(true);
        Layouts.place(icon, 148, (HEIGHT - 36) / 2, 36, 36);
        cardsArea.getStyleClass().add("gm-row-cards");
        Layouts.place(cardsArea, CARDS_X, CARDS_Y, CARDS_W, CARDS_H);
        frostLayer = frostLayer();
        fogLayer = fogLayer();
        rainLayer = rainLayer();
        cardsArea.getChildren().addAll(cards, frostLayer, fogLayer, rainLayer);
        getChildren().addAll(scoreBadge, slot, icon, cardsArea);
        addEventHandler(MouseEvent.MOUSE_CLICKED, new RowClick());
    }

    /**
     * Shows the row. Card nodes are reused by id; cards whose strength went up / down are remembered
     * (see {@link #raisedViews()} / {@link #loweredViews()}).
     *
     * @param animate fade weather layers and pulse a changed score (false for the very first rendering)
     */
    void update(RowSnapshot row, boolean frost, boolean fog, boolean rain, boolean animate) {
        raised.clear();
        lowered.clear();
        Map<Integer, CardView> next = new LinkedHashMap<>();
        List<Node> ordered = new ArrayList<>();
        for (CardSnapshot card : row.cards()) {
            CardView view = views.get(card.id());
            CardSnapshot before = shown.get(card.id());
            if (view == null) {
                view = createView(card);
            } else if (!card.equals(before)) {
                view.update(card);
                if (before != null && card.def() == before.def() && card.strength() > before.strength()) {
                    raised.add(view);
                } else if (before != null && card.def() == before.def() && card.strength() < before.strength()) {
                    lowered.add(view);
                }
            }
            next.put(card.id(), view);
            ordered.add(view);
        }
        views.clear();
        views.putAll(next);
        shown.clear();
        for (CardSnapshot card : row.cards()) {
            shown.put(card.id(), card);
        }
        cards.getChildren().setAll(ordered);
        updateSlot(row);
        pseudoClassStateChanged(HORN, row.hornActive());
        cardsArea.pseudoClassStateChanged(HORN, row.hornActive());
        cardsArea.pseudoClassStateChanged(MARDROEME, row.mardroemeActive());
        setWeatherLayer(0, frostLayer, frost, animate);
        setWeatherLayer(1, fogLayer, fog, animate);
        setWeatherLayer(2, rainLayer, rain, animate);
        if (row.score() != score) {
            score = row.score();
            scoreLabel.setText(String.valueOf(score));
            if (animate) {
                Animations.pulse(scoreBadge, 1.3, 280).play();
            }
        }
    }

    private void updateSlot(RowSnapshot row) {
        CardSnapshot special = row.special();
        if (special != null) {
            if (specialView == null || specialCard == null || specialCard.id() != special.id()) {
                specialView = new CardView(special, CardSize.TINY);
                specialView.addEventHandler(MouseEvent.MOUSE_ENTERED, new HoverSpecial());
                slot.getChildren().setAll(specialView);
            } else if (!special.equals(specialCard)) {
                specialView.update(special);
            }
            specialCard = special;
            leaderHornIcon = null;
            return;
        }
        specialView = null;
        specialCard = null;
        if (row.leaderHorn()) {
            if (leaderHornIcon == null) {
                StackPane holder = new StackPane(Icons.special(SpecialType.COMMANDERS_HORN, 46));
                holder.getStyleClass().add("gm-leader-horn");
                holder.addEventHandler(MouseEvent.MOUSE_ENTERED, new HoverLeaderHorn());
                leaderHornIcon = holder;
                slot.getChildren().setAll(holder);
            }
            return;
        }
        leaderHornIcon = null;
        slot.getChildren().clear();
    }

    private CardView createView(CardSnapshot card) {
        CardView view = new CardView(card, CardSize.BOARD);
        int id = card.id();
        view.addEventHandler(MouseEvent.MOUSE_ENTERED, new HoverCard(id));
        view.addEventHandler(MouseEvent.MOUSE_EXITED, new LeaveCard());
        view.addEventHandler(MouseEvent.MOUSE_CLICKED, new CardClick(id));
        return view;
    }

    /** Fades one weather layer in or out; layers start hidden, matching the initial "no weather" state. */
    private void setWeatherLayer(int index, Pane layer, boolean on, boolean animate) {
        if (weatherOn[index] == on) {
            return;
        }
        weatherOn[index] = on;
        if (fades[index] != null) {
            fades[index].stop();
        }
        double target = on ? 1 : 0;
        if (!animate) {
            layer.setOpacity(target);
            return;
        }
        fades[index] = Animations.fade(layer, layer.getOpacity(), target, on ? 450 : 380);
        fades[index].play();
    }

    // ------------------------------------------------------------------ queries for the animator & input

    /** The node of a card in this row, or null. */
    CardView view(int cardId) {
        return views.get(cardId);
    }

    /** The latest snapshot of a card in this row, or null. */
    CardSnapshot card(int cardId) {
        return shown.get(cardId);
    }

    /** Ids of the cards in this row, in display order. */
    List<Integer> ids() {
        return new ArrayList<>(views.keySet());
    }

    /** Cards whose strength went up in the last update. */
    List<CardView> raisedViews() {
        return new ArrayList<>(raised);
    }

    /** Cards whose strength went down in the last update. */
    List<CardView> loweredViews() {
        return new ArrayList<>(lowered);
    }

    /** The card area (for row-wide flashes). */
    Node cardsArea() {
        return cardsArea;
    }

    /** What the slot shows right now (special card or leader horn), or null. */
    Node slotContent() {
        if (specialView != null) {
            return specialView;
        }
        return leaderHornIcon;
    }

    /** The special card in the slot, or null. */
    CardSnapshot specialCard() {
        return specialCard;
    }

    /** Highlights the row as a place to play the selected card. */
    void setTarget(boolean target) {
        pseudoClassStateChanged(TARGET, target);
        cardsArea.pseudoClassStateChanged(TARGET, target);
    }

    /** Highlights the special slot as a place to play the selected card. */
    void setSlotTarget(boolean target) {
        slot.pseudoClassStateChanged(TARGET, target);
    }

    // ------------------------------------------------------------------ weather layers

    private Pane weatherLayer(String styleClass) {
        Pane layer = new Pane();
        layer.getStyleClass().addAll("gm-weather-layer", styleClass);
        layer.setMouseTransparent(true);
        layer.setOpacity(0);
        Layouts.fix(layer, CARDS_W, CARDS_H);
        layer.setClip(new Rectangle(CARDS_W, CARDS_H));
        return layer;
    }

    private Pane frostLayer() {
        Pane layer = weatherLayer("gm-weather-frost");
        for (int i = 0; i < 16; i++) {
            double x = 26 + i * 60 + (i % 2) * 14;
            double y = 18 + (i * 37) % 90;
            double radius = 6 + (i % 3) * 3;
            layer.getChildren().add(snowflake(x, y, radius));
        }
        return layer;
    }

    private Group snowflake(double x, double y, double radius) {
        Group flake = new Group();
        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(i * 60);
            double dx = Math.cos(angle) * radius;
            double dy = Math.sin(angle) * radius;
            Line line = new Line(-dx, -dy, dx, dy);
            line.getStyleClass().add("gm-snowflake");
            flake.getChildren().add(line);
        }
        flake.setLayoutX(x);
        flake.setLayoutY(y);
        return flake;
    }

    private Pane fogLayer() {
        Pane layer = weatherLayer("gm-weather-fog");
        for (int i = 0; i < 7; i++) {
            Ellipse puff = new Ellipse(40 + i * 140 + (i % 2) * 30, 30 + (i % 3) * 32, 110, 34);
            puff.getStyleClass().add("gm-fog-puff");
            puff.setEffect(new GaussianBlur(22));
            layer.getChildren().add(puff);
        }
        return layer;
    }

    private Pane rainLayer() {
        Pane layer = weatherLayer("gm-weather-rain");
        for (double x = 10; x < CARDS_W + 40; x += 24) {
            Line streak = new Line(x, -10, x - 36, CARDS_H + 10);
            streak.getStyleClass().add("gm-rain-streak");
            layer.getChildren().add(streak);
        }
        return layer;
    }

    // ------------------------------------------------------------------ mouse

    private final class HoverCard implements EventHandler<MouseEvent> {

        private final int cardId;

        HoverCard(int cardId) {
            this.cardId = cardId;
        }

        @Override
        public void handle(MouseEvent event) {
            if (event.getSource() instanceof Node node) {
                node.setViewOrder(-1);
            }
            CardSnapshot card = shown.get(cardId);
            if (card != null) {
                input.showCard(card);
            }
        }
    }

    /** Overlapping cards: the hovered one comes to the front until the mouse leaves it. */
    private static final class LeaveCard implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (event.getSource() instanceof Node node) {
                node.setViewOrder(0);
            }
        }
    }

    private final class CardClick implements EventHandler<MouseEvent> {

        private final int cardId;

        CardClick(int cardId) {
            this.cardId = cardId;
        }

        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY && input.boardCardClicked(side, type, cardId)) {
                event.consume();
            }
        }
    }

    private final class HoverSpecial implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (specialCard != null) {
                input.showCard(specialCard);
            }
        }
    }

    private final class HoverLeaderHorn implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            input.showLeader(side);
        }
    }

    private final class SlotClick implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY) {
                event.consume();
                input.slotClicked(side, type);
            }
        }
    }

    private final class RowClick implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY) {
                event.consume();
                input.rowClicked(side, type);
            }
        }
    }
}
