package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.model.SpecialType;
import javafx.scene.Group;
import javafx.scene.paint.Color;

/**
 * Vector art of the special and weather card icons, drawn in the 100 x 100 box of {@link IconKit}.
 * With {@code color == null} every icon uses its thematic colours (icy frost, golden sun...), otherwise it is drawn
 * in shades of the given colour.
 */
final class WeatherArt {

    private static final String CLOUD = "M22,60 C10,60 4,52 4,44 C4,35 11,28 20,28 C22,17 32,10 44,10 C56,10 65,17 68,26 C70,25 72,25 74,25 C86,25 96,34 96,44 C96,53 88,60 78,60 Z";
    private static final String CLOUD_SHADE = "M14,52 C20,57 30,57 38,54 C46,58 58,58 66,53 C74,57 84,56 90,50 C88,56 84,58 78,58 L22,58 C18,58 16,56 14,52 Z";
    private static final String BOLT = "M54,50 L36,76 L50,76 L40,98 L70,66 L56,66 L66,50 Z";

    private static final String FOG_TOP = "M14,28 C26,20 38,20 50,28 C62,36 74,36 86,28";
    private static final String FOG_MIDDLE = "M8,52 C20,44 32,44 44,52 C56,60 68,60 80,52 C84,49 88,48 92,48";
    private static final String FOG_BOTTOM = "M18,76 C30,68 42,68 54,76 C66,84 78,84 90,76";

    private static final String DECOY_TUFT_LEFT = "M10,38 L0,32 L5,44 L0,56 L10,50 Z";
    private static final String DECOY_TUFT_RIGHT = "M90,38 L100,32 L95,44 L100,56 L90,50 Z";
    private static final String DECOY_BAR = "M8,40 L92,40 L92,48 L8,48 Z";
    private static final String DECOY_POST = "M46,44 L54,44 L54,98 L46,98 Z";
    private static final String DECOY_TUNIC = "M34,42 L66,42 L72,80 L66,76 L60,82 L54,76 L50,82 L46,76 L40,82 L34,76 L28,80 Z";
    private static final String DECOY_BRIM = "M28,14 L72,14 L72,19 L28,19 Z";
    private static final String DECOY_HAT = "M38,14 L45,1 L57,4 L62,14 Z";

    private WeatherArt() {
    }

    /** Icon of a special or weather card; an empty group for NONE. */
    static Group special(SpecialType type, Color color, Color line) {
        switch (type) {
            case COMMANDERS_HORN:
                return UnitArt.horn(pick(color, type), line);
            case DECOY:
                return decoy(color, line);
            case SCORCH:
                return scorch(color, line);
            case MARDROEME:
                return mardroeme(color, line);
            case BITING_FROST:
                return snowflake(pick(color, type), line);
            case IMPENETRABLE_FOG:
                return fog(pick(color, type), line);
            case TORRENTIAL_RAIN:
                return rain(color, line);
            case SKELLIGE_STORM:
                return storm(color, line);
            case CLEAR_WEATHER:
                return sun(color, line);
            default:
                return new Group();
        }
    }

    private static Color pick(Color color, SpecialType type) {
        return color != null ? color : Palette.weatherColor(type);
    }

    /** Straw scarecrow (decoy). */
    static Group decoy(Color color, Color line) {
        Color straw = color != null ? color : Color.web("#dcc27e");
        Color tunic = color != null ? IconKit.darker(color, 0.7) : Color.web("#8c5b2e");
        Color hat = color != null ? IconKit.darker(color, 0.55) : Color.web("#5e4128");
        Color sack = color != null ? IconKit.tint(color, 0.2) : Color.web("#d6bd8a");
        Color wood = color != null ? IconKit.darker(color, 0.62) : Color.web("#6e4c2c");
        Group group = new Group();
        group.getChildren().add(IconKit.path(DECOY_TUFT_LEFT, straw, line));
        group.getChildren().add(IconKit.path(DECOY_TUFT_RIGHT, straw, line));
        group.getChildren().add(IconKit.path(DECOY_BAR, wood, line));
        group.getChildren().add(IconKit.path(DECOY_POST, wood, line));
        group.getChildren().add(IconKit.path(DECOY_TUNIC, tunic, line));
        group.getChildren().add(IconKit.circle(50, 26, 13, sack, line));
        Color face = IconKit.shade(sack, line);
        group.getChildren().add(IconKit.circle(45, 27, 2.4, face, null));
        group.getChildren().add(IconKit.circle(55, 27, 2.4, face, null));
        group.getChildren().add(IconKit.line(44, 33, 56, 33, face, 1.6));
        group.getChildren().add(IconKit.path(DECOY_BRIM, hat, line));
        group.getChildren().add(IconKit.path(DECOY_HAT, hat, line));
        return group;
    }

