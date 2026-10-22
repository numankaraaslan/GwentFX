package com.numankaraaslan.gwent.engine;

/** Where the {@link Resolver} reports events. The real game forwards them to the UI; AI simulations ignore them. */
public interface EventSink {

    void emit(GameEvent event);

    /** A sink that drops every event (used for AI look-ahead). */
    EventSink NONE = new EventSink() {
        @Override
        public void emit(GameEvent event) {
            // ignored
        }
    };
}
