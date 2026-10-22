package com.numankaraaslan.gwent.ui;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;

/**
 * Every screen is laid out on a fixed 1920 x 1080 canvas. This pane scales that canvas uniformly to fit the window
 * and centres it (black bars fill the rest), so layouts never have to care about the window size. The canvas is
 * clipped to its bounds so glows near the edge never spill onto the bars.
 */
public final class ScaledPane extends Pane
{

    public static final double WIDTH = 1920;
    public static final double HEIGHT = 1080;

    private final StackPane canvas = new StackPane();
    private final Scale scale = new Scale(1, 1, 0, 0);

    public ScaledPane()
    {
        getStyleClass().add("letterbox");
        canvas.getStyleClass().add("canvas");
        canvas.setMinSize(WIDTH, HEIGHT);
        canvas.setPrefSize(WIDTH, HEIGHT);
        canvas.setMaxSize(WIDTH, HEIGHT);
        canvas.getTransforms().add(scale);
        canvas.setClip(new Rectangle(WIDTH, HEIGHT));
        getChildren().add(canvas);
    }

    /** Replaces the current screen. The screen is resized to 1920 x 1080. */
    public void setScreen(Node screen)
    {
        canvas.getChildren().setAll(screen);
    }

    @Override
    protected void layoutChildren()
    {
        double factor = Math.min(getWidth() / WIDTH, getHeight() / HEIGHT);
        if (factor <= 0)
        {
            factor = 1;
        }
        scale.setX(factor);
        scale.setY(factor);
        canvas.resize(WIDTH, HEIGHT);
        canvas.relocate((getWidth() - WIDTH * factor) / 2, (getHeight() - HEIGHT * factor) / 2);
    }
}
