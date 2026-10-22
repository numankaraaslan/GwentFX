package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.model.LeaderDef;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * A rendered leader card: polished gold frame with a faction-coloured inner trim, a royal portrait with a crown, the
 * faction emblem in the top-left medallion, and the leader's name ("Foltest") above the title ("King of Temeria").
 * <p>
 * States: used (desaturated + "USED" stamp), cancelled (desaturated + red "CANCELLED" stamp), highlighted (glow:
 * ability usable now) and selected; styled in cards.css via {@link CardStyles} pseudo classes.
 */
public class LeaderView extends StackPane {

    private static final Color NAME_INK = Color.web("#2a1d12");
    private static final Color TITLE_INK = Color.web("#5a4127");

    private final LeaderDef leader;
    private final CardSize size;
    private final boolean titleOnly;
    private final Pane face = new Pane();
    private boolean used;
    private boolean cancelled;
    private ColorAdjust inactiveEffect;

    /** Leader card of the given size, with the character name and the title ("Foltest" / "King of Temeria"). */
    public LeaderView(LeaderDef leader, CardSize size) {
        this(leader, size, false);
    }

    /**
     * Leader card of the given size. {@code titleOnly}: only the title is written on the card ("The Steel-Forged"), larger,
     * since it tells which ability the leader has while the character is the same for the whole faction; for small cards
     * where both lines would be too small to read. Leaders without a title (Skellige) show their name.
     */
    public LeaderView(LeaderDef leader, CardSize size, boolean titleOnly) {
        this.leader = leader;
        this.size = size;
        this.titleOnly = titleOnly;
        double width = size.width();
        double height = size.height();
        getStyleClass().add("leader-view");
        getStyleClass().add(CardView.sizeClass(size));
        getStyleClass().add(CardView.factionClass(leader.faction()));
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        face.getStyleClass().add("card-face");
        face.setMinSize(width, height);
        face.setPrefSize(width, height);
        face.setMaxSize(width, height);
        build(width, height);
        Rectangle ring = new Rectangle();
        CardView.overlay(ring, "card-ring", width, height);
        getChildren().addAll(face, stamp("USED", "leader-stamp-used", width), stamp("CANCELLED", "leader-stamp-cancelled", width), ring);
    }

    /** Leader shown by this card. */
    public LeaderDef getLeader() {
        return leader;
    }

    /** Size of this card. */
    public CardSize getCardSize() {
        return size;
    }

    /** Ability already used: greyed out with a "USED" stamp. */
    public void setUsed(boolean used) {
        this.used = used;
        pseudoClassStateChanged(CardStyles.USED, used);
        refreshEffect();
    }

