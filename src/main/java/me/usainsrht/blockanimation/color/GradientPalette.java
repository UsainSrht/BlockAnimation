package me.usainsrht.blockanimation.color;


import java.util.List;
import java.util.Objects;

/**
 * A {@link ColorPalette} defined by arbitrary RGB color stops with linear interpolation
 * between consecutive stops.
 *
 * <p>Example – a blue→purple→yellow sweep:
 * <pre>{@code
 * GradientPalette gradient = new GradientPalette(List.of(
 *     new RGBColor(0, 0, 255),   // blue
 *     new RGBColor(128, 0, 255), // purple
 *     new RGBColor(255, 255, 0)  // yellow
 * ));
 * }</pre>
 */
public final class GradientPalette implements ColorPalette {

    private final List<RGBColor> stops;

    /**
     * @param stops at least 1 color stop; order matters
     * @throws IllegalArgumentException if the list is empty
     */
    public GradientPalette(List<RGBColor> stops) {
        Objects.requireNonNull(stops, "stops must not be null");
        if (stops.isEmpty()) throw new IllegalArgumentException("At least one color stop is required");
        this.stops = List.copyOf(stops);
    }

    @Override
    public RGBColor getColorAt(float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        if (stops.size() == 1) return stops.get(0);

        float scaled = clamped * (stops.size() - 1);
        int idx = (int) scaled;
        float frac = scaled - idx;

        if (idx >= stops.size() - 1) return stops.get(stops.size() - 1);
        return stops.get(idx).lerp(stops.get(idx + 1), frac);
    }

    @Override
    public int size() {
        return stops.size();
    }

    /**
     * @return an unmodifiable view of the color stops
     */
    public List<RGBColor> getStops() {
        return stops;
    }
}
