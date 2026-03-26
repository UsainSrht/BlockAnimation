package me.usainsrht.blockanimation.animation;

/**
 * Controls how an animation cycles when it reaches the end of its palette/sweep.
 *
 * <ul>
 *   <li>{@link #RESTART} — R→G→B → R→G→B → … (jumps back to start)</li>
 *   <li>{@link #REVERSE} — R→G→B → G→R → G→B → … (ping-pong / yoyo)</li>
 *   <li>{@link #ONCE}    — R→G→B, then holds at the final frame until the animation ends</li>
 * </ul>
 */
public enum LoopMode {

    /**
     * Loop by jumping back to the beginning.
     * <p>Progress: 0→1 → 0→1 → 0→1 …
     */
    RESTART,

    /**
     * Loop by reversing direction (ping-pong / yoyo).
     * <p>Progress: 0→1 → 1→0 → 0→1 …
     */
    REVERSE,

    /**
     * Play once and hold the final frame until the animation ends.
     * <p>Progress: 0→1, then stays at 1.
     */
    ONCE;

    /**
     * Compute the effective progress value for the given elapsed ticks and cycle duration,
     * respecting this loop mode.
     *
     * @param elapsedTicks ticks since animation started
     * @param cycleTicks   ticks for one full cycle (0→1)
     * @return a value in [0, 1] representing the current position in the palette/sweep
     */
    public float resolveProgress(long elapsedTicks, long cycleTicks) {
        if (cycleTicks <= 0) return 0f;

        float raw = (float) elapsedTicks / cycleTicks;

        return switch (this) {
            case RESTART -> raw % 1.0f;
            case REVERSE -> {
                // Full cycle = forward + backward = 2× cycleTicks
                float phase = raw % 2.0f;
                yield (phase <= 1.0f) ? phase : 2.0f - phase;
            }
            case ONCE -> Math.min(raw, 1.0f);
        };
    }
}
