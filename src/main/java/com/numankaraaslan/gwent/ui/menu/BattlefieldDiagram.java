package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import com.numankaraaslan.gwent.ui.card.Icons;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Schematic Gwent board for the rules screen: the six rows with their horn slots and example scores, the two leaders,
 * the weather zone and both players' totals.
 */
final class BattlefieldDiagram {

    private static final double ROW_ICON_SIZE = 28;
    private static final double HORN_SLOT_SIZE = 46;
    private static final double BOX_ICON_SIZE = 28;

    private BattlefieldDiagram() {
    }

    /** Builds the diagram (opponent's rows on top, yours below; example totals 35 : 29). */
    static HBox create() {
        Region divider = new Region();
        divider.getStyleClass().add("rules-board-divider");
        VBox rows = new VBox(row(RowType.SIEGE, false, 8), row(RowType.RANGED, false, 12), row(RowType.CLOSE, false, 15), divider, row(RowType.CLOSE, true, 14), row(RowType.RANGED, true, 9), row(RowType.SIEGE, true, 6));
        rows.getStyleClass().add("rules-board-rows");
        VBox side = new VBox(box(Icons.crown(BOX_ICON_SIZE), "Opponent's leader", "used once per game"), MenuUi.vSpacer(), weatherBox(), MenuUi.vSpacer(), box(Icons.crown(BOX_ICON_SIZE), "Your leader", "click it to use it"));
        side.getStyleClass().add("rules-board-side");
        VBox totals = new VBox(total("Opponent's total", 35, false), MenuUi.vSpacer(), total("Your total", 29, true));
        totals.getStyleClass().add("rules-board-totals");
        HBox board = new HBox(side, rows, totals);
        board.getStyleClass().add("rules-board");
        board.setMaxWidth(Region.USE_PREF_SIZE);
        return board;
    }

    /** Horn slot plus the row strip with its icon, name and score. */
    private static HBox row(RowType type, boolean own, int score) {
        Node horn = MenuUi.iconBox(Icons.special(SpecialType.COMMANDERS_HORN, 26), HORN_SLOT_SIZE, "rules-board-horn");
        Label name = MenuUi.label((own ? "Your " : "Opponent's ") + type.displayName() + " row", "rules-board-row-label");
        Label value = MenuUi.label(String.valueOf(score), "rules-board-score");
        HBox strip = new HBox(MenuUi.iconBox(Icons.row(type, ROW_ICON_SIZE), ROW_ICON_SIZE + 4), name, MenuUi.hSpacer(), value);
        strip.getStyleClass().addAll("rules-board-strip", own ? "rules-board-strip-own" : "rules-board-strip-enemy");
        HBox.setHgrow(strip, Priority.ALWAYS);
        HBox row = new HBox(horn, strip);
        row.getStyleClass().add("rules-board-row");
        return row;
    }

    private static VBox box(Node icon, String title, String text) {
        VBox box = new VBox(MenuUi.iconBox(icon, BOX_ICON_SIZE + 4), MenuUi.label(title, "rules-board-box-title"), MenuUi.wrapping(text, "rules-board-box-text"));
        box.getStyleClass().add("rules-board-box");
        return box;
    }

    private static VBox weatherBox() {
        HBox icons = new HBox(MenuUi.iconBox(Icons.special(SpecialType.BITING_FROST, 24), 28), MenuUi.iconBox(Icons.special(SpecialType.IMPENETRABLE_FOG, 24), 28), MenuUi.iconBox(Icons.special(SpecialType.TORRENTIAL_RAIN, 24), 28));
        icons.getStyleClass().add("rules-board-weather");
        VBox box = new VBox(icons, MenuUi.label("Weather zone", "rules-board-box-title"), MenuUi.wrapping("hits both sides", "rules-board-box-text"));
        box.getStyleClass().add("rules-board-box");
        return box;
    }

    private static VBox total(String label, int value, boolean own) {
        VBox box = new VBox(MenuUi.label(String.valueOf(value), "rules-board-total-value"), MenuUi.label(label, "rules-board-total-label"));
        box.getStyleClass().addAll("rules-board-total", own ? "rules-board-total-own" : "rules-board-total-enemy");
        return box;
    }
}
