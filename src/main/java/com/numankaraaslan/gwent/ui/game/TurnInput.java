package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.Move;
import com.numankaraaslan.gwent.engine.snapshot.CardSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.GameSnapshot;
import com.numankaraaslan.gwent.engine.snapshot.PlayerSnapshot;
import com.numankaraaslan.gwent.model.CardDef;
import com.numankaraaslan.gwent.model.LeaderDef;
import com.numankaraaslan.gwent.model.RowType;
import com.numankaraaslan.gwent.model.SpecialType;
import com.numankaraaslan.gwent.ui.card.CardSize;
import com.numankaraaslan.gwent.ui.card.CardView;
import com.numankaraaslan.gwent.ui.card.LeaderView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.scene.control.Button;

/**
 * The human player's turn on the board. While the engine waits in {@code chooseMove}, hand cards with a legal move are
 * clickable (the others are dimmed). Clicking a card selects it and highlights where it can go (rows, special slots,
 * decoy targets, weather zone); clicking a highlighted target - or the selected card again when it has exactly one
 * legal move - answers the engine. Right-click or ESC cancels the selection. Leader and Pass ask for confirmation.
 * Also answers hover / click requests that are allowed at any time (info panel, discard piles, menu).
 */
final class TurnInput implements BoardInput {

    private final GameScreen screen;
    private final OverlayLayer overlays;
    private final EffectsLayer effects;
    private final List<CardView> highlightedCards = new ArrayList<>();
    private BoardView board;
    private Reply<Move> reply;
    private List<Move> legal = List.of();
    private int selectedId = -1;
    private List<Move> selectedMoves = List.of();
    private Overlay confirm;
    private Overlay pileOverlay;

    TurnInput(GameScreen screen, OverlayLayer overlays, EffectsLayer effects) {
        this.screen = screen;
        this.overlays = overlays;
        this.effects = effects;
    }

    /** Connects the board (created after this object because the board needs the input). */
    void bind(BoardView boardView) {
        this.board = boardView;
    }

    // ------------------------------------------------------------------ turn lifecycle

    /** The engine waits for the human's move: enable the input. */
    void begin(List<Move> legalMoves, Reply<Move> moveReply) {
        this.reply = moveReply;
        this.legal = legalMoves;
        clearSelection();
        Set<Integer> playable = new HashSet<>();
        for (Move move : legalMoves) {
            if (move.isPlay()) {
                playable.add(move.cardId());
            }
        }
        board.hand().setActive(true);
        board.hand().setPlayable(playable);
        board.panel(0).setLeaderHighlighted(hasLeaderMove());
        board.passButton().setDisable(false);
        board.setHint(turnHint());
    }

    /** Disables the input (after the move was made, or when the screen is left). */
    void end() {
        reply = null;
        legal = List.of();
        clearSelection();
        closeConfirm();
        if (board == null) {
            return;
        }
        board.hand().setActive(false);
        board.hand().setPlayable(null);
        board.panel(0).setLeaderHighlighted(false);
        board.passButton().setDisable(true);
        board.setHint("");
    }

    private boolean isMyTurn() {
        return reply != null && !reply.isDone();
    }

    private boolean hasLeaderMove() {
        for (Move move : legal) {
            if (move.isLeader()) {
                return true;
            }
        }
        return false;
    }

    private String turnHint() {
        boolean cards = false;
        for (Move move : legal) {
            if (move.isPlay()) {
                cards = true;
            }
        }
        if (!cards) {
            return hasLeaderMove() ? "Your turn - use your leader or pass" : "Your turn - nothing left to play, pass";
        }
        return hasLeaderMove() ? "Your turn - play a card, use your leader or pass" : "Your turn - play a card or pass";
    }

    private void submit(Move move) {
        if (!isMyTurn()) {
            return;
        }
        Reply<Move> current = reply;
        end();
        current.answer(move);
    }

    // ------------------------------------------------------------------ selection

    private List<Move> movesFor(int cardId) {
        List<Move> moves = new ArrayList<>();
        for (Move move : legal) {
            if (move.isPlay() && move.cardId() == cardId) {
                moves.add(move);
            }
        }
        return moves;
    }

