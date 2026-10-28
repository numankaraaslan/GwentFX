package com.numankaraaslan.gwent.ui;

import com.numankaraaslan.gwent.model.GameSettings;
import com.numankaraaslan.gwent.ui.mode.GameMode;

/** Screen switching (and the releases page in the browser). Implemented by {@link GwentApp}; call on the JavaFX thread. */
public interface Navigator
{

    void showMainMenu();

    /** Opens the setup screen of Free Play: every card is in the game. */
    void showFreePlay();

    /** Opens the setup screen of Collector Mode: only the cards you own are in the game. */
    void showCollectorMode();

    /** Opens My Collection: the cards you own in Collector Mode and the ones still to win. */
    void showCollection();

    /** Starts a new game with these settings in this mode (also used for "play again"). */
    void startGame(GameSettings settings, GameMode mode);

    void showGallery();

    void showRules();

    /** Opens the GitHub releases page in the default browser, where new versions can be downloaded. */
    void showReleases();

    void quit();
}
