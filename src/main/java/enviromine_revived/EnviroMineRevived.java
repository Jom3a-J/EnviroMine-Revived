package enviromine_revived;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import enviromine_revived.config.ServerConfig;
import enviromine_revived.config.Settings;
import enviromine_revived.data.ModAttachments;
import enviromine_revived.network.DrinkPayload;
import enviromine_revived.registry.ModComponents;
import enviromine_revived.registry.ModDataMaps;
import enviromine_revived.registry.ModEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
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
        modEventBus.addListener(ModDataMaps::register);

        // Per-world gameplay settings, synced to clients. Reloaded live when the file changes.
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        modEventBus.addListener(ModConfigEvent.Loading.class, Settings::onConfigEvent);
        modEventBus.addListener(ModConfigEvent.Reloading.class, Settings::onConfigEvent);
        modEventBus.addListener(ModConfigEvent.Unloading.class, Settings::onConfigEvent);

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
