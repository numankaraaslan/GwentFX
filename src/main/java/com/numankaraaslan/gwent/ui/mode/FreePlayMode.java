package com.numankaraaslan.gwent.ui.mode;

import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardPool;
import com.numankaraaslan.gwent.model.Difficulty;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.GameSettings;
import com.numankaraaslan.gwent.ui.Navigator;
import java.util.List;
import java.util.OptionalInt;

/** Free Play: both decks hold every card of the collection, and nothing is saved. */
public final class FreePlayMode implements GameMode {

    private static final String DECK_NOTE = "Your deck holds every card of your faction plus all neutral cards, each as many times as it exists in the original game. Your hand is dealt from it at random.";

    private final CardPool pool = CardPool.full(CardDatabase.get());

    @Override
    public String title() {
        return "Free Play";
    }

    @Override
    public String subtitle() {
        return "Choose your faction and leader, your opponent and the rules — then start the battle.";
    }

    @Override
    public List<SetupLink> setupLinks(Navigator navigator) {
        return List.of();
    }

    @Override
    public String deckNote() {
        return DECK_NOTE;
    }

    @Override
    public String deckSummary(Faction faction) {
        return "Deck: " + pool.count(faction) + " faction cards + " + pool.count(Faction.NEUTRAL) + " neutral cards";
    }

    @Override
    public OptionalInt cardsLeft(Difficulty difficulty) {
        return OptionalInt.empty();
    }

    @Override
    public CardPool pool() {
        return pool;
    }

    @Override
    public ResultNote gameEnded(GameSettings settings, int winner) {
        return null;
    }

    @Override
    public void openSetup(Navigator navigator) {
        navigator.showFreePlay();
    }
}
