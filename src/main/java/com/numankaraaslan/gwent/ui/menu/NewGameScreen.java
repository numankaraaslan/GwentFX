package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Difficulty;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.GameSettings;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.ui.Navigator;
import com.numankaraaslan.gwent.ui.card.CardBackView;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.Icons;
import com.numankaraaslan.gwent.ui.card.LeaderView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.util.Callback;

/**
 * Game setup on a single page. "Your faction" and "Your leader" are two equally wide drop-down lists, each with a big
 * card tile under it (the faction's card back with its perk, the leader card with how its ability is used). Below them
 * the computer opponent with its difficulty and the rule options (starting hand, redraws). The right column explains the
 * leader: the chosen leader's ability, the one hovered in the leader list, or every possible leader for "Random"; it
 * scrolls when the text is long. "Start game" hands the resulting {@link GameSettings} to the navigator. The choices are
 * remembered for the rest of the session, so "New Game" reopens with them.
 */
public class NewGameScreen extends StackPane {

    private static final int[] HAND_SIZES = {10, 11, 12, 13, 14, 15};
    private static final int[] REDRAW_COUNTS = {0, 1, 2, 3, 4, 5};
    /** Size of the faction and leader cards under the drop-down lists. */
    private static final CardSize PICK_CARD = CardSize.LARGE;
    private static final double COMBO_ICON_SIZE = 28;
    private static final String SEPARATOR = "   •   ";
    private static final String DOT = " · ";
    private static final String DECK_EXPLANATION = "Your deck holds every card of your faction plus all neutral cards, each as many times as it exists in the original game. Your hand is dealt from it at random.";
    private static final String ACTIVE_HINT = "During the battle, click your leader card to use the ability. It takes your turn, just like playing a card.";
    private static final String PASSIVE_HINT = "Works on its own for the whole game. There is nothing to click.";
    private static final String RANDOM_HINT = "One of the leaders listed on the right is picked when the game starts.";

    /** The choices of the last visit; "New Game" reopens with them for the rest of the session. */
    private static GameSettings remembered = GameSettings.defaults(Faction.NORTHERN_REALMS);

    private final Navigator navigator;
    private final CardDatabase db = CardDatabase.get();
    private final ComboBox<Choice<Faction>> factionBox = new ComboBox<>();
    private final ComboBox<Choice<LeaderDef>> leaderBox = new ComboBox<>();
    private final VBox factionTile = new VBox();
    private final VBox leaderTile = new VBox();
    private final StackPane factionCard = cardHolder();
    private final StackPane leaderCard = cardHolder();
    private final Label factionName = MenuUi.wrapping("", "ng-pick-name");
    private final Label factionPerk = MenuUi.wrapping("", "ng-pick-text");
    private final Label factionDeck = MenuUi.wrapping("", "ng-pick-detail");
    private final Label leaderTitle = MenuUi.wrapping("", "ng-pick-name");
    private final Label leaderKind = MenuUi.wrapping("", "ng-pick-caption");
    private final Label leaderHint = MenuUi.wrapping("", "ng-pick-detail");
    private final LeaderDetails details = new LeaderDetails();
    private final ScrollPane detailsScroll = MenuUi.scrollPane(details);
    private final ComboBox<Choice<Faction>> aiFactionBox = new ComboBox<>();
    private final ComboBox<Choice<LeaderDef>> aiLeaderBox = new ComboBox<>();
    private final Label aiInfo = MenuUi.wrapping("", "ng-note");
    private final OptionGroup<Integer> handSizes = new OptionGroup<>("menu-options", "menu-option");
    private final OptionGroup<Integer> redraws = new OptionGroup<>("menu-options", "menu-option");
    private final OptionGroup<Difficulty> difficulties = new OptionGroup<>("menu-options", "menu-option");
    private final Label summary = MenuUi.wrapping("", "ng-summary");

    private Faction playerFaction;
    private LeaderDef playerLeader;
    /** True while the controls are being filled from the remembered settings (suppresses refreshes). */
    private boolean restoring;
    /** True while code changes the leader list or its selection (its listener must not react). */
    private boolean updatingLeaders;

