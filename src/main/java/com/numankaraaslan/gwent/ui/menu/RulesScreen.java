package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderAbility;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.model.SpecialType;
import com.numankaraaslan.gwent.ui.Navigator;
import com.numankaraaslan.gwent.ui.ScrollSupport;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import com.numankaraaslan.gwent.ui.card.Icons;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * "How to play": the rules of The Witcher 3 Gwent on one scrollable page, with a table of contents on the left that
 * jumps to a section and follows the scrolling. Abilities, special cards and factions are listed straight from the
 * model enums with their icons, so the page always matches the game.
 */
public class RulesScreen extends StackPane {

    /** Maximum width of plain paragraphs (keeps lines readable). */
    private static final double TEXT_WIDTH = 1100;
    /** Maximum width of icon entries, callouts and bullet lists. */
    private static final double BLOCK_WIDTH = 1160;
    private static final double ICON_SIZE = 40;
    private static final double ICON_BOX_SIZE = 58;
    /** A section counts as "current" once its top is less than this far below the top of the viewport. */
    private static final double SCROLL_SPY_OFFSET = 140;
    /** Leaders shown as examples in the Leaders section (two active, two passive). */
    private static final LeaderAbility[] EXAMPLE_LEADERS = {LeaderAbility.FOG_FROM_DECK, LeaderAbility.HORN_CLOSE, LeaderAbility.CANCEL_LEADER, LeaderAbility.EXTRA_CARD};

    private final CardDatabase db = CardDatabase.get();
    private final VBox content = new VBox();
    private final ScrollPane scroll = MenuUi.scrollPane(content);
    private final VBox toc = new VBox();
    private final List<VBox> sections = new ArrayList<>();
    private final List<Button> tocEntries = new ArrayList<>();

    /** Builds the rules page. */
    public RulesScreen(Navigator navigator) {
        getStyleClass().addAll("screen", "rules-screen");
        content.getStyleClass().add("rules-content");
        toc.getStyleClass().addAll("gw-panel", "rules-toc");
        toc.getChildren().add(MenuUi.label("Contents", "rules-toc-title"));
        addSection("Goal of the game", goalSection());
        addSection("A round", roundSection());
        addSection("Card advantage", advantageSection());
        addSection("The battlefield", battlefieldSection());
        addSection("Card types", cardTypesSection());
        addSection("Unit abilities", abilitiesSection());
        addSection("Special cards", specialsSection());
        addSection("Factions & perks", factionsSection());
        addSection("Leaders", leadersSection());
        addSection("Starting the game", startSection());
        addSection("This version's options", optionsSection());
        addSection("Controls", controlsSection());
        StackPane frame = new StackPane(scroll);
        frame.getStyleClass().add("rules-frame");
        HBox.setHgrow(frame, Priority.ALWAYS);
        HBox body = new HBox(toc, frame);
        body.getStyleClass().add("rules-body");
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox page = new VBox(buildHeader(navigator), body);
        page.getStyleClass().add("menu-page");
        Region frameLines = new Region();
        frameLines.getStyleClass().add("menu-frame");
        frameLines.setMouseTransparent(true);
        getChildren().addAll(page, frameLines);
        scroll.vvalueProperty().addListener(new ScrollSpy());
        selectToc(0);
    }

    private static HBox buildHeader(Navigator navigator) {
        Label title = MenuUi.label("How to Play", "gw-heading", "menu-page-title");
        Label subtitle = MenuUi.label("The rules of Gwent as played in The Witcher 3 — everything you need for your first game", "menu-page-subtitle");
        HBox header = new HBox(title, subtitle, MenuUi.hSpacer(), MenuUi.backButton(navigator));
        header.getStyleClass().add("menu-header");
        return header;
    }

