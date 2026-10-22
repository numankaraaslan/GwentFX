package com.numankaraaslan.gwent.engine.snapshot;

import com.numankaraaslan.gwent.model.RowType;
import java.util.List;

/**
 * Immutable view of one combat row of one player.
 *
 * @param type            close / ranged / siege
 * @param cards           cards in the row, in display order (units and decoys)
 * @param special         Commander's Horn or Mardroeme in the special slot, or null
 * @param leaderHorn      a leader ability doubles this row this round (show a horn in the slot)
 * @param hornActive      the row is doubled by anything (special horn, leader horn or a horn unit like Dandelion)
 * @param mardroemeActive berserkers in this row transform (Mardroeme in the slot or Ermion in the row)
 * @param weatherActive   bad weather affects this row type
 * @param score           sum of the current strengths in the row
 */
public record RowSnapshot(RowType type, List<CardSnapshot> cards, CardSnapshot special, boolean leaderHorn, boolean hornActive, boolean mardroemeActive, boolean weatherActive, int score) {
}
