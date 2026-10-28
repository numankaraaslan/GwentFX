package com.numankaraaslan.gwent.collector;

import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Difficulty;

/**
 * How hard a card is to get in Collector Mode. Starter cards are owned from the start; every win unlocks one card of the
 * tier that matches the difficulty of the game: Easy, Normal or Hard.
 */
public enum Tier {
    STARTER("Starter", "Yours from the start: units of strength 4 or less (except spies, Medics, Scorch and Horn units) and every weather card."),
    EASY("Easy", "Won on Easy: units of strength 5 to 7."),
    NORMAL("Normal", "Won on Normal: units of strength 8 or more, spies, Medics, Scorch and Horn units, and the special cards."),
    HARD("Hard", "Won on Hard: the heroes.");

    /** Units up to this strength are starter cards (unless their ability makes them Normal). */
    private static final int STARTER_MAX_STRENGTH = 4;
    /** Units from this strength on are Normal cards. */
    private static final int NORMAL_MIN_STRENGTH = 8;

    private final String displayName;
    private final String description;

    Tier(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    /** Which cards belong to this tier, in one sentence (it matches {@link #of}). */
    public String description() {
        return description;
    }

    /**
     * The tier of a single card. Heroes are Hard. Weather cards are starter cards, so every leader ability works from the
     * first game. The other special cards are Normal, and so are spies, medics, units with a Scorch or Horn ability and
     * units of strength 8 or more. The remaining units are starter cards up to strength 4 and Easy above that.
     */
    public static Tier of(CardDef card) {
        if (card.hero()) {
            return HARD;
        }
        if (card.isWeather()) {
            return STARTER;
        }
        if (!card.isUnit() || hasStrongAbility(card) || card.strength() >= NORMAL_MIN_STRENGTH) {
            return NORMAL;
        }
        if (card.strength() <= STARTER_MAX_STRENGTH) {
            return STARTER;
        }
        return EASY;
    }

    /** The tier a win at this difficulty unlocks a card from. */
    public static Tier rewardedBy(Difficulty difficulty) {
        switch (difficulty) {
            case EASY:
                return EASY;
            case HARD:
                return HARD;
            default:
                return NORMAL;
        }
    }

    /** Spies and medics win cards, and Scorch and Horn swing whole rows, whatever the strength of the unit. */
    private static boolean hasStrongAbility(CardDef card) {
        Ability ability = card.ability();
        return ability == Ability.SPY || ability == Ability.MEDIC || ability == Ability.HORN || ability == Ability.SCORCH || ability.scorchRow() != null;
    }
}
