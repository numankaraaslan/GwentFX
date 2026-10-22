package com.numankaraaslan.gwent.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** What a SPECIAL or WEATHER card does. Units use NONE. */
public enum SpecialType
{
    NONE("", ""), COMMANDERS_HORN("Commander's Horn", "Doubles the strength of all unit cards in that row. Limited to 1 per row."), DECOY("Decoy", "Swap with a card on the battlefield to return it to your hand."), SCORCH("Scorch", "Discard after playing. Kills the strongest card(s) on the battlefield."), MARDROEME("Mardroeme", "Triggers transformation of all Berserker cards on the same row."), BITING_FROST("Biting Frost", "Sets the strength of all Close Combat cards to 1 for both players."), IMPENETRABLE_FOG("Impenetrable Fog", "Sets the strength of all Ranged Combat cards to 1 for both players."), TORRENTIAL_RAIN("Torrential Rain", "Sets the strength of all Siege Combat cards to 1 for both players."), SKELLIGE_STORM("Skellige Storm", "Reduces the strength of all Ranged and Siege Combat cards to 1 for both players."), CLEAR_WEATHER("Clear Weather", "Removes all Weather Card effects (Biting Frost, Impenetrable Fog, Torrential Rain and Skellige Storm).");

    private final String displayName;
    private final String description;

    SpecialType(String displayName, String description)
    {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName()
    {
        return displayName;
    }

    public String description()
    {
        return description;
    }

    /** True for the weather effects that weaken rows (not Clear Weather). */
    public boolean isBadWeather()
    {
        return this == BITING_FROST || this == IMPENETRABLE_FOG || this == TORRENTIAL_RAIN || this == SKELLIGE_STORM;
    }

    /** Rows weakened by this weather card (on both sides). Empty for everything else. */
    public Set<RowType> affectedRows()
    {
        switch (this)
        {
        case BITING_FROST:
            return Collections.unmodifiableSet(EnumSet.of(RowType.CLOSE));
        case IMPENETRABLE_FOG:
            return Collections.unmodifiableSet(EnumSet.of(RowType.RANGED));
        case TORRENTIAL_RAIN:
            return Collections.unmodifiableSet(EnumSet.of(RowType.SIEGE));
        case SKELLIGE_STORM:
            return Collections.unmodifiableSet(EnumSet.of(RowType.RANGED, RowType.SIEGE));
        default:
            return Collections.unmodifiableSet(EnumSet.noneOf(RowType.class));
        }
    }
}
