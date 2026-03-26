package me.usainsrht.blockanimation.visibility;

import org.bukkit.Material;


/**
 * Utility for classifying blocks as solid, transparent, or passable for the
 * BFS visibility flood-fill.
 * <p>
 * The categories control how the scanner propagates:
 * <ul>
 *   <li><b>Solid/opaque</b> — stops propagation, registered as a visible surface.</li>
 *   <li><b>Transparent/translucent/non-full</b> — propagation passes through,
 *       but the block itself is also registered as visible (glass, ice, slabs, stairs).</li>
 *   <li><b>Air/passable</b> — propagation passes through, block is <i>not</i> registered.</li>
 * </ul>
 */
public final class TransparencyHelper {

    private TransparencyHelper() {}

    // ------------------------------------------------------------------
    // Primary classification
    // ------------------------------------------------------------------

    /**
     * @return {@code true} if the block is fully opaque and blocks all visibility.
     *         These are the "wall" blocks that stop the BFS.
     */
    public static boolean isOpaque(Material material) {
        if (material.isAir()) return false;
        if (!material.isBlock()) return false;
        if (isTransparent(material)) return false;
        if (isPassable(material)) return false;
        return material.isOccluding();
    }

    /**
     * @return {@code true} if the block is transparent or translucent but still
     *         has a visible rendered face (glass, ice, leaves, slabs, stairs, etc.).
     *         The BFS passes through these blocks but also records them as visible.
     */
    public static boolean isTransparent(Material material) {
        if (material.isAir()) return false;
        String name = material.name();

        // Glass / stained glass / glass panes
        if (name.contains("GLASS")) return true;

        // Ice
        if (name.contains("ICE")) return true;

        // Leaves
        if (name.contains("LEAVES")) return true;

        // Slabs (non-full)
        if (name.endsWith("_SLAB")) return true;

        // Stairs (non-full)
        if (name.endsWith("_STAIRS")) return true;

        // Fences & walls (narrow collision)
        if (name.endsWith("_FENCE") || name.endsWith("_WALL")) return true;
        if (name.equals("IRON_BARS")) return true;

        // Doors & trapdoors
        if (name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR")) return true;

        // Honey, slime
        if (material == Material.HONEY_BLOCK || material == Material.SLIME_BLOCK) return true;

        // Tinted glass
        if (name.equals("TINTED_GLASS")) return true;

        // Carpets
        if (name.endsWith("_CARPET")) return true;

        // Lanterns / bells / chains
        if (name.contains("LANTERN") || name.equals("CHAIN") || name.equals("BELL")) return true;

        // Brewing stand, enchanting table, anvil, hopper, etc.
        if (name.contains("ANVIL") || name.equals("BREWING_STAND")
                || name.equals("ENCHANTING_TABLE") || name.equals("HOPPER")) return true;

        // Sculk sensor / calibrated sculk sensor
        if (name.contains("SCULK_SENSOR")) return true;

        return false;
    }

    /**
     * @return {@code true} if the block is passable / non-solid (air, flowers, tall grass, torches, etc.).
     *         The BFS passes through these and does <i>not</i> record them as visible surfaces.
     */
    public static boolean isPassable(Material material) {
        if (material.isAir()) return true;
        if (!material.isBlock()) return true;

        String name = material.name();

        // Vegetation
        if (name.endsWith("_FLOWER") || name.endsWith("_TULIP") || name.endsWith("_ORCHID")
                || name.equals("DANDELION") || name.equals("POPPY") || name.equals("CORNFLOWER")
                || name.equals("LILY_OF_THE_VALLEY") || name.equals("AZURE_BLUET")
                || name.equals("ALLIUM") || name.equals("OXEYE_DAISY") || name.equals("TORCHFLOWER")
                || name.equals("SHORT_GRASS") || name.equals("TALL_GRASS")
                || name.equals("FERN") || name.equals("LARGE_FERN")
                || name.equals("DEAD_BUSH") || name.equals("VINE")
                || name.equals("HANGING_ROOTS") || name.equals("GLOW_LICHEN")
                || name.equals("MOSS_CARPET") || name.equals("SMALL_DRIPLEAF")
                || name.equals("BIG_DRIPLEAF") || name.equals("SPORE_BLOSSOM")
                || name.equals("CAVE_VINES") || name.equals("CAVE_VINES_PLANT")
                || name.equals("SWEET_BERRY_BUSH") || name.equals("PITCHER_PLANT")
                || name.equals("PINK_PETALS") || name.contains("SAPLING")) {
            return true;
        }

        // Torches
        if (name.contains("TORCH")) return true;

        // Fire
        if (name.equals("FIRE") || name.equals("SOUL_FIRE")) return true;

        // Redstone components (non-solid)
        if (name.equals("REDSTONE_WIRE") || name.equals("REDSTONE_TORCH")
                || name.equals("REDSTONE_WALL_TORCH") || name.equals("LEVER")
                || name.equals("TRIPWIRE") || name.equals("TRIPWIRE_HOOK")
                || name.equals("STRING")) return true;

        // Signs (all variants)
        if (name.contains("SIGN")) return true;

        // Buttons
        if (name.endsWith("_BUTTON")) return true;

        // Pressure plates
        if (name.endsWith("_PRESSURE_PLATE")) return true;

        // Rails
        if (name.contains("RAIL")) return true;

        // Cobweb (passable for scan purposes — the BFS should see through it)
        if (name.equals("COBWEB")) return true;

        // Water / lava (buckets are items, but the block variants)
        if (material == Material.WATER || material == Material.LAVA) return true;

        // Snow layer (single)
        if (name.equals("SNOW")) return true;

        // Banner
        if (name.contains("BANNER")) return true;

        // Heads / skulls
        if (name.contains("HEAD") || name.contains("SKULL")) return true;

        // Light block
        if (name.equals("LIGHT")) return true;

        // Structure void / barrier are special
        if (name.equals("STRUCTURE_VOID")) return true;

        return false;
    }

    /**
     * Combined check: can the BFS propagate through this block?
     * (i.e., it is not an opaque wall)
     */
    public static boolean canPropagate(Material material) {
        return !isOpaque(material);
    }

    /**
     * Should this block be recorded as a visible surface?
     * Opaque blocks adjacent to air/transparent → yes.
     * Transparent blocks → yes (they are visible themselves).
     * Passable blocks → no.
     */
    public static boolean isVisibleSurface(Material material) {
        return isOpaque(material) || isTransparent(material);
    }
}
