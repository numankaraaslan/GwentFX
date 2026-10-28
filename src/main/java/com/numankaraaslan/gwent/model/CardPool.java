package com.numankaraaslan.gwent.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The cards both decks of a game are built from: every card in Free Play, only the cards you own in Collector Mode.
 * Each card stands for all its copies. Tokens are never part of a pool: they only appear during play. Immutable, so it
 * can be shared between the engine thread and the JavaFX thread.
 */
public final class CardPool
{

    private final List<CardDef> cards;
    private final Set<String> ids;

    private CardPool(List<CardDef> cards)
    {
        List<CardDef> kept = new ArrayList<>();
        Set<String> keptIds = new HashSet<>();
        for (CardDef card : cards)
        {
            if (!card.token() && keptIds.add(card.id()))
            {
                kept.add(card);
            }
        }
        this.cards = Collections.unmodifiableList(kept);
        this.ids = Collections.unmodifiableSet(keptIds);
    }

    /** Every card of the original collection. */
    public static CardPool full(CardDatabase db)
    {
        return new CardPool(db.cards());
    }

    /** Exactly these cards (tokens are left out). */
    public static CardPool of(List<CardDef> cards)
    {
        return new CardPool(cards);
    }

    public boolean contains(CardDef card)
    {
        return ids.contains(card.id());
    }

    /** Cards of exactly this faction in the pool, every copy counted. */
    public int count(Faction faction)
    {
        int count = 0;
        for (CardDef card : cards)
        {
            if (card.faction() == faction)
            {
                count += card.copies();
            }
        }
        return count;
    }

    /** Average strength of the units in the pool, every copy counted (0 if there are none). */
    public double averageUnitStrength()
    {
        int strength = 0;
        int units = 0;
        for (CardDef card : cards)
        {
            if (card.isUnit())
            {
                strength += card.strength() * card.copies();
                units += card.copies();
            }
        }
        return units == 0 ? 0 : (double) strength / units;
    }
}
