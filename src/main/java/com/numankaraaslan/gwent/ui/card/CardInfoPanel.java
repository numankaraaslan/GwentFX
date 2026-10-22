package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.GameSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.PlayerSnapshot;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.LeaderDef;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Big preview of one card plus its rules text (right side of the board, card gallery). Nothing the card already shows
 * is repeated: not the name, strength, row or card kind (all on the card face), nor the faction (known from the game or
 * the gallery tab). A changed strength gets a line with the base value; a leader gets a line saying whether its ability
 * is active or passive. Fixed width {@link #PANEL_WIDTH}; never taller than {@link #MAX_HEIGHT} (long texts are clipped,
 * never drawn outside the panel). Styled in cards.css ({@code card-info-*} classes).
 */
public class CardInfoPanel extends VBox {

    public static final double PANEL_WIDTH = 340;
    /** The panel never grows taller than this (preview card 424 px + texts). */
    public static final double MAX_HEIGHT = 820;

    private static final String HINT = "Hover over a card to see its details.";
    private static final int COMPACT_RULES_LENGTH = 240;

    private final StackPane preview = new StackPane();
    /** Leaders only: active or passive ability. */
    private final Label typeLine = new Label();
    private final Label strengthLine = new Label();
    private final TextFlow rules = new TextFlow();
    private final Label status = new Label();
    private String shownKey = "";
    /** The game card shown by {@link #showCard(CardSnapshot)}, kept up to date by {@link #refresh}; -1 for none. */
    private int shownCardId = -1;
    /** The leader shown by {@link #showLeader}, kept up to date by {@link #refresh}; null for none. */
    private LeaderDef shownLeader;

    /** Empty panel showing the neutral hint. */
    public CardInfoPanel() {
        getStyleClass().add("card-info-panel");
        setMinWidth(PANEL_WIDTH);
        setPrefWidth(PANEL_WIDTH);
        setMaxWidth(PANEL_WIDTH);
        setMaxHeight(MAX_HEIGHT);
        setFillWidth(true);
        setAlignment(Pos.TOP_CENTER);
        double previewWidth = CardSize.PREVIEW.width();
        double previewHeight = CardSize.PREVIEW.height();
        preview.getStyleClass().add("card-info-preview");
        preview.setMinSize(previewWidth, previewHeight);
        preview.setPrefSize(previewWidth, previewHeight);
        preview.setMaxSize(previewWidth, previewHeight);
        label(typeLine, "card-info-type");
        label(strengthLine, "card-info-strength");
        label(status, "card-info-status");
        rules.getStyleClass().add("card-info-rules");
        rules.setMaxWidth(Double.MAX_VALUE);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        setClip(clip);
        getChildren().addAll(preview, typeLine, strengthLine, rules, status);
        clear();
    }

    /** Catalogue card with its printed strength. */
    public void showCard(CardDef def) {
        shownCardId = -1;
        shownLeader = null;
        String key = "def:" + def.id();
        if (key.equals(shownKey)) {
            return;
        }
        shownKey = key;
        showCardInfo(def, new CardView(def, CardSize.PREVIEW));
        hide(strengthLine);
    }

    /** Game card with its current strength. */
    public void showCard(CardSnapshot card) {
        shownCardId = card.id();
        shownLeader = null;
        CardDef def = card.def();
        String key = "card:" + card.id() + ":" + def.id() + ":" + card.strength();
        if (key.equals(shownKey)) {
            return;
        }
        shownKey = key;
        showCardInfo(def, new CardView(card, CardSize.PREVIEW));
        strengthLine.getStyleClass().removeAll("buffed", "debuffed");
        if (def.isUnit() && card.strength() != def.strength()) {
            strengthLine.getStyleClass().add(card.strength() > def.strength() ? "buffed" : "debuffed");
            show(strengthLine, "Current strength " + card.strength() + " (base " + def.strength() + ")");
        } else {
            hide(strengthLine);
        }
    }

    /** Leader card with its ability text and state (already used / cancelled by Emhyr: The White Flame). */
    public void showLeader(LeaderDef leader, boolean used, boolean cancelled) {
        shownCardId = -1;
        shownLeader = leader;
        String key = "leader:" + leader.id() + ":" + used + ":" + cancelled;
        if (key.equals(shownKey)) {
            return;
        }
        shownKey = key;
        LeaderView view = new LeaderView(leader, CardSize.PREVIEW);
        view.setUsed(used);
        view.setCancelled(cancelled);
        preview.getChildren().setAll(view);
        show(typeLine, leader.isPassive() ? "Passive leader ability — always active" : "Leader ability — usable once per game");
        hide(strengthLine);
        setRules(leader.description(), false);
        if (cancelled) {
            show(status, "Cancelled by Emhyr: The White Flame");
        } else if (used && !leader.isPassive()) {
            show(status, "Already used");
        } else {
            hide(status);
        }
    }

