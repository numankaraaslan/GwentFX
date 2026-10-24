package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.snapshot.PlayerSnapshot;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.Icons;
import com.numankaraaslan.gwent.ui.card.LeaderView;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Profile panel of one player in the left column: the leader card (title only, hovering shows it in full), the player's
 * name, the faction with the count of cards in hand under it, the total score badge (gold for the player who is ahead)
 * and, stacked on the right, the two round gems as big as the score badge. Glows while it is this player's turn, shows a
 * PASSED ribbon over the top of the leader after passing and a pulsing "Thinking..." note while the computer decides.
 * (USED / CANCELLED stamps are drawn by the leader card itself.)
 */
final class PlayerPanel extends Pane {

    static final double WIDTH = 360;
    static final double HEIGHT = 240;
    private static final PseudoClass TURN = PseudoClass.getPseudoClass("turn");
    private static final PseudoClass AHEAD = PseudoClass.getPseudoClass("ahead");
    /** Three-digit scores get a smaller font so they fit the badge. */
    private static final PseudoClass LONG = PseudoClass.getPseudoClass("long");
    /** Score badge and gem holders. */
    private static final double BADGE_SIZE = 72;
    private static final double GEM_SIZE = 68;
    private static final double GEM_GAP = 6;
    private static final double HAND_ICON_SIZE = 32;
    private static final double HAND_HEIGHT = 44;
    /** Left edge of the texts, right of the leader card. */
    private static final double INFO_X = 172;
    /** The gem column at the right edge, and the top of the gem / count / score area. */
    private static final double GEMS_X = WIDTH - 10 - BADGE_SIZE;
    private static final double GEMS_Y = 80;
    /** The count of cards in hand is level with the upper gem. */
    private static final double HAND_Y = GEMS_Y + (BADGE_SIZE - HAND_HEIGHT) / 2;
    /** The score badge sits at the bottom, level with the lower gem. */
    private static final double SCORE_Y = GEMS_Y + BADGE_SIZE + GEM_GAP;

    private final int side;
    private final BoardInput input;
    private final StackPane leaderHolder = new StackPane();
    private final Label factionLabel = Layouts.label("", "gm-panel-faction");
    private final Label handLabel = Layouts.label("0", "gm-panel-count");
    private final HBox handBox;
    private final StackPane[] gemHolders = {new StackPane(), new StackPane()};
    private final boolean[] gemLit = {true, true};
    private final StackPane scoreBadge = new StackPane();
    private final Label scoreLabel = Layouts.label("0", "gm-score-label");
    private final Label passedStamp = Layouts.label("PASSED", "gm-passed-stamp");
    private final Label thinkingLabel = Layouts.label("Thinking...", "gm-thinking-label");
    private LeaderView leaderView;
    private LeaderDef leader;
    private Faction faction;
    private int handCount = -1;
    private int score;
    private Animation thinkingPulse;

