package org.hwyl.sexytopo.control.sketch;

import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * How the rock side of a wall is shown on screen, so which way each wall faces can be seen all
 * along it: a faint gradient, stronger against the wall, with fine stipple fading out into the
 * rock. It's never exported.
 *
 * <p>It's a stack of layers, each a shape stamped along the wall with PathDashPathEffect (as line
 * decoration is) and drawn at its own opacity. Measurements are in multiples of the line ornament
 * size, so it zooms with the cave as decoration does. In a stamp, positive y is the interior side
 * of an oriented wall (see LineOrienter), so everything here is drawn at negative y.
 */
public final class WallRockSide {

    private static final List<Layer> LAYERS = createLayers();

    private WallRockSide() {}

    public static List<Layer> getLayers() {
        return LAYERS;
    }

    private static List<Layer> createLayers() {
        List<Layer> layers = new ArrayList<>();
        // a gradient fading out over an ornament size, with a second, shallow one on top so it
        // starts out about twice as strong against the wall (otherwise it's hard to see in dark
        // mode)
        addGradient(layers, 1.0f, 6, 0x08);
        addGradient(layers, 0.35f, 3, 0x10);
        layers.add(scatter(2.0f, 36, 1.2f, 1.5f, 0.035f, 0x48));
        return Collections.unmodifiableList(layers);
    }

    /** One stamped shape and the opacity (0-255) to draw it at. */
    public static final class Layer {

        private interface StampFactory {
            PathEffect create(float ornamentSizePixels, float lineWidthPixels);
        }

        private final StampFactory factory;
        private final int alpha;

        private Layer(StampFactory factory, int alpha) {
            this.factory = factory;
            this.alpha = alpha;
        }

        public PathEffect createEffect(float ornamentSizePixels, float lineWidthPixels) {
            return factory.create(ornamentSizePixels, lineWidthPixels);
        }

        public int getAlpha() {
            return alpha;
        }
    }

    /**
     * Bands strips, all starting at the wall and reaching up to depth, each at alpha. Stacked,
     * they're darkest against the wall and fade out into the rock.
     */
    private static void addGradient(List<Layer> layers, float depth, int bands, int alpha) {
        for (int i = 0; i < bands; i++) {
            layers.add(strip(depth * (i + 1) / bands, alpha));
        }
    }

    /** A continuous strip running from the wall to depth out from it. */
    private static Layer strip(float depth, int alpha) {
        return new Layer(
                (ornament, lineWidth) -> {
                    float depthPixels = Math.max(depth * ornament, 1);
                    // short pieces bend smoothly round tight curves; the slight overlap stops
                    // seams (the pieces are one path, so overlaps don't darken)
                    float advance = Math.max(depthPixels / 2, 1);
                    Path stamp = new Path();
                    stamp.addRect(0, -depthPixels, advance + 0.5f, 0, Path.Direction.CW);
                    return morph(stamp, advance);
                },
                alpha);
    }

    /**
     * Dots scattered over a stretch of wall spacing long and repeated along it: count dots of the
     * given radius, up to depth out from the wall. Their places are random but fixed, and the
     * stretch is long enough that the repeat isn't obvious. A falloff above 1 crowds them towards
     * the wall, so they thin out into the rock.
     */
    private static Layer scatter(
            float spacing, int count, float depth, float falloff, float radius, int alpha) {
        return new Layer(
                (ornament, lineWidth) -> {
                    float advance = Math.max(spacing * ornament, 3);
                    float radiusPixels = Math.max(radius * ornament, lineWidth * 0.3f);
                    // the same seed every time, so the dots don't jump about as the view changes
                    Random random = new Random(count);
                    Path stamp = new Path();
                    for (int i = 0; i < count; i++) {
                        // spread evenly along the stretch, jittered, so there are no clumps
                        float x = advance * (i + random.nextFloat()) / count;
                        float out =
                                (float) Math.pow(random.nextFloat(), falloff) * depth * ornament;
                        stamp.addCircle(x, -out - radiusPixels, radiusPixels, Path.Direction.CW);
                    }
                    return morph(stamp, advance);
                },
                alpha);
    }

    private static PathEffect morph(Path stamp, float advance) {
        return new PathDashPathEffect(stamp, advance, 0, PathDashPathEffect.Style.MORPH);
    }
}
