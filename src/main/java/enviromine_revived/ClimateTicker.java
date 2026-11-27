package enviromine_revived;

import enviromine_revived.data.ClimateData;
import enviromine_revived.data.ModAttachments;
import enviromine_revived.model.HydrationModel;
import enviromine_revived.model.HydrationTuning;
import enviromine_revived.model.TemperatureModel;
import enviromine_revived.model.TemperatureTuning;
import enviromine_revived.world.EnvironmentSampler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Runs the climate model for every player on the server. The server is the only side that
 * changes stats; the client just displays the synced attachment.
 */
public final class ClimateTicker {
    private ClimateTicker() {}

    /** Ticks between model updates. The model's rates are "per update", tuned for 30 (1.5 s). */
    public static final int UPDATE_INTERVAL = 30;

    // M5 replaces these defaults with values from the server config.
    public static final TemperatureModel TEMPERATURE = new TemperatureModel(TemperatureTuning.defaults());
    public static final HydrationModel HYDRATION = new HydrationModel(HydrationTuning.defaults());

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return; // client side: nothing to do
        }
        // tickCount differs per player, so updates are spread over ticks instead of all at once.
        if (player.tickCount % UPDATE_INTERVAL != 0 || !player.isAlive()) {
            return;
        }
        update(player);
    }

    /** One model update for one player. Public so the debug command can force one. */
    public static void update(ServerPlayer player) {
        ClimateData data = player.getData(ModAttachments.CLIMATE);
        // Like the original: in creative and spectator the stats are frozen, but the ambient keeps
        // updating so the HUD still shows what a place feels like.
        boolean frozen = player.isCreative() || player.isSpectator();
        player.setData(ModAttachments.CLIMATE, next(player, data, frozen));
    }

    /** The stats after one update, without storing them. Separate so GameTests can call it. */
    public static ClimateData next(ServerPlayer player, ClimateData data, boolean frozen) {
        EnvironmentSampler.Sample sample = EnvironmentSampler.sample(player, TEMPERATURE);
        TemperatureModel.Ambient ambient = TEMPERATURE.ambient(sample.environment(), data.bodyTemp());
        if (frozen) {
            return data.withAmbient(ambient.ambient());
        }

        double body = TEMPERATURE.drift(data.bodyTemp(), ambient);
        HydrationModel.Step thirst = HYDRATION.step(data.hydration(), body, sample.activity());
        body += thirst.bodyTempChange();
        return new ClimateData(body, thirst.hydration(), ambient.ambient());
    }
}
