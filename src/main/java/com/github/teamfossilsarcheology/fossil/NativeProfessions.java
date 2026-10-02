package com.github.teamfossilsarcheology.fossil;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.block.Block;

public final class NativeProfessions {
    private NativeProfessions() {}
    public static void initialize() {
        register("paleontologist", ModContent.ANALYZER);
        register("archeologist", NativeBlocks.BLOCKS.get("worktable"));
    }
    private static void register(String role, Block workstation) {
        var states = java.util.Set.copyOf(workstation.getStateDefinition().getPossibleStates());
        var poi = Registry.registerForHolder(BuiltInRegistries.POINT_OF_INTEREST_TYPE, ModContent.id(role), new PoiType(states, 1, 1));
        com.github.teamfossilsarcheology.fossil.mixin.PoiTypesInvoker.fossil$registerStates(poi, states);
        var trades = new Int2ObjectOpenHashMap<ResourceKey<TradeSet>>();
        for (int level = 1; level <= 5; level++) trades.put(level, ResourceKey.create(Registries.TRADE_SET, ModContent.id(role + "/level_" + level)));
        Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, ModContent.id(role), new VillagerProfession(Component.translatable("entity.minecraft.villager.fossil." + role), p -> p.is(poi.unwrapKey().orElseThrow()), p -> p.is(poi.unwrapKey().orElseThrow()), ImmutableSet.of(), ImmutableSet.of(), net.minecraft.sounds.SoundEvents.VILLAGER_WORK_MASON, trades));
    }
}
