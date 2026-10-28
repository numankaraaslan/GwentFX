package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.ui.Navigator;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import com.numankaraaslan.gwent.ui.card.Icons;
import com.numankaraaslan.gwent.ui.card.LeaderView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Title screen: the big GWENT title with the menu buttons (Free Play, Collector Mode, Card Gallery, How to Play, Quit)
 * on the left and a fan of iconic cards (Foltest, Triss, Geralt, Ciri, Yennefer, Eredin) on the right. The version and a
 * button that opens the releases page sit in the bottom-left corner.
 */
public class MainMenuScreen extends StackPane {

    /** Shown in the bottom-left corner. Keep it the same as the version in pom.xml. */
    private static final String VERSION = "2.0.0";

    /** Size of the (unscaled) area the card fan is laid out in. */
    private static final double FAN_WIDTH = 1000;
    private static final double FAN_HEIGHT = 470;
    /** The cards sit on an arc around a point this far below the middle of the fan. */
    private static final double FAN_RADIUS = 950;
    /** Angle between two neighbouring cards, in degrees. */
    private static final double FAN_STEP = 7.2;
    /** Vertical position of the centre of the middle cards. */
    private static final double FAN_CENTER_Y = 200;
    private static final double FAN_SCALE = 1.1;
    /** Size of the warm light behind the fan. */
    private static final double GLOW_WIDTH = 1080;
    private static final double GLOW_HEIGHT = 760;
    /** Heroes of the fan, left to right, between the two leaders (looked up by name). */
    private static final String[] FAN_HEROES = {"Triss Merigold", "Geralt of Rivia", "Cirilla Fiona Elen Riannon", "Yennefer of Vengerberg"};
    /** If some of the cards above are missing from the data, other heroes fill the fan up to this many cards. */
    private static final int FAN_MIN_CARDS = 4;