    /** Builds the setup page, pre-filled with the choices of the last visit (or the original-game defaults). */
    public NewGameScreen(Navigator navigator) {
        this.navigator = navigator;
        getStyleClass().addAll("screen", "ng-screen");
        fillOptions();
        VBox main = new VBox(buildPickRow(), buildSettingsRow(), MenuUi.vSpacer(), buildButtonBar());
        main.getStyleClass().add("ng-main");
        HBox.setHgrow(main, Priority.ALWAYS);
        HBox body = new HBox(main, buildSideColumn());
        body.getStyleClass().add("ng-body");
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox page = new VBox(buildHeader(), body);
        page.getStyleClass().add("menu-page");
        Region frame = new Region();
        frame.getStyleClass().add("menu-frame");
        frame.setMouseTransparent(true);
        getChildren().addAll(page, frame);
        restore(remembered);
    }

    // ------------------------------------------------------------------ layout

    private HBox buildHeader() {
        Label title = MenuUi.label("New Game", "gw-heading", "menu-page-title");
        Label subtitle = MenuUi.label("Choose your faction and leader, your opponent and the rules — then start the battle.", "menu-page-subtitle");
        HBox header = new HBox(title, subtitle);
        header.getStyleClass().add("menu-header");
        return header;
    }

    /** "Your faction" and "Your leader": two equally wide groups, each a drop-down list with a big card tile under it. */
    private HBox buildPickRow() {
        for (Faction faction : Faction.playable()) {
            factionBox.getItems().add(new Choice<Faction>(faction, faction.displayName()));
        }
        setUpFactionBox(factionBox, "ng-pick-combo");
        setUpLeaderBox(leaderBox, "ng-pick-combo", true);
        factionBox.valueProperty().addListener(new FactionListener());
        leaderBox.valueProperty().addListener(new LeaderListener());
        leaderBox.setOnHidden(new LeaderListClosed());
        VBox factionTexts = new VBox(factionName, MenuUi.label("Faction perk", "ng-pick-caption"), factionPerk, factionDeck);
        VBox leaderTexts = new VBox(leaderTitle, leaderKind, leaderHint);
        buildTile(factionTile, factionCard, factionTexts, factionBox);
        buildTile(leaderTile, leaderCard, leaderTexts, leaderBox);
        HBox row = new HBox(pickGroup("Your faction", factionBox, factionTile), pickGroup("Your leader", leaderBox, leaderTile));
        row.getStyleClass().add("ng-picks");
        return row;
    }

    /** Heading, drop-down list and card tile; both groups of the row start equally wide and share the rest equally. */
    private static VBox pickGroup(String title, ComboBox<?> box, VBox tile) {
        VBox group = new VBox(MenuUi.label(title, "gw-subheading"), box, tile);
        group.getStyleClass().add("ng-pick-group");
        HBox.setHgrow(group, Priority.ALWAYS);
        return group;
    }

    /** A big card tile: a faction-coloured band on top, the card on the left and its texts beside it. A click opens the list above it. */
    private static void buildTile(VBox tile, StackPane card, VBox texts, ComboBox<?> box) {
        Region band = new Region();
        band.getStyleClass().add("menu-faction-band");
        texts.getStyleClass().add("ng-pick-texts");
        HBox.setHgrow(texts, Priority.ALWAYS);
        HBox content = new HBox(card, texts);
        content.getStyleClass().add("ng-pick-content");
        tile.getChildren().setAll(band, content);
        tile.getStyleClass().add("ng-pick-tile");
        tile.setOnMouseClicked(new OpenList(box));
    }

    /** A fixed-size box for one card of {@link #PICK_CARD} size. */
    private static StackPane cardHolder() {
        StackPane holder = new StackPane();
        holder.setMinSize(PICK_CARD.width(), PICK_CARD.height());
        holder.setPrefSize(PICK_CARD.width(), PICK_CARD.height());
        holder.setMaxSize(PICK_CARD.width(), PICK_CARD.height());
        return holder;
    }

    /** A faction list: every entry shows the faction emblem (a question mark for "Random") and the name. */
    private static void setUpFactionBox(ComboBox<Choice<Faction>> box, String styleClass) {
        box.getStyleClass().add(styleClass);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setCellFactory(new FactionCells());
        box.setButtonCell(new FactionCell());
        box.setOnShowing(new ScaleList(box));
    }

