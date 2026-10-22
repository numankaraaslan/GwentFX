package com.numankaraaslan.gwent.model;

/** Which part of The Witcher 3 a card or leader comes from. */
public enum Expansion
{
    BASE("Base game"), HEARTS_OF_STONE("Hearts of Stone"), BLOOD_AND_WINE("Blood and Wine");

    private final String displayName;

    Expansion(String displayName)
    {
        this.displayName = displayName;
    }

    public String displayName()
    {
        return displayName;
    }
}
