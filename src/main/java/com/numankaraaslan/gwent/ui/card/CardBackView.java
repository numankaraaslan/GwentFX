package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.model.Faction;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

/**
 * The back of a card (deck piles, opponent's hand): faction-coloured leather with a gold double border, a faint
 * diamond lattice and the faction emblem in a central gold medallion.
 * Style classes: {@code card-back}, {@code size-<size>}, {@code faction-<faction>}.
 */
public class CardBackView extends StackPane {

    private final Faction faction;
    private final CardSize size;

    /** Card back of the given faction and size. */
    public CardBackView(Faction faction, CardSize size) {
        this.faction = faction;
        this.size = size;
        double width = size.width();
        double height = size.height();
        getStyleClass().add("card-back");
        getStyleClass().add(CardView.sizeClass(size));
        getStyleClass().add(CardView.factionClass(faction));
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        Pane face = new Pane();
        face.getStyleClass().add("card-face");
        face.setMinSize(width, height);
        face.setPrefSize(width, height);
        face.setMaxSize(width, height);
        build(face, width, height);
        Rectangle ring = new Rectangle();
        CardView.overlay(ring, "card-ring", width, height);
        getChildren().addAll(face, ring);
    }

    /** Faction whose back is shown. */
    public Faction getFaction() {
        return faction;
    }

    /** Size of this card back. */
    public CardSize getCardSize() {
        return size;
    }

    private void build(Pane face, double width, double height) {
        Color main = Palette.faction(faction);
        Color dark = Palette.factionDark(faction);
        Color accent = Palette.factionAccent(faction);
        LinearGradient leather = new LinearGradient(0, 0, 0.35, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.mix(main, dark, 0.15)), new Stop(0.55, dark), new Stop(1, Palette.mix(dark, Color.BLACK, 0.35)));
        Rectangle base = CardView.frameShape(width, height, leather);
        base.setStroke(Palette.GOLD_DARK);
        base.setStrokeWidth(Math.max(1, width * 0.014));
        face.getChildren().add(base);
        double outer = Math.max(3, width * 0.06);
        double inner = outer + Math.max(2, width * 0.03);
        double radius = CardView.cornerRadius(width);
        Group lattice = new Group(CardArt.pattern(CardArt.LATTICE, 60, 0, 0, inner, inner, width - 2 * inner, height - 2 * inner, Palette.withOpacity(accent, 0.13), Math.max(0.6, width * 0.005)));
        lattice.setClip(new Rectangle(inner, inner, width - 2 * inner, height - 2 * inner));
        face.getChildren().add(lattice);
        face.getChildren().add(border(outer, width, height, radius * 0.6, Palette.withOpacity(Palette.GOLD, 0.85), Math.max(0.8, width * 0.012)));
        face.getChildren().add(border(inner, width, height, radius * 0.4, Palette.withOpacity(accent, 0.5), Math.max(0.6, width * 0.006)));
        double diamond = Math.max(2, width * 0.035);
        double[][] corners = {{outer, outer}, {width - outer, outer}, {outer, height - outer}, {width - outer, height - outer}};
        for (double[] corner : corners) {
            face.getChildren().add(diamond(corner[0], corner[1], diamond));
        }
        double cx = width / 2;
        double cy = height / 2;
        double medallion = width * 0.3;
        Color thread = Palette.withOpacity(Palette.GOLD, 0.55);
        face.getChildren().add(thread(cx, inner, cx, cy - medallion, thread, width));
        face.getChildren().add(thread(cx, cy + medallion, cx, height - inner, thread, width));
        face.getChildren().add(diamond(cx, inner + diamond * 0.6, diamond * 0.8));
        face.getChildren().add(diamond(cx, height - inner - diamond * 0.6, diamond * 0.8));
        Circle disc = new Circle(cx, cy, medallion);
        disc.setFill(new RadialGradient(0, 0, 0.42, 0.38, 0.75, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.mix(main, Palette.factionLight(faction), 0.3)), new Stop(1, dark)));
        disc.setStroke(CardView.goldPaint());
        disc.setStrokeWidth(Math.max(1, width * 0.022));
        disc.setStrokeType(StrokeType.INSIDE);
        face.getChildren().add(disc);
        face.getChildren().add(CardView.outlineCircle(cx, cy, medallion, width));
        Circle innerRing = new Circle(cx, cy, medallion * 0.84);
        innerRing.setFill(null);
        innerRing.setStroke(Palette.withOpacity(Palette.GOLD_BRIGHT, 0.4));
        innerRing.setStrokeWidth(Math.max(0.5, width * 0.005));
        face.getChildren().add(innerRing);
        double emblem = medallion * 1.36;
        face.getChildren().add(IconKit.place(EmblemArt.faction(faction, Palette.GOLD, Palette.INK), cx - emblem / 2, cy - emblem / 2, emblem / IconKit.BOX));
    }

    private static Rectangle border(double inset, double width, double height, double radius, Color color, double strokeWidth) {
        Rectangle border = new Rectangle(inset, inset, width - 2 * inset, height - 2 * inset);
        border.setArcWidth(radius * 2);
        border.setArcHeight(radius * 2);
        border.setFill(null);
        border.setStroke(color);
        border.setStrokeWidth(strokeWidth);
        return border;
    }

    private static Polygon diamond(double cx, double cy, double size) {
        Polygon diamond = new Polygon(IconKit.star(cx, cy, 2, size, size * 0.6, -90));
        diamond.setFill(Palette.GOLD);
        diamond.setStroke(Palette.INK);
        diamond.setStrokeWidth(Math.max(0.5, size * 0.18));
        return diamond;
    }

    private static Line thread(double x1, double y1, double x2, double y2, Color color, double cardWidth) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(color);
        line.setStrokeWidth(Math.max(0.6, cardWidth * 0.006));
        return line;
    }
}
