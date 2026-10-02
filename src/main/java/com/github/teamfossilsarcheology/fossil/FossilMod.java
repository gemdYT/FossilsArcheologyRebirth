package com.github.teamfossilsarcheology.fossil;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;

public final class FossilMod implements ModInitializer {
    @Override public void onInitialize() {
        ModConfig.load();
        ModContent.initialize();
        NativeAnimals.initialize();
        NativeAquaticSpawns.initialize();
        AnimalPart.initialize();
        NativeBlocks.initialize();
        NativeItems.initialize();
        NativeHostiles.initialize();
        NativeMachineMenu.initialize();
        ProcessingRecipes.initialize();
        NativeWorldGen.initialize();
        NativePortals.initialize();
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(DinopediaPayload.TYPE, DinopediaPayload.CODEC);
        NativeProfessions.initialize();
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity((machine, side) -> machine.energy, ModContent.MACHINE);
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity((machine, side) -> net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(machine, side), ModContent.MACHINE);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE, ModContent.id("ore_fossil")));
        for (String ore : java.util.List.of("ore_amber", "ore_fossil_deepslate", "ore_fossil_stone", "ore_fossil_calcite", "ore_fossil_tuff", "ore_fossil_dripstone", "ore_fossil_sandstone", "ore_fossil_red_sandstone"))
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES, ResourceKey.create(Registries.PLACED_FEATURE, ModContent.id(ore)));
        for (String tree : java.util.List.of("calamites", "cordaites", "palm", "sigillaria", "tempskya"))
            BiomeModifications.addFeature(BiomeSelectors.includeByKey(net.minecraft.world.level.biome.Biomes.JUNGLE, net.minecraft.world.level.biome.Biomes.SWAMP), GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, ModContent.id(tree + "_tree")));
        BiomeModifications.addFeature(BiomeSelectors.includeByKey(net.minecraft.world.level.biome.Biomes.JUNGLE, net.minecraft.world.level.biome.Biomes.SWAMP), GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, ModContent.id("prehistoric_ferns")));
    }
}
