package com.numankaraaslan.gwent.ui.card;

/** Standard card sizes on the 1920 x 1080 canvas (aspect ratio ~0.71, like the in-game cards). */
public enum CardSize {
    /** Weather zone, discard pile top, special slots. */
    TINY(60, 85),
    /** Cards in the battlefield rows. */
    BOARD(82, 116),
    /** Cards in the player's hand. */
    HAND(104, 147),
    /** Choice dialogs, gallery grid, leader cards on the board. */
    MEDIUM(150, 212),
    /** Redraw screen, leader selection. */
    LARGE(210, 297),
    /** The big card in the info panel. */
    PREVIEW(300, 424);

    private final double width;
    private final double height;

    CardSize(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /** Width on the 1920 x 1080 canvas. */
    public double width() {
        return width;
    }

    /** Height on the 1920 x 1080 canvas. */
    public double height() {
        return height;
    }
}
