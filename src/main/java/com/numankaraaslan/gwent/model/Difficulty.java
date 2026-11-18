package com.numankaraaslan.gwent.model;

/** Strength of the computer opponent. Hard+ is Hard plus the {@link Advantage}s picked on the setup screen. */
public enum Difficulty
{
    EASY("Easy"), NORMAL("Normal"), HARD("Hard"), HARD_PLUS("Hard+");

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
