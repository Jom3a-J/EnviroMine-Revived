package enviromine_revived.registry;

import enviromine_revived.EnviroMineRevived;
import enviromine_revived.model.Consequences;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * Damage types, defined as data in data/enviromine_revived/damage_type/*.json. Like starvation
 * they bypass armour and Resistance and cause no knockback (see the minecraft damage_type tags).
 * Death messages are in the lang file under death.attack.enviromine_revived.*.
 */
public final class ModDamageTypes {
    private ModDamageTypes() {}

    public static final ResourceKey<DamageType> HEATSTROKE = key("heatstroke");
    public static final ResourceKey<DamageType> HYPOTHERMIA = key("hypothermia");
    public static final ResourceKey<DamageType> DEHYDRATION = key("dehydration");

    public static ResourceKey<DamageType> forHazard(Consequences.Hazard hazard) {
        return switch (hazard) {
            case HEAT -> HEATSTROKE;
            case COLD -> HYPOTHERMIA;
            case THIRST -> DEHYDRATION;
            case NONE -> throw new IllegalArgumentException("no damage type for NONE");
        };
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(EnviroMineRevived.MOD_ID, path));
    }
}
