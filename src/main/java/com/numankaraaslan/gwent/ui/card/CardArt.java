package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.model.SpecialType;
import java.util.Random;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

/**
 * Procedural "portraits" (there is no card art): a faction-tinted gradient, a faint ornamental line pattern, a large
 * translucent emblem and a decorative initial. Everything varies deterministically with the card id (hue shift,
 * gradient and pattern angle, motif offset and rotation), so different cards never look identical but a card always
 * looks the same.
 */
final class CardArt {

    /** Pattern styles. */
    static final int HATCH = 0;
    static final int RAYS = 1;
    static final int RINGS = 2;
    static final int LATTICE = 3;

    private CardArt() {
    }

    /** Deterministic random source of a card / leader id. */
    static Random random(String id) {
        return new Random(id.hashCode() * 2654435761L + 97L);
    }

    /** Portrait of a unit, special or weather card in the rectangle (x, y, width, height), clipped to it. */
    static Group portrait(CardDef def, double x, double y, double width, double height) {
        Random random = random(def.id());
        Group group = def.isUnit() ? unitPortrait(def, random, x, y, width, height) : specialPortrait(def, random, x, y, width, height);
        group.setClip(new Rectangle(x, y, width, height));
        return group;
    }

    private static Group unitPortrait(CardDef def, Random random, double x, double y, double width, double height) {
        Faction faction = def.faction();
        double hueShift = (random.nextDouble() - 0.5) * 24;
        Color base = Palette.faction(faction).deriveColor(hueShift, 0.85 + random.nextDouble() * 0.3, 0.85 + random.nextDouble() * 0.3, 1);
        Color light = Palette.mix(base, Palette.factionLight(faction), 0.4);
        Color dark = Palette.factionDark(faction).deriveColor(hueShift, 1, 1, 1);
        Color accent = Palette.factionAccent(faction);
        Color glow = Palette.withOpacity(Palette.factionLight(faction), 0.28);
        if (def.hero()) {
            light = Palette.mix(light, Palette.GOLD, 0.35);
            accent = Palette.mix(accent, Palette.GOLD_BRIGHT, 0.6);
            glow = Palette.withOpacity(Palette.GOLD_BRIGHT, 0.38);
        }
        Group group = new Group();
        group.getChildren().add(background(x, y, width, height, light, base, dark, 70 + random.nextDouble() * 40));
        double cx = x + width * (0.56 + random.nextDouble() * 0.1);
        double cy = y + height * (0.44 + random.nextDouble() * 0.12);
        group.getChildren().add(glow(x, y, width, height, cx, cy, glow));
        int style = random.nextInt(4);
        double angle = 25 + random.nextDouble() * 130;
        double fx = x + width * random.nextDouble();
        double fy = y + height * (0.2 + random.nextDouble() * 0.6);
        group.getChildren().add(pattern(style, angle, fx, fy, x, y, width, height, Palette.withOpacity(accent, 0.11), Math.max(0.6, width * 0.006)));
        double emblemSize = Math.min(width, height) * (0.9 + random.nextDouble() * 0.3);
        double ex = x + width * (0.4 + random.nextDouble() * 0.3);
        double ey = y + height * (0.35 + random.nextDouble() * 0.3);
        Group emblem = IconKit.rotate(EmblemArt.faction(faction, Palette.withOpacity(accent, 0.13), null), (random.nextDouble() - 0.5) * 28);
        group.getChildren().add(IconKit.place(emblem, ex - emblemSize / 2, ey - emblemSize / 2, emblemSize / IconKit.BOX));
        if (def.hero()) {
            double laurelSize = Math.min(width * 0.8, height * 0.74);
            group.getChildren().add(IconKit.place(EmblemArt.laurel(Palette.withOpacity(Palette.GOLD_BRIGHT, 0.32), null), cx - laurelSize / 2, cy - laurelSize / 2, laurelSize / IconKit.BOX));
        }
        group.getChildren().add(initial(def.name(), cx, cy, height * 0.48, Palette.withOpacity(Palette.mix(accent, Color.WHITE, 0.25), def.hero() ? 0.52 : 0.36)));
        group.getChildren().add(vignette(x, y, width, height));
        return group;
    }

