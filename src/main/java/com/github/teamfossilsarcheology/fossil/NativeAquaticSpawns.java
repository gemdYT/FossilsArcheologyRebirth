package com.github.teamfossilsarcheology.fossil;

import java.util.Set;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import com.github.teamfossilsarcheology.fossil.mixin.SpawnPlacementsInvoker;

/** Four surviving aquatic species use vanilla natural spawning and water-only placement. */
public final class NativeAquaticSpawns {
    public static final Set<String> SPECIES = Set.of("alligator_gar", "coelacanth", "nautilus", "sturgeon");
    private NativeAquaticSpawns() {}

    public static void initialize() {
        for (String name : SPECIES) {
            EntityType<NativeAnimal> type = NativeAnimals.TYPES.get(name);
            var biomes = TagKey.create(Registries.BIOME, ModContent.id("spawns/" + name));
            BiomeModifications.addSpawn(BiomeSelectors.tag(biomes), type.getCategory(), type,
                    name.equals("nautilus") ? 4 : 6, 1, name.equals("nautilus") ? 2 : 3);
            SpawnPlacementsInvoker.fossil$register(type, SpawnPlacementTypes.IN_WATER,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (animal, level, reason, pos, random) -> canSpawn(level, pos));
        }
    }

    static boolean canSpawn(ServerLevelAccessor level, BlockPos pos) {
        int seaLevel = level.getLevel().getSeaLevel();
        return pos.getY() >= seaLevel - 48 && pos.getY() <= seaLevel
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.below()).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }
}
