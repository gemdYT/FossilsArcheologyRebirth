package com.github.teamfossilsarcheology.fossil;

import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.*;

final class AquaticSpawnSmoke {
    private BlockPos center;
    void setup(MinecraftServer server) {
        var level = server.overworld();
        center = new BlockPos(64, level.getSeaLevel() - 5, 48);
        for (var pos : BlockPos.betweenClosed(center.offset(-16, -3, -16), center.offset(16, 5, 16)))
            level.setBlock(pos, pos.getY() < level.getSeaLevel() ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        // WATER_AMBIENT uses a 64-block despawn radius; stay between its 24/64 limits.
        level.setBlock(new BlockPos(32, level.getSeaLevel() + 1, 48), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        server.getPlayerList().getPlayers().getFirst().teleportTo(level, 32.5, level.getSeaLevel() + 2, 48.5, java.util.Set.of(), 180, 20, true);
        for (String name : NativeAquaticSpawns.SPECIES) {
            var type = NativeAnimals.TYPES.get(name);
            if (SpawnPlacements.getPlacementType(type) != SpawnPlacementTypes.IN_WATER
                    || !SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, center, level.getRandom()))
                throw new AssertionError("Aquatic spawn placement failed: " + name);
            var animal = type.create(level, EntitySpawnReason.NATURAL);
            animal.setPos(center.getX() + .5, center.getY(), center.getZ() + .5);
            if (!animal.checkSpawnRules(level, EntitySpawnReason.NATURAL)) throw new AssertionError("Fish rejected aquatic habitat: " + name);
            if (!animal.checkSpawnObstruction(level)) throw new AssertionError("Fish refused to spawn in water: " + name);
            var plains = level.registryAccess().lookupOrThrow(Registries.BIOME).getValue(net.minecraft.resources.Identifier.withDefaultNamespace("plains"));
            var plainsSpawns = plains.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY)
                    .getMobsToSpawn(type.getCategory());
            if (plainsSpawns.unwrap().stream().anyMatch(w -> w.value().type() == type))
                throw new AssertionError("Aquatic animal added to plains spawns");
            animal.discard();
        }
        level.setBlock(center, Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (NativeAquaticSpawns.canSpawn(level, center)) throw new AssertionError("Fish can spawn in lava");
        level.setBlock(center, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (NativeAquaticSpawns.canSpawn(level, center)) throw new AssertionError("Fish can spawn on land");
        level.setBlock(center, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    String biomeCommand(String biome) {
        return "fillbiome 48 " + (center.getY() - 3) + " 32 80 " + (center.getY() + 5) + " 64 minecraft:" + biome;
    }

    void verifyNaturalSpawning(MinecraftServer server, String biome) {
        var level = server.overworld();
        var expected = biome.equals("ocean") ? List.of("coelacanth", "nautilus") : List.of(biome.equals("swamp") ? "alligator_gar" : "sturgeon");
        var spawns = level.environmentAttributes().getValue(EnvironmentAttributes.NATURAL_MOB_SPAWNS, center)
                .getMobsToSpawn(MobCategory.WATER_AMBIENT).unwrap().stream()
                .map(w -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(w.value().type()).getPath())
                .filter(NativeAquaticSpawns.SPECIES::contains).toList();
        if (!spawns.containsAll(expected) || !expected.containsAll(spawns))
            throw new AssertionError("Incorrect " + biome + " aquatic spawn table: " + spawns);
        var found = new HashSet<String>();
        for (int attempt = 0; attempt < 100 && !found.containsAll(expected); attempt++)
            NaturalSpawner.spawnCategoryForPosition(MobCategory.WATER_AMBIENT, level, level.getChunkAt(center), center,
                    (type, world, pos, chunk) -> NativeAquaticSpawns.SPECIES.contains(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath()),
                    (mob, chunk) -> {
                        if (!mob.removeWhenFarAway(4096)) throw new AssertionError("Wild fish cannot despawn");
                        ((NativeAnimal) mob).setFromBucket(true);
                        if (mob.removeWhenFarAway(4096)) throw new AssertionError("Captured fish can despawn");
                        found.add(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath());
                        mob.discard();
                    });
        if (!found.containsAll(expected)) throw new AssertionError("Native natural spawning failed for " + biome + ": " + found
                + " player=" + level.players().getFirst().position() + " spawn=" + level.getRespawnData().pos()
                + " generatorTable=" + level.getChunkSource().getGenerator().getMobsAt(level, level.structureManager(), MobCategory.WATER_AMBIENT, center).unwrap());
        System.out.println("FOSSIL WILD AQUATICS: native " + biome + " spawning passed " + found);
    }
}
