package dev.blockanimation.color;

import dev.blockanimation.shape.ShapeGroup;
import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Master registry mapping every interesting {@link Material} to its average {@link RGBColor}.
 * <p>
 * Also provides fast reverse-lookup: given an {@link RGBColor}, find the closest matching
 * {@link Material} (optionally filtered by {@link ShapeGroup}).
 * <p>
 * The palette is hardcoded for cross-version stability — no resource-pack dependency.
 */
public final class BlockColorRegistry {

    /** Material → average RGB */
    private static final Map<Material, RGBColor> COLORS = new ConcurrentHashMap<>(256);

    /** Per-ShapeGroup caches for fast filtered lookups */
    private static final Map<ShapeGroup, Map<Material, RGBColor>> GROUP_CACHES = new ConcurrentHashMap<>();

    private BlockColorRegistry() {}

    // ------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------

    /** Warm up the registry on plugin enable. */
    public static void init() {
        if (!COLORS.isEmpty()) return;
        registerDefaults();
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * @return the registered average color for the given material, or {@code null} if unknown.
     */
    public static RGBColor getColor(Material material) {
        return COLORS.get(material);
    }

    /**
     * Register or override a material's average color.
     */
    public static void register(Material material, RGBColor color) {
        COLORS.put(material, color);
        GROUP_CACHES.clear(); // invalidate caches
    }

    /**
     * @return an unmodifiable view of every registered material → color entry.
     */
    public static Map<Material, RGBColor> getAll() {
        return Collections.unmodifiableMap(COLORS);
    }

    /**
     * Find the closest matching material to the given color across <b>all</b> registered blocks.
     */
    public static Material findClosest(RGBColor target) {
        return findClosestIn(COLORS, target);
    }

    /**
     * Find the closest matching material within a specific {@link ShapeGroup}.
     */
    public static Material findClosest(RGBColor target, ShapeGroup group) {
        Map<Material, RGBColor> filtered = GROUP_CACHES.computeIfAbsent(group, g -> {
            Map<Material, RGBColor> map = new HashMap<>();
            for (Map.Entry<Material, RGBColor> entry : COLORS.entrySet()) {
                if (ShapeGroup.classify(entry.getKey()) == g) {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
            return map;
        });
        if (filtered.isEmpty()) return findClosestIn(COLORS, target); // fallback
        return findClosestIn(filtered, target);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static Material findClosestIn(Map<Material, RGBColor> pool, RGBColor target) {
        Material best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Map.Entry<Material, RGBColor> entry : pool.entrySet()) {
            int d = target.distanceSquared(entry.getValue());
            if (d < bestDist) {
                bestDist = d;
                best = entry.getKey();
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Hardcoded palette (~150 common building blocks)
    // ------------------------------------------------------------------

    @SuppressWarnings("java:S3776") // complexity is intentional — one big lookup table
    private static void registerDefaults() {
        // === Stone variants ===
        put(Material.STONE, 125, 125, 125);
        put(Material.GRANITE, 153, 114, 89);
        put(Material.POLISHED_GRANITE, 154, 106, 89);
        put(Material.DIORITE, 188, 188, 188);
        put(Material.POLISHED_DIORITE, 192, 192, 193);
        put(Material.ANDESITE, 136, 136, 136);
        put(Material.POLISHED_ANDESITE, 132, 135, 133);
        put(Material.DEEPSLATE, 80, 80, 82);
        put(Material.COBBLESTONE, 127, 127, 127);
        put(Material.COBBLED_DEEPSLATE, 77, 77, 80);
        put(Material.SMOOTH_STONE, 158, 158, 158);
        put(Material.STONE_BRICKS, 122, 122, 122);
        put(Material.MOSSY_STONE_BRICKS, 115, 121, 105);
        put(Material.CRACKED_STONE_BRICKS, 118, 117, 118);
        put(Material.CHISELED_STONE_BRICKS, 120, 118, 120);
        put(Material.BRICKS, 150, 97, 83);

        // === Dirt / Nature ===
        put(Material.DIRT, 134, 96, 67);
        put(Material.GRASS_BLOCK, 127, 148, 79);
        put(Material.PODZOL, 91, 63, 24);
        put(Material.MYCELIUM, 111, 99, 107);
        put(Material.MUD, 60, 57, 53);
        put(Material.MUD_BRICKS, 137, 104, 75);
        put(Material.PACKED_MUD, 142, 106, 79);
        put(Material.CLAY, 160, 166, 179);
        put(Material.SAND, 219, 207, 163);
        put(Material.RED_SAND, 190, 102, 33);
        put(Material.SANDSTONE, 216, 203, 155);
        put(Material.RED_SANDSTONE, 186, 99, 29);
        put(Material.GRAVEL, 131, 127, 126);

        // === Wood planks ===
        put(Material.OAK_PLANKS, 162, 131, 78);
        put(Material.SPRUCE_PLANKS, 114, 84, 48);
        put(Material.BIRCH_PLANKS, 196, 179, 123);
        put(Material.JUNGLE_PLANKS, 160, 115, 80);
        put(Material.ACACIA_PLANKS, 168, 90, 50);
        put(Material.DARK_OAK_PLANKS, 66, 43, 20);
        put(Material.MANGROVE_PLANKS, 117, 54, 48);
        put(Material.CHERRY_PLANKS, 226, 178, 172);
        put(Material.BAMBOO_PLANKS, 194, 175, 81);
        put(Material.CRIMSON_PLANKS, 101, 48, 70);
        put(Material.WARPED_PLANKS, 43, 104, 99);

        // === Wood logs ===
        put(Material.OAK_LOG, 109, 85, 50);
        put(Material.SPRUCE_LOG, 58, 37, 16);
        put(Material.BIRCH_LOG, 216, 215, 210);
        put(Material.JUNGLE_LOG, 85, 68, 25);
        put(Material.ACACIA_LOG, 103, 96, 86);
        put(Material.DARK_OAK_LOG, 60, 46, 26);

        // === Wool / Concrete / Terracotta (full spectrum) ===
        put(Material.WHITE_WOOL, 234, 236, 236);
        put(Material.ORANGE_WOOL, 241, 118, 20);
        put(Material.MAGENTA_WOOL, 189, 68, 179);
        put(Material.LIGHT_BLUE_WOOL, 58, 175, 217);
        put(Material.YELLOW_WOOL, 248, 199, 40);
        put(Material.LIME_WOOL, 112, 185, 26);
        put(Material.PINK_WOOL, 238, 141, 172);
        put(Material.GRAY_WOOL, 63, 68, 72);
        put(Material.LIGHT_GRAY_WOOL, 142, 142, 135);
        put(Material.CYAN_WOOL, 21, 138, 145);
        put(Material.PURPLE_WOOL, 121, 42, 173);
        put(Material.BLUE_WOOL, 53, 57, 157);
        put(Material.BROWN_WOOL, 114, 72, 41);
        put(Material.GREEN_WOOL, 85, 110, 28);
        put(Material.RED_WOOL, 161, 39, 35);
        put(Material.BLACK_WOOL, 20, 21, 26);

        put(Material.WHITE_CONCRETE, 207, 213, 214);
        put(Material.ORANGE_CONCRETE, 224, 97, 1);
        put(Material.MAGENTA_CONCRETE, 169, 48, 159);
        put(Material.LIGHT_BLUE_CONCRETE, 36, 137, 199);
        put(Material.YELLOW_CONCRETE, 241, 175, 21);
        put(Material.LIME_CONCRETE, 94, 169, 25);
        put(Material.PINK_CONCRETE, 214, 101, 143);
        put(Material.GRAY_CONCRETE, 55, 58, 62);
        put(Material.LIGHT_GRAY_CONCRETE, 125, 125, 115);
        put(Material.CYAN_CONCRETE, 21, 119, 136);
        put(Material.PURPLE_CONCRETE, 100, 32, 156);
        put(Material.BLUE_CONCRETE, 45, 47, 143);
        put(Material.BROWN_CONCRETE, 96, 60, 32);
        put(Material.GREEN_CONCRETE, 73, 91, 36);
        put(Material.RED_CONCRETE, 142, 33, 33);
        put(Material.BLACK_CONCRETE, 8, 10, 15);

        put(Material.WHITE_TERRACOTTA, 210, 178, 161);
        put(Material.ORANGE_TERRACOTTA, 162, 84, 38);
        put(Material.MAGENTA_TERRACOTTA, 150, 88, 109);
        put(Material.LIGHT_BLUE_TERRACOTTA, 113, 109, 138);
        put(Material.YELLOW_TERRACOTTA, 186, 133, 35);
        put(Material.LIME_TERRACOTTA, 103, 118, 53);
        put(Material.PINK_TERRACOTTA, 162, 78, 79);
        put(Material.GRAY_TERRACOTTA, 58, 42, 36);
        put(Material.LIGHT_GRAY_TERRACOTTA, 135, 107, 98);
        put(Material.CYAN_TERRACOTTA, 87, 91, 91);
        put(Material.PURPLE_TERRACOTTA, 118, 70, 86);
        put(Material.BLUE_TERRACOTTA, 74, 60, 91);
        put(Material.BROWN_TERRACOTTA, 77, 51, 36);
        put(Material.GREEN_TERRACOTTA, 76, 83, 42);
        put(Material.RED_TERRACOTTA, 143, 61, 47);
        put(Material.BLACK_TERRACOTTA, 37, 23, 16);
        put(Material.TERRACOTTA, 152, 94, 68);

        // === Glazed Terracotta ===
        put(Material.WHITE_GLAZED_TERRACOTTA, 188, 212, 202);
        put(Material.ORANGE_GLAZED_TERRACOTTA, 154, 147, 91);
        put(Material.MAGENTA_GLAZED_TERRACOTTA, 208, 100, 192);
        put(Material.LIGHT_BLUE_GLAZED_TERRACOTTA, 93, 164, 195);
        put(Material.YELLOW_GLAZED_TERRACOTTA, 234, 192, 88);
        put(Material.LIME_GLAZED_TERRACOTTA, 162, 197, 55);
        put(Material.PINK_GLAZED_TERRACOTTA, 235, 155, 181);
        put(Material.GRAY_GLAZED_TERRACOTTA, 83, 90, 93);
        put(Material.LIGHT_GRAY_GLAZED_TERRACOTTA, 144, 166, 167);
        put(Material.CYAN_GLAZED_TERRACOTTA, 52, 118, 125);
        put(Material.PURPLE_GLAZED_TERRACOTTA, 109, 48, 152);
        put(Material.BLUE_GLAZED_TERRACOTTA, 47, 65, 139);
        put(Material.BROWN_GLAZED_TERRACOTTA, 119, 106, 85);
        put(Material.GREEN_GLAZED_TERRACOTTA, 117, 142, 67);
        put(Material.RED_GLAZED_TERRACOTTA, 181, 59, 53);
        put(Material.BLACK_GLAZED_TERRACOTTA, 67, 30, 32);

        // === Metals / Minerals ===
        put(Material.IRON_BLOCK, 220, 220, 220);
        put(Material.GOLD_BLOCK, 249, 236, 79);
        put(Material.DIAMOND_BLOCK, 98, 237, 228);
        put(Material.EMERALD_BLOCK, 42, 176, 72);
        put(Material.LAPIS_BLOCK, 30, 67, 140);
        put(Material.REDSTONE_BLOCK, 171, 27, 4);
        put(Material.NETHERITE_BLOCK, 66, 61, 63);
        put(Material.COPPER_BLOCK, 192, 107, 79);
        put(Material.EXPOSED_COPPER, 161, 125, 103);
        put(Material.WEATHERED_COPPER, 108, 153, 110);
        put(Material.OXIDIZED_COPPER, 82, 162, 132);
        put(Material.AMETHYST_BLOCK, 133, 97, 191);
        put(Material.RAW_IRON_BLOCK, 166, 136, 107);
        put(Material.RAW_GOLD_BLOCK, 221, 169, 47);
        put(Material.RAW_COPPER_BLOCK, 154, 105, 70);
        put(Material.COAL_BLOCK, 16, 15, 15);

        // === Nether ===
        put(Material.NETHERRACK, 98, 38, 38);
        put(Material.NETHER_BRICKS, 44, 22, 26);
        put(Material.CRIMSON_NYLIUM, 130, 32, 32);
        put(Material.WARPED_NYLIUM, 43, 114, 101);
        put(Material.BASALT, 73, 72, 77);
        put(Material.POLISHED_BASALT, 100, 100, 101);
        put(Material.SMOOTH_BASALT, 72, 72, 78);
        put(Material.BLACKSTONE, 42, 36, 41);
        put(Material.POLISHED_BLACKSTONE, 53, 49, 56);
        put(Material.POLISHED_BLACKSTONE_BRICKS, 48, 43, 50);
        put(Material.GLOWSTONE, 171, 131, 84);
        put(Material.SOUL_SAND, 81, 62, 51);
        put(Material.SOUL_SOIL, 75, 57, 46);
        put(Material.SHROOMLIGHT, 240, 146, 70);
        put(Material.CRYING_OBSIDIAN, 32, 10, 60);
        put(Material.OBSIDIAN, 15, 11, 25);

        // === End ===
        put(Material.END_STONE, 219, 223, 158);
        put(Material.END_STONE_BRICKS, 218, 224, 162);
        put(Material.PURPUR_BLOCK, 170, 126, 170);
        put(Material.PURPUR_PILLAR, 172, 130, 172);

        // === Prismarine ===
        put(Material.PRISMARINE, 99, 156, 151);
        put(Material.PRISMARINE_BRICKS, 99, 172, 158);
        put(Material.DARK_PRISMARINE, 51, 91, 75);
        put(Material.SEA_LANTERN, 172, 199, 190);

        // === Misc ===
        put(Material.QUARTZ_BLOCK, 236, 230, 223);
        put(Material.SMOOTH_QUARTZ, 236, 230, 223);
        put(Material.BONE_BLOCK, 229, 225, 207);
        put(Material.DRIED_KELP_BLOCK, 50, 58, 37);
        put(Material.HONEY_BLOCK, 251, 186, 52);
        put(Material.HONEYCOMB_BLOCK, 229, 148, 29);
        put(Material.SLIME_BLOCK, 112, 192, 91);
        put(Material.ICE, 145, 183, 253);
        put(Material.PACKED_ICE, 141, 180, 250);
        put(Material.BLUE_ICE, 116, 167, 253);
        put(Material.SNOW_BLOCK, 249, 254, 254);
        put(Material.MOSS_BLOCK, 89, 109, 45);
        put(Material.CALCITE, 224, 225, 221);
        put(Material.TUFF, 108, 109, 102);
        put(Material.DRIPSTONE_BLOCK, 134, 107, 92);
        put(Material.OCHRE_FROGLIGHT, 248, 228, 165);
        put(Material.VERDANT_FROGLIGHT, 229, 244, 212);
        put(Material.PEARLESCENT_FROGLIGHT, 245, 224, 237);
        put(Material.SCULK, 12, 29, 38);

        // === Glass (transparent but visible) ===
        put(Material.GLASS, 175, 213, 219);
        put(Material.WHITE_STAINED_GLASS, 255, 255, 255);
        put(Material.ORANGE_STAINED_GLASS, 216, 127, 51);
        put(Material.MAGENTA_STAINED_GLASS, 178, 76, 216);
        put(Material.LIGHT_BLUE_STAINED_GLASS, 102, 153, 216);
        put(Material.YELLOW_STAINED_GLASS, 229, 229, 51);
        put(Material.LIME_STAINED_GLASS, 127, 204, 25);
        put(Material.PINK_STAINED_GLASS, 242, 127, 165);
        put(Material.GRAY_STAINED_GLASS, 76, 76, 76);
        put(Material.LIGHT_GRAY_STAINED_GLASS, 153, 153, 153);
        put(Material.CYAN_STAINED_GLASS, 76, 127, 153);
        put(Material.PURPLE_STAINED_GLASS, 127, 63, 178);
        put(Material.BLUE_STAINED_GLASS, 51, 76, 178);
        put(Material.BROWN_STAINED_GLASS, 102, 76, 51);
        put(Material.GREEN_STAINED_GLASS, 102, 127, 51);
        put(Material.RED_STAINED_GLASS, 153, 51, 51);
        put(Material.BLACK_STAINED_GLASS, 25, 25, 25);

        // === Stairs (inherits parent color; classified as STAIR shape) ===
        put(Material.OAK_STAIRS, 162, 131, 78);
        put(Material.SPRUCE_STAIRS, 114, 84, 48);
        put(Material.BIRCH_STAIRS, 196, 179, 123);
        put(Material.JUNGLE_STAIRS, 160, 115, 80);
        put(Material.ACACIA_STAIRS, 168, 90, 50);
        put(Material.DARK_OAK_STAIRS, 66, 43, 20);
        put(Material.MANGROVE_STAIRS, 117, 54, 48);
        put(Material.CHERRY_STAIRS, 226, 178, 172);
        put(Material.BAMBOO_STAIRS, 194, 175, 81);
        put(Material.CRIMSON_STAIRS, 101, 48, 70);
        put(Material.WARPED_STAIRS, 43, 104, 99);
        put(Material.STONE_STAIRS, 125, 125, 125);
        put(Material.COBBLESTONE_STAIRS, 127, 127, 127);
        put(Material.STONE_BRICK_STAIRS, 122, 122, 122);
        put(Material.MOSSY_STONE_BRICK_STAIRS, 115, 121, 105);
        put(Material.BRICK_STAIRS, 150, 97, 83);
        put(Material.SANDSTONE_STAIRS, 216, 203, 155);
        put(Material.RED_SANDSTONE_STAIRS, 186, 99, 29);
        put(Material.NETHER_BRICK_STAIRS, 44, 22, 26);
        put(Material.QUARTZ_STAIRS, 236, 230, 223);
        put(Material.PRISMARINE_STAIRS, 99, 156, 151);
        put(Material.PRISMARINE_BRICK_STAIRS, 99, 172, 158);
        put(Material.DARK_PRISMARINE_STAIRS, 51, 91, 75);
        put(Material.PURPUR_STAIRS, 170, 126, 170);
        put(Material.POLISHED_GRANITE_STAIRS, 154, 106, 89);
        put(Material.POLISHED_DIORITE_STAIRS, 192, 192, 193);
        put(Material.POLISHED_ANDESITE_STAIRS, 132, 135, 133);
        put(Material.DEEPSLATE_BRICK_STAIRS, 70, 70, 73);
        put(Material.COBBLED_DEEPSLATE_STAIRS, 77, 77, 80);
        put(Material.BLACKSTONE_STAIRS, 42, 36, 41);
        put(Material.POLISHED_BLACKSTONE_STAIRS, 53, 49, 56);
        put(Material.POLISHED_BLACKSTONE_BRICK_STAIRS, 48, 43, 50);
        put(Material.MUD_BRICK_STAIRS, 137, 104, 75);
        put(Material.END_STONE_BRICK_STAIRS, 218, 224, 162);

        // === Slabs ===
        put(Material.OAK_SLAB, 162, 131, 78);
        put(Material.SPRUCE_SLAB, 114, 84, 48);
        put(Material.BIRCH_SLAB, 196, 179, 123);
        put(Material.JUNGLE_SLAB, 160, 115, 80);
        put(Material.ACACIA_SLAB, 168, 90, 50);
        put(Material.DARK_OAK_SLAB, 66, 43, 20);
        put(Material.MANGROVE_SLAB, 117, 54, 48);
        put(Material.CHERRY_SLAB, 226, 178, 172);
        put(Material.BAMBOO_SLAB, 194, 175, 81);
        put(Material.CRIMSON_SLAB, 101, 48, 70);
        put(Material.WARPED_SLAB, 43, 104, 99);
        put(Material.STONE_SLAB, 125, 125, 125);
        put(Material.COBBLESTONE_SLAB, 127, 127, 127);
        put(Material.STONE_BRICK_SLAB, 122, 122, 122);
        put(Material.MOSSY_STONE_BRICK_SLAB, 115, 121, 105);
        put(Material.BRICK_SLAB, 150, 97, 83);
        put(Material.SANDSTONE_SLAB, 216, 203, 155);
        put(Material.RED_SANDSTONE_SLAB, 186, 99, 29);
        put(Material.NETHER_BRICK_SLAB, 44, 22, 26);
        put(Material.QUARTZ_SLAB, 236, 230, 223);
        put(Material.PRISMARINE_SLAB, 99, 156, 151);
        put(Material.PRISMARINE_BRICK_SLAB, 99, 172, 158);
        put(Material.DARK_PRISMARINE_SLAB, 51, 91, 75);
        put(Material.PURPUR_SLAB, 170, 126, 170);
        put(Material.DEEPSLATE_BRICK_SLAB, 70, 70, 73);
        put(Material.COBBLED_DEEPSLATE_SLAB, 77, 77, 80);
        put(Material.BLACKSTONE_SLAB, 42, 36, 41);
        put(Material.POLISHED_BLACKSTONE_SLAB, 53, 49, 56);
        put(Material.POLISHED_BLACKSTONE_BRICK_SLAB, 48, 43, 50);
        put(Material.MUD_BRICK_SLAB, 137, 104, 75);
        put(Material.END_STONE_BRICK_SLAB, 218, 224, 162);
        put(Material.SMOOTH_STONE_SLAB, 158, 158, 158);
        put(Material.CUT_SANDSTONE_SLAB, 218, 206, 160);
        put(Material.CUT_RED_SANDSTONE_SLAB, 189, 102, 32);

        // === Fences ===
        put(Material.OAK_FENCE, 162, 131, 78);
        put(Material.SPRUCE_FENCE, 114, 84, 48);
        put(Material.BIRCH_FENCE, 196, 179, 123);
        put(Material.JUNGLE_FENCE, 160, 115, 80);
        put(Material.ACACIA_FENCE, 168, 90, 50);
        put(Material.DARK_OAK_FENCE, 66, 43, 20);
        put(Material.MANGROVE_FENCE, 117, 54, 48);
        put(Material.CHERRY_FENCE, 226, 178, 172);
        put(Material.BAMBOO_FENCE, 194, 175, 81);
        put(Material.CRIMSON_FENCE, 101, 48, 70);
        put(Material.WARPED_FENCE, 43, 104, 99);
        put(Material.NETHER_BRICK_FENCE, 44, 22, 26);

        // === Glass panes ===
        put(Material.GLASS_PANE, 175, 213, 219);
        put(Material.WHITE_STAINED_GLASS_PANE, 255, 255, 255);
        put(Material.ORANGE_STAINED_GLASS_PANE, 216, 127, 51);
        put(Material.MAGENTA_STAINED_GLASS_PANE, 178, 76, 216);
        put(Material.LIGHT_BLUE_STAINED_GLASS_PANE, 102, 153, 216);
        put(Material.YELLOW_STAINED_GLASS_PANE, 229, 229, 51);
        put(Material.LIME_STAINED_GLASS_PANE, 127, 204, 25);
        put(Material.PINK_STAINED_GLASS_PANE, 242, 127, 165);
        put(Material.GRAY_STAINED_GLASS_PANE, 76, 76, 76);
        put(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 153, 153, 153);
        put(Material.CYAN_STAINED_GLASS_PANE, 76, 127, 153);
        put(Material.PURPLE_STAINED_GLASS_PANE, 127, 63, 178);
        put(Material.BLUE_STAINED_GLASS_PANE, 51, 76, 178);
        put(Material.BROWN_STAINED_GLASS_PANE, 102, 76, 51);
        put(Material.GREEN_STAINED_GLASS_PANE, 102, 127, 51);
        put(Material.RED_STAINED_GLASS_PANE, 153, 51, 51);
        put(Material.BLACK_STAINED_GLASS_PANE, 25, 25, 25);
        put(Material.IRON_BARS, 164, 164, 164);
    }

    private static void put(Material mat, int r, int g, int b) {
        COLORS.put(mat, new RGBColor(r, g, b));
    }
}
