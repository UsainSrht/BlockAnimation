package dev.blockanimation.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Brigadier test command for the BlockAnimation plugin.
 * <p>
 * Usage:
 * <pre>
 * /blockanimation spread &lt;radius&gt; &lt;duration&gt; &lt;r1&gt; &lt;g1&gt; &lt;b1&gt; &lt;r2&gt; &lt;g2&gt; &lt;b2&gt; [loopMode]
 * /blockanimation passthrough &lt;radius&gt; &lt;duration&gt; &lt;yaw&gt; &lt;r1&gt; &lt;g1&gt; &lt;b1&gt; &lt;r2&gt; &lt;g2&gt; &lt;b2&gt; [loopMode]
 * /blockanimation sparkle &lt;radius&gt; &lt;duration&gt; &lt;density&gt; &lt;r1&gt; &lt;g1&gt; &lt;b1&gt; &lt;r2&gt; &lt;g2&gt; &lt;b2&gt;
 * /blockanimation stop
 * </pre>
 */
@SuppressWarnings("UnstableApiUsage") // Paper command API is still experimental
public final class BlockAnimationCommand {

    private final BlockAnimationAPI api;

    public BlockAnimationCommand(BlockAnimationAPI api) {
        this.api = api;
    }

    /**
     * Build the Brigadier command tree.
     *
     * @return the root command node
     */
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return Commands.literal("blockanimation")
                .requires(src -> src.getSender() instanceof Player
                        && src.getSender().hasPermission("blockanimation.test"))

