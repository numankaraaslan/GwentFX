package com.numankaraaslan.gwent.model;

/** The three combat rows each player has on the battlefield. */
public enum RowType
{
    CLOSE("Close Combat"), RANGED("Ranged Combat"), SIEGE("Siege Combat");

    private final String displayName;

    RowType(String displayName)
    {
        this.displayName = displayName;
    }

    public String displayName()
    {
        return displayName;
    }
}
