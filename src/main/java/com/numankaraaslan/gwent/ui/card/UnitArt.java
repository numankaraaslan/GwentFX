package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.RowType;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.StrokeLineCap;

/**
 * Vector art of the row and unit-ability icons, drawn in the 100 x 100 box of {@link IconKit}.
 * {@code fill} is the main colour, {@code line} the outline colour (or null for outline-free motifs).
 */
final class UnitArt {

    private static final String SWORD_BLADE = "M50,-8 L57,4 L57,66 L43,66 L43,4 Z";
    private static final String SWORD_GRIP = "M45,74 L55,74 L55,94 L45,94 Z";
    private static final String SWORD_GUARD = "M22,64 L78,64 L74,74 L26,74 Z";

    private static final String BOW_LIMB = "M40,4 Q100,50 40,96 L37,91 Q86,50 37,9 Z";
    private static final String BOW_GRIP = "M60,43 L72,43 L72,57 L60,57 Z";
    private static final String ARROW_SHAFT = "M22,48.6 L85,48.6 L85,51.4 L22,51.4 Z";
    private static final String ARROW_HEAD = "M82,41 L99,50 L82,59 L86,50 Z";
    private static final String ARROW_FLETCHING = "M36,48.5 L29,41 L20,41 L25,48.5 Z M36,51.5 L29,59 L20,59 L25,51.5 Z";

    private static final String SIEGE_BASE = "M6,64 L94,64 L94,74 L6,74 Z";
    private static final String SIEGE_FRAME = "M34,64 L50,32 L66,64 L57,64 L50,48 L43,64 Z";
    private static final String SIEGE_WEIGHT = "M67,48 L85,48 L85,62 L67,62 Z";
    private static final String SIEGE_ARM = "M16.1,6.6 L78.1,44.6 L73.9,51.4 L11.9,13.4 Z";

    private static final String MEDIC_CROSS = "M38,8 L62,8 L62,38 L92,38 L92,62 L62,62 L62,92 L38,92 L38,62 L8,62 L8,38 L38,38 Z";
    private static final String CHEVRON_TOP = "M50,8 L90,42 L90,58 L50,24 L10,58 L10,42 Z";
    private static final String CHEVRON_BOTTOM = "M50,42 L90,76 L90,92 L50,58 L10,92 L10,76 Z";

    private static final String FIGURE_LEFT = "M6,82 Q6,50 25,48 Q44,50 44,82 Z";
    private static final String FIGURE_RIGHT = "M56,82 Q56,50 75,48 Q94,50 94,82 Z";
    private static final String FIGURE_FRONT = "M22,96 Q22,60 50,58 Q78,60 78,96 Z";

    private static final String EYE = "M4,50 Q50,2 96,50 Q50,98 4,50 Z";

    private static final String FLAME_OUTER = "M50,4 C56,20 74,30 76,52 C78,74 66,94 50,96 C34,94 22,80 22,62 C22,48 30,40 36,32 C36,42 40,48 46,50 C42,36 44,18 50,4 Z";
    private static final String FLAME_INNER = "M50,48 C56,58 64,64 64,76 C64,88 56,94 50,94 C44,94 36,88 36,78 C36,68 44,62 50,48 Z";

    private static final String HORN_BODY = "M8,22 Q34,62 76,36 L90,70 Q36,86 10,30 Z";
    private static final String HORN_MOUTHPIECE = "M3,21 L9,19.5 L12,31 L6,33 Z";

    private static final String CLAW_LEFT = "M46,14 Q40,54 14,90 Q22,50 46,14 Z";
    private static final String CLAW_MIDDLE = "M66,4 Q58,52 30,96 Q40,48 66,4 Z";
    private static final String CLAW_RIGHT = "M86,14 Q78,56 50,90 Q58,50 86,14 Z";

    private static final String MUSHROOM_CAP = "M6,54 C6,24 28,8 50,8 C72,8 94,24 94,54 C80,60 20,60 6,54 Z";
    private static final String MUSHROOM_STEM = "M38,57 C38,70 34,84 30,94 L70,94 C66,84 62,70 62,57 Z";

    private static final String AVENGER_ARROW = "M80.6,26.3 A40,40 0 1,1 30,17.4 L26,10.4 L48.7,19 L41,36.4 L37,29.5 A26,26 0 1,0 69.9,35.3 Z";

    private UnitArt() {
    }

    // ------------------------------------------------------------------ rows

    /** Row icon: sword, bow with arrow or catapult. */
    static Group row(RowType row, Color fill, Color line) {
        switch (row) {
            case CLOSE:
                return IconKit.rotate(sword(fill, line), 45);
            case RANGED:
                return IconKit.rotate(bow(fill, line, true), -45);
            default:
                return siege(fill, line);
        }
    }

