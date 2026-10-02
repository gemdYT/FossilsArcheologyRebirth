package com.github.teamfossilsarcheology.fossil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Fixed mod dimensions use native teleportation, with a persisted per-player return location. */
public final class NativePortals {
    public record ReturnPoint(String dimension, BlockPos pos, float yaw, float pitch) {
        static final Codec<ReturnPoint> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("dimension").forGetter(ReturnPoint::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(ReturnPoint::pos),
                Codec.FLOAT.fieldOf("yaw").forGetter(ReturnPoint::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(ReturnPoint::pitch)).apply(i, ReturnPoint::new));
    }
    private static final AttachmentType<ReturnPoint> RETURN = AttachmentRegistry.<ReturnPoint>builder().persistent(ReturnPoint.CODEC).copyOnDeath().buildAndRegister(ModContent.id("portal_return"));
    private static final AttachmentType<Boolean> ARENA_BUILT = AttachmentRegistry.<Boolean>builder().persistent(Codec.BOOL).buildAndRegister(ModContent.id("arena_built"));
    private static final AttachmentType<Boolean> BOSS_DEFEATED = AttachmentRegistry.<Boolean>builder().persistent(Codec.BOOL).buildAndRegister(ModContent.id("anu_defeated"));
    public static final ResourceKey<Level> LAIR = ResourceKey.create(Registries.DIMENSION, ModContent.id("anu_lair"));
    public static final ResourceKey<Level> TREASURE = ResourceKey.create(Registries.DIMENSION, ModContent.id("treasure_room"));
    private NativePortals() {}
    public static void initialize() { /* Register the return attachment before worlds load. */ }
    public static boolean enter(ServerPlayer player, boolean treasure) {
        if (treasure && !player.isCreative()) {
            var lair = player.level().getServer().getLevel(LAIR);
            if (lair == null || !Boolean.TRUE.equals(((AttachmentTarget)lair).getAttached(BOSS_DEFEATED))) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Defeat Anu before opening the treasury."), true); return false;
            }
        }
        var target = player.level().getServer().getLevel(treasure ? TREASURE : LAIR);
        if (target == null) return false;
        if (!treasure && target.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Anu's encounter requires Easy, Normal or Hard difficulty."), true);
            return false;
        }
        if (!player.level().dimension().equals(LAIR) && !player.level().dimension().equals(TREASURE)) ((AttachmentTarget)player).setAttached(RETURN, new ReturnPoint(player.level().dimension().identifier().toString(), player.blockPosition(), player.getYRot(), player.getXRot()));
        BlockPos center = new BlockPos(0, 5, 0);
        if (!Boolean.TRUE.equals(((AttachmentTarget)target).getAttached(ARENA_BUILT))) {
            // Saved world state prevents repeat visits or block edits from resetting rewards.
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                for (int y = 0; y < 7; y++) target.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                target.setBlock(center.offset(x, -1, z), (treasure ? Blocks.SANDSTONE : NativeBlocks.BLOCKS.get("volcanic_rock")).defaultBlockState(), 3);
                if (Math.abs(x) == 12 || Math.abs(z) == 12) for (int y = 0; y < 4; y++) target.setBlock(center.offset(x, y, z), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
            }
            target.setBlock(center.offset(0, 0, -11), NativeBlocks.BLOCKS.get("home_portal").defaultBlockState(), 3);
            if (treasure) {
                target.setBlock(center.offset(0, 0, 4), Blocks.CHEST.defaultBlockState(), 3);
                if (target.getBlockEntity(center.offset(0, 0, 4)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
                    chest.setItem(0, new net.minecraft.world.item.ItemStack(NativeItems.get("scarab_gem"), 3));
                    chest.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 8));
                    chest.setItem(2, new net.minecraft.world.item.ItemStack(NativeItems.get("music_disc_scarab")));
                }
            } else {
                var boss = NativeHostiles.ANU.create(target, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                if (boss != null) { boss.setPos(.5, center.getY(), 5.5); target.addFreshEntity(boss); }
            }
            ((AttachmentTarget)target).setAttached(ARENA_BUILT, true);
        }
        return player.teleportTo(target, .5, center.getY(), -7.5, java.util.Set.of(), 0, 0, true);
    }
    public static void defeated(net.minecraft.server.level.ServerLevel level) { if (level.dimension().equals(LAIR)) ((AttachmentTarget)level).setAttached(BOSS_DEFEATED, true); }
    public static boolean home(ServerPlayer player) {
        var point = ((AttachmentTarget)player).getAttached(RETURN);
        if (point == null) return false;
        var target = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, net.minecraft.resources.Identifier.parse(point.dimension())));
        if (target == null) return false;
        BlockPos pos = point.pos();
        for (int up = 0; up < 16; up++) if (target.getBlockState(pos.above(up)).canBeReplaced() && target.getBlockState(pos.above(up + 1)).canBeReplaced()) {
            return player.teleportTo(target, pos.getX() + .5, pos.getY() + up, pos.getZ() + .5, java.util.Set.of(), point.yaw(), point.pitch(), true);
        }
        return false;
    }
}
