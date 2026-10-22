package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.model.Faction;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;

/**
 * Vector art of the faction emblems and the board / leader symbols (hero star, laurel, gem, crown, card stack),
 * drawn in the 100 x 100 box of {@link IconKit}.
 */
final class EmblemArt {

    private static final String SHIELD = "M14,8 L86,8 L86,46 C86,72 70,88 50,96 C30,88 14,72 14,46 Z";
    private static final String SHIELD_RIGHT = "M50,8 L86,8 L86,46 C86,72 70,88 50,96 Z";
    private static final String LILY_CENTER = "M50,14 C60,28 60,44 50,58 C40,44 40,28 50,14 Z";
    private static final String LILY_LEFT = "M44,58 C38,46 36,36 28,34 C20,32 14,40 18,50 C20,44 24,42 28,44 C34,46 38,52 40,58 Z";
    private static final String LILY_RIGHT = "M56,58 C62,46 64,36 72,34 C80,32 86,40 82,50 C80,44 76,42 72,44 C66,46 62,52 60,58 Z";
    private static final String LILY_BAND = "M34,57 L66,57 L66,64 L34,64 Z";
    private static final String LILY_FOOT = "M42,64 L58,64 L56,72 C54,76 52,78 50,80 C48,78 46,76 44,72 Z";

    private static final String LEAF = "M50,4 C70,20 80,44 72,66 C66,82 56,88 50,92 C44,88 34,82 28,66 C20,44 30,20 50,4 Z";
    private static final String LEAF_STEM = "M48.5,90 L51.5,90 L53,99 L47,99 Z";

    private static final String SKULL = "M50,20 C70,20 84,34 84,52 C84,64 78,70 72,73 L72,84 C72,88 68,90 64,90 L36,90 C32,90 28,88 28,84 L28,73 C22,70 16,64 16,52 C16,34 30,20 50,20 Z";
    private static final String SKULL_HORN_LEFT = "M22,44 C12,38 4,24 8,6 C14,20 22,28 32,32 Z";
    private static final String SKULL_HORN_RIGHT = "M78,44 C88,38 96,24 92,6 C86,20 78,28 68,32 Z";
    private static final String SKULL_EYE_LEFT = "M26,50 L44,54 C44,62 38,64 33,62 C28,60 26,56 26,50 Z";
    private static final String SKULL_EYE_RIGHT = "M74,50 L56,54 C56,62 62,64 67,62 C72,60 74,56 74,50 Z";
    private static final String SKULL_NOSE = "M50,62 L45,72 L55,72 Z";

    private static final String AXE_HAFT = "M51,8 L57,8 L57,98 L51,98 Z";
    private static final String AXE_BLADE = "M52,12 C40,6 26,6 16,14 C8,26 8,40 16,52 C28,56 42,52 52,44 Z";
    private static final String AXE_SPIKE = "M57,20 L71,26 L57,32 Z";

    private static final String CROWN_BODY = "M12,72 L8,28 L31,47 L50,16 L69,47 L92,28 L88,72 Z";
    private static final String CROWN_BAND = "M10,70 L90,70 L90,88 L10,88 Z";

    private EmblemArt() {
    }

    /** Faction emblem. */
    static Group faction(Faction faction, Color color, Color line) {
        switch (faction) {
            case NORTHERN_REALMS:
                return lilyShield(color, line);
            case NILFGAARD:
                return greatSun(color, line);
            case SCOIATAEL:
                return leaf(color, line);
            case MONSTERS:
                return skull(color, line);
            case SKELLIGE:
                return axes(color, line);
            default:
                return compass(color, line);
        }
    }

