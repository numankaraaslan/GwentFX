package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.EventType;
import com.numankaraaslan.gwent.engine.GameEvent;
import com.numankaraaslan.gwent.engine.RoundResult;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.GameSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.PlayerSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.RowSnapshot;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.model.SpecialType;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.ParallelTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.paint.Color;

/**
 * Turns engine events into what the player sees. For every event (on the JavaFX thread): an optional pre-animation on
 * the current nodes (e.g. scorched cards burn before they disappear), then the new snapshot is rendered (card nodes are
 * reused by id), then a post-animation shows what changed (ghost cards fly from the hand / deck / discard pile to their
 * new place, glows, row flashes, banners). The engine's {@code done} callback runs exactly when the event is over,
 * also when an animation fails or the screen was left, so the engine thread can never hang.
 *
 * <p>While the pause menu is open, a newly arriving event is held back and played on resume.</p>
 */
final class EventAnimator implements EventPlayer {

    private static final Color GOLD = Color.web("#f3d690");
    private static final Color GREEN = Color.web("#62d86c");
    private static final Color RED = Color.web("#ff5a3c");
    private static final Color ORANGE = Color.web("#ff9a2e");
    private static final Color ICE = Color.web("#a9d4ff");
    private static final Color SPY = Color.web("#c48bff");

    private final GameScreen screen;
    private final BoardView board;
    private final EffectsLayer effects;
    private final PromptOverlays prompts;
    private volatile boolean paused;
    private boolean disposed;
    private boolean rendered;
    private Ctx held;

    EventAnimator(GameScreen screen, BoardView board, EffectsLayer effects, PromptOverlays prompts) {
        this.screen = screen;
        this.board = board;
        this.effects = effects;
        this.prompts = prompts;
    }

    // ------------------------------------------------------------------ EventPlayer

    @Override
    public void playEvent(GameEvent event, GameSnapshot snapshot, Runnable done) {
        Ctx ctx = new Ctx(event, snapshot, new Once(done));
        if (disposed) {
            ctx.done.run();
            return;
        }
        if (paused) {
            if (held != null) {
                held.done.run();
            }
            held = ctx;
            return;
        }
        start(ctx);
    }

    @Override
    public void showError(Throwable error) {
        if (!disposed) {
            screen.showError(error);
        }
    }

    @Override
    public boolean isHolding() {
        return paused;
    }

    /** Holds back new events while the pause menu is open; resuming plays a held event. */
    void setPaused(boolean value) {
        paused = value;
        if (!value && held != null && !disposed) {
            Ctx next = held;
            held = null;
            start(next);
        }
    }

    /** The screen was left: finish everything at once and ignore later events. */
    void dispose() {
        disposed = true;
        paused = false;
        board.panel(1).setThinking(false);
        if (held != null) {
            held.done.run();
            held = null;
        }
    }

    private void start(Ctx ctx) {
        try {
            board.log().add(ctx.event.text(), ctx.event.player());
            if (ctx.event.type() != EventType.TURN_STARTED) {
                board.panel(1).setThinking(false);
            }
            dispatch(ctx);
        } catch (RuntimeException e) {
            ctx.fail(e);
        }
    }

