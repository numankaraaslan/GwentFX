package com.numankaraaslan.gwent.ui;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Skin;
import javafx.scene.input.ScrollEvent;

/**
 * Scroll helpers shared by the menus and the game: sharp text, faster mouse-wheel scrolling and scrolling to a content
 * position.
 */
public final class ScrollSupport
{

    /** How many times further than the JavaFX default one mouse-wheel step scrolls. */
    public static final double SCROLL_SPEED = 2.5;

    private ScrollSupport()
    {
    }

    /**
     * Draws the pane's content directly instead of from a cached image. The JavaFX scroll pane caches what it shows, and
     * that image is then scaled with the 1920x1080 canvas, which blurs the text whenever the window has another size.
     */
    public static void keepTextSharp(ScrollPane pane)
    {
        pane.skinProperty().addListener(new NoViewportCache(pane));
        turnOffViewportCache(pane);
    }

    /** Turns off the cache of the skin's viewport, if the skin is there yet. */
    private static void turnOffViewportCache(ScrollPane pane)
    {
        Node viewport = pane.lookup(".viewport");
        if (viewport != null)
        {
            viewport.setCache(false);
        }
    }

    /** Makes the mouse wheel scroll {@link #SCROLL_SPEED} times further in this pane (vertical scrolling). */
    public static void installFastScroll(ScrollPane pane)
    {
        pane.addEventFilter(ScrollEvent.SCROLL, new FastScroll(pane));
    }

    /** Scrolls so that the content position {@code y} is at the top of the viewport (as far as possible). */
    public static void scrollTo(ScrollPane pane, double y)
    {
        double scrollable = scrollableHeight(pane);
        if (scrollable <= 0)
        {
            pane.setVvalue(pane.getVmin());
            return;
        }
        double fraction = Math.max(0, Math.min(1, y / scrollable));
        pane.setVvalue(pane.getVmin() + fraction * (pane.getVmax() - pane.getVmin()));
    }

    /** The content position currently shown at the top of the viewport. */
    public static double scrollTop(ScrollPane pane)
    {
        double scrollable = scrollableHeight(pane);
        double range = pane.getVmax() - pane.getVmin();
        if (scrollable <= 0 || range <= 0)
        {
            return 0;
        }
        return (pane.getVvalue() - pane.getVmin()) / range * scrollable;
    }

    /** How far the content can scroll: content height minus viewport height (0 or less if it all fits). */
    private static double scrollableHeight(ScrollPane pane)
    {
        Node content = pane.getContent();
        if (content == null)
        {
            return 0;
        }
        return content.getLayoutBounds().getHeight() - pane.getViewportBounds().getHeight();
    }

    /** Turns off the viewport cache of every skin the pane gets (the skin is created once the pane is shown). */
    private static final class NoViewportCache implements ChangeListener<Skin<?>>
    {

        private final ScrollPane pane;

        NoViewportCache(ScrollPane pane)
        {
            this.pane = pane;
        }

        @Override
        public void changed(ObservableValue<? extends Skin<?>> observable, Skin<?> oldValue, Skin<?> newValue)
        {
            turnOffViewportCache(pane);
        }
    }

    /** Mouse-wheel handler that scrolls {@link #SCROLL_SPEED} times further than the default. */
    private static final class FastScroll implements EventHandler<ScrollEvent>
    {

        private final ScrollPane pane;

        FastScroll(ScrollPane pane)
        {
            this.pane = pane;
        }

        @Override
        public void handle(ScrollEvent event)
        {
            if (event.getDeltaY() == 0 || scrollableHeight(pane) <= 0)
            {
                return;
            }
            scrollTo(pane, scrollTop(pane) - event.getDeltaY() * SCROLL_SPEED);
            event.consume();
        }
    }
}
