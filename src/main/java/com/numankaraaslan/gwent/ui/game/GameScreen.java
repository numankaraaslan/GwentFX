package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.ai.AiController;
import com.numankaraaslan.gwent.engine.GameEngine;
import com.numankaraaslan.gwent.engine.snapshot.GameSnapshot;
import com.numankaraaslan.gwent.model.Difficulty;
import com.numankaraaslan.gwent.model.GameSettings;
import com.numankaraaslan.gwent.ui.Disposable;
import com.numankaraaslan.gwent.ui.Navigator;
import com.numankaraaslan.gwent.ui.mode.GameMode;
import com.numankaraaslan.gwent.ui.mode.ResultNote;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * The game board screen. Creates the {@link GameEngine} with the human controller (board + prompts), the computer
 * opponent and the FX listener, and starts it; {@link #dispose()} stops it again. The {@link GameMode} supplies the cards
 * the decks are built from and decides what happens after the game.
 *
 * <p>Threading: the engine runs on its own thread. Events reach the screen through {@link FxGameListener}, which waits
 * on a latch until {@link EventAnimator} has animated them; decisions are asked by {@link HumanController}, which waits
 * on a {@link Reply} answered from the board or a prompt overlay. Disposing interrupts the engine thread and releases
 * both waits, so leaving the screen never leaves a blocked thread behind.</p>
 *
 * <p>Layers, bottom to top: the board, the effects layer (flying cards, glows, banners, toasts) and the overlay layer
 * (prompts, dialogs, menus).</p>
 */
public class GameScreen extends StackPane implements Disposable {

    private final Navigator navigator;
    private final GameSettings settings;
    private final GameMode mode;
    private final EffectsLayer effects = new EffectsLayer();
    private final OverlayLayer overlays = new OverlayLayer();
    private final BoardView board;
    private final TurnInput input;
    private final PromptOverlays prompts;
    private final EventAnimator animator;
    private final HumanController human;
    private final FxGameListener listener;
    private final KeyHandler keys = new KeyHandler();
    private GameEngine engine;
    private Scene keyScene;
    private Overlay pauseOverlay;
    private Overlay endOverlay;
    private boolean disposed;

    /**
     * Builds the board and starts a new game right away.
     *
     * @param navigator screen switching (menus, play again)
     * @param settings  the options chosen on the setup screen
     * @param mode      Free Play or Collector Mode
     */
    public GameScreen(Navigator navigator, GameSettings settings, GameMode mode) {
        this.navigator = navigator;
        this.settings = settings;
        this.mode = mode;
        getStyleClass().addAll("screen", "gm-game-screen");
        Layouts.fix(this, BoardView.WIDTH, BoardView.HEIGHT);
        Difficulty difficulty = settings.difficulty() == null ? Difficulty.NORMAL : settings.difficulty();
        input = new TurnInput(this, overlays, effects);
        board = new BoardView(input, "Difficulty: " + difficulty.displayName());
        input.bind(board);
        prompts = new PromptOverlays(overlays, input);
        animator = new EventAnimator(this, board, effects, prompts);
        Layouts.fix(effects, BoardView.WIDTH, BoardView.HEIGHT);
        Layouts.fix(overlays, BoardView.WIDTH, BoardView.HEIGHT);
        getChildren().addAll(board, effects, overlays);
        board.addEventFilter(MouseEvent.MOUSE_CLICKED, new RightClickCancel());
        sceneProperty().addListener(new SceneWatcher());
        board.info().clear();
        human = new HumanController(prompts);
        listener = new FxGameListener(animator);
        try {
            engine = new GameEngine(settings, mode.pool(), human, new AiController(difficulty), listener);
            engine.start();
        } catch (RuntimeException e) {
            engine = null;
            showError(e);
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (engine != null) {
            engine.stop();
        }
        human.abort();
        listener.abort();
        animator.dispose();
        prompts.dispose();
        input.end();
        if (keyScene != null) {
            keyScene.removeEventHandler(KeyEvent.KEY_PRESSED, keys);
            keyScene = null;
        }
    }

    // ------------------------------------------------------------------ menus & end screens

    /** Opens the pause menu (Resume / Main menu / Quit); new events wait while it is open. */
    void openPauseMenu() {
        if (disposed || (pauseOverlay != null && pauseOverlay.isOpen()) || (endOverlay != null && endOverlay.isOpen())) {
            return;
        }
        List<Button> buttons = new ArrayList<>();
        buttons.add(Layouts.button("Resume", true, new Runnable() {
            @Override
            public void run() {
                closePauseMenu();
            }
        }));
        buttons.add(Layouts.button("Main menu", false, new Runnable() {
            @Override
            public void run() {
                navigator.showMainMenu();
            }
        }));
        buttons.add(Layouts.button("Quit", false, new Runnable() {
            @Override
            public void run() {
                navigator.quit();
            }
        }));
        animator.setPaused(true);
        pauseOverlay = overlays.showFull(Dialogs.panel("Paused", "The battle waits for you. Leaving now abandons the game.", null, buttons));
    }

    private void closePauseMenu() {
        if (pauseOverlay != null) {
            pauseOverlay.close();
            pauseOverlay = null;
        }
        animator.setPaused(false);
    }

    /** Shows the result screen (called by the animator for GAME_ENDED), with what the mode adds to it, if anything. */
    void showGameOver(GameSnapshot snapshot) {
        if (disposed) {
            return;
        }
        closePauseMenu();
        input.end();
        ResultNote note = mode.gameEnded(settings, snapshot.winner());
        GameOverOverlay content = new GameOverOverlay(snapshot, note, new Runnable() {
            @Override
            public void run() {
                navigator.startGame(settings, mode);
            }
        }, new Runnable() {
            @Override
            public void run() {
                mode.openSetup(navigator);
            }
        }, new Runnable() {
            @Override
            public void run() {
                navigator.showMainMenu();
            }
        });
        endOverlay = overlays.showFull(content);
    }

    /** The engine crashed (or could not be created): explain and offer the way back. */
    void showError(Throwable error) {
        if (disposed) {
            return;
        }
        input.end();
        if (pauseOverlay != null) {
            pauseOverlay.close();
            pauseOverlay = null;
        }
        String message = error.getMessage() == null || error.getMessage().isEmpty() ? error.getClass().getSimpleName() : error.getMessage();
        List<Button> buttons = new ArrayList<>();
        buttons.add(Layouts.button("Main menu", true, new Runnable() {
            @Override
            public void run() {
                navigator.showMainMenu();
            }
        }));
        endOverlay = overlays.showFull(Dialogs.panel("Something went wrong", "The game cannot continue: " + message, null, buttons));
    }

    /** ESC handling, topmost first: pause menu, confirmation, discard pile, selection; otherwise open the pause menu. */
    private void escape() {
        if (disposed) {
            return;
        }
        if (pauseOverlay != null && pauseOverlay.isOpen()) {
            closePauseMenu();
            return;
        }
        if (input.closeConfirm() || input.closePileOverlay()) {
            return;
        }
        if (input.hasSelection()) {
            input.cancelSelection();
            return;
        }
        openPauseMenu();
    }

    // ------------------------------------------------------------------ input plumbing

    /** ESC: cancel the selection, close a confirmation or discard pile, or toggle the pause menu. */
    private final class KeyHandler implements EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent event) {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                escape();
            }
        }
    }

    /** A right-click anywhere on the board cancels the selected card. */
    private final class RightClickCancel implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() == MouseButton.SECONDARY) {
                input.cancelSelection();
            }
        }
    }

    /** Registers the key handler on whatever scene shows this screen. */
    private final class SceneWatcher implements ChangeListener<Scene> {
        @Override
        public void changed(ObservableValue<? extends Scene> observable, Scene oldScene, Scene newScene) {
            if (oldScene != null) {
                oldScene.removeEventHandler(KeyEvent.KEY_PRESSED, keys);
            }
            keyScene = null;
            if (newScene != null && !disposed) {
                newScene.addEventHandler(KeyEvent.KEY_PRESSED, keys);
                keyScene = newScene;
            }
        }
    }
}