    private void dispatch(Ctx ctx) {
        switch (ctx.event.type()) {
            case GAME_STARTED -> gameStarted(ctx);
            case LEADER_CANCELLED -> leaderCancelled(ctx);
            case HAND_DEALT -> handDealt(ctx);
            case CARD_REDRAWN -> cardRedrawn(ctx);
            case REDRAW_FINISHED -> redrawFinished(ctx);
            case FIRST_PLAYER_CHOSEN -> firstPlayerChosen(ctx);
            case ROUND_STARTED -> roundStarted(ctx);
            case TURN_STARTED -> turnStarted(ctx);
            case CARD_PLAYED -> cardPlayed(ctx, false);
            case CARD_REVIVED -> cardPlayed(ctx, true);
            case CARDS_SUMMONED -> cardsSummoned(ctx);
            case CARDS_DRAWN -> cardsDrawn(ctx);
            case CARDS_DESTROYED -> cardsDestroyed(ctx);
            case CARD_RETURNED -> cardReturned(ctx);
            case CARD_TRANSFORMED -> cardTransformed(ctx);
            case CARD_TAKEN -> cardTaken(ctx);
            case CARDS_DISCARDED -> cardsDiscarded(ctx);
            case WEATHER_PLAYED -> weatherPlayed(ctx);
            case WEATHER_CLEARED -> weatherCleared(ctx);
            case SPECIAL_PLAYED -> specialPlayed(ctx);
            case LEADER_USED -> leaderUsed(ctx);
            case LEADER_HORN -> leaderHorn(ctx);
            case AGILE_MOVED -> agileMoved(ctx);
            case DECKS_SHUFFLED -> decksShuffled(ctx);
            case CARDS_REVEALED -> message(ctx, 500);
            case PLAYER_PASSED -> playerPassed(ctx);
            case ROUND_ENDED -> roundEnded(ctx);
            case FACTION_ABILITY -> factionAbility(ctx);
            case BOARD_CLEARED -> boardCleared(ctx);
            case GAME_ENDED -> gameEnded(ctx);
            case MESSAGE -> message(ctx, 600);
            default -> {
                render(ctx.snapshot);
                ctx.finishAfter(150);
            }
        }
    }

    // ------------------------------------------------------------------ game flow

    private void gameStarted(Ctx ctx) {
        render(ctx.snapshot);
        PlayerSnapshot me = ctx.snapshot.human();
        PlayerSnapshot ai = ctx.snapshot.ai();
        FxBatch batch = effects.batch();
        effects.banner(batch, me.faction().displayName() + "  vs  " + ai.faction().displayName(), me.leader().name() + "   against   " + ai.leader().name(), "gm-banner-start", 1100);
        batch.play(ctx.done);
    }

    private void leaderCancelled(Ctx ctx) {
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        effects.halo(batch, board.panel(side(ctx)).leaderNode(), RED, 0, 1000);
        effects.toast(ctx.event.text(), "gm-toast-warning");
        batch.add(Animations.pause(900));
        batch.play(ctx.done);
    }

    private void handDealt(Ctx ctx) {
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        if (ctx.event.player() == 0) {
            flyIntoHand(batch, ctx.event.cardIds(), effects.boundsOf(board.pile(0).deckNode()), 70);
        } else if (ctx.event.player() == 1) {
            backsToHand(batch, 1, Math.min(ctx.event.cardIds().size(), 6));
        }
        batch.play(ctx.done);
    }

    private void cardRedrawn(Ctx ctx) {
        RedrawOverlay overlay = prompts.redrawOverlay();
        if (ctx.event.player() != 0 || overlay == null || ctx.event.cardIds().size() < 2) {
            render(ctx.snapshot);
            ctx.finishAfter(150);
            return;
        }
        int oldId = ctx.event.cardIds().get(0);
        int newId = ctx.event.cardIds().get(1);
        FxBatch out = effects.batch();
        Node oldNode = overlay.node(oldId);
        if (oldNode != null) {
            out.add(Animations.scaleX(oldNode, 1, 0, 180));
        }
        out.play(new Step(ctx) {
            @Override
            void body() {
                render(ctx.snapshot);
                overlay.setHand(ctx.snapshot.human().hand(), newId);
                FxBatch in = effects.batch();
                Node fresh = overlay.node(newId);
                if (fresh != null) {
                    fresh.setScaleX(0);
                    in.add(Animations.scaleX(fresh, 0, 1, 240));
                }
                in.add(Animations.pause(520));
                in.play(ctx.done);
            }
        });
    }

