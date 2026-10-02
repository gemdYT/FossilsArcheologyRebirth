package com.github.teamfossilsarcheology.fossil;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public final class NativeTarBucket extends Item {
    public NativeTarBucket(Properties properties) { super(properties.stacksTo(1).craftRemainder(Items.BUCKET)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = level.getBlockState(context.getClickedPos()).canBeReplaced() ? context.getClickedPos() : context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced() || context.getPlayer() == null || !context.getPlayer().mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            level.setBlock(pos, NativeBlocks.BLOCKS.get("tar").defaultBlockState(), Block.UPDATE_ALL);
            context.getPlayer().setItemInHand(context.getHand(), ItemUtils.createFilledResult(context.getItemInHand(), context.getPlayer(), new ItemStack(Items.BUCKET)));
        }
        return InteractionResult.SUCCESS;
    }
}
