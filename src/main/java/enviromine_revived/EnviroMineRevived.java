package enviromine_revived;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Entry point. NeoForge constructs this class once when the mod loads.
 *
 * M0 only proves the mod loads. Registries, config, events and networking are added in later
 * milestones (see docs/PORTING_MAP.md).
 */
@Mod(EnviroMineRevived.MOD_ID)
public final class EnviroMineRevived {
    /** Must match mod_id in gradle.properties. */
    public static final String MOD_ID = "enviromine_revived";

    public static final Logger LOGGER = LogUtils.getLogger();

    public EnviroMineRevived(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("{} loading (temperature and hydration, based on EnviroMine)", MOD_ID);
    }
}