    private void redrawFinished(Ctx ctx) {
        render(ctx.snapshot);
        if (ctx.event.player() == 0 && prompts.redrawOverlay() != null) {
            prompts.closeRedraw();
            ctx.finishAfter(260);
            return;
        }
        ctx.finishAfter(1);
    }

    private void firstPlayerChosen(Ctx ctx) {
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        effects.banner(batch, ctx.event.player() == 0 ? "You go first" : "The opponent goes first", ctx.event.text(), "gm-banner-coin", 1000);
        batch.play(ctx.done);
    }

    private void roundStarted(Ctx ctx) {
        render(ctx.snapshot);
        board.setHint("");
        FxBatch batch = effects.batch();
        effects.banner(batch, "Round " + ctx.snapshot.round(), ctx.event.player() == 0 ? "You start" : "The opponent starts", "gm-banner-round", 1000);
        batch.play(ctx.done);
    }

    private void turnStarted(Ctx ctx) {
        render(ctx.snapshot);
        if (ctx.event.player() == 1) {
            board.panel(1).setThinking(true);
            board.setHint("Opponent's turn");
            ctx.finishAfter(40);
            return;
        }
        effects.toast("Your turn", "gm-toast-turn");
        ctx.finishAfter(60);
    }

    private void playerPassed(Ctx ctx) {
        render(ctx.snapshot);
        int side = side(ctx);
        if (side == 1) {
            board.setHint("");
        }
        FxBatch batch = effects.batch();
        String subtitle = ctx.event.text().contains("no moves") ? "No moves left" : "";
        effects.banner(batch, side == 0 ? "You passed" : "Opponent passed", subtitle, "gm-banner-pass", 750);
        effects.halo(batch, board.panel(side), GOLD, 0, 900);
        batch.play(ctx.done);
    }

    private void roundEnded(Ctx ctx) {
        GameSnapshot before = board.snapshot();
        render(ctx.snapshot);
        board.setHint("");
        int winner = ctx.event.player();
        List<RoundResult> results = ctx.snapshot.results();
        RoundResult last = results.isEmpty() ? null : results.get(results.size() - 1);
        String title = winner == 0 ? "You won the round!" : winner == 1 ? "The opponent won the round" : "Round drawn";
        String subtitle = last == null ? "" : "Round " + last.round() + "     " + last.score0() + "  :  " + last.score1();
        String variant = winner == 0 ? "gm-banner-win" : winner == 1 ? "gm-banner-loss" : "gm-banner-draw";
        FxBatch batch = effects.batch();
        effects.banner(batch, title, subtitle, variant, 1100);
        for (int side = 0; side < 2 && before != null; side++) {
            int gems = ctx.snapshot.player(side).gems();
            if (gems < before.player(side).gems() && gems >= 0 && gems < 2) {
                effects.halo(batch, board.panel(side).gemNode(gems), RED, 150, 1000);
            }
        }
        batch.play(ctx.done);
    }

    private void boardCleared(Ctx ctx) {
        Set<Integer> staying = new HashSet<>();
        for (PlayerSnapshot player : ctx.snapshot.players()) {
            for (RowSnapshot row : player.rows()) {
                for (CardSnapshot card : row.cards()) {
                    staying.add(card.id());
                }
            }
        }
        FxBatch pre = effects.batch();
        int index = 0;
        for (RowView row : board.rows()) {
            for (Integer id : row.ids()) {
                CardView view = row.view(id);
                if (view == null || staying.contains(id)) {
                    continue;
                }
                pre.add(new ParallelTransition(Animations.fade(view, 1, 0, 380), Animations.move(view, 0, 0, 70, 0, 380)), (index % 12) * 25);
                index++;
            }
            Node slot = row.slotContent();
            if (slot != null) {
                pre.add(Animations.fade(slot, 1, 0, 380));
            }
        }
        for (CardView view : board.weather().views()) {
            pre.add(Animations.fade(view, 1, 0, 380));
        }
        pre.play(new Step(ctx) {
            @Override
            void body() {
                render(ctx.snapshot);
                ctx.finishAfter(150);
            }
        });
    }

