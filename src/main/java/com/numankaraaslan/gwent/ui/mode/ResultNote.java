package com.numankaraaslan.gwent.ui.mode;

import com.numankaraaslan.gwent.model.CardDef;
import java.util.List;

/**
 * What a game mode adds to the result screen, e.g. the card a Collector Mode win unlocked.
 *
 * @param title              a heading such as "New card unlocked in collection!", or null for none
 * @param text               a line of text, or null for none
 * @param cards              the cards to show with it (empty for none)
 * @param collectionComplete whether this game won the last card of the collection: the result screen then celebrates
 *                           with a popup
 */
public record ResultNote(String title, String text, List<CardDef> cards, boolean collectionComplete) {
}
