package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import com.numankaraaslan.gwent.ui.Navigator;
import com.numankaraaslan.gwent.ui.card.CardInfoPanel;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import com.numankaraaslan.gwent.ui.card.Icons;
import com.numankaraaslan.gwent.ui.card.LeaderView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Card gallery: one tab per faction plus "Neutral & Special". Each tab shows the faction's perk, its leaders and its
 * cards grouped by row (close, ranged, siege, agile), special and weather cards and summoned-only cards, with the
 * number of copies in the original collection under each card. Hovering a card shows it large in the info panel on
 * the right; clicking keeps it there. A tab's content is built the first time the tab is opened.
 */
public class GalleryScreen extends StackPane {

    /** Tab order. */
    private static final Faction[] TABS = {Faction.NORTHERN_REALMS, Faction.NILFGAARD, Faction.SCOIATAEL, Faction.MONSTERS, Faction.SKELLIGE, Faction.NEUTRAL};
    private static final String NEUTRAL_TEXT = "Neutral cards go into every faction's deck: neutral heroes and units plus all special and weather cards.";
    private static final double TAB_ICON_SIZE = 24;
    private static final double SECTION_ICON_SIZE = 30;

    /** The tab shown last, so the gallery reopens where the player left it (for the rest of the session). */
    private static Faction lastTab = Faction.NORTHERN_REALMS;

    private final CardDatabase db = CardDatabase.get();
    private final OptionGroup<Faction> tabs = new OptionGroup<>("gallery-tabs", "gallery-tab");
    private final ScrollPane scroll = MenuUi.scrollPane(new VBox());
    private final Map<Faction, Node> pages = new EnumMap<>(Faction.class);
    private final CardInfoPanel infoPanel = new CardInfoPanel();
    private CardTile pinnedTile;
    private CardDef pinnedCard;
    private LeaderDef pinnedLeader;

    /** Builds the gallery and opens the tab that was shown last. */
    public GalleryScreen(Navigator navigator) {
        getStyleClass().addAll("screen", "gallery-screen");
        for (Faction faction : TABS) {
            tabs.add(faction, tabTitle(faction), MenuUi.iconBox(Icons.faction(faction, TAB_ICON_SIZE), TAB_ICON_SIZE + 4));
        }
        StackPane frame = new StackPane(scroll);
        frame.getStyleClass().add("gallery-frame");
        VBox.setVgrow(frame, Priority.ALWAYS);
        VBox center = new VBox(tabs, frame);
        center.getStyleClass().add("gallery-center");
        HBox.setHgrow(center, Priority.ALWAYS);
        Label hint = MenuUi.wrapping("Hover over a card to see it here; click a card to keep it shown.", "gallery-hint");
        VBox side = new VBox(MenuUi.label("Card details", "gw-subheading"), infoPanel, hint);
        side.getStyleClass().add("gallery-side");
        HBox body = new HBox(center, side);
        body.getStyleClass().add("gallery-body");
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox page = new VBox(buildHeader(navigator), body);
        page.getStyleClass().add("menu-page");
        Region frameLines = new Region();
        frameLines.getStyleClass().add("menu-frame");
        frameLines.setMouseTransparent(true);
        getChildren().addAll(page, frameLines);
        infoPanel.clear();
        tabs.select(lastTab);
        tabs.setOnChange(new TabChange());
        showTab(tabs.getValue());
    }

    private static HBox buildHeader(Navigator navigator) {
        Label title = MenuUi.label("Card Gallery", "gw-heading", "menu-page-title");
        Label subtitle = MenuUi.label("Every leader and card of the original Witcher 3 Gwent", "menu-page-subtitle");
        HBox header = new HBox(title, subtitle, MenuUi.hSpacer(), MenuUi.backButton(navigator));
        header.getStyleClass().add("menu-header");
        return header;
    }

    private static String tabTitle(Faction faction) {
        return faction == Faction.NEUTRAL ? "Neutral & Special" : faction.shortName();
    }

    /** Shows a tab's page, building it on first use. */
    private void showTab(Faction faction) {
        lastTab = faction;
        Node page = pages.get(faction);
        if (page == null) {
            page = buildPage(faction);
            pages.put(faction, page);
        }
        scroll.setContent(page);
        scroll.setVvalue(scroll.getVmin());
    }

    // ------------------------------------------------------------------ page content

