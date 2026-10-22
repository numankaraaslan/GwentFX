package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.Move;

/**
 * Result of simulating one move on a copy of the game.
 *
 * @param move      the simulated move
 * @param gain      change of (my score - opponent score)
 * @param myScore   my total strength afterwards
 * @param oppScore  the opponent's total strength afterwards
 * @param handDelta change of my hand size (normal play -1, spy +1)
 * @param resources change of my hand's future value (negative = potential spent), leader use included
 * @param score     overall attractiveness: gain plus weighted resources, minus risks
 */
record MoveEval(Move move, int gain, int myScore, int oppScore, int handDelta, double resources, double score)
{

    int diffAfter()
    {
        return myScore - oppScore;
    }
}
