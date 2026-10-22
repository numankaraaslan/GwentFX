package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.CardInstance;
import com.numankaraaslan.gwent.engine.ChoiceRequest;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.PlayerState;
import com.numankaraaslan.gwent.engine.Rules;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Quick, non-blocking heuristics for the decisions made in the middle of resolving a move (Medic target, leader picks,
 * discards, agile rows). Used both for the AI's real decisions and inside its look-ahead simulations.
 */
final class ChoicePolicy
{

    private ChoicePolicy()
    {
    }

    /** Answers a {@link ChoiceRequest} with between min and max valid ids. */
    static List<Integer> choose(GameState state, int me, ChoiceRequest request)
    {
        List<CardSnapshot> options = request.options();
        List<Integer> result = new ArrayList<>();
        if (options.isEmpty())
        {
            return result;
        }
        switch (request.kind())
        {
        case MEDIC -> result.add(bestRevive(state, me, options));
        case WEATHER_FROM_DECK -> result.add(bestWeather(state, me, options));
        case DISCARD_FROM_HAND -> result.addAll(lowestKeep(state, me, options, Math.max(1, request.min())));
        default -> result.add(highestKeep(state, me, options));
        }
        return result;
    }

    /** Row where the card would be strongest (agile units). */
    static RowType bestRow(GameState state, int me, CardSnapshot card, List<RowType> options)
    {
        PlayerState player = state.player(me);
        CardInstance probe = new CardInstance(card.id(), card.def(), me);
        RowType best = options.get(0);
        int bestValue = Integer.MIN_VALUE;
        for (RowType row : options)
        {
            int value = Rules.strength(state, player, player.row(row), probe);
            if (value > bestValue)
            {
                best = row;
                bestValue = value;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ medic

    private static int bestRevive(GameState state, int me, List<CardSnapshot> options)
    {
        double deckValue = Math.max(4, CardValues.averageDeckValue(state, me));
        int best = options.get(0).id();
        double bestValue = Double.NEGATIVE_INFINITY;
        for (CardSnapshot option : options)
        {
            double value = reviveValue(state, me, option, deckValue);
            if (value > bestValue)
            {
                best = option.id();
                bestValue = value;
            }
        }
        return best;
    }

    /** What reviving this unit right now is worth: its strength where it lands plus what its ability does. */
    static double reviveValue(GameState state, int me, CardSnapshot option, double deckValue)
    {
        CardDef def = option.def();
        int opp = Rules.opponent(me);
        PlayerState player = state.player(me);
        if (def.isSpy())
        {
            int draws = Math.min(Rules.SPY_DRAW_COUNT, player.deck().size());
            return draws * deckValue - def.strength();
        }
        CardInstance probe = new CardInstance(option.id(), def, me);
        RowType row = Rules.bestRow(state, me, probe);
        double value = Rules.strength(state, player, player.row(row), probe);
        switch (def.ability())
        {
        case MEDIC -> value += 0.8 * CardValues.bestReviveStrength(player, option.id());
        case MUSTER -> value += CardValues.musterStrength(player, def, option.id());
        case SCORCH_CLOSE, SCORCH_RANGED, SCORCH_SIEGE -> value += strengthOf(state, Rules.rowScorchVictims(state, opp, def.ability().scorchRow()));
        case SCORCH -> value += scorchNet(state, me);
        case MORALE_BOOST -> value += player.row(row).cards().size();
        case HORN -> value += Rules.rowScore(state, player, player.row(row));
        default -> {
        }
        }
        return value;
    }

    /** Strength the opponent loses minus the strength I lose if a battlefield scorch happened now. */
    private static double scorchNet(GameState state, int me)
    {
        double net = 0;
        for (CardInstance victim : Rules.scorchVictims(state))
        {
            int side = Rules.sideOf(state, victim);
            PlayerState owner = state.player(side);
            double strength = Rules.strength(state, owner, owner.row(victim.row()), victim);
            net += side == me ? -strength : strength;
        }
        return net;
    }

    private static double strengthOf(GameState state, List<CardInstance> cards)
    {
        double total = 0;
        for (CardInstance card : cards)
        {
            int side = Rules.sideOf(state, card);
            if (side >= 0 && card.row() != null)
            {
                PlayerState owner = state.player(side);
                total += Rules.strength(state, owner, owner.row(card.row()), card);
            }
        }
        return total;
    }

    // ------------------------------------------------------------------ weather

    /** The weather card (or Clear Weather) that leaves me best off compared to the opponent. */
    private static int bestWeather(GameState state, int me, List<CardSnapshot> options)
    {
        int opp = Rules.opponent(me);
        int best = options.get(0).id();
        int bestDiff = Integer.MIN_VALUE;
        for (CardSnapshot option : options)
        {
            GameState sim = state.copy();
            CardInstance card = sim.player(me).findInDeck(option.id());
            if (card == null)
            {
                continue;
            }
            sim.player(me).deck().remove(card);
            SpecialType type = card.def().special();
            if (type == SpecialType.CLEAR_WEATHER)
            {
                sim.weather().clear();
            }
            else if (Rules.findSpecial(sim.weather(), type) == null)
            {
                sim.weather().add(card);
            }
            int diff = Rules.score(sim, me) - Rules.score(sim, opp);
            if (diff > bestDiff)
            {
                best = option.id();
                bestDiff = diff;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ keep values

    private static int highestKeep(GameState state, int me, List<CardSnapshot> options)
    {
        List<Scored> scored = score(state, me, options);
        Collections.sort(scored, new ByValue());
        return scored.get(scored.size() - 1).id();
    }

    private static List<Integer> lowestKeep(GameState state, int me, List<CardSnapshot> options, int count)
    {
        List<Scored> scored = score(state, me, options);
        Collections.sort(scored, new ByValue());
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < count && i < scored.size(); i++)
        {
            ids.add(scored.get(i).id());
        }
        return ids;
    }

    private static List<Scored> score(GameState state, int me, List<CardSnapshot> options)
    {
        List<Scored> scored = new ArrayList<>();
        for (CardSnapshot option : options)
        {
            scored.add(new Scored(option.id(), CardValues.keepValue(state, me, option.def(), option.id())));
        }
        return scored;
    }

    /** A card id with a value. */
    private record Scored(int id, double value)
    {
    }

    /** Ascending by value. */
    private static final class ByValue implements Comparator<Scored>
    {
        @Override
        public int compare(Scored a, Scored b)
        {
            return Double.compare(a.value(), b.value());
        }
    }
}
