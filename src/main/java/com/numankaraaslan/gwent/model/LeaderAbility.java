package com.numankaraaslan.gwent.model;

/**
 * Leader card abilities. Active abilities can be used once per game (using one takes your turn).
 * Passive abilities work automatically for the whole game.
 */
public enum LeaderAbility
{
    // Northern Realms (Foltest)
    FOG_FROM_DECK(false), CLEAR_WEATHER(false), HORN_SIEGE(false), SCORCH_SIEGE(false), SCORCH_RANGED(false),
    // Nilfgaard (Emhyr)
    RAIN_FROM_DECK(false), LOOK_AT_HAND(false), CANCEL_LEADER(true), DRAW_FROM_OPPONENT_DISCARD(false), RANDOM_MEDIC(true),
    // Scoia'tael (Francesca)
    SCORCH_CLOSE(false), HORN_RANGED(false), EXTRA_CARD(true), FROST_FROM_DECK(false), OPTIMIZE_AGILE(false),
    // Monsters (Eredin)
    HORN_CLOSE(false), RESTORE_FROM_DISCARD(false), DISCARD_2_DRAW_1(false), WEATHER_FROM_DECK(false), DOUBLE_SPIES(true),
    // Skellige
    SHUFFLE_GRAVEYARDS(false), HALF_WEATHER(true);

    private final boolean passive;

    LeaderAbility(boolean passive)
    {
        this.passive = passive;
    }

    /** Passive abilities are always on (unless cancelled) and can't be activated. */
    public boolean isPassive()
    {
        return passive;
    }

    /** Row doubled by a horn-type leader ability, or null. */
    public RowType hornRow()
    {
        if (this == HORN_CLOSE)
        {
            return RowType.CLOSE;
        }
        if (this == HORN_RANGED)
        {
            return RowType.RANGED;
        }
        if (this == HORN_SIEGE)
        {
            return RowType.SIEGE;
        }
        return null;
    }

    /** Enemy row hit by a scorch-type leader ability, or null. */
    public RowType scorchRow()
    {
        if (this == SCORCH_CLOSE)
        {
            return RowType.CLOSE;
        }
        if (this == SCORCH_RANGED)
        {
            return RowType.RANGED;
        }
        if (this == SCORCH_SIEGE)
        {
            return RowType.SIEGE;
        }
        return null;
    }

    /** Weather card a "pick X from your deck" leader plays, or null. */
    public SpecialType weatherFromDeck()
    {
        if (this == FOG_FROM_DECK)
        {
            return SpecialType.IMPENETRABLE_FOG;
        }
        if (this == RAIN_FROM_DECK)
        {
            return SpecialType.TORRENTIAL_RAIN;
        }
        if (this == FROST_FROM_DECK)
        {
            return SpecialType.BITING_FROST;
        }
        return null;
    }
}