    /** Adds a section (heading, gold line, body) to the page and an entry to the table of contents. */
    private void addSection(String title, List<Node> body) {
        Region line = MenuUi.separator();
        line.getStyleClass().add("rules-section-line");
        VBox section = new VBox(MenuUi.label(title, "gw-heading", "rules-section-title"), line);
        section.getChildren().addAll(body);
        section.getStyleClass().add("rules-section");
        content.getChildren().add(section);
        sections.add(section);
        Button entry = new Button(title);
        entry.getStyleClass().add("rules-toc-entry");
        entry.setMaxWidth(Double.MAX_VALUE);
        entry.setOnAction(new TocClick(sections.size() - 1));
        tocEntries.add(entry);
        toc.getChildren().add(entry);
    }

    private void selectToc(int index) {
        for (int i = 0; i < tocEntries.size(); i++) {
            tocEntries.get(i).pseudoClassStateChanged(MenuUi.SELECTED, i == index);
        }
    }

    // ------------------------------------------------------------------ sections

    private List<Node> goalSection() {
        List<Node> body = new ArrayList<>();
        body.add(callout(RulesText.NUTSHELL_TITLE, bullet(RulesText.NUTSHELL_1), bullet(RulesText.NUTSHELL_2), bullet(RulesText.NUTSHELL_3), bullet(RulesText.NUTSHELL_4)));
        body.add(paragraph(RulesText.GOAL_1));
        body.add(paragraph(RulesText.GOAL_2));
        HBox gems = new HBox(gemExample(true, RulesText.GEMS_FULL), gemExample(false, RulesText.GEMS_ONE));
        gems.getStyleClass().add("rules-gems");
        body.add(gems);
        body.add(paragraph(RulesText.GOAL_3));
        return body;
    }

    private List<Node> roundSection() {
        List<Node> body = new ArrayList<>();
        body.add(paragraph(RulesText.ROUND_1));
        body.add(bullet(RulesText.ROUND_PLAY));
        body.add(bullet(RulesText.ROUND_LEADER));
        body.add(bullet(RulesText.ROUND_PASS));
        body.add(paragraph(RulesText.ROUND_2));
        body.add(paragraph(RulesText.ROUND_3));
        body.add(paragraph(RulesText.ROUND_4));
        body.add(paragraph(RulesText.ROUND_5));
        return body;
    }

    private List<Node> advantageSection() {
        List<Node> body = new ArrayList<>();
        body.add(paragraph(RulesText.ADVANTAGE_1));
        body.add(paragraph(RulesText.ADVANTAGE_2));
        body.add(paragraph(RulesText.ADVANTAGE_3));
        body.add(paragraph(RulesText.ADVANTAGE_4));
        body.add(entry(Icons.ability(Ability.SPY, ICON_SIZE), Ability.SPY.displayName(), null, RulesText.ADVANTAGE_SPY, null));
        body.add(entry(Icons.ability(Ability.MEDIC, ICON_SIZE), Ability.MEDIC.displayName(), null, RulesText.ADVANTAGE_MEDIC, null));
        body.add(entry(Icons.ability(Ability.MUSTER, ICON_SIZE), Ability.MUSTER.displayName(), null, RulesText.ADVANTAGE_MUSTER, null));
        body.add(entry(Icons.special(SpecialType.DECOY, ICON_SIZE), SpecialType.DECOY.displayName(), null, RulesText.ADVANTAGE_DECOY, null));
        return body;
    }

    private List<Node> battlefieldSection() {
        List<Node> body = new ArrayList<>();
        body.add(paragraph(RulesText.BATTLEFIELD_1));
        body.add(paragraph(RulesText.BATTLEFIELD_2));
        body.add(paragraph(RulesText.BATTLEFIELD_3));
        body.add(paragraph(RulesText.BATTLEFIELD_4));
        body.add(BattlefieldDiagram.create());
        body.add(tip(RulesText.BATTLEFIELD_EXAMPLE));
        return body;
    }

