package com.numankaraaslan.gwent.ui;

import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.GameSettings;
import com.numankaraaslan.gwent.ui.game.GameScreen;
import com.numankaraaslan.gwent.ui.menu.CollectionScreen;
import com.numankaraaslan.gwent.ui.menu.GalleryScreen;
import com.numankaraaslan.gwent.ui.menu.MainMenuScreen;
import com.numankaraaslan.gwent.ui.menu.NewGameScreen;
import com.numankaraaslan.gwent.ui.menu.RulesScreen;
import com.numankaraaslan.gwent.ui.mode.CollectorMode;
import com.numankaraaslan.gwent.ui.mode.FreePlayMode;
import com.numankaraaslan.gwent.ui.mode.GameMode;
import java.net.URL;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/** JavaFX entry point: one window, screens swapped inside a {@link ScaledPane}. */
public final class GwentApp extends Application implements Navigator
{

    private static final String[] STYLESHEETS = { "/com/numankaraaslan/gwent/css/app.css", "/com/numankaraaslan/gwent/css/cards.css", "/com/numankaraaslan/gwent/css/menu.css", "/com/numankaraaslan/gwent/css/game.css" };

    private static final String ICON = "/com/numankaraaslan/gwent/images/gwentfx-icon.jpg";

    private static final String RELEASES_URL = "https://github.com/numankaraaslan/GwentFX/releases";

    private Stage stage;
    private ScaledPane root;
    private Node current;

    @Override
    public void start(Stage primaryStage)
    {
        this.stage = primaryStage;
        root = new ScaledPane();
        Scene scene = new Scene(root, 1600, 900);
        for (String stylesheet : STYLESHEETS)
        {
            URL url = GwentApp.class.getResource(stylesheet);
            if (url != null)
            {
                scene.getStylesheets().add(url.toExternalForm());
            }
        }
        scene.addEventFilter(KeyEvent.KEY_PRESSED, new FullScreenToggle());
        stage.setScene(scene);
        stage.setTitle("Gwent");
        URL iconUrl = GwentApp.class.getResource(ICON);
        if (iconUrl != null)
        {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("Press F11 to leave full screen.");
        stage.setMinWidth(960);
        stage.setMinHeight(540);
        try
        {
            CardDatabase.get();
        }
        catch (RuntimeException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "The card data could not be loaded:\n" + e.getMessage());
            alert.setHeaderText("Gwent cannot start");
            alert.showAndWait();
            Platform.exit();
            return;
        }
        showMainMenu();
        stage.show();
    }

    @Override
    public void stop()
    {
        disposeCurrent();
    }

    @Override
    public void showMainMenu()
    {
        show(new MainMenuScreen(this));
    }

    @Override
    public void showFreePlay()
    {
        show(new NewGameScreen(this, new FreePlayMode()));
    }

    @Override
    public void showCollectorMode()
    {
        show(new NewGameScreen(this, new CollectorMode()));
    }

    @Override
    public void showCollection()
    {
        show(new CollectionScreen(this));
    }

    @Override
    public void startGame(GameSettings settings, GameMode mode)
    {
        show(new GameScreen(this, settings, mode));
    }

    @Override
    public void showGallery()
    {
        show(new GalleryScreen(this));
    }

    @Override
    public void showRules()
    {
        show(new RulesScreen(this));
    }

    @Override
    public void showReleases()
    {
        getHostServices().showDocument(RELEASES_URL);
    }

    @Override
    public void quit()
    {
        disposeCurrent();
        Platform.exit();
    }

    private void show(Node screen)
    {
        disposeCurrent();
        current = screen;
        root.setScreen(screen);
    }

    private void disposeCurrent()
    {
        if (current instanceof Disposable disposable)
        {
            disposable.dispose();
        }
        current = null;
    }

    /** F11 toggles full screen (Esc is left to the game: cancel / pause menu). */
    private final class FullScreenToggle implements EventHandler<KeyEvent>
    {
        @Override
        public void handle(KeyEvent event)
        {
            if (event.getCode() == KeyCode.F11)
            {
                stage.setFullScreen(!stage.isFullScreen());
                event.consume();
            }
        }
    }
}
