package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.PlayerState;
import com.numankaraaslan.gwent.engine.Rules;
import com.numankaraaslan.gwent.model.Faction;

/**
 * The strategic situation of the current round from the AI's point of view (only public information).
 *
 * @param me        the AI's player index
 * @param opp       the opponent's player index
 * @param myGems    the AI's remaining round lives
 * @param oppGems   the opponent's remaining round lives
 * @param myScore   the AI's current total strength
 * @param oppScore  the opponent's current total strength
 * @param myHand    cards in the AI's hand
 * @param oppHand   cards in the opponent's hand (only the count is known)
 * @param oppPassed the opponent passed this round
 * @param iWinTies  the AI wins tied rounds (Nilfgaard against a non-Nilfgaard opponent)
 * @param oppWinsTies the opponent wins tied rounds (Nilfgaard against a non-Nilfgaard AI)
 * @param startHand starting hand size of this game (for "don't spend more than half in round 1")
 * @param oppLeaderReady the opponent's active leader ability is still unused and not cancelled (public information)
 */
record RoundContext(int me, int opp, int myGems, int oppGems, int myScore, int oppScore, int myHand, int oppHand, boolean oppPassed, boolean iWinTies, boolean oppWinsTies, int startHand, boolean oppLeaderReady)
{

    static RoundContext of(GameState state, int me)
    {
        int opp = Rules.opponent(me);
        PlayerState mine = state.player(me);
        PlayerState theirs = state.player(opp);
        boolean iWinTies = mine.faction() == Faction.NILFGAARD && theirs.faction() != Faction.NILFGAARD;
        boolean oppWinsTies = theirs.faction() == Faction.NILFGAARD && mine.faction() != Faction.NILFGAARD;
        boolean oppLeaderReady = !theirs.leaderUsed() && !theirs.leaderDisabled() && !theirs.leader().isPassive();
        return new RoundContext(me, opp, mine.gems(), theirs.gems(), Rules.score(state, me), Rules.score(state, opp), mine.hand().size(), theirs.hand().size(), theirs.passed(), iWinTies, oppWinsTies, state.settings().handSize(), oppLeaderReady);
    }

    /** My score minus the opponent's score. */
    int diff()
    {
        return myScore - oppScore;
    }

    /** True if the round would be mine if it ended now. */
    boolean winning()
    {
        return winningWith(diff());
    }

    /** True if a score difference of {@code scoreDiff} wins the round for me. */
    boolean winningWith(int scoreDiff)
    {
        return scoreDiff > 0 || (scoreDiff == 0 && iWinTies);
    }

    /** True if the round would be the opponent's if it ended now: behind, or tied when they win ties (Nilfgaard). */
    boolean losing()
    {
        return diff() < 0 || (diff() == 0 && oppWinsTies);
    }

    /** Strength I still have to gain to be winning (0 if I'm winning already). */
    int need()
    {
        if (winning())
        {
            return 0;
        }
        return iWinTies ? -diff() : -diff() + 1;
    }

    /** Both players have one gem left: whoever wins this round wins the game. */
    boolean finalRound()
    {
        return myGems <= 1 && oppGems <= 1;
    }

    /** I lose the game if I lose this round, the opponent doesn't. */
    boolean mustWin()
    {
        return myGems <= 1 && oppGems >= 2;
    }

    /** The opponent loses the game if they lose this round, I don't. */
    boolean oppMustWin()
    {
        return myGems >= 2 && oppGems <= 1;
    }

    /** The opponent can't change the score any more: no cards in hand and no leader ability left to use. */
    boolean oppOutOfMoves()
    {
        return oppHand == 0 && !oppLeaderReady;
    }

    /** Nothing has been put on the battlefield yet this round (or only 0-strength cards). */
    boolean emptyBoard()
    {
        return myScore == 0 && oppScore == 0;
    }

    int cardAdvantage()
    {
        return myHand - oppHand;
    }

    /**
     * How much card potential matters compared to strength on the board right now: 0 in the final round (cards are
     * worth nothing afterwards), high in round 1 (every card is needed later).
     */
    double conserve()
    {
        if (finalRound())
        {
            return 0;
        }
        if (mustWin())
        {
            return 0.35;
        }
        if (oppMustWin())
        {
            return 0.8;
        }
        return 0.9;
    }

    /** What the unused leader ability is worth keeping for later. */
    double leaderValue()
    {
        if (finalRound())
        {
            return 0;
        }
        return mustWin() ? 4 : 8;
    }

    /** In round 1, don't go below this many cards in hand. */
    int roundOneHandFloor()
    {
        return Math.max(3, startHand / 2 - 1);
    }
}
