package enviromine_revived.gametest;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import enviromine_revived.ClimateTicker;
import enviromine_revived.EnviroMineRevived;
import enviromine_revived.data.ClimateData;
import enviromine_revived.data.ModAttachments;
import enviromine_revived.world.EnvironmentSampler;
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

        EnvironmentSampler.Sample sample = EnvironmentSampler.sample(player, ClimateTicker.TEMPERATURE);
        helper.assertTrue(sample.environment().nearLava(), "lava 3 blocks away was not detected");
        helper.assertTrue(sample.environment().heatSourceTemp() > 100,
                "heat source " + sample.environment().heatSourceTemp());

        ClimateData next = ClimateTicker.next(player, ClimateData.DEFAULT, false);
        double idleDrain = ClimateTicker.HYDRATION.tuning().baseDrain();
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
        double unlit = EnvironmentSampler.sample(player, ClimateTicker.TEMPERATURE).environment().heatSourceTemp();
        helper.setBlock(4, 1, 3, Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.LIT, true));
        double lit = EnvironmentSampler.sample(player, ClimateTicker.TEMPERATURE).environment().heatSourceTemp();

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

        ClimateData after = ClimateTicker.next(player, before, true);
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
