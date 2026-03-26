package me.usainsrht.blockanimation.color;

/**
 * An ordered sequence of colors that can be sampled at any position.
 * <p>
 * Implementations include {@link GradientPalette} (RGB stops) and
 * {@link BlockGradientPalette} (Material stops resolved via {@link BlockColorRegistry}).
 */
public interface ColorPalette {

    /**
     * Sample the palette at position {@code t}.
     *
     * @param t normalized position in the range [0, 1]
     * @return the interpolated color at that position
     */
    RGBColor getColorAt(float t);

    /**
     * @return the number of color stops in this palette
     */
    int size();
}
