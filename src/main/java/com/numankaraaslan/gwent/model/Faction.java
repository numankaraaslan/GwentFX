package com.numankaraaslan.gwent.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The Gwent factions of The Witcher 3 (base game + Blood and Wine's Skellige).
 * NEUTRAL is only used for cards that every faction can use.
 */
public enum Faction
{
    NEUTRAL("Neutral", "Neutral", ""), NORTHERN_REALMS("Northern Realms", "Northern Realms", "Draw a card from your deck whenever you win a round."), NILFGAARD("Nilfgaardian Empire", "Nilfgaard", "Win any round that ends in a draw."), SCOIATAEL("Scoia'tael", "Scoia'tael", "You decide who goes first at the start of battle."), MONSTERS("Monsters", "Monsters", "Keeps a random Unit Card on the battlefield after each round."), SKELLIGE("Skellige", "Skellige", "2 random cards from your graveyard are placed on the battlefield at the start of the third round.");

    private final String displayName;
    private final String shortName;
    private final String abilityDescription;

    Faction(String displayName, String shortName, String abilityDescription)
    {
        this.displayName = displayName;
        this.shortName = shortName;
        this.abilityDescription = abilityDescription;
    }

    public String displayName()
    {
        return displayName;
    }

    public String shortName()
    {
        return shortName;
    }

    /** Text of the passive faction perk. Empty for NEUTRAL. */
    public String abilityDescription()
    {
        return abilityDescription;
    }

    /** True for the five factions a player can pick (everything except NEUTRAL). */
    public boolean isPlayable()
    {
        return this != NEUTRAL;
    }

    public static List<Faction> playable()
    {
        List<Faction> result = new ArrayList<>();
        for (Faction faction : values())
        {
            if (faction.isPlayable())
            {
                result.add(faction);
            }
        }
        return result;
    }
}