    private void select(int cardId) {
        clearSelection();
        List<Move> moves = movesFor(cardId);
        CardSnapshot card = board.hand().card(cardId);
        if (moves.isEmpty() || card == null) {
            return;
        }
        selectedId = cardId;
        selectedMoves = moves;
        board.hand().setSelected(cardId);
        board.info().showCard(card);
        CardDef def = card.def();
        String name = def.name();
        if (def.isUnit()) {
            int side = def.isSpy() ? 1 : 0;
            for (Move move : moves) {
                if (move.row() != null) {
                    board.row(side, move.row()).setTarget(true);
                }
            }
            if (def.isSpy()) {
                board.setHint("Spy: click a highlighted row on the opponent's side to place " + name);
            } else if (moves.size() > 1) {
                board.setHint("Choose a row for " + name);
            } else {
                board.setHint("Click the highlighted row, or " + name + " again, to play it");
            }
        } else if (def.isDecoy()) {
            for (Move move : moves) {
                CardView target = move.row() == null ? null : board.row(0, move.row()).view(move.targetId());
                if (target != null) {
                    target.setHighlighted(true);
                    highlightedCards.add(target);
                }
            }
            board.setHint("Decoy: click one of your highlighted units to take it back to your hand");
        } else if (def.special() == SpecialType.COMMANDERS_HORN || def.special() == SpecialType.MARDROEME) {
            for (Move move : moves) {
                if (move.row() != null) {
                    board.row(0, move.row()).setTarget(true);
                    board.row(0, move.row()).setSlotTarget(true);
                }
            }
            board.setHint("Choose one of your rows for " + name);
        } else if (def.isWeather()) {
            board.weather().setTarget(true);
            board.setHint("Click the weather zone, or " + name + " again, to play it");
        } else {
            board.setHint("Click " + name + " again to play it");
        }
    }

    private void clearSelection() {
        selectedId = -1;
        selectedMoves = List.of();
        for (CardView view : highlightedCards) {
            view.setHighlighted(false);
        }
        highlightedCards.clear();
        if (board == null) {
            return;
        }
        board.hand().setSelected(-1);
        for (RowView row : board.rows()) {
            row.setTarget(false);
            row.setSlotTarget(false);
        }
        board.weather().setTarget(false);
        if (isMyTurn()) {
            board.setHint(turnHint());
        }
    }

    /** True while a hand card is selected. */
    boolean hasSelection() {
        return selectedId >= 0;
    }

    /** Right-click / ESC: drop the selection. */
    void cancelSelection() {
        if (selectedId >= 0) {
            clearSelection();
        }
    }

    /** Closes an open Pass / leader confirmation; returns true if there was one. */
    boolean closeConfirm() {
        if (confirm != null && confirm.isOpen()) {
            confirm.close();
            confirm = null;
            return true;
        }
        confirm = null;
        return false;
    }

    /** Closes an open discard pile overlay; returns true if there was one. */
    boolean closePileOverlay() {
        if (pileOverlay != null && pileOverlay.isOpen()) {
            pileOverlay.close();
            pileOverlay = null;
            return true;
        }
        pileOverlay = null;
        return false;
    }

    // ------------------------------------------------------------------ BoardInput

    @Override
    public void showCard(CardSnapshot card) {
        if (card != null) {
            board.info().showCard(card);
        }
    }

    @Override
    public void showLeader(int side) {
        GameSnapshot snapshot = board.snapshot();
        if (snapshot == null) {
            return;
        }
        PlayerSnapshot player = snapshot.player(side);
        board.info().showLeader(player.leader(), player.leaderUsed(), player.leaderDisabled());
    }

    @Override
    public void handCardClicked(int cardId) {
        CardSnapshot card = board.hand().card(cardId);
        if (card != null) {
            board.info().showCard(card);
        }
        if (!isMyTurn() || overlays.hasOpenOverlay()) {
            return;
        }
        if (cardId == selectedId) {
            if (selectedMoves.size() == 1) {
                submit(selectedMoves.get(0));
            }
            return;
        }
        if (movesFor(cardId).isEmpty()) {
            clearSelection();
            board.setHint(card == null ? turnHint() : card.def().name() + " cannot be played right now");
            return;
        }
        select(cardId);
    }

    @Override
    public boolean boardCardClicked(int side, RowType row, int cardId) {
        if (!isMyTurn() || selectedId < 0) {
            return false;
        }
        for (Move move : selectedMoves) {
            if (move.targetId() >= 0 && move.targetId() == cardId) {
                submit(move);
                return true;
            }
        }
        return false;
    }