    private List<Node> cardTypesSection() {
        List<Node> body = new ArrayList<>();
        body.add(paragraph(RulesText.TYPES_INTRO));
        body.add(bullet(RulesText.TYPES_UNIT));
        body.add(bullet(RulesText.TYPES_HERO));
        body.add(bullet(RulesText.TYPES_SPECIAL));
        body.add(bullet(RulesText.TYPES_WEATHER));
        HBox examples = new HBox();
        examples.getStyleClass().add("rules-examples");
        addExample(examples, findUnitExample(), "Unit", "strength, row, ability");
        addExample(examples, findHeroExample(), "Hero", "immune to everything");
        addExample(examples, findSpecial(SpecialType.COMMANDERS_HORN), "Special card", "one-time effect");
        addExample(examples, findSpecial(SpecialType.BITING_FROST), "Weather card", "hits both sides");
        if (!examples.getChildren().isEmpty()) {
            body.add(examples);
        }
        return body;
    }

    private List<Node> abilitiesSection() {
        List<Node> body = new ArrayList<>();
        body.add(paragraph(RulesText.ABILITIES_INTRO));
        body.add(entry(Icons.hero(ICON_SIZE), "Hero", null, RulesText.HERO_TEXT, RulesText.HERO_TIP));
        body.add(entry(Icons.agile(ICON_SIZE), "Agile", null, RulesText.AGILE_TEXT, RulesText.AGILE_TIP));
        for (Ability ability : Ability.values()) {
            if (ability != Ability.NONE) {
                body.add(entry(Icons.ability(ability, ICON_SIZE), ability.displayName(), null, ability.description(), RulesText.abilityTip(ability)));
            }
        }
        return body;
    }

    private List<Node> specialsSection() {
        List<Node> body = new ArrayList<>();
        body.add(paragraph(RulesText.SPECIALS_INTRO));
        for (SpecialType type : SpecialType.values()) {
            if (type != SpecialType.NONE && !isWeather(type)) {
                body.add(entry(Icons.special(type, ICON_SIZE), type.displayName(), null, type.description(), RulesText.specialTip(type)));
            }
        }
        body.add(MenuUi.label("Weather cards", "gw-subheading", "rules-subtitle"));
        body.add(paragraph(RulesText.WEATHER_INTRO));
        for (SpecialType type : SpecialType.values()) {
            if (isWeather(type)) {
                body.add(entry(Icons.special(type, ICON_SIZE), type.displayName(), null, type.description(), RulesText.specialTip(type)));
            }
        }
        return body;
    }

    private List<Node> factionsSection() {
        List<Node> body = new ArrayList<>();
        body.add(paragraph(RulesText.FACTIONS_INTRO));
        for (Faction faction : Faction.playable()) {
            body.add(entry(Icons.faction(faction, ICON_SIZE + 4), faction.displayName(), null, "**Perk:** " + faction.abilityDescription(), RulesText.factionTip(faction)));
        }
        body.add(paragraph(RulesText.FACTIONS_NEUTRAL));
        return body;
    }

    private List<Node> leadersSection() {
        List<Node> body = new ArrayList<>();
        body.add(paragraph(RulesText.LEADERS_1));
        body.add(paragraph(RulesText.LEADERS_2));
        body.add(paragraph(RulesText.LEADERS_3));
        List<Node> examples = new ArrayList<>();
        for (LeaderAbility ability : EXAMPLE_LEADERS) {
            LeaderDef leader = findLeader(ability);
            if (leader != null) {
                examples.add(entry(Icons.crown(ICON_SIZE), leader.name(), leader.isPassive() ? "passive" : "once per game", leader.description(), null));
            }
        }
        if (!examples.isEmpty()) {
            body.add(paragraph(RulesText.LEADERS_EXAMPLES));
            body.addAll(examples);
        }
        body.add(paragraph(RulesText.LEADERS_GALLERY));
        return body;
    }

