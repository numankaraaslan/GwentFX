package com.numankaraaslan.gwent.collector;

import com.numankaraaslan.gwent.model.CardDef;
import java.util.ArrayList;
import java.util.List;

/**
 * What one win unlocks in Collector Mode: a card with all its copies, or a whole Muster family whose cards have
 * different names (the Crones, the Vampires), so Muster works as soon as you own it. Starter rewards are owned from the
 * start.
 *
 * @param cards the cards that are unlocked together, in file order
 * @param tier  the difficulty whose wins unlock them; a family takes the highest tier of its cards
 */
public record Reward(List<CardDef> cards, Tier tier) {

    /** The card names, each with its number of copies when there are several: "Dol Blathanna Scout ×3". */
    public String describe() {
        List<String> names = new ArrayList<>();
        for (CardDef card : cards) {
            names.add(card.copies() > 1 ? card.name() + " ×" + card.copies() : card.name());
        }
        return String.join(", ", names);
    }
}
