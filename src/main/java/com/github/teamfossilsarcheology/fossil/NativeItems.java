package com.github.teamfossilsarcheology.fossil;

import com.google.gson.Gson;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.sounds.SoundEvents;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NativeItems {
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<Item, String> DNA_SPECIES = new LinkedHashMap<>();
    public static final Map<String, Item> INCUBATION_ITEMS = new LinkedHashMap<>();
    private NativeItems() {}
    private static final Set<String> VANILLA = Set.of("axolotl", "bat", "bee", "cat", "chicken", "cod", "cow", "dolphin", "donkey", "fox", "goat", "glow_squid", "hoglin", "horse", "llama", "mooshroom", "ocelot", "panda", "parrot", "pig", "polar_bear", "pufferfish", "rabbit", "salmon", "sheep", "squid", "strider", "tropical_fish", "turtle", "wolf");
    @SuppressWarnings("unchecked")
    public static EntityType<? extends Mob> animalType(String name) {
        if (name.equals("dodo")) return ModContent.DODO;
        if (name.equals("quagga")) return NativeAnimals.QUAGGA;
        return VANILLA.contains(name) ? (EntityType<? extends Mob>) BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(name)) : NativeAnimals.TYPES.get(name);
    }
    public static Item get(String name) {
        if (name.equals("bio_goo")) return ModContent.BIO_GOO;
        if (name.equals("dodo_dna")) return ModContent.DODO_DNA;
        return ITEMS.get(name);
    }
    private static Block plant(String name) { return NativeBlocks.BLOCKS.getOrDefault(name, NativeBlocks.BLOCKS.get(name + "_small")); }
    public static void initialize() {
        String[] names;
        try (var reader = new InputStreamReader(Objects.requireNonNull(NativeItems.class.getResourceAsStream("/assets/fossil/items.json")), StandardCharsets.UTF_8)) {
            names = new Gson().fromJson(reader, String[].class);
        } catch (Exception exception) { throw new IllegalStateException("Cannot load native item catalog", exception); }
        DNA_SPECIES.put(ModContent.DODO_DNA, "dodo");
        for (String name : names) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ModContent.id(name));
            Item.Properties properties = new Item.Properties().setId(key);
            Item item;
            String species = name.replaceFirst("^(egg_item_|syringe_|bucket_item_)", "");
            EntityType<? extends Mob> animal = animalType(species);
            if (NativeBlocks.TOYS.containsKey(name)) {
                item = new BlockItem(NativeBlocks.TOYS.get(name), properties);
            } else if ((name.startsWith("egg_item_") || name.startsWith("syringe_")) && animal != null) {
                item = new IncubationItem(species, properties);
                INCUBATION_ITEMS.put(species, item);
            } else if (name.startsWith("bucket_item_") && animal != null) {
                item = new MobBucketItem(animal, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, properties.stacksTo(1));
            } else if (name.equals("tar_bucket")) {
                item = new NativeTarBucket(properties);
            } else if (name.equals("fern_seed")) {
                item = new BlockItem(NativeBlocks.BLOCKS.get("plant_ferns"), properties);
            } else if (name.startsWith("seed_") && plant(name.substring(5)) != null) {
                item = new BlockItem(plant(name.substring(5)), properties);
            } else {
                if (name.startsWith("meat_") || name.equals("frozen_meat")) properties.food(new FoodProperties.Builder().nutrition(3).saturationModifier(.3f).build());
                if (name.startsWith("cooked_") || name.equals("chicken_soup_cooked") || name.startsWith("egg_") && !name.startsWith("egg_item_")) properties.food(new FoodProperties.Builder().nutrition(7).saturationModifier(.8f).build());
                if (name.startsWith("berry_") || name.equals("chicken_soup_raw")) properties.food(new FoodProperties.Builder().nutrition(3).saturationModifier(.4f).build());
                if (name.startsWith("music_disc_")) {
                    String music = name.replace("music_disc_", "music_");
                    Registry.register(BuiltInRegistries.SOUND_EVENT, ModContent.id(music), net.minecraft.sounds.SoundEvent.createVariableRangeEvent(ModContent.id(music)));
                    properties.stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG, ModContent.id(name)));
                }
                ToolMaterial material = name.startsWith("scarab_") ? ToolMaterial.DIAMOND : ToolMaterial.IRON;
                if (name.endsWith("_sword") && !name.startsWith("broken_") || name.equals("tooth_dagger") || name.equals("whip")) properties.sword(material, 3, -2.4f);
                if (name.endsWith("_pickaxe")) properties.pickaxe(material, 1, -2.8f);
                if (name.endsWith("_axe")) properties.axe(material, 4, -3);
                if (name.endsWith("_shovel")) properties.shovel(material, 1.5f, -3);
                if (name.endsWith("_hoe")) properties.hoe(material, -2, -1);
                if (name.endsWith("_javelin")) {
                    ToolMaterial spear = name.startsWith("diamond") ? ToolMaterial.DIAMOND : name.startsWith("stone") ? ToolMaterial.STONE
                            : name.startsWith("gold") ? ToolMaterial.GOLD : name.startsWith("wooden") || name.startsWith("ancient") ? ToolMaterial.WOOD : ToolMaterial.IRON;
                    properties.spear(spear, .65f, .7f, .75f, 5, 14, 10, 5.1f, 15, 4.6f);
                }
                if (name.startsWith("bone_") && List.of("helmet", "chestplate", "leggings", "boots").contains(name.substring(5)) || name.equals("ancient_helmet")) {
                    ArmorType type = name.endsWith("helmet") ? ArmorType.HELMET : name.endsWith("chestplate") ? ArmorType.CHESTPLATE : name.endsWith("leggings") ? ArmorType.LEGGINGS : ArmorType.BOOTS;
                    properties.humanoidArmor(name.startsWith("bone_") ? ArmorMaterials.CHAINMAIL : ArmorMaterials.IRON, type);
                }
                item = new NativeUtilityItem(name, properties);
            }
            Registry.register(BuiltInRegistries.ITEM, key, item);
            ITEMS.put(name, item);
            if (name.endsWith("_dna") && animalType(name.substring(0, name.length() - 4)) != null) DNA_SPECIES.put(item, name.substring(0, name.length() - 4));
        }
    }
}
