package com.numankaraaslan.gwent.ui.menu;

/**
 * One entry of a combo box: a value (may be null, e.g. for "Random") and the text shown for it.
 *
 * @param <T> type of the value
 */
final class Choice<T> {

    private final T value;
    private final String label;

    Choice(T value, String label) {
        this.value = value;
        this.label = label;
    }

    /** The chosen value, or null for "Random". */
    T value() {
        return value;
    }

    /** Shown by the combo box. */
    @Override
    public String toString() {
        return label;
    }
}
