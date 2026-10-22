package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.CardInstance;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.PlayerState;
import com.numankaraaslan.gwent.engine.RowState;
import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.SpecialType;
import java.util.List;

/**
 * Static estimates of what a card is worth to the AI, in "strength points". Two views:
 * <ul>
 *   <li>{@link #keepValue}: how much the AI wants to keep the card in hand for later (redraw, discard, restore choices).</li>
 *   <li>{@link #playCost}: how much of the hand's future potential is used up by playing the card now. Spies cost nothing
 *       (they turn into two new cards), so the AI happily plays them early, which is classic Gwent card-advantage play.</li>
 * </ul>
 */
final class CardValues
{

    /** Assumed value of a revive target when the discard pile has none yet. */
    private static final double DEFAULT_REVIVE = 3;

    private CardValues()
    {
    }

    /** How much the AI wants to keep this card (with id {@code selfId}) in its hand for later. */
    static double keepValue(GameState state, int me, CardDef def, int selfId)
    {
        PlayerState player = state.player(me);
        if (def.isWeather())
        {
            return def.special() == SpecialType.CLEAR_WEATHER ? 3 : 4;
        }
        if (def.isSpecialCard())
        {
            switch (def.special())
            {
            case COMMANDERS_HORN:
                return 8;
            case DECOY:
                return 6;
            case SCORCH:
                return 6;
            case MARDROEME:
                return countBerserkers(player) > 0 ? 7 : 2;
            default:
                return 3;
            }
        }
        double strength = def.strength();
        double value;
        switch (def.ability())
        {
        case SPY:
            value = 10 - strength * 0.3;
            break;
        case MEDIC:
            value = strength + 0.8 * bestReviveStrength(player, selfId);
            break;
        case MUSTER:
            value = strength + 0.85 * musterStrength(player, def, selfId);
            break;
        case TIGHT_BOND:
            value = strength * (1 + 0.6 * Math.min(2, bondPartners(player, def, selfId)));
            break;
        case MORALE_BOOST:
            value = strength + 2;
            break;
        case HORN:
            value = strength + 4;
            break;
        case SCORCH:
        case SCORCH_CLOSE:
        case SCORCH_RANGED:
        case SCORCH_SIEGE:
            value = strength + 3;
            break;
        case BERSERKER:
            value = strength + (hasMardroeme(player) ? 5 : 1);
            break;
        case MARDROEME:
            value = strength + (countBerserkers(player) > 0 ? 3 : 0);
            break;
        case AVENGER:
            value = strength + 0.7 * summonedStrength(state, def);
            break;
        default:
            value = strength;
            break;
        }
        if (def.hero())
        {
            value += 2;
        }
        return value;
    }

    static double keepValue(GameState state, int me, CardInstance card)
    {
        return keepValue(state, me, card.def(), card.id());
    }

    /** Future potential used up by playing this card now. */
    static double playCost(GameState state, int me, CardInstance card)
    {
        CardDef def = card.def();
        if (def.isSpy())
        {
            return 0;
        }
        if (def.isDecoy())
        {
            return 3;
        }
        return keepValue(state, me, card);
    }

    /** Average keep value of the cards left in the AI's own deck (what a drawn card is worth on average). */
    static double averageDeckValue(GameState state, int me)
    {
        List<CardInstance> deck = state.player(me).deck();
        if (deck.isEmpty())
        {
            return 0;
        }
        double total = 0;
        for (CardInstance card : deck)
        {
            total += keepValue(state, me, card);
        }
        return total / deck.size();
    }

    /** Strongest non-hero unit in the discard pile (what a medic could bring back). */
    static double bestReviveStrength(PlayerState player, int excludeId)
    {
        double best = 0;
        boolean any = false;
        for (CardInstance card : player.discard())
        {
            if (card.id() != excludeId && card.def().isUnit() && !card.def().hero() && !card.def().token())
            {
                best = Math.max(best, reviveWorth(card.def()));
                any = true;
            }
        }
        return any ? best : DEFAULT_REVIVE;
    }

    /** Rough worth of a unit as a revive target: spies and medics are worth more than their strength. */
    static double reviveWorth(CardDef def)
    {
        if (def.isSpy())
        {
            return 9;
        }
        if (def.ability() == Ability.MEDIC)
        {
            return def.strength() + 5;
        }
        return def.strength();
    }

    /** Total strength of the cards a muster card would pull from the deck and the hand. */
    static double musterStrength(PlayerState player, CardDef def, int excludeId)
    {
        if (def.summons().isEmpty())
        {
            return 0;
        }
        double total = 0;
        for (CardInstance card : player.deck())
        {
            if (card.id() != excludeId && def.summons().equals(card.def().group()))
            {
                total += card.def().strength();
            }
        }
        for (CardInstance card : player.hand())
        {
            if (card.id() != excludeId && def.summons().equals(card.def().group()))
            {
                total += card.def().strength();
            }
        }
        return total;
    }

    /** Other copies of a tight bond card the player can still use (deck, hand, own board). */
    static int bondPartners(PlayerState player, CardDef def, int excludeId)
    {
        int count = 0;
        for (CardInstance card : player.deck())
        {
            if (card.id() != excludeId && card.def().name().equals(def.name()))
            {
                count++;
            }
        }
        for (CardInstance card : player.hand())
        {
            if (card.id() != excludeId && card.def().name().equals(def.name()))
            {
                count++;
            }
        }
        for (RowState row : player.rows())
        {
            for (CardInstance card : row.cards())
            {
                if (card.id() != excludeId && card.def().name().equals(def.name()))
                {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countBerserkers(PlayerState player)
    {
        int count = 0;
        for (CardInstance card : player.deck())
        {
            if (card.def().ability() == Ability.BERSERKER)
            {
                count++;
            }
        }
        for (CardInstance card : player.hand())
        {
            if (card.def().ability() == Ability.BERSERKER)
            {
                count++;
            }
        }
        for (CardInstance card : player.boardCards())
        {
            if (card.def().ability() == Ability.BERSERKER)
            {
                count++;
            }
        }
        return count;
    }

    private static boolean hasMardroeme(PlayerState player)
    {
        for (CardInstance card : player.deck())
        {
            if (isMardroeme(card.def()))
            {
                return true;
            }
        }
        for (CardInstance card : player.hand())
        {
            if (isMardroeme(card.def()))
            {
                return true;
            }
        }
        for (CardInstance card : player.boardCards())
        {
            if (isMardroeme(card.def()))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean isMardroeme(CardDef def)
    {
        return def.special() == SpecialType.MARDROEME || (def.isUnit() && def.ability() == Ability.MARDROEME);
    }

    private static double summonedStrength(GameState state, CardDef def)
    {
        CardDef summoned = state.db().findCard(def.avengerSummons());
        return summoned == null ? 0 : summoned.strength();
    }
}
