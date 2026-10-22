package com.numankaraaslan.gwent.engine.snapshot;

import com.numankaraaslan.gwent.model.CardDef;

/**
 * Immutable view of one card at one moment.
 *
 * @param id       game-unique card id (stable across zones; use it to match UI nodes between snapshots)
 * @param def      card definition (immutable, shared)
 * @param strength current strength: on the battlefield it includes weather / bond / morale / horn; elsewhere it is the base strength
 * @param owner    player whose deck the card came from
 */
public record CardSnapshot(int id, CardDef def, int strength, int owner) {

    /** Strength is above the printed value (show it green). */
    public boolean isBuffed() {
        return def.isUnit() && strength > def.strength();
    }

    /** Strength is below the printed value (show it red). */
    public boolean isDebuffed() {
        return def.isUnit() && strength < def.strength();
    }
}
