package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.ChoiceRequest;
import com.numankaraaslan.gwent.engine.Decider;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.model.RowType;
import java.util.List;

/** Instant, non-blocking decisions for the AI's look-ahead simulations (no delays, nothing is shown). */
final class SimulationDecider implements Decider
{

    @Override
    public List<Integer> chooseCards(GameState state, int player, ChoiceRequest request)
    {
        return ChoicePolicy.choose(state, player, request);
    }

    @Override
    public RowType chooseRow(GameState state, int player, CardSnapshot card, List<RowType> options)
    {
        return ChoicePolicy.bestRow(state, player, card, options);
    }

    @Override
    public void reveal(GameState state, int player, String title, List<CardSnapshot> cards)
    {
        // nothing to show in a simulation
    }
}
