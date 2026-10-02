package com.github.teamfossilsarcheology.fossil;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NativeAnimals {
    public static final Map<String, EntityType<NativeAnimal>> TYPES = new LinkedHashMap<>();
    public static final Map<String, Item> SPAWN_EGGS = new LinkedHashMap<>();
    public static final Map<String, EntityType<NativeAnimal>> DISPLAYS = new LinkedHashMap<>();
    public static final Map<EntityType<?>, AnimalSpecies> PROFILES = new LinkedHashMap<>();
    public static final Map<EntityType<?>, AnimalSpecies> DISPLAY_PROFILES = new HashMap<>();
    public static EntityType<NativeQuagga> QUAGGA;
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();
    private NativeAnimals() {}

    public static void initialize() {
        AnimalSpecies[] species;
        try (var stream = Objects.requireNonNull(NativeAnimals.class.getResourceAsStream("/assets/fossil/species.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            species = new Gson().fromJson(reader, AnimalSpecies[].class);
        } catch (Exception exception) { throw new IllegalStateException("Cannot load prehistoric species profiles", exception); }
        if (species.length == 0) throw new IllegalStateException("The species catalog is empty");
        for (AnimalSpecies profile : species) {
            if (profile.name().equals("dodo")) { PROFILES.put(ModContent.DODO, profile); TYPES.put("dodo", ModContent.DODO); FabricDefaultAttributeRegistry.register(ModContent.DODO, NativeAnimal.attributes(profile)); registerDisplay(profile); continue; }
            EntityType<?> type;
            if (profile.name().equals("quagga")) {
                QUAGGA = Registry.register(BuiltInRegistries.ENTITY_TYPE, ModContent.id("quagga"),
                        EntityType.Builder.of(NativeQuagga::new, MobCategory.CREATURE).sized(profile.width(), profile.height())
                                .build(ResourceKey.create(Registries.ENTITY_TYPE, ModContent.id("quagga"))));
                FabricDefaultAttributeRegistry.register(QUAGGA, NativeQuagga.attributes());
                type = QUAGGA;
            } else {
                EntityType<NativeAnimal> animal = Registry.register(BuiltInRegistries.ENTITY_TYPE, ModContent.id(profile.name()),
                        EntityType.Builder.of(NativeAnimal::new, NativeAquaticSpawns.SPECIES.contains(profile.name()) ? MobCategory.WATER_AMBIENT : profile.aquatic() ? MobCategory.WATER_CREATURE : MobCategory.CREATURE)
                                .sized(profile.width() * profile.scale(), profile.height() * profile.scale()).clientTrackingRange(10)
                                .build(ResourceKey.create(Registries.ENTITY_TYPE, ModContent.id(profile.name()))));
                PROFILES.put(animal, profile);
                TYPES.put(profile.name(), animal);
                FabricDefaultAttributeRegistry.register(animal, NativeAnimal.attributes(profile));
                type = animal;
            }
            PROFILES.put(type, profile);
            registerDisplay(profile);
            String itemName = "spawn_egg_" + profile.name();
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ModContent.id(itemName));
            SPAWN_EGGS.put(profile.name(), Registry.register(BuiltInRegistries.ITEM, itemKey,
                    new SpawnEggItem(new Item.Properties().setId(itemKey).spawnEgg(type))));
            for (String suffix : List.of("ambient", "hurt", "death")) {
                String soundName = profile.name() + "_" + suffix;
                SOUNDS.put(soundName, Registry.register(BuiltInRegistries.SOUND_EVENT, ModContent.id(soundName),
                        SoundEvent.createVariableRangeEvent(ModContent.id(soundName))));
            }
        }
    }
    private static void registerDisplay(AnimalSpecies profile) {
            EntityType<NativeAnimal> display = Registry.register(BuiltInRegistries.ENTITY_TYPE, ModContent.id(profile.name() + "_skeleton"),
                    EntityType.Builder.of(NativeAnimal::new, MobCategory.MISC).sized(profile.width() * profile.scale(), profile.height() * profile.scale()).clientTrackingRange(10)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, ModContent.id(profile.name() + "_skeleton"))));
            DISPLAYS.put(profile.name(), display);
            DISPLAY_PROFILES.put(display, profile);
            FabricDefaultAttributeRegistry.register(display, NativeAnimal.attributes(profile));
    }
    public static AnimalSpecies profile(EntityType<?> type) {
        AnimalSpecies profile = PROFILES.get(type);
        if (profile == null) profile = DISPLAY_PROFILES.get(type);
        return Objects.requireNonNull(profile, "Unregistered animal type " + type);
    }
    public static SoundEvent sound(AnimalSpecies species, String suffix) {
        if (species.name().equals("dodo")) return switch (suffix) { case "ambient" -> ModContent.DODO_AMBIENT; case "hurt" -> ModContent.DODO_HURT; default -> ModContent.DODO_DEATH; };
        return SOUNDS.get(species.name() + "_" + suffix);
    }
}
