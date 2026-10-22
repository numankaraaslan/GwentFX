package com.numankaraaslan.gwent.ui.card;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Fonts of the card faces (sizes are derived from the card size, so they are set in code) and text measuring helpers.
 * Strength numbers use a serif with lining figures (Georgia's old-style digits look uneven in the badge).
 */
final class CardFonts {

    private static final String[] NUMBER_FAMILIES = {"Palatino Linotype", "Book Antiqua", "Cambria", "Times New Roman", "Georgia"};
    private static final String[] TEXT_FAMILIES = {"Georgia", "Palatino Linotype", "Cambria", "Times New Roman"};

    private static final Map<String, Double> FIT_CACHE = new ConcurrentHashMap<>();

    private static String numberFamily;
    private static String textFamily;

    private CardFonts() {
    }

    /** Bold font for strength numbers. */
    static Font number(double size) {
        return Font.font(numberFamily(), FontWeight.BOLD, size);
    }

    /** Bold serif font for card names. */
    static Font name(double size) {
        return Font.font(textFamily(), FontWeight.BOLD, size);
    }

    /** Italic serif font for leader titles. */
    static Font title(double size) {
        return Font.font(textFamily(), FontWeight.NORMAL, FontPosture.ITALIC, size);
    }

    /** Bold or italic serif font. */
    static Font text(boolean italic, double size) {
        return italic ? title(size) : name(size);
    }

    /** Bold serif font for stamps and the big decorative initials. */
    static Font display(double size) {
        return Font.font(numberFamily(), FontWeight.BOLD, size);
    }

    private static synchronized String numberFamily() {
        if (numberFamily == null) {
            numberFamily = firstInstalled(NUMBER_FAMILIES);
        }
        return numberFamily;
    }

    private static synchronized String textFamily() {
        if (textFamily == null) {
            textFamily = firstInstalled(TEXT_FAMILIES);
        }
        return textFamily;
    }

    private static String firstInstalled(String[] families) {
        List<String> installed = Font.getFamilies();
        for (String family : families) {
            if (installed.contains(family)) {
                return family;
            }
        }
        return Font.getDefault().getFamily();
    }

    /** Width of a single line of text. */
    static double width(String text, Font font) {
        Text probe = new Text(text);
        probe.setFont(font);
        return probe.getLayoutBounds().getWidth();
    }

    /** Height of one line of text. */
    static double lineHeight(Font font) {
        Text probe = new Text("Hg");
        probe.setFont(font);
        return probe.getLayoutBounds().getHeight();
    }

    /** Height of the text wrapped at {@code width}. */
    static double wrappedHeight(String text, Font font, double width) {
        Text probe = new Text(text);
        probe.setFont(font);
        probe.setWrappingWidth(width);
        return probe.getLayoutBounds().getHeight();
    }

    /**
     * Largest font size between {@code maxSize} and {@code minSize} at which the text wraps into at most
     * {@code maxLines} lines of {@code width} (no word wider than a line) and is at most {@code height} tall.
     */
    static double fitSize(String text, boolean italic, double width, double height, int maxLines, double maxSize, double minSize) {
        String key = text + '|' + italic + '|' + Math.round(width * 10) + '|' + Math.round(height * 10) + '|' + maxLines + '|' + Math.round(maxSize * 10) + '|' + Math.round(minSize * 10);
        Double cached = FIT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        double size = maxSize;
        while (size > minSize && !fits(text, text(italic, size), width, height, maxLines)) {
            size = size * 0.94;
        }
        double result = Math.max(size, minSize);
        FIT_CACHE.put(key, result);
        return result;
    }

    /** True if the text fits the box when wrapped (see {@link #fitSize}). */
    static boolean fits(String text, Font font, double width, double height, int maxLines) {
        for (String word : text.trim().split("\\s+")) {
            if (width(word, font) > width) {
                return false;
            }
        }
        double line = lineHeight(font);
        double total = wrappedHeight(text, font, width);
        int lines = (int) Math.round(total / line);
        return lines <= maxLines && total <= height + 0.5;
    }
}
