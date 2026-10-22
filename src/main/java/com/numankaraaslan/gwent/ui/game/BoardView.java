package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.GamePhase;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.GameSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.PlayerSnapshot;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import com.numankaraaslan.gwent.ui.card.CardInfoPanel;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Scale;

/**
 * The static board on the fixed 1920 x 1080 canvas.
 *
 * <ul>
 *   <li>Left column: round header, opponent panel, weather zone, your panel, Pass button, game log.</li>
 *   <li>Centre: opponent rows (siege, ranged, close), a divider with the hint line, your rows (close, ranged, siege),
 *       your hand at the bottom.</li>
 *   <li>Right column: opponent discard / deck and the Menu button, the card info panel, your discard / deck.</li>
 * </ul>
 * {@link #render} brings everything in line with a snapshot, reusing card nodes by id.
 */
final class BoardView extends Pane {

    static final double WIDTH = 1920;
    static final double HEIGHT = 1080;
    static final double LEFT_X = 10;
    static final double LEFT_W = 360;
    static final double CENTER_X = 380;
    static final double CENTER_W = 1180;
    static final double RIGHT_COLUMN_X = 1560;
    static final double RIGHT_X = 1568;
    static final double RIGHT_W = 344;
    static final double ROW_X = 388;
    static final double DIVIDER_Y = 422;
    static final double DIVIDER_H = 40;
    static final double HAND_Y = 884;
    static final double HAND_H = 188;
    /** Row tops by RowType ordinal (CLOSE, RANGED, SIEGE) for your side: close next to the divider. */
    private static final double[] OWN_ROW_Y = {466, 604, 742};
    /** Row tops by RowType ordinal for the opponent: mirrored, close next to the divider. */
    private static final double[] ENEMY_ROW_Y = {284, 146, 8};
    private static final double INFO_Y = 126;
    /** Room for the card info panel: its own maximum height, but never more than the column can give. */
    private static final double INFO_H = Math.min(CardInfoPanel.MAX_HEIGHT, 820);

    private final PlayerPanel[] panels = new PlayerPanel[2];
    private final RowView[][] rows = new RowView[2][RowType.values().length];
    private final PileView[] piles = new PileView[2];
    private final WeatherZone weather;
    private final HandView hand;
    private final CardInfoPanel info = new CardInfoPanel();
    private final GameLog log = new GameLog();
    private final Label roundLabel = Layouts.label("Preparing the battle", "gm-round-label");
    private final Label hintLabel = Layouts.label("", "gm-hint-label");
    private final Button passButton;
    private GameSnapshot snapshot;

    BoardView(BoardInput input, String subtitle) {
        getStyleClass().add("gm-board-view");
        Layouts.fix(this, WIDTH, HEIGHT);
        getChildren().add(Layouts.place(styled(new Region(), "gm-side-column"), 0, 0, CENTER_X, HEIGHT));
        getChildren().add(Layouts.place(styled(new Region(), "gm-side-column", "gm-side-column-right"), RIGHT_COLUMN_X, 0, WIDTH - RIGHT_COLUMN_X, HEIGHT));
        getChildren().add(Layouts.place(styled(new Region(), "gm-board-surface", "gm-board-surface-enemy"), CENTER_X, 0, CENTER_W, DIVIDER_Y));
        getChildren().add(Layouts.place(styled(new Region(), "gm-board-surface", "gm-board-surface-own"), CENTER_X, DIVIDER_Y + DIVIDER_H, CENTER_W, HAND_Y - DIVIDER_Y - DIVIDER_H - 4));
        getChildren().add(Layouts.place(styled(new Region(), "gm-hand-tray"), CENTER_X, HAND_Y - 4, CENTER_W, HEIGHT - HAND_Y + 4));
        getChildren().add(Layouts.place(styled(new Region(), "gm-board-divider"), ROW_X, DIVIDER_Y + DIVIDER_H / 2 - 1, RowView.WIDTH, 2));
        for (int side = 0; side < 2; side++) {
            for (RowType type : RowType.values()) {
                RowView row = new RowView(side, type, input);
                double y = side == 0 ? OWN_ROW_Y[type.ordinal()] : ENEMY_ROW_Y[type.ordinal()];
                row.relocate(ROW_X, y);
                rows[side][type.ordinal()] = row;
                getChildren().add(row);
            }
        }
        hintLabel.setAlignment(Pos.CENTER);
        hintLabel.setVisible(false);
        StackPane hintBox = new StackPane(hintLabel);
        hintBox.setMouseTransparent(true);
        getChildren().add(Layouts.place(hintBox, ROW_X, DIVIDER_Y, RowView.WIDTH, DIVIDER_H));
        hand = Layouts.place(new HandView(input), ROW_X, HAND_Y, RowView.WIDTH, HAND_H);
        getChildren().add(hand);
        roundLabel.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(Layouts.place(roundLabel, LEFT_X + 6, 18, LEFT_W - 12, 44));
        Label subtitleLabel = Layouts.label(subtitle, "gm-round-subtitle");
        getChildren().add(Layouts.place(subtitleLabel, LEFT_X + 6, 62, LEFT_W - 12, 30));
        panels[1] = Layouts.place(new PlayerPanel(1, input), LEFT_X, 112, PlayerPanel.WIDTH, PlayerPanel.HEIGHT);
        weather = Layouts.place(new WeatherZone(input), LEFT_X, DIVIDER_Y + DIVIDER_H / 2 - WeatherZone.HEIGHT / 2, WeatherZone.WIDTH, WeatherZone.HEIGHT);
        panels[0] = Layouts.place(new PlayerPanel(0, input), LEFT_X, 532, PlayerPanel.WIDTH, PlayerPanel.HEIGHT);
        passButton = Layouts.button("Pass", true, new Runnable() {
            @Override
            public void run() {
                input.passClicked();
            }
        });
        passButton.getStyleClass().add("gm-pass-button");
        passButton.setDisable(true);
        Layouts.place(passButton, LEFT_X, 788, LEFT_W, 54);
        Layouts.place(log, LEFT_X, 856, LEFT_W, HEIGHT - 856 - 12);
        getChildren().addAll(panels[1], weather, panels[0], passButton, log);
        piles[1] = new PileView(1, input);
        piles[1].relocate(RIGHT_X + 8, 6);
        Button menuButton = Layouts.button("Menu", false, new Runnable() {
            @Override
            public void run() {
                input.menuClicked();
            }
        });
        menuButton.getStyleClass().add("gm-menu-button");
        Layouts.place(menuButton, RIGHT_X + RIGHT_W - 130, 10, 130, 46);
        StackPane infoBox = new StackPane(info);
        infoBox.getStyleClass().add("gm-info-box");
        infoBox.setAlignment(Pos.TOP_CENTER);
        Layouts.place(infoBox, RIGHT_X, INFO_Y, RIGHT_W, INFO_H);
        Scale shrink = new Scale(1, 1, CardInfoPanel.PANEL_WIDTH / 2, 0);
        info.getTransforms().add(shrink);
        info.heightProperty().addListener(new FitInfoPanel(shrink));
        piles[0] = new PileView(0, input);
        piles[0].relocate(RIGHT_X + 8, INFO_Y + INFO_H + 8);
        getChildren().addAll(piles[1], menuButton, infoBox, piles[0]);
    }