    /** A leader list: every entry shows a crown (a question mark for "Random") and the name; {@code preview}: hovering an entry shows it on the right. */
    private void setUpLeaderBox(ComboBox<Choice<LeaderDef>> box, String styleClass, boolean preview) {
        box.getStyleClass().add(styleClass);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setCellFactory(new LeaderCells(preview));
        box.setButtonCell(new LeaderCell(false));
        box.setOnShowing(new ScaleList(box));
    }

    private HBox buildSettingsRow() {
        HBox row = new HBox(buildOpponentPanel(), buildRulesPanel());
        row.getStyleClass().add("ng-settings");
        return row;
    }

    private VBox buildOpponentPanel() {
        aiFactionBox.getItems().add(new Choice<Faction>(null, "Random"));
        for (Faction faction : Faction.playable()) {
            aiFactionBox.getItems().add(new Choice<Faction>(faction, faction.displayName()));
        }
        setUpFactionBox(aiFactionBox, "ng-combo");
        setUpLeaderBox(aiLeaderBox, "ng-combo", false);
        aiFactionBox.getSelectionModel().selectFirst();
        fillAiLeaders(null);
        aiFactionBox.valueProperty().addListener(new AiFactionListener());
        aiLeaderBox.valueProperty().addListener(new AiLeaderListener());
        GridPane grid = new GridPane();
        grid.getStyleClass().add("ng-grid");
        grid.addRow(0, fieldLabel("Faction"), aiFactionBox);
        grid.addRow(1, fieldLabel("Leader"), aiLeaderBox);
        grid.addRow(2, fieldLabel("Difficulty"), difficulties);
        GridPane.setHgrow(aiFactionBox, Priority.ALWAYS);
        GridPane.setHgrow(aiLeaderBox, Priority.ALWAYS);
        return settingsPanel("Opponent", grid, aiInfo);
    }

