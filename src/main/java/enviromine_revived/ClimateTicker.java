package enviromine_revived;

import enviromine_revived.config.Settings;
import enviromine_revived.data.ClimateData;
import enviromine_revived.data.ModAttachments;
import enviromine_revived.model.Consequences;
import enviromine_revived.model.HydrationModel;
import enviromine_revived.model.TemperatureModel;
import enviromine_revived.registry.ModDamageTypes;
import enviromine_revived.registry.ModEffects;
import enviromine_revived.world.EnvironmentSampler;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Runs the climate model for every player on the server. The server is the only side that
 * changes stats; the client just displays the synced attachment. All numbers come from
 * {@link Settings} (the server config).
 */
public final class ClimateTicker {
    private ClimateTicker() {}

    /** Ticks between model updates. The model's rates are "per update", tuned for 30 (1.5 s). */
    public static final int UPDATE_INTERVAL = 30;

    /** Effects last a little longer than one update, so they stay on while the condition lasts. */
    private static final int EFFECT_TICKS = UPDATE_INTERVAL * 2;

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
        Settings.Snapshot settings = Settings.get();
        ClimateData before = player.getData(ModAttachments.CLIMATE);
        // Like the original: in creative and spectator the stats are frozen, but the ambient keeps
        // updating so the HUD still shows what a place feels like.
        boolean frozen = player.isCreative() || player.isSpectator();
        ClimateData after = next(player, before, frozen, settings);
        player.setData(ModAttachments.CLIMATE, after);
        if (!frozen) {
            applyConsequences(player, before, after, settings);
        }
    }

    /** The stats after one update, without storing them. Separate so GameTests can call it. */
    public static ClimateData next(ServerPlayer player, ClimateData data, boolean frozen, Settings.Snapshot settings) {
        TemperatureModel temperature = settings.temperature();
        HydrationModel hydration = settings.hydration();
        EnvironmentSampler.Sample sample = EnvironmentSampler.sample(player, temperature);
        TemperatureModel.Ambient ambient = temperature.ambient(sample.environment(), data.bodyTemp());
        if (frozen) {
            return data.withAmbient(ambient.ambient());
        }

        double body = settings.features().temperature()
                ? temperature.drift(data.bodyTemp(), ambient)
                : temperature.tuning().normalBodyTemp();
        double water = hydration.tuning().max();
        if (settings.features().hydration()) {
            HydrationModel.Step step = hydration.step(data.hydration(), body, withThirstEffect(player, sample.activity(), settings));
            water = step.hydration();
            if (settings.features().temperature()) {
                body += step.bodyTempChange(); // sweat cooling
            }
        }
        return new ClimateData(body, water, ambient.ambient());
    }

    /** The Thirst effect from salt water makes hydration drain faster while it lasts. */
    private static HydrationModel.Activity withThirstEffect(ServerPlayer player, HydrationModel.Activity a,
                                                            Settings.Snapshot settings) {
        MobEffectInstance thirst = player.getEffect(ModEffects.THIRST);
        if (thirst == null) {
            return a;
        }
        double extra = settings.hydration().tuning().thirstEffectDrain() * (thirst.getAmplifier() + 1);
        return new HydrationModel.Activity(a.sprinting(), a.nearLava(), a.aridDaytime(), a.hungerEffect(),
                a.extraDrain() + extra);
    }

    /** Warnings, effects and damage for the new stats. See {@link Consequences} for the rules. */
    public static void applyConsequences(ServerPlayer player, ClimateData before, ClimateData after,
                                         Settings.Snapshot settings) {
        int oldTemp = settings.temperatureLevel(before.bodyTemp());
        int newTemp = settings.temperatureLevel(after.bodyTemp());
        int oldThirst = settings.thirstLevel(before.hydration());
        int newThirst = settings.thirstLevel(after.hydration());

        // Telegraph: one action-bar message whenever things get worse (temperature first).
        if (Consequences.worsened(oldTemp, newTemp)) {
            String key = (newTemp > 0 ? "hot." : "cold.") + Math.abs(newTemp);
            player.displayClientMessage(Component.translatable("enviromine_revived.warning." + key), true);
        } else if (Consequences.worsened(oldThirst, newThirst)) {
            player.displayClientMessage(Component.translatable("enviromine_revived.warning.thirst." + newThirst), true);
        }

        if (!settings.features().effects()) {
            return;
        }
        boolean fireResistant = player.hasEffect(MobEffects.FIRE_RESISTANCE);
        if (fireResistant && player.hasEffect(ModEffects.HEATSTROKE)) {
            player.removeEffect(ModEffects.HEATSTROKE);
        }
        Consequences.Outcome outcome = Consequences.decide(newTemp, newThirst, fireResistant);
        refresh(player, ModEffects.HEATSTROKE, outcome.heatstrokeAmplifier());
        refresh(player, ModEffects.HYPOTHERMIA, outcome.hypothermiaAmplifier());
        refresh(player, ModEffects.DEHYDRATION, outcome.dehydrationAmplifier());

        if (settings.features().damage()
                && outcome.damage() != Consequences.Hazard.NONE
                && player.tickCount % Consequences.DAMAGE_INTERVAL_TICKS == 0
                && Consequences.damageAllowed(player.level().getDifficulty().getId(), player.getHealth())) {
            player.hurt(player.damageSources().source(ModDamageTypes.forHazard(outcome.damage())), Consequences.DAMAGE);
        }
    }

    private static void refresh(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        if (amplifier >= 0) {
            // ambient = true: shown like a beacon effect, because it comes from the surroundings.
            player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, amplifier, true, false, true));
        }
    }
}
