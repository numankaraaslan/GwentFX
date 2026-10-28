package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.CardInstance;
import com.numankaraaslan.gwent.engine.ChoiceRequest;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.Move;
import com.numankaraaslan.gwent.engine.PlayerState;
import com.numankaraaslan.gwent.engine.RowState;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.Difficulty;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The AI's decision making (no delays here, see {@link AiController}).
 *
 * <p>Every legal move is simulated ({@link MoveSimulator}) and scored as "strength gained + card potential kept",
 * where card potential counts more in early rounds. On top of that sit Witcher 3 round tactics:</p>
 * <ul>
 *   <li>Round 1: win it only if that doesn't cost card advantage. Pass when ahead with at least as many cards
 *       as the opponent, chase only cheaply, never spend more than a third of the hand, play spies early.</li>
 *   <li>After winning a round (opponent must win this one): pass when ahead (they have to spend), keep exchanging
 *       only with card advantage, otherwise concede and save cards for round 3.</li>
 *   <li>After losing a round (must win this one): retake the lead as cheaply as possible, never pass while behind
 *       as long as a move (or a spy's draw) can still help.</li>
 *   <li>Final round: cards are worth nothing afterwards, so simply play the strongest move until nothing helps; spies
 *       are still played when nothing else helps, since their cards can be used in the same round.</li>
 *   <li>Opponent passed: pass if ahead (first squeezing in spies that keep the lead), otherwise win with the cheapest move
 *       when it's worth it.</li>
 *   <li>Giving up a round that isn't decisive: first save what can be saved (spies, a Decoy taking back a valuable unit,
 *       an Avenger whose replacement arrives next round), then pass.</li>
 * </ul>
 *
 * <p>The budgets and leads below are tuned for the full collection. They are always used through
 * {@link RoundContext#scaled}, which shrinks them when the decks hold weaker cards (Collector Mode).</p>
 */
final class AiBrain
{

    /** Chance that the Easy AI plays a random move instead of its best one. */
    private static final double EASY_RANDOM_MOVE = 0.35;
    /** Lead at which the Easy AI may pass early. */
    private static final double EASY_PASS_LEAD = 6;
    /** How likely Hard assumes an unseen Scorch in the opponent's hand, when their deck has one. */
    private static final double SCORCH_RISK_UNKNOWN = 0.2;
    /** ... and when Emperor of Nilfgaard revealed one. */
    private static final double SCORCH_RISK_REVEALED = 0.8;
    /** Most card potential the AI spends per move to win round 1 after the opponent passed. */
    private static final double ROUND_ONE_CHASE_BUDGET = 8;
    /** Most card potential the AI spends to retake the lead in round 1 while the opponent still plays. */
    private static final double ROUND_ONE_RETAKE_BUDGET = 8;
    /** Most card potential spent per exchange in a round the opponent must win. */
    private static final double OPPONENT_MUST_WIN_BUDGET = 10;
    /** In a must-win round, a lead this big is considered safe enough to pass. */
    private static final int MUST_WIN_SAFE_LEAD = 25;
    /** ...or this big when the opponent has at most one card left. */
    private static final int MUST_WIN_SAFE_LEAD_LAST_CARD = 15;
    /** In round 1, while ahead with fewer cards than the opponent, only play on with a move scoring at least this much. */
    private static final double ROUND_ONE_PUSH_MIN_SCORE = 2;
    /** When giving up a lost round, only make "save" moves that keep at least this much card potential. */
    private static final double SAVE_MIN_RESOURCES = 5;

    private final Difficulty difficulty;
    private final Random random = new Random();
    private final MoveSimulator simulator = new MoveSimulator(random);
    /** Opponent cards seen through Emperor of Nilfgaard, by id. */
    private final Map<Integer, CardDef> revealed = new HashMap<>();
    private int trackedRound = -1;
    private int roundStartHand;
    /** Round in which the AI already gave up: no multi-card chase in it any more, even if the opponent passes. */
    private int concededRound = -1;

    AiBrain(Difficulty difficulty)
    {
        this.difficulty = difficulty;
    }

    // ================================================================== turn decision

    Move chooseMove(GameState state, int me, List<Move> legal)
    {
        List<Move> plays = new ArrayList<>();
        for (Move move : legal)
        {
            if (!move.isPass())
            {
                plays.add(move);
            }
        }
        if (plays.isEmpty())
        {
            return Move.pass();
        }
        RoundContext ctx = RoundContext.of(state, me);
        if (state.round() != trackedRound)
        {
            trackedRound = state.round();
            roundStartHand = ctx.myHand();
        }
        double risk = 0;
        if (difficulty == Difficulty.HARD && !ctx.oppPassed() && ctx.oppHand() > 0)
        {
            if (opponentMayHoldScorch(state))
            {
                risk = SCORCH_RISK_REVEALED;
            }
            else if (opponentDeckHasScorch(state, ctx.opp()))
            {
                risk = SCORCH_RISK_UNKNOWN;
            }
        }
        List<MoveEval> evals = simulator.evaluateAll(state, ctx, plays, risk);
        if (evals.isEmpty())
        {
            return Move.pass();
        }
        if (difficulty == Difficulty.EASY)
        {
            return easyMove(ctx, evals);
        }
        if (ctx.oppPassed())
        {
            return afterOpponentPassed(ctx, evals);
        }
        if (ctx.finalRound())
        {
            return finalRound(ctx, evals);
        }
        if (ctx.mustWin())
        {
            return mustWin(ctx, evals);
        }
        if (ctx.oppMustWin())
        {
            return opponentMustWin(ctx, evals);
        }
        return roundOne(ctx, evals);
    }

    /** The opponent passed: the round is decided by what I do now. */
    private Move afterOpponentPassed(RoundContext ctx, List<MoveEval> evals)
    {
        if (ctx.winning())
        {
            if (ctx.finalRound() || ctx.oppMustWin())
            {
                return Move.pass();
            }
            MoveEval free = safeCardGain(ctx, evals);
            if (free != null)
            {
                return free.move();
            }
            return passOrFreeMove(ctx, evals);
        }
        if (ctx.finalRound() || ctx.mustWin())
        {
            return lastStand(ctx, evals);
        }
        if (ctx.oppMustWin())
        {
            if (ctx.diff() == 0 && !ctx.oppWinsTies())
            {
                // a drawn round costs the opponent their last gem: passing wins the game (a tie they win as Nilfgaard is no draw)
                return Move.pass();
            }
            MoveEval win = cheapestWinning(ctx, evals, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
            if (win != null)
            {
                return win.move();
            }
            MoveEval progress = bestGain(evals);
            if (progress != null && progress.gain() > 0 && reachable(evals, ctx.need(), ctx.myHand(), Double.NEGATIVE_INFINITY))
            {
                return progress.move();
            }
            return concede(ctx, evals);
        }
        double chaseBudget = ctx.scaled(ROUND_ONE_CHASE_BUDGET);
        MoveEval cheap = cheapestWinning(ctx, evals, -chaseBudget, Double.NEGATIVE_INFINITY);
        if (cheap != null)
        {
            return cheap.move();
        }
        if (ctx.cardAdvantage() >= 1 && concededRound != trackedRound)
        {
            MoveEval progress = bestGainWithin(evals, -chaseBudget);
            if (progress != null && progress.gain() > 0 && reachable(evals, ctx.need(), 2, -chaseBudget))
            {
                return progress.move();
            }
        }
        return concede(ctx, evals);
    }

    /**
     * Both players on their last gem: cards have no future value, so play whatever helps most. Passing while behind
     * loses the game, so then any move that helps (or draws cards that might) is played instead.
     */
    private Move finalRound(RoundContext ctx, List<MoveEval> evals)
    {
        if (ctx.winning() && ctx.oppOutOfMoves())
        {
            return Move.pass();
        }
        MoveEval best = bestScore(evals);
        if (best != null && best.score() > 0)
        {
            return best.move();
        }
        if (!ctx.winning())
        {
            MoveEval progress = bestGain(evals);
            if (progress != null && progress.gain() > 0)
            {
                return progress.move();
            }
            MoveEval draw = bestDraw(evals, Double.NEGATIVE_INFINITY);
            return draw != null ? draw.move() : Move.pass();
        }
        MoveEval draw = bestDraw(evals, 0);
        return draw != null ? draw.move() : Move.pass();
    }

    /** I lost a round already: losing this one loses the game. */
    private Move mustWin(RoundContext ctx, List<MoveEval> evals)
    {
        if (!ctx.winning())
        {
            MoveEval retake = cheapestWinning(ctx, evals, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
            if (retake != null)
            {
                return retake.move();
            }
            MoveEval progress = bestGain(evals);
            if (progress != null && progress.gain() > 0)
            {
                return progress.move();
            }
            MoveEval draw = bestDraw(evals, Double.NEGATIVE_INFINITY);
            return draw != null ? draw.move() : Move.pass();
        }
        MoveEval cardGain = bestCardGain(ctx, evals, true);
        if (cardGain != null)
        {
            return cardGain.move();
        }
        if (ctx.oppOutOfMoves() || ctx.diff() >= ctx.scaled(MUST_WIN_SAFE_LEAD) || (ctx.oppHand() <= 1 && ctx.diff() >= ctx.scaled(MUST_WIN_SAFE_LEAD_LAST_CARD)))
        {
            return passOrFreeMove(ctx, evals);
        }
        MoveEval cheapest = cheapestPositive(evals);
        return cheapest != null ? cheapest.move() : passOrFreeMove(ctx, evals);
    }

    /** I won a round already: the opponent has to win this one, so make them pay for it. */
    private Move opponentMustWin(RoundContext ctx, List<MoveEval> evals)
    {
        MoveEval cardGain = bestCardGain(ctx, evals, ctx.winning());
        if (ctx.winning())
        {
            return cardGain != null ? cardGain.move() : passOrFreeMove(ctx, evals);
        }
        if (cardGain != null)
        {
            return cardGain.move();
        }
        if (ctx.cardAdvantage() >= 1)
        {
            MoveEval retake = cheapestWinning(ctx, evals, -ctx.scaled(OPPONENT_MUST_WIN_BUDGET), Double.NEGATIVE_INFINITY);
            if (retake != null)
            {
                return retake.move();
            }
        }
        return concede(ctx, evals);
    }

    /**
     * First round (nobody lost a gem yet): don't trade card advantage for the round. A tie win (Nilfgaard) on a still
     * empty board is no reason to pass: that would hand the round over for a single card.
     */
    private Move roundOne(RoundContext ctx, List<MoveEval> evals)
    {
        int spent = roundStartHand - ctx.myHand();
        int maxSpent = Math.max(3, ctx.startHand() / 3);
        boolean canSpend = spent < maxSpent && ctx.myHand() > ctx.roundOneHandFloor();
        MoveEval best = bestScore(evals);
        if (ctx.winning() && !ctx.emptyBoard())
        {
            MoveEval cardGain = bestCardGain(ctx, evals, true);
            if (cardGain != null)
            {
                return cardGain.move();
            }
            if (ctx.cardAdvantage() >= 0)
            {
                return passOrFreeMove(ctx, evals);
            }
            if (canSpend && best != null && best.score() >= ctx.scaled(ROUND_ONE_PUSH_MIN_SCORE))
            {
                return best.move();
            }
            return passOrFreeMove(ctx, evals);
        }
        MoveEval cardGain = bestCardGain(ctx, evals, false);
        if (cardGain != null)
        {
            return cardGain.move();
        }
        MoveEval retake = cheapestWinning(ctx, evals, -ctx.scaled(ROUND_ONE_RETAKE_BUDGET), 0);
        if (retake != null && canSpend)
        {
            return retake.move();
        }
        if (ctx.cardAdvantage() >= 2 && canSpend && best != null && best.score() > 0)
        {
            return best.move();
        }
        return concede(ctx, evals);
    }

    /**
     * The opponent passed in a round whose loss would lose me the game (final round or must-win): anything that can still
     * help is better than passing. A drawn final round is still better than a lost one, so a spy is only gambled on when
     * the round is lost as it stands or a draw isn't enough.
     */
    private static Move lastStand(RoundContext ctx, List<MoveEval> evals)
    {
        MoveEval win = cheapestWinning(ctx, evals, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        if (win != null)
        {
            return win.move();
        }
        MoveEval progress = bestGain(evals);
        if (progress != null && progress.gain() > 0)
        {
            return progress.move();
        }
        if (ctx.losing() || ctx.mustWin())
        {
            MoveEval draw = bestDraw(evals, Double.NEGATIVE_INFINITY);
            if (draw != null)
            {
                return draw.move();
            }
        }
        return Move.pass();
    }

    /**
     * Gives up a round that doesn't decide the game. If the round is lost anyway, first saves what it can for the next
     * rounds (spies, a Decoy taking back a valuable unit, an Avenger...), whatever that does to this round's score.
     */
    private Move concede(RoundContext ctx, List<MoveEval> evals)
    {
        concededRound = trackedRound;
        if (!ctx.finalRound() && ctx.losing())
        {
            MoveEval save = bestSave(ctx, evals);
            if (save != null)
            {
                return save.move();
            }
        }
        return passOrFreeMove(ctx, evals);
    }

    /**
     * Passes, unless a move costs nothing at all and pays off later (an Avenger whose replacement arrives next round,
     * taking back a 0-strength Medic...). Never used in the final round, where nothing pays off later.
     */
    private static Move passOrFreeMove(RoundContext ctx, List<MoveEval> evals)
    {
        if (!ctx.finalRound())
        {
            MoveEval free = bestFreeMove(evals);
            if (free != null)
            {
                return free.move();
            }
        }
        return Move.pass();
    }

    /** Easy: greedy, sometimes random, gives up easily. */
    private Move easyMove(RoundContext ctx, List<MoveEval> evals)
    {
        if (ctx.oppPassed() && ctx.winning())
        {
            return Move.pass();
        }
        if (!ctx.finalRound() && !ctx.mustWin())
        {
            if (ctx.oppPassed() && random.nextDouble() < 0.5)
            {
                return Move.pass();
            }
            if (!ctx.oppPassed() && ctx.winning() && ctx.diff() >= ctx.scaled(EASY_PASS_LEAD) && random.nextDouble() < 0.6)
            {
                return Move.pass();
            }
        }
        if (random.nextDouble() < EASY_RANDOM_MOVE)
        {
            List<MoveEval> sensible = new ArrayList<>();
            for (MoveEval eval : evals)
            {
                if (eval.gain() >= 0 || eval.handDelta() >= 1)
                {
                    sensible.add(eval);
                }
            }
            if (!sensible.isEmpty())
            {
                return sensible.get(random.nextInt(sensible.size())).move();
            }
        }
        MoveEval greedy = bestGain(evals);
        if (greedy == null || greedy.gain() <= 0)
        {
            return Move.pass();
        }
        return greedy.move();
    }

    // ================================================================== move pickers

    private static MoveEval bestScore(List<MoveEval> evals)
    {
        MoveEval best = null;
        for (MoveEval eval : evals)
        {
            if (best == null || eval.score() > best.score())
            {
                best = eval;
            }
        }
        return best;
    }

    private static MoveEval bestGain(List<MoveEval> evals)
    {
        MoveEval best = null;
        for (MoveEval eval : evals)
        {
            if (best == null || eval.gain() > best.gain() || (eval.gain() == best.gain() && eval.resources() > best.resources()))
            {
                best = eval;
            }
        }
        return best;
    }

    /** Highest-gain move among those spending at most {@code -minResources} card potential (ties: the cheaper one), or null. */
    private static MoveEval bestGainWithin(List<MoveEval> evals, double minResources)
    {
        MoveEval best = null;
        for (MoveEval eval : evals)
        {
            if (eval.resources() < minResources)
            {
                continue;
            }
            if (best == null || eval.gain() > best.gain() || (eval.gain() == best.gain() && eval.resources() > best.resources()))
            {
                best = eval;
            }
        }
        return best;
    }

    /** The winning move that spends the least card potential (ties: the bigger lead). */
    private static MoveEval cheapestWinning(RoundContext ctx, List<MoveEval> evals, double minResources, double minScore)
    {
        MoveEval best = null;
        for (MoveEval eval : evals)
        {
            if (!ctx.winningWith(eval.diffAfter()) || eval.resources() < minResources || eval.score() < minScore)
            {
                continue;
            }
            if (best == null || eval.resources() > best.resources() || (eval.resources() == best.resources() && eval.diffAfter() > best.diffAfter()))
            {
                best = eval;
            }
        }
        return best;
    }

    /** The positive-gain move that spends the least card potential. */
    private static MoveEval cheapestPositive(List<MoveEval> evals)
    {
        MoveEval best = null;
        for (MoveEval eval : evals)
        {
            if (eval.gain() <= 0)
            {
                continue;
            }
            if (best == null || eval.resources() > best.resources())
            {
                best = eval;
            }
        }
        return best;
    }

    /**
     * Best move that leaves me with more cards in hand than before (spies, a Medic reviving a spy...), with a positive
     * score. With {@code keepWinning} it must also keep me ahead.
     */
    private static MoveEval bestCardGain(RoundContext ctx, List<MoveEval> evals, boolean keepWinning)
    {
        MoveEval best = null;
        for (MoveEval eval : evals)
        {
            if (eval.handDelta() < 1 || eval.score() <= 0 || eval.move().isLeader())
            {
                continue;
            }
            if (keepWinning && !ctx.winningWith(eval.diffAfter()))
            {
                continue;
            }
            if (best == null || eval.score() > best.score())
            {
                best = eval;
            }
        }
        return best;
    }

    /**
     * The opponent passed and I'm ahead: a move that gains cards (spies, a Medic reviving a spy) and keeps me ahead costs
     * nothing, whatever it does to the score. Picks the one adding the most card potential.
     */
    private static MoveEval safeCardGain(RoundContext ctx, List<MoveEval> evals)
    {
        MoveEval best = null;
        for (MoveEval eval : evals)
        {
            if (eval.handDelta() < 1 || eval.resources() <= 0 || eval.move().isLeader() || !ctx.winningWith(eval.diffAfter()))
            {
                continue;
            }
            if (best == null || eval.resources() > best.resources() || (eval.resources() == best.resources() && eval.diffAfter() > best.diffAfter()))
            {
                best = eval;
            }
        }
        return best;
    }

    /**
     * Best move that adds cards to my hand (spies, a Medic reviving a spy, a discard-pile leader), judged by the strength
     * change plus the value of the new cards. Null if none is worth more than {@code minValue}.
     */
    private static MoveEval bestDraw(List<MoveEval> evals, double minValue)
    {
        MoveEval best = null;
        double bestValue = minValue;
        for (MoveEval eval : evals)
        {
            if (eval.handDelta() < 1)
            {
                continue;
            }
            double value = eval.gain() + eval.resources();
            if (value > bestValue)
            {
                best = eval;
                bestValue = value;
            }
        }
        return best;
    }

    /** The move that saves the most card potential for later rounds, whatever it does to the current (lost) round. */
    private static MoveEval bestSave(RoundContext ctx, List<MoveEval> evals)
    {
        double minResources = ctx.scaled(SAVE_MIN_RESOURCES);
        MoveEval best = null;
        for (MoveEval eval : evals)
        {
            if (eval.move().isLeader() || eval.resources() < minResources)
            {
                continue;
            }
            if (best == null || eval.resources() > best.resources())
            {
                best = eval;
            }
        }
        return best;
    }

    /** Best move that costs neither strength nor card potential (and so can't cost the lead either), or null. */
    private static MoveEval bestFreeMove(List<MoveEval> evals)
    {
        MoveEval best = null;
        for (MoveEval eval : evals)
        {
            if (eval.move().isLeader() || eval.gain() < 0 || eval.resources() < 0 || eval.score() <= 0)
            {
                continue;
            }
            if (best == null || eval.score() > best.score())
            {
                best = eval;
            }
        }
        return best;
    }

    /**
     * True if the best {@code cards} positive gains together could cover {@code need} (rough upper bound), counting only
     * moves that spend at most {@code -minResources} card potential.
     */
    private static boolean reachable(List<MoveEval> evals, int need, int cards, double minResources)
    {
        List<Integer> gains = new ArrayList<>();
        for (MoveEval eval : evals)
        {
            if (eval.gain() > 0 && eval.resources() >= minResources)
            {
                gains.add(eval.gain());
            }
        }
        gains.sort(null);
        int total = 0;
        for (int i = gains.size() - 1, used = 0; i >= 0 && used < cards; i--, used++)
        {
            total += gains.get(i);
        }
        return total >= need;
    }

    /** Hard only: was a Scorch seen in the opponent's hand (Emperor of Nilfgaard) that hasn't been played since? */
    private boolean opponentMayHoldScorch(GameState state)
    {
        for (Map.Entry<Integer, CardDef> entry : revealed.entrySet())
        {
            if (entry.getValue().special() == SpecialType.SCORCH && !isPublic(state, entry.getKey()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the opponent's deck holds a card that scorches the whole battlefield: Scorch, or Clan Dimun Pirate for
     * Skellige. In Collector Mode it may hold neither, and then there is no Scorch to fear. Both players know which
     * cards the decks are built from.
     */
    private static boolean opponentDeckHasScorch(GameState state, int opp)
    {
        for (CardDef card : state.db().deckPool(state.player(opp).faction()))
        {
            if (state.pool().contains(card) && (card.special() == SpecialType.SCORCH || (card.isUnit() && card.ability() == Ability.SCORCH)))
            {
                return true;
            }
        }
        return false;
    }

    /** True if the card is visible on the battlefield, in the weather zone or in a discard pile. */
    private static boolean isPublic(GameState state, int cardId)
    {
        for (int p = 0; p < 2; p++)
        {
            PlayerState player = state.player(p);
            if (player.findOnBoard(cardId) != null || player.findInDiscard(cardId) != null)
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
        for (CardInstance weather : state.weather())
        {
            if (weather.id() == cardId)
            {
                return true;
            }
        }
        return false;
    }

    // ================================================================== other decisions

    /**
     * Redraw: swap the least useful card if it's clearly worse than an average card from the deck. Extra copies of a
     * muster group are swapped first: they still come out with the muster from the deck, so the swap is free value.
     */
    int chooseRedraw(GameState state, int me, int redrawsLeft)
    {
        if (difficulty == Difficulty.EASY)
        {
            return -1;
        }
        PlayerState player = state.player(me);
        double average = CardValues.averageDeckValue(state, me);
        Set<Integer> pulledByOthers = musterDuplicates(player.hand());
        Map<SpecialType, Integer> weatherCount = new HashMap<>();
        int weatherTotal = 0;
        CardInstance worst = null;
        double worstValue = Double.MAX_VALUE;
        for (CardInstance card : player.hand())
        {
            double value = CardValues.keepValue(state, me, card);
            if (pulledByOthers.contains(card.id()))
            {
                value = 0.5;
            }
            if (card.def().isWeather())
            {
                SpecialType type = card.def().special();
                Integer seen = weatherCount.get(type);
                int count = seen == null ? 1 : seen + 1;
                weatherCount.put(type, count);
                weatherTotal++;
                if (count > 1)
                {
                    value = 1;
                }
                else if (weatherTotal > 2)
                {
                    value = Math.min(value, 2);
                }
            }
            if (value < worstValue)
            {
                worst = card;
                worstValue = value;
            }
        }
        if (worst != null && worstValue < average - 0.5)
        {
            return worst.id();
        }
        return -1;
    }

    /** Hand cards that another (kept) hand card will muster anyway: all but one card of each muster group. */
    private static Set<Integer> musterDuplicates(List<CardInstance> hand)
    {
        Set<Integer> flagged = new HashSet<>();
        for (CardInstance card : hand)
        {
            String group = card.def().group();
            if (group.isEmpty())
            {
                continue;
            }
            for (CardInstance other : hand)
            {
                if (other != card && !flagged.contains(other.id()) && group.equals(other.def().summons()))
                {
                    flagged.add(card.id());
                    break;
                }
            }
        }
        return flagged;
    }

    List<Integer> chooseCards(GameState state, int me, ChoiceRequest request)
    {
        return ChoicePolicy.choose(state, me, request);
    }

    RowType chooseRow(GameState state, int me, CardSnapshot card, List<RowType> options)
    {
        return ChoicePolicy.bestRow(state, me, card, options);
    }

    /** Remembers cards seen in the opponent's hand. */
    void remember(List<CardSnapshot> cards)
    {
        for (CardSnapshot card : cards)
        {
            revealed.put(card.id(), card.def());
        }
    }
}
