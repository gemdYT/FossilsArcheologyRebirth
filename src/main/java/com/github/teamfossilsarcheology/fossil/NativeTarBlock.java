package com.github.teamfossilsarcheology.fossil;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;

/** A visible, slow, non-flowing tar pool avoids a second liquid simulation in this mod. */
public final class NativeTarBlock extends NativeDecorBlock implements BucketPickup {
    public NativeTarBlock(Properties properties) { super(properties); }
    @Override public ItemStack pickupBlock(LivingEntity entity, LevelAccessor level, BlockPos pos, BlockState state) {
        level.removeBlock(pos, false); return new ItemStack(NativeItems.get("tar_bucket"));
    }
    @Override public Optional<SoundEvent> getPickupSound() { return Optional.of(SoundEvents.BUCKET_FILL); }
}
