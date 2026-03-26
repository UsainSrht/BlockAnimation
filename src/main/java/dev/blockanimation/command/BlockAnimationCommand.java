package dev.blockanimation.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
 * Brigadier test command for the BlockAnimation plugin.
 * <p>
 * Colors are provided as comma-separated hex strings, e.g. {@code "ff0000,00ff00,0000ff"}.
 *
 * <pre>
 * /ba spread &lt;radius&gt; &lt;duration&gt; &lt;colors&gt; [sweepTicks] [cycleTicks] [loopMode]
 * /ba passthrough &lt;radius&gt; &lt;duration&gt; &lt;colors&gt; [yaw] [sweepTicks] [cycleTicks] [bandWidth] [continuous] [loopMode]
 * /ba sparkle &lt;radius&gt; &lt;duration&gt; &lt;colors&gt; [density] [cycleTicks] [loopMode]
 * /ba stop
 * </pre>
 */
@SuppressWarnings("UnstableApiUsage")
public final class BlockAnimationCommand {

    private final BlockAnimationAPI api;

    public BlockAnimationCommand(BlockAnimationAPI api) {
        this.api = api;
    }

    /**
     * Build the Brigadier command tree.
     */
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

    // ---------------------------------------------------------------
    // /ba spread <radius> <duration> <colors> [sweepTicks] [cycleTicks] [loopMode]
    // ---------------------------------------------------------------

