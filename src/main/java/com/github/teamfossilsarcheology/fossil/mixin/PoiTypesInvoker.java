package com.github.teamfossilsarcheology.fossil.mixin;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Native POI state registration currently has no public vanilla entrypoint. */
@Mixin(PoiTypes.class)
public interface PoiTypesInvoker {
    @Invoker("registerBlockStates")
    static void fossil$registerStates(Holder<PoiType> type, Set<BlockState> states) { throw new AssertionError(); }
}
