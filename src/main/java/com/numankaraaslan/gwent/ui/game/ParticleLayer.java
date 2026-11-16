package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.ui.card.Palette;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * Fireworks, confetti, gold coins and twinkles for {@link CollectionCompletePopup}. Every particle is a plain shape that
 * {@link #update} moves once a frame, so nothing here runs on its own: it all stops with the popup's timer. Coordinates
 * are the popup's, with y pointing down. Shapes are centred on (0, 0) and placed with their translation, so they turn and
 * stretch around their centre; they are unmanaged, so they never cause a layout pass. The layer never takes mouse input.
 */
final class ParticleLayer extends Pane {

    /** New particles are left out while this many are alive, so clicking fast can't slow the game down. */
    private static final int MAX_PARTICLES = 800;
    /** Particles this far outside the layer are dropped. */
    private static final double MARGIN = 60;
    /** Fireworks: sparks drift down slowly once the air has slowed them. */
    private static final double SPARK_GRAVITY = 70;
    private static final double SPARK_DRAG = 1.5;
    /** A rocket climbs against this gravity and bursts once it rises slower than the burst speed, at the top of its climb. */
    private static final double ROCKET_GRAVITY = 640;
    private static final double ROCKET_BURST_SPEED = 45;
    /** Confetti flutters: gravity against strong air drag, so it never falls faster than 80 pixels a second. */
    private static final double CONFETTI_GRAVITY = 240;
    private static final double CONFETTI_DRAG = 3;
    /** Coins are heavy: they fall fast and the air hardly slows them. */
    private static final double COIN_GRAVITY = 820;
    private static final double COIN_DRAG = 0.35;
    /** Rocket heads and their trails. */
    private static final Color TRAIL_COLOUR = Color.web("#ffd98a");
    private static final Color GLINT_COLOUR = Color.web("#fff6dc");
    private static final Paint COIN_FACE = new RadialGradient(0, 0, 0.38, 0.32, 0.75, true, CycleMethod.NO_CYCLE, new Stop(0, Color.web("#fff4c8")), new Stop(0.45, Palette.GOLD), new Stop(1, Palette.GOLD_DARK));
    private static final Color COIN_EDGE = Color.web("#6b5224");

    private final double width;
    private final double height;
    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    /** Particles made since the last update; they join the layer at the end of the next one. */
    private final List<Particle> born = new ArrayList<>();
    /** The glowing paint of every spark colour, shared by all sparks of that colour. */
    private final Map<Color, Paint> glows = new HashMap<>();

    ParticleLayer(double width, double height) {
        this.width = width;
        this.height = height;
        setMouseTransparent(true);
        Layouts.fix(this, width, height);
    }

    // ------------------------------------------------------------------ fireworks

    /**
     * A firework bursting at (x, y): a flash, then a ring or a ball of sparks in these colours, some of which glitter as
     * they fade. Power 1 is a full-size firework; a lower power makes fewer, slower and shorter-lived sparks.
     */
    void burst(double x, double y, Color[] colours, double power) {
        if (full()) {
            return;
        }
        add(flash(x, y, 40 + 30 * power, colours[colours.length - 1]));
        int count = Math.max(8, (int) Math.round(56 * power));
        double speed = 70 + 170 * power;
        double radius = 2 + Math.min(1.2, power);
        boolean ring = random.nextBoolean();
        for (int i = 0; i < count; i++) {
            double direction = 2 * Math.PI * (i + random.nextDouble() * 0.6) / count;
            double velocity = ring ? speed * (0.88 + 0.24 * random.nextDouble()) : speed * (0.2 + 0.95 * Math.sqrt(random.nextDouble()));
            Kind kind = random.nextDouble() < 0.3 ? Kind.GLITTER : Kind.SPARK;
            Color colour = colours[random.nextInt(colours.length)];
            Particle spark = new Particle(kind, new Circle(radius, glow(colour)), x, y, (0.6 + 0.5 * power) * (0.8 + 0.5 * random.nextDouble()));
            spark.vx = Math.cos(direction) * velocity;
            spark.vy = Math.sin(direction) * velocity;
            spark.gravity = SPARK_GRAVITY;
            spark.drag = SPARK_DRAG;
            spark.phase = random.nextDouble() * 2 * Math.PI;
            spark.phaseSpeed = 30 + random.nextDouble() * 25;
            add(spark);
        }
    }

    /** A firework that rises from below the bottom edge at {@code x} and bursts in these colours at about {@code burstY}. */
    void rocket(double x, double burstY, Color[] colours) {
        if (full()) {
            return;
        }
        double climb = Math.max(80, height + 10 - burstY);
        Particle rocket = new Particle(Kind.ROCKET, new Circle(2.6, glow(TRAIL_COLOUR)), x, height + 10, 6);
        rocket.gravity = ROCKET_GRAVITY;
        rocket.vy = -Math.sqrt(2 * ROCKET_GRAVITY * climb);
        rocket.vx = (random.nextDouble() - 0.5) * 36;
        rocket.colours = colours;
        add(rocket);
    }

    /** A ring of light that widens from (x, y) to {@code radius} and fades within {@code life} seconds. */
    void ring(double x, double y, double radius, Color colour, double life) {
        if (full()) {
            return;
        }
        Circle circle = new Circle(4);
        circle.setFill(null);
        circle.setStroke(colour);
        Particle ring = new Particle(Kind.RING, circle, x, y, life);
        ring.size = radius;
        add(ring);
    }

    // ------------------------------------------------------------------ confetti, coins and twinkles

    /**
     * A strip of confetti thrown from (x, y) at (vx, vy) pixels per second; the air soon slows it down and it flutters
     * down, tumbling and swaying.
     */
    void confetti(double x, double y, double vx, double vy, Color colour) {
        if (full()) {
            return;
        }
        double stripWidth = 6 + random.nextDouble() * 3;
        double stripHeight = 10 + random.nextDouble() * 5;
        Rectangle strip = new Rectangle(-stripWidth / 2, -stripHeight / 2, stripWidth, stripHeight);
        strip.setFill(colour);
        Particle confetti = new Particle(Kind.CONFETTI, strip, x, y, 14);
        confetti.vx = vx;
        confetti.vy = vy;
        confetti.gravity = CONFETTI_GRAVITY;
        confetti.drag = CONFETTI_DRAG;
        confetti.angle = random.nextDouble() * 360;
        confetti.spin = (random.nextDouble() - 0.5) * 400;
        confetti.phase = random.nextDouble() * 2 * Math.PI;
        confetti.phaseSpeed = 4 + random.nextDouble() * 6;
        confetti.size = 6 + random.nextDouble() * 14;
        add(confetti);
    }

    /** A gold coin thrown from (x, y) at (vx, vy) pixels per second; it spins as it falls. */
    void coin(double x, double y, double vx, double vy) {
        if (full()) {
            return;
        }
        Circle disc = new Circle(7 + random.nextDouble() * 3, COIN_FACE);
        disc.setStroke(COIN_EDGE);
        disc.setStrokeWidth(1.2);
        Particle coin = new Particle(Kind.COIN, disc, x, y, 6);
        coin.vx = vx;
        coin.vy = vy;
        coin.gravity = COIN_GRAVITY;
        coin.drag = COIN_DRAG;
        coin.phase = random.nextDouble() * 2 * Math.PI;
        coin.phaseSpeed = 7 + random.nextDouble() * 9;
        add(coin);
    }

    /** A four-pointed twinkle at (x, y) that grows to {@code size} pixels, turning, and shrinks away. */
    void glint(double x, double y, double size) {
        if (full()) {
            return;
        }
        Polygon star = new Polygon(starPoints(size / 2, size / 10));
        star.setFill(GLINT_COLOUR);
        star.setEffect(new DropShadow(BlurType.GAUSSIAN, Palette.GOLD_BRIGHT, 10, 0.35, 0, 0));
        Particle glint = new Particle(Kind.GLINT, star, x, y, 0.5 + random.nextDouble() * 0.4);
        glint.angle = random.nextDouble() * 90;
        glint.spin = 140;
        add(glint);
    }

    /** A speck of gold dust that appears at (x, y), drifts upwards, twinkles and fades. */
    void mote(double x, double y) {
        if (full()) {
            return;
        }
        Particle mote = new Particle(Kind.MOTE, new Circle(1 + random.nextDouble() * 1.4, glow(Palette.GOLD_BRIGHT)), x, y, 3 + random.nextDouble() * 3);
        mote.vx = (random.nextDouble() - 0.5) * 10;
        mote.vy = -(8 + random.nextDouble() * 16);
        mote.phase = random.nextDouble() * 2 * Math.PI;
        mote.phaseSpeed = 2 + random.nextDouble() * 4;
        add(mote);
    }

    // ------------------------------------------------------------------ frame update

    /**
     * Moves every particle {@code dt} seconds on, drops the ones whose time is up or that left the layer, and shows the
     * ones made since the last update.
     */
    void update(double dt) {
        Set<Node> gone = new HashSet<>();
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            particle.age += dt;
            move(particle, dt);
            if (particle.kind == Kind.ROCKET) {
                climb(particle);
            }
            if (particle.age >= particle.life || outside(particle)) {
                iterator.remove();
                gone.add(particle.shape);
            } else {
                apply(particle);
            }
        }
        if (!gone.isEmpty()) {
            getChildren().removeAll(gone);
        }
        if (!born.isEmpty()) {
            List<Node> shapes = new ArrayList<>();
            for (Particle particle : born) {
                shapes.add(particle.shape);
            }
            particles.addAll(born);
            born.clear();
            getChildren().addAll(shapes);
        }
    }

    /** A rocket leaves sparks behind as it climbs and bursts at the top. */
    private void climb(Particle rocket) {
        if (rocket.vy > -ROCKET_BURST_SPEED) {
            burst(rocket.x, rocket.y, rocket.colours, 1);
            rocket.age = rocket.life;
        } else if (!full()) {
            add(trail(rocket.x, rocket.y + 4));
        }
    }

    /** Gravity pulls, the air slows it down, and it moves, turns and flips on. */
    private static void move(Particle particle, double dt) {
        double damping = Math.exp(-particle.drag * dt);
        particle.vx = particle.vx * damping;
        particle.vy = particle.vy * damping + particle.gravity * dt;
        particle.x += particle.vx * dt;
        particle.y += particle.vy * dt;
        particle.angle += particle.spin * dt;
        particle.phase += particle.phaseSpeed * dt;
    }

    private boolean outside(Particle particle) {
        return particle.y > height + MARGIN || particle.x < -MARGIN || particle.x > width + MARGIN;
    }

    /** Puts the shape where its particle is and gives it the look of its kind at its age. */
    private static void apply(Particle particle) {
        Shape shape = particle.shape;
        double t = Math.min(1, particle.age / particle.life);
        shape.setTranslateX(particle.x);
        shape.setTranslateY(particle.y);
        switch (particle.kind) {
            case SPARK:
            case ROCKET:
                stretch(particle);
                shape.setOpacity(fade(t, 0.4));
                break;
            case GLITTER:
                stretch(particle);
                shape.setOpacity(t > 0.3 && Math.sin(particle.phase) < 0 ? 0.15 * fade(t, 0.4) : fade(t, 0.4));
                break;
            case TRAIL:
                shape.setOpacity(0.85 * (1 - t));
                break;
            case CONFETTI:
                shape.setTranslateX(particle.x + Math.sin(particle.phase * 0.45) * particle.size);
                shape.setRotate(particle.angle);
                shape.setScaleY(Math.cos(particle.phase));
                shape.setOpacity(fade(t, 0.85));
                break;
            case COIN:
                shape.setScaleX(Math.cos(particle.phase));
                break;
            case GLINT:
                shape.setScaleX(Math.sin(Math.PI * t));
                shape.setScaleY(Math.sin(Math.PI * t));
                shape.setRotate(particle.angle);
                break;
            case MOTE:
                shape.setOpacity(Math.sin(Math.PI * t) * (0.55 + 0.45 * Math.sin(particle.phase)));
                break;
            case RING:
                widen(particle, t);
                break;
            case FLASH:
                shape.setOpacity((1 - t) * (1 - t));
                shape.setScaleX(0.7 + 0.5 * t);
                shape.setScaleY(0.7 + 0.5 * t);
                break;
            default:
                break;
        }
    }

    /** Points a spark along its flight and stretches it the faster it flies. */
    private static void stretch(Particle particle) {
        double speed = Math.hypot(particle.vx, particle.vy);
        particle.shape.setRotate(Math.toDegrees(Math.atan2(particle.vy, particle.vx)));
        particle.shape.setScaleX(1 + Math.min(2.2, speed / 100));
    }

    /** A ring grows fast, then slowly, while its line thins and fades. */
    private static void widen(Particle ring, double t) {
        Circle circle = (Circle) ring.shape;
        double eased = 1 - Math.pow(1 - t, 3);
        circle.setRadius(4 + (ring.size - 4) * eased);
        circle.setStrokeWidth(0.5 + 5 * (1 - t));
        circle.setOpacity(1 - t);
    }

    /** 1 until {@code from} of the life has passed, then fading to 0 at its end. */
    private static double fade(double t, double from) {
        return t <= from ? 1 : Math.max(0, 1 - (t - from) / (1 - from));
    }

    // ------------------------------------------------------------------ helpers

    /** A short-lived spark a climbing rocket leaves behind. */
    private Particle trail(double x, double y) {
        Particle trail = new Particle(Kind.TRAIL, new Circle(1.8, glow(TRAIL_COLOUR)), x + (random.nextDouble() - 0.5) * 2, y, 0.3 + random.nextDouble() * 0.2);
        trail.vx = (random.nextDouble() - 0.5) * 24;
        trail.vy = 20 + random.nextDouble() * 30;
        trail.drag = 2;
        return trail;
    }

    /** The round flash of light a firework starts with. */
    private static Particle flash(double x, double y, double radius, Color colour) {
        Circle light = new Circle(radius, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.withOpacity(Color.WHITE, 0.8)), new Stop(0.3, Palette.withOpacity(colour, 0.45)), new Stop(1, Palette.withOpacity(colour, 0))));
        return new Particle(Kind.FLASH, light, x, y, 0.35);
    }

    /** A round glow in this colour: white-hot in the middle, fading out at the edge. Made once per colour. */
    private Paint glow(Color colour) {
        Paint paint = glows.get(colour);
        if (paint == null) {
            paint = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE, new Stop(0, Color.WHITE), new Stop(0.3, Palette.mix(colour, Color.WHITE, 0.4)), new Stop(0.65, colour), new Stop(1, Palette.withOpacity(colour, 0)));
            glows.put(colour, paint);
        }
        return paint;
    }

    /** The corners of a four-pointed star around (0, 0) with its points straight up, right, down and left. */
    private static double[] starPoints(double outer, double inner) {
        double[] points = new double[16];
        for (int i = 0; i < 8; i++) {
            double radius = i % 2 == 0 ? outer : inner;
            double angle = Math.toRadians(-90 + i * 45);
            points[i * 2] = Math.cos(angle) * radius;
            points[i * 2 + 1] = Math.sin(angle) * radius;
        }
        return points;
    }

    /** Shows the particle at its starting point and lets the next update add it to the layer. */
    private void add(Particle particle) {
        apply(particle);
        born.add(particle);
    }

    private boolean full() {
        return particles.size() + born.size() >= MAX_PARTICLES;
    }

    /** What a particle is; it decides how the particle moves and looks. */
    private enum Kind {
        /** A firework spark: stretched along its flight, fading at the end. */
        SPARK,
        /** A firework spark that flickers as it fades. */
        GLITTER,
        /** A short-lived spark a rocket leaves behind. */
        TRAIL,
        /** A climbing firework that bursts at the top. */
        ROCKET,
        /** A paper strip that tumbles and sways as it falls. */
        CONFETTI,
        /** A gold coin that spins as it falls. */
        COIN,
        /** A four-pointed twinkle that grows and shrinks. */
        GLINT,
        /** A speck of gold dust drifting upwards. */
        MOTE,
        /** A ring of light that widens and fades. */
        RING,
        /** A round flash of light that fades. */
        FLASH
    }

    /** One particle: its shape and where it is, how it moves and how old it is (in pixels and seconds). */
    private static final class Particle {

        private final Kind kind;
        private final Shape shape;
        private double x;
        private double y;
        private double vx;
        private double vy;
        private double gravity;
        private double drag;
        private double age;
        private final double life;
        /** Turn in degrees and its speed per second. */
        private double angle;
        private double spin;
        /** Flip, sway or flicker cycle in radians and its speed per second. */
        private double phase;
        private double phaseSpeed;
        /** How far confetti sways, or how wide a ring grows. */
        private double size;
        /** The colours a rocket bursts into. */
        private Color[] colours;

        Particle(Kind kind, Shape shape, double x, double y, double life) {
            this.kind = kind;
            this.shape = shape;
            this.x = x;
            this.y = y;
            this.life = life;
            shape.setManaged(false);
        }
    }
}
