package com.github.teamfossilsarcheology.fossil;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.item.*;
import java.util.*;

/** Native hostile registrations and their shared combat attributes. */
public final class NativeHostiles {
    public static EntityType<NativeAnu> ANU;
    public static EntityType<PiglinBrute> SENTRY;
    public static EntityType<Slime> TAR_SLIME;
    public static EntityType<NativeFailure> FAILURE;
    public static final Map<String, Item> EGGS = new LinkedHashMap<>();
    public static void initialize() {
        ANU = register("anu_boss", EntityType.Builder.of(NativeAnu::new, MobCategory.MONSTER).sized(.8f, 2.4f));
        FabricDefaultAttributeRegistry.register(ANU, PiglinBrute.createAttributes().add(Attributes.MAX_HEALTH, 150).add(Attributes.ATTACK_DAMAGE, 10));
        SENTRY = brute("sentry_piglin", 35, 6);
        TAR_SLIME = register("tar_slime", EntityType.Builder.of(Slime::new, MobCategory.MONSTER).sized(.52f, .52f));
        FabricDefaultAttributeRegistry.register(TAR_SLIME, net.minecraft.world.entity.animal.Animal.createAnimalAttributes().add(Attributes.ATTACK_DAMAGE, 2));
        FAILURE = register("failuresaurus", EntityType.Builder.of(NativeFailure::new, MobCategory.MONSTER).sized(1.2f, 1.8f));
        FabricDefaultAttributeRegistry.register(FAILURE, NativeFailure.attributes());
    }
    private static EntityType<PiglinBrute> brute(String name, double health, double damage) {
        EntityType<PiglinBrute> type = register(name, EntityType.Builder.<PiglinBrute>of((entityType, level) -> {
            PiglinBrute brute = new PiglinBrute(entityType, level); brute.setImmuneToZombification(true); return brute;
        }, MobCategory.MONSTER).sized(.6f, 1.95f));
        FabricDefaultAttributeRegistry.register(type, PiglinBrute.createAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.ATTACK_DAMAGE, damage));
        return type;
    }
    private static <T extends Mob> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, ModContent.id(name), builder.build(ResourceKey.create(Registries.ENTITY_TYPE, ModContent.id(name))));
        String id = "spawn_egg_" + name;
        var key = ResourceKey.create(Registries.ITEM, ModContent.id(id));
        EGGS.put(name, Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(type))));
        return type;
    }
}
