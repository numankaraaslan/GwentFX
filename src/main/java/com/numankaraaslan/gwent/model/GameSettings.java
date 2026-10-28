package com.numankaraaslan.gwent.model;

/**
 * Everything chosen on the setup screen of Free Play or Collector Mode. Which cards the decks are built from is not a
 * choice: the game mode decides it.
 *
 * @param playerFaction faction of the human player
 * @param playerLeader  leader of the human player, or null for a random leader of that faction
 * @param aiFaction     faction of the computer, or null for a random faction
 * @param aiLeader      leader of the computer, or null for a random leader of its faction
 * @param handSize      cards dealt at the start of the game (original: 10)
 * @param redraws       cards that may be swapped right after the deal (original: 2)
 * @param difficulty    computer opponent strength
 */
public record GameSettings(Faction playerFaction, LeaderDef playerLeader, Faction aiFaction, LeaderDef aiLeader, int handSize, int redraws, Difficulty difficulty)
{

    public static final int DEFAULT_HAND_SIZE = 10;
    public static final int DEFAULT_REDRAWS = 2;

    /** Original-rules defaults: random everything, 10 cards, 2 redraws. */
    public static GameSettings defaults(Faction playerFaction)
    {
        return new GameSettings(playerFaction, null, null, null, DEFAULT_HAND_SIZE, DEFAULT_REDRAWS, Difficulty.NORMAL);
    }
}
