package dev.blockanimation.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.blockanimation.BlockAnimationAPI;
import dev.blockanimation.animation.LoopMode;
import dev.blockanimation.animation.types.NoiseSparkleAnimation;
import dev.blockanimation.animation.types.PassThroughAnimation;
import dev.blockanimation.animation.types.SpreadAnimation;
import dev.blockanimation.color.GradientPalette;
import dev.blockanimation.color.RGBColor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Brigadier test command for BlockAnimation.
 * <p>
 * Colors: comma-separated hex strings, e.g. {@code "ff0000,00ff00,0000ff"}.
 * <p>
 * Common args first (radius, duration, colors, loopMode), specific args after.
 * Default value hints shown in suggestions.
 */
@SuppressWarnings("UnstableApiUsage")
public final class BlockAnimationCommand {

    private final BlockAnimationAPI api;

    public BlockAnimationCommand(BlockAnimationAPI api) {
        this.api = api;
    }

    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return Commands.literal("ba")
                .requires(src -> src.getSender() instanceof Player
                        && src.getSender().hasPermission("blockanimation.test"))
                .then(buildSpread())
                .then(buildPassThrough())
                .then(buildSparkle())
                .then(Commands.literal("stop").executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    api.stopAnimations(player);
                    player.sendMessage(Component.text("All animations stopped.", NamedTextColor.GREEN));
                    return 1;
                }))
                .build();
    }

    // ================================================================
    // /ba spread <radius> <duration> <colors> [loopMode] [sweepTicks] [cycleTicks]
    // ================================================================

    private LiteralArgumentBuilder<CommandSourceStack> buildSpread() {
        return Commands.literal("spread")
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                .then(Commands.argument("colors", StringArgumentType.string())
                    .executes(ctx -> runSpread(ctx, LoopMode.RESTART, 40, 60))
                    .then(loopModeArg()
                        .executes(ctx -> runSpread(ctx, getLoopMode(ctx), 40, 60))
                        .then(hintedInt("sweepTicks", 1, 600, 40)
                            .executes(ctx -> runSpread(ctx, getLoopMode(ctx),
                                    IntegerArgumentType.getInteger(ctx, "sweepTicks"), 60))
                            .then(hintedInt("cycleTicks", 1, 600, 60)
                                .executes(ctx -> runSpread(ctx, getLoopMode(ctx),
                                        IntegerArgumentType.getInteger(ctx, "sweepTicks"),
                                        IntegerArgumentType.getInteger(ctx, "cycleTicks")))
                            )
                        )
                    )
                )));
    }

    private int runSpread(CommandContext<CommandSourceStack> ctx,
                          LoopMode loopMode, int sweepTicks, int cycleTicks) {
        Player player = (Player) ctx.getSource().getSender();
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        List<RGBColor> colors = parseHexColors(StringArgumentType.getString(ctx, "colors"));
        if (colors.isEmpty()) return colorError(player);

        player.sendMessage(Component.text("Scanning (r=" + radius + ")…", NamedTextColor.AQUA));
        api.calculateVisibleBlocks(player.getLocation(), radius).thenAccept(blocks -> {
            player.sendMessage(Component.text(blocks.getBlocks().size() + " blocks → spread"
                    + " | loop=" + loopMode + " sweep=" + sweepTicks + "t cycle=" + cycleTicks + "t",
                    NamedTextColor.GREEN));
            api.playAnimation(player, blocks,
                    new SpreadAnimation(sweepTicks, cycleTicks),
                    new GradientPalette(colors), duration, TimeUnit.SECONDS, loopMode);
        });
        return 1;
    }

    // ================================================================
    // /ba passthrough <radius> <duration> <colors> [loopMode] [trail] [yaw] [sweepTicks] [cycleTicks] [bandWidth]
    // ================================================================

    private LiteralArgumentBuilder<CommandSourceStack> buildPassThrough() {
        return Commands.literal("passthrough")
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                .then(Commands.argument("colors", StringArgumentType.string())
                    .executes(ctx -> runPT(ctx, LoopMode.RESTART, false, 0f, 40, 60, 0.3f))
                    .then(loopModeArg()
                        .executes(ctx -> runPT(ctx, getLoopMode(ctx), false, 0f, 40, 60, 0.3f))
                        .then(hintedBool("trail", false)
                            .executes(ctx -> runPT(ctx, getLoopMode(ctx),
                                    BoolArgumentType.getBool(ctx, "trail"), 0f, 40, 60, 0.3f))
                            .then(hintedFloat("yaw", 0, 360, 0f)
                                .executes(ctx -> runPT(ctx, getLoopMode(ctx),
                                        BoolArgumentType.getBool(ctx, "trail"),
                                        FloatArgumentType.getFloat(ctx, "yaw"), 40, 60, 0.3f))
                                .then(hintedInt("sweepTicks", 1, 600, 40)
                                    .executes(ctx -> runPT(ctx, getLoopMode(ctx),
                                            BoolArgumentType.getBool(ctx, "trail"),
                                            FloatArgumentType.getFloat(ctx, "yaw"),
                                            IntegerArgumentType.getInteger(ctx, "sweepTicks"), 60, 0.3f))
                                    .then(hintedInt("cycleTicks", 1, 600, 60)
                                        .executes(ctx -> runPT(ctx, getLoopMode(ctx),
                                                BoolArgumentType.getBool(ctx, "trail"),
                                                FloatArgumentType.getFloat(ctx, "yaw"),
                                                IntegerArgumentType.getInteger(ctx, "sweepTicks"),
                                                IntegerArgumentType.getInteger(ctx, "cycleTicks"), 0.3f))
                                        .then(hintedFloat("bandWidth", 0.05f, 1.0f, 0.3f)
                                            .executes(ctx -> runPT(ctx, getLoopMode(ctx),
                                                    BoolArgumentType.getBool(ctx, "trail"),
                                                    FloatArgumentType.getFloat(ctx, "yaw"),
                                                    IntegerArgumentType.getInteger(ctx, "sweepTicks"),
                                                    IntegerArgumentType.getInteger(ctx, "cycleTicks"),
                                                    FloatArgumentType.getFloat(ctx, "bandWidth")))
                                        )
                                    )
                                )
                            )
                        )
                    )
                )));
    }

    private int runPT(CommandContext<CommandSourceStack> ctx,
                      LoopMode loopMode, boolean trail, float yaw,
                      int sweepTicks, int cycleTicks, float bandWidth) {
        Player player = (Player) ctx.getSource().getSender();
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        List<RGBColor> colors = parseHexColors(StringArgumentType.getString(ctx, "colors"));
        if (colors.isEmpty()) return colorError(player);

        player.sendMessage(Component.text("Scanning (r=" + radius + ")…", NamedTextColor.AQUA));
        api.calculateVisibleBlocks(player.getLocation(), radius).thenAccept(blocks -> {
            player.sendMessage(Component.text(blocks.getBlocks().size() + " blocks → passthrough"
                    + " | loop=" + loopMode + " trail=" + trail + " yaw=" + yaw
                    + " sweep=" + sweepTicks + "t cycle=" + cycleTicks + "t band=" + bandWidth,
                    NamedTextColor.GREEN));
            api.playAnimation(player, blocks,
                    new PassThroughAnimation(yaw, sweepTicks, cycleTicks, bandWidth, trail),
                    new GradientPalette(colors), duration, TimeUnit.SECONDS, loopMode);
        });
        return 1;
    }

    // ================================================================
    // /ba sparkle <radius> <duration> <colors> [loopMode] [density] [cycleTicks]
    // ================================================================

    private LiteralArgumentBuilder<CommandSourceStack> buildSparkle() {
        return Commands.literal("sparkle")
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                .then(Commands.argument("colors", StringArgumentType.string())
                    .executes(ctx -> runSparkle(ctx, LoopMode.RESTART, 0.15f, 60))
                    .then(loopModeArg()
                        .executes(ctx -> runSparkle(ctx, getLoopMode(ctx), 0.15f, 60))
                        .then(hintedFloat("density", 0.01f, 1.0f, 0.15f)
                            .executes(ctx -> runSparkle(ctx, getLoopMode(ctx),
                                    FloatArgumentType.getFloat(ctx, "density"), 60))
                            .then(hintedInt("cycleTicks", 1, 600, 60)
                                .executes(ctx -> runSparkle(ctx, getLoopMode(ctx),
                                        FloatArgumentType.getFloat(ctx, "density"),
                                        IntegerArgumentType.getInteger(ctx, "cycleTicks")))
                            )
                        )
                    )
                )));
    }

    private int runSparkle(CommandContext<CommandSourceStack> ctx,
                            LoopMode loopMode, float density, int cycleTicks) {
        Player player = (Player) ctx.getSource().getSender();
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        List<RGBColor> colors = parseHexColors(StringArgumentType.getString(ctx, "colors"));
        if (colors.isEmpty()) return colorError(player);

        player.sendMessage(Component.text("Scanning (r=" + radius + ")…", NamedTextColor.AQUA));
        api.calculateVisibleBlocks(player.getLocation(), radius).thenAccept(blocks -> {
            player.sendMessage(Component.text(blocks.getBlocks().size() + " blocks → sparkle"
                    + " | loop=" + loopMode + " density=" + density + " cycle=" + cycleTicks + "t",
                    NamedTextColor.GREEN));
            api.playAnimation(player, blocks,
                    new NoiseSparkleAnimation(density, cycleTicks),
                    new GradientPalette(colors), duration, TimeUnit.SECONDS, loopMode);
        });
        return 1;
    }

    // ================================================================
    // Arg builders with default-value hints
    // ================================================================

    /** loopMode arg with tab-completions showing all enum values. */
    private static RequiredArgumentBuilder<CommandSourceStack, String> loopModeArg() {
        return Commands.argument("loopMode", StringArgumentType.word())
                .suggests((ctx, b) -> {
                    for (LoopMode lm : LoopMode.values()) {
                        b.suggest(lm.name().toLowerCase(), () -> "default: restart");
                    }
                    return b.buildFuture();
                });
    }

    /** Integer arg whose suggestion tooltip shows the default value. */
    private static RequiredArgumentBuilder<CommandSourceStack, Integer> hintedInt(
            String name, int min, int max, int defaultVal) {
        return Commands.argument(name, IntegerArgumentType.integer(min, max))
                .suggests((ctx, b) -> {
                    b.suggest(defaultVal, () -> name + " (default: " + defaultVal + ")");
                    return b.buildFuture();
                });
    }

    /** Float arg whose suggestion tooltip shows the default value. */
    private static RequiredArgumentBuilder<CommandSourceStack, Float> hintedFloat(
            String name, float min, float max, float defaultVal) {
        return Commands.argument(name, FloatArgumentType.floatArg(min, max))
                .suggests((ctx, b) -> {
                    b.suggest(String.valueOf(defaultVal), () -> name + " (default: " + defaultVal + ")");
                    return b.buildFuture();
                });
    }

    /** Boolean arg whose suggestion tooltip shows the default value. */
    private static RequiredArgumentBuilder<CommandSourceStack, Boolean> hintedBool(
            String name, boolean defaultVal) {
        return Commands.argument(name, BoolArgumentType.bool())
                .suggests((ctx, b) -> {
                    b.suggest("true", () -> name + " (default: " + defaultVal + ")");
                    b.suggest("false", () -> name + " (default: " + defaultVal + ")");
                    return b.buildFuture();
                });
    }

    // ================================================================
    // Parsing
    // ================================================================

    private static LoopMode getLoopMode(CommandContext<CommandSourceStack> ctx) {
        return parseLoopMode(StringArgumentType.getString(ctx, "loopMode"));
    }

    private static LoopMode parseLoopMode(String input) {
        try {
            return LoopMode.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LoopMode.RESTART;
        }
    }

    /**
     * Parse comma-separated hex colors: {@code "ff0000,00ff00,0000ff"}.
     * Accepts optional {@code #} prefix and 3-char shorthand.
     */
    static List<RGBColor> parseHexColors(String input) {
        List<RGBColor> colors = new ArrayList<>();
        try {
            for (String token : input.split(",")) {
                String hex = token.trim().replaceFirst("^#", "");
                if (hex.length() == 3) {
                    hex = "" + hex.charAt(0) + hex.charAt(0)
                            + hex.charAt(1) + hex.charAt(1)
                            + hex.charAt(2) + hex.charAt(2);
                }
                if (hex.length() != 6) return List.of();
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b4 = Integer.parseInt(hex.substring(4, 6), 16);
                colors.add(new RGBColor(r, g, b4));
            }
        } catch (NumberFormatException e) {
            return List.of();
        }
        return colors.size() >= 2 ? colors : List.of();
    }

    private static int colorError(Player player) {
        player.sendMessage(Component.text("Invalid colors. Use 2+ hex values: ff0000,00ff00,0000ff", NamedTextColor.RED));
        return 0;
    }
}
