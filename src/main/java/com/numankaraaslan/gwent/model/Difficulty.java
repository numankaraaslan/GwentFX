package com.numankaraaslan.gwent.model;

/** Strength of the computer opponent. */
public enum Difficulty
{
    EASY("Easy"), NORMAL("Normal"), HARD("Hard");

    private final String displayName;

    Difficulty(String displayName)
    {
        this.displayName = displayName;
    }

    public String displayName()
    {
        return displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
