package com.numankaraaslan.gwent.engine;

/**
 * Everything that happens in a game, in the order it happens. Each event arrives with a snapshot of the state
 * AFTER the event. "player" and "cardIds" meaning per type is documented below; unused values are -1 / empty.
 */
public enum EventType {
    /** Game begins. player -1. */
    GAME_STARTED,
    /** Emhyr: The White Flame cancelled a leader. player = the player whose leader was cancelled. */
    LEADER_CANCELLED,
    /** Starting hand dealt. player = who; cardIds = the dealt cards. */
    HAND_DEALT,
    /** A card was swapped during the redraw phase. player = who; cardIds = [old card id, new card id]. */
    CARD_REDRAWN,
    /** A player finished redrawing. player = who. */
    REDRAW_FINISHED,
    /** Who starts the first round was decided (coin toss or Scoia'tael choice). player = who goes first. */
    FIRST_PLAYER_CHOSEN,
    /** A round begins. player = who acts first. */
    ROUND_STARTED,
    /** A player must decide a move now. player = whose turn. */
    TURN_STARTED,
    /** A unit card was played from the hand. player = who played it; cardIds = [card]; row = row it went to (spies: on the other side). */
    CARD_PLAYED,
    /** A unit came back from the discard pile through a Medic. player = who revived it; cardIds = [card]; row = its row. */
    CARD_REVIVED,
    /** Units appeared on the battlefield without being played (Muster from the deck, Avenger, Skellige perk). player = side they appeared on; cardIds = the cards. */
    CARDS_SUMMONED,
    /** Cards moved from a deck into a hand (spy draw, Northern Realms perk...). player = who drew; cardIds = the cards. */
    CARDS_DRAWN,
    /** Units were destroyed by a scorch effect and moved to the discard piles. cardIds = destroyed cards. Their nodes are still in the previous snapshot. */
    CARDS_DESTROYED,
    /** Decoy swapped with a unit. player = who; cardIds = [decoy id, unit id returned to hand]; row = the row. */
    CARD_RETURNED,
    /** A berserker transformed (same id, new definition). player = side; cardIds = [card]. */
    CARD_TRANSFORMED,
    /** A card moved from a discard pile (or deck, via leader choice) into a hand. player = who took it; cardIds = [card]. */
    CARD_TAKEN,
    /** Cards were discarded from a hand (Eredin: Destroyer of Worlds). player = who; cardIds = the cards. */
    CARDS_DISCARDED,
    /** A weather card entered the weather zone (or fizzled if already active). player = who played it; cardIds = [card]. */
    WEATHER_PLAYED,
    /** All weather was cleared. player = who cleared it; cardIds = removed weather cards (+ the Clear Weather card). */
    WEATHER_CLEARED,
    /** Commander's Horn / Mardroeme placed in a special slot, or a Scorch card played. player = who; cardIds = [card]; row = target row (null for Scorch). */
    SPECIAL_PLAYED,
    /** A leader ability was activated. player = who. The effect follows as further events. */
    LEADER_USED,
    /** A leader ability doubled one of the player's rows for the round. player = who; row = the row. */
    LEADER_HORN,
    /** Francesca: Hope of the Aen Seidhe moved agile units. player = who; cardIds = moved cards. */
    AGILE_MOVED,
    /** Crach an Craite shuffled both discard piles back into the decks. player = who used it. */
    DECKS_SHUFFLED,
    /** Emhyr: Emperor of Nilfgaard looked at cards in the opponent's hand. player = who looked. */
    CARDS_REVEALED,
    /** A player passed for the rest of the round. player = who. */
    PLAYER_PASSED,
    /** The round is over. player = round winner or GameState.DRAW. Gems are already updated in the snapshot. */
    ROUND_ENDED,
    /** A faction perk fired (Nilfgaard tie win, Northern Realms draw, Monsters keep, Skellige return). player = whose perk; cardIds = affected cards. */
    FACTION_ABILITY,
    /** All cards left the battlefield at the end of a round. */
    BOARD_CLEARED,
    /** Game over. player = winner or GameState.DRAW. */
    GAME_ENDED,
    /** Plain information for the log (e.g. "Scorch found nothing to burn"). */
    MESSAGE
}