    private List<Node> startSection() {
        List<Node> body = new ArrayList<>();
        body.add(step(1, RulesText.START_DEAL));
        body.add(step(2, RulesText.START_REDRAW));
        body.add(step(3, RulesText.START_FIRST));
        body.add(step(4, RulesText.START_BATTLE));
        body.add(tip(RulesText.START_LEADERS));
        return body;
    }

    private List<Node> optionsSection() {
        List<Node> body = new ArrayList<>();
        body.add(paragraph(RulesText.OPTIONS_1));
        body.add(bullet(RulesText.OPTIONS_HAND));
        body.add(bullet(RulesText.OPTIONS_REDRAWS));
        body.add(bullet(RulesText.OPTIONS_DIFFICULTY));
        body.add(paragraph(RulesText.OPTIONS_2));
        return body;
    }

    private List<Node> controlsSection() {
        VBox table = new VBox();
        table.getStyleClass().add("rules-controls");
        int count = Math.min(RulesText.CONTROL_KEYS.length, RulesText.CONTROL_TEXTS.length);
        for (int i = 0; i < count; i++) {
            StackPane key = new StackPane(MenuUi.label(RulesText.CONTROL_KEYS[i], "rules-key"));
            key.getStyleClass().add("rules-key-box");
            TextFlow text = flow(RulesText.CONTROL_TEXTS[i], "rules-text", "rules-strong");
            HBox.setHgrow(text, Priority.ALWAYS);
            HBox row = new HBox(key, text);
            row.getStyleClass().add("rules-control");
            row.setMaxWidth(BLOCK_WIDTH);
            table.getChildren().add(row);
        }
        List<Node> body = new ArrayList<>();
        body.add(table);
        return body;
    }

    // ------------------------------------------------------------------ building blocks

    /** A paragraph of body text. */
    private static TextFlow paragraph(String markup) {
        TextFlow flow = flow(markup, "rules-text", "rules-strong");
        flow.setMaxWidth(TEXT_WIDTH);
        return flow;
    }

    /** A dim, italic remark. */
    private static TextFlow tip(String markup) {
        TextFlow flow = flow(markup, "rules-tip", "rules-tip-strong");
        flow.setMaxWidth(TEXT_WIDTH);
        return flow;
    }