    private static Group specialPortrait(CardDef def, Random random, double x, double y, double width, double height) {
        SpecialType type = def.special();
        Color glowColor = Palette.weatherColor(type);
        Color dark = Palette.weatherDark(type);
        Color base = Palette.mix(dark, glowColor, 0.16).deriveColor((random.nextDouble() - 0.5) * 10, 1, 1, 1);
        Color light = Palette.mix(dark, glowColor, 0.34);
        Group group = new Group();
        group.getChildren().add(background(x, y, width, height, light, base, dark, 80 + random.nextDouble() * 20));
        double cx = x + width * 0.56;
        double cy = y + height * 0.5;
        group.getChildren().add(glow(x, y, width, height, cx, cy, Palette.withOpacity(glowColor, 0.42)));
        group.getChildren().add(pattern(patternOf(type), angleOf(type, random), cx, cy, x, y, width, height, Palette.withOpacity(glowColor, 0.13), Math.max(0.6, width * 0.006)));
        double echoSize = Math.max(width, height) * 1.15;
        Group echo = WeatherArt.special(type, null, null);
        echo.setOpacity(0.1);
        group.getChildren().add(IconKit.place(IconKit.rotate(echo, (random.nextDouble() - 0.5) * 30), cx - echoSize / 2 + width * 0.1, cy - echoSize / 2, echoSize / IconKit.BOX));
        double motifSize = Math.min(width, height) * 0.72;
        Group motif = WeatherArt.special(type, null, null);
        motif.setOpacity(0.92);
        group.getChildren().add(IconKit.place(motif, cx - motifSize / 2, cy - motifSize / 2, motifSize / IconKit.BOX));
        group.getChildren().add(vignette(x, y, width, height));
        return group;
    }

    /** Portrait of a leader card: royal gradient, sunburst, faction emblem, big gold initial and a crown on top. */
    static Group leaderPortrait(LeaderDef leader, double x, double y, double width, double height) {
        Random random = random(leader.id());
        Faction faction = leader.faction();
        Color base = Palette.faction(faction).deriveColor((random.nextDouble() - 0.5) * 14, 1.0, 0.95, 1);
        Color light = Palette.mix(Palette.mix(base, Palette.factionLight(faction), 0.35), Palette.GOLD, 0.22);
        Color dark = Palette.factionDark(faction);
        double cx = x + width * 0.5;
        double crownY = y + height * 0.17;
        Group group = new Group();
        group.getChildren().add(background(x, y, width, height, light, base, dark, 90));
        group.getChildren().add(glow(x, y, width, height, cx, crownY + height * 0.1, Palette.withOpacity(Palette.GOLD_BRIGHT, 0.34)));
        group.getChildren().add(pattern(RAYS, random.nextDouble() * 20, cx, crownY, x, y, width, height, Palette.withOpacity(Palette.GOLD_BRIGHT, 0.1), Math.max(0.6, width * 0.005)));
        double emblemSize = Math.min(width, height) * 1.02;
        double emblemY = y + height * 0.58;
        Group emblem = EmblemArt.faction(faction, Palette.withOpacity(Palette.factionAccent(faction), 0.17), null);
        group.getChildren().add(IconKit.place(emblem, cx - emblemSize / 2, emblemY - emblemSize / 2, emblemSize / IconKit.BOX));
        group.getChildren().add(initial(leader.name(), cx, y + height * 0.6, height * 0.42, Palette.withOpacity(Palette.GOLD_BRIGHT, 0.5)));
        double crownSize = width * 0.34;
        group.getChildren().add(IconKit.place(EmblemArt.crown(Palette.GOLD, Palette.INK), cx - crownSize / 2, crownY - crownSize / 2, crownSize / IconKit.BOX));
        group.getChildren().add(vignette(x, y, width, height));
        group.setClip(new Rectangle(x, y, width, height));
        return group;
    }

    // ------------------------------------------------------------------ layers

    /** Base gradient from {@code light} through {@code base} to {@code dark} along {@code angle} degrees. */
    static Rectangle background(double x, double y, double width, double height, Color light, Color base, Color dark, double angle) {
        double radians = Math.toRadians(angle);
        double dx = 0.5 * Math.cos(radians);
        double dy = 0.5 * Math.sin(radians);
        LinearGradient paint = new LinearGradient(0.5 - dx, 0.5 - dy, 0.5 + dx, 0.5 + dy, true, CycleMethod.NO_CYCLE, new Stop(0, light), new Stop(0.55, base), new Stop(1, dark));
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setFill(paint);
        return rectangle;
    }

    /** Soft radial light around (cx, cy). */
    static Rectangle glow(double x, double y, double width, double height, double cx, double cy, Color color) {
        double rx = (cx - x) / width;
        double ry = (cy - y) / height;
        RadialGradient paint = new RadialGradient(0, 0, rx, ry, 0.55, true, CycleMethod.NO_CYCLE, new Stop(0, color), new Stop(1, Palette.withOpacity(color, 0)));
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setFill(paint);
        return rectangle;
    }