    PlayerPanel(int side, BoardInput input) {
        this.side = side;
        this.input = input;
        getStyleClass().addAll("gw-panel", "gm-player-panel", side == 0 ? "gm-panel-own" : "gm-panel-enemy");
        Layouts.fix(this, WIDTH, HEIGHT);
        leaderHolder.getStyleClass().add("gm-leader-holder");
        Layouts.place(leaderHolder, 14, 14, CardSize.MEDIUM.width(), CardSize.MEDIUM.height());
        passedStamp.setVisible(false);
        passedStamp.setMouseTransparent(true);
        thinkingLabel.setVisible(false);
        thinkingLabel.setMouseTransparent(true);
        StackPane.setAlignment(thinkingLabel, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(passedStamp, Pos.TOP_CENTER);
        leaderHolder.getChildren().addAll(passedStamp, thinkingLabel);
        Label nameLabel = Layouts.label(side == 0 ? "You" : "Opponent", "gm-panel-name");
        handLabel.setMinWidth(Region.USE_PREF_SIZE);
        handBox = new HBox(6, Icons.cards(HAND_ICON_SIZE), handLabel);
        handBox.getStyleClass().add("gm-panel-hand");
        handBox.setAlignment(Pos.CENTER_LEFT);
        VBox gems = new VBox(GEM_GAP);
        for (StackPane holder : gemHolders) {
            Layouts.fix(holder, BADGE_SIZE, BADGE_SIZE);
            holder.getChildren().add(Icons.gem(true, GEM_SIZE));
            gems.getChildren().add(holder);
        }
        scoreBadge.getStyleClass().add("gm-score-badge");
        scoreBadge.getChildren().add(scoreLabel);
        double textWidth = WIDTH - INFO_X - 6;
        Layouts.place(nameLabel, INFO_X, 8, textWidth, 36);
        Layouts.place(factionLabel, INFO_X, 46, textWidth, 30);
        Layouts.place(handBox, INFO_X, HAND_Y, GEMS_X - INFO_X - 6, HAND_HEIGHT);
        Layouts.place(scoreBadge, INFO_X, SCORE_Y, BADGE_SIZE, BADGE_SIZE);
        Layouts.place(gems, GEMS_X, GEMS_Y, BADGE_SIZE, 2 * BADGE_SIZE + GEM_GAP);
        getChildren().addAll(leaderHolder, nameLabel, factionLabel, handBox, scoreBadge, gems);
    }

    /**
     * Shows the player's state.
     *
     * @param turn    it is this player's turn (panel glows)
     * @param ahead   this player has the higher total score (gold badge)
     * @param animate pulse changed numbers and animate lost gems
     */
    void update(PlayerSnapshot player, boolean turn, boolean ahead, boolean animate) {
        if (leaderView == null || !player.leader().equals(leader)) {
            setLeader(player.leader());
        }
        leaderView.setUsed(player.leaderUsed());
        leaderView.setCancelled(player.leaderDisabled());
        if (faction != player.faction()) {
            faction = player.faction();
            factionLabel.setText(faction.shortName());
        }
        if (handCount != player.handCount()) {
            boolean pulse = animate && handCount >= 0;
            handCount = player.handCount();
            handLabel.setText(String.valueOf(handCount));
            if (pulse) {
                Animations.pulse(handBox, 1.25, 260).play();
            }
        }
        for (int i = 0; i < gemHolders.length; i++) {
            boolean lit = i < player.gems();
            if (lit != gemLit[i]) {
                setGem(i, lit, animate);
            }
        }
        if (score != player.score()) {
            score = player.score();
            scoreLabel.setText(String.valueOf(score));
            scoreLabel.pseudoClassStateChanged(LONG, score >= 100);
            if (animate) {
                Animations.pulse(scoreBadge, 1.18, 280).play();
            }
        }
        scoreBadge.pseudoClassStateChanged(AHEAD, ahead);
        passedStamp.setVisible(player.passed());
        pseudoClassStateChanged(TURN, turn);
    }

    private void setLeader(LeaderDef def) {
        if (leaderView != null) {
            leaderHolder.getChildren().remove(leaderView);
        }
        leader = def;
        leaderView = new LeaderView(def, CardSize.MEDIUM, true);
        leaderView.addEventHandler(MouseEvent.MOUSE_ENTERED, new LeaderHover());
        leaderView.addEventHandler(MouseEvent.MOUSE_CLICKED, new LeaderClick());
        leaderHolder.getChildren().add(0, leaderView);
    }

    /** Swaps a gem icon; a lost gem bursts and fades over the dark one. */
    private void setGem(int index, boolean lit, boolean animate) {
        gemLit[index] = lit;
        StackPane holder = gemHolders[index];
        Node old = holder.getChildren().isEmpty() ? null : holder.getChildren().get(holder.getChildren().size() - 1);
        Node fresh = Icons.gem(lit, GEM_SIZE);
        if (!animate || old == null || lit) {
            holder.getChildren().setAll(fresh);
            return;
        }
        holder.getChildren().setAll(fresh, old);
        ParallelTransition burst = new ParallelTransition(Animations.scale(old, 1, 2.0, 700), Animations.fade(old, 1, 0, 700));
        Animations.play(burst, new Animations.RemoveNode(old));
    }

    /** Highlights the leader card (its ability can be used now). */
    void setLeaderHighlighted(boolean highlighted) {
        if (leaderView != null) {
            leaderView.setHighlighted(highlighted);
        }
    }

    /** Shows or hides the pulsing "Thinking..." note. */
    void setThinking(boolean thinking) {
        if (thinking == thinkingLabel.isVisible()) {
            return;
        }
        thinkingLabel.setVisible(thinking);
        if (thinkingPulse != null) {
            thinkingPulse.stop();
            thinkingPulse = null;
        }
        if (thinking) {
            FadeTransition pulse = Animations.fade(thinkingLabel, 1, 0.35, 650);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            thinkingPulse = pulse;
            pulse.play();
        } else {
            thinkingLabel.setOpacity(1);
        }
    }

    /** The leader card node, or null before the first update. */
    Node leaderNode() {
        return leaderView;
    }

    /** The hand counter (target of the opponent's draws). */
    Node handNode() {
        return handBox;
    }

    /** One of the two gem holders. */
    Node gemNode(int index) {
        return gemHolders[index];
    }

    private final class LeaderHover implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            input.showLeader(side);
        }
    }

    private final class LeaderClick implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY) {
                event.consume();
                input.leaderClicked(side);
            }
        }
    }
}