    private VBox buildRulesPanel() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("ng-grid");
        grid.addRow(0, fieldLabel("Starting hand"), handSizes);
        grid.addRow(1, fieldLabel("Redraws"), redraws);
        return settingsPanel("Rules", grid, MenuUi.wrapping(DECK_EXPLANATION, "ng-note"));
    }

    /** Heading, option grid and a note; the opponent and rules panels start equally wide and share the rest equally. */
    private static VBox settingsPanel(String title, GridPane grid, Label note) {
        VBox panel = new VBox(MenuUi.label(title, "gw-subheading"), grid, note);
        panel.getStyleClass().addAll("gw-panel", "ng-panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private static Label fieldLabel(String text) {
        Label label = MenuUi.label(text, "ng-field-label");
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    private HBox buildButtonBar() {
        Button back = MenuUi.backButton(navigator);
        Button start = MenuUi.button("Start game", new StartAction(), "gw-button-primary", "ng-start");
        start.setDefaultButton(true);
        summary.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(summary, Priority.ALWAYS);
        HBox bar = new HBox(back, summary, start);
        bar.getStyleClass().add("ng-bar");
        return bar;
    }

    /** Right column: the leader explanations, scrolling when they are longer than the column. */
    private VBox buildSideColumn() {
        detailsScroll.setFocusTraversable(false);
        VBox.setVgrow(detailsScroll, Priority.ALWAYS);
        VBox side = new VBox(MenuUi.label("Leader details", "gw-subheading"), detailsScroll);
        side.getStyleClass().add("ng-side");
        return side;
    }

    private void fillOptions() {
        for (int size : HAND_SIZES) {
            handSizes.add(size, size == GameSettings.DEFAULT_HAND_SIZE ? size + " (original)" : String.valueOf(size), null);
        }
        for (int count : REDRAW_COUNTS) {
            redraws.add(count, count == GameSettings.DEFAULT_REDRAWS ? count + " (original)" : String.valueOf(count), null);
        }
        for (Difficulty difficulty : Difficulty.values()) {
            difficulties.add(difficulty, difficulty.displayName(), null);
        }
        OptionChanged listener = new OptionChanged();
        handSizes.setOnChange(listener);
        redraws.setOnChange(listener);
        difficulties.setOnChange(listener);
    }

    // ------------------------------------------------------------------ faction and leader tiles

    /** Shows the faction in its tile: card back, name, perk and the size of the deck. */
    private void showFaction(Faction faction) {
        setFactionStyle(factionTile, faction);
        factionCard.getChildren().setAll(new CardBackView(faction, PICK_CARD));
        factionName.setText(faction.displayName());
        factionPerk.setText(faction.abilityDescription());
        factionDeck.setText("Deck: " + deckCount(faction) + " faction cards + " + deckCount(Faction.NEUTRAL) + " neutral cards");
    }

    /** Cards of {@code faction} in a deck: every copy counts, summoned tokens don't. */
    private int deckCount(Faction faction) {
        int count = 0;
        for (CardDef card : db.cards(faction)) {
            if (!card.token()) {
                count += card.copies();
            }
        }
        return count;
    }

    /** Shows the leader card (a card back with a question mark for "Random", null) and how its ability is used. */
    private void showLeaderCard(LeaderDef leader) {
        setFactionStyle(leaderTile, playerFaction);
        if (leader == null) {
            leaderCard.getChildren().setAll(buildRandomCard(playerFaction));
            leaderTitle.setText("Random leader");
            leaderKind.setText("Picked when the game starts");
            leaderHint.setText(RANDOM_HINT);
            return;
        }
        String character = MenuUi.leaderCharacter(leader);
        leaderCard.getChildren().setAll(new LeaderView(leader, PICK_CARD));
        leaderTitle.setText(MenuUi.leaderTitle(leader));
        leaderKind.setText(character.isEmpty() ? MenuUi.leaderKind(leader) : character + DOT + MenuUi.leaderKind(leader));
        leaderHint.setText(leader.isPassive() ? PASSIVE_HINT : ACTIVE_HINT);
    }

    /** The faction's card back with a big question mark. */
    private static StackPane buildRandomCard(Faction faction) {
        StackPane badge = new StackPane(MenuUi.label("?", "ng-random-mark"));
        badge.getStyleClass().add("ng-random-badge");
        return new StackPane(new CardBackView(faction, PICK_CARD), badge);
    }

    /** Gives {@code node} the colour style class of {@code faction} and removes the other factions' ones. */
    private static void setFactionStyle(Node node, Faction faction) {
        for (Faction other : Faction.values()) {
            node.getStyleClass().remove(MenuUi.factionStyle(other));
        }
        node.getStyleClass().add(MenuUi.factionStyle(faction));
    }

    /** Shows a leader in the right column, or the list of possible leaders for "Random" (null). */
    private void showLeaderDetails(LeaderDef leader) {
        if (leader == null) {
            details.showRandom(db.leaders(playerFaction));
        } else {
            details.showLeader(leader);
        }
        detailsScroll.setVvalue(detailsScroll.getVmin());
    }

    /** A bold question mark in place of an emblem, for the "Random" entries of the lists. */
    private static Node randomMark() {
        return MenuUi.iconBox(MenuUi.label("?", "ng-combo-random"), COMBO_ICON_SIZE + 4);
    }

    // ------------------------------------------------------------------ selection

    private void selectFaction(Faction faction) {
        if (faction == playerFaction) {
            return;
        }
        playerFaction = faction;
        selectChoice(factionBox, faction);
        showFaction(faction);
        List<Choice<LeaderDef>> choices = new ArrayList<>();
        choices.add(new Choice<LeaderDef>(null, "Random leader"));
        for (LeaderDef leader : db.leaders(faction)) {
            choices.add(new Choice<LeaderDef>(leader, leader.name()));
        }
        updatingLeaders = true;
        leaderBox.getItems().setAll(choices);
        updatingLeaders = false;
        selectLeader(null);
    }

    /** Selects a leader of the current faction; null (or a leader that isn't offered) means "Random leader". */
    private void selectLeader(LeaderDef leader) {
        playerLeader = leader != null && db.leaders(playerFaction).contains(leader) ? leader : null;
        updatingLeaders = true;
        selectChoice(leaderBox, playerLeader);
        updatingLeaders = false;
        showLeaderCard(playerLeader);
        showLeaderDetails(playerLeader);
        refresh();
    }

    /**
     * Fills the opponent's leader box with "Random" plus the leaders of {@code faction} (disabled for a random faction).
     * The entries show only the leader titles: the faction is chosen right above, and full names don't fit this narrower box.
     */
    private void fillAiLeaders(Faction faction) {
        List<Choice<LeaderDef>> choices = new ArrayList<>();
        choices.add(new Choice<LeaderDef>(null, "Random"));
        if (faction != null) {
            for (LeaderDef leader : db.leaders(faction)) {
                choices.add(new Choice<LeaderDef>(leader, MenuUi.leaderTitle(leader)));
            }
        }
        aiLeaderBox.getItems().setAll(choices);
        aiLeaderBox.getSelectionModel().selectFirst();
        aiLeaderBox.setDisable(faction == null);
    }

    private static <T> void selectChoice(ComboBox<Choice<T>> box, T value) {
        for (Choice<T> choice : box.getItems()) {
            if (Objects.equals(choice.value(), value)) {
                box.getSelectionModel().select(choice);
                return;
            }
        }
        box.getSelectionModel().selectFirst();
    }

    private Faction aiFaction() {
        Choice<Faction> choice = aiFactionBox.getValue();
        return choice == null ? null : choice.value();
    }

    private LeaderDef aiLeader() {
        Choice<LeaderDef> choice = aiLeaderBox.getValue();
        return choice == null ? null : choice.value();
    }

    // ------------------------------------------------------------------ settings

    /** Fills every control from {@code settings}; values that aren't offered fall back to the original-game defaults. */
    private void restore(GameSettings settings) {
        restoring = true;
        Faction faction = settings.playerFaction();
        if (faction == null || !faction.isPlayable()) {
            faction = Faction.NORTHERN_REALMS;
        }
        selectFaction(faction);
        selectLeader(settings.playerLeader());
        selectChoice(aiFactionBox, settings.aiFaction());
        if (settings.aiLeader() != null && settings.aiLeader().faction() == aiFaction()) {
            selectChoice(aiLeaderBox, settings.aiLeader());
        }
        if (!handSizes.select(settings.handSize())) {
            handSizes.select(GameSettings.DEFAULT_HAND_SIZE);
        }
        if (!redraws.select(settings.redraws())) {
            redraws.select(GameSettings.DEFAULT_REDRAWS);
        }
        if (!difficulties.select(settings.difficulty())) {
            difficulties.select(Difficulty.NORMAL);
        }
        restoring = false;
        refresh();
    }

    private GameSettings currentSettings() {
        return new GameSettings(playerFaction, playerLeader, aiFaction(), aiLeader(), handSizes.getValue(), redraws.getValue(), difficulties.getValue());
    }

    /** Updates the texts that depend on several choices and remembers the choices for this session. */
    private void refresh() {
        if (restoring) {
            return;
        }
        Faction faction = aiFaction();
        LeaderDef leader = aiLeader();
        if (faction == null) {
            aiInfo.setText("The computer plays a random faction with a random leader.");
        } else if (leader == null) {
            aiInfo.setText(faction.shortName() + " perk: " + faction.abilityDescription() + " The leader is picked at random.");
        } else {
            aiInfo.setText(faction.shortName() + " perk: " + faction.abilityDescription() + "\n" + MenuUi.leaderTitle(leader) + ": " + leader.description());
        }
        summary.setText(describe());
        remembered = currentSettings();
    }

    /** One-line summary of all choices, shown next to the Start button. */
    private String describe() {
        String you = playerFaction.displayName() + ", " + (playerLeader == null ? "random leader" : MenuUi.leaderTitle(playerLeader));
        Faction faction = aiFaction();
        LeaderDef leader = aiLeader();
        String opponent = faction == null ? "random" : faction.displayName() + ", " + (leader == null ? "random leader" : MenuUi.leaderTitle(leader));
        int redrawCount = redraws.getValue();
        String rules = handSizes.getValue() + " cards, " + redrawCount + (redrawCount == 1 ? " redraw" : " redraws");
        return "You: " + you + SEPARATOR + "Opponent: " + opponent + SEPARATOR + rules + SEPARATOR + difficulties.getValue().displayName();
    }

    // ------------------------------------------------------------------ list cells

    /** Drop-down entry of a faction list: the emblem (a question mark for "Random") and the name. */
    private static final class FactionCell extends ListCell<Choice<Faction>> {
        @Override
        protected void updateItem(Choice<Faction> item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(item.toString());
            setGraphic(item.value() == null ? randomMark() : MenuUi.iconBox(Icons.faction(item.value(), COMBO_ICON_SIZE), COMBO_ICON_SIZE + 4));
        }
    }

    private static final class FactionCells implements Callback<ListView<Choice<Faction>>, ListCell<Choice<Faction>>> {
        @Override
        public ListCell<Choice<Faction>> call(ListView<Choice<Faction>> list) {
            return new FactionCell();
        }
    }

    /** Drop-down entry of a leader list: a crown (a question mark for "Random") and the name. */
    private final class LeaderCell extends ListCell<Choice<LeaderDef>> {

        LeaderCell(boolean preview) {
            if (preview) {
                setOnMouseEntered(new LeaderPreview(this));
            }
        }

        @Override
        protected void updateItem(Choice<LeaderDef> item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(item.toString());
            setGraphic(item.value() == null ? randomMark() : MenuUi.iconBox(Icons.crown(COMBO_ICON_SIZE), COMBO_ICON_SIZE + 4));
        }
    }

    private final class LeaderCells implements Callback<ListView<Choice<LeaderDef>>, ListCell<Choice<LeaderDef>>> {

        private final boolean preview;

        LeaderCells(boolean preview) {
            this.preview = preview;
        }

        @Override
        public ListCell<Choice<LeaderDef>> call(ListView<Choice<LeaderDef>> list) {
            return new LeaderCell(preview);
        }
    }

    // ------------------------------------------------------------------ handlers

    private final class FactionListener implements ChangeListener<Choice<Faction>> {
        @Override
        public void changed(ObservableValue<? extends Choice<Faction>> observable, Choice<Faction> oldValue, Choice<Faction> newValue) {
            if (newValue != null && newValue.value() != null) {
                selectFaction(newValue.value());
            }
        }
    }

    private final class LeaderListener implements ChangeListener<Choice<LeaderDef>> {
        @Override
        public void changed(ObservableValue<? extends Choice<LeaderDef>> observable, Choice<LeaderDef> oldValue, Choice<LeaderDef> newValue) {
            if (!updatingLeaders && newValue != null) {
                selectLeader(newValue.value());
            }
        }
    }

    /** Hovering an entry of your leader list shows that leader (or the random list) in the right column. */
    private final class LeaderPreview implements EventHandler<MouseEvent> {

        private final ListCell<Choice<LeaderDef>> cell;

        LeaderPreview(ListCell<Choice<LeaderDef>> cell) {
            this.cell = cell;
        }

        @Override
        public void handle(MouseEvent event) {
            Choice<LeaderDef> item = cell.getItem();
            if (!cell.isEmpty() && item != null) {
                showLeaderDetails(item.value());
            }
        }
    }

    /** Closing your leader list shows the chosen leader again (hovering the entries showed other ones). */
    private final class LeaderListClosed implements EventHandler<Event> {
        @Override
        public void handle(Event event) {
            showLeaderDetails(playerLeader);
        }
    }

    /** Clicking a card tile opens the drop-down list above it. */
    private static final class OpenList implements EventHandler<MouseEvent> {

        private final ComboBox<?> box;

        OpenList(ComboBox<?> box) {
            this.box = box;
        }

        @Override
        public void handle(MouseEvent event) {
            box.show();
        }
    }

    /**
     * Opening a list scales it like the rest of the screen. The list opens in its own small window, outside the
     * ScaledPane that fits everything else to the window size, so it would otherwise always open at full size.
     */
    private static final class ScaleList implements EventHandler<Event> {

        private final ComboBox<?> box;

        ScaleList(ComboBox<?> box) {
            this.box = box;
        }

        @Override
        public void handle(Event event) {
            if (box.getSkin() instanceof ComboBoxListViewSkin<?> skin) {
                double factor = box.getLocalToSceneTransform().getMxx();
                skin.getPopupContent().getTransforms().setAll(new Scale(factor, factor));
            }
        }
    }

    private final class AiFactionListener implements ChangeListener<Choice<Faction>> {
        @Override
        public void changed(ObservableValue<? extends Choice<Faction>> observable, Choice<Faction> oldValue, Choice<Faction> newValue) {
            fillAiLeaders(newValue == null ? null : newValue.value());
            refresh();
        }
    }

    private final class AiLeaderListener implements ChangeListener<Choice<LeaderDef>> {
        @Override
        public void changed(ObservableValue<? extends Choice<LeaderDef>> observable, Choice<LeaderDef> oldValue, Choice<LeaderDef> newValue) {
            refresh();
        }
    }

    private final class OptionChanged implements Runnable {
        @Override
        public void run() {
            refresh();
        }
    }

    private final class StartAction implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            GameSettings settings = currentSettings();
            remembered = settings;
            navigator.startGame(settings);
        }
    }
}
