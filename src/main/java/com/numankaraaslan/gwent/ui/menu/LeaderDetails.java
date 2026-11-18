package com.numankaraaslan.gwent.ui.menu;

import com.numankaraaslan.gwent.model.Expansion;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderDef;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A "Leader details" panel of the new game screen, text only (the leader card itself is shown big in the main area):
 * one leader's ability in large type, or every leader a random pick chooses from. The screen has one for your leader and
 * one for the computer's, under it, both in a scroll pane, so a long list simply scrolls.
 */
final class LeaderDetails extends VBox {

    private static final String DOT = " · ";

    private final Label name = MenuUi.wrapping("", "ng-details-name");
    private final Label kind = MenuUi.wrapping("", "ng-details-kind");
    private final Label faction = MenuUi.wrapping("", "ng-details-faction");
    private final Region line = MenuUi.separator();
    private final Label ability = MenuUi.wrapping("", "ng-details-ability");

    LeaderDetails() {
        getStyleClass().add("ng-details");
    }

    /** One leader: full name, whether the ability is active or passive, faction and the ability text. */
    void showLeader(LeaderDef leader) {
        name.setText(leader.name());
        kind.setText(leader.isPassive() ? "Passive ability — always on" : "Active ability — once per game");
        faction.setText(factionText(leader));
        ability.setText(leader.description());
        getChildren().setAll(name, kind, faction, line, ability);
    }

    /**
     * The computer's pick: its leader as in {@link #showLeader}, or what is left to chance, followed by its faction perk
     * (for the computer, the screen shows the perk nowhere else). Null means random, for the faction and for the leader.
     */
    void showOpponent(Faction side, LeaderDef leader) {
        if (side == null) {
            name.setText("Random");
            ability.setText("The computer plays a random faction with a random leader.");
            getChildren().setAll(name, line, ability);
            return;
        }
        if (leader == null) {
            name.setText("Random leader");
            faction.setText(side.displayName());
            ability.setText("One of the " + side.shortName() + " leaders is picked when the game starts.");
            getChildren().setAll(name, faction, line, ability);
        } else {
            showLeader(leader);
        }
        VBox perk = new VBox(MenuUi.wrapping("Faction perk", "ng-details-entry-name"), MenuUi.wrapping(side.abilityDescription(), "ng-details-entry-text"));
        perk.getStyleClass().add("ng-details-entry");
        getChildren().add(perk);
    }

    /** The leaders a random pick chooses from, each with its ability. */
    void showRandom(List<LeaderDef> leaders) {
        getChildren().setAll(MenuUi.wrapping("Random leader — one of:", "ng-details-title"));
        for (LeaderDef leader : leaders) {
            Label entryName = MenuUi.wrapping(leader.name(), "ng-details-entry-name");
            Label entryText = MenuUi.wrapping((leader.isPassive() ? "Passive: " : "Once per game: ") + leader.description(), "ng-details-entry-text");
            VBox entry = new VBox(entryName, entryText);
            entry.getStyleClass().add("ng-details-entry");
            getChildren().add(entry);
        }
    }

    /** "Northern Realms", or "Skellige · Blood and Wine" for leaders of an expansion. */
    private static String factionText(LeaderDef leader) {
        Expansion expansion = leader.expansion();
        if (expansion == null || expansion == Expansion.BASE) {
            return leader.faction().displayName();
        }
        return leader.faction().displayName() + DOT + expansion.displayName();
    }
}
