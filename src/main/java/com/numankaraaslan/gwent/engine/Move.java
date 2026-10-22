package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.model.RowType;

/**
 * A complete turn decision. The engine hands out the list of legal moves and the player picks one of them.
 *
 * <ul>
 *   <li>Unit: {@code row} = row to place it in. For spies this row is on the OPPONENT's side.</li>
 *   <li>Commander's Horn / Mardroeme: {@code row} = own row whose special slot gets the card.</li>
 *   <li>Decoy: {@code row} = own row of the target, {@code targetId} = id of the unit taken back to the hand.</li>
 *   <li>Weather, Clear Weather, Scorch: {@code row} = null.</li>
 * </ul>
 *
 * @param type     PLAY, LEADER or PASS
 * @param cardId   id of the hand card (PLAY only, else -1)
 * @param row      target row (see above) or null
 * @param targetId decoy target id, else -1
 */
public record Move(MoveType type, int cardId, RowType row, int targetId) {

    public static Move pass() {
        return new Move(MoveType.PASS, -1, null, -1);
    }

    public static Move leader() {
        return new Move(MoveType.LEADER, -1, null, -1);
    }

    public static Move play(int cardId, RowType row) {
        return new Move(MoveType.PLAY, cardId, row, -1);
    }

    public static Move decoy(int cardId, RowType row, int targetId) {
        return new Move(MoveType.PLAY, cardId, row, targetId);
    }

    public boolean isPass() {
        return type == MoveType.PASS;
    }

    public boolean isLeader() {
        return type == MoveType.LEADER;
    }

    public boolean isPlay() {
        return type == MoveType.PLAY;
    }
}