    private VBox buildPage(Faction faction) {
        VBox page = new VBox(buildBanner(faction));
        page.getStyleClass().add("gallery-page");
        List<LeaderDef> leaders = db.leaders(faction);
        if (!leaders.isEmpty()) {
            FlowPane grid = grid();
            for (LeaderDef leader : leaders) {
                grid.getChildren().add(leaderTile(leader));
            }
            page.getChildren().add(section(Icons.crown(SECTION_ICON_SIZE), "Leaders", leaders.size(), "Every army has one leader. An active ability can be used once per game (it takes your turn); a passive one is always on.", grid));
        }
        Map<Section, List<CardDef>> groups = new EnumMap<>(Section.class);
        for (CardDef card : db.cards(faction)) {
            Section key = Section.of(card);
            List<CardDef> group = groups.get(key);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(key, group);
            }
            group.add(card);
        }
        for (Section key : Section.values()) {
            List<CardDef> group = groups.get(key);
            if (group == null) {
                continue;
            }
            Collections.sort(group, new UnitOrder());
            FlowPane grid = grid();
            for (CardDef card : group) {
                grid.getChildren().add(cardTile(card));
            }
            page.getChildren().add(section(key.icon(SECTION_ICON_SIZE), key.title(), group.size(), key.description(), grid));
        }
        return page;
    }

    /** Emblem, faction name, perk and a few numbers about the faction's cards. */
    private HBox buildBanner(Faction faction) {
        String title = faction == Faction.NEUTRAL ? "Neutral & Special cards" : faction.displayName();
        String text = faction == Faction.NEUTRAL ? NEUTRAL_TEXT : "Faction perk: " + faction.abilityDescription();
        VBox texts = new VBox(MenuUi.label(title, "gallery-banner-title"), MenuUi.wrapping(text, "gallery-banner-text"), MenuUi.wrapping(stats(faction), "gallery-stats"));
        texts.getStyleClass().add("gallery-banner-texts");
        HBox.setHgrow(texts, Priority.ALWAYS);
        HBox banner = new HBox(MenuUi.iconBox(Icons.faction(faction, 72), 80), texts);
        banner.getStyleClass().add("gallery-banner");
        return banner;
    }

    private String stats(Faction faction) {
        int different = 0;
        int total = 0;
        for (CardDef card : db.cards(faction)) {
            if (!card.token()) {
                different++;
                total += card.copies();
            }
        }
        String leaders = faction == Faction.NEUTRAL ? "" : db.leaders(faction).size() + " leaders  •  ";
        return leaders + different + " different cards, " + total + " in the original collection  •  \"x2\" under a card = copies in the original collection";
    }

    private static VBox section(Node icon, String title, int count, String description, FlowPane grid) {
        HBox head = new HBox(MenuUi.iconBox(icon, SECTION_ICON_SIZE + 4), MenuUi.label(title, "gw-subheading"), MenuUi.label("(" + count + ")", "gallery-section-count"));
        head.getStyleClass().add("gallery-section-head");
        VBox section = new VBox(head, MenuUi.wrapping(description, "gallery-section-text"), grid);
        section.getStyleClass().add("gallery-section");
        return section;
    }

    private static FlowPane grid() {
        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("gallery-grid");
        return grid;
    }

    private CardTile leaderTile(LeaderDef leader) {
        CardTile tile = new CardTile(new LeaderView(leader, CardSize.MEDIUM), CardSize.MEDIUM.width(), MenuUi.leaderTitle(leader), MenuUi.leaderKind(leader));
        tile.setOnMouseEntered(new Hover(null, leader));
        tile.setOnMouseExited(new HoverEnd());
        tile.setOnMouseClicked(new Pin(tile, null, leader));
        return tile;
    }

    /** Card with its caption: "Hero", "summoned only" or the number of copies ("x3"). */
    private CardTile cardTile(CardDef card) {
        String caption = "x" + card.copies();
        String style = "menu-caption-copies";
        if (card.token()) {
            caption = "summoned only";
            style = "menu-caption-token";
        } else if (card.hero()) {
            caption = "Hero";
            style = "menu-caption-hero";
        }
        CardTile tile = new CardTile(new CardView(card, CardSize.MEDIUM), CardSize.MEDIUM.width(), caption, null, style);
        tile.setOnMouseEntered(new Hover(card, null));
        tile.setOnMouseExited(new HoverEnd());
        tile.setOnMouseClicked(new Pin(tile, card, null));
        return tile;
    }

    /** Shows a card or a leader in the info panel (both null: empty panel). */
    private void show(CardDef card, LeaderDef leader) {
        if (card != null) {
            infoPanel.showCard(card);
        } else if (leader != null) {
            infoPanel.showLeader(leader, false, false);
        } else {
            infoPanel.clear();
        }
    }

    // ------------------------------------------------------------------ grouping

    /** The groups a tab's cards are shown in, in this order. */
    private enum Section {
        CLOSE("Close Combat units", "Sword icon: these units fight in the Close Combat row."),
        RANGED("Ranged Combat units", "Bow icon: these units go to the Ranged Combat row."),
        SIEGE("Siege Combat units", "Catapult icon: these units go to the Siege Combat row."),
        AGILE("Agile units", "These units can be played to either the Close Combat or the Ranged Combat row."),
        SPECIAL("Special cards", "One-time effects with no strength of their own."),
        WEATHER("Weather cards", "Weaken a row on both sides of the battlefield until Clear Weather is played or the round ends."),
        SUMMONED("Summoned only", "Never in your deck: these appear through a Berserker transformation or a Summon Avenger ability.");

        private final String title;
        private final String description;

        Section(String title, String description) {
            this.title = title;
            this.description = description;
        }

        String title() {
            return title;
        }

        String description() {
            return description;
        }

        static Section of(CardDef card) {
            if (card.token()) {
                return SUMMONED;
            }
            if (card.isWeather()) {
                return WEATHER;
            }
            if (card.isSpecialCard()) {
                return SPECIAL;
            }
            if (card.isAgile()) {
                return AGILE;
            }
            if (card.defaultRow() == RowType.RANGED) {
                return RANGED;
            }
            if (card.defaultRow() == RowType.SIEGE) {
                return SIEGE;
            }
            return CLOSE;
        }

        Node icon(double size) {
            switch (this) {
                case CLOSE:
                    return Icons.row(RowType.CLOSE, size);
                case RANGED:
                    return Icons.row(RowType.RANGED, size);
                case SIEGE:
                    return Icons.row(RowType.SIEGE, size);
                case AGILE:
                    return Icons.agile(size);
                case SPECIAL:
                    return Icons.special(SpecialType.COMMANDERS_HORN, size);
                case WEATHER:
                    return Icons.special(SpecialType.BITING_FROST, size);
                default:
                    return Icons.ability(Ability.AVENGER, size);
            }
        }
    }

    /** Units: heroes first, then by strength (strongest first), then by name. Other cards keep the file order. */
    private static final class UnitOrder implements Comparator<CardDef> {
        @Override
        public int compare(CardDef a, CardDef b) {
            if (!a.isUnit() || !b.isUnit()) {
                return 0;
            }
            if (a.hero() != b.hero()) {
                return a.hero() ? -1 : 1;
            }
            if (a.strength() != b.strength()) {
                return Integer.compare(b.strength(), a.strength());
            }
            return a.name().compareTo(b.name());
        }
    }

    // ------------------------------------------------------------------ handlers

    private final class TabChange implements Runnable {
        @Override
        public void run() {
            showTab(tabs.getValue());
        }
    }

    private final class Hover implements EventHandler<MouseEvent> {

        private final CardDef card;
        private final LeaderDef leader;

        Hover(CardDef card, LeaderDef leader) {
            this.card = card;
            this.leader = leader;
        }

        @Override
        public void handle(MouseEvent event) {
            show(card, leader);
        }
    }

    /** Leaving a card shows the clicked (pinned) card again. */
    private final class HoverEnd implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            show(pinnedCard, pinnedLeader);
        }
    }

    /** Clicking a card keeps it in the info panel and marks its tile. */
    private final class Pin implements EventHandler<MouseEvent> {

        private final CardTile tile;
        private final CardDef card;
        private final LeaderDef leader;

        Pin(CardTile tile, CardDef card, LeaderDef leader) {
            this.tile = tile;
            this.card = card;
            this.leader = leader;
        }

        @Override
        public void handle(MouseEvent event) {
            if (pinnedTile != null) {
                pinnedTile.setSelected(false);
            }
            pinnedTile = tile;
            pinnedCard = card;
            pinnedLeader = leader;
            tile.setSelected(true);
            show(card, leader);
        }
    }
}