    private LiteralArgumentBuilder<CommandSourceStack> buildSpread() {
        return Commands.literal("spread")
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                .then(Commands.argument("colors", StringArgumentType.string())
                    // defaults only
                    .executes(ctx -> runSpread(ctx, 40, 60, LoopMode.RESTART))
                    // + sweepTicks
                    .then(Commands.argument("sweepTicks", IntegerArgumentType.integer(1, 600))
                        .executes(ctx -> runSpread(ctx,
                                IntegerArgumentType.getInteger(ctx, "sweepTicks"),
                                60, LoopMode.RESTART))
                        // + cycleTicks
                        .then(Commands.argument("cycleTicks", IntegerArgumentType.integer(1, 600))
                            .executes(ctx -> runSpread(ctx,
                                    IntegerArgumentType.getInteger(ctx, "sweepTicks"),
                                    IntegerArgumentType.getInteger(ctx, "cycleTicks"),
                                    LoopMode.RESTART))
                            // + loopMode
                            .then(loopModeArg(ctx2 -> runSpread(ctx2,
                                    IntegerArgumentType.getInteger(ctx2, "sweepTicks"),
                                    IntegerArgumentType.getInteger(ctx2, "cycleTicks"),
                                    parseLoopMode(StringArgumentType.getString(ctx2, "loopMode")))
                            ))
                        )
                    )
                )));
    }

    private int runSpread(CommandContext<CommandSourceStack> ctx,
                          int sweepTicks, int cycleTicks, LoopMode loopMode) {
        Player player = (Player) ctx.getSource().getSender();
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        List<RGBColor> colors = parseHexColors(StringArgumentType.getString(ctx, "colors"));

        if (colors.isEmpty()) {
            player.sendMessage(Component.text("Invalid color format. Use hex: ff0000,00ff00,0000ff", NamedTextColor.RED));
            return 0;
        }

        player.sendMessage(Component.text("Scanning blocks (r=" + radius + ")…", NamedTextColor.AQUA));
        api.calculateVisibleBlocks(player.getLocation(), radius).thenAccept(blocks -> {
            player.sendMessage(Component.text("Found " + blocks.getBlocks().size()
                    + " blocks. Spread: sweep=" + sweepTicks + "t, cycle=" + cycleTicks
                    + "t, loop=" + loopMode, NamedTextColor.GREEN));

            api.playAnimation(player, blocks,
                    new SpreadAnimation(sweepTicks, cycleTicks),
                    new GradientPalette(colors),
                    duration, TimeUnit.SECONDS, loopMode);
        });
        return 1;
    }

    // ---------------------------------------------------------------
    // /ba passthrough <radius> <duration> <colors> [yaw] [sweepTicks] [cycleTicks] [bandWidth] [continuous] [loopMode]
    // ---------------------------------------------------------------

    private LiteralArgumentBuilder<CommandSourceStack> buildPassThrough() {
        return Commands.literal("passthrough")
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                .then(Commands.argument("colors", StringArgumentType.string())
                    // defaults only
                    .executes(ctx -> runPassThrough(ctx, 0f, 40, 60, 0.3f, true, LoopMode.RESTART))
                    // + yaw
                    .then(Commands.argument("yaw", FloatArgumentType.floatArg(0, 360))
                        .executes(ctx -> runPassThrough(ctx,
                                FloatArgumentType.getFloat(ctx, "yaw"),
                                40, 60, 0.3f, true, LoopMode.RESTART))
                        // + sweepTicks
                        .then(Commands.argument("sweepTicks", IntegerArgumentType.integer(1, 600))
                            .executes(ctx -> runPassThrough(ctx,
                                    FloatArgumentType.getFloat(ctx, "yaw"),
                                    IntegerArgumentType.getInteger(ctx, "sweepTicks"),
                                    60, 0.3f, true, LoopMode.RESTART))
                            // + cycleTicks
                            .then(Commands.argument("cycleTicks", IntegerArgumentType.integer(1, 600))
                                .executes(ctx -> runPassThrough(ctx,
                                        FloatArgumentType.getFloat(ctx, "yaw"),
                                        IntegerArgumentType.getInteger(ctx, "sweepTicks"),
                                        IntegerArgumentType.getInteger(ctx, "cycleTicks"),
                                        0.3f, true, LoopMode.RESTART))
                                // + bandWidth
                                .then(Commands.argument("bandWidth", FloatArgumentType.floatArg(0.05f, 1.0f))
                                    .executes(ctx -> runPassThrough(ctx,
                                            FloatArgumentType.getFloat(ctx, "yaw"),
                                            IntegerArgumentType.getInteger(ctx, "sweepTicks"),
                                            IntegerArgumentType.getInteger(ctx, "cycleTicks"),
                                            FloatArgumentType.getFloat(ctx, "bandWidth"),
                                            true, LoopMode.RESTART))
                                    // + continuous
                                    .then(Commands.argument("continuous", BoolArgumentType.bool())
                                        .executes(ctx -> runPassThrough(ctx,
                                                FloatArgumentType.getFloat(ctx, "yaw"),
                                                IntegerArgumentType.getInteger(ctx, "sweepTicks"),
                                                IntegerArgumentType.getInteger(ctx, "cycleTicks"),
                                                FloatArgumentType.getFloat(ctx, "bandWidth"),
                                                BoolArgumentType.getBool(ctx, "continuous"),
                                                LoopMode.RESTART))
                                        // + loopMode
                                        .then(loopModeArg(ctx2 -> runPassThrough(ctx2,
                                                FloatArgumentType.getFloat(ctx2, "yaw"),
                                                IntegerArgumentType.getInteger(ctx2, "sweepTicks"),
                                                IntegerArgumentType.getInteger(ctx2, "cycleTicks"),
                                                FloatArgumentType.getFloat(ctx2, "bandWidth"),
                                                BoolArgumentType.getBool(ctx2, "continuous"),
                                                parseLoopMode(StringArgumentType.getString(ctx2, "loopMode")))
                                        ))
                                    )
                                )
                            )
                        )
                    )
                )));
    }

    private int runPassThrough(CommandContext<CommandSourceStack> ctx,
                                float yaw, int sweepTicks, int cycleTicks,
                                float bandWidth, boolean continuous, LoopMode loopMode) {
        Player player = (Player) ctx.getSource().getSender();
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        List<RGBColor> colors = parseHexColors(StringArgumentType.getString(ctx, "colors"));

        if (colors.isEmpty()) {
            player.sendMessage(Component.text("Invalid color format. Use hex: ff0000,00ff00,0000ff", NamedTextColor.RED));
            return 0;
        }

        player.sendMessage(Component.text("Scanning blocks (r=" + radius + ")…", NamedTextColor.AQUA));
        api.calculateVisibleBlocks(player.getLocation(), radius).thenAccept(blocks -> {
            player.sendMessage(Component.text("Found " + blocks.getBlocks().size()
                    + " blocks. PassThrough: yaw=" + yaw + ", sweep=" + sweepTicks
                    + "t, cycle=" + cycleTicks + "t, band=" + bandWidth
                    + ", cont=" + continuous + ", loop=" + loopMode, NamedTextColor.GREEN));

            api.playAnimation(player, blocks,
                    new PassThroughAnimation(yaw, sweepTicks, cycleTicks, bandWidth, continuous),
                    new GradientPalette(colors),
                    duration, TimeUnit.SECONDS, loopMode);
        });
        return 1;
    }

    // ---------------------------------------------------------------
    // /ba sparkle <radius> <duration> <colors> [density] [cycleTicks] [loopMode]
    // ---------------------------------------------------------------

    private LiteralArgumentBuilder<CommandSourceStack> buildSparkle() {
        return Commands.literal("sparkle")
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                .then(Commands.argument("colors", StringArgumentType.string())
                    // defaults only
                    .executes(ctx -> runSparkle(ctx, 0.15f, 60, LoopMode.RESTART))
                    // + density
                    .then(Commands.argument("density", FloatArgumentType.floatArg(0.01f, 1.0f))
                        .executes(ctx -> runSparkle(ctx,
                                FloatArgumentType.getFloat(ctx, "density"),
                                60, LoopMode.RESTART))
                        // + cycleTicks
                        .then(Commands.argument("cycleTicks", IntegerArgumentType.integer(1, 600))
                            .executes(ctx -> runSparkle(ctx,
                                    FloatArgumentType.getFloat(ctx, "density"),
                                    IntegerArgumentType.getInteger(ctx, "cycleTicks"),
                                    LoopMode.RESTART))
                            // + loopMode
                            .then(loopModeArg(ctx2 -> runSparkle(ctx2,
                                    FloatArgumentType.getFloat(ctx2, "density"),
                                    IntegerArgumentType.getInteger(ctx2, "cycleTicks"),
                                    parseLoopMode(StringArgumentType.getString(ctx2, "loopMode")))
                            ))
                        )
                    )
                )));
    }

    private int runSparkle(CommandContext<CommandSourceStack> ctx,
                            float density, int cycleTicks, LoopMode loopMode) {
        Player player = (Player) ctx.getSource().getSender();
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int duration = IntegerArgumentType.getInteger(ctx, "duration");
        List<RGBColor> colors = parseHexColors(StringArgumentType.getString(ctx, "colors"));

        if (colors.isEmpty()) {
            player.sendMessage(Component.text("Invalid color format. Use hex: ff0000,00ff00,0000ff", NamedTextColor.RED));
            return 0;
        }

        player.sendMessage(Component.text("Scanning blocks (r=" + radius + ")…", NamedTextColor.AQUA));
        api.calculateVisibleBlocks(player.getLocation(), radius).thenAccept(blocks -> {
            player.sendMessage(Component.text("Found " + blocks.getBlocks().size()
                    + " blocks. Sparkle: density=" + density
                    + ", cycle=" + cycleTicks + "t, loop=" + loopMode, NamedTextColor.GREEN));

            api.playAnimation(player, blocks,
                    new NoiseSparkleAnimation(density, cycleTicks),
                    new GradientPalette(colors),
                    duration, TimeUnit.SECONDS, loopMode);
        });
        return 1;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /**
     * Builds a loopMode string argument with tab-complete suggestions.
     */
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String>
    loopModeArg(com.mojang.brigadier.Command<CommandSourceStack> executor) {
        return Commands.argument("loopMode", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    for (LoopMode lm : LoopMode.values()) {
                        builder.suggest(lm.name().toLowerCase());
                    }
                    return builder.buildFuture();
                })
                .executes(executor);
    }

    /**
     * Parse a comma-separated hex color string into a list of {@link RGBColor}.
     * <p>Accepts formats: {@code "ff0000,00ff00,0000ff"} or {@code "#ff0000,#00ff00"}.
     *
     * @param input the color string
     * @return list of parsed colors, or empty list on failure
     */
    static List<RGBColor> parseHexColors(String input) {
        List<RGBColor> colors = new ArrayList<>();
        try {
            for (String token : input.split(",")) {
                String hex = token.trim().replaceFirst("^#", "");
                if (hex.length() == 3) {
                    // Shorthand: "f0a" → "ff00aa"
                    hex = "" + hex.charAt(0) + hex.charAt(0)
                            + hex.charAt(1) + hex.charAt(1)
                            + hex.charAt(2) + hex.charAt(2);
                }
                if (hex.length() != 6) return List.of();
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                colors.add(new RGBColor(r, g, b));
            }
        } catch (NumberFormatException e) {
            return List.of();
        }
        return colors.size() >= 2 ? colors : List.of();
    }

    private static LoopMode parseLoopMode(String input) {
        try {
            return LoopMode.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LoopMode.RESTART;
        }
    }
}
