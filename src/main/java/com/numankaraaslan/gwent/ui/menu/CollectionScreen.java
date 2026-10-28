package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.collector.CardCollection;
import com.numankaraaslan.gwent.collector.CollectionFile;
import com.numankaraaslan.gwent.collector.Reward;
import com.numankaraaslan.gwent.collector.Tier;
import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderAbility;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.ui.Navigator;
import com.numankaraaslan.gwent.ui.card.CardInfoPanel;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import com.numankaraaslan.gwent.ui.card.Icons;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
 * My Collection (Collector Mode), laid out like the card gallery: one tab per faction plus "Neutral & Special". A tab
 * shows the faction's cards grouped by how they are won: the starter cards, then the Easy, Normal and Hard rewards.
 * Cards you don't own yet are greyed out, and notes on how unlocking works end every tab. Under the tabs: how many cards
 * each difficulty has given so far, and "Reset progress" (it asks first). Hovering a card shows it large on the right;
 * clicking keeps it there. Back leads to the Collector Mode setup screen.
 */
public class CollectionScreen extends StackPane {

    /** Tab order. */
    private static final Faction[] TABS = {Faction.NORTHERN_REALMS, Faction.NILFGAARD, Faction.SCOIATAEL, Faction.MONSTERS, Faction.SKELLIGE, Faction.NEUTRAL};
    /** The tiers whose cards are won, in difficulty order. */
    private static final Tier[] REWARD_TIERS = {Tier.EASY, Tier.NORMAL, Tier.HARD};
    private static final double TAB_ICON_SIZE = 24;
    private static final double SECTION_ICON_SIZE = 30;

    /** The tab shown last, so the screen reopens where the player left it (for the rest of the session). */
    private static Faction lastTab = Faction.NORTHERN_REALMS;

    private final Navigator navigator;
    private final CardDatabase db = CardDatabase.get();
    private final OptionGroup<Faction> tabs = new OptionGroup<>("gallery-tabs", "gallery-tab");
    private final ScrollPane scroll = MenuUi.scrollPane(new VBox());
    private final Map<Faction, Node> pages = new EnumMap<>(Faction.class);
    private final CardInfoPanel infoPanel = new CardInfoPanel();
    /** Right end of the progress line: "Reset progress", or the question that confirms it. */
    private final HBox resetBox = new HBox();
    private final CardCollection collection;
    private CardTile pinnedTile;
    private CardDef pinnedCard;

