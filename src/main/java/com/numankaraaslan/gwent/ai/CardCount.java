package com.numankaraaslan.gwent.ai;

import com.numankaraaslan.gwent.engine.CardInstance;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.PlayerState;
import com.numankaraaslan.gwent.engine.RowState;
import com.numankaraaslan.gwent.engine.Rules;
import com.numankaraaslan.gwent.model.Ability;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.SpecialType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Card counting, using only what a human player could know too. The opponent's deck list is public: every card of
 * their faction plus every neutral card in the game's card pool, with all copies. Every card of theirs that has shown up
 * since, on the battlefield, in the weather zone or in a discard pile, is crossed off, and so is any card of theirs in
 * the AI's own hand or deck. The rest is in their hand or their deck. Cards that Emperor of Nilfgaard showed in their
 * hand and that haven't been played since are known to be in the hand; the other hand cards could be any of the rest.
 */
final class CardCount
{

    /** Emperor of Nilfgaard showed a card that scorches the whole battlefield in the opponent's hand. */
    private final boolean scorchShown;
    /** The opponent's cards that scorch the whole battlefield and haven't shown up yet. */
    private final int unseenScorches;
    /** The opponent's hand cards the AI doesn't know. */
    private final int hiddenHand;
    /** The cards those can be: the hidden hand cards plus the opponent's deck. */
    private final int unknownCards;

    private CardCount(boolean scorchShown, int unseenScorches, int hiddenHand, int unknownCards)
    {
        this.scorchShown = scorchShown;
        this.unseenScorches = unseenScorches;
        this.hiddenHand = hiddenHand;
        this.unknownCards = unknownCards;
    }

    /**
     * Counts the opponent's cards.
     *
     * @param knownInHand the cards Emperor of Nilfgaard showed in the opponent's hand that haven't been played since
     */
    static CardCount of(GameState state, int me, Collection<CardDef> knownInHand)
    {
        int opp = Rules.opponent(me);
        PlayerState theirs = state.player(opp);
        int scorches = 0;
        for (CardDef card : state.db().deckPool(theirs.faction()))
        {
            if (state.pool().contains(card) && scorchesBattlefield(card))
            {
                scorches += card.copies();
            }
        }
        for (CardInstance card : seenCards(state, me))
        {
            if (card.owner() == opp && scorchesBattlefield(card.def()))
            {
                scorches--;
            }
        }
        boolean shown = false;
        for (CardDef card : knownInHand)
        {
            if (scorchesBattlefield(card))
            {
                shown = true;
            }
        }
        int hidden = Math.max(0, theirs.hand().size() - knownInHand.size());
        return new CardCount(shown, Math.max(0, scorches), hidden, hidden + theirs.deck().size());
    }

    /** The chance that the opponent's hand holds a card that scorches the whole battlefield: Scorch, or Clan Dimun Pirate. */
    double scorchChance()
    {
        if (scorchShown)
        {
            return 1;
        }
        return chanceInHand(unseenScorches);
    }

    /**
     * The chance that the hidden hand cards include at least one of {@code copies} cards among the unknown cards: 1 minus
     * the chance that every hidden hand card is one of the others.
     */
    private double chanceInHand(int copies)
    {
        int count = Math.min(copies, unknownCards);
        double none = 1;
        for (int i = 0; i < hiddenHand; i++)
        {
            none *= (double) Math.max(0, unknownCards - count - i) / (unknownCards - i);
        }
        return 1 - none;
    }

    /** Scorch, or a unit with the Scorch ability (Clan Dimun Pirate); not the row scorches like Villentretenmerth's. */
    private static boolean scorchesBattlefield(CardDef card)
    {
        return card.special() == SpecialType.SCORCH || (card.isUnit() && card.ability() == Ability.SCORCH);
    }

    /**
     * The cards the AI can see: everything on the battlefield, in the weather zone and in the discard piles, plus its own
     * hand and deck.
     */
    private static List<CardInstance> seenCards(GameState state, int me)
    {
        List<CardInstance> seen = new ArrayList<>();
        for (int p = 0; p < 2; p++)
        {
            PlayerState player = state.player(p);
            seen.addAll(player.boardCards());
            seen.addAll(player.discard());
            for (RowState row : player.rows())
            {
                if (row.special() != null)
                {
                    seen.add(row.special());
                }
            }
        }
        seen.addAll(state.weather());
        seen.addAll(state.player(me).hand());
        seen.addAll(state.player(me).deck());
        return seen;
    }
}
