package com.numankaraaslan.gwent.model;

/** What can be given to the computer on Hard+, on top of playing like Hard. Each one is picked on the setup screen. */
public enum Advantage
{
    EXTRA_CARD("Extra card", "The computer starts with one more card."), CARD_EACH_ROUND("Card each round", "The computer draws a card when rounds 2 and 3 start."), NO_REDRAWS("No redraws", "You can't redraw cards. The computer still can.");

    private final String displayName;
    private final String description;

    Advantage(String displayName, String description)
    {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName()
    {
        return displayName;
    }

    public String description()
    {
        return description;
    }
}
