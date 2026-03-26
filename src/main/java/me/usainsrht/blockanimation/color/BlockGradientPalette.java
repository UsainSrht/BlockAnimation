package me.usainsrht.blockanimation.color;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A convenience {@link ColorPalette} defined by Minecraft {@link Material} stops.
 * <p>
 * Each material is resolved to its average {@link RGBColor} via {@link BlockColorRegistry},
 * then the palette delegates to a {@link GradientPalette} for interpolation.
 *
 * <p>Example – copper ageing gradient:
 * <pre>{@code
 * BlockGradientPalette palette = new BlockGradientPalette(List.of(
 *     Material.COPPER_BLOCK,
 *     Material.EXPOSED_COPPER,
 *     Material.WEATHERED_COPPER,
 *     Material.OXIDIZED_COPPER
 * ));
 * }</pre>
 */
public final class BlockGradientPalette implements ColorPalette {

    private final GradientPalette delegate;
    private final List<Material> materials;

    /**
     * @param materials ordered list of block materials whose average colours define the gradient
     * @throws IllegalArgumentException if any material is not registered in the colour registry
     */
    public BlockGradientPalette(List<Material> materials) {
        Objects.requireNonNull(materials, "materials must not be null");
        if (materials.isEmpty()) throw new IllegalArgumentException("At least one material is required");

        this.materials = List.copyOf(materials);
        List<RGBColor> stops = new ArrayList<>(materials.size());
        for (Material mat : materials) {
            RGBColor color = BlockColorRegistry.getColor(mat);
            if (color == null) {
                throw new IllegalArgumentException("Material " + mat + " has no registered color in BlockColorRegistry");
            }
            stops.add(color);
        }
        this.delegate = new GradientPalette(stops);
    }

    @Override
    public RGBColor getColorAt(float t) {
        return delegate.getColorAt(t);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    /**
     * @return the ordered list of materials used as gradient stops
     */
    public List<Material> getMaterials() {
        return materials;
    }
}