    /** Builds the title screen; the buttons call the navigator. */
    public MainMenuScreen(Navigator navigator) {
        getStyleClass().addAll("screen", "mm-screen");
        StackPane fanArea = buildFanArea();
        HBox.setHgrow(fanArea, Priority.ALWAYS);
        HBox layout = new HBox(buildMenu(navigator), fanArea);
        Region frame = new Region();
        frame.getStyleClass().add("menu-frame");
        frame.setMouseTransparent(true);
        StackPane footer = buildFooter(navigator);
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);
        getChildren().addAll(layout, frame, footer);
    }

    /** Bottom line: the version and the update button on the left, the full-screen hint in the middle. */
    private StackPane buildFooter(Navigator navigator) {
        Label version = MenuUi.label("Version " + VERSION, "mm-version");
        Button updates = MenuUi.button("Check for Updates", new MenuAction(navigator, MenuAction.Target.RELEASES), "mm-updates");
        HBox versionBar = new HBox(version, updates);
        versionBar.getStyleClass().add("mm-version-bar");
        versionBar.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane.setAlignment(versionBar, Pos.CENTER_LEFT);
        Label hint = MenuUi.label("F11 toggles full screen", "gw-dim", "mm-footer");
        StackPane footer = new StackPane(versionBar, hint);
        footer.getStyleClass().add("mm-footer-bar");
        footer.setMaxHeight(Region.USE_PREF_SIZE);
        return footer;
    }

    private VBox buildMenu(Navigator navigator) {
        Node crown = MenuUi.iconBox(Icons.crown(52), 60);
        Label title = MenuUi.label("GWENT", "gw-title", "mm-title");
        Label subtitle = MenuUi.label("The Witcher 3 card game — you against the computer", "mm-subtitle");
        Region line = MenuUi.separator();
        line.getStyleClass().add("mm-separator");
        Button freePlay = MenuUi.button("Free Play", new MenuAction(navigator, MenuAction.Target.FREE_PLAY), "gw-button-primary", "mm-button");
        freePlay.setDefaultButton(true);
        Button collector = MenuUi.button("Collector Mode", new MenuAction(navigator, MenuAction.Target.COLLECTOR_MODE), "mm-button");
        Button gallery = MenuUi.button("Card Gallery", new MenuAction(navigator, MenuAction.Target.GALLERY), "mm-button");
        Button rules = MenuUi.button("How to Play", new MenuAction(navigator, MenuAction.Target.RULES), "mm-button");
        Button quit = MenuUi.button("Quit", new MenuAction(navigator, MenuAction.Target.QUIT), "mm-button");
        VBox buttons = new VBox(freePlay, collector, gallery, rules, quit);
        buttons.getStyleClass().add("mm-buttons");
        VBox menu = new VBox(crown, title, subtitle, line, buttons);
        menu.getStyleClass().add("mm-menu");
        return menu;
    }

    private StackPane buildFanArea() {
        Region glow = new Region();
        glow.getStyleClass().add("mm-fan-glow");
        glow.setMouseTransparent(true);
        glow.setMaxSize(GLOW_WIDTH, GLOW_HEIGHT);
        return new StackPane(glow, buildFan());
    }

    /** The cards on an arc, each slightly rotated; hovering a card lifts it out of the fan. */
    private Pane buildFan() {
        List<Node> cards = fanCards(CardDatabase.get());
        Pane fan = new Pane();
        fan.setMinSize(FAN_WIDTH, FAN_HEIGHT);
        fan.setPrefSize(FAN_WIDTH, FAN_HEIGHT);
        fan.setMaxSize(FAN_WIDTH, FAN_HEIGHT);
        double width = CardSize.LARGE.width();
        double height = CardSize.LARGE.height();
        double middle = (cards.size() - 1) / 2.0;
        for (int i = 0; i < cards.size(); i++) {
            double angle = (i - middle) * FAN_STEP;
            double radians = Math.toRadians(angle);
            double centerX = FAN_WIDTH / 2 + FAN_RADIUS * Math.sin(radians);
            double centerY = FAN_CENTER_Y + FAN_RADIUS * (1 - Math.cos(radians));
            StackPane lift = new StackPane(cards.get(i));
            lift.getStyleClass().add("mm-fan-lift");
            StackPane slot = new StackPane(lift);
            slot.getStyleClass().add("mm-fan-slot");
            slot.setMinSize(width, height);
            slot.setPrefSize(width, height);
            slot.setMaxSize(width, height);
            slot.setLayoutX(centerX - width / 2);
            slot.setLayoutY(centerY - height / 2);
            slot.setRotate(angle);
            fan.getChildren().add(slot);
        }
        fan.setScaleX(FAN_SCALE);
        fan.setScaleY(FAN_SCALE);
        return fan;
    }

    /** Foltest, the four heroes and Eredin; cards missing from the data are skipped (other heroes fill in). */
    private static List<Node> fanCards(CardDatabase db) {
        List<Node> cards = new ArrayList<>();
        List<CardDef> used = new ArrayList<>();
        LeaderDef foltest = findLeader(db, Faction.NORTHERN_REALMS, "King of Temeria");
        if (foltest != null) {
            cards.add(new LeaderView(foltest, CardSize.LARGE));
        }
        for (String name : FAN_HEROES) {
            CardDef card = findCard(db, name);
            if (card != null) {
                cards.add(new CardView(card, CardSize.LARGE));
                used.add(card);
            }
        }
        LeaderDef eredin = findLeader(db, Faction.MONSTERS, "King of the Wild Hunt");
        if (eredin != null) {
            cards.add(new LeaderView(eredin, CardSize.LARGE));
        }
        for (CardDef card : db.cards()) {
            if (cards.size() >= FAN_MIN_CARDS) {
                break;
            }
            if (card.hero() && !card.token() && !used.contains(card)) {
                cards.add(new CardView(card, CardSize.LARGE));
                used.add(card);
            }
        }
        return cards;
    }

    /** The card with exactly this name, else the first non-token card whose name contains it, else null. */
    private static CardDef findCard(CardDatabase db, String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        CardDef partial = null;
        for (CardDef card : db.cards()) {
            String cardName = card.name().toLowerCase(Locale.ROOT);
            if (cardName.equals(wanted)) {
                return card;
            }
            if (partial == null && !card.token() && cardName.contains(wanted)) {
                partial = card;
            }
        }
        return partial;
    }

    /** The faction's leader whose name contains {@code title}, else its first leader, else null. */
    private static LeaderDef findLeader(CardDatabase db, Faction faction, String title) {
        List<LeaderDef> leaders = db.leaders(faction);
        for (LeaderDef leader : leaders) {
            if (leader.name().toLowerCase(Locale.ROOT).contains(title.toLowerCase(Locale.ROOT))) {
                return leader;
            }
        }
        return leaders.isEmpty() ? null : leaders.get(0);
    }
}
