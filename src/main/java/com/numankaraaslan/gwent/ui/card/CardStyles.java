package com.numankaraaslan.gwent.ui.card;

import javafx.css.PseudoClass;

/** CSS pseudo classes shared by card-like nodes (styled in cards.css). */
public final class CardStyles {

    public static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    public static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
    public static final PseudoClass DIMMED = PseudoClass.getPseudoClass("dimmed");
    public static final PseudoClass USED = PseudoClass.getPseudoClass("used");
    public static final PseudoClass CANCELLED = PseudoClass.getPseudoClass("cancelled");

    private CardStyles() {
    }
}
