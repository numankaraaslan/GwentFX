package com.numankaraaslan.gwent.model;

/**
 * Abilities of unit cards. Agile is not an ability here: an agile unit simply has two rows (CLOSE and RANGED).
 */
public enum Ability
{
    NONE("", ""), MEDIC("Medic", "Choose one card from your discard pile and play it instantly (no Heroes or Special Cards)."), MORALE_BOOST("Morale Boost", "Adds +1 to all units in the row (excluding itself)."), MUSTER("Muster", "Find any cards with the same name in your deck and hand and play them instantly."), SPY("Spy", "Place on your opponent's battlefield (counts towards your opponent's total) and draw 2 cards from your deck."), TIGHT_BOND("Tight Bond", "Place next to a card with the same name to double the strength of both cards."), SCORCH("Scorch", "Kills the strongest card(s) on the battlefield."), SCORCH_CLOSE("Scorch - Close Combat", "Destroy your enemy's strongest Close Combat unit(s) if the combined strength of all his or her Close Combat units is 10 or more."), SCORCH_RANGED("Scorch - Ranged Combat", "Destroy your enemy's strongest Ranged Combat unit(s) if the combined strength of all his or her Ranged Combat units is 10 or more."), SCORCH_SIEGE("Scorch - Siege Combat", "Destroy your enemy's strongest Siege Combat unit(s) if the combined strength of all his or her Siege Combat units is 10 or more."), HORN("Commander's Horn", "Doubles the strength of all other unit cards in its row. Limited to 1 per row."), BERSERKER("Berserker", "Transforms into a bear when a Mardroeme card is on its row."), MARDROEME("Mardroeme", "Triggers transformation of all Berserker cards on the same row."), AVENGER("Summon Avenger", "When this card is removed from the battlefield, it summons a powerful new Unit Card to take its place.");

    private final String displayName;
    private final String description;

    Ability(String displayName, String description)
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

    /** The row a row-scorch ability targets on the enemy side, or null for other abilities. */
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
}
