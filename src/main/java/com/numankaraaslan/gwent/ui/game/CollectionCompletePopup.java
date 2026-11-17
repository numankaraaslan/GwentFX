package com.numankaraaslan.gwent.ui.game;

import com.numankaraaslan.gwent.collector.CardCollection;
import com.numankaraaslan.gwent.collector.Reward;
import com.numankaraaslan.gwent.collector.Tier;
import com.numankaraaslan.gwent.model.CardDatabase;
import com.numankaraaslan.gwent.model.Faction;
import com.numankaraaslan.gwent.ui.ScaledPane;
import com.numankaraaslan.gwent.ui.card.Icons;
import com.numankaraaslan.gwent.ui.card.Palette;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

/**
 * The popup that celebrates the last card of Collector Mode, whatever order the difficulties were won in. Everything
 * happens inside its window, which is half as wide and half as tall as the game: golden rays turn behind a crown in a
 * laurel wreath, which slams down and throws gold coins and confetti; the title, the six faction emblems (each with a
 * burst in its colours) and the three complete tiers pop in one after another, and fireworks in the faction colours keep
 * rising on both sides until the popup is closed. A click anywhere in it sets off another firework.
 *
 * <p>One {@link AnimationTimer} drives it all while the popup is in a scene: it plays the steps of the opening in order,
 * keeps the rays, glow and shine moving and updates the two {@link ParticleLayer}s (fireworks behind the text; confetti,
 * coins and twinkles in front of it). Removing the popup from its scene stops the timer, so a closed popup leaves nothing
 * running.</p>
 */
public final class CollectionCompletePopup extends StackPane {

    /**
     * Half the width and height of the 1920 x 1080 canvas. The canvas is scaled to fit the window, so the popup always
     * covers half the width and height of the game, whatever the screen size or DPI.
     */
    private static final double WIDTH = ScaledPane.WIDTH / 2;
    private static final double HEIGHT = ScaledPane.HEIGHT / 2;

    private static final String TITLE = "Collection Complete";
    /** The order the faction emblems pop in, neutral last. */
    private static final Faction[] FACTIONS = {Faction.NORTHERN_REALMS, Faction.NILFGAARD, Faction.SCOIATAEL, Faction.MONSTERS, Faction.SKELLIGE, Faction.NEUTRAL};
    /** The tiers whose cards are won, in difficulty order. */
    private static final Tier[] REWARD_TIERS = {Tier.EASY, Tier.NORMAL, Tier.HARD};
    /** Colours of the burst when the seal lands and of the bursts behind the tiers. */
    private static final Color[] GOLD = {Palette.GOLD_BRIGHT, Palette.GOLD, Color.web("#fff6dc")};
    private static final Color[] CONFETTI = confettiColours();
    /** The band of light that sweeps over the title. */
    private static final List<Stop> SHINE = List.of(new Stop(0, Palette.withOpacity(Color.WHITE, 0)), new Stop(0.5, Palette.withOpacity(Color.web("#fffaf0"), 0.85)), new Stop(1, Palette.withOpacity(Color.WHITE, 0)));

    // ------------------------------------------------------------------ layout, in pixels of the 960 x 540 window

    private static final double CORNER_RADIUS = 16;
    /** The gold stars on the corners of the inner frame line. */
    private static final double ORNAMENT_SIZE = 18;
    private static final double ORNAMENT_INSET = 4;
    /** The crown in its wreath, near the top edge; the rays, the glow and the flashes come from its centre. */
    private static final double SEAL_SIZE = 156;
    private static final double SEAL_X = WIDTH / 2;
    private static final double SEAL_Y = 98;
    private static final double SEAL_DISC_RADIUS = 45;
    private static final double CROWN_SIZE = 62;
    private static final double GLOW_RADIUS = 135;
    /** Long enough to reach the far corners from the seal. */
    private static final double RAY_LENGTH = 700;
    private static final double FLASH_RADIUS = 560;
    private static final double TITLE_Y = 174;
    private static final double TITLE_HEIGHT = 62;
    private static final double SUBTITLE_Y = 238;
    private static final double SUBTITLE_HEIGHT = 32;
    /** Centre line of the row of faction emblems. */
    private static final double MEDALLION_Y = 314;
    private static final double MEDALLION_SIZE = 60;
    private static final double MEDALLION_GAP = 24;
    private static final double CHIPS_Y = 366;
    private static final double CHIPS_HEIGHT = 40;
    private static final double CHIP_ICON_SIZE = 24;
    private static final double BUTTON_Y = 426;
    private static final double BUTTON_HEIGHT = 56;
    private static final double HINT_Y = 486;
    private static final double HINT_HEIGHT = 24;
    /** Rockets rise this far in from either side edge (plus up to the spread), beside the title and the seal. */
    private static final double ROCKET_SIDE = 60;
    private static final double ROCKET_SPREAD = 220;
    /** Rockets burst between these heights. */
    private static final double ROCKET_TOP = 60;
    private static final double ROCKET_BOTTOM = 230;

