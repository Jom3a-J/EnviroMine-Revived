package enviromine_revived;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import enviromine_revived.data.ModAttachments;
import enviromine_revived.network.DrinkPayload;
import enviromine_revived.registry.ModComponents;
import enviromine_revived.registry.ModEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Entry point. NeoForge constructs this class once when the mod loads.
 *
 * <p>Two event buses: the mod bus (registration, set-up) and NeoForge.EVENT_BUS (game events
 * such as ticks and commands).
 */
@Mod(EnviroMineRevived.MOD_ID)
public final class EnviroMineRevived {
    /** Must match mod_id in gradle.properties. */
    public static final String MOD_ID = "enviromine_revived";

    /** Bump when a network payload changes, so mismatched client and server refuse to connect. */
    public static final String NETWORK_VERSION = "1";

    public static final Logger LOGGER = LogUtils.getLogger();

    public EnviroMineRevived(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);
        modEventBus.addListener(EnviroMineRevived::registerPayloads);

        NeoForge.EVENT_BUS.addListener(ClimateTicker::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ClimateCommand::register);
        NeoForge.EVENT_BUS.addListener(Drinking::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(Drinking::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(Drinking::onUseItemFinish);
        // Client-only set-up lives in client.EnviroMineRevivedClient.
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION)
                .playToServer(DrinkPayload.TYPE, DrinkPayload.STREAM_CODEC, Drinking::onDrinkPayload);
    }
}
