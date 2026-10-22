package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.CardInstance;
import com.numankaraaslan.gwent.engine.EventSink;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.Move;
import com.numankaraaslan.gwent.engine.PlayerState;
import com.numankaraaslan.gwent.engine.Resolver;
import com.numankaraaslan.gwent.engine.RowState;
import com.numankaraaslan.gwent.engine.Rules;
import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Plays each candidate move on a copy of the game with the real {@link Resolver}, so every ability is applied exactly,
 * and measures the result. Hidden information stays hidden: both decks are shuffled in the copy (the AI doesn't know
 * the order), and the opponent's hand is only ever used as a count.
 */
final class MoveSimulator
{

    private final Random random;
    private final SimulationDecider decider = new SimulationDecider();

    MoveSimulator(Random random)
    {
        this.random = random;
    }

    /**
     * Evaluates every move. A move whose simulation fails (it shouldn't) is skipped instead of breaking the AI.
     *
     * @param scorchRisk probability-like weight of the opponent answering with a Scorch (0 = ignore)
     */
    List<MoveEval> evaluateAll(GameState state, RoundContext ctx, List<Move> moves, double scorchRisk)
    {
        double deckValue = CardValues.averageDeckValue(state, ctx.me());
        double exposureBefore = scorchRisk > 0 ? scorchExposure(state, ctx.me()) : 0;
        double avengersBefore = pendingAvengers(state, ctx.me());
        List<MoveEval> result = new ArrayList<>();
        for (Move move : moves)
        {
            try
            {
                result.add(evaluate(state, ctx, move, deckValue, scorchRisk, exposureBefore, avengersBefore));
            }
            catch (RuntimeException e)
            {
                System.err.println("AI: simulation of " + move + " failed: " + e);
            }
        }
        return result;
    }

    private MoveEval evaluate(GameState state, RoundContext ctx, Move move, double deckValue, double scorchRisk, double exposureBefore, double avengersBefore)
    {
        int me = ctx.me();
        int opp = ctx.opp();
        PlayerState before = state.player(me);
        Set<Integer> handIds = ids(before.hand());
        Set<Integer> deckIds = ids(before.deck());
        GameState sim = state.copy();
        Collections.shuffle(sim.player(0).deck(), random);
        Collections.shuffle(sim.player(1).deck(), random);
        new Resolver(sim, decider, decider, EventSink.NONE).applyMove(me, move);
        int myScore = Rules.score(sim, me);
        int oppScore = Rules.score(sim, opp);
        int gain = (myScore - oppScore) - ctx.diff();
        PlayerState after = sim.player(me);
        Set<Integer> afterIds = ids(after.hand());
        double resources = 0;
        int drawn = 0;
        for (CardInstance card : after.hand())
        {
            if (handIds.contains(card.id()))
            {
                continue;
            }
            if (deckIds.contains(card.id()))
            {
                drawn++;
            }
            else
            {
                resources += CardValues.keepValue(sim, me, card);
            }
        }
        resources += drawn * deckValue;
        for (CardInstance card : before.hand())
        {
            if (afterIds.contains(card.id()))
            {
                continue;
            }
            // A hand card that went to the battlefield without being played was pulled by Muster: the played card's
            // value already includes the whole muster group, so charging it again would count it twice.
            boolean played = move.isPlay() && move.cardId() == card.id();
            if (played)
            {
                resources -= CardValues.playCost(state, me, card);
            }
            else if (!onBattlefield(sim, card.id()))
            {
                resources -= CardValues.keepValue(state, me, card);
            }
        }
        if (move.isLeader())
        {
            resources -= ctx.leaderValue();
        }
        resources += pendingAvengers(sim, me) - avengersBefore;
        double score = gain + ctx.conserve() * resources;
        if (scorchRisk > 0)
        {
            score -= scorchRisk * (scorchExposure(sim, me) - exposureBefore);
        }
        return new MoveEval(move, gain, myScore, oppScore, after.hand().size() - before.hand().size(), resources, score);
    }

    /**
     * What my Avengers on the battlefield will bring: their replacement arrives at the latest in the next round, so an
     * Avenger on the board is worth about a card in hand (weighted like hand potential by the caller).
     */
    private static double pendingAvengers(GameState state, int me)
    {
        double total = 0;
        for (CardInstance card : state.player(me).boardCards())
        {
            if (card.def().isUnit() && card.def().ability() == Ability.AVENGER)
            {
                CardDef summoned = state.db().findCard(card.def().avengerSummons());
                if (summoned != null)
                {
                    total += CardValues.keepValue(state, me, summoned, -1);
                }
            }
        }
        return total;
    }

    /** Strength I would lose to an enemy Scorch right now, minus what the opponent would lose (never negative). */
    private static double scorchExposure(GameState sim, int me)
    {
        double mine = 0;
        double theirs = 0;
        for (CardInstance victim : Rules.scorchVictims(sim))
        {
            int side = Rules.sideOf(sim, victim);
            if (side < 0 || victim.row() == null)
            {
                continue;
            }
            PlayerState owner = sim.player(side);
            double strength = Rules.strength(sim, owner, owner.row(victim.row()), victim);
            if (side == me)
            {
                mine += strength;
            }
            else
            {
                theirs += strength;
            }
        }
        return Math.max(0, mine - theirs);
    }

    private static boolean onBattlefield(GameState sim, int cardId)
    {
        for (int p = 0; p < 2; p++)
        {
            PlayerState player = sim.player(p);
            if (player.findOnBoard(cardId) != null)
            {
                return true;
            }
            for (RowState row : player.rows())
            {
                if (row.special() != null && row.special().id() == cardId)
                {
                    return true;
                }
            }
        }
        for (CardInstance weather : sim.weather())
        {
            if (weather.id() == cardId)
            {
                return true;
            }
        }
        return false;
    }

    private static Set<Integer> ids(List<CardInstance> cards)
    {
        Set<Integer> ids = new HashSet<>();
        for (CardInstance card : cards)
        {
            ids.add(card.id());
        }
        return ids;
    }
}