    // ------------------------------------------------------------------ timing, in seconds after the popup appears

    private static final double SEAL_DROP = 0.2;
    private static final double SEAL_FALL = 0.32;
    private static final double SEAL_LANDS = SEAL_DROP + SEAL_FALL;
    private static final double TITLE_IN = 0.7;
    private static final double SUBTITLE_IN = 1.0;
    private static final double MEDALLIONS_IN = 1.25;
    private static final double MEDALLION_STEP = 0.16;
    private static final double CHIPS_IN = 2.35;
    private static final double CHIP_STEP = 0.16;
    private static final double BUTTON_IN = 2.9;
    private static final double FIRST_ROCKET = 1.0;
    /** A band of light sweeps over the title every period, taking the sweep time to cross it. */
    private static final double SHINE_START = 1.3;
    private static final double SHINE_PERIOD = 3.2;
    private static final double SHINE_SWEEP = 0.8;
    /** Until then the fireworks come faster; after that the celebration calms down a little. */
    private static final double FINALE = 8;
    /** Longest step of a frame, so a stalled frame never makes the particles jump. */
    private static final double MAX_STEP = 0.05;

    private static final int LANDING_COINS = 30;
    private static final int CANNON_CONFETTI = 40;
    /** Confetti per second from the top edge: a downpour for a few seconds after the landing, then a steady fall. */
    private static final double CONFETTI_DOWNPOUR = 20;
    private static final double CONFETTI_DOWNPOUR_TIME = 4;
    private static final double CONFETTI_PER_SECOND = 10;
    private static final double MOTES_PER_SECOND = 6;
    private static final double GLINTS_PER_SECOND = 5;
    /** How often a second rocket rises with the first, on the other side. */
    private static final double ROCKET_PAIRS = 0.4;

    private final Random random = new Random();
    /** Everything that moves, clipped to the window so nothing spills out of it. */
    private final Pane stage = new Pane();
    private final ParticleLayer sky = new ParticleLayer(WIDTH, HEIGHT);
    private final ParticleLayer front = new ParticleLayer(WIDTH, HEIGHT);
    private final Group rays = new Group();
    private final Rotate raySpin = new Rotate();
    private final Rotate rayCounterSpin = new Rotate();
    private final Circle glow;
    private final StackPane seal;
    private final StackPane title;
    private final Text shine;
    private final Label subtitle;
    private final List<StackPane> medallions = new ArrayList<>();
    private final List<HBox> chips = new ArrayList<>();
    private final Button button;
    private final Label hint;
    private final Rectangle flash;
    private final Ticker ticker = new Ticker();
    /** Seconds since the popup appeared. */
    private double elapsed;
    private int medallionsShown;
    private double nextRocket = FIRST_ROCKET;
    private int rocketFaction;
    /** Parts of a particle the spawn rates owe, carried over from frame to frame. */
    private double confettiDue;
    private double motesDue;
    private double glintsDue;

