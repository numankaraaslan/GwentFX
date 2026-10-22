package com.numankaraaslan.gwent.engine;

import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderAbility;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.model.RowType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Everything one player owns during a game. Mutated only by the engine thread. */
public final class PlayerState {

    public static final int STARTING_GEMS = 2;

    private final int index;
    private final String name;
    private final Faction faction;
    private final LeaderDef leader;
    private final List<CardInstance> deck = new ArrayList<>();
    private final List<CardInstance> hand = new ArrayList<>();
    private final List<CardInstance> discard = new ArrayList<>();
    private final Map<RowType, RowState> rows = new EnumMap<>(RowType.class);
    private boolean leaderUsed;
    private boolean leaderDisabled;
    private int gems = STARTING_GEMS;
    private boolean passed;

    public PlayerState(int index, String name, Faction faction, LeaderDef leader) {
        this.index = index;
        this.name = name;
        this.faction = faction;
        this.leader = leader;
        for (RowType type : RowType.values()) {
            rows.put(type, new RowState(type));
        }
    }

    public int index() {
        return index;
    }

    /** "You" or "Opponent"; used in log messages. */
    public String name() {
        return name;
    }

    public Faction faction() {
        return faction;
    }

    public LeaderDef leader() {
        return leader;
    }

    /** Top of the deck is index 0. */
    public List<CardInstance> deck() {
        return deck;
    }

    public List<CardInstance> hand() {
        return hand;
    }

    public List<CardInstance> discard() {
        return discard;
    }

    public RowState row(RowType type) {
        return rows.get(type);
    }

    /** Rows in CLOSE, RANGED, SIEGE order. */
    public List<RowState> rows() {
        List<RowState> result = new ArrayList<>();
        for (RowType type : RowType.values()) {
            result.add(rows.get(type));
        }
        return result;
    }

    /** All cards in this player's rows (units and decoys; not the special slots). */
    public List<CardInstance> boardCards() {
        List<CardInstance> result = new ArrayList<>();
        for (RowType type : RowType.values()) {
            result.addAll(rows.get(type).cards());
        }
        return result;
    }

    public boolean leaderUsed() {
        return leaderUsed;
    }

    public void setLeaderUsed(boolean leaderUsed) {
        this.leaderUsed = leaderUsed;
    }

    /** True when the opponent's Emhyr: The White Flame cancelled this player's leader. */
    public boolean leaderDisabled() {
        return leaderDisabled;
    }

    public void setLeaderDisabled(boolean leaderDisabled) {
        this.leaderDisabled = leaderDisabled;
    }

    /** True if this player's leader has the given ability and it hasn't been cancelled. */
    public boolean leaderActive(LeaderAbility ability) {
        return !leaderDisabled && leader.ability() == ability;
    }

    /** Remaining round "lives" (red gems). The game is lost when they reach 0. */
    public int gems() {
        return gems;
    }

    public void loseGem() {
        if (gems > 0) {
            gems--;
        }
    }

    public boolean passed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public CardInstance findInHand(int cardId) {
        return find(hand, cardId);
    }

    public CardInstance findInDeck(int cardId) {
        return find(deck, cardId);
    }

    public CardInstance findInDiscard(int cardId) {
        return find(discard, cardId);
    }

    public CardInstance findOnBoard(int cardId) {
        for (RowType type : RowType.values()) {
            CardInstance card = find(rows.get(type).cards(), cardId);
            if (card != null) {
                return card;
            }
        }
        return null;
    }

    private static CardInstance find(List<CardInstance> cards, int cardId) {
        for (CardInstance card : cards) {
            if (card.id() == cardId) {
                return card;
            }
        }
        return null;
    }

    public PlayerState copy() {
        PlayerState copy = new PlayerState(index, name, faction, leader);
        copyCards(deck, copy.deck);
        copyCards(hand, copy.hand);
        copyCards(discard, copy.discard);
        for (RowType type : RowType.values()) {
            copy.rows.put(type, rows.get(type).copy());
        }
        copy.leaderUsed = leaderUsed;
        copy.leaderDisabled = leaderDisabled;
        copy.gems = gems;
        copy.passed = passed;
        return copy;
    }

    private static void copyCards(List<CardInstance> from, List<CardInstance> to) {
        for (CardInstance card : from) {
            to.add(card.copy());
        }
    }
}
