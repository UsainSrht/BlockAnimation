package me.usainsrht.blockanimation.visibility;

import com.google.gson.*;
import org.bukkit.Material;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Immutable collection of visible surface blocks around a center point.
 * <p>
 * This is the primary output of {@link VisibilityAnalyzer} and the primary input
 * to the animation engine. It is fully serializable to JSON via Gson for disk
 * caching of pre-calculated rooms/areas.
 *
 * <p><b>Thread safety:</b> instances are immutable after construction and safe to
 * share across threads.
 */
public final class VisibleBlocks {

    private final String worldName;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final int radius;
    private final List<BlockInfo> blocks;

    /**
     * @param worldName the world this scan was performed in
     * @param centerX   scan center X
     * @param centerY   scan center Y
     * @param centerZ   scan center Z
     * @param radius    scan radius
     * @param blocks    the detected visible surface blocks (defensively copied)
     */
    public VisibleBlocks(String worldName, double centerX, double centerY, double centerZ,
                         int radius, List<BlockInfo> blocks) {
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.radius = radius;
        this.blocks = List.copyOf(blocks);
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public String getWorldName()    { return worldName; }
    public double getCenterX()      { return centerX; }
    public double getCenterY()      { return centerY; }
    public double getCenterZ()      { return centerZ; }
    public int    getRadius()       { return radius; }

    /**
     * @return unmodifiable list of visible blocks
     */
    public List<BlockInfo> getBlocks() {
        return blocks; // already unmodifiable via List.copyOf
    }

    /**
     * Convenience shorthand for {@code getBlocks().size()}.
     *
     * @return number of visible surface blocks detected
     */
    public int size() {
        return blocks.size();
    }

    /**
     * @return the maximum Euclidean distance (squared) of any block from the center
     */
    public double getMaxDistanceSquared() {
        double max = 0;
        for (BlockInfo b : blocks) {
            max = Math.max(max, b.distanceSquared(centerX, centerY, centerZ));
        }
        return max;
    }

    // ------------------------------------------------------------------
    // Serialization
    // ------------------------------------------------------------------

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Material.class, new MaterialAdapter())
            .create();

    /**
     * Serialize this object to a JSON file.
     *
     * @param file target file (parent dirs are created automatically)
     * @throws IOException on write failure
     */
    public void save(File file) throws IOException {
        file.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        }
    }

    /**
     * Deserialize a {@link VisibleBlocks} from a JSON file.
     *
     * @param file source file
     * @return the reconstructed object
     * @throws IOException on read failure or malformed JSON
     */
    public static VisibleBlocks load(File file) throws IOException {
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            VisibleBlocks loaded = GSON.fromJson(reader, VisibleBlocks.class);
            if (loaded == null) throw new IOException("Failed to deserialize VisibleBlocks from " + file);
            return loaded;
        }
    }

    /**
     * Serialize to a JSON string.
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * Deserialize from a JSON string.
     */
    public static VisibleBlocks fromJson(String json) {
        return GSON.fromJson(json, VisibleBlocks.class);
    }

    // ------------------------------------------------------------------
    // Gson helpers
    // ------------------------------------------------------------------

    /** Custom adapter for {@link Material} — stores as the enum name string. */
    private static final class MaterialAdapter implements JsonSerializer<Material>, JsonDeserializer<Material> {
        @Override
        public JsonElement serialize(Material src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.name());
        }

        @Override
        public Material deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return Material.matchMaterial(json.getAsString());
        }
    }

    @Override
    public String toString() {
        return "VisibleBlocks{world=" + worldName +
                ", center=(" + centerX + "," + centerY + "," + centerZ + ")" +
                ", radius=" + radius +
                ", blocks=" + blocks.size() + "}";
    }
}
