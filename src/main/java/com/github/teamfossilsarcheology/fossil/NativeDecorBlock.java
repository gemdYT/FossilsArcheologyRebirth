package com.github.teamfossilsarcheology.fossil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import java.util.*;

/** Native directional decorations and plants, with small shared growth/multiblock behavior. */
public class NativeDecorBlock extends Block implements BonemealableBlock {
    protected final BlockProfile profile;
    protected final Map<String, CatalogProperty> catalogProperties = new LinkedHashMap<>();
    public NativeDecorBlock(Properties properties) {
        super(properties);
        profile = NativeBlocks.CONSTRUCTING.get();
        BlockState state = stateDefinition.any();
        for (var property : stateDefinition.getProperties()) {
            if (property instanceof CatalogProperty catalog) {
                catalogProperties.put(catalog.getName(), catalog);
                if (catalog.getPossibleValues().contains("north")) state = state.setValue(catalog, "north");
                else if (catalog.getPossibleValues().contains("false")) state = state.setValue(catalog, "false");
                else if (catalog.getPossibleValues().contains("lower")) state = state.setValue(catalog, "lower");
            }
        }
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        NativeBlocks.CONSTRUCTING.get().properties().forEach((name, values) -> {
            if (!values.isEmpty()) builder.add(new CatalogProperty(name, values));
        });
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        var facing = catalogProperties.get("facing");
        if (facing != null) state = state.setValue(facing, context.getHorizontalDirection().getOpposite().getSerializedName());
        int height = height();
        for (int i = 1; i < height; i++) if (!context.getLevel().getBlockState(context.getClickedPos().above(i)).canBeReplaced()) return null;
        return state;
    }
    private int height() { return profile.kind().equals("four_plant") ? 4 : catalogProperties.containsKey("half") ? 2 : 1; }
    private int layer(BlockState state) {
        if (height() == 4) return Integer.parseInt(state.getValue(catalogProperties.get("layer")));
        return height() == 2 && state.getValue(catalogProperties.get("half")).equals("upper") ? 1 : 0;
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            net.minecraft.core.Direction direction, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (!canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        int layer = layer(state);
        if (height() > 1 && ((layer > 0 && !level.getBlockState(pos.below()).is(this))
                || (layer < height() - 1 && !level.getBlockState(pos.above()).is(this)))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && height() > 1) {
            BlockPos base = pos.below(layer(state));
            if (layer(state) > 0 && !player.getAbilities().instabuild) Block.popResource(level, pos, new ItemStack(this));
            for (int i = 0; i < height(); i++) {
                BlockPos part = base.above(i);
                if (!part.equals(pos) && level.getBlockState(part).is(this)) level.setBlock(part, Blocks.AIR.defaultBlockState(), UPDATE_CLIENTS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (height() == 4) for (int i = 1; i < 4; i++) level.setBlock(pos.above(i), state.setValue(catalogProperties.get("layer"), Integer.toString(i)), UPDATE_ALL);
        else if (height() == 2) level.setBlock(pos.above(), state.setValue(catalogProperties.get("half"), "upper"), UPDATE_ALL);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return profile.kind().equals("decor") ? Block.box(2, 0, 2, 14, 14, 14) : super.getShape(state, level, pos, context);
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!profile.kind().contains("plant") && !profile.kind().equals("sapling")) return true;
        return level.getBlockState(pos.below()).is(this) || level.getBlockState(pos.below()).isSolid();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (profile.name().equals("home_portal")) { if (player instanceof net.minecraft.server.level.ServerPlayer server) NativePortals.home(server); return InteractionResult.SUCCESS; }
        if (profile.name().equals("anu_portal")) { if (player instanceof net.minecraft.server.level.ServerPlayer server) NativePortals.enter(server, false); return InteractionResult.SUCCESS; }
        if (profile.kind().contains("plant") && catalogProperties.containsKey("age")) {
            var age = catalogProperties.get("age");
            if (state.getValue(age).equals(age.getPossibleValues().getLast())) {
                var berry = NativeItems.get("berry_" + profile.name().replace("plant_", ""));
                if (berry != null && !level.isClientSide()) { Block.popResource(level, pos, new ItemStack(berry, 2)); level.setBlock(pos, state.setValue(age, age.getPossibleValues().getFirst()), UPDATE_ALL); }
                return InteractionResult.SUCCESS;
            }
        }
        if (profile.name().equals("drum")) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASEDRUM.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        var age = catalogProperties.get("age");
        return profile.kind().equals("sapling") || age != null && !state.getValue(age).equals(age.getPossibleValues().getLast());
    }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) { return true; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        if (profile.kind().equals("sapling")) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.FEATURE, ModContent.id(profile.name().replace("_sapling", "_tree")));
            var feature = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.FEATURE).getValue(key);
            if (feature != null) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_ALL);
                if (!feature.place(level, level.getChunkSource().getGenerator(), random, pos)) level.setBlock(pos, state, UPDATE_ALL);
            }
        } else {
            var age = catalogProperties.get("age");
            if (age != null) level.setBlock(pos, state.setValue(age, age.getPossibleValues().get(Math.min(age.getInternalIndex(state.getValue(age)) + 1, age.getPossibleValues().size() - 1))), UPDATE_ALL);
        }
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (profile.name().equals("ash_vent")) { level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 8, .1, .5, .1, .02); return; }
        if (profile.name().equals("bubble_blower") && level.getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.WATER)) {
            BlockPos water = pos.above();
            for (int y = 0; y < 16 && level.getBlockState(water).is(Blocks.WATER); y++, water = water.above()) level.setBlock(water, Blocks.BUBBLE_COLUMN.defaultBlockState(), UPDATE_ALL);
            return;
        }
        if (random.nextInt(7) == 0 && isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION)) performBonemeal(level, random, pos, state, BonemealSource.INTERACTION);
    }
}