    /** Cancelled by Emhyr: The White Flame (greyed out with a red "CANCELLED" stamp). */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
        pseudoClassStateChanged(CardStyles.CANCELLED, cancelled);
        refreshEffect();
    }

    /** Ability can be used right now: glow. */
    public void setHighlighted(boolean highlighted) {
        pseudoClassStateChanged(CardStyles.HIGHLIGHTED, highlighted);
    }

    /** Selected (e.g. on the new game screen). */
    public void setSelected(boolean selected) {
        pseudoClassStateChanged(CardStyles.SELECTED, selected);
    }

    /** True if shown as used. */
    public boolean isUsed() {
        return used;
    }

    /** True if shown as cancelled. */
    public boolean isCancelled() {
        return cancelled;
    }

    private void refreshEffect() {
        if (used || cancelled) {
            if (inactiveEffect == null) {
                inactiveEffect = new ColorAdjust(0, -0.85, -0.22, 0);
            }
            face.setEffect(inactiveEffect);
        } else {
            face.setEffect(null);
        }
    }

    // ------------------------------------------------------------------ building

    private void build(double width, double height) {
        double frame = Math.max(2.5, width * 0.04);
        double bannerHeight = height * 0.22;
        double portraitBottom = height - frame - bannerHeight;
        face.getChildren().add(CardView.frameShape(width, height, CardView.goldPaint()));
        Rectangle trim = new Rectangle(frame * 0.42, frame * 0.42, width - frame * 0.84, height - frame * 0.84);
        double radius = CardView.cornerRadius(width) * 0.7;
        trim.setArcWidth(radius * 2);
        trim.setArcHeight(radius * 2);
        trim.setFill(null);
        trim.setStroke(Palette.faction(leader.faction()));
        trim.setStrokeWidth(Math.max(0.8, frame * 0.28));
        face.getChildren().add(trim);
        face.getChildren().add(CardArt.leaderPortrait(leader, frame, frame, width - 2 * frame, portraitBottom - frame));
        face.getChildren().add(CardView.innerLine(frame, frame, width - 2 * frame, portraitBottom - frame, Palette.GOLD_BRIGHT, width));
        face.getChildren().addAll(CardView.banner(frame, portraitBottom, width - 2 * frame, bannerHeight, Palette.GOLD_BRIGHT, width));
        addNames(frame, portraitBottom, width - 2 * frame, bannerHeight, width);
        addEmblem(width, frame);
    }

    private void addNames(double x, double y, double width, double height, double cardWidth) {
        String[] parts = splitName(leader.name());
        String name = titleOnly && !parts[1].isEmpty() ? parts[1] : parts[0];
        String title = titleOnly ? "" : parts[1];
        double pad = Math.max(2, cardWidth * 0.04);
        double textWidth = width - 2 * pad;
        double usable = height - pad;
        Text nameText;
        Text titleText = null;
        if (title.isEmpty()) {
            double max = height * 0.34;
            nameText = text(name, CardFonts.name(CardFonts.fitSize(name, false, textWidth, usable, 2, max, max * 0.5)), NAME_INK, textWidth);
        } else {
            double max = height * 0.32;
            nameText = text(name, CardFonts.name(CardFonts.fitSize(name, false, textWidth, usable * 0.5, 1, max, max * 0.45)), NAME_INK, textWidth);
            double titleMax = height * 0.19;
            titleText = text(title, CardFonts.title(CardFonts.fitSize(title, true, textWidth, usable * 0.46, 2, titleMax, titleMax * 0.6)), TITLE_INK, textWidth);
        }
        double nameHeight = nameText.getLayoutBounds().getHeight();
        double titleHeight = titleText == null ? 0 : titleText.getLayoutBounds().getHeight();
        double gap = titleText == null ? 0 : height * 0.02;
        double top = y + Math.max(0, (height - nameHeight - gap - titleHeight) / 2);
        nameText.setX(x + pad);
        nameText.setY(top);
        face.getChildren().add(nameText);
        if (titleText != null) {
            titleText.setX(x + pad);
            titleText.setY(top + nameHeight + gap);
            face.getChildren().add(titleText);
        }
    }

    private static Text text(String value, Font font, Color color, double width) {
        Text text = new Text(value);
        text.setFont(font);
        text.setFill(color);
        text.setTextAlignment(TextAlignment.CENTER);
        text.setWrappingWidth(width);
        text.setTextOrigin(VPos.TOP);
        return text;
    }

    private void addEmblem(double width, double frame) {
        double diameter = CardView.badgeDiameter(width) * 0.92;
        double radius = diameter / 2;
        double inset = frame * 0.55 + width * 0.012;
        double cx = inset + radius;
        double cy = inset + radius;
        Circle disc = new Circle(cx, cy, radius);
        disc.setFill(new RadialGradient(0, 0, 0.4, 0.35, 0.8, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.faction(leader.faction()).brighter()), new Stop(1, Palette.factionDark(leader.faction()))));
        disc.setStroke(CardView.goldPaint());
        disc.setStrokeWidth(Math.max(1, radius * 0.13));
        disc.setStrokeType(StrokeType.INSIDE);
        face.getChildren().add(disc);
        face.getChildren().add(CardView.outlineCircle(cx, cy, radius, width));
        double iconSize = diameter * 0.66;
        Node emblem = Icons.faction(leader.faction(), iconSize, Palette.GOLD_BRIGHT);
        emblem.relocate(cx - iconSize / 2, cy - iconSize / 2);
        face.getChildren().add(emblem);
    }

    /** "Foltest: King of Temeria" gives {"Foltest", "King of Temeria"}; names without a colon have an empty title. */
    static String[] splitName(String name) {
        int colon = name.indexOf(':');
        if (colon > 0) {
            return new String[] {name.substring(0, colon).trim(), name.substring(colon + 1).trim()};
        }
        return new String[] {name.trim(), ""};
    }

    private static StackPane stamp(String value, String styleClass, double cardWidth) {
        Text text = new Text(value);
        text.getStyleClass().add("stamp-text");
        text.setFill(Palette.TEXT);
        text.setFont(CardFonts.display(Math.max(9, cardWidth * (value.length() > 5 ? 0.1 : 0.14))));
        StackPane stamp = new StackPane(text);
        stamp.getStyleClass().add("leader-stamp");
        stamp.getStyleClass().add(styleClass);
        stamp.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        stamp.setRotate(-14);
        stamp.setMouseTransparent(true);
        stamp.setVisible(false);
        return stamp;
    }
}
