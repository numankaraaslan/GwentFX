package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.ui.Navigator;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/** Button handler that switches to another screen, opens the releases page or quits the game. */
final class MenuAction implements EventHandler<ActionEvent> {

    /** Where a menu button leads. */
    enum Target {
        MAIN_MENU,
        NEW_GAME,
        GALLERY,
        RULES,
        RELEASES,
        QUIT
    }

    private final Navigator navigator;
    private final Target target;

    MenuAction(Navigator navigator, Target target) {
        this.navigator = navigator;
        this.target = target;
    }

    @Override
    public void handle(ActionEvent event) {
        switch (target) {
            case MAIN_MENU:
                navigator.showMainMenu();
                break;
            case NEW_GAME:
                navigator.showNewGame();
                break;
            case GALLERY:
                navigator.showGallery();
                break;
            case RULES:
                navigator.showRules();
                break;
            case RELEASES:
                navigator.showReleases();
                break;
            case QUIT:
                navigator.quit();
                break;
            default:
                break;
        }
    }
}