    private void gameEnded(Ctx ctx) {
        render(ctx.snapshot);
        board.setHint("");
        screen.showGameOver(ctx.snapshot);
        ctx.finishAfter(400);
    }

    private void message(Ctx ctx, double millis) {
        render(ctx.snapshot);
        effects.toast(ctx.event.text(), "gm-toast-info");
        ctx.finishAfter(ctx.event.text().isEmpty() ? 1 : millis);
    }

    // ------------------------------------------------------------------ cards entering the battlefield

    private void cardPlayed(Ctx ctx, boolean revived) {
        int side = side(ctx);
        int id = firstId(ctx);
        Bounds from = revived ? effects.boundsOf(board.pile(side).discardNode()) : side == 0 ? effects.boundsOf(board.hand().node(id)) : effects.boundsOf(board.panel(side).handNode());
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        RowView row = board.rowOf(id);
        if (row != null && row.view(id) != null) {
            CardView view = row.view(id);
            CardSnapshot card = row.card(id);
            effects.fly(batch, card, CardSize.BOARD, from, view, 0, revived ? 400 : 340);
            Color glow = revived ? GREEN : card.def().isSpy() ? SPY : GOLD;
            effects.halo(batch, view, glow, revived ? 340 : 280, revived ? 480 : 400);
        }
        strengthCues(batch, 320);
        batch.play(ctx.done);
    }

    private void cardsSummoned(Ctx ctx) {
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        int index = 0;
        for (Integer id : ctx.event.cardIds()) {
            RowView row = board.rowOf(id);
            if (row != null && row.view(id) != null) {
                effects.pop(batch, row.card(id), CardSize.BOARD, row.view(id), index * 90, 300);
                effects.halo(batch, row.view(id), GOLD, index * 90 + 240, 400);
                index++;
            }
        }
        strengthCues(batch, index * 90 + 200);
        batch.play(ctx.done);
    }

    private void specialPlayed(Ctx ctx) {
        int side = side(ctx);
        int id = firstId(ctx);
        Bounds from = side == 0 ? effects.boundsOf(board.hand().node(id)) : effects.boundsOf(board.panel(side).handNode());
        CardSnapshot inHand = side == 0 ? board.hand().card(id) : null;
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        if (ctx.event.row() == null) {
            CardSnapshot card = inHand != null ? inHand : ctx.snapshot.findCard(id);
            if (card != null) {
                effects.showcase(batch, card, 900);
            } else {
                batch.add(Animations.pause(300));
            }
            batch.play(ctx.done);
            return;
        }
        RowView row = board.row(side, ctx.event.row());
        CardSnapshot special = row.specialCard();
        Node content = row.slotContent();
        if (special != null && content != null) {
            effects.fly(batch, special, CardSize.TINY, from, content, 0, 360);
        }
        boolean mardroeme = special != null && special.def().special() == SpecialType.MARDROEME;
        effects.sweep(batch, row.cardsArea(), mardroeme ? GREEN : GOLD, 250, 550);
        strengthCues(batch, 380);
        batch.play(ctx.done);
    }

    private void weatherPlayed(Ctx ctx) {
        int side = side(ctx);
        int id = firstId(ctx);
        GameSnapshot before = board.snapshot();
        Bounds from = side == 0 ? effects.boundsOf(board.hand().node(id)) : null;
        if (side == 1 && before != null && ctx.snapshot.ai().handCount() < before.ai().handCount()) {
            from = effects.boundsOf(board.panel(1).handNode());
        }
        if (from == null) {
            from = effects.boundsOf(board.pile(side).deckNode());
        }
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        CardView view = board.weather().view(id);
        if (view != null) {
            effects.fly(batch, board.weather().card(id), CardSize.TINY, from, view, 0, 380);
            effects.halo(batch, view, ICE, 340, 600);
        } else {
            effects.toast(ctx.event.text(), "gm-toast-info");
        }
        strengthCues(batch, 380);
        batch.add(Animations.pause(620));
        batch.play(ctx.done);
    }

