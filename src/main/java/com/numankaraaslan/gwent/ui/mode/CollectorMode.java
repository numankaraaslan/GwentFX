package com.numankaraaslan.gwent.ui.mode;

import com.numankaraaslan.gwent.collector.CardCollection;
import com.numankaraaslan.gwent.collector.CollectionFile;
import com.numankaraaslan.gwent.collector.Reward;
import com.numankaraaslan.gwent.collector.Tier;
import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.CardPool;
import com.numankaraaslan.gwent.model.Difficulty;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.GameSettings;
import com.numankaraaslan.gwent.ui.Navigator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;

/**
 * Collector Mode: both decks are built from the cards you own, the starter cards plus everything unlocked so far, and
 * every win unlocks one more card from the tier of the difficulty you beat. The collection is read from its file when
 * the mode is opened and saved after every win.
 */
public final class CollectorMode implements GameMode {

    private static final String DECK_NOTE = "Your deck holds the cards you own: the starter cards plus every card you have won, with all their copies. The computer's deck is built from the same cards.";

    private final CardDatabase db = CardDatabase.get();
    private final CardPool everything = CardPool.full(db);
    private final Random random = new Random();
    /** The collection, read when the mode is opened and grown by every win. */
    private CardCollection collection = CollectionFile.load(db);

    @Override
    public String title() {
        return "Collector Mode";
    }

    @Override
    public String subtitle() {
        return "Every win unlocks a card: mid-strength units on Easy, strong cards on Normal, heroes on Hard.";
    }

    @Override
    public List<SetupLink> setupLinks(Navigator navigator) {
        return List.of(new SetupLink("My Collection", new Runnable() {
            @Override
            public void run() {
                navigator.showCollection();
            }
        }));
    }

    @Override
    public String deckNote() {
        return DECK_NOTE;
    }

    @Override
    public String deckSummary(Faction faction) {
        CardPool owned = collection.pool();
        return owned.count(faction) + "/" + everything.count(faction) + " faction cards\n" + owned.count(Faction.NEUTRAL) + "/" + everything.count(Faction.NEUTRAL) + " neutral cards";
    }

    @Override
    public OptionalInt cardsLeft(Difficulty difficulty) {
        return OptionalInt.of(collection.remaining(Tier.rewardedBy(difficulty)).size());
    }

    @Override
    public CardPool pool() {
        return collection.pool();
    }

    /**
     * A win unlocks a random card of the difficulty's tier and saves the collection; the result screen shows the card.
     * The cards in the file are added first, in case another window has won some, and every save writes the whole
     * collection, so a card whose save failed is saved again with the next win. A deleted file means the player started
     * over.
     */
    @Override
    public ResultNote gameEnded(GameSettings settings, int winner) {
        if (winner != GameState.HUMAN) {
            return null;
        }
        Tier tier = Tier.rewardedBy(settings.difficulty() == null ? Difficulty.NORMAL : settings.difficulty());
        if (CollectionFile.exists()) {
            collection.addUnlocked(CollectionFile.load(db));
        } else {
            collection = CollectionFile.load(db);
        }
        Reward reward = collection.unlockRandom(tier, random);
        CollectionFile.save(collection);
        if (reward == null) {
            return new ResultNote(null, tier.displayName() + " wins have no cards left to unlock.", List.of());
        }
        String title = reward.cards().size() == 1 ? "New card unlocked in collection!" : "New cards unlocked in collection!";
        return new ResultNote(title, null, reward.cards());
    }

    @Override
    public void openSetup(Navigator navigator) {
        navigator.showCollectorMode();
    }
}
