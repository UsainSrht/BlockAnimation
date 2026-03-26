package me.usainsrht.blockanimation.shape;

import org.bukkit.Material;

/**
 * Groups blocks by their visual shape so that animations can swap materials
 * without breaking the block's silhouette (stairs stay stairs, doors stay doors, etc.).
 * <p>
 * Classification is based on material <b>name heuristics</b> for cross-version stability.
 */
public enum ShapeGroup {

    /** Standard full-cube blocks (stone, planks, concrete, wool, glass, etc.) */
    FULL_BLOCK,

    /** Bottom/top/double slabs */
    SLAB,

    /** Stair blocks with facing, half, and shape data */
    STAIR,

    /** Glass panes and iron bars (thin + connectable) */
    PANE,

    /** Wooden/nether-brick fences (connectable posts) */
    FENCE,

    /** Wall blocks (connectable with height per face) */
    WALL,

    /** Carpet blocks (flat 1-pixel-tall) */
    CARPET,

    /** Doors (two-block tall, facing, hinge, open/closed) */
    DOOR,

    /** Trapdoors (facing, half, open/closed, waterlogged) */
    TRAPDOOR,

    /** Buttons (face-attachable, facing, powered) */
    BUTTON,

    /** Pressure plates (simple plate on ground) */
    PRESSURE_PLATE,

    /** Thin pass-through decorations: flowers, tall grass, saplings, etc. */
    THIN_PASSTHROUGH,

    /** Anything that doesn't fit the above categories */
    OTHER;

    /**
     * Classify a material into its {@link ShapeGroup}.
     */
    public static ShapeGroup classify(Material material) {
        if (material == null || !material.isBlock()) return OTHER;

        String name = material.name();

        // ---- Specific patterns first (most → least specific) ----
        if (name.endsWith("_STAIRS"))          return STAIR;
        if (name.endsWith("_SLAB"))            return SLAB;
        if (name.endsWith("_FENCE"))           return FENCE;
        if (name.endsWith("_WALL"))            return WALL;
        if (name.endsWith("_CARPET"))          return CARPET;
        if (name.endsWith("_DOOR"))            return DOOR;
        if (name.endsWith("_TRAPDOOR"))        return TRAPDOOR;
        if (name.endsWith("_BUTTON"))          return BUTTON;
        if (name.endsWith("_PRESSURE_PLATE"))  return PRESSURE_PLATE;

        // Special-case: heavy/light weighted pressure plates
        if (name.equals("HEAVY_WEIGHTED_PRESSURE_PLATE")
                || name.equals("LIGHT_WEIGHTED_PRESSURE_PLATE")) return PRESSURE_PLATE;

        // Panes
        if (name.endsWith("_GLASS_PANE") || name.equals("GLASS_PANE") || name.equals("IRON_BARS")) {
            return PANE;
        }

        // Thin vegetation / decorations
        if (isThinPassthrough(name)) return THIN_PASSTHROUGH;

        // Near-full blocks — treat as full cubes (negligible shape differences)
        if (isNearFullBlock(name)) return FULL_BLOCK;

        // Everything else that is a solid or semi-solid block
        if (material.isSolid() || name.endsWith("_GLASS") || name.equals("GLASS")
                || name.contains("ICE") || name.contains("LEAVES")) {
            return FULL_BLOCK;
        }

        return OTHER;
    }

    /**
     * @return true if this shape group can meaningfully be animated
     */
    public boolean isAnimatable() {
        return this == FULL_BLOCK || this == SLAB || this == STAIR
                || this == PANE || this == FENCE || this == WALL
                || this == CARPET || this == DOOR || this == TRAPDOOR
                || this == BUTTON || this == PRESSURE_PLATE;
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static boolean isNearFullBlock(String name) {
        return name.equals("DIRT_PATH") || name.equals("FARMLAND")
                || name.equals("SOUL_SAND") || name.equals("SOUL_SOIL")
                || name.equals("CACTUS") || name.equals("COMPOSTER")
                || name.equals("ENCHANTING_TABLE") || name.equals("DAYLIGHT_DETECTOR")
                || name.equals("HONEY_BLOCK") || name.equals("SLIME_BLOCK")
                || name.equals("SCULK_SENSOR") || name.equals("CALIBRATED_SCULK_SENSOR");
    }

    private static boolean isThinPassthrough(String name) {
        return name.endsWith("_FLOWER") || name.endsWith("_TULIP") || name.endsWith("_ORCHID")
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
                || name.equals("PITCHER_PLANT") || name.equals("PINK_PETALS");
    }
}