    private void weatherCleared(Ctx ctx) {
        int side = side(ctx);
        FxBatch pre = effects.batch();
        CardSnapshot clearCard = null;
        for (Integer id : ctx.event.cardIds()) {
            CardView view = board.weather().view(id);
            if (view != null) {
                pre.add(new ParallelTransition(Animations.fade(view, 1, 0, 340), Animations.scale(view, 1, 0.6, 340)));
            }
            CardSnapshot card = side == 0 ? board.hand().card(id) : null;
            if (card == null) {
                card = ctx.snapshot.findCard(id);
            }
            if (card != null && card.def().special() == SpecialType.CLEAR_WEATHER) {
                clearCard = card;
            }
        }
        if (clearCard != null) {
            effects.showcase(pre, clearCard, 800);
        }
        effects.halo(pre, board.weather(), GOLD, 0, 700);
        pre.play(new Step(ctx) {
            @Override
            void body() {
                render(ctx.snapshot);
                FxBatch post = effects.batch();
                strengthCues(post, 0);
                post.add(Animations.pause(420));
                post.play(ctx.done);
            }
        });
    }

    private void leaderHorn(Ctx ctx) {
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        if (ctx.event.row() != null) {
            RowView row = board.row(side(ctx), ctx.event.row());
            Node icon = row.slotContent();
            if (icon != null) {
                batch.add(Animations.scale(icon, 1.8, 1, 380));
            }
            effects.sweep(batch, row.cardsArea(), GOLD, 150, 550);
        }
        strengthCues(batch, 300);
        batch.play(ctx.done);
    }

    private void leaderUsed(Ctx ctx) {
        int side = side(ctx);
        LeaderDef leader = ctx.snapshot.player(side).leader();
        FxBatch pre = effects.batch();
        effects.halo(pre, board.panel(side).leaderNode(), GOLD, 0, 420);
        pre.play(new Step(ctx) {
            @Override
            void body() {
                render(ctx.snapshot);
                FxBatch post = effects.batch();
                effects.banner(post, leader.name(), leader.description(), side == 0 ? "gm-banner-leader-own" : "gm-banner-leader-enemy", 900);
                post.play(ctx.done);
            }
        });
    }

    // ------------------------------------------------------------------ cards leaving / changing

    private void cardsDestroyed(Ctx ctx) {
        FxBatch pre = effects.batch();
        List<RowView> burning = new ArrayList<>();
        for (Integer id : ctx.event.cardIds()) {
            RowView row = board.rowOf(id);
            CardView view = row == null ? null : row.view(id);
            if (view == null) {
                continue;
            }
            effects.halo(pre, view, ORANGE, 0, 800);
            pre.add(Animations.shake(view, 5, 4, 450));
            pre.add(new ParallelTransition(Animations.scale(view, 1, 0.2, 320), Animations.fade(view, 1, 0, 320)), 450);
            if (!burning.contains(row)) {
                burning.add(row);
            }
        }
        for (RowView row : burning) {
            effects.fire(pre, row.cardsArea(), 0, 800);
        }
        pre.play(new Step(ctx) {
            @Override
            void body() {
                render(ctx.snapshot);
                FxBatch post = effects.batch();
                strengthCues(post, 0);
                post.add(Animations.pause(150));
                post.play(ctx.done);
            }
        });
    }