    /** Text with the parts between two {@code **} in the emphasised style. */
    private static TextFlow flow(String markup, String textStyle, String strongStyle) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("rules-flow");
        String[] parts = markup.split("\\*\\*", -1);
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                Text text = new Text(parts[i]);
                text.getStyleClass().add(i % 2 == 1 ? strongStyle : textStyle);
                flow.getChildren().add(text);
            }
        }
        return flow;
    }

    /** A gold dot followed by a paragraph. */
    private static HBox bullet(String markup) {
        return listItem(MenuUi.label("•", "rules-bullet-dot"), markup, "rules-bullet");
    }

    /** A numbered step. */
    private static HBox step(int number, String markup) {
        return listItem(MenuUi.label(String.valueOf(number), "rules-step-number"), markup, "rules-step");
    }

    private static HBox listItem(Node marker, String markup, String style) {
        TextFlow text = flow(markup, "rules-text", "rules-strong");
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox item = new HBox(marker, text);
        item.getStyleClass().add(style);
        item.setMaxWidth(BLOCK_WIDTH);
        return item;
    }

    /** A highlighted box with a title. */
    private static VBox callout(String title, Node... body) {
        VBox box = new VBox(MenuUi.label(title, "rules-callout-title"));
        box.getChildren().addAll(body);
        box.getStyleClass().add("rules-callout");
        box.setMaxWidth(BLOCK_WIDTH);
        return box;
    }

    /** Icon on the left; title (with an optional tag), text and an optional tip on the right. */
    private static HBox entry(Node icon, String title, String tag, String text, String tipText) {
        HBox head = new HBox(MenuUi.label(title, "rules-entry-title"));
        head.getStyleClass().add("rules-entry-head");
        if (tag != null) {
            head.getChildren().add(MenuUi.label(tag, "rules-tag"));
        }
        VBox texts = new VBox(head, flow(text, "rules-text", "rules-strong"));
        if (tipText != null) {
            texts.getChildren().add(flow(tipText, "rules-tip", "rules-tip-strong"));
        }
        texts.getStyleClass().add("rules-entry-text");
        HBox.setHgrow(texts, Priority.ALWAYS);
        HBox entry = new HBox(MenuUi.iconBox(icon, ICON_BOX_SIZE, "rules-entry-icon"), texts);
        entry.getStyleClass().add("rules-entry");
        entry.setMaxWidth(BLOCK_WIDTH);
        return entry;
    }

    /** Two gems (the second lit or not) with an explanation. */
    private static HBox gemExample(boolean bothLit, String text) {
        HBox example = new HBox(Icons.gem(true, 30), Icons.gem(bothLit, 30), MenuUi.label(text, "rules-gem-text"));
        example.getStyleClass().add("rules-gem-example");
        return example;
    }

    /** Adds an example card with a caption; does nothing if the card is missing from the data. */
    private static void addExample(HBox row, CardDef card, String caption, String detail) {
        if (card == null) {
            return;
        }
        VBox example = new VBox(new CardView(card, CardSize.MEDIUM), MenuUi.label(caption, "rules-example-caption"), MenuUi.label(detail, "rules-example-detail"));
        example.getStyleClass().add("rules-example");
        row.getChildren().add(example);
    }

    // ------------------------------------------------------------------ data lookups

    /** Blue Stripes Commando if present, else the first plain unit with an ability. */
    private CardDef findUnitExample() {
        CardDef card = db.findCard("nr_blue_stripes_commando");
        if (card != null) {
            return card;
        }
        for (CardDef candidate : db.cards()) {
            if (candidate.isUnit() && !candidate.hero() && !candidate.token() && candidate.ability() != Ability.NONE) {
                return candidate;
            }
        }
        return null;
    }

    /** Geralt if present, else the first hero. */
    private CardDef findHeroExample() {
        CardDef card = db.findCard("ne_geralt_of_rivia");
        if (card != null) {
            return card;
        }
        for (CardDef candidate : db.cards()) {
            if (candidate.isUnit() && candidate.hero() && !candidate.token()) {
                return candidate;
            }
        }
        return null;
    }

    private CardDef findSpecial(SpecialType type) {
        for (CardDef card : db.cards()) {
            if (card.special() == type) {
                return card;
            }
        }
        return null;
    }

    private LeaderDef findLeader(LeaderAbility ability) {
        for (LeaderDef leader : db.leaders()) {
            if (leader.ability() == ability) {
                return leader;
            }
        }
        return null;
    }

    private static boolean isWeather(SpecialType type) {
        return type.isBadWeather() || type == SpecialType.CLEAR_WEATHER;
    }

    // ------------------------------------------------------------------ handlers

    /** Jumps to a section. */
    private final class TocClick implements EventHandler<ActionEvent> {

        private final int index;

        TocClick(int index) {
            this.index = index;
        }

        @Override
        public void handle(ActionEvent event) {
            ScrollSupport.scrollTo(scroll, sections.get(index).getBoundsInParent().getMinY() - 12);
            selectToc(index);
        }
    }

    /** Highlights the table-of-contents entry of the section at the top of the viewport. */
    private final class ScrollSpy implements ChangeListener<Number> {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            double top = ScrollSupport.scrollTop(scroll) + SCROLL_SPY_OFFSET;
            int current = 0;
            for (int i = 0; i < sections.size(); i++) {
                if (sections.get(i).getBoundsInParent().getMinY() <= top) {
                    current = i;
                }
            }
            selectToc(current);
        }
    }
}
