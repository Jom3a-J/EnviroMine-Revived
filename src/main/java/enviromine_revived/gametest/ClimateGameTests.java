package enviromine_revived.gametest;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import enviromine_revived.ClimateTicker;
import enviromine_revived.Drinking;
import enviromine_revived.EnviroMineRevived;
import enviromine_revived.config.Settings;
import enviromine_revived.data.ClimateData;
import enviromine_revived.data.ModAttachments;
import enviromine_revived.model.WaterType;
import enviromine_revived.registry.ModEffects;
import enviromine_revived.model.ArmorModifier;
import enviromine_revived.world.DataModifiers;
import enviromine_revived.world.EnvironmentSampler;
import enviromine_revived.world.WaterSources;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In-world tests of the Minecraft side (sampling, storage, the tick logic), run headless with
 * {@code gradlew runGameTestServer}. They only load when the game is started with GameTests
 * enabled for this mod, so they never run in normal play.
 *
 * <p>The template "empty" is an empty 7×5×7 structure in data/enviromine_revived/structure.
 */
@GameTestHolder(EnviroMineRevived.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ClimateGameTests {
    private ClimateGameTests() {}

    /** A test player standing at a position relative to the test structure. */
    private static ServerPlayer playerAt(GameTestHelper helper, double x, double y, double z) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(x, y, z));
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }

    private static void removePlayer(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    @GameTest(template = "empty")
    public static void lavaNearbyHeatsAndDrainsFaster(GameTestHelper helper) {
        helper.setBlock(5, 1, 3, Blocks.LAVA);
        ServerPlayer player = playerAt(helper, 2.5, 1, 3.5);

        EnvironmentSampler.Sample sample = EnvironmentSampler.sample(player, Settings.get().temperature());
        helper.assertTrue(sample.environment().nearLava(), "lava 3 blocks away was not detected");
        helper.assertTrue(sample.environment().heatSourceTemp() > 100,
                "heat source " + sample.environment().heatSourceTemp());

        ClimateData next = ClimateTicker.next(player, ClimateData.DEFAULT, false, Settings.get());
        double idleDrain = Settings.get().hydration().tuning().baseDrain();
        helper.assertTrue(next.bodyTemp() > ClimateData.DEFAULT.bodyTemp(), "body did not warm: " + next);
        helper.assertTrue(next.ambient() > 60, "ambient " + next.ambient());
        helper.assertTrue(next.hydration() < ClimateData.DEFAULT.hydration() - idleDrain,
                "lava should drain hydration faster: " + next);

        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void furnaceOnlyHeatsWhenLit(GameTestHelper helper) {
        ServerPlayer player = playerAt(helper, 2.5, 1, 3.5);

        helper.setBlock(4, 1, 3, Blocks.FURNACE.defaultBlockState());
        double unlit = EnvironmentSampler.sample(player, Settings.get().temperature()).environment().heatSourceTemp();
        helper.setBlock(4, 1, 3, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        double lit = EnvironmentSampler.sample(player, Settings.get().temperature()).environment().heatSourceTemp();

        helper.assertTrue(unlit == 0, "unlit furnace gave heat " + unlit);
        helper.assertTrue(lit > 0, "lit furnace gave no heat");

        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void frozenStatsOnlyUpdateAmbient(GameTestHelper helper) {
        helper.setBlock(5, 1, 3, Blocks.LAVA);
        ServerPlayer player = playerAt(helper, 2.5, 1, 3.5);
        ClimateData before = new ClimateData(36.0, 42.0, 0.0);

        ClimateData after = ClimateTicker.next(player, before, true, Settings.get());
        helper.assertTrue(after.bodyTemp() == before.bodyTemp() && after.hydration() == before.hydration(),
                "frozen stats changed: " + after);
        helper.assertTrue(after.ambient() > 60, "ambient not updated: " + after);

        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void statsAreSavedWithThePlayer(GameTestHelper helper) {
        ServerPlayer player = playerAt(helper, 2.5, 1, 3.5);
        ClimateData stored = new ClimateData(33.5, 47.25, 12.0);
        player.setData(ModAttachments.CLIMATE, stored);

        CompoundTag saved = player.saveWithoutId(new CompoundTag());
        removePlayer(helper, player);

        ServerPlayer reloaded = playerAt(helper, 2.5, 1, 3.5);
        reloaded.load(saved);
        ClimateData loaded = reloaded.getData(ModAttachments.CLIMATE);
        helper.assertTrue(stored.equals(loaded), "saved " + stored + " but loaded " + loaded);

        removePlayer(helper, reloaded);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------
    // M4: water and consequences
    // ------------------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void deepWaterIsDirtyButBoilingCleansIt(GameTestHelper helper) {
        // The GameTest world is superflat near y = -60, far below sea level, so water counts as
        // stagnant cave water. A cauldron over a campfire boils it clean.
        helper.setBlock(1, 1, 1, Blocks.WATER);
        BlockPos water = helper.absolutePos(new BlockPos(1, 1, 1));
        WaterType type = WaterSources.ofWater(helper.getLevel(), water);
        helper.assertTrue(type == WaterType.DIRTY, "deep water was " + type);

        helper.setBlock(3, 1, 3, Blocks.CAMPFIRE.defaultBlockState()); // campfires are lit by default
        helper.setBlock(3, 2, 3, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        BlockPos cauldron = helper.absolutePos(new BlockPos(3, 2, 3));
        WaterType boiled = WaterSources.ofCauldron(helper.getLevel(), cauldron);
        helper.assertTrue(boiled == WaterType.CLEAN, "boiled cauldron was " + boiled);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkingRestoresHydration(GameTestHelper helper) {
        ServerPlayer player = playerAt(helper, 2.5, 1, 3.5);
        player.setData(ModAttachments.CLIMATE, new ClimateData(37, 50, 20));

        Drinking.drink(player, WaterType.CLEAN, true);
        double afterBottle = player.getData(ModAttachments.CLIMATE).hydration();
        helper.assertTrue(afterBottle == 50 + Settings.get().water(WaterType.CLEAN).bottleHydration(), "after bottle " + afterBottle);

        Drinking.drink(player, WaterType.SALTY, false);
        helper.assertTrue(player.hasEffect(ModEffects.THIRST), "salt water should cause Thirst");

        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void effectsFollowSeverity(GameTestHelper helper) {
        ServerPlayer player = playerAt(helper, 2.5, 1, 3.5);
        ClimateData fine = new ClimateData(37, 100, 25);

        ClimateTicker.applyConsequences(player, fine, new ClimateData(39.5, 100, 25), Settings.get()); // level 1: warning only
        helper.assertFalse(player.hasEffect(ModEffects.HEATSTROKE), "level 1 should not give Heatstroke");

        ClimateTicker.applyConsequences(player, fine, new ClimateData(41.5, 100, 25), Settings.get()); // level 2
        MobEffectInstance heat = player.getEffect(ModEffects.HEATSTROKE);
        helper.assertTrue(heat != null && heat.getAmplifier() == 0, "level 2 should give Heatstroke I, got " + heat);

        ClimateTicker.applyConsequences(player, fine, new ClimateData(31, 100, 25), Settings.get()); // cold level 2
        helper.assertTrue(player.hasEffect(ModEffects.HYPOTHERMIA), "31 °C should give Hypothermia");

        ClimateTicker.applyConsequences(player, fine, new ClimateData(37, 5, 25), Settings.get()); // thirst level 2
        helper.assertTrue(player.hasEffect(ModEffects.DEHYDRATION), "hydration 5 should give Dehydration");

        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fireResistancePreventsHeatstroke(GameTestHelper helper) {
        ServerPlayer player = playerAt(helper, 2.5, 1, 3.5);
        ClimateData fine = new ClimateData(37, 100, 25);
        ClimateTicker.applyConsequences(player, fine, new ClimateData(44, 100, 25), Settings.get());
        helper.assertTrue(player.hasEffect(ModEffects.HEATSTROKE), "44 °C should give Heatstroke");

        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));
        ClimateTicker.applyConsequences(player, fine, new ClimateData(44, 100, 25), Settings.get());
        helper.assertFalse(player.hasEffect(ModEffects.HEATSTROKE), "Fire Resistance should remove Heatstroke");

        removePlayer(helper, player);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------
    // M5: data maps
    // ------------------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void blockHeatComesFromTheDataMap(GameTestHelper helper) {
        double torch = DataModifiers.blockTemperature(Blocks.TORCH.defaultBlockState());
        double lava = DataModifiers.blockTemperature(Blocks.LAVA.defaultBlockState());
        double ice = DataModifiers.blockTemperature(Blocks.BLUE_ICE.defaultBlockState());
        double stone = DataModifiers.blockTemperature(Blocks.STONE.defaultBlockState());
        helper.assertTrue(torch == 30 && lava == 200 && ice < 0 && stone == 0,
                "torch " + torch + ", lava " + lava + ", blue ice " + ice + ", stone " + stone);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void armourComesFromTheDataMap(GameTestHelper helper) {
        ArmorModifier leather = DataModifiers.armor(new ItemStack(Items.LEATHER_CHESTPLATE));
        ArmorModifier iron = DataModifiers.armor(new ItemStack(Items.IRON_HELMET));
        ArmorModifier stick = DataModifiers.armor(new ItemStack(Items.STICK));
        helper.assertTrue(leather.nightAdd() == 3 && leather.shadeAdd() == 3 && leather.sunAdd() == 2,
                "leather " + leather);
        helper.assertTrue(iron.sunMult() == 1.1 && iron.nightAdd() == -1, "iron " + iron);
        helper.assertTrue(stick.equals(ArmorModifier.NONE), "stick " + stick);

        ServerPlayer player = playerAt(helper, 2.5, 1, 3.5);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        EnvironmentSampler.Sample sample = EnvironmentSampler.sample(player, Settings.get().temperature());
        helper.assertTrue(sample.environment().armor().size() == 1, "worn armour not sampled");
        removePlayer(helper, player);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void saveFormatIsStable(GameTestHelper helper) {
        // Renaming these fields would silently reset every player's stats in existing worlds.
        ClimateData sample = new ClimateData(36.25, 72.5, 14.0);
        JsonObject json = ClimateData.CODEC.encodeStart(JsonOps.INSTANCE, sample).getOrThrow().getAsJsonObject();
        helper.assertTrue(json.get("body_temp").getAsDouble() == 36.25
                && json.get("hydration").getAsDouble() == 72.5
                && json.get("ambient").getAsDouble() == 14.0, "unexpected save format " + json);

        // Saves from before "ambient" existed must still load.
        JsonObject old = new JsonObject();
        old.addProperty("body_temp", 35.0);
        old.addProperty("hydration", 50.0);
        ClimateData loaded = ClimateData.CODEC.parse(JsonOps.INSTANCE, old).getOrThrow();
        helper.assertTrue(loaded.equals(new ClimateData(35.0, 50.0, ClimateData.DEFAULT.ambient())),
                "old save loaded as " + loaded);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void networkFormatRoundTrips(GameTestHelper helper) {
        ClimateData sample = new ClimateData(36.25, 72.5, 14.0);
        ByteBuf buf = Unpooled.buffer();
        try {
            ClimateData.STREAM_CODEC.encode(buf, sample);
            helper.assertTrue(buf.readableBytes() == 24, "expected 3 doubles, got " + buf.readableBytes() + " bytes");
            ClimateData decoded = ClimateData.STREAM_CODEC.decode(buf);
            helper.assertTrue(sample.equals(decoded), "sent " + sample + " but received " + decoded);
        } finally {
            buf.release();
        }
        helper.succeed();
    }
}
