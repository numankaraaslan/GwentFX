package com.numankaraaslan.gwent.ui.card;

import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.SpecialType;
import javafx.scene.paint.Color;

/**
 * Colours used by the procedural card art and anything faction-themed (Witcher palette: dark leather, gold, muted
 * faction colours).
 */
public final class Palette {

    /** Main gold (hero frames, ornaments, default icon accents). */
    public static final Color GOLD = Color.web("#d4af5a");
    /** Strength above the printed value. */
    public static final Color BUFF = Color.web("#3fbf4a");
    /** Strength below the printed value. */
    public static final Color DEBUFF = Color.web("#e0473a");

    /** Darker buff green, readable on the light parchment strength disc. */
    public static final Color BUFF_DARK = Color.web("#1f8a2e");
    /** Darker debuff red, readable on the light parchment strength disc. */
    public static final Color DEBUFF_DARK = Color.web("#b8231a");

    /** Bright gold highlight. */
    public static final Color GOLD_BRIGHT = Color.web("#f3d690");
    /** Dark, burnished gold. */
    public static final Color GOLD_DARK = Color.web("#8a6d2f");
    /** Default light colour of vector icons on dark backgrounds. */
    public static final Color ICON = Color.web("#ead39a");
    /** Near-black brown used for outlines and ink. */
    public static final Color INK = Color.web("#1a120b");
    /** Light parchment (strength disc of normal units, name banners). */
    public static final Color PARCHMENT = Color.web("#efe2c2");
    /** Darker parchment (gradients on parchment surfaces). */
    public static final Color PARCHMENT_DARK = Color.web("#c9b48a");
    /** Dark leather (card backs, frames). */
    public static final Color LEATHER = Color.web("#3a2a1c");
    /** Very dark leather. */
    public static final Color LEATHER_DARK = Color.web("#1c140d");
    /** Steel / silver frame of special and weather cards. */
    public static final Color SILVER = Color.web("#c3c7cc");
    /** Dark steel. */
    public static final Color SILVER_DARK = Color.web("#5d636b");
    /** Text colour on dark surfaces. */
    public static final Color TEXT = Color.web("#f6efe0");
    /** Dimmed text colour on dark surfaces. */
    public static final Color TEXT_DIM = Color.web("#cfc1a4");
    /** Ruby red (lit gems, stamps). */
    public static final Color RUBY = Color.web("#c8182a");

    private Palette() {
    }

    /** Main faction colour. */
    public static Color faction(Faction faction) {
        switch (faction) {
            case NORTHERN_REALMS:
                return Color.web("#2f5f9e");
            case NILFGAARD:
                return Color.web("#34322e");
            case SCOIATAEL:
                return Color.web("#3f7a35");
            case MONSTERS:
                return Color.web("#7a2420");
            case SKELLIGE:
                return Color.web("#5a3d7a");
            default:
                return Color.web("#6e5a3e");
        }
    }

    /** Darker shade of the faction colour (gradients, borders). */
    public static Color factionDark(Faction faction) {
        switch (faction) {
            case NORTHERN_REALMS:
                return Color.web("#162d4d");
            case NILFGAARD:
                return Color.web("#121110");
            case SCOIATAEL:
                return Color.web("#1b3a17");
            case MONSTERS:
                return Color.web("#3a0f0d");
            case SKELLIGE:
                return Color.web("#2a1a3b");
            default:
                return Color.web("#33291b");
        }
    }

    /** Lighter shade of the faction colour (highlights, frame bevels). */
    public static Color factionLight(Faction faction) {
        switch (faction) {
            case NORTHERN_REALMS:
                return Color.web("#7aa6dc");
            case NILFGAARD:
                return Color.web("#7a7468");
            case SCOIATAEL:
                return Color.web("#88bd6a");
            case MONSTERS:
                return Color.web("#c25546");
            case SKELLIGE:
                return Color.web("#9a7cc0");
            default:
                return Color.web("#b29468");
        }
    }

    /**
     * Secondary heraldic colour of the faction: silver for the Northern Realms, gold for Nilfgaard, leaf green for the
     * Scoia'tael, bone for the Monsters, sea teal for Skellige and bronze for neutral cards.
     */
    public static Color factionAccent(Faction faction) {
        switch (faction) {
            case NORTHERN_REALMS:
                return Color.web("#cdd8e6");
            case NILFGAARD:
                return Color.web("#d9b24c");
            case SCOIATAEL:
                return Color.web("#b9d27a");
            case MONSTERS:
                return Color.web("#dccbb0");
            case SKELLIGE:
                return Color.web("#4fb3ad");
            default:
                return Color.web("#c8975a");
        }
    }

    /** Main colour of special cards (steel blue-grey). */
    public static Color specialColor() {
        return Color.web("#4a5562");
    }

    /** Darker colour of special cards. */
    public static Color specialDark() {
        return Color.web("#1d232b");
    }

    /** Thematic colour of a special or weather effect (icons, portrait tint). */
    public static Color weatherColor(SpecialType type) {
        switch (type) {
            case BITING_FROST:
                return Color.web("#bfe6ff");
            case IMPENETRABLE_FOG:
                return Color.web("#c9ccd2");
            case TORRENTIAL_RAIN:
                return Color.web("#7fb2e6");
            case SKELLIGE_STORM:
                return Color.web("#b9a6e8");
            case CLEAR_WEATHER:
                return Color.web("#ffd46a");
            case SCORCH:
                return Color.web("#ff8a3a");
            case MARDROEME:
                return Color.web("#e0705a");
            case DECOY:
                return Color.web("#d9bf7c");
            case COMMANDERS_HORN:
                return Color.web("#e8c374");
            default:
                return ICON;
        }
    }

    /** Dark background colour of a special or weather card portrait. */
    public static Color weatherDark(SpecialType type) {
        switch (type) {
            case BITING_FROST:
                return Color.web("#13283a");
            case IMPENETRABLE_FOG:
                return Color.web("#24272c");
            case TORRENTIAL_RAIN:
                return Color.web("#132235");
            case SKELLIGE_STORM:
                return Color.web("#1e1631");
            case CLEAR_WEATHER:
                return Color.web("#3a2c10");
            case SCORCH:
                return Color.web("#3a1208");
            case MARDROEME:
                return Color.web("#2d1512");
            case DECOY:
                return Color.web("#2e2614");
            case COMMANDERS_HORN:
                return Color.web("#2e2210");
            default:
                return specialDark();
        }
    }

    /** Colour with a different opacity. */
    public static Color withOpacity(Color color, double opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(1, opacity)));
    }

    /** Linear mix of two colours: {@code t = 0} gives {@code a}, {@code t = 1} gives {@code b}. */
    public static Color mix(Color a, Color b, double t) {
        double k = Math.max(0, Math.min(1, t));
        return new Color(a.getRed() + (b.getRed() - a.getRed()) * k, a.getGreen() + (b.getGreen() - a.getGreen()) * k, a.getBlue() + (b.getBlue() - a.getBlue()) * k, a.getOpacity() + (b.getOpacity() - a.getOpacity()) * k);
    }
}
