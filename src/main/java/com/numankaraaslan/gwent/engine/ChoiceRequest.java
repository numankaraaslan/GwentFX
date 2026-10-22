package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import java.util.List;

/**
 * A request to pick between {@code min} and {@code max} cards out of {@code options}.
 *
 * @param kind    why the choice is needed
 * @param title   prompt to show, e.g. "Choose a card to revive"
 * @param options the cards to choose from (never empty)
 * @param min     minimum number of cards to pick
 * @param max     maximum number of cards to pick
 */
public record ChoiceRequest(ChoiceKind kind, String title, List<CardSnapshot> options, int min, int max) {

    public ChoiceRequest {
        options = List.copyOf(options);
    }
}