    private static Region styled(Region region, String... styleClasses) {
        region.getStyleClass().addAll(styleClasses);
        region.setMouseTransparent(true);
        return region;
    }

    // ------------------------------------------------------------------ rendering

    /**
     * Brings the whole board in line with {@code snapshot}.
     *
     * @param animate let views fade / pulse changes (false for the first rendering)
     */
    void render(GameSnapshot next, boolean animate) {
        this.snapshot = next;
        boolean playing = next.phase() == GamePhase.PLAYING;
        for (int side = 0; side < 2; side++) {
            PlayerSnapshot player = next.player(side);
            PlayerSnapshot other = next.player(1 - side);
            boolean turn = playing && next.currentPlayer() == side && !player.passed();
            panels[side].update(player, turn, player.score() > other.score(), animate);
            for (RowType type : RowType.values()) {
                boolean frost = hasWeather(next, type, SpecialType.BITING_FROST);
                boolean fog = hasWeather(next, type, SpecialType.IMPENETRABLE_FOG) || hasWeather(next, type, SpecialType.SKELLIGE_STORM);
                boolean rain = hasWeather(next, type, SpecialType.TORRENTIAL_RAIN) || hasWeather(next, type, SpecialType.SKELLIGE_STORM);
                rows[side][type.ordinal()].update(player.row(type), frost, fog, rain, animate);
            }
            piles[side].update(player);
        }
        weather.update(next.weather());
        hand.update(next.human().hand());
        roundLabel.setText(next.round() == 0 ? "Preparing the battle" : "Round " + next.round());
        info.refresh(next);
    }

    private static boolean hasWeather(GameSnapshot snapshot, RowType row, SpecialType weatherType) {
        for (CardSnapshot card : snapshot.weather()) {
            if (card.def().special() == weatherType && weatherType.affectedRows().contains(row)) {
                return true;
            }
        }
        return false;
    }

    /** The last rendered snapshot, or null before the first event. */
    GameSnapshot snapshot() {
        return snapshot;
    }

    // ------------------------------------------------------------------ parts

    PlayerPanel panel(int side) {
        return panels[side];
    }

    RowView row(int side, RowType type) {
        return rows[side][type.ordinal()];
    }

    /** All six rows. */
    List<RowView> rows() {
        List<RowView> all = new ArrayList<>();
        for (int side = 0; side < 2; side++) {
            for (RowView row : rows[side]) {
                all.add(row);
            }
        }
        return all;
    }

    PileView pile(int side) {
        return piles[side];
    }

    WeatherZone weather() {
        return weather;
    }

    HandView hand() {
        return hand;
    }

    CardInfoPanel info() {
        return info;
    }

    GameLog log() {
        return log;
    }

    Button passButton() {
        return passButton;
    }

    /** Sets the hint line on the divider (empty hides it). */
    void setHint(String text) {
        hintLabel.setText(text == null ? "" : text);
        hintLabel.setVisible(text != null && !text.isEmpty());
    }

    /** The row holding a battlefield card, or null. */
    RowView rowOf(int cardId) {
        for (int side = 0; side < 2; side++) {
            for (RowView row : rows[side]) {
                if (row.view(cardId) != null) {
                    return row;
                }
            }
        }
        return null;
    }

    /** Scales the info panel down when its content is taller than the space in the right column. */
    private static final class FitInfoPanel implements ChangeListener<Number> {

        private final Scale shrink;

        FitInfoPanel(Scale shrink) {
            this.shrink = shrink;
        }

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            double height = newValue.doubleValue();
            double factor = height > INFO_H ? INFO_H / height : 1;
            shrink.setX(factor);
            shrink.setY(factor);
        }
    }
}
