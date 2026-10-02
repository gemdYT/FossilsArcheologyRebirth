package com.github.teamfossilsarcheology.fossil.mixin;

import net.minecraft.world.entity.*;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 26.3 exposes spawn checks publicly, but registration still needs a native invoker. */
@Mixin(SpawnPlacements.class)
public interface SpawnPlacementsInvoker {
    @Invoker("register")
    static <T extends Mob> void fossil$register(EntityType<T> type, SpawnPlacementType placement,
            Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate) { throw new AssertionError(); }
}
