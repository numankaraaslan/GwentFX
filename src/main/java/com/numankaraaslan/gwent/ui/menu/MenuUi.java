package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.ui.Navigator;
import com.numankaraaslan.gwent.ui.ScrollSupport;
import java.util.Locale;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Small building blocks shared by the menu screens: styled labels and buttons, fixed-size icon boxes, layout fillers,
 * leader caption texts and a scroll pane that scrolls long pages faster.
 */
final class MenuUi {

    /** Pseudo class of the selected tile, tab, option or table-of-contents entry (styled in menu.css). */
    static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    /** One mouse-wheel step scrolls this many times the default distance. */

    private MenuUi() {
    }

    /** A label with the given style classes. */
    static Label label(String text, String... styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    /** A label that wraps its text and is never squeezed below the height it needs. */
    static Label wrapping(String text, String... styleClasses) {
        Label label = label(text, styleClasses);
        label.setWrapText(true);
        label.setMinHeight(Region.USE_PREF_SIZE);
        return label;
    }

    /** A gold-framed button ({@code gw-button}) with extra style classes. */
    static Button button(String text, EventHandler<ActionEvent> handler, String... styleClasses) {
        Button button = new Button(text);
        button.getStyleClass().add("gw-button");
        button.getStyleClass().addAll(styleClasses);
        button.setOnAction(handler);
        button.setMinWidth(Region.USE_PREF_SIZE);
        return button;
    }

    /** "Back" to the main menu; also triggered by the Esc key. */
    static Button backButton(Navigator navigator) {
        Button button = button("Back", new MenuAction(navigator, MenuAction.Target.MAIN_MENU), "menu-back");
        button.setCancelButton(true);
        return button;
    }

    /** The thin gold line ({@code gw-separator}). */
    static Region separator() {
        Region line = new Region();
        line.getStyleClass().add("gw-separator");
        return line;
    }

    /** An empty region that takes all spare horizontal space of an HBox. */
    static Region hSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /** An empty region that takes all spare vertical space of a VBox. */
    static Region vSpacer() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /** Puts an icon into a fixed-size box with the icon centred, so icons of any shape line up. */
    static StackPane iconBox(Node icon, double size, String... styleClasses) {
        StackPane box = new StackPane(icon);
        box.getStyleClass().addAll(styleClasses);
        box.setMinSize(size, size);
        box.setPrefSize(size, size);
        box.setMaxSize(size, size);
        return box;
    }

    /** Style class that carries the faction colours in menu.css, e.g. "menu-faction-northern-realms". */
    static String factionStyle(Faction faction) {
        return "menu-faction-" + faction.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** The leader's title without the character name: "Foltest: King of Temeria" becomes "King of Temeria". */
    static String leaderTitle(LeaderDef leader) {
        String name = leader.name();
        int colon = name.indexOf(':');
        if (colon < 0 || colon == name.length() - 1) {
            return name;
        }
        return name.substring(colon + 1).trim();
    }

    /** The character name of a leader: "Foltest: King of Temeria" becomes "Foltest"; empty if the name has no title part. */
    static String leaderCharacter(LeaderDef leader) {
        String name = leader.name();
        int colon = name.indexOf(':');
        return colon <= 0 ? "" : name.substring(0, colon).trim();
    }

    /** Short description of when a leader ability works, for captions. */
    static String leaderKind(LeaderDef leader) {
        return leader.isPassive() ? "Passive, always on" : "Active, once per game";
    }

    /** A vertical scroll pane for long pages: the content fills the width and the mouse wheel scrolls faster. */
    static ScrollPane scrollPane(Node content) {
        ScrollPane pane = new ScrollPane(content);
        pane.getStyleClass().add("menu-scroll");
        pane.setFitToWidth(true);
        pane.setHbarPolicy(ScrollBarPolicy.NEVER);
        pane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        ScrollSupport.installFastScroll(pane);
        return pane;
    }
}