    /** Agile: sword and bow crossed. */
    static Group agile(Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.rotate(bow(fill, line, false), 45));
        group.getChildren().add(IconKit.rotate(sword(fill, line), -45));
        return group;
    }

    /** Vertical sword, tip up, about 116 units long and centred in the box (meant to be rotated). */
    static Group sword(Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(SWORD_BLADE, IconKit.tint(fill, 0.25), line));
        group.getChildren().add(IconKit.line(50, 8, 50, 58, IconKit.darker(fill, 0.6), 2.5));
        group.getChildren().add(IconKit.path(SWORD_GRIP, IconKit.darker(fill, 0.62), line));
        group.getChildren().add(IconKit.path(SWORD_GUARD, fill, line));
        group.getChildren().add(IconKit.circle(50, 100, 7, fill, line));
        return group;
    }

    /** Vertical bow bulging right; with an arrow (pointing right) the string is drawn back. */
    static Group bow(Color fill, Color line, boolean arrow) {
        Group group = new Group();
        Color string = IconKit.tint(fill, 0.35);
        if (arrow) {
            if (line != null) {
                group.getChildren().add(IconKit.line(38.5, 7, 22, 50, line, 4.6));
                group.getChildren().add(IconKit.line(22, 50, 38.5, 93, line, 4.6));
            }
            group.getChildren().add(IconKit.line(38.5, 7, 22, 50, string, 2.2));
            group.getChildren().add(IconKit.line(22, 50, 38.5, 93, string, 2.2));
        } else {
            if (line != null) {
                group.getChildren().add(IconKit.line(38.5, 7, 38.5, 93, line, 4.6));
            }
            group.getChildren().add(IconKit.line(38.5, 7, 38.5, 93, string, 2.2));
        }
        group.getChildren().add(IconKit.path(BOW_LIMB, fill, line));
        group.getChildren().add(IconKit.path(BOW_GRIP, IconKit.darker(fill, 0.6), line));
        if (arrow) {
            group.getChildren().add(IconKit.path(ARROW_SHAFT, IconKit.tint(fill, 0.2), line));
            group.getChildren().add(IconKit.path(ARROW_FLETCHING, IconKit.darker(fill, 0.75), line));
            group.getChildren().add(IconKit.path(ARROW_HEAD, IconKit.tint(fill, 0.3), line));
        }
        return group;
    }

    /** Wheeled trebuchet. */
    static Group siege(Color fill, Color line) {
        Group group = new Group();
        Color dark = IconKit.darker(fill, 0.72);
        Color shade = IconKit.shade(fill, line);
        group.getChildren().add(IconKit.circle(24, 82, 12, dark, line));
        group.getChildren().add(IconKit.circle(76, 82, 12, dark, line));
        group.getChildren().add(IconKit.circle(24, 82, 3.5, shade, null));
        group.getChildren().add(IconKit.circle(76, 82, 3.5, shade, null));
        group.getChildren().add(IconKit.path(SIEGE_BASE, fill, line));
        group.getChildren().add(IconKit.path(SIEGE_FRAME, fill, line));
        group.getChildren().add(IconKit.path(SIEGE_WEIGHT, dark, line));
        group.getChildren().add(IconKit.path(SIEGE_ARM, IconKit.tint(fill, 0.15), line));
        group.getChildren().add(IconKit.circle(10, 9, 7.5, IconKit.darker(fill, 0.85), line));
        group.getChildren().add(IconKit.circle(50, 32, 3.2, shade, null));
        return group;
    }

    // ------------------------------------------------------------------ abilities

    /** Unit ability icon; an empty group for NONE. */
    static Group ability(Ability ability, Color fill, Color line) {
        switch (ability) {
            case MEDIC:
                return medic(fill, line);
            case MORALE_BOOST:
                return morale(fill, line);
            case MUSTER:
                return muster(fill, line);
            case SPY:
                return spy(fill, line);
            case TIGHT_BOND:
                return bond(fill, line);
            case SCORCH:
                return flame(fill, line);
            case SCORCH_CLOSE:
                return scorchRow(RowType.CLOSE, fill, line);
            case SCORCH_RANGED:
                return scorchRow(RowType.RANGED, fill, line);
            case SCORCH_SIEGE:
                return scorchRow(RowType.SIEGE, fill, line);
            case HORN:
                return horn(fill, line);
            case BERSERKER:
                return claws(fill, line);
            case MARDROEME:
                return mushroom(fill, IconKit.tint(fill, 0.35), IconKit.shade(fill, line), line);
            case AVENGER:
                return avenger(fill, line);
            default:
                return new Group();
        }
    }

    /** Healing cross. */
    static Group medic(Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(MEDIC_CROSS, fill, line));
        group.getChildren().add(IconKit.circle(50, 50, 7, IconKit.darker(fill, 0.7), null));
        return group;
    }

    /** Two upward chevrons. */
    static Group morale(Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(CHEVRON_BOTTOM, IconKit.darker(fill, 0.8), line));
        group.getChildren().add(IconKit.path(CHEVRON_TOP, fill, line));
        return group;
    }

    /** Three figures standing together. */
    static Group muster(Color fill, Color line) {
        Group group = new Group();
        Color back = IconKit.darker(fill, 0.75);
        group.getChildren().add(IconKit.path(FIGURE_LEFT, back, line));
        group.getChildren().add(IconKit.path(FIGURE_RIGHT, back, line));
        group.getChildren().add(IconKit.circle(25, 30, 11, back, line));
        group.getChildren().add(IconKit.circle(75, 30, 11, back, line));
        group.getChildren().add(IconKit.path(FIGURE_FRONT, fill, line));
        group.getChildren().add(IconKit.circle(50, 38, 14, fill, line));
        return group;
    }

    /** Watching eye. */
    static Group spy(Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(EYE, fill, line));
        group.getChildren().add(IconKit.circle(50, 50, 17, IconKit.darker(fill, 0.5), line));
        group.getChildren().add(IconKit.circle(50, 50, 8, IconKit.shade(fill, line), null));
        group.getChildren().add(IconKit.circle(56, 44, 4.5, IconKit.tint(fill, 0.6), null));
        return group;
    }

    /** Two interlocked rings. */
    static Group bond(Color fill, Color line) {
        Group group = new Group();
        Color second = IconKit.darker(fill, 0.85);
        group.getChildren().addAll(ring(37, 50, fill, line));
        group.getChildren().addAll(ring(63, 50, second, line));
        if (line != null) {
            group.getChildren().add(ringArc(37, 50, -80, 56, line, 16));
        }
        group.getChildren().add(ringArc(37, 50, -80, 56, fill, 9));
        return group;
    }

    private static Group ring(double cx, double cy, Color color, Color line) {
        Group group = new Group();
        if (line != null) {
            group.getChildren().add(ringCircle(cx, cy, line, 16));
        }
        group.getChildren().add(ringCircle(cx, cy, color, 9));
        return group;
    }

    private static Circle ringCircle(double cx, double cy, Color color, double width) {
        Circle circle = new Circle(cx, cy, 21);
        circle.setFill(null);
        circle.setStroke(color);
        circle.setStrokeWidth(width);
        return circle;
    }

    private static Arc ringArc(double cx, double cy, double start, double length, Color color, double width) {
        Arc arc = new Arc(cx, cy, 21, 21, start, length);
        arc.setType(ArcType.OPEN);
        arc.setFill(null);
        arc.setStroke(color);
        arc.setStrokeWidth(width);
        arc.setStrokeLineCap(StrokeLineCap.BUTT);
        return arc;
    }

    /** Flame (scorch). */
    static Group flame(Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(FLAME_OUTER, fill, line));
        group.getChildren().add(IconKit.flat(FLAME_INNER, IconKit.tint(fill, 0.55)));
        return group;
    }

    /** Coloured flame for the Scorch special card. */
    static Group flame(Color outer, Color inner, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(FLAME_OUTER, outer, line));
        group.getChildren().add(IconKit.flat(FLAME_INNER, inner));
        return group;
    }

    /** Flame with a small row badge (row-limited scorch). */
    static Group scorchRow(RowType row, Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.place(flame(fill, line), -2, 0, 0.84));
        Color disc = IconKit.shade(fill, line);
        Circle badge = new Circle(74, 74, 24, disc);
        badge.setStroke(fill);
        badge.setStrokeWidth(3.5);
        group.getChildren().add(badge);
        group.getChildren().add(IconKit.place(row(row, fill, line), 55, 55, 0.38));
        return group;
    }

    /** Curved war horn. */
    static Group horn(Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(HORN_BODY, fill, line));
        Color band = IconKit.darker(fill, 0.55);
        group.getChildren().add(IconKit.line(22, 37.9, 24.75, 53.5, band, 3));
        group.getChildren().add(IconKit.line(45, 46.2, 51.3, 71.3, band, 3));
        group.getChildren().add(IconKit.path(HORN_MOUTHPIECE, IconKit.darker(fill, 0.7), line));
        Ellipse bell = IconKit.ellipse(83, 53, 7, 18.5, -22.4, IconKit.shade(fill, line), null);
        bell.setStroke(IconKit.tint(fill, 0.25));
        bell.setStrokeWidth(3);
        group.getChildren().add(bell);
        return group;
    }

    /** Three claw slashes (berserker). */
    static Group claws(Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(CLAW_LEFT, fill, line));
        group.getChildren().add(IconKit.path(CLAW_MIDDLE, fill, line));
        group.getChildren().add(IconKit.path(CLAW_RIGHT, fill, line));
        return group;
    }

    /** Spotted mushroom (mardroeme). */
    static Group mushroom(Color cap, Color stem, Color spots, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(MUSHROOM_STEM, stem, line));
        group.getChildren().add(IconKit.path(MUSHROOM_CAP, cap, line));
        group.getChildren().add(IconKit.circle(30, 34, 7, spots, null));
        group.getChildren().add(IconKit.circle(55, 22, 6, spots, null));
        group.getChildren().add(IconKit.circle(74, 40, 5, spots, null));
        group.getChildren().add(IconKit.circle(47, 44, 4, spots, null));
        return group;
    }

    /** Circular arrow around a spark (summon avenger). */
    static Group avenger(Color fill, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(AVENGER_ARROW, fill, line));
        group.getChildren().add(IconKit.polygon(IconKit.star(50, 52, 4, 15, 5, -90), IconKit.tint(fill, 0.3), line));
        return group;
    }
}
