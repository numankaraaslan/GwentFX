package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;

/**
 * Vector icons (no image files, no font glyphs). Every method returns a NEW node of exactly {@code size} x
 * {@code size} (a {@link Pane} with style class {@code gw-icon}); "empty" icons are 0 x 0 regions.
 * The default colours are light (gold / parchment) with a thin dark outline, so the icons read well on dark
 * backgrounds from 16 to 64 px and beyond. Overloads take an explicit main colour.
 */
public final class Icons {

    private Icons() {
    }

    /** Sword / bow / catapult in the default icon colour. */
    public static Node row(RowType row, double size) {
        return row(row, size, Palette.ICON);
    }

    /** Sword / bow / catapult in the given colour. */
    public static Node row(RowType row, double size, Color color) {
        if (row == null) {
            return empty();
        }
        return wrap(UnitArt.row(row, color, Palette.INK), size);
    }

    /** Agile (sword + bow crossed) in the default icon colour. */
    public static Node agile(double size) {
        return agile(size, Palette.ICON);
    }

    /** Agile (sword + bow crossed) in the given colour. */
    public static Node agile(double size, Color color) {
        return wrap(UnitArt.agile(color, Palette.INK), size);
    }

    /** Unit ability icon in the default icon colour; an empty node for NONE. */
    public static Node ability(Ability ability, double size) {
        return ability(ability, size, Palette.ICON);
    }

    /** Unit ability icon in the given colour; an empty node for NONE. */
    public static Node ability(Ability ability, double size, Color color) {
        if (ability == null || ability == Ability.NONE) {
            return empty();
        }
        return wrap(UnitArt.ability(ability, color, Palette.INK), size);
    }

    /**
     * Special / weather card icon (horn, decoy, scorch, mardroeme, frost, fog, rain, storm, clear sky) in its thematic
     * colours; an empty node for NONE.
     */
    public static Node special(SpecialType type, double size) {
        if (type == null || type == SpecialType.NONE) {
            return empty();
        }
        return wrap(WeatherArt.special(type, null, Palette.INK), size);
    }

    /** Special / weather card icon drawn in shades of the given colour; an empty node for NONE. */
    public static Node special(SpecialType type, double size, Color color) {
        if (type == null || type == SpecialType.NONE) {
            return empty();
        }
        return wrap(WeatherArt.special(type, color, Palette.INK), size);
    }

    /** Faction emblem in the default icon colour. */
    public static Node faction(Faction faction, double size) {
        return faction(faction, size, Palette.ICON);
    }

    /**
     * Faction emblem in the given colour: lily shield (Northern Realms), great sun (Nilfgaard), leaf (Scoia'tael),
     * horned skull (Monsters), crossed axes (Skellige), compass star (Neutral).
     */
    public static Node faction(Faction faction, double size, Color color) {
        return wrap(EmblemArt.faction(faction == null ? Faction.NEUTRAL : faction, color, Palette.INK), size);
    }

    /** Hero marker: a gold star. */
    public static Node hero(double size) {
        return hero(size, Palette.GOLD);
    }

    /** Hero marker: a star in the given colour. */
    public static Node hero(double size, Color color) {
        return wrap(EmblemArt.heroStar(color, Palette.INK), size);
    }

    /** Round-life gem: lit (ruby red) or lost (dark grey glass), in a gold setting. */
    public static Node gem(boolean lit, double size) {
        return wrap(EmblemArt.gem(lit), size);
    }

    /** Gold crown (leader, round winner, leading score). */
    public static Node crown(double size) {
        return crown(size, Palette.GOLD);
    }

    /** Crown in the given colour. */
    public static Node crown(double size, Color color) {
        return wrap(EmblemArt.crown(color, Palette.INK), size);
    }

    /** Card-stack icon (hand count, deck size) in parchment colour. */
    public static Node cards(double size) {
        return cards(size, Palette.PARCHMENT);
    }

    /** Card-stack icon in the given colour. */
    public static Node cards(double size, Color color) {
        return wrap(EmblemArt.cards(color, Palette.INK), size);
    }

    /** Gold laurel wreath (decoration for heroes, round winners). */
    public static Node laurel(double size) {
        return laurel(size, Palette.GOLD);
    }

    /** Laurel wreath in the given colour. */
    public static Node laurel(double size, Color color) {
        return wrap(EmblemArt.laurel(color, Palette.INK), size);
    }

    /** Weather icon of a row: frost for close combat, fog for ranged, rain for siege (thematic colours). */
    public static Node weatherFor(RowType row, double size) {
        switch (row) {
            case CLOSE:
                return special(SpecialType.BITING_FROST, size);
            case RANGED:
                return special(SpecialType.IMPENETRABLE_FOG, size);
            default:
                return special(SpecialType.TORRENTIAL_RAIN, size);
        }
    }

    /** A 0 x 0 placeholder (for NONE). */
    public static Node empty() {
        Region region = new Region();
        region.setMinSize(0, 0);
        region.setPrefSize(0, 0);
        region.setMaxSize(0, 0);
        region.setMouseTransparent(true);
        return region;
    }

    /** Scales art from the 100-unit drawing box to a fixed-size pane. */
    private static Node wrap(Group art, double size) {
        Group scaled = new Group(art);
        scaled.getTransforms().add(new Scale(size / IconKit.BOX, size / IconKit.BOX));
        Pane pane = new Pane(scaled);
        pane.getStyleClass().add("gw-icon");
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        return pane;
    }
}
