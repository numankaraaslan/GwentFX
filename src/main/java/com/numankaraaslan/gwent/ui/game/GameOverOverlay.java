package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.engine.GameState;
import com.numankaraaslan.gwent.engine.RoundResult;
import com.numankaraaslan.gwent.engine.snapshot.GameSnapshot;
import com.numankaraaslan.gwent.ui.card.Icons;
import java.util.List;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** The end of the game: Victory / Defeat / Draw, the score of every round and the ways to go on. */
final class GameOverOverlay extends VBox {

    /** Width of each score table column ("Opponent" in the large table font must fit). */
    private static final double COLUMN_WIDTH = 175;

    GameOverOverlay(GameSnapshot snapshot, Runnable playAgain, Runnable newGame, Runnable mainMenu) {
        getStyleClass().addAll("gw-panel", "gm-game-dialog", "gm-game-over-dialog");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(22);
        int winner = snapshot.winner();
        String title = winner == GameState.HUMAN ? "Victory" : winner == GameState.AI ? "Defeat" : "Draw";
        String variant = winner == GameState.HUMAN ? "gm-result-victory" : winner == GameState.AI ? "gm-result-defeat" : "gm-result-draw";
        String text = winner == GameState.HUMAN ? "You won the game." : winner == GameState.AI ? "The opponent won the game." : "Nobody won the game.";
        HBox heading = new HBox(16);
        heading.setAlignment(Pos.CENTER);
        if (winner == GameState.HUMAN) {
            heading.getChildren().add(Icons.crown(54));
        }
        heading.getChildren().add(Layouts.label(title, "gm-result-title", variant));
        if (winner == GameState.HUMAN) {
            heading.getChildren().add(Icons.crown(54));
        }
        HBox buttons = new HBox(16, Layouts.button("Play again", true, playAgain), Layouts.button("New game", false, newGame), Layouts.button("Main menu", false, mainMenu));
        buttons.setAlignment(Pos.CENTER);
        getChildren().addAll(heading, Layouts.label(text, "gw-subheading", "gm-result-text"), table(snapshot.results()), buttons);
    }

    private static GridPane table(List<RoundResult> results) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("gm-result-table");
        grid.setAlignment(Pos.CENTER);
        String[] headers = {"Round", "You", "Opponent", "Winner"};
        for (int column = 0; column < headers.length; column++) {
            ColumnConstraints constraints = new ColumnConstraints(COLUMN_WIDTH);
            constraints.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(constraints);
            grid.add(cell(headers[column], "gm-result-header"), column, 0);
        }
        int row = 1;
        for (RoundResult result : results) {
            String winner = result.winner() == GameState.HUMAN ? "You" : result.winner() == GameState.AI ? "Opponent" : "Draw";
            grid.add(cell(String.valueOf(result.round()), "gm-result-cell"), 0, row);
            grid.add(cell(String.valueOf(result.score0()), result.score0() > result.score1() ? "gm-result-cell-win" : "gm-result-cell"), 1, row);
            grid.add(cell(String.valueOf(result.score1()), result.score1() > result.score0() ? "gm-result-cell-win" : "gm-result-cell"), 2, row);
            grid.add(cell(winner, result.winner() == GameState.HUMAN ? "gm-result-cell-win" : "gm-result-cell"), 3, row);
            row++;
        }
        return grid;
    }

    private static Label cell(String text, String styleClass) {
        Label label = Layouts.label(text, styleClass);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        return label;
    }
}