    /** Flame (scorch special card). */
    static Group scorch(Color color, Color line) {
        if (color != null) {
            return UnitArt.flame(color, line);
        }
        return UnitArt.flame(Color.web("#f0742a"), Color.web("#ffd77a"), line);
    }

    /** Red spotted mushroom. */
    static Group mardroeme(Color color, Color line) {
        if (color != null) {
            return UnitArt.mushroom(color, IconKit.tint(color, 0.35), IconKit.shade(color, line), line);
        }
        return UnitArt.mushroom(Color.web("#c23a2c"), Color.web("#eadcbe"), Color.web("#f6ecd8"), line);
    }

    /** Six-armed snowflake (biting frost). */
    static Group snowflake(Color color, Color line) {
        IconKit.LineArt art = new IconKit.LineArt(color, line, 6);
        for (int i = 0; i < 6; i++) {
            double angle = -90 + i * 60;
            art.line(50, 50, IconKit.px(50, 44, angle), IconKit.py(50, 44, angle));
            double bx = IconKit.px(50, 27, angle);
            double by = IconKit.py(50, 27, angle);
            art.line(bx, by, bx + 15 * Math.cos(Math.toRadians(angle - 45)), by + 15 * Math.sin(Math.toRadians(angle - 45)));
            art.line(bx, by, bx + 15 * Math.cos(Math.toRadians(angle + 45)), by + 15 * Math.sin(Math.toRadians(angle + 45)));
        }
        Group group = art.build();
        group.getChildren().add(IconKit.polygon(IconKit.star(50, 50, 3, 9.5, 9.5, -90), IconKit.tint(color, 0.3), line));
        return group;
    }

    /** Drifting fog streaks (impenetrable fog). */
    static Group fog(Color color, Color line) {
        IconKit.LineArt back = new IconKit.LineArt(IconKit.darker(color, 0.8), line, 8);
        back.path(FOG_MIDDLE);
        IconKit.LineArt front = new IconKit.LineArt(color, line, 8);
        front.path(FOG_TOP);
        front.path(FOG_BOTTOM);
        return new Group(back.build(), front.build());
    }

    /** Cloud (shared by rain and storm), occupying the upper 60 units of the box. */
    static Group cloud(Color color, Color line) {
        Group group = new Group();
        group.getChildren().add(IconKit.path(CLOUD, color, line));
        group.getChildren().add(IconKit.flat(CLOUD_SHADE, IconKit.darker(color, 0.78)));
        return group;
    }

    /** Cloud with slanted rain (torrential rain). */
    static Group rain(Color color, Color line) {
        Color cloudColor = color != null ? color : Color.web("#9fadbf");
        Color dropColor = color != null ? IconKit.tint(color, 0.3) : Color.web("#86c4ff");
        IconKit.LineArt drops = new IconKit.LineArt(dropColor, line, 5);
        drops.line(24, 70, 18, 90);
        drops.line(42, 70, 36, 90);
        drops.line(60, 70, 54, 90);
        drops.line(78, 70, 72, 90);
        Group group = new Group();
        group.getChildren().add(drops.build());
        group.getChildren().add(cloud(cloudColor, line));
        return group;
    }

    /** Dark cloud with a lightning bolt (Skellige storm). */
    static Group storm(Color color, Color line) {
        Color cloudColor = color != null ? IconKit.darker(color, 0.8) : Color.web("#7f7596");
        Color boltColor = color != null ? IconKit.tint(color, 0.45) : Color.web("#ffd84a");
        Group group = new Group();
        group.getChildren().add(cloud(cloudColor, line));
        group.getChildren().add(IconKit.path(BOLT, boltColor, line));
        return group;
    }

    /** Sun with eight broad rays (clear weather). */
    static Group sun(Color color, Color line) {
        Color rayColor = color != null ? IconKit.darker(color, 0.85) : Color.web("#ffb238");
        Color discColor = color != null ? color : Color.web("#ffd86a");
        Group group = new Group();
        for (int i = 0; i < 8; i++) {
            double angle = -90 + i * 45;
            double[] ray = {IconKit.px(50, 46, angle), IconKit.py(50, 46, angle), IconKit.px(50, 25, angle + 17), IconKit.py(50, 25, angle + 17), IconKit.px(50, 25, angle - 17), IconKit.py(50, 25, angle - 17)};
            group.getChildren().add(IconKit.polygon(ray, rayColor, line));
        }
        group.getChildren().add(IconKit.circle(50, 50, 22, discColor, line));
        group.getChildren().add(IconKit.circle(50, 50, 14, IconKit.tint(discColor, 0.35), null));
        return group;
    }
}