    private void cardReturned(Ctx ctx) {
        int side = side(ctx);
        List<Integer> ids = ctx.event.cardIds();
        if (ids.size() < 2) {
            render(ctx.snapshot);
            ctx.finishAfter(200);
            return;
        }
        int decoyId = ids.get(0);
        int unitId = ids.get(1);
        RowView oldRow = board.rowOf(unitId);
        CardSnapshot unitBefore = oldRow == null ? null : oldRow.card(unitId);
        Bounds unitFrom = oldRow == null ? null : effects.boundsOf(oldRow.view(unitId));
        Bounds decoyFrom = side == 0 ? effects.boundsOf(board.hand().node(decoyId)) : effects.boundsOf(board.panel(side).handNode());
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        RowView row = board.rowOf(decoyId);
        if (row != null && row.view(decoyId) != null) {
            effects.fly(batch, row.card(decoyId), CardSize.BOARD, decoyFrom, row.view(decoyId), 0, 380);
        }
        if (side == 0 && board.hand().node(unitId) != null) {
            effects.fly(batch, board.hand().card(unitId), CardSize.HAND, unitFrom, board.hand().node(unitId), 120, 440);
        } else if (unitBefore != null) {
            effects.flyAway(batch, unitBefore, CardSize.BOARD, unitFrom, effects.boundsOf(board.panel(side).handNode()), 120, 440);
        }
        strengthCues(batch, 420);
        batch.play(ctx.done);
    }

    private void cardTransformed(Ctx ctx) {
        int id = firstId(ctx);
        RowView row = board.rowOf(id);
        CardView view = row == null ? null : row.view(id);
        if (view == null) {
            render(ctx.snapshot);
            ctx.finishAfter(200);
            return;
        }
        FxBatch pre = effects.batch();
        pre.add(Animations.scaleX(view, 1, 0, 180));
        pre.play(new Step(ctx) {
            @Override
            void body() {
                render(ctx.snapshot);
                RowView after = board.rowOf(id);
                CardView fresh = after == null ? null : after.view(id);
                if (fresh != view) {
                    view.setScaleX(1);
                }
                FxBatch post = effects.batch();
                if (fresh != null) {
                    fresh.setScaleX(1);
                    effects.halo(post, fresh, RED, 160, 650);
                    fresh.setScaleX(0);
                    post.add(Animations.scaleX(fresh, 0, 1, 220));
                }
                strengthCues(post, 300);
                post.play(ctx.done);
            }
        });
    }

    private void agileMoved(Ctx ctx) {
        Map<Integer, Bounds> from = new HashMap<>();
        for (Integer id : ctx.event.cardIds()) {
            RowView row = board.rowOf(id);
            if (row != null) {
                from.put(id, effects.boundsOf(row.view(id)));
            }
        }
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        int index = 0;
        for (Integer id : ctx.event.cardIds()) {
            RowView row = board.rowOf(id);
            if (row != null && row.view(id) != null) {
                effects.fly(batch, row.card(id), CardSize.BOARD, from.get(id), row.view(id), index * 90, 420);
                effects.halo(batch, row.view(id), GOLD, index * 90 + 380, 500);
                index++;
            }
        }
        effects.toast(ctx.event.text(), "gm-toast-info");
        strengthCues(batch, index * 90 + 400);
        batch.play(ctx.done);
    }

    // ------------------------------------------------------------------ hands, decks and piles

    private void cardsDrawn(Ctx ctx) {
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        if (ctx.event.player() == 0) {
            flyIntoHand(batch, ctx.event.cardIds(), effects.boundsOf(board.pile(0).deckNode()), 110);
        } else if (ctx.event.player() == 1) {
            backsToHand(batch, 1, Math.min(ctx.event.cardIds().size(), 6));
        }
        batch.play(ctx.done);
    }