    /** Shows a neutral hint instead of a card. */
    public void clear() {
        shownKey = "";
        shownCardId = -1;
        shownLeader = null;
        preview.getChildren().setAll(placeholder());
        hide(typeLine);
        hide(strengthLine);
        hide(status);
        rules.getChildren().clear();
        rules.setVisible(false);
        rules.setManaged(false);
    }

    /**
     * Keeps the shown game card or leader up to date with {@code snapshot}, e.g. a card hit by weather while the mouse stays
     * on it, or a leader whose ability was just used. Cards that left every visible place (back into a deck) stay as they
     * were; so does a leader that both players have (mirror match), since it is unclear whose it was.
     */
    public void refresh(GameSnapshot snapshot) {
        if (shownCardId >= 0) {
            CardSnapshot card = snapshot.findCard(shownCardId);
            if (card != null) {
                showCard(card);
            }
            return;
        }
        if (shownLeader == null) {
            return;
        }
        PlayerSnapshot owner = null;
        int owners = 0;
        for (PlayerSnapshot player : snapshot.players()) {
            if (player.leader().equals(shownLeader)) {
                owner = player;
                owners++;
            }
        }
        if (owners == 1) {
            showLeader(owner.leader(), owner.leaderUsed(), owner.leaderDisabled());
        }
    }

    // ------------------------------------------------------------------ helpers

    /** A card: preview and rules text only, since strength, row and card kind are shown on the card itself. */
    private void showCardInfo(CardDef def, CardView view) {
        preview.getChildren().setAll(view);
        hide(typeLine);
        setRules(def.description(), def.isUnit());
        hide(status);
    }

    /** Rules text, one paragraph per line; "Name: text" paragraphs get the name in gold. */
    private void setRules(String description, boolean named) {
        rules.getChildren().clear();
        rules.getStyleClass().remove("compact");
        if (description == null || description.isBlank()) {
            rules.setVisible(false);
            rules.setManaged(false);
            return;
        }
        if (description.length() > COMPACT_RULES_LENGTH) {
            rules.getStyleClass().add("compact");
        }
        String[] paragraphs = description.split("\n");
        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i].trim();
            String end = i < paragraphs.length - 1 ? "\n" : "";
            int colon = named ? paragraph.indexOf(": ") : -1;
            if (colon > 0) {
                rules.getChildren().add(ruleText(paragraph.substring(0, colon + 1) + " ", "card-info-rule-name"));
                rules.getChildren().add(ruleText(paragraph.substring(colon + 2) + end, "card-info-rule-text"));
            } else {
                rules.getChildren().add(ruleText(paragraph + end, "card-info-rule-text"));
            }
        }
        rules.setVisible(true);
        rules.setManaged(true);
    }

    private static Text ruleText(String value, String styleClass) {
        Text text = new Text(value);
        text.getStyleClass().add(styleClass);
        return text;
    }

    private static Node placeholder() {
        StackPane placeholder = new StackPane();
        placeholder.getStyleClass().add("card-info-placeholder");
        placeholder.setMinSize(CardSize.PREVIEW.width(), CardSize.PREVIEW.height());
        placeholder.setPrefSize(CardSize.PREVIEW.width(), CardSize.PREVIEW.height());
        placeholder.setMaxSize(CardSize.PREVIEW.width(), CardSize.PREVIEW.height());
        Pane emblem = new Pane(IconKit.place(EmblemArt.compass(Palette.withOpacity(Palette.ICON, 0.16), null), 0, 0, 1.2));
        emblem.setMinSize(120, 120);
        emblem.setPrefSize(120, 120);
        emblem.setMaxSize(120, 120);
        Label hint = new Label(HINT);
        hint.getStyleClass().add("card-info-hint");
        hint.setWrapText(true);
        hint.setMaxWidth(CardSize.PREVIEW.width() - 60);
        VBox content = new VBox(18, emblem, hint);
        content.setAlignment(Pos.CENTER);
        content.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        placeholder.getChildren().add(content);
        return placeholder;
    }

    private static void label(Label label, String styleClass) {
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
    }

    private static void show(Label label, String text) {
        label.setText(text);
        label.setVisible(true);
        label.setManaged(true);
    }

    private static void hide(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }
}