                // /blockanimation spread <radius> <duration> <r1> <g1> <b1> <r2> <g2> <b2> [loopMode]
                .then(Commands.literal("spread")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                        .then(Commands.argument("r1", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("g1", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("b1", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("r2", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("g2", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("b2", IntegerArgumentType.integer(0, 255))
                        .executes(ctx -> executeSpread(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "radius"),
                                IntegerArgumentType.getInteger(ctx, "duration"),
                                IntegerArgumentType.getInteger(ctx, "r1"),
                                IntegerArgumentType.getInteger(ctx, "g1"),
                                IntegerArgumentType.getInteger(ctx, "b1"),
                                IntegerArgumentType.getInteger(ctx, "r2"),
                                IntegerArgumentType.getInteger(ctx, "g2"),
                                IntegerArgumentType.getInteger(ctx, "b2"),
                                LoopMode.RESTART))
                        .then(Commands.argument("loopMode", StringArgumentType.word())
                        .suggests((ctx2, builder) -> {
                            for (LoopMode lm : LoopMode.values()) {
                                builder.suggest(lm.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> executeSpread(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "radius"),
                                IntegerArgumentType.getInteger(ctx, "duration"),
                                IntegerArgumentType.getInteger(ctx, "r1"),
                                IntegerArgumentType.getInteger(ctx, "g1"),
                                IntegerArgumentType.getInteger(ctx, "b1"),
                                IntegerArgumentType.getInteger(ctx, "r2"),
                                IntegerArgumentType.getInteger(ctx, "g2"),
                                IntegerArgumentType.getInteger(ctx, "b2"),
                                parseLoopMode(StringArgumentType.getString(ctx, "loopMode"))
                        )))
                )))))))))

                // /blockanimation passthrough <radius> <duration> <yaw> <r1..b2> [loopMode]
                .then(Commands.literal("passthrough")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                        .then(Commands.argument("yaw", FloatArgumentType.floatArg(0, 360))
                        .then(Commands.argument("r1", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("g1", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("b1", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("r2", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("g2", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("b2", IntegerArgumentType.integer(0, 255))
                        .executes(ctx -> executePassThrough(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "radius"),
                                IntegerArgumentType.getInteger(ctx, "duration"),
                                FloatArgumentType.getFloat(ctx, "yaw"),
                                IntegerArgumentType.getInteger(ctx, "r1"),
                                IntegerArgumentType.getInteger(ctx, "g1"),
                                IntegerArgumentType.getInteger(ctx, "b1"),
                                IntegerArgumentType.getInteger(ctx, "r2"),
                                IntegerArgumentType.getInteger(ctx, "g2"),
                                IntegerArgumentType.getInteger(ctx, "b2"),
                                LoopMode.RESTART))
                        .then(Commands.argument("loopMode", StringArgumentType.word())
                        .suggests((ctx2, builder) -> {
                            for (LoopMode lm : LoopMode.values()) {
                                builder.suggest(lm.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> executePassThrough(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "radius"),
                                IntegerArgumentType.getInteger(ctx, "duration"),
                                FloatArgumentType.getFloat(ctx, "yaw"),
                                IntegerArgumentType.getInteger(ctx, "r1"),
                                IntegerArgumentType.getInteger(ctx, "g1"),
                                IntegerArgumentType.getInteger(ctx, "b1"),
                                IntegerArgumentType.getInteger(ctx, "r2"),
                                IntegerArgumentType.getInteger(ctx, "g2"),
                                IntegerArgumentType.getInteger(ctx, "b2"),
                                parseLoopMode(StringArgumentType.getString(ctx, "loopMode"))
                        )))
                ))))))))))

                // /blockanimation sparkle <radius> <duration> <density> <r1..b2>
                .then(Commands.literal("sparkle")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                        .then(Commands.argument("density", FloatArgumentType.floatArg(0.01f, 1.0f))
                        .then(Commands.argument("r1", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("g1", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("b1", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("r2", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("g2", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("b2", IntegerArgumentType.integer(0, 255))
                        .executes(ctx -> executeSparkle(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "radius"),
                                IntegerArgumentType.getInteger(ctx, "duration"),
                                FloatArgumentType.getFloat(ctx, "density"),
                                IntegerArgumentType.getInteger(ctx, "r1"),
                                IntegerArgumentType.getInteger(ctx, "g1"),
                                IntegerArgumentType.getInteger(ctx, "b1"),
                                IntegerArgumentType.getInteger(ctx, "r2"),
                                IntegerArgumentType.getInteger(ctx, "g2"),
                                IntegerArgumentType.getInteger(ctx, "b2")
                        ))
                ))))))))))

                // /blockanimation stop
                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            Player player = (Player) ctx.getSource().getSender();
                            api.stopAnimations(player);
                            player.sendMessage(Component.text("All animations stopped.", NamedTextColor.GREEN));
                            return 1;
                        })
                )
                .build();
    }

    // ---------------------------------------------------------------
    // Executors
    // ---------------------------------------------------------------

    private int executeSpread(CommandSourceStack source, int radius, int duration,
                              int r1, int g1, int b1, int r2, int g2, int b2,
                              LoopMode loopMode) {
        Player player = (Player) source.getSender();
        player.sendMessage(Component.text("Scanning blocks (radius " + radius + ")...", NamedTextColor.AQUA));

        api.calculateVisibleBlocks(player.getLocation(), radius).thenAccept(blocks -> {
            player.sendMessage(Component.text("Found " + blocks.getBlocks().size() + " blocks. Starting spread animation...", NamedTextColor.GREEN));

            GradientPalette palette = new GradientPalette(List.of(
                    new RGBColor(r1, g1, b1),
                    new RGBColor(r2, g2, b2)
            ));

            api.playAnimation(player, blocks, new SpreadAnimation(40, 60),
                    palette, duration, TimeUnit.SECONDS, loopMode);
        });

        return 1;
    }

    private int executePassThrough(CommandSourceStack source, int radius, int duration,
                                    float yaw, int r1, int g1, int b1,
                                    int r2, int g2, int b2, LoopMode loopMode) {
        Player player = (Player) source.getSender();
        player.sendMessage(Component.text("Scanning blocks (radius " + radius + ")...", NamedTextColor.AQUA));

        api.calculateVisibleBlocks(player.getLocation(), radius).thenAccept(blocks -> {
            player.sendMessage(Component.text("Found " + blocks.getBlocks().size() + " blocks. Starting pass-through animation...", NamedTextColor.GREEN));

            GradientPalette palette = new GradientPalette(List.of(
                    new RGBColor(r1, g1, b1),
                    new RGBColor(r2, g2, b2)
            ));

            api.playAnimation(player, blocks, new PassThroughAnimation(yaw, 40, 0.3f, true),
                    palette, duration, TimeUnit.SECONDS, loopMode);
        });

        return 1;
    }

    private int executeSparkle(CommandSourceStack source, int radius, int duration,
                                float density, int r1, int g1, int b1,
                                int r2, int g2, int b2) {
        Player player = (Player) source.getSender();
        player.sendMessage(Component.text("Scanning blocks (radius " + radius + ")...", NamedTextColor.AQUA));

        api.calculateVisibleBlocks(player.getLocation(), radius).thenAccept(blocks -> {
            player.sendMessage(Component.text("Found " + blocks.getBlocks().size() + " blocks. Starting sparkle animation...", NamedTextColor.GREEN));

            GradientPalette palette = new GradientPalette(List.of(
                    new RGBColor(r1, g1, b1),
                    new RGBColor(r2, g2, b2)
            ));

            api.playAnimation(player, blocks, new NoiseSparkleAnimation(density),
                    palette, duration, TimeUnit.SECONDS, LoopMode.RESTART);
        });

        return 1;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static LoopMode parseLoopMode(String input) {
        try {
            return LoopMode.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return LoopMode.RESTART;
        }
    }
}
