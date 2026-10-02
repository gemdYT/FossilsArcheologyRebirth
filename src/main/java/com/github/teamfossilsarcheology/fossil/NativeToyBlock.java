package com.github.teamfossilsarcheology.fossil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Reusable enclosure enrichment; native blocks avoid another entity/rendering framework. */
public final class NativeToyBlock extends Block {
    public NativeToyBlock(Properties properties) { super(properties); }
    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(3, 0, 3, 13, 12, 13); }
    private void enrich(ServerLevel level, BlockPos pos) {
        for (var animal : level.getEntitiesOfClass(NativeAnimal.class, new AABB(pos).inflate(6), a -> !a.isDisplay())) animal.enrich();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.getX() + .5, pos.getY() + .8, pos.getZ() + .5, 3, .2, .2, .2, .01);
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { enrich(level, pos); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) { if (level instanceof ServerLevel server) enrich(server, pos); return InteractionResult.SUCCESS; }
}
