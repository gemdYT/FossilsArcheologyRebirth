package com.github.teamfossilsarcheology.fossil;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import terrablender.api.*;

public final class NativeWorldGen implements TerraBlenderApi {
    public static final ResourceKey<Biome> VOLCANO = ResourceKey.create(Registries.BIOME, ModContent.id("volcano"));
    @Override public void onTerraBlenderInitialized() {
        Regions.register(new Region(ModContent.id("prehistoric"), RegionType.OVERWORLD, 2) {
            @Override public void addBiomes(net.minecraft.core.Registry<Biome> registry, java.util.function.Consumer<com.mojang.datafixers.util.Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
                addModifiedVanillaOverworldBiomes(mapper, builder -> builder.replaceBiome(Biomes.STONY_PEAKS, VOLCANO));
            }
        });
        MaterialRuleManager.addRules(MaterialRuleManager.RuleCategory.OVERWORLD, "fossil", access ->
                MaterialRules.ifTrue(MaterialRules.isBiome(access.lookupOrThrow(Registries.BIOME), VOLCANO),
                        MaterialRules.ifTrue(MaterialRules.stoneDepthCheck(0, true, CaveSurface.FLOOR),
                                MaterialRules.state(NativeBlocks.BLOCKS.get("volcanic_rock").defaultBlockState()))));
    }
    public static void initialize() {
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.FEATURE_TYPE, ModContent.id("volcano"), VolcanoFeature.CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            for (String village : java.util.List.of("plains", "desert", "savanna", "snowy", "taiga")) {
                for (String role : java.util.List.of("archeo", "paleo")) {
                    String building = "houses/" + role + "_house_" + village;
                    if (village.equals("plains") || village.equals("taiga") && role.equals("archeo")) building += "_top";
                    var pool = net.minecraft.resources.Identifier.withDefaultNamespace("village/" + village + "/houses");
                    var structure = ModContent.id(building);
                    net.rpg_foundation.structure_pool.api.StructurePoolAPI.injectIntoStructurePool(server, pool, structure, 2);
                    net.rpg_foundation.structure_pool.api.StructurePoolAPI.limitSpawn(pool, structure, 1);
                }
            }
        });
    }
}
