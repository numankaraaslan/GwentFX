package com.numankaraaslan.gwent.ui.card;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

/**
 * Drawing helpers for the vector art. Every icon is drawn in a {@link #BOX} x {@link #BOX} box (origin top-left) and
 * scaled afterwards, so outlines scale with the icon. A {@code null} outline colour draws the art without outlines
 * (used for the large, translucent motifs of the card portraits).
 */
final class IconKit {

    /** Side of the drawing box every icon is designed in. */
    static final double BOX = 100;
    /** Width of the dark outline in box units. */
    static final double OUTLINE = 4;

    private IconKit() {
    }

    /** Filled SVG path with an optional dark outline. */
    static SVGPath path(String data, Paint fill, Color line) {
        SVGPath path = new SVGPath();
        path.setContent(data);
        path.setFill(fill);
        outline(path, line, OUTLINE);
        return path;
    }

    /** Filled SVG path without outline. */
    static SVGPath flat(String data, Paint fill) {
        SVGPath path = new SVGPath();
        path.setContent(data);
        path.setFill(fill);
        path.setStroke(null);
        return path;
    }

    /** Filled polygon with an optional dark outline. */
    static Polygon polygon(double[] points, Paint fill, Color line) {
        Polygon polygon = new Polygon(points);
        polygon.setFill(fill);
        outline(polygon, line, OUTLINE);
        return polygon;
    }

    /** Filled circle with an optional dark outline. */
    static Circle circle(double cx, double cy, double r, Paint fill, Color line) {
        Circle circle = new Circle(cx, cy, r);
        circle.setFill(fill);
        outline(circle, line, OUTLINE);
        return circle;
    }

    /** Filled ellipse rotated by {@code angle} degrees (clockwise) around its centre. */
    static Ellipse ellipse(double cx, double cy, double rx, double ry, double angle, Paint fill, Color line) {
        Ellipse ellipse = new Ellipse(cx, cy, rx, ry);
        ellipse.setFill(fill);
        outline(ellipse, line, OUTLINE);
        if (angle != 0) {
            ellipse.getTransforms().add(new Rotate(angle, cx, cy));
        }
        return ellipse;
    }

    /** Straight line with round caps. */
    static Line line(double x1, double y1, double x2, double y2, Color color, double width) {
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(color);
        line.setStrokeWidth(width);
        line.setStrokeLineCap(StrokeLineCap.ROUND);
        return line;
    }

    /** Stroke-only SVG path with round caps and joins. */
    static SVGPath strokePath(String data, Color color, double width) {
        SVGPath path = new SVGPath();
        path.setContent(data);
        path.setFill(null);
        path.setStroke(color);
        path.setStrokeWidth(width);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return path;
    }

    /** Applies (or removes) the dark outline of a shape. */
    static void outline(Shape shape, Color line, double width) {
        if (line != null) {
            shape.setStroke(line);
            shape.setStrokeWidth(width);
            shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        } else {
            shape.setStroke(null);
        }
    }

    /** Dark detail colour: the outline colour, or a dark version of the fill when there is no outline. */
    static Color shade(Color fill, Color line) {
        if (line != null) {
            return line;
        }
        return fill.deriveColor(0, 1.0, 0.45, 1.0);
    }

    /** Darker version of a colour with the same opacity. */
    static Color darker(Color color, double brightness) {
        return color.deriveColor(0, 1.0, brightness, 1.0);
    }

    /** Lighter version of a colour (mixed with white) with the same opacity. */
    static Color tint(Color color, double amount) {
        return Palette.withOpacity(Palette.mix(color, Color.WHITE, amount), color.getOpacity());
    }

    /** Wraps art in a group placed at (x, y) and scaled by {@code scale} (art designed in the 100 box). */
    static Group place(Node art, double x, double y, double scale) {
        Group group = new Group(art);
        group.getTransforms().add(new Affine(scale, 0, x, 0, scale, y));
        return group;
    }

    /** Wraps art in a group rotated by {@code degrees} (clockwise) around the centre of the box. */
    static Group rotate(Node art, double degrees) {
        Group group = new Group(art);
        group.getTransforms().add(new Rotate(degrees, BOX / 2, BOX / 2));
        return group;
    }

    /** Wraps art in a group mirrored horizontally around the centre of the box. */
    static Group mirror(Node art) {
        Group group = new Group(art);
        group.getTransforms().add(new Scale(-1, 1, BOX / 2, BOX / 2));
        return group;
    }

    /** Points of a star polygon: {@code count} tips on radius {@code outer}, valleys on radius {@code inner}. */
    static double[] star(double cx, double cy, int count, double outer, double inner, double startDegrees) {
        double[] points = new double[count * 4];
        for (int i = 0; i < count * 2; i++) {
            double radius = i % 2 == 0 ? outer : inner;
            double angle = Math.toRadians(startDegrees + i * 180.0 / count);
            points[i * 2] = cx + radius * Math.cos(angle);
            points[i * 2 + 1] = cy + radius * Math.sin(angle);
        }
        return points;
    }

    /** Point on a circle (screen angles: 0 = right, 90 = down). */
    static double px(double cx, double radius, double degrees) {
        return cx + radius * Math.cos(Math.toRadians(degrees));
    }

    /** Point on a circle (screen angles: 0 = right, 90 = down). */
    static double py(double cy, double radius, double degrees) {
        return cy + radius * Math.sin(Math.toRadians(degrees));
    }

    /**
     * Line art drawn in two passes: a wide dark pass under a narrower coloured pass, so crossings of several strokes
     * stay clean (no outline cutting through another stroke).
     */
    static final class LineArt {

        private final Group under = new Group();
        private final Group over = new Group();
        private final Color color;
        private final Color line;
        private final double width;

        LineArt(Color color, Color line, double width) {
            this.color = color;
            this.line = line;
            this.width = width;
        }

        /** Adds a straight stroke. */
        void line(double x1, double y1, double x2, double y2) {
            if (line != null) {
                under.getChildren().add(IconKit.line(x1, y1, x2, y2, line, width + OUTLINE));
            }
            over.getChildren().add(IconKit.line(x1, y1, x2, y2, color, width));
        }

        /** Adds a stroked SVG path. */
        void path(String data) {
            if (line != null) {
                under.getChildren().add(strokePath(data, line, width + OUTLINE));
            }
            over.getChildren().add(strokePath(data, color, width));
        }

        /** Both passes, ready to add to an icon. */
        Group build() {
            return new Group(under, over);
        }
    }
}
