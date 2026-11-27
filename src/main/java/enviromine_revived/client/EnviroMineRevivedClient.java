package enviromine_revived.client;

import enviromine_revived.EnviroMineRevived;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client entry point. NeoForge never loads this class on a dedicated server, so client-only
 * classes (rendering, Minecraft.getInstance()) are safe to reference from here.
 */
@Mod(value = EnviroMineRevived.MOD_ID, dist = Dist.CLIENT)
public final class EnviroMineRevivedClient {
    public EnviroMineRevivedClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // NeoForge's generic config screen: Mods → EnviroMine Revived → Config.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(ClimateHud::register);
        NeoForge.EVENT_BUS.addListener(ClimateDebugText::onDebugText);
        NeoForge.EVENT_BUS.addListener(ClientDrinking::onRightClickEmpty);
        NeoForge.EVENT_BUS.addListener(ClientDrinking::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ClientDrinking::onTooltip);
    }
}