    /** Reads the collection and builds the screen, opening the tab that was shown last. */
    public CollectionScreen(Navigator navigator) {
        this.navigator = navigator;
        collection = CollectionFile.load(db);
        getStyleClass().addAll("screen", "collection-screen");
        for (Faction faction : TABS) {
            tabs.add(faction, tabTitle(faction), MenuUi.iconBox(Icons.faction(faction, TAB_ICON_SIZE), TAB_ICON_SIZE + 4));
        }
        StackPane frame = new StackPane(scroll);
        frame.getStyleClass().add("gallery-frame");
        VBox.setVgrow(frame, Priority.ALWAYS);
        VBox center = new VBox(tabs, frame, buildProgress());
        center.getStyleClass().add("gallery-center");
        HBox.setHgrow(center, Priority.ALWAYS);
        Label hint = MenuUi.wrapping("Hover over a card to see it here; click a card to keep it shown.", "gallery-hint");
        VBox side = new VBox(MenuUi.label("Card details", "gw-subheading"), infoPanel, hint);
        side.getStyleClass().add("gallery-side");
        HBox body = new HBox(center, side);
        body.getStyleClass().add("gallery-body");
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox page = new VBox(buildHeader(), body);
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

    private HBox buildHeader() {
        Label title = MenuUi.label("My Collection", "gw-heading", "menu-page-title");
        Label subtitle = MenuUi.label("The cards you play with in Collector Mode — and so does the computer", "menu-page-subtitle");
        HBox header = new HBox(title, subtitle, MenuUi.hSpacer(), MenuUi.backButton(navigator, MenuAction.Target.COLLECTOR_MODE));
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

    // ------------------------------------------------------------------ progress and reset

    /** "Cards won: Easy 3/47, Normal 0/26, Hard 0/24" and "Reset progress". */
    private HBox buildProgress() {
        HBox progress = new HBox(MenuUi.label("Cards won", "collection-progress-title"));
        progress.getStyleClass().add("collection-progress");
        for (Tier tier : REWARD_TIERS) {
            int total = collection.rewardCount(tier);
            int won = total - collection.remaining(tier).size();
            progress.getChildren().add(MenuUi.label(tier.displayName() + "  " + won + "/" + total, "collection-chip"));
        }
        resetBox.getStyleClass().add("collection-reset");
        showResetButton();
        progress.getChildren().addAll(MenuUi.hSpacer(), resetBox);
        return progress;
    }

    /** How many cards (Muster families count once) all difficulties have given so far. */
    private int wonCount() {
        int won = 0;
        for (Tier tier : REWARD_TIERS) {
            won += collection.rewardCount(tier) - collection.remaining(tier).size();
        }
        return won;
    }

    /** "Reset progress": usable once something was won. */
    private void showResetButton() {
        Button reset = MenuUi.button("Reset progress", new ResetAsk(), "collection-button");
        reset.setDisable(wonCount() == 0);
        resetBox.getChildren().setAll(reset);
    }

    /** Asks before anything is removed. */
    private void showResetQuestion() {
        int won = wonCount();
        String question = "Remove the " + won + " cards you have won and start over?";
        if (won == 1) {
            question = "Remove the card you have won and start over?";
        }
        Button yes = MenuUi.button("Reset", new ResetConfirmed(), "collection-button");
        Button no = MenuUi.button("Cancel", new ResetCancelled(), "collection-button");
        resetBox.getChildren().setAll(MenuUi.label(question, "collection-reset-text"), yes, no);
    }

    /** Empties the save file and reopens the screen with the starter cards only. */
    private void reset() {
        CollectionFile.save(CardCollection.of(db, Collections.emptySet()));
        navigator.showCollection();
    }

    // ------------------------------------------------------------------ page content

    /** The banner, one section per tier with the faction's cards, and the notes. */
    private VBox buildPage(Faction faction) {
        VBox page = new VBox(buildBanner(faction));
        page.getStyleClass().add("gallery-page");
        Map<Tier, List<CardDef>> groups = new EnumMap<>(Tier.class);
        for (CardDef card : db.cards(faction)) {
            Tier tier = collection.tierOf(card);
            if (tier == null) {
                continue;
            }
            List<CardDef> group = groups.get(tier);
            if (group == null) {
                group = new ArrayList<>();
                groups.put(tier, group);
            }
            group.add(card);
        }
        for (Tier tier : Tier.values()) {
            List<CardDef> group = groups.get(tier);
            if (group == null) {
                continue;
            }
            Collections.sort(group, new CardOrder());
            FlowPane grid = new FlowPane();
            grid.getStyleClass().add("gallery-grid");
            int owned = 0;
            for (CardDef card : group) {
                if (collection.owns(card)) {
                    owned++;
                }
                grid.getChildren().add(cardTile(card));
            }
            page.getChildren().add(section(tier, owned, group.size(), grid));
        }
        page.getChildren().add(buildNotes());
        return page;
    }

    /** Emblem, faction name, what the cards are for and how many of them you own. */
    private HBox buildBanner(Faction faction) {
        String title = faction == Faction.NEUTRAL ? "Neutral & Special cards" : faction.displayName();
        String text = faction == Faction.NEUTRAL ? "Neutral cards go into every deck, yours and the computer's, whatever the faction." : "These cards and your neutral cards make up your " + faction.shortName() + " deck. Every leader is yours from the start.";
        int owned = 0;
        int total = 0;
        for (CardDef card : db.cards(faction)) {
            if (!card.token()) {
                total++;
                if (collection.owns(card)) {
                    owned++;
                }
            }
        }
        VBox texts = new VBox(MenuUi.label(title, "gallery-banner-title"), MenuUi.wrapping(text, "gallery-banner-text"), MenuUi.wrapping("You own " + owned + " of " + total + " different cards.", "gallery-stats"));
        texts.getStyleClass().add("gallery-banner-texts");
        HBox.setHgrow(texts, Priority.ALWAYS);
        HBox banner = new HBox(MenuUi.iconBox(Icons.faction(faction, 72), 80), texts);
        banner.getStyleClass().add("gallery-banner");
        return banner;
    }

    /** Tier icon, title, "owned/total", which cards the tier holds, and the cards. */
    private static VBox section(Tier tier, int owned, int total, FlowPane grid) {
        String title = tier == Tier.STARTER ? "Starter cards" : tier.displayName() + " rewards";
        HBox head = new HBox(MenuUi.iconBox(tierIcon(tier), SECTION_ICON_SIZE + 4), MenuUi.label(title, "gw-subheading"), MenuUi.label("(" + owned + "/" + total + ")", "gallery-section-count"));
        head.getStyleClass().add("gallery-section-head");
        VBox section = new VBox(head, MenuUi.wrapping(tier.description(), "gallery-section-text"), grid);
        section.getStyleClass().add("gallery-section");
        return section;
    }

    private static Node tierIcon(Tier tier) {
        switch (tier) {
            case STARTER:
                return Icons.cards(SECTION_ICON_SIZE);
            case HARD:
                return Icons.hero(SECTION_ICON_SIZE);
            default:
                return Icons.laurel(SECTION_ICON_SIZE);
        }
    }

    /** Card with its caption: the number of copies ("x3") or "Hero" once you own it, "Locked" (greyed out) before. */
    private CardTile cardTile(CardDef card) {
        CardView view = new CardView(card, CardSize.MEDIUM);
        String caption = "x" + card.copies();
        String style = "menu-caption-copies";
        if (!collection.owns(card)) {
            view.setDimmed(true);
            caption = "Locked";
            style = "menu-caption-locked";
        } else if (card.hero()) {
            caption = "Hero";
            style = "menu-caption-hero";
        }
        CardTile tile = new CardTile(view, CardSize.MEDIUM.width(), caption, null, style);
        tile.setOnMouseEntered(new Hover(card));
        tile.setOnMouseExited(new HoverEnd());
        tile.setOnMouseClicked(new Pin(tile, card));
        return tile;
    }

    /** How unlocking works: copies, Muster families, weather, the two leaders that need won cards, the computer, the file. */
    private VBox buildNotes() {
        VBox box = new VBox(MenuUi.label("Good to know", "rules-callout-title"));
        box.getStyleClass().add("rules-callout");
        box.getChildren().add(note("Every card you win comes with all its copies: a card that has three copies puts all three into your deck."));
        List<String> families = new ArrayList<>();
        for (Reward reward : collection.rewards()) {
            if (reward.cards().size() > 1) {
                families.add(reward.describe() + " (" + reward.tier().displayName() + ")");
            }
        }
        if (!families.isEmpty()) {
            box.getChildren().add(note("Muster families whose cards have different names are won together, in a single win, so their Muster works right away: " + String.join("; ", families) + "."));
        }
        box.getChildren().add(note("Weather cards are starter cards, so the leaders who play weather from the deck work from your first game."));
        LeaderDef spyLeader = findLeader(LeaderAbility.DOUBLE_SPIES);
        LeaderDef medicLeader = findLeader(LeaderAbility.RANDOM_MEDIC);
        if (spyLeader != null && medicLeader != null) {
            box.getChildren().add(note("Two passive leaders need cards that are won on Normal or Hard: " + spyLeader.name() + " doubles the strength of spies, and " + medicLeader.name() + " makes Medics revive a random unit. Until spies or Medics are won, these two do nothing."));
        }
        box.getChildren().add(note("The computer's deck is built from the cards you own too, so its decks grow with yours."));
        box.getChildren().add(note("Your collection is saved in " + CollectionFile.path() + ", one card id per line. Deleting that file starts over, just like Reset progress."));
        return box;
    }

    /** A gold dot and a line of text. */
    private static HBox note(String text) {
        Label label = MenuUi.wrapping(text, "collection-note");
        HBox.setHgrow(label, Priority.ALWAYS);
        HBox line = new HBox(MenuUi.label("•", "rules-bullet-dot"), label);
        line.getStyleClass().add("rules-bullet");
        return line;
    }

    private LeaderDef findLeader(LeaderAbility ability) {
        for (LeaderDef leader : db.leaders()) {
            if (leader.ability() == ability) {
                return leader;
            }
        }
        return null;
    }

    /** Shows a card in the info panel (null: empty panel). */
    private void show(CardDef card) {
        if (card != null) {
            infoPanel.showCard(card);
        } else {
            infoPanel.clear();
        }
    }

    /** Units first (heroes, then the strongest, then by name), then special and weather cards in file order. */
    private static final class CardOrder implements Comparator<CardDef> {
        @Override
        public int compare(CardDef a, CardDef b) {
            if (a.isUnit() != b.isUnit()) {
                return a.isUnit() ? -1 : 1;
            }
            if (!a.isUnit()) {
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

        Hover(CardDef card) {
            this.card = card;
        }

        @Override
        public void handle(MouseEvent event) {
            show(card);
        }
    }

    /** Leaving a card shows the clicked (pinned) card again. */
    private final class HoverEnd implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            show(pinnedCard);
        }
    }

    /** Clicking a card keeps it in the info panel and marks its tile. */
    private final class Pin implements EventHandler<MouseEvent> {

        private final CardTile tile;
        private final CardDef card;

        Pin(CardTile tile, CardDef card) {
            this.tile = tile;
            this.card = card;
        }

        @Override
        public void handle(MouseEvent event) {
            if (pinnedTile != null) {
                pinnedTile.setSelected(false);
            }
            pinnedTile = tile;
            pinnedCard = card;
            tile.setSelected(true);
            show(card);
        }
    }

    private final class ResetAsk implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            showResetQuestion();
        }
    }

    private final class ResetConfirmed implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            reset();
        }
    }

    private final class ResetCancelled implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            showResetButton();
        }
    }
}
