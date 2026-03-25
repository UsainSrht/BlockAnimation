package dev.blockanimation.color;

/**
 * Immutable RGB color value.
 *
 * @param r red component (0-255)
 * @param g green component (0-255)
 * @param b blue component (0-255)
 */
public record RGBColor(int r, int g, int b) {

    /**
     * Squared Euclidean distance in RGB space — avoids the sqrt for fast comparisons.
     */
    public int distanceSquared(RGBColor other) {
        int dr = this.r - other.r;
        int dg = this.g - other.g;
        int db = this.b - other.b;
        return dr * dr + dg * dg + db * db;
    }

    /**
     * Linearly interpolate between this color and {@code other} at position {@code t} (0..1).
     */
    public RGBColor lerp(RGBColor other, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return new RGBColor(
                Math.round(this.r + (other.r - this.r) * clamped),
                Math.round(this.g + (other.g - this.g) * clamped),
                Math.round(this.b + (other.b - this.b) * clamped)
        );
    }

    /**
     * Pack into a single 24-bit integer (0xRRGGBB).
     */
    public int toInt() {
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Unpack from a 24-bit integer.
     */
    public static RGBColor fromInt(int rgb) {
        return new RGBColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    @Override
    public String toString() {
        return String.format("#%02X%02X%02X", r, g, b);
    }
}
