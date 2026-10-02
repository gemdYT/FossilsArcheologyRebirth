package com.github.teamfossilsarcheology.fossil;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;

/** Every species uses one incubation block; the species is persisted by its block entity. */
public final class IncubationItem extends BlockItem {
    private final String species;
    public IncubationItem(String species, Properties properties) { super(ModContent.DODO_EGG, properties); this.species = species; }
    @Override protected boolean placeBlock(net.minecraft.world.item.context.BlockPlaceContext context, net.minecraft.world.level.block.state.BlockState state) {
        boolean placed = super.placeBlock(context, state);
        if (placed && !context.getLevel().isClientSide() && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MachineBlockEntity egg) egg.setSpecies(species);
        return placed;
    }
    @Override public void registerBlocks(java.util.Map<net.minecraft.world.level.block.Block, net.minecraft.world.item.Item> map, net.minecraft.world.item.Item item) {
        // Preserve the shared incubation block's own item identity.
    }
}
