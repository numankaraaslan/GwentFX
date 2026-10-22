package com.numankaraaslan.gwent.ui;

import com.numankaraaslan.gwent.model.GameSettings;

/** Screen switching. Implemented by {@link GwentApp}; call on the JavaFX thread. */
public interface Navigator
{

    void showMainMenu();

    void showNewGame();

    /** Starts a new game with these settings (also used for "play again"). */
    void startGame(GameSettings settings);

    void showGallery();

    void showRules();

    void quit();
}
