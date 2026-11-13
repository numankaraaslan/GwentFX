package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.PlayerState;
import com.numankaraaslan.gwent.engine.Rules;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.LeaderAbility;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import java.util.EnumSet;
import java.util.Set;

/**
 * What the weather cards are worth to the AI at the redraw, given both factions and leaders. Weather weakens a row on
 * both sides, so a weather card is worth more the more the opponent's deck relies on the rows it hits, and less the more
 * the AI's own deck does.
 *
 * <p>Both decks are public knowledge: every card of the faction plus every neutral card in the game's card pool. A row's
 * share is the part of a deck's unit strength in that row, counting only the units weather weakens on their owner's
 * side: no heroes, no spies. An agile unit counts half in each of its rows. King Bran halves the shares of his side,
 * since its units lose only half their strength.</p>
 */
final class WeatherMatchup
{

    /** Value points per share a weather card takes more from the opponent than from the AI: 10% of a deck is 1 point. */
    private static final double SHARE_WEIGHT = 10;
    /** Least a weather card is worth at the redraw. */
    private static final double MIN_VALUE = 1;
    /** Most a weather card, Clear Weather included, is worth at the redraw. */
    private static final double MAX_VALUE = 8;

    /** The AI's row shares, by {@link RowType} ordinal. */
    private final double[] mine;
    /** The opponent's row shares, by {@link RowType} ordinal. */
    private final double[] theirs;
    /** The weather cards the opponent's leader can still play from their deck. */
    private final Set<SpecialType> leaderWeather;

    private WeatherMatchup(double[] mine, double[] theirs, Set<SpecialType> leaderWeather)
    {
        this.mine = mine;
        this.theirs = theirs;
        this.leaderWeather = leaderWeather;
    }

    static WeatherMatchup of(GameState state, int me)
    {
        int opp = Rules.opponent(me);
        return new WeatherMatchup(rowShares(state, me), rowShares(state, opp), leaderWeather(state, opp));
    }

    /**
     * Redraw value of a weather card. Biting Frost, Impenetrable Fog, Torrential Rain and Skellige Storm: the keep value
     * + 10 x (the opponent's share of the rows it hits - the AI's share of them), between 1 and 8. Clear Weather: the
     * keep value + 10 x how much more the worst weather card the opponent's leader can play from their deck would hit
     * the AI than the opponent, at most 8.
     */
    double value(CardDef weather)
    {
        if (weather.special() == SpecialType.CLEAR_WEATHER)
        {
            double threat = 0;
            for (SpecialType type : leaderWeather)
            {
                threat = Math.max(threat, hit(mine, type) - hit(theirs, type));
            }
            return Math.min(MAX_VALUE, CardValues.CLEAR_WEATHER_VALUE + SHARE_WEIGHT * threat);
        }
        double edge = hit(theirs, weather.special()) - hit(mine, weather.special());
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, CardValues.WEATHER_VALUE + SHARE_WEIGHT * edge));
    }

    /** The share of the rows this weather card weakens. */
    private static double hit(double[] shares, SpecialType weather)
    {
        double total = 0;
        for (RowType row : weather.affectedRows())
        {
            total += shares[row.ordinal()];
        }
        return total;
    }

    /** A player's row shares, halved while their King Bran is active. */
    private static double[] rowShares(GameState state, int side)
    {
        PlayerState player = state.player(side);
        double[] strength = new double[RowType.values().length];
        double total = 0;
        for (CardDef card : state.db().deckPool(player.faction()))
        {
            if (!state.pool().contains(card) || !card.isUnit() || card.hero() || card.isSpy())
            {
                continue;
            }
            double cardStrength = card.strength() * card.copies();
            for (RowType row : card.rows())
            {
                strength[row.ordinal()] += cardStrength / card.rows().size();
            }
            total += cardStrength;
        }
        double factor = player.leaderActive(LeaderAbility.HALF_WEATHER) ? 0.5 : 1;
        double[] shares = new double[strength.length];
        for (int i = 0; i < shares.length; i++)
        {
            shares[i] = total > 0 ? factor * strength[i] / total : 0;
        }
        return shares;
    }

    /** The weather cards in a player's deck that their leader can play from it; none once the leader is used or cancelled. */
    private static Set<SpecialType> leaderWeather(GameState state, int side)
    {
        PlayerState player = state.player(side);
        Set<SpecialType> result = EnumSet.noneOf(SpecialType.class);
        if (player.leaderUsed() || player.leaderDisabled())
        {
            return result;
        }
        for (CardDef card : state.db().deckPool(player.faction()))
        {
            if (state.pool().contains(card) && card.isWeather() && leaderPlays(player.leader().ability(), card.special()))
            {
                result.add(card.special());
            }
        }
        return result;
    }

    /** True if a leader with this ability can pick this weather card from the deck and play it. */
    private static boolean leaderPlays(LeaderAbility ability, SpecialType weather)
    {
        switch (ability)
        {
        case FROST_FROM_DECK:
            return weather == SpecialType.BITING_FROST;
        case FOG_FROM_DECK:
            return weather == SpecialType.IMPENETRABLE_FOG;
        case RAIN_FROM_DECK:
            return weather == SpecialType.TORRENTIAL_RAIN;
        case WEATHER_FROM_DECK:
            return weather.isBadWeather();
        default:
            return false;
        }
    }
}
