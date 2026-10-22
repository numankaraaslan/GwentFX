package com.numankaraaslan.gwent.model;

import java.util.Set;

/**
 * Immutable definition of a card (one entry of cards.json). Many {@code CardInstance}s can share one definition.
 *
 * @param id             unique key, e.g. "nr_vernon_roche"
 * @param name           display name
 * @param faction        owning faction (NEUTRAL for cards every faction can use)
 * @param type           unit / special / weather
 * @param hero           hero units are immune to every special card and ability
 * @param rows           rows a unit can be played to (two rows = agile); empty for special and weather cards
 * @param strength       base strength (0 for special and weather cards)
 * @param ability        unit ability
 * @param special        effect of special / weather cards (NONE for units)
 * @param copies         copies in the complete original collection (0 for tokens)
 * @param group          muster tag of this card ("" if none)
 * @param summons        muster tag this card pulls from the deck when played ("" if none)
 * @param transformsTo   id of the card a berserker turns into ("" if none)
 * @param avengerSummons id of the card an avenger summons when removed ("" if none)
 * @param token          true for cards that only appear through transformation / avenger
 * @param expansion      which part of the game the card comes from
 */
public record CardDef(String id, String name, Faction faction, CardType type, boolean hero, Set<RowType> rows, int strength, Ability ability, SpecialType special, int copies, String group, String summons, String transformsTo, String avengerSummons, boolean token, Expansion expansion)
{

    public boolean isUnit()
    {
        return type == CardType.UNIT;
    }

    public boolean isSpecialCard()
    {
        return type == CardType.SPECIAL;
    }

    public boolean isWeather()
    {
        return type == CardType.WEATHER;
    }

    /** Agile units can go to either the close or the ranged row. */
    public boolean isAgile()
    {
        return rows.size() > 1;
    }

    public boolean isSpy()
    {
        return isUnit() && ability == Ability.SPY;
    }

    public boolean isDecoy()
    {
        return special == SpecialType.DECOY;
    }

    /** The first row (in CLOSE, RANGED, SIEGE order) this unit may be played to, or null for non-units. */
    public RowType defaultRow()
    {
        for (RowType row : RowType.values())
        {
            if (rows.contains(row))
            {
                return row;
            }
        }
        return null;
    }

    /** Name of the card's ability (unit ability, or the special card's effect), "" if none. */
    public String abilityName()
    {
        if (isUnit())
        {
            return ability.displayName();
        }
        return special.displayName();
    }

    /** Rules text for this card, suitable for a tooltip / info panel. */
    public String description()
    {
        StringBuilder text = new StringBuilder();
        if (isUnit())
        {
            if (hero)
            {
                text.append("Hero: Not affected by any Special Cards or abilities.");
            }
            if (isAgile())
            {
                appendSentence(text, "Agile: Can be placed in either the Close Combat or the Ranged Combat row. Cannot be moved once placed.");
            }
            if (ability != Ability.NONE)
            {
                appendSentence(text, ability.displayName() + ": " + ability.description());
            }
        }
        else
        {
            text.append(special.description());
        }
        return text.toString();
    }

    private static void appendSentence(StringBuilder text, String sentence)
    {
        if (text.length() > 0)
        {
            text.append('\n');
        }
        text.append(sentence);
    }
}
