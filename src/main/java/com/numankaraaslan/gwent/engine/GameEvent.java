package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.model.RowType;
import java.util.List;

/**
 * Something that happened in the game (see {@link EventType} for the meaning of the fields per type).
 *
 * @param type    what happened
 * @param player  acting player / affected side, or -1
 * @param cardIds ids of the cards involved (never null)
 * @param row     row involved, or null
 * @param text    human readable log line (may be empty)
 */
public record GameEvent(EventType type, int player, List<Integer> cardIds, RowType row, String text) {

    public GameEvent {
        cardIds = cardIds == null ? List.of() : List.copyOf(cardIds);
        text = text == null ? "" : text;
    }
}