    private void cardTaken(Ctx ctx) {
        int side = side(ctx);
        int id = firstId(ctx);
        GameSnapshot before = board.snapshot();
        CardSnapshot card = null;
        Bounds from = null;
        for (int owner = 0; owner < 2 && before != null && card == null; owner++) {
            card = findIn(before.player(owner).discard(), id);
            if (card != null) {
                from = effects.boundsOf(board.pile(owner).discardNode());
            }
        }
        if (from == null) {
            from = effects.boundsOf(board.pile(side).deckNode());
        }
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        Node handNode = side == 0 ? board.hand().node(id) : null;
        if (handNode != null) {
            effects.fly(batch, board.hand().card(id), CardSize.HAND, from, handNode, 0, 440);
            effects.halo(batch, handNode, GOLD, 400, 600);
        } else if (card != null) {
            effects.flyAway(batch, card, CardSize.BOARD, from, effects.boundsOf(board.panel(side).handNode()), 0, 480);
        } else {
            backsToHand(batch, side, 1);
        }
        batch.play(ctx.done);
    }

    private void cardsDiscarded(Ctx ctx) {
        int side = side(ctx);
        List<CardSnapshot> cards = new ArrayList<>();
        List<Bounds> sources = new ArrayList<>();
        if (side == 0) {
            for (Integer id : ctx.event.cardIds()) {
                CardSnapshot card = board.hand().card(id);
                if (card != null) {
                    cards.add(card);
                    sources.add(effects.boundsOf(board.hand().node(id)));
                }
            }
        }
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        Bounds to = effects.boundsOf(board.pile(side).discardNode());
        for (int i = 0; i < cards.size(); i++) {
            effects.flyAway(batch, cards.get(i), CardSize.HAND, sources.get(i), to, i * 100, 420);
        }
        if (cards.isEmpty()) {
            effects.halo(batch, board.pile(side).discardNode(), RED, 0, 600);
        }
        batch.play(ctx.done);
    }

    private void decksShuffled(Ctx ctx) {
        GameSnapshot before = board.snapshot();
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        for (int side = 0; side < 2 && before != null; side++) {
            List<CardSnapshot> pile = before.player(side).discard();
            Bounds from = effects.boundsOf(board.pile(side).discardNode());
            Bounds to = effects.boundsOf(board.pile(side).deckNode());
            int count = Math.min(pile.size(), 5);
            for (int i = 0; i < count; i++) {
                effects.flyAway(batch, pile.get(pile.size() - 1 - i), CardSize.BOARD, from, to, i * 80, 420);
            }
        }
        effects.toast(ctx.event.text(), "gm-toast-info");
        batch.add(Animations.pause(500));
        batch.play(ctx.done);
    }

    private void factionAbility(Ctx ctx) {
        int side = side(ctx);
        Set<Integer> wasOnBoard = new HashSet<>();
        for (Integer id : ctx.event.cardIds()) {
            if (board.rowOf(id) != null) {
                wasOnBoard.add(id);
            }
        }
        Bounds discard = effects.boundsOf(board.pile(side).discardNode());
        Bounds deck = effects.boundsOf(board.pile(side).deckNode());
        render(ctx.snapshot);
        FxBatch batch = effects.batch();
        int index = 0;
        for (Integer id : ctx.event.cardIds()) {
            Node handNode = side == 0 ? board.hand().node(id) : null;
            if (handNode != null) {
                effects.fly(batch, board.hand().card(id), CardSize.HAND, deck, handNode, index * 90, 420);
                index++;
                continue;
            }
            RowView row = board.rowOf(id);
            if (row == null || row.view(id) == null) {
                continue;
            }
            if (wasOnBoard.contains(id)) {
                effects.halo(batch, row.view(id), GOLD, index * 90, 900);
            } else {
                effects.fly(batch, row.card(id), CardSize.BOARD, discard, row.view(id), index * 120, 440);
                effects.halo(batch, row.view(id), GREEN, index * 120 + 400, 600);
            }
            index++;
        }
        if (index == 0) {
            if (side == 1 && !ctx.event.cardIds().isEmpty()) {
                backsToHand(batch, 1, Math.min(ctx.event.cardIds().size(), 6));
            }
            effects.halo(batch, board.panel(side), GOLD, 0, 900);
        }
        effects.toast(ctx.event.text(), "gm-toast-faction");
        strengthCues(batch, 400);
        batch.add(Animations.pause(600));
        batch.play(ctx.done);
    }

