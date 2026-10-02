package com.github.teamfossilsarcheology.fossil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

/** Checks native data execution rather than merely finding JSON files. */
final class ProgressionSmoke {
    static void verify(ServerLevel level) {
        var pos = new BlockPos(16, 100, 16);
        var pick = new ItemStack(Items.IRON_PICKAXE);
        var ore = ModContent.FOSSIL_ORE.defaultBlockState();
        if (Block.getDrops(ore, level, pos, null, null, pick).stream().noneMatch(i -> i.is(ModContent.BIO_FOSSIL)))
            throw new AssertionError("Native fossil mining loot failed");
        var leaf = NativeBlocks.BLOCKS.get("calamites_leaves");
        if (Block.getDrops(leaf.defaultBlockState(), level, pos, null, null, new ItemStack(Items.SHEARS)).stream().noneMatch(i -> i.is(leaf.asItem())))
            throw new AssertionError("Native shears loot predicate failed");
        var door = NativeBlocks.BLOCKS.get("calamites_door").defaultBlockState();
        if (Block.getDrops(door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), level, pos, null, null, pick).isEmpty()
                || !Block.getDrops(door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), level, pos, null, null, pick).isEmpty())
            throw new AssertionError("Door loot duplicated or missing");
        var template = level.getServer().getStructureTemplateManager().get(ModContent.id("houses/paleo_house_desert")).orElseThrow();
        if (!template.placeInWorld(level, pos, pos, new StructurePlaceSettings().setIgnoreEntities(true), net.minecraft.util.RandomSource.create(123), Block.UPDATE_ALL))
            throw new AssertionError("Machine template placement failed");
        boolean machineFound = false;
        var size = template.getSize();
        for (var block : BlockPos.betweenClosed(pos, pos.offset(size.getX(), size.getY(), size.getZ())))
            machineFound |= level.getBlockEntity(block) instanceof MachineBlockEntity;
        if (!machineFound) throw new AssertionError("Template machine inventory failed to load");
        var treePos = new BlockPos(32, 100, 32);
        for (var air : BlockPos.betweenClosed(treePos.offset(-4, 0, -4), treePos.offset(4, 15, 4)))
            level.setBlock(air, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(treePos.below(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
        var tree = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(ModContent.id("tempskya_tree"));
        if (tree == null || !tree.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(123), treePos)
                || !level.getBlockState(treePos).is(NativeBlocks.BLOCKS.get("tempskya_log")))
            throw new AssertionError("Native prehistoric tree placement failed");
        var cone = new BlockPos(48, 0, 48);
        int base = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, cone.getX(), cone.getZ());
        var feature = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(ModContent.id("volcano_cone"));
        if (feature == null || !feature.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(123), cone)
                || !level.getBlockState(cone.atY(base + 13)).is(net.minecraft.world.level.block.Blocks.LAVA))
            throw new AssertionError("Volcano feature failed");
        System.out.println("FOSSIL PROGRESSION: native mining, shears, multiblock loot, template inventories and volcano placement passed");
    }
}
