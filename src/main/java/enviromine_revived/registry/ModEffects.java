package enviromine_revived.registry;

import enviromine_revived.EnviroMineRevived;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Status effects. Heatstroke, Hypothermia and Dehydration are applied by ClimateTicker from
 * severity level 2 (amplifier 0) and level 3 (amplifier 1); their penalties come from attribute
 * modifiers, which scale with the level. Thirst comes from salt water and only speeds up thirst.
 *
 * <p>Penalties are deliberately mild (vanilla Slowness I is -15 % speed, Weakness I is -4 damage).
 */
public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, EnviroMineRevived.MOD_ID);

    /** Per level: -15 % attack damage, -5 % speed. */
    public static final DeferredHolder<MobEffect, MobEffect> HEATSTROKE = EFFECTS.register("heatstroke",
            () -> new ClimateEffect(0xE8573C)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, id("heatstroke_attack"), -0.15,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("heatstroke_speed"), -0.05,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Per level: -10 % speed, -15 % mining speed (stiff, cold hands). */
    public static final DeferredHolder<MobEffect, MobEffect> HYPOTHERMIA = EFFECTS.register("hypothermia",
            () -> new ClimateEffect(0x7FB2F0)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("hypothermia_speed"), -0.10,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, id("hypothermia_mining"), -0.15,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** Per level: -5 % speed, -10 % attack damage. */
    public static final DeferredHolder<MobEffect, MobEffect> DEHYDRATION = EFFECTS.register("dehydration",
            () -> new ClimateEffect(0xC9A05A)
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("dehydration_speed"), -0.05,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.ATTACK_DAMAGE, id("dehydration_attack"), -0.10,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /** From salt water: hydration drains faster while it lasts (see HydrationTuning.thirstEffectDrain). */
    public static final DeferredHolder<MobEffect, MobEffect> THIRST = EFFECTS.register("thirst",
            () -> new ClimateEffect(0xD8D8C0));

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EnviroMineRevived.MOD_ID, "effect." + path);
    }

    /** MobEffect's constructor is protected, so effects need a subclass. They do nothing per tick. */
    static final class ClimateEffect extends MobEffect {
        ClimateEffect(int colour) {
            super(MobEffectCategory.HARMFUL, colour);
        }
    }
}
