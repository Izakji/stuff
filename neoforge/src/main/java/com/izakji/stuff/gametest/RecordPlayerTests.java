package com.izakji.stuff.gametest;

import com.izakji.stuff.content.recordplayer.RecordPlayerBlock;
import com.izakji.stuff.content.recordplayer.RecordPlayerBlockEntity;
import com.izakji.stuff.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for record insertion, playback, ejection, and persistence.
 *
 * @author Izakji
 */
@GameTestHolder("stuff")
@PrefixGameTestTemplate(false)
public final class RecordPlayerTests {
    private static RecordPlayerBlockEntity place(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.getLevel().setBlock(pos, ModContent.OAK_RECORD_PLAYER.get().defaultBlockState(), 3);
        return (RecordPlayerBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void happyDaysPlayback(GameTestHelper helper) {
        var stack = new ItemStack(ModContent.HAPPY_DAYS_DISC.get());
        var song = JukeboxSong.fromStack(helper.getLevel().registryAccess(), stack);
        helper.assertTrue(song.isPresent(), "Happy Days jukebox song data is missing");
        helper.assertTrue(song.orElseThrow().is(ModContent.HAPPY_DAYS_SONG), "Wrong song on custom disc");
        helper.assertTrue(
                Math.abs(song.orElseThrow().value().lengthInSeconds() - 184.6F) < .01F, "Wrong audio duration");
        var record = place(helper);
        helper.assertTrue(record.insert(stack, helper.makeMockPlayer(GameType.SURVIVAL)), "Custom disc rejected");
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(record.isPlaying(), "Happy Days did not begin playback");
            record.eject();
            helper.assertTrue(!record.isPlaying() && record.disc().isEmpty(), "Custom disc did not stop on ejection");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void insertionPlaybackAndEjection(GameTestHelper helper) {
        var record = place(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(Items.MUSIC_DISC_CAT);
        helper.assertTrue(record.insert(stack, player) && stack.isEmpty(), "Disc not transferred exactly once");
        helper.assertTrue(!record.isPlaying(), "Audio started before animation");
        helper.assertTrue(
                !record.insert(new ItemStack(Items.MUSIC_DISC_13), player), "Occupied player accepted a second disc");
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(record.lidProgress(0) == 1 && !record.isPlaying(), "Lid/placement sequence incorrect");
            helper.assertTrue(record.armProgress(0) == 0, "Needle moved before disc was placed");
        });
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(
                    record.phase() == RecordPlayerBlockEntity.Phase.PLAYING && record.isPlaying(),
                    "Vanilla disc did not start");
            record.eject();
            record.eject();
            helper.assertTrue(record.disc().isEmpty() && !record.isPlaying(), "Ejection did not stop audio");
            long count =
                    helper
                            .getLevel()
                            .getEntitiesOfClass(ItemEntity.class, new AABB(record.getBlockPos()).inflate(2))
                            .stream()
                            .filter(item -> item.getItem().is(Items.MUSIC_DISC_CAT))
                            .mapToInt(item -> item.getItem().getCount())
                            .sum();
            helper.assertTrue(count == 1, "Ejection lost or duplicated disc");
        });
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(record.phase() == RecordPlayerBlockEntity.Phase.IDLE, "Player did not close");
            helper.assertTrue(!record.getBlockState().getValue(RecordPlayerBlock.ACTIVE), "Empty player still ticking");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void cancellationAndBreak(GameTestHelper helper) {
        var record = place(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(!record.insert(new ItemStack(Items.DIAMOND), player), "Non-disc was accepted");
        record.insert(new ItemStack(Items.MUSIC_DISC_13), player);
        helper.runAfterDelay(5, () -> {
            record.eject();
            helper.getLevel().destroyBlock(record.getBlockPos(), true);
        });
        helper.runAfterDelay(42, () -> {
            helper.assertTrue(!record.isPlaying(), "Cancelled insertion started playback later");
            int count =
                    helper
                            .getLevel()
                            .getEntitiesOfClass(ItemEntity.class, new AABB(record.getBlockPos()).inflate(3))
                            .stream()
                            .filter(item -> item.getItem().is(Items.MUSIC_DISC_13))
                            .mapToInt(item -> item.getItem().getCount())
                            .sum();
            helper.assertTrue(count == 1, "Break after cancellation duplicated disc");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void persistenceAndBlockedLid(GameTestHelper helper) {
        var record = place(helper);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.getLevel().setBlock(record.getBlockPos().above(), Blocks.STONE.defaultBlockState(), 3);
        var stack = new ItemStack(Items.MUSIC_DISC_CAT);
        helper.assertTrue(!record.insert(stack, player) && stack.getCount() == 1, "Blocked lid consumed disc");
        helper.getLevel().removeBlock(record.getBlockPos().above(), false);
        record.insert(stack, player);
        var saved = record.saveWithoutMetadata(helper.getLevel().registryAccess());
        record.loadWithComponents(saved, helper.getLevel().registryAccess());
        record.recoverOnLoad();
        helper.assertTrue(record.disc().is(Items.MUSIC_DISC_CAT), "Reload lost disc");
        helper.assertTrue(
                record.phase() == RecordPlayerBlockEntity.Phase.LOADED && !record.isPlaying(),
                "Reload restarted audio");
        helper.assertTrue(
                !record.getBlockState().getValue(RecordPlayerBlock.ACTIVE), "Stopped loaded player still ticking");
        helper.succeed();
    }
}
