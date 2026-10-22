package com.numankaraaslan.gwent.ui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * A row of toggle buttons of which exactly one is always selected: used as a segmented option control on the new
 * game screen and as the faction tabs of the gallery. The first and last buttons get the extra style classes
 * {@code <buttonStyle>-first} and {@code <buttonStyle>-last} (rounded ends).
 *
 * @param <T> type of the values
 */
final class OptionGroup<T> extends HBox {

    private final ToggleGroup group = new ToggleGroup();
    private final List<ToggleButton> buttons = new ArrayList<>();
    private final List<T> values = new ArrayList<>();
    private final String buttonStyle;
    private Runnable onChange;
    private T current;

    /**
     * @param groupStyle  style class of the row
     * @param buttonStyle style class of every button
     */
    OptionGroup(String groupStyle, String buttonStyle) {
        this.buttonStyle = buttonStyle;
        getStyleClass().add(groupStyle);
        group.selectedToggleProperty().addListener(new SelectionListener());
    }

    /** Adds a button for {@code value}; the first button added starts selected. {@code graphic} may be null. */
    ToggleButton add(T value, String text, Node graphic) {
        ToggleButton button = new ToggleButton(text);
        button.getStyleClass().add(buttonStyle);
        if (graphic != null) {
            button.setGraphic(graphic);
        }
        button.setMinWidth(Region.USE_PREF_SIZE);
        if (buttons.isEmpty()) {
            button.getStyleClass().add(buttonStyle + "-first");
        } else {
            buttons.get(buttons.size() - 1).getStyleClass().remove(buttonStyle + "-last");
        }
        button.getStyleClass().add(buttonStyle + "-last");
        buttons.add(button);
        values.add(value);
        getChildren().add(button);
        button.setToggleGroup(group);
        if (buttons.size() == 1) {
            current = value;
            button.setSelected(true);
        }
        return button;
    }

    /** The value of the selected button. */
    T getValue() {
        return current;
    }

    /** Selects the button of {@code value}. Returns false (and changes nothing) if no button has that value. */
    boolean select(T value) {
        int index = values.indexOf(value);
        if (index < 0) {
            return false;
        }
        buttons.get(index).setSelected(true);
        return true;
    }

    /** Called after the user (or {@link #select}) picked a different value. */
    void setOnChange(Runnable onChange) {
        this.onChange = onChange;
    }

    /** Keeps one button selected (clicking the selected one would otherwise unselect it) and reports changes. */
    private final class SelectionListener implements ChangeListener<Toggle> {
        @Override
        public void changed(ObservableValue<? extends Toggle> observable, Toggle oldToggle, Toggle newToggle) {
            if (newToggle == null) {
                if (oldToggle != null) {
                    oldToggle.setSelected(true);
                }
                return;
            }
            int index = buttons.indexOf(newToggle);
            if (index < 0) {
                return;
            }
            T value = values.get(index);
            if (Objects.equals(value, current)) {
                return;
            }
            current = value;
            if (onChange != null) {
                onChange.run();
            }
        }
    }
}
