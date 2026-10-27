package com.numankaraaslan.gwent.ui;

import com.numankaraaslan.gwent.model.GameSettings;

/** Screen switching (and the releases page in the browser). Implemented by {@link GwentApp}; call on the JavaFX thread. */
public interface Navigator
{

    void showMainMenu();

    void showNewGame();

    /** Starts a new game with these settings (also used for "play again"). */
    void startGame(GameSettings settings);

    void showGallery();

    void showRules();

    /** Opens the GitHub releases page in the default browser, where new versions can be downloaded. */
    void showReleases();

    void quit();
}
