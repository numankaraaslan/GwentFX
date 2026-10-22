package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Faction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * A rendered Gwent card (procedural art: no image files), laid out like the Witcher 3 cards: strength badge in the
 * top-left corner with the row and ability icons below it, a procedural portrait, and the name on a parchment banner.
 * <p>
 * Frames: faction colour for normal units, gold for heroes, steel for special and weather cards. Heroes get a black
 * strength disc on a golden sunburst; special and weather cards show their icon instead of a number.
 * All geometry and font sizes are derived from the {@link CardSize}. The node tree is built once; {@link #update}
 * only touches the strength text unless the definition changed (berserker transformation).
 * <p>
 * Style classes: {@code card-view}, {@code size-<size>}, {@code unit-card} / {@code hero} / {@code special-card} /
 * {@code weather-card}, {@code faction-<faction>}; pseudo classes from {@link CardStyles}.
 */
public class CardView extends StackPane {

    private static final Color NAME_INK = Color.web("#2a1d12");
    private static final Color HERO_NUMBER = Color.web("#fff1d0");

    private CardDef def;
    private final int cardId;
    private final CardSize size;
    private int strength;

    private final Pane face = new Pane();
    private final Rectangle dimOverlay = new Rectangle();
    private final Rectangle ring = new Rectangle();
    private final List<String> variantClasses = new ArrayList<>();

    private Text strengthText;
    private double badgeCenterX;
    private double badgeCenterY;
    private double badgeSize;
    private ColorAdjust dimEffect;

    /** Card from the catalogue (gallery, previews): shows the printed strength, id = -1. */
    public CardView(CardDef def, CardSize size) {
        this(def, def.strength(), -1, size);
    }

    /** Card from a game snapshot: shows the current strength (green if buffed, red if weakened). */
    public CardView(CardSnapshot card, CardSize size) {
        this(card.def(), card.strength(), card.id(), size);
    }

    private CardView(CardDef def, int strength, int cardId, CardSize size) {
        this.def = def;
        this.strength = strength;
        this.cardId = cardId;
        this.size = size;
        double width = size.width();
        double height = size.height();
        getStyleClass().add("card-view");
        getStyleClass().add(sizeClass(size));
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        face.getStyleClass().add("card-face");
        face.setMinSize(width, height);
        face.setPrefSize(width, height);
        face.setMaxSize(width, height);
        overlay(dimOverlay, "card-dim", width, height);
        overlay(ring, "card-ring", width, height);
        getChildren().addAll(face, dimOverlay, ring);
        build();
    }

    /** Definition currently shown (changes after a berserker transformation). */
    public CardDef getDef() {
        return def;
    }

    /** Game card id, or -1 for catalogue cards. */
    public int getCardId() {
        return cardId;
    }

    /** Size of this card. */
    public CardSize getCardSize() {
        return size;
    }

    /** The strength currently shown. */
    public int getStrength() {
        return strength;
    }

    /** Re-renders with a newer snapshot of the same card (strength change, berserker transformation). */
    public void update(CardSnapshot card) {
        CardDef newDef = card.def();
        if (!newDef.id().equals(def.id())) {
            def = newDef;
            strength = card.strength();
            build();
            return;
        }
        def = newDef;
        if (card.strength() != strength) {
            strength = card.strength();
            refreshStrength();
        }
    }

    /** Selected card (strong gold glow). Only styling: the board positions / lifts the card itself. */
    public void setSelected(boolean selected) {
        pseudoClassStateChanged(CardStyles.SELECTED, selected);
    }

    /** True while the card is shown as selected. */
    public boolean isSelected() {
        return getPseudoClassStates().contains(CardStyles.SELECTED);
    }

    /** Valid target / playable highlight (green-gold glow). */
    public void setHighlighted(boolean highlighted) {
        pseudoClassStateChanged(CardStyles.HIGHLIGHTED, highlighted);
    }

    /** True while the card is highlighted. */
    public boolean isHighlighted() {
        return getPseudoClassStates().contains(CardStyles.HIGHLIGHTED);
    }

    /** Greyed out (not selectable): faded and desaturated. */
    public void setDimmed(boolean dimmed) {
        pseudoClassStateChanged(CardStyles.DIMMED, dimmed);
        if (dimmed) {
            if (dimEffect == null) {
                dimEffect = new ColorAdjust(0, -0.7, -0.08, 0);
            }
            face.setEffect(dimEffect);
        } else {
            face.setEffect(null);
        }
    }

    /** True while the card is dimmed. */
    public boolean isDimmed() {
        return getPseudoClassStates().contains(CardStyles.DIMMED);
    }

    // ------------------------------------------------------------------ building

    private void build() {
        double width = size.width();
        double height = size.height();
        updateVariantClasses();
        face.getChildren().clear();
        strengthText = null;
        double frame = frameThickness(width);
        boolean named = showsName(size);
        double bannerHeight = named ? bannerHeight(width, height) : 0;
        double portraitBottom = height - frame - bannerHeight;
        face.getChildren().add(frameShape(width, height, framePaint(def)));
        face.getChildren().add(CardArt.portrait(def, frame, frame, width - 2 * frame, portraitBottom - frame));
        face.getChildren().add(innerLine(frame, frame, width - 2 * frame, portraitBottom - frame, accentColor(def), width));
        if (named) {
            addBanner(frame, portraitBottom, width - 2 * frame, bannerHeight, width);
        }
        addBadge(width, frame);
        if (def.isUnit()) {
            addIconColumn(portraitBottom);
        }
    }

    private void addBanner(double x, double y, double width, double height, double cardWidth) {
        face.getChildren().addAll(banner(x, y, width, height, accentColor(def), cardWidth));
        double pad = Math.max(2, cardWidth * 0.035);
        double textWidth = width - 2 * pad;
        double maxFont = height * 0.40;
        double fontSize = CardFonts.fitSize(def.name(), false, textWidth, height - pad * 0.5, 2, maxFont, maxFont * 0.55);
        Text name = new Text(def.name());
        name.getStyleClass().add("card-name");
        name.setFont(CardFonts.name(fontSize));
        name.setFill(NAME_INK);
        name.setTextAlignment(TextAlignment.CENTER);
        name.setWrappingWidth(textWidth);
        name.setTextOrigin(VPos.TOP);
        double textHeight = name.getLayoutBounds().getHeight();
        name.setX(x + pad);
        name.setY(y + Math.max(0, (height - textHeight) / 2));
        name.setClip(new Rectangle(x, y, width, height));
        face.getChildren().add(name);
    }

    private void addBadge(double width, double frame) {
        double diameter = badgeDiameter(width);
        double radius = diameter / 2;
        double inset = frame * 0.55 + width * 0.012;
        badgeSize = diameter;
        badgeCenterX = inset + radius;
        badgeCenterY = inset + radius;
        if (!def.isUnit()) {
            Circle disc = new Circle(badgeCenterX, badgeCenterY, radius);
            disc.setFill(new RadialGradient(0, 0, 0.4, 0.35, 0.75, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#5a6470")), new Stop(1, Color.web("#13171c"))));
            disc.setStroke(silverPaint());
            disc.setStrokeWidth(Math.max(1, radius * 0.13));
            disc.setStrokeType(StrokeType.INSIDE);
            face.getChildren().add(disc);
            face.getChildren().add(outlineCircle(badgeCenterX, badgeCenterY, radius, width));
            double iconSize = diameter * 0.74;
            Node icon = Icons.special(def.special(), iconSize);
            icon.relocate(badgeCenterX - iconSize / 2, badgeCenterY - iconSize / 2);
            face.getChildren().add(icon);
            return;
        }
        if (def.hero()) {
            badgeCenterX = inset + radius * 1.12;
            badgeCenterY = inset + radius * 1.12;
            Polygon burst = new Polygon(IconKit.star(badgeCenterX, badgeCenterY, 16, radius * 1.2, radius * 0.98, -90));
            burst.setFill(goldPaint());
            burst.setStroke(Palette.INK);
            burst.setStrokeWidth(Math.max(0.6, radius * 0.05));
            Circle disc = new Circle(badgeCenterX, badgeCenterY, radius * 0.93);
            disc.setFill(new RadialGradient(0, 0, 0.4, 0.35, 0.8, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#3d342c")), new Stop(1, Color.web("#070505"))));
            disc.setStroke(goldPaint());
            disc.setStrokeWidth(Math.max(1, radius * 0.13));
            disc.setStrokeType(StrokeType.INSIDE);
            face.getChildren().addAll(burst, disc);
        } else {
            Circle disc = new Circle(badgeCenterX, badgeCenterY, radius);
            disc.setFill(new RadialGradient(0, 0, 0.4, 0.35, 0.75, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#fbf4e2")), new Stop(0.7, Palette.PARCHMENT), new Stop(1, Palette.PARCHMENT_DARK)));
            disc.setStroke(Palette.INK);
            disc.setStrokeWidth(Math.max(1, radius * 0.09));
            disc.setStrokeType(StrokeType.INSIDE);
            Circle inner = new Circle(badgeCenterX, badgeCenterY, radius * 0.8);
            inner.setFill(null);
            inner.setStroke(Palette.withOpacity(Palette.INK, 0.22));
            inner.setStrokeWidth(Math.max(0.5, radius * 0.04));
            face.getChildren().addAll(disc, inner);
        }
        strengthText = new Text();
        strengthText.getStyleClass().add("card-strength");
        strengthText.setTextOrigin(VPos.BASELINE);
        face.getChildren().add(strengthText);
        refreshStrength();
    }

    /**
     * The row icon (and the ability icon, if any) in medallions under the strength badge, as big as the badge. On small
     * cards they shrink just enough that two medallions always fit above the name banner.
     */
    private void addIconColumn(double portraitBottom) {
        double gap = badgeSize * 0.06;
        double top = badgeCenterY + badgeSize / 2 + gap + (def.hero() ? badgeSize * 0.08 : 0);
        double room = portraitBottom - top - gap;
        double diameter = Math.min(badgeSize, (room - gap) / 2);
        double cy = top + diameter / 2;
        double iconSize = diameter * 0.74;
        Node rowIcon = def.isAgile() ? Icons.agile(iconSize) : Icons.row(def.defaultRow(), iconSize);
        addMedallion(badgeCenterX, cy, diameter, rowIcon);
        if (def.ability() != Ability.NONE) {
            addMedallion(badgeCenterX, cy + diameter + gap, diameter, Icons.ability(def.ability(), iconSize));
        }
    }

    private void addMedallion(double cx, double cy, double diameter, Node icon) {
        Circle disc = new Circle(cx, cy, diameter / 2);
        disc.setFill(new RadialGradient(0, 0, 0.4, 0.35, 0.8, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#3a2c1f")), new Stop(1, Color.web("#0d0907"))));
        disc.setStroke(def.hero() ? Palette.GOLD : Palette.GOLD_DARK);
        disc.setStrokeWidth(Math.max(0.8, diameter * 0.06));
        face.getChildren().add(disc);
        double iconSize = diameter * 0.74;
        icon.relocate(cx - iconSize / 2, cy - iconSize / 2);
        face.getChildren().add(icon);
    }

    private void refreshStrength() {
        if (strengthText == null) {
            return;
        }
        String value = Integer.toString(strength);
        double factor = value.length() <= 1 ? 0.62 : (value.length() == 2 ? 0.53 : 0.42);
        if (def.hero()) {
            factor = factor * 0.9;
        }
        double fontSize = badgeSize * factor;
        strengthText.setText(value);
        strengthText.setFont(CardFonts.number(fontSize));
        strengthText.setFill(strengthColor());
        double textWidth = strengthText.getLayoutBounds().getWidth();
        strengthText.setX(badgeCenterX - textWidth / 2);
        strengthText.setY(badgeCenterY + fontSize * 0.35);
    }

    private Color strengthColor() {
        if (def.hero()) {
            if (strength > def.strength()) {
                return Palette.BUFF;
            }
            if (strength < def.strength()) {
                return Palette.DEBUFF;
            }
            return HERO_NUMBER;
        }
        if (strength > def.strength()) {
            return Palette.BUFF_DARK;
        }
        if (strength < def.strength()) {
            return Palette.DEBUFF_DARK;
        }
        return Palette.INK;
    }

    private void updateVariantClasses() {
        getStyleClass().removeAll(variantClasses);
        variantClasses.clear();
        if (def.isUnit()) {
            variantClasses.add(def.hero() ? "hero" : "unit-card");
        } else if (def.isWeather()) {
            variantClasses.add("weather-card");
        } else {
            variantClasses.add("special-card");
        }
        variantClasses.add(factionClass(def.faction()));
        getStyleClass().addAll(variantClasses);
    }

    // ------------------------------------------------------------------ shared geometry and paints

    /** Style class of a card size, e.g. {@code size-board}. */
    static String sizeClass(CardSize size) {
        return "size-" + size.name().toLowerCase(Locale.ROOT);
    }

    /** Style class of a faction, e.g. {@code faction-northern-realms}. */
    static String factionClass(Faction faction) {
        return "faction-" + faction.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** Cards narrower than the board size (TINY) hide the name. */
    static boolean showsName(CardSize size) {
        return size.width() >= 80;
    }

    /** Thickness of the coloured frame. */
    static double frameThickness(double width) {
        return Math.max(2.0, width * 0.032);
    }

    /** Corner radius of the card. */
    static double cornerRadius(double width) {
        return Math.max(3.0, width * 0.055);
    }

    /** Diameter of the strength badge: relatively larger on small cards so the number stays readable. */
    static double badgeDiameter(double width) {
        double small = Math.max(0, Math.min(1, (150 - width) / 90.0));
        return width * (0.27 + 0.13 * small);
    }

    /** Height of the name banner: relatively taller on small cards (two lines of small text). */
    static double bannerHeight(double width, double height) {
        double small = Math.max(0, Math.min(1, (150 - width) / 68.0));
        return height * (0.19 + 0.04 * small);
    }

    /** Frame paint: faction colour, gold for heroes, steel for special / weather cards. */
    static Paint framePaint(CardDef def) {
        if (!def.isUnit()) {
            return silverPaint();
        }
        if (def.hero()) {
            return goldPaint();
        }
        Faction faction = def.faction();
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.factionLight(faction)), new Stop(0.45, Palette.faction(faction)), new Stop(1, Palette.factionDark(faction)));
    }

    /** Accent colour of the card's trims: faction accent, bright gold for heroes, silver for special cards. */
    static Color accentColor(CardDef def) {
        if (!def.isUnit()) {
            return Palette.SILVER;
        }
        if (def.hero()) {
            return Palette.GOLD_BRIGHT;
        }
        return Palette.factionAccent(def.faction());
    }

    /** Polished gold with diagonal shine bands. */
    static LinearGradient goldPaint() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.GOLD_BRIGHT), new Stop(0.22, Palette.GOLD), new Stop(0.45, Palette.GOLD_DARK), new Stop(0.62, Palette.GOLD), new Stop(0.78, Palette.GOLD_BRIGHT), new Stop(1, Palette.GOLD_DARK));
    }

    /** Brushed steel with diagonal shine bands. */
    static LinearGradient silverPaint() {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#eef1f4")), new Stop(0.3, Color.web("#a9b0b8")), new Stop(0.55, Palette.SILVER_DARK), new Stop(0.8, Palette.SILVER), new Stop(1, Color.web("#6b7179")));
    }

    /** Rounded card frame with a drop shadow. */
    static Rectangle frameShape(double width, double height, Paint paint) {
        double radius = cornerRadius(width);
        Rectangle frame = new Rectangle(0, 0, width, height);
        frame.setArcWidth(radius * 2);
        frame.setArcHeight(radius * 2);
        frame.setFill(paint);
        frame.setStroke(Palette.INK);
        frame.setStrokeWidth(Math.max(1, width * 0.006));
        frame.setStrokeType(StrokeType.INSIDE);
        frame.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, Palette.withOpacity(Color.BLACK, 0.6), Math.max(3, width * 0.04), 0.15, 0, Math.max(1, width * 0.012)));
        return frame;
    }

    /** Thin trim line around the portrait. */
    static Rectangle innerLine(double x, double y, double width, double height, Color color, double cardWidth) {
        Rectangle line = new Rectangle(x, y, width, height);
        line.setFill(null);
        line.setStroke(Palette.withOpacity(color, 0.85));
        line.setStrokeWidth(Math.max(0.8, cardWidth * 0.006));
        line.setStrokeType(StrokeType.INSIDE);
        return line;
    }

    /** Parchment name banner with a trim rule (and a small diamond on larger cards) along its top edge. */
    static List<Node> banner(double x, double y, double width, double height, Color accent, double cardWidth) {
        List<Node> nodes = new ArrayList<>();
        Rectangle banner = new Rectangle(x, y, width, height);
        banner.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.PARCHMENT), new Stop(1, Palette.PARCHMENT_DARK)));
        nodes.add(banner);
        double rule = Math.max(1, cardWidth * 0.01);
        Rectangle line = new Rectangle(x, y - rule / 2, width, rule);
        line.setFill(accent);
        nodes.add(line);
        if (cardWidth >= 140) {
            Polygon diamond = new Polygon(IconKit.star(x + width / 2, y, 2, rule * 3.4, rule * 1.8, -90));
            diamond.setFill(accent);
            diamond.setStroke(Palette.INK);
            diamond.setStrokeWidth(Math.max(0.6, rule * 0.35));
            nodes.add(diamond);
        }
        return nodes;
    }

    /** Thin dark ring around a badge. */
    static Circle outlineCircle(double cx, double cy, double radius, double cardWidth) {
        Circle circle = new Circle(cx, cy, radius);
        circle.setFill(null);
        circle.setStroke(Palette.INK);
        circle.setStrokeWidth(Math.max(0.6, cardWidth * 0.005));
        return circle;
    }

    /** Full-card overlay shape styled from cards.css (selection ring, dim layer). */
    static void overlay(Rectangle shape, String styleClass, double width, double height) {
        double radius = cornerRadius(width);
        shape.setWidth(width);
        shape.setHeight(height);
        shape.setArcWidth(radius * 2);
        shape.setArcHeight(radius * 2);
        shape.setFill(Color.TRANSPARENT);
        shape.setStroke(null);
        shape.getStyleClass().add(styleClass);
        shape.setMouseTransparent(true);
    }
}
