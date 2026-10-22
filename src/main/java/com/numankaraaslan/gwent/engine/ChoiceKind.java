package com.numankaraaslan.gwent.engine;

/** Why the engine asks a player to pick cards in the middle of resolving something. */
public enum ChoiceKind {
    /** Medic: pick a unit from your discard pile to play. */
    MEDIC,
    /** Eredin: Bringer of Death: pick a card from your discard pile to return to your hand. */
    RESTORE_FROM_DISCARD,
    /** Emhyr: The Relentless: pick a card from the opponent's discard pile to put in your hand. */
    DRAW_FROM_OPPONENT_DISCARD,
    /** Eredin: Destroyer of Worlds, step 1: pick 2 cards from your hand to discard. */
    DISCARD_FROM_HAND,
    /** Eredin: Destroyer of Worlds, step 2: pick 1 card from your deck to put in your hand. */
    DRAW_FROM_DECK,
    /** Eredin: King of the Wild Hunt: pick a weather card from your deck to play. */
    WEATHER_FROM_DECK
}
