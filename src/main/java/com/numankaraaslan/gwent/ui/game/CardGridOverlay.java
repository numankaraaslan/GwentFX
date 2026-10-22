package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.ui.ScrollSupport;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

/**
 * Cards in a scrollable grid (MEDIUM size): card choices from the engine, revealed cards and discard piles.
 * <ul>
 *   <li>pick one (min == max == 1): a click picks the card at once;</li>
 *   <li>pick several: clicks toggle cards, Confirm is enabled while the count is within [min, max];</li>
 *   <li>view only (max == 0): just a button to close.</li>
 * </ul>
 * Hovering a card shows it in the info panel.
 */
final class CardGridOverlay extends VBox {

    /** Receives the result once. */
    interface Callback {

        /** The chosen card ids (empty for view-only grids). */
        void done(List<Integer> cardIds);
    }

    private static final PseudoClass CHOSEN = PseudoClass.getPseudoClass("chosen");
    private static final PseudoClass PICKABLE = PseudoClass.getPseudoClass("pickable");
    private static final int MAX_COLUMNS = 8;
    private static final int MAX_VISIBLE_ROWS = 3;
    private static final double GAP = 14;
    /** Room around the grid for the highlight borders drawn outside the cards. */
    private static final double GRID_PADDING = 8;

    private final int min;
    private final int max;
    private final Callback callback;
    private final BoardInput input;
    private final Set<Integer> chosen = new LinkedHashSet<>();
    private final Label subtitle;
    private final Button confirm;
    private boolean finished;

    /**
     * @param subtitleText text under the title (for choices a "selected" counter is appended automatically)
     * @param max          0 for a view-only grid
     * @param buttonText   text of the confirm / close button
     */
    CardGridOverlay(String title, String subtitleText, List<CardSnapshot> cards, int min, int max, String buttonText, BoardInput input, Callback callback) {
        this.min = min;
        this.max = max;
        this.callback = callback;
        this.input = input;
        getStyleClass().addAll("gw-panel", "gm-game-dialog", "gm-card-grid-dialog");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(16);
        Label titleLabel = Layouts.label(title, "gw-heading", "gm-dialog-title");
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        subtitle = Layouts.label(subtitleText, "gw-body", "gm-dialog-text");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(TextAlignment.CENTER);
        int columns = Math.max(1, Math.min(MAX_COLUMNS, cards.size()));
        int rows = Math.max(1, (cards.size() + columns - 1) / columns);
        int visibleRows = Math.min(rows, MAX_VISIBLE_ROWS);
        double gridWidth = columns * CardSize.MEDIUM.width() + (columns - 1) * GAP;
        double gridHeight = visibleRows * CardSize.MEDIUM.height() + (visibleRows - 1) * GAP;
        FlowPane grid = new FlowPane(GAP, GAP);
        grid.getStyleClass().add("gm-card-grid");
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPrefWrapLength(gridWidth);
        grid.setPadding(new Insets(GRID_PADDING));
        for (CardSnapshot card : cards) {
            grid.getChildren().add(cell(card));
        }
        ScrollPane scroll = new ScrollPane(grid);
        scroll.getStyleClass().add("gm-card-grid-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(rows > visibleRows ? ScrollPane.ScrollBarPolicy.ALWAYS : ScrollPane.ScrollBarPolicy.NEVER);
        ScrollSupport.installFastScroll(scroll);
        scroll.setPrefViewportWidth(gridWidth + GRID_PADDING * 2);
        scroll.setPrefViewportHeight(gridHeight + GRID_PADDING * 2);
        scroll.setMaxWidth(Region.USE_PREF_SIZE);
        confirm = Layouts.button(buttonText, true, new Runnable() {
            @Override
            public void run() {
                finish(new ArrayList<>(chosen));
            }
        });
        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);
        if (!isSinglePick()) {
            buttons.getChildren().add(confirm);
        }
        getChildren().addAll(titleLabel, subtitle, scroll, buttons);
        titleLabel.setMaxWidth(gridWidth + 120);
        subtitle.setMaxWidth(gridWidth + 120);
        refresh();
    }

    private boolean isSinglePick() {
        return min == 1 && max == 1;
    }

    private boolean isViewOnly() {
        return max <= 0;
    }

    private StackPane cell(CardSnapshot card) {
        CardView view = new CardView(card, CardSize.MEDIUM);
        StackPane cell = new StackPane(view);
        cell.getStyleClass().add("gm-choice-card");
        Layouts.fix(cell, CardSize.MEDIUM.width(), CardSize.MEDIUM.height());
        cell.pseudoClassStateChanged(PICKABLE, !isViewOnly());
        cell.addEventHandler(MouseEvent.MOUSE_ENTERED, new Hover(card));
        cell.addEventHandler(MouseEvent.MOUSE_CLICKED, new Click(card.id(), cell, view));
        return cell;
    }

    private void refresh() {
        if (isViewOnly() || isSinglePick()) {
            confirm.setDisable(false);
            return;
        }
        int count = chosen.size();
        confirm.setDisable(count < min || count > max);
        String need = min == max ? "Choose " + min : "Choose " + min + " to " + max;
        subtitle.setText(need + " (" + count + " selected)");
    }

    private void finish(List<Integer> ids) {
        if (finished) {
            return;
        }
        finished = true;
        callback.done(ids);
    }

    private final class Hover implements EventHandler<MouseEvent> {

        private final CardSnapshot card;

        Hover(CardSnapshot card) {
            this.card = card;
        }

        @Override
        public void handle(MouseEvent event) {
            input.showCard(card);
        }
    }

    private final class Click implements EventHandler<MouseEvent> {

        private final int cardId;
        private final StackPane cell;
        private final CardView view;

        Click(int cardId, StackPane cell, CardView view) {
            this.cardId = cardId;
            this.cell = cell;
            this.view = view;
        }

        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() != MouseButton.PRIMARY || isViewOnly() || finished) {
                return;
            }
            event.consume();
            if (isSinglePick()) {
                view.setSelected(true);
                cell.pseudoClassStateChanged(CHOSEN, true);
                List<Integer> ids = new ArrayList<>();
                ids.add(cardId);
                finish(ids);
                return;
            }
            if (chosen.contains(cardId)) {
                chosen.remove(cardId);
            } else if (chosen.size() < max) {
                chosen.add(cardId);
            }
            boolean selected = chosen.contains(cardId);
            view.setSelected(selected);
            cell.pseudoClassStateChanged(CHOSEN, selected);
            refresh();
        }
    }
}
