package com.numankaraaslan.gwent.ui.mode;

import com.numankaraaslan.gwent.model.CardPool;
import com.numankaraaslan.gwent.model.Difficulty;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.GameSettings;
import com.numankaraaslan.gwent.ui.Navigator;
import java.util.List;
import java.util.OptionalInt;

/**
 * Everything that differs between the two ways to play: {@link FreePlayMode} (every card) and {@link CollectorMode}
 * (only the cards you have unlocked). The setup and game screens only talk to this interface, so they never check which
 * mode they are in.
 */
public interface GameMode {

    /** Title of the setup screen. */
    String title();

    /** The line next to the title of the setup screen. */
    String subtitle();

    /** Extra buttons in the header of the setup screen (empty for none). */
    List<SetupLink> setupLinks(Navigator navigator);

    /** Note under the rule options of the setup screen: what the decks are made of. */
    String deckNote();

    /** Text in the faction tile of the setup screen, one or two lines: how many cards a deck of this faction holds. */
    String deckSummary(Faction faction);

    /** Shown under the name of a difficulty button ("47 cards left"): how many more cards wins there unlock; empty for none. */
    OptionalInt cardsLeft(Difficulty difficulty);

    /** The cards both decks are built from, as of now. Asked every time a game starts. */
    CardPool pool();

    /**
     * Called once when a game is over, with its settings and the winner (GameState.HUMAN, GameState.AI or
     * GameState.DRAW). Returns what to add to the result screen, or null for nothing.
     */
    ResultNote gameEnded(GameSettings settings, int winner);

    /** Opens the setup screen of this mode ("New game" on the result screen). */
    void openSetup(Navigator navigator);
}