    @Override
    public void rowClicked(int side, RowType row) {
        if (!isMyTurn() || selectedId < 0) {
            return;
        }
        CardSnapshot card = board.hand().card(selectedId);
        if (card == null || card.def().isDecoy()) {
            return;
        }
        int targetSide = card.def().isSpy() ? 1 : 0;
        if (side != targetSide) {
            return;
        }
        for (Move move : selectedMoves) {
            if (move.row() == row && move.targetId() < 0) {
                submit(move);
                return;
            }
        }
    }

    @Override
    public void slotClicked(int side, RowType row) {
        rowClicked(side, row);
    }

    @Override
    public void weatherClicked() {
        if (!isMyTurn() || selectedId < 0) {
            return;
        }
        CardSnapshot card = board.hand().card(selectedId);
        if (card != null && card.def().isWeather() && selectedMoves.size() == 1) {
            submit(selectedMoves.get(0));
        }
    }

    @Override
    public void leaderClicked(int side) {
        showLeader(side);
        GameSnapshot snapshot = board.snapshot();
        if (side != 0 || snapshot == null) {
            return;
        }
        LeaderDef leader = snapshot.human().leader();
        if (leader.isPassive()) {
            effects.toast(leader.name() + ": passive ability, always active.", "gm-toast-info");
            return;
        }
        if (!isMyTurn() || !hasLeaderMove() || overlays.hasOpenOverlay()) {
            return;
        }
        clearSelection();
        LeaderView view = new LeaderView(leader, CardSize.MEDIUM);
        List<Button> buttons = new ArrayList<>();
        buttons.add(Layouts.button("Use ability", true, new Runnable() {
            @Override
            public void run() {
                closeConfirm();
                submit(Move.leader());
            }
        }));
        buttons.add(Layouts.button("Cancel", false, new Runnable() {
            @Override
            public void run() {
                closeConfirm();
            }
        }));
        confirm = overlays.showOverBoard(Dialogs.panel(leader.name(), leader.description(), view, buttons));
        confirm.setOnVeilClicked(new Runnable() {
            @Override
            public void run() {
                closeConfirm();
            }
        });
    }

    @Override
    public void discardClicked(int side) {
        GameSnapshot snapshot = board.snapshot();
        if (snapshot == null) {
            return;
        }
        List<CardSnapshot> pile = new ArrayList<>(snapshot.player(side).discard());
        Collections.reverse(pile);
        String title = side == 0 ? "Your discard pile" : "Opponent's discard pile";
        String subtitle = pile.isEmpty() ? "The pile is empty." : pile.size() + (pile.size() == 1 ? " card, newest first." : " cards, newest first.");
        if (pileOverlay != null) {
            pileOverlay.close();
        }
        OverlayRef ref = new OverlayRef();
        CardGridOverlay grid = new CardGridOverlay(title, subtitle, pile, 0, 0, "Close", this, new CardGridOverlay.Callback() {
            @Override
            public void done(List<Integer> cardIds) {
                ref.close();
            }
        });
        ref.set(overlays.showOverBoard(grid));
        ref.get().setOnVeilClicked(new Runnable() {
            @Override
            public void run() {
                ref.close();
            }
        });
        pileOverlay = ref.get();
    }

    @Override
    public void passClicked() {
        if (!isMyTurn() || overlays.hasOpenOverlay()) {
            return;
        }
        clearSelection();
        List<Button> buttons = new ArrayList<>();
        buttons.add(Layouts.button("Pass", true, new Runnable() {
            @Override
            public void run() {
                closeConfirm();
                submit(Move.pass());
            }
        }));
        buttons.add(Layouts.button("Cancel", false, new Runnable() {
            @Override
            public void run() {
                closeConfirm();
            }
        }));
        confirm = overlays.showOverBoard(Dialogs.panel("Pass?", "Pass for the rest of this round? You will not be able to play any more cards until the next round.", null, buttons));
        confirm.setOnVeilClicked(new Runnable() {
            @Override
            public void run() {
                closeConfirm();
            }
        });
    }

    @Override
    public void menuClicked() {
        screen.openPauseMenu();
    }

    /** Mutable holder so a button can close the overlay it lives in. */
    static final class OverlayRef {

        private Overlay overlay;

        /** Closes the overlay if it is set and open. */
        void close() {
            if (overlay != null) {
                overlay.close();
            }
        }

        /** Remembers the overlay to close. */
        void set(Overlay value) {
            this.overlay = value;
        }

        /** The overlay, or null before {@link #set}. */
        Overlay get() {
            return overlay;
        }
    }
}