    /** Darkened edges. */
    static Rectangle vignette(double x, double y, double width, double height) {
        RadialGradient paint = new RadialGradient(0, 0, 0.55, 0.42, 0.78, true, CycleMethod.NO_CYCLE, new Stop(0, Color.TRANSPARENT), new Stop(0.6, Palette.withOpacity(Color.BLACK, 0.06)), new Stop(1, Palette.withOpacity(Color.BLACK, 0.55)));
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setFill(paint);
        return rectangle;
    }

    /** Big decorative initial letter centred on (cx, cy). */
    static Text initial(String name, double cx, double cy, double size, Color color) {
        Text text = new Text(initialOf(name));
        text.setFont(CardFonts.display(size));
        text.setFill(color);
        text.setTextOrigin(VPos.BASELINE);
        double width = text.getLayoutBounds().getWidth();
        text.setX(cx - width / 2);
        text.setY(cy + size * 0.34);
        return text;
    }

    private static String initialOf(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetter(c)) {
                return String.valueOf(Character.toUpperCase(c));
            }
        }
        return "";
    }

    private static int patternOf(SpecialType type) {
        switch (type) {
            case BITING_FROST:
            case DECOY:
                return LATTICE;
            case IMPENETRABLE_FOG:
            case TORRENTIAL_RAIN:
                return HATCH;
            case COMMANDERS_HORN:
            case MARDROEME:
                return RINGS;
            default:
                return RAYS;
        }
    }

    private static double angleOf(SpecialType type, Random random) {
        switch (type) {
            case IMPENETRABLE_FOG:
                return 4 * (random.nextDouble() - 0.5);
            case TORRENTIAL_RAIN:
                return 105 + random.nextDouble() * 10;
            case BITING_FROST:
                return 60;
            default:
                return 30 + random.nextDouble() * 60;
        }
    }

    // ------------------------------------------------------------------ line patterns

    /**
     * Thin ornamental lines as ONE path (cheap to render): parallel hatching at {@code angle}, rays or rings around
     * (fx, fy), or a diamond lattice. Lines run past the rectangle, so the caller should clip.
     */
    static SVGPath pattern(int style, double angle, double fx, double fy, double x, double y, double width, double height, Color color, double strokeWidth) {
        StringBuilder data = new StringBuilder();
        double cx = x + width / 2;
        double cy = y + height / 2;
        double reach = Math.hypot(width, height);
        switch (style) {
            case HATCH:
                hatch(data, cx, cy, reach, angle, Math.max(3, width * 0.085));
                break;
            case RAYS:
                rays(data, fx, fy, reach * 1.3, 18, angle, reach * 0.06);
                break;
            case RINGS:
                rings(data, fx, fy, reach * 1.2, Math.max(3, width * 0.075));
                break;
            default:
                hatch(data, cx, cy, reach, angle, Math.max(4, width * 0.11));
                hatch(data, cx, cy, reach, 180 - angle, Math.max(4, width * 0.11));
                break;
        }
        SVGPath path = new SVGPath();
        path.setContent(data.toString());
        path.setFill(null);
        path.setStroke(color);
        path.setStrokeWidth(strokeWidth);
        return path;
    }

    private static void hatch(StringBuilder data, double cx, double cy, double reach, double angle, double spacing) {
        double radians = Math.toRadians(angle);
        double dx = Math.cos(radians);
        double dy = Math.sin(radians);
        double half = reach / 2;
        for (double k = -half; k <= half; k += spacing) {
            double ox = cx - dy * k;
            double oy = cy + dx * k;
            data.append('M').append(n(ox - dx * half)).append(',').append(n(oy - dy * half)).append(" L").append(n(ox + dx * half)).append(',').append(n(oy + dy * half)).append(' ');
        }
    }

    private static void rays(StringBuilder data, double fx, double fy, double length, int count, double offset, double start) {
        for (int i = 0; i < count; i++) {
            double radians = Math.toRadians(offset + i * 360.0 / count);
            double dx = Math.cos(radians);
            double dy = Math.sin(radians);
            data.append('M').append(n(fx + dx * start)).append(',').append(n(fy + dy * start)).append(" L").append(n(fx + dx * length)).append(',').append(n(fy + dy * length)).append(' ');
        }
    }

    private static void rings(StringBuilder data, double fx, double fy, double reach, double step) {
        for (double r = step; r < reach; r += step) {
            data.append('M').append(n(fx - r)).append(',').append(n(fy)).append(" A").append(n(r)).append(',').append(n(r)).append(" 0 1,0 ").append(n(fx + r)).append(',').append(n(fy)).append(" A").append(n(r)).append(',').append(n(r)).append(" 0 1,0 ").append(n(fx - r)).append(',').append(n(fy)).append(' ');
        }
    }

    private static String n(double value) {
        return Double.toString(Math.round(value * 10) / 10.0);
    }
}
