package com.numankaraaslan.gwent.model;

/**
 * Immutable definition of a leader card (one entry of leaders.json).
 *
 * @param id          unique key, e.g. "nr_foltest_king_of_temeria"
 * @param name        display name, e.g. "Foltest: King of Temeria"
 * @param faction     faction the leader belongs to
 * @param ability     what the leader does
 * @param description in-game ability text
 * @param expansion   which part of the game the leader comes from
 */
public record LeaderDef(String id, String name, Faction faction, LeaderAbility ability, String description, Expansion expansion)
{

    public boolean isPassive()
    {
        return ability.isPassive();
    }
}
