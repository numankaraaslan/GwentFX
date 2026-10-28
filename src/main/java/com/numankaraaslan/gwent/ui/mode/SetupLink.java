package com.numankaraaslan.gwent.ui.mode;

/**
 * An extra button in the header of a game mode's setup screen, e.g. "My Collection" in Collector Mode.
 *
 * @param label  the button text
 * @param action what the button does
 */
public record SetupLink(String label, Runnable action) {
}
