package enviromine_revived;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import enviromine_revived.data.ModAttachments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

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

    public static final Logger LOGGER = LogUtils.getLogger();

    public EnviroMineRevived(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(ClimateTicker::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ClimateCommand::register);
        // Client-only set-up lives in client.EnviroMineRevivedClient.
    }
}
