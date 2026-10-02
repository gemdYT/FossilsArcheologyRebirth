package com.github.teamfossilsarcheology.fossil;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.Set;
import java.util.function.Function;

public final class ModContent {
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath("fossil", path); }

    public static final EntityType<NativeAnimal> DODO = Registry.register(BuiltInRegistries.ENTITY_TYPE, id("dodo"),
            EntityType.Builder.of(NativeAnimal::new, MobCategory.CREATURE).sized(0.85f, 1.0f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id("dodo"))));
    public static final Item BIO_FOSSIL = item("bio_fossil", Item::new);
    public static final Item BIO_GOO = item("bio_goo", Item::new);
    public static final Item DODO_DNA = item("dodo_dna", Item::new);
    public static final Block FOSSIL_ORE = block("fossil_ore", properties -> new DropExperienceBlock(UniformInt.of(1, 3),
            properties.strength(3f).requiresCorrectToolForDrops().sound(SoundType.STONE).mapColor(MapColor.STONE)));
    public static final MachineBlock ANALYZER = block("analyzer", MachineBlock::new);
    public static final MachineBlock CULTURE_VAT = block("culture_vat", MachineBlock::new);
    public static final MachineBlock DODO_EGG = block("dodo_egg", properties -> new MachineBlock(properties.strength(0.5f).noOcclusion()));
    public static final BlockEntityType<MachineBlockEntity> MACHINE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("revival_machine"),
            new BlockEntityType<>(MachineBlockEntity::new, Set.of(ANALYZER, CULTURE_VAT, DODO_EGG)));
    public static final Item FOSSIL_ORE_ITEM = blockItem("fossil_ore", FOSSIL_ORE);
    public static final Item ANALYZER_ITEM = blockItem("analyzer", ANALYZER);
    public static final Item CULTURE_VAT_ITEM = blockItem("culture_vat", CULTURE_VAT);
    public static final Item DODO_EGG_ITEM = blockItem("dodo_egg", DODO_EGG);
    public static final Item DODO_SPAWN_EGG = item("spawn_egg_dodo", properties -> new SpawnEggItem(properties.spawnEgg(DODO)));
    public static final SoundEvent DODO_AMBIENT = sound("dodo_ambient");
    public static final SoundEvent DODO_HURT = sound("dodo_hurt");
    public static final SoundEvent DODO_DEATH = sound("dodo_death");

    public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("fa_mob_item_tab"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0).title(Component.translatable("itemGroup.fossil"))
                    .icon(() -> new ItemStack(BIO_FOSSIL)).displayItems((parameters, output) -> {
                        output.accept(BIO_FOSSIL); output.accept(BIO_GOO); output.accept(DODO_DNA);
                        output.accept(ANALYZER_ITEM); output.accept(CULTURE_VAT_ITEM); output.accept(DODO_EGG_ITEM);
                        output.accept(FOSSIL_ORE_ITEM); output.accept(DODO_SPAWN_EGG);
                        NativeAnimals.SPAWN_EGGS.values().forEach(output::accept);
                        NativeBlocks.ITEMS.values().forEach(output::accept);
                        NativeItems.ITEMS.values().forEach(output::accept);
                        NativeHostiles.EGGS.values().forEach(output::accept);
                    }).build());

    private static <T extends Item> T item(String name, Function<Item.Properties, T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
    private static Item blockItem(String name, Block block) {
        return item(name, properties -> new BlockItem(block, properties.useBlockDescriptionPrefix()));
    }
    private static <T extends Block> T block(String name, Function<BlockBehaviour.Properties, T> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id(name));
        return Registry.register(BuiltInRegistries.BLOCK, key,
                factory.apply(BlockBehaviour.Properties.of().setId(key).strength(2.5f).sound(SoundType.METAL)));
    }
    private static SoundEvent sound(String name) {
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id(name), SoundEvent.createVariableRangeEvent(id(name)));
    }
    public static void initialize() { /* Initialize the base registries before the catalogs. */ }
}
