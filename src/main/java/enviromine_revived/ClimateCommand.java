package enviromine_revived;

import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import enviromine_revived.data.ClimateData;
import enviromine_revived.data.ModAttachments;
import enviromine_revived.model.Severity;
import enviromine_revived.model.TemperatureTuning;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Operator command for testing and admin use (permission level 2, like /effect):
 *
 * <pre>
 * /enviromine_revived show [player]
 * /enviromine_revived set &lt;player&gt; body_temp &lt;20..50&gt;
 * /enviromine_revived set &lt;player&gt; hydration &lt;0..100&gt;
 * /enviromine_revived reset &lt;player&gt;
 * </pre>
 */
public final class ClimateCommand {
    private ClimateCommand() {}

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        TemperatureTuning tuning = ClimateTicker.TEMPERATURE.tuning();
        dispatcher.register(Commands.literal(EnviroMineRevived.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("show")
                        .executes(c -> show(c, c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> show(c, EntityArgument.getPlayer(c, "player")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.literal("body_temp")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(
                                                        tuning.minBodyTemp(), tuning.maxBodyTemp()))
                                                .executes(c -> setBodyTemp(c))))
                                .then(Commands.literal("hydration")
                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(
                                                        0, ClimateTicker.HYDRATION.tuning().max()))
                                                .executes(c -> setHydration(c))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> reset(c)))));
    }

    private static int show(CommandContext<CommandSourceStack> c, ServerPlayer player) {
        ClimateData d = player.getData(ModAttachments.CLIMATE);
        int heat = Severity.temperature(d.bodyTemp(), Severity.TemperatureThresholds.defaults());
        int thirst = Severity.hydration(d.hydration(), Severity.HydrationThresholds.defaults());
        double target = d.ambient() + ClimateTicker.TEMPERATURE.tuning().comfortOffset();
        c.getSource().sendSuccess(() -> Component.translatable("commands.enviromine_revived.show",
                player.getDisplayName(),
                format(d.bodyTemp(), 2), format(target, 1), format(d.ambient(), 1), heat,
                format(d.hydration(), 1), thirst), false);
        return 1;
    }

    private static int setBodyTemp(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        double value = DoubleArgumentType.getDouble(c, "value");
        player.setData(ModAttachments.CLIMATE, player.getData(ModAttachments.CLIMATE).withBodyTemp(value));
        return show(c, player);
    }

    private static int setHydration(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        double value = DoubleArgumentType.getDouble(c, "value");
        player.setData(ModAttachments.CLIMATE, player.getData(ModAttachments.CLIMATE).withHydration(value));
        return show(c, player);
    }

    private static int reset(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        player.setData(ModAttachments.CLIMATE, ClimateData.DEFAULT);
        ClimateTicker.update(player); // refresh the ambient value straight away
        return show(c, player);
    }

    private static String format(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }
}
