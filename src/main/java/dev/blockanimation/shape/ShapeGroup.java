package dev.blockanimation.shape;

import org.bukkit.Material;

/**
 * Groups blocks by their visual shape so that animations can swap materials
 * without breaking the block's silhouette (stairs stay stairs, slabs stay slabs, etc.).
 * <p>
 * Classification is intentionally based on material <b>name heuristics</b> rather than
 * NMS bounding-box data, keeping the code version-stable across 1.17–1.21+.
 */
public enum ShapeGroup {

    /** Standard full-cube blocks (stone, planks, concrete, wool, etc.) */
    FULL_BLOCK,

    /** Bottom/top/double slabs */
    SLAB,

    /** Stair blocks with facing, half, and shape data */
    STAIR,

    /** Glass panes and iron bars (thin + connectable) */
    PANE,

    /** Wooden/nether-brick fences (connectable posts) */
    FENCE,

    /** Wall blocks */
    WALL,

    /** Thin pass-through decorations: flowers, tall grass, saplings, etc. */
    THIN_PASSTHROUGH,

    /** Carpet blocks */
    CARPET,

    /** Anything that doesn't fit the above categories */
    OTHER;

    /**
     * Classify a material into its {@link ShapeGroup} using material name patterns.
     * <p>
     * This is intentionally loose — it covers ~95 % of common building blocks.
     * Unknown materials fall through to {@link #OTHER}.
     */
    public static ShapeGroup classify(Material material) {
        if (material == null || !material.isBlock()) return OTHER;

        String name = material.name();

        // Order matters: check more specific patterns first
        if (name.endsWith("_STAIRS"))       return STAIR;
        if (name.endsWith("_SLAB"))         return SLAB;
        if (name.endsWith("_FENCE"))        return FENCE;
        if (name.endsWith("_WALL"))         return WALL;
        if (name.endsWith("_CARPET"))       return CARPET;

        if (name.endsWith("_GLASS_PANE") || name.equals("GLASS_PANE") || name.equals("IRON_BARS")) {
            return PANE;
        }

        // Thin vegetation / decorations
        if (isThinPassthrough(material, name)) return THIN_PASSTHROUGH;

        // Everything else that is a solid or semi-solid block
        if (material.isSolid() || name.endsWith("_GLASS") || name.equals("GLASS")
                || name.contains("ICE") || name.contains("LEAVES")) {
            return FULL_BLOCK;
        }

        return OTHER;
    }

    /**
     * @return true if this shape group can meaningfully be animated
     *         (i.e. we have palette-matching materials for it)
     */
    public boolean isAnimatable() {
        return this == FULL_BLOCK || this == SLAB || this == STAIR
                || this == PANE || this == FENCE;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static boolean isThinPassthrough(Material material, String name) {
        if (name.endsWith("_FLOWER") || name.endsWith("_TULIP") || name.endsWith("_ORCHID")
                || name.equals("DANDELION") || name.equals("POPPY") || name.equals("CORNFLOWER")
                || name.equals("LILY_OF_THE_VALLEY") || name.equals("AZURE_BLUET")
                || name.equals("ALLIUM") || name.equals("OXEYE_DAISY") || name.equals("TORCHFLOWER")
                || name.contains("SAPLING") || name.equals("DEAD_BUSH")
                || name.equals("SHORT_GRASS") || name.equals("TALL_GRASS") || name.equals("FERN")
                || name.equals("LARGE_FERN") || name.equals("VINE") || name.equals("SUGAR_CANE")
                || name.equals("KELP") || name.equals("KELP_PLANT")
                || name.equals("SEAGRASS") || name.equals("TALL_SEAGRASS")
                || name.equals("HANGING_ROOTS") || name.equals("GLOW_LICHEN")
                || name.equals("MOSS_CARPET") || name.equals("SMALL_DRIPLEAF")
                || name.equals("BIG_DRIPLEAF") || name.equals("SPORE_BLOSSOM")
                || name.equals("CAVE_VINES") || name.equals("CAVE_VINES_PLANT")
                || name.contains("TORCH") || name.equals("FIRE") || name.equals("SOUL_FIRE")
                || name.equals("COBWEB") || name.equals("SWEET_BERRY_BUSH")
                || name.equals("PITCHER_PLANT") || name.equals("PINK_PETALS")) {
            return true;
        }
        return false;
    }
}