    /** Northern Realms: heater shield with the Temerian lily. */
    static Group lilyShield(Color color, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(SHIELD, color, line));
        group.getChildren().add(IconKit.flat(SHIELD_RIGHT, Palette.withOpacity(Color.BLACK, 0.16 * color.getOpacity())));
        Color lily = IconKit.shade(color, line);
        group.getChildren().add(IconKit.flat(LILY_LEFT, lily));
        group.getChildren().add(IconKit.flat(LILY_RIGHT, lily));
        group.getChildren().add(IconKit.flat(LILY_CENTER, lily));
        group.getChildren().add(IconKit.flat(LILY_BAND, lily));
        group.getChildren().add(IconKit.flat(LILY_FOOT, lily));
        return group;
    }

    /** Nilfgaard: the Great Sun with sixteen alternating rays. */
    static Group greatSun(Color color, Color line) {
        Group group = new Group();
        Color shortRay = IconKit.darker(color, 0.82);
        for (int i = 0; i < 16; i++) {
            double angle = -90 + i * 22.5;
            boolean longRay = i % 2 == 0;
            double tip = longRay ? 48 : 36;
            double half = longRay ? 7 : 6;
            double[] ray = {IconKit.px(50, tip, angle), IconKit.py(50, tip, angle), IconKit.px(50, 18, angle + half), IconKit.py(50, 18, angle + half), IconKit.px(50, 18, angle - half), IconKit.py(50, 18, angle - half)};
            group.getChildren().add(IconKit.polygon(ray, longRay ? color : shortRay, line));
        }
        group.getChildren().add(IconKit.circle(50, 50, 20, color, line));
        Ellipse ring = new Ellipse(50, 50, 12, 12);
        ring.setFill(null);
        ring.setStroke(IconKit.shade(color, line));
        ring.setStrokeWidth(2.5);
        group.getChildren().add(ring);
        group.getChildren().add(IconKit.circle(50, 50, 5, IconKit.shade(color, line), null));
        return group;
    }

    /** Scoia'tael: a slanted leaf with veins. */
    static Group leaf(Color color, Color line) {
        Group leaf = new Group();
        leaf.getChildren().add(IconKit.path(LEAF_STEM, IconKit.darker(color, 0.7), line));
        leaf.getChildren().add(IconKit.path(LEAF, color, line));
        Color vein = Palette.withOpacity(IconKit.shade(color, line), 0.75 * color.getOpacity());
        leaf.getChildren().add(IconKit.line(50, 12, 50, 90, vein, 2.6));
        double[] ys = {34, 52, 70};
        for (double y : ys) {
            double reach = y < 60 ? 16 : 12;
            leaf.getChildren().add(IconKit.line(50, y, 50 - reach, y - 11, vein, 2.1));
            leaf.getChildren().add(IconKit.line(50, y, 50 + reach, y - 11, vein, 2.1));
        }
        return IconKit.rotate(leaf, 28);
    }

    /** Monsters: a horned skull. */
    static Group skull(Color color, Color line) {
        Group group = new Group();
        Color horn = IconKit.darker(color, 0.75);
        Color dark = IconKit.shade(color, line);
        group.getChildren().add(IconKit.path(SKULL_HORN_LEFT, horn, line));
        group.getChildren().add(IconKit.path(SKULL_HORN_RIGHT, horn, line));
        group.getChildren().add(IconKit.path(SKULL, color, line));
        group.getChildren().add(IconKit.flat(SKULL_EYE_LEFT, dark));
        group.getChildren().add(IconKit.flat(SKULL_EYE_RIGHT, dark));
        group.getChildren().add(IconKit.flat(SKULL_NOSE, dark));
        group.getChildren().add(IconKit.line(33, 82, 67, 82, dark, 2));
        group.getChildren().add(IconKit.line(41, 76, 41, 88, dark, 2.2));
        group.getChildren().add(IconKit.line(50, 76, 50, 88, dark, 2.2));
        group.getChildren().add(IconKit.line(59, 76, 59, 88, dark, 2.2));
        return group;
    }

    /** Skellige: two crossed bearded axes. */
    static Group axes(Color color, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.mirror(IconKit.rotate(axe(color, line), -32)));
        group.getChildren().add(IconKit.rotate(axe(color, line), -32));
        return group;
    }

    private static Group axe(Color color, Color line) {
        Group group = new Group();
        Color wood = IconKit.darker(color, 0.65);
        group.getChildren().add(IconKit.path(AXE_HAFT, wood, line));
        group.getChildren().add(IconKit.circle(54, 98, 4, wood, line));
        group.getChildren().add(IconKit.path(AXE_SPIKE, color, line));
        group.getChildren().add(IconKit.path(AXE_BLADE, color, line));
        group.getChildren().add(IconKit.line(20, 16, 20, 48, IconKit.tint(color, 0.45), 2.2));
        return group;
    }

    /** Neutral: an eight-pointed compass star. */
    static Group compass(Color color, Color line) {
        Group group = new Group();
        double[] points = new double[32];
        for (int i = 0; i < 16; i++) {
            double angle = -90 + i * 22.5;
            double radius = i % 4 == 0 ? 48 : (i % 2 == 0 ? 28 : 10);
            points[i * 2] = IconKit.px(50, radius, angle);
            points[i * 2 + 1] = IconKit.py(50, radius, angle);
        }
        group.getChildren().add(IconKit.polygon(points, color, line));
        Color facet = Palette.withOpacity(Color.BLACK, 0.28 * color.getOpacity());
        for (int i = 0; i < 16; i += 2) {
            int next = (i + 1) % 16;
            double[] half = {50, 50, points[i * 2], points[i * 2 + 1], points[next * 2], points[next * 2 + 1]};
            Polygon shadow = new Polygon(half);
            shadow.setFill(facet);
            group.getChildren().add(shadow);
        }
        group.getChildren().add(IconKit.circle(50, 50, 6, IconKit.tint(color, 0.3), line));
        return group;
    }

    /** Hero marker: a bevelled five-pointed star. */
    static Group heroStar(Color color, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.polygon(IconKit.star(50, 54, 5, 47, 19, -90), color, line));
        group.getChildren().add(IconKit.polygon(IconKit.star(50, 54, 5, 24, 10, -90), IconKit.tint(color, 0.4), null));
        return group;
    }

    /** Laurel wreath, open at the top. */
    static Group laurel(Color color, Color line) {
        Group group = new Group();
        group.getChildren().add(laurelBranch(color, line));
        group.getChildren().add(IconKit.mirror(laurelBranch(color, line)));
        return group;
    }

    /** Left half of the wreath: a curved stem with two rows of leaves. */
    private static Group laurelBranch(Color color, Color line) {
        Group branch = new Group();
        Arc stem = new Arc(50, 50, 38, 38, -238, 140);
        stem.setType(ArcType.OPEN);
        stem.setFill(null);
        stem.setStroke(IconKit.darker(color, 0.7));
        stem.setStrokeWidth(3);
        branch.getChildren().add(stem);
        for (int i = 0; i < 7; i++) {
            double angle = 104 + i * 20;
            branch.getChildren().add(IconKit.ellipse(IconKit.px(50, 44, angle), IconKit.py(50, 44, angle), 4.2, 9, angle - 28, color, line));
            branch.getChildren().add(IconKit.ellipse(IconKit.px(50, 32, angle), IconKit.py(50, 32, angle), 3.8, 8, angle + 28, IconKit.darker(color, 0.85), line));
        }
        branch.getChildren().add(IconKit.ellipse(IconKit.px(50, 38, 244), IconKit.py(50, 38, 244), 4, 9, 244, color, line));
        return branch;
    }

    /** Round-life gem in a gold setting: lit ruby or dark grey glass. */
    static Group gem(boolean lit) {
        Group group = new Group();
        LinearGradient setting = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.GOLD_BRIGHT), new Stop(0.5, Palette.GOLD), new Stop(1, Palette.GOLD_DARK));
        group.getChildren().add(IconKit.circle(50, 50, 46, setting, Palette.INK));
        RadialGradient stone;
        if (lit) {
            stone = new RadialGradient(0, 0, 0.42, 0.38, 0.65, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#ffa090")), new Stop(0.35, Color.web("#e3283c")), new Stop(0.78, Color.web("#7c0712")), new Stop(1, Color.web("#3a0207")));
        } else {
            stone = new RadialGradient(0, 0, 0.42, 0.38, 0.65, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#7b7b7b")), new Stop(0.35, Color.web("#3d3d3f")), new Stop(0.78, Color.web("#1b1b1d")), new Stop(1, Color.web("#0a0a0b")));
        }
        Ellipse body = new Ellipse(50, 50, 37, 37);
        body.setFill(stone);
        body.setStroke(Palette.withOpacity(Color.BLACK, 0.7));
        body.setStrokeWidth(2);
        group.getChildren().add(body);
        Polygon table = new Polygon(IconKit.star(50, 50, 4, 19, 19, -67.5));
        table.setFill(Palette.withOpacity(Color.WHITE, lit ? 0.14 : 0.06));
        table.setStroke(Palette.withOpacity(Color.BLACK, 0.35));
        table.setStrokeWidth(1.5);
        group.getChildren().add(table);
        for (int i = 0; i < 8; i++) {
            double angle = -67.5 + i * 45;
            group.getChildren().add(IconKit.line(IconKit.px(50, 19, angle), IconKit.py(50, 19, angle), IconKit.px(50, 36, angle), IconKit.py(50, 36, angle), Palette.withOpacity(Color.BLACK, 0.3), 1.3));
        }
        group.getChildren().add(IconKit.ellipse(38, 34, 10, 6, -35, Palette.withOpacity(Color.WHITE, lit ? 0.55 : 0.18), null));
        group.getChildren().add(IconKit.circle(63, 64, 3, Palette.withOpacity(Color.WHITE, lit ? 0.35 : 0.12), null));
        return group;
    }

    /** Crown with three points and jewels. */
    static Group crown(Color color, Color line) {
        Group group = new Group();
        Color jewel = line != null ? Palette.RUBY : IconKit.shade(color, line);
        group.getChildren().add(IconKit.path(CROWN_BODY, color, line));
        group.getChildren().add(IconKit.circle(8, 24, 6, color, line));
        group.getChildren().add(IconKit.circle(50, 11, 7, color, line));
        group.getChildren().add(IconKit.circle(92, 24, 6, color, line));
        group.getChildren().add(IconKit.path(CROWN_BAND, IconKit.darker(color, 0.85), line));
        group.getChildren().add(IconKit.circle(30, 79, 4.5, jewel, null));
        group.getChildren().add(IconKit.circle(50, 79, 5.5, jewel, null));
        group.getChildren().add(IconKit.circle(70, 79, 4.5, jewel, null));
        return group;
    }

    /** Small fanned stack of three cards. */
    static Group cards(Color color, Color line) {
        Group group = new Group();
        group.getChildren().add(card(-18, IconKit.darker(color, 0.7), line));
        group.getChildren().add(card(18, IconKit.darker(color, 0.82), line));
        group.getChildren().add(card(0, color, line));
        Rectangle inner = new Rectangle(33, 27, 34, 52);
        inner.setArcWidth(6);
        inner.setArcHeight(6);
        inner.setFill(null);
        inner.setStroke(IconKit.shade(color, line));
        inner.setStrokeWidth(2);
        group.getChildren().add(inner);
        group.getChildren().add(IconKit.polygon(IconKit.star(50, 53, 2, 10, 6, -90), IconKit.shade(color, line), null));
        return group;
    }

    private static Rectangle card(double angle, Color color, Color line) {
        Rectangle card = new Rectangle(28, 22, 44, 62);
        card.setArcWidth(10);
        card.setArcHeight(10);
        card.setFill(color);
        IconKit.outline(card, line, IconKit.OUTLINE);
        if (angle != 0) {
            card.getTransforms().add(new Rotate(angle, 50, 84));
        }
        return card;
    }
}