    // ------------------------------------------------------------------ helpers

    private void render(GameSnapshot snapshot) {
        board.render(snapshot, rendered);
        rendered = true;
        screen.applyCss();
        screen.layout();
    }

    /** Green / red glows on every card whose strength went up / down in the last rendering. */
    private void strengthCues(FxBatch batch, double delay) {
        for (RowView row : board.rows()) {
            for (CardView view : row.raisedViews()) {
                effects.halo(batch, view, GREEN, delay, 420);
            }
            for (CardView view : row.loweredViews()) {
                effects.halo(batch, view, RED, delay, 420);
            }
        }
    }

    /** New hand cards fly in from {@code from}, one after the other in hand order. */
    private void flyIntoHand(FxBatch batch, List<Integer> ids, Bounds from, double stagger) {
        int index = 0;
        for (Integer id : board.hand().ids()) {
            if (ids.contains(id)) {
                effects.fly(batch, board.hand().card(id), CardSize.HAND, from, board.hand().node(id), index * stagger, 360);
                index++;
            }
        }
    }

    /** Card backs travel from a player's deck to the hand counter on the panel. */
    private void backsToHand(FxBatch batch, int side, int count) {
        GameSnapshot snapshot = board.snapshot();
        if (snapshot == null) {
            return;
        }
        Faction faction = snapshot.player(side).faction();
        Bounds from = effects.boundsOf(board.pile(side).deckNode());
        Bounds to = effects.boundsOf(board.panel(side).handNode());
        for (int i = 0; i < count; i++) {
            effects.flyBack(batch, faction, from, to, i * 80, 380);
        }
    }

    private static CardSnapshot findIn(List<CardSnapshot> cards, int id) {
        for (CardSnapshot card : cards) {
            if (card.id() == id) {
                return card;
            }
        }
        return null;
    }

    private static int side(Ctx ctx) {
        int player = ctx.event.player();
        return player == 1 ? 1 : 0;
    }

    private static int firstId(Ctx ctx) {
        return ctx.event.cardIds().isEmpty() ? -1 : ctx.event.cardIds().get(0);
    }

    /** One event being played: its data and its completion callback. */
    private final class Ctx {

        private final GameEvent event;
        private final GameSnapshot snapshot;
        private final Runnable done;

        Ctx(GameEvent event, GameSnapshot snapshot, Runnable done) {
            this.event = event;
            this.snapshot = snapshot;
            this.done = done;
        }

        /** Finishes the event after a short pause. */
        void finishAfter(double millis) {
            FxBatch batch = effects.batch();
            batch.add(Animations.pause(millis));
            batch.play(done);
        }

        /** Something went wrong: show the new state without animation and release the engine. */
        void fail(RuntimeException error) {
            error.printStackTrace();
            try {
                render(snapshot);
            } catch (RuntimeException again) {
                again.printStackTrace();
            }
            done.run();
        }
    }

    /** A continuation step of an event's animation; failures release the engine instead of hanging it. */
    private abstract class Step implements Runnable {

        private final Ctx ctx;

        Step(Ctx ctx) {
            this.ctx = ctx;
        }

        @Override
        public final void run() {
            if (disposed) {
                ctx.done.run();
                return;
            }
            try {
                body();
            } catch (RuntimeException e) {
                ctx.fail(e);
            }
        }

        abstract void body();
    }

    /** Runs the wrapped callback at most once. */
    private static final class Once implements Runnable {

        private Runnable target;

        Once(Runnable target) {
            this.target = target;
        }

        @Override
        public void run() {
            Runnable current = target;
            target = null;
            if (current != null) {
                current.run();
            }
        }
    }
}