    /**
     * Builds the popup; the show starts once it is in a scene.
     *
     * @param onClose what the button does: close the popup
     */
    public CollectionCompletePopup(Runnable onClose) {
        getStyleClass().add("gm-complete-popup");
        Layouts.fix(this, WIDTH, HEIGHT);
        CardCollection all = CardCollection.of(CardDatabase.get(), Collections.emptySet());
        Region backdrop = new Region();
        backdrop.getStyleClass().add("gm-complete-backdrop");
        backdrop.setCache(true);
        rays.getChildren().addAll(rayFan(16, 3.2, 0.3, raySpin), rayFan(9, 7.5, 0.13, rayCounterSpin));
        rays.setManaged(false);
        rays.setMouseTransparent(true);
        rays.setLayoutX(SEAL_X);
        rays.setLayoutY(SEAL_Y);
        rays.setOpacity(0);
        glow = new Circle(GLOW_RADIUS, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.withOpacity(Palette.GOLD_BRIGHT, 0.55)), new Stop(0.45, Palette.withOpacity(Palette.GOLD, 0.2)), new Stop(1, Palette.withOpacity(Palette.GOLD, 0))));
        glow.setManaged(false);
        glow.setMouseTransparent(true);
        glow.setLayoutX(SEAL_X);
        glow.setLayoutY(SEAL_Y);
        glow.setOpacity(0);
        seal = seal();
        seal.setOpacity(0);
        Text base = new Text(TITLE);
        base.getStyleClass().addAll("gm-complete-title", "gm-complete-title-base");
        shine = new Text(TITLE);
        shine.getStyleClass().add("gm-complete-title");
        shine.setVisible(false);
        title = Layouts.place(new StackPane(base, shine), 0, TITLE_Y, WIDTH, TITLE_HEIGHT);
        title.setOpacity(0);
        subtitle = Layouts.label("All " + cardCount(all) + " cards are yours. Gwent has a new master!", "gm-complete-subtitle");
        subtitle.setOpacity(0);
        Pane content = new Pane(seal, title, Layouts.place(new StackPane(subtitle), 0, SUBTITLE_Y, WIDTH, SUBTITLE_HEIGHT));
        for (int i = 0; i < FACTIONS.length; i++) {
            StackPane medallion = medallion(FACTIONS[i]);
            Layouts.place(medallion, medallionX(i) - MEDALLION_SIZE / 2, MEDALLION_Y - MEDALLION_SIZE / 2, MEDALLION_SIZE, MEDALLION_SIZE);
            medallion.setOpacity(0);
            medallions.add(medallion);
            content.getChildren().add(medallion);
        }
        HBox chipRow = new HBox();
        chipRow.getStyleClass().add("gm-complete-chips");
        for (Tier tier : REWARD_TIERS) {
            int total = all.rewardCount(tier);
            HBox chip = new HBox(tierIcon(tier), Layouts.label(tier.displayName() + "  " + total + "/" + total, "gm-complete-chip-label"));
            chip.getStyleClass().add("gm-complete-chip");
            chip.setOpacity(0);
            chips.add(chip);
            chipRow.getChildren().add(chip);
        }
        button = Layouts.button("Glorious!", true, onClose);
        button.getStyleClass().add("gm-complete-button");
        button.setVisible(false);
        hint = Layouts.label("Click anywhere for another firework", "gm-complete-hint");
        hint.setOpacity(0);
        StackPane hintRow = Layouts.place(new StackPane(hint), 0, HINT_Y, WIDTH, HINT_HEIGHT);
        hintRow.setMouseTransparent(true);
        content.getChildren().addAll(Layouts.place(chipRow, 0, CHIPS_Y, WIDTH, CHIPS_HEIGHT), Layouts.place(new StackPane(button), 0, BUTTON_Y, WIDTH, BUTTON_HEIGHT), hintRow);
        Layouts.fix(content, WIDTH, HEIGHT);
        flash = new Rectangle(WIDTH, HEIGHT, new RadialGradient(0, 0, SEAL_X, SEAL_Y, FLASH_RADIUS, false, CycleMethod.NO_CYCLE, new Stop(0, Palette.withOpacity(Color.WHITE, 0.9)), new Stop(0.3, Palette.withOpacity(Palette.GOLD_BRIGHT, 0.45)), new Stop(1, Palette.withOpacity(Palette.GOLD, 0))));
        flash.setMouseTransparent(true);
        flash.setOpacity(0);
        stage.getChildren().addAll(rays, glow, sky, content, front, flash);
        Layouts.fix(stage, WIDTH, HEIGHT);
        Rectangle clip = new Rectangle(WIDTH, HEIGHT);
        clip.setArcWidth(CORNER_RADIUS * 2);
        clip.setArcHeight(CORNER_RADIUS * 2);
        stage.setClip(clip);
        Region frame = new Region();
        frame.getStyleClass().add("gm-complete-frame");
        frame.setMouseTransparent(true);
        getChildren().addAll(backdrop, stage, frame, ornaments());
        addEventHandler(MouseEvent.MOUSE_PRESSED, new ClickFirework());
        sceneProperty().addListener(new SceneWatch());
    }

    /**
     * Makes the button the cancel button, so Esc presses it once it shows. For screens whose own cancel button is switched
     * off while the popup is open; the game screen handles Esc itself.
     */
    public void closeWithEscape() {
        button.setCancelButton(true);
    }

    // ------------------------------------------------------------------ building

    /** {@code count} thin wedges of light around (0, 0) that fade out towards their tips, turned by {@code spin}. */
    private static Group rayFan(int count, double halfAngle, double strength, Rotate spin) {
        RadialGradient fill = new RadialGradient(0, 0, 0, 0, RAY_LENGTH, false, CycleMethod.NO_CYCLE, new Stop(0, Palette.withOpacity(Palette.GOLD_BRIGHT, strength)), new Stop(0.5, Palette.withOpacity(Palette.GOLD, strength * 0.3)), new Stop(1, Palette.withOpacity(Palette.GOLD, 0)));
        double half = Math.toRadians(halfAngle);
        Group fan = new Group();
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            Polygon ray = new Polygon(0, 0, RAY_LENGTH * Math.cos(angle - half), RAY_LENGTH * Math.sin(angle - half), RAY_LENGTH * Math.cos(angle + half), RAY_LENGTH * Math.sin(angle + half));
            ray.setFill(fill);
            fan.getChildren().add(ray);
        }
        fan.getTransforms().add(spin);
        return fan;
    }

    /** The crown in a laurel wreath, on a dark disc with a gold rim. */
    private static StackPane seal() {
        Circle disc = new Circle(SEAL_DISC_RADIUS, new RadialGradient(0, 0, 0.5, 0.4, 0.65, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.mix(Palette.LEATHER, Palette.GOLD, 0.3)), new Stop(1, Palette.LEATHER_DARK)));
        disc.setStroke(Palette.GOLD);
        disc.setStrokeWidth(3);
        StackPane seal = new StackPane(disc, Icons.laurel(SEAL_SIZE), Icons.crown(CROWN_SIZE));
        seal.getStyleClass().add("gm-complete-seal");
        return Layouts.place(seal, SEAL_X - SEAL_SIZE / 2, SEAL_Y - SEAL_SIZE / 2, SEAL_SIZE, SEAL_SIZE);
    }

    /** A faction's emblem on a round disc in the faction's colours with a gold rim. */
    private static StackPane medallion(Faction faction) {
        Circle disc = new Circle(MEDALLION_SIZE / 2 - 2, new RadialGradient(0, 0, 0.5, 0.35, 0.7, true, CycleMethod.NO_CYCLE, new Stop(0, Palette.factionLight(faction)), new Stop(0.55, Palette.faction(faction)), new Stop(1, Palette.factionDark(faction))));
        disc.setStroke(Palette.GOLD);
        disc.setStrokeWidth(2.5);
        StackPane medallion = new StackPane(disc, Icons.faction(faction, MEDALLION_SIZE * 0.62));
        medallion.getStyleClass().add("gm-complete-medallion");
        return medallion;
    }

    /** The tier's mark on My Collection: a laurel for Easy and Normal, the hero star for Hard. */
    private static Node tierIcon(Tier tier) {
        return tier == Tier.HARD ? Icons.hero(CHIP_ICON_SIZE) : Icons.laurel(CHIP_ICON_SIZE);
    }

    /** Gold stars on the corners of the inner frame line. */
    private static Pane ornaments() {
        Pane ornaments = new Pane();
        ornaments.setMouseTransparent(true);
        double[] xs = {ORNAMENT_INSET, WIDTH - ORNAMENT_INSET - ORNAMENT_SIZE};
        double[] ys = {ORNAMENT_INSET, HEIGHT - ORNAMENT_INSET - ORNAMENT_SIZE};
        for (double x : xs) {
            for (double y : ys) {
                Node star = Icons.hero(ORNAMENT_SIZE);
                star.relocate(x, y);
                ornaments.getChildren().add(star);
            }
        }
        return ornaments;
    }

    /** How many different cards there are to own, starter cards included and tokens left out. */
    private static int cardCount(CardCollection collection) {
        int count = 0;
        for (Reward reward : collection.rewards()) {
            count += reward.cards().size();
        }
        return count;
    }

    /** Centre of an emblem in the row, which is centred in the window. */
    private static double medallionX(int index) {
        double row = FACTIONS.length * MEDALLION_SIZE + (FACTIONS.length - 1) * MEDALLION_GAP;
        return (WIDTH - row) / 2 + MEDALLION_SIZE / 2 + index * (MEDALLION_SIZE + MEDALLION_GAP);
    }

    /** Every faction's light colour and its accent, with gold instead of Nilfgaard's grey. */
    private static Color[] confettiColours() {
        List<Color> colours = new ArrayList<>();
        for (Faction faction : FACTIONS) {
            colours.add(faction == Faction.NILFGAARD ? Palette.GOLD : Palette.factionLight(faction));
            colours.add(Palette.factionAccent(faction));
        }
        return colours.toArray(new Color[0]);
    }

    /** A faction's firework colours: its light colour (gold for Nilfgaard, whose light colour is grey), its accent and a near-white tint. */
    private static Color[] fireworkColours(Faction faction) {
        Color main = faction == Faction.NILFGAARD ? Palette.GOLD_BRIGHT : Palette.mix(Palette.factionLight(faction), Color.WHITE, 0.15);
        return new Color[] {main, Palette.factionAccent(faction), Palette.mix(main, Color.WHITE, 0.6)};
    }

    private Color confettiColour() {
        return CONFETTI[random.nextInt(CONFETTI.length)];
    }

    // ------------------------------------------------------------------ every frame

    private void tick(double dt) {
        double before = elapsed;
        elapsed += dt;
        play(before);
        spawn(dt);
        animate();
        sky.update(dt);
        front.update(dt);
    }

    /** Runs the steps of the opening whose time has come since the last frame. */
    private void play(double before) {
        if (reached(before, SEAL_DROP)) {
            dropSeal();
        }
        if (reached(before, SEAL_LANDS)) {
            sealLands();
        }
        if (reached(before, TITLE_IN)) {
            popIn(title, 0.5, 1.6, 420);
        }
        if (reached(before, SUBTITLE_IN)) {
            Animations.fade(subtitle, 0, 1, 450).play();
        }
        for (int i = 0; i < medallions.size(); i++) {
            if (reached(before, MEDALLIONS_IN + i * MEDALLION_STEP)) {
                popMedallion(i);
            }
        }
        for (int i = 0; i < chips.size(); i++) {
            if (reached(before, CHIPS_IN + i * CHIP_STEP)) {
                showChip(chips.get(i));
            }
        }
        if (reached(before, BUTTON_IN)) {
            showButton();
        }
    }

    /** Whether {@code time} was passed in this frame. */
    private boolean reached(double before, double time) {
        return before < time && elapsed >= time;
    }

    /** Keeps the party going: confetti from the top, gold dust, twinkles and the next fireworks. */
    private void spawn(double dt) {
        if (elapsed >= SEAL_LANDS) {
            confettiDue += (elapsed < SEAL_LANDS + CONFETTI_DOWNPOUR_TIME ? CONFETTI_DOWNPOUR : CONFETTI_PER_SECOND) * dt;
        }
        while (confettiDue >= 1) {
            confettiDue -= 1;
            front.confetti(random.nextDouble() * WIDTH, -12, (random.nextDouble() - 0.5) * 40, 40 + random.nextDouble() * 50, confettiColour());
        }
        motesDue += MOTES_PER_SECOND * dt;
        while (motesDue >= 1) {
            motesDue -= 1;
            sky.mote(random.nextDouble() * WIDTH, 60 + random.nextDouble() * (HEIGHT - 60));
        }
        if (elapsed >= TITLE_IN) {
            glintsDue += GLINTS_PER_SECOND * dt;
        }
        while (glintsDue >= 1) {
            glintsDue -= 1;
            glint();
        }
        if (elapsed >= nextRocket) {
            launchRockets();
            nextRocket = elapsed + (elapsed < FINALE ? 0.55 + random.nextDouble() * 0.6 : 0.9 + random.nextDouble());
        }
    }

    /** Moves what never stops: the rays, the glow, the floating seal and emblems, the flashes and the shine on the title. */
    private void animate() {
        rays.setOpacity(Math.min(1, elapsed / 0.9));
        raySpin.setAngle(elapsed * 7);
        rayCounterSpin.setAngle(-elapsed * 4);
        double pulse = 1 + 0.07 * Math.sin(elapsed * 2.8);
        glow.setScaleX(pulse);
        glow.setScaleY(pulse);
        glow.setOpacity(Math.min(1, elapsed / 0.6));
        seal.setTranslateY(3.5 * Math.sin(elapsed * 2.2) * ramp(elapsed - SEAL_LANDS - 0.3, 0.8));
        double bob = ramp(elapsed - MEDALLIONS_IN - FACTIONS.length * MEDALLION_STEP - 0.3, 1);
        for (int i = 0; i < medallions.size(); i++) {
            medallions.get(i).setTranslateY(2.5 * Math.sin(elapsed * 3.2 - i * 0.7) * bob);
        }
        flash.setOpacity(Math.max(fadeOut(0, 0.7, 0.85), fadeOut(SEAL_LANDS, 0.5, 0.6)));
        sweepShine();
    }

    /** Every few seconds a band of light sweeps over the title, left to right. */
    private void sweepShine() {
        double since = elapsed - SHINE_START;
        double into = since % SHINE_PERIOD;
        if (since < 0 || into > SHINE_SWEEP) {
            shine.setVisible(false);
            return;
        }
        double centre = -0.2 + 1.4 * into / SHINE_SWEEP;
        shine.setFill(new LinearGradient(centre - 0.1, 0, centre + 0.1, 0.35, true, CycleMethod.NO_CYCLE, SHINE));
        shine.setVisible(true);
    }

    /** {@code peak} at {@code start}, fading to 0 over {@code length} seconds; 0 outside that time. */
    private double fadeOut(double start, double length, double peak) {
        if (elapsed < start || elapsed > start + length) {
            return 0;
        }
        double left = 1 - (elapsed - start) / length;
        return peak * left * left;
    }

    /** 0 while {@code since} is negative, then rising to 1 over {@code length} seconds. */
    private static double ramp(double since, double length) {
        return Math.max(0, Math.min(1, since / length));
    }

    // ------------------------------------------------------------------ steps of the opening

    /** The seal falls from big to its size, speeding up, and gives a little thump when it lands. */
    private void dropSeal() {
        ScaleTransition fall = Animations.scale(seal, 2.6, 1, SEAL_FALL * 1000);
        fall.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition drop = new ParallelTransition(fall, Animations.fade(seal, 0, 1, SEAL_FALL * 500));
        new SequentialTransition(drop, Animations.pulse(seal, 0.9, 240)).play();
    }

    /** The seal hits: a flash, a ring, a burst of gold, a shower of coins and two cannons of confetti, and the window shakes. */
    private void sealLands() {
        front.ring(SEAL_X, SEAL_Y, 240, Palette.GOLD_BRIGHT, 0.8);
        front.burst(SEAL_X, SEAL_Y, GOLD, 1.4);
        for (int i = 0; i < LANDING_COINS; i++) {
            front.coin(SEAL_X + (random.nextDouble() - 0.5) * 60, SEAL_Y + 10, (random.nextDouble() - 0.5) * 700, -(180 + random.nextDouble() * 320));
        }
        for (int i = 0; i < CANNON_CONFETTI; i++) {
            front.confetti(-8, HEIGHT + 8, 240 + random.nextDouble() * 460, -(560 + random.nextDouble() * 460), confettiColour());
            front.confetti(WIDTH + 8, HEIGHT + 8, -(240 + random.nextDouble() * 460), -(560 + random.nextDouble() * 460), confettiColour());
        }
        Animations.shake(stage, 6, 3, 320).play();
    }

    /** An emblem pops in with a burst and a ring in its faction's colours. */
    private void popMedallion(int index) {
        popIn(medallions.get(index), 0, 2.2, 400);
        Color[] colours = fireworkColours(FACTIONS[index]);
        front.burst(medallionX(index), MEDALLION_Y, colours, 0.5);
        front.ring(medallionX(index), MEDALLION_Y, 56, colours[0], 0.5);
        medallionsShown = index + 1;
    }

    /** A tier rises into place with a little burst of gold and a twinkle on its end. */
    private void showChip(HBox chip) {
        new ParallelTransition(Animations.move(chip, 0, 14, 0, 0, 320), Animations.fade(chip, 0, 1, 320)).play();
        Point2D centre = front.sceneToLocal(chip.localToScene(chip.getWidth() / 2, chip.getHeight() / 2));
        if (centre != null) {
            front.burst(centre.getX(), centre.getY(), GOLD, 0.28);
            front.glint(centre.getX() + chip.getWidth() / 2 - 10, centre.getY() - 6, 28);
        }
    }

    private void showButton() {
        button.setOpacity(0);
        button.setVisible(true);
        Animations.fade(button, 0, 1, 450).play();
        Animations.fade(hint, 0, 1, 450).play();
    }

    /** Grows a node from {@code from} to its size, overshooting a little, while it fades in. */
    private static void popIn(Node node, double from, double overshoot, double millis) {
        ScaleTransition grow = Animations.scale(node, from, 1, millis);
        grow.setInterpolator(new BackOut(overshoot));
        new ParallelTransition(grow, Animations.fade(node, 0, 1, millis * 0.45)).play();
    }

    /** A twinkle around the seal, on the title or on an emblem that is already in. */
    private void glint() {
        double roll = random.nextDouble();
        if (roll < 0.45) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 54 + random.nextDouble() * 34;
            front.glint(SEAL_X + Math.cos(angle) * distance, SEAL_Y + Math.sin(angle) * distance, 15 + random.nextDouble() * 13);
        } else if (roll < 0.8 || medallionsShown == 0) {
            front.glint(WIDTH / 4 + random.nextDouble() * WIDTH / 2, TITLE_Y + 16 + random.nextDouble() * 30, 13 + random.nextDouble() * 11);
        } else {
            int index = random.nextInt(medallionsShown);
            front.glint(medallionX(index) + (random.nextDouble() - 0.5) * 44, MEDALLION_Y + (random.nextDouble() - 0.5) * 44, 11 + random.nextDouble() * 9);
        }
    }

    /** One rocket, often two, on the sides, so the middle stays clear for the crown and the title. */
    private void launchRockets() {
        boolean left = random.nextBoolean();
        launchRocket(left);
        if (random.nextDouble() < ROCKET_PAIRS) {
            launchRocket(!left);
        }
    }

    /** The factions take turns: every rocket bursts in the colours of the next one. */
    private void launchRocket(boolean left) {
        double offset = ROCKET_SIDE + random.nextDouble() * ROCKET_SPREAD;
        sky.rocket(left ? offset : WIDTH - offset, ROCKET_TOP + random.nextDouble() * (ROCKET_BOTTOM - ROCKET_TOP), fireworkColours(FACTIONS[rocketFaction]));
        rocketFaction = (rocketFaction + 1) % FACTIONS.length;
    }

    private boolean isOnButton(Node node) {
        for (Node at = node; at != null; at = at.getParent()) {
            if (at == button) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ handlers

    /** A click anywhere but on the button sets off a firework where it was. */
    private final class ClickFirework implements EventHandler<MouseEvent> {
        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() != MouseButton.PRIMARY || isOnButton(event.getPickResult().getIntersectedNode())) {
                return;
            }
            front.burst(event.getX(), event.getY(), fireworkColours(FACTIONS[random.nextInt(FACTIONS.length)]), 0.9);
        }
    }

    /** Runs the show while the popup is in a scene and stops it once the popup is removed (closed, or the screen was left). */
    private final class SceneWatch implements ChangeListener<Scene> {
        @Override
        public void changed(ObservableValue<? extends Scene> observable, Scene oldScene, Scene newScene) {
            if (newScene != null) {
                ticker.start();
            } else {
                ticker.stop();
            }
        }
    }

    /** Calls {@link #tick} once a frame with the seconds since the previous frame. */
    private final class Ticker extends AnimationTimer {

        private long last = -1;

        @Override
        public void handle(long now) {
            double dt = last < 0 ? 0 : Math.min(MAX_STEP, (now - last) / 1e9);
            last = now;
            tick(dt);
        }

        @Override
        public void stop() {
            super.stop();
            last = -1;
        }
    }

    /** Goes a little past the end value and settles back on it, like something that lands with a bounce. */
    private static final class BackOut extends Interpolator {

        private final double overshoot;

        BackOut(double overshoot) {
            this.overshoot = overshoot;
        }

        @Override
        protected double curve(double t) {
            double u = t - 1;
            return 1 + (overshoot + 1) * u * u * u + overshoot * u * u;
        }
    }
}
