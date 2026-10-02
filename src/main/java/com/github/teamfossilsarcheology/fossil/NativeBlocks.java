package com.github.teamfossilsarcheology.fossil;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Use native block families rather than maintaining parallel catalog implementations. */
public final class NativeBlocks {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final Map<String, Block> TOYS = new LinkedHashMap<>();
    static final ThreadLocal<BlockProfile> CONSTRUCTING = new ThreadLocal<>();
    private NativeBlocks() {}
    public static void initialize() {
        BlockProfile[] profiles;
        try (var stream = Objects.requireNonNull(NativeBlocks.class.getResourceAsStream("/assets/fossil/blocks.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            profiles = new Gson().fromJson(reader, BlockProfile[].class);
        } catch (Exception exception) { throw new IllegalStateException("Cannot load catalog block catalog", exception); }
        for (BlockProfile profile : profiles) {
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ModContent.id(profile.name()));
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(key).strength(2.5f)
                    .sound(profile.sound().equals("wood") ? SoundType.WOOD : profile.sound().equals("grass") ? SoundType.GRASS : SoundType.STONE)
                    .lightLevel(state -> profile.light());
            if (!profile.kind().equals("solid") && !profile.kind().equals("fossil")) properties.noOcclusion();
            if (profile.kind().contains("plant") || profile.kind().equals("sapling") || profile.kind().equals("vine")) properties.noCollision().randomTicks();
            if (profile.kind().equals("leaves")) properties.randomTicks();
            if (profile.kind().equals("fossil") || profile.kind().equals("ore")) properties.requiresCorrectToolForDrops();
            if (profile.name().equals("ash_vent") || profile.name().equals("bubble_blower")) properties.randomTicks();
            if (profile.kind().equals("tar")) properties.noCollision().speedFactor(.2f).jumpFactor(.3f);
            CONSTRUCTING.set(profile);
            Block block;
            try { block = construct(profile, properties); } finally { CONSTRUCTING.remove(); }
            Registry.register(BuiltInRegistries.BLOCK, key, block);
            BLOCKS.put(profile.name(), block);
            if (profile.kind().equals("storage")) ((FabricBlockEntityType) BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("barrel"))).addValidBlock(block);
            if (profile.kind().equals("machine")) ((FabricBlockEntityType) ModContent.MACHINE).addValidBlock(block);
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ModContent.id(profile.name()));
            ITEMS.put(profile.name(), Registry.register(BuiltInRegistries.ITEM, itemKey,
                    new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix())));
        }
        try (var reader = new InputStreamReader(Objects.requireNonNull(NativeBlocks.class.getResourceAsStream("/assets/fossil/items.json")), StandardCharsets.UTF_8)) {
            for (String name : new Gson().fromJson(reader, String[].class)) if (name.startsWith("toy_")) {
                var key = ResourceKey.create(Registries.BLOCK, ModContent.id(name));
                TOYS.put(name, Registry.register(BuiltInRegistries.BLOCK, key, new NativeToyBlock(BlockBehaviour.Properties.of().setId(key).strength(1).noOcclusion().randomTicks().sound(SoundType.WOOD))));
            }
        } catch (Exception e) { throw new IllegalStateException("Cannot register enrichment blocks", e); }
    }
    private static Block construct(BlockProfile profile, BlockBehaviour.Properties properties) {
        return switch (profile.kind()) {
            case "solid", "fossil", "ore" -> new Block(properties);
            case "pillar" -> new RotatedPillarBlock(properties);
            case "stairs" -> new NativeStairs(properties);
            case "slab" -> new SlabBlock(properties);
            case "fence" -> new FenceBlock(properties);
            case "gate" -> new FenceGateBlock(WoodType.OAK, properties);
            case "door" -> new NativeDoor(properties);
            case "trapdoor" -> new NativeTrapdoor(properties);
            case "button" -> new NativeButton(properties);
            case "pressure" -> new NativePressurePlate(properties);
            case "wall" -> new WallBlock(properties);
            case "leaves" -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties);
            case "glass" -> new NativeDecorBlock(properties);
            case "bars" -> new NativeBars(properties);
            case "bed" -> new BedBlock(DyeColor.WHITE, properties);
            case "sand" -> new NativeSand(properties);
            case "vine" -> new VineBlock(properties);
            case "rail" -> new NativeRail(properties);
            case "storage" -> new BarrelBlock(properties);
            case "machine" -> new NativeMachineBlock(properties);
            case "tar" -> new NativeTarBlock(properties);
            case "barrier" -> new BarrierBlock(properties);
            default -> new NativeDecorBlock(properties);
        };
    }
    private static final class NativeStairs extends StairBlock { NativeStairs(BlockBehaviour.Properties p) { super(Blocks.OAK_PLANKS.defaultBlockState(), p); } }
    private static final class NativeDoor extends DoorBlock { NativeDoor(BlockBehaviour.Properties p) { super(BlockSetType.OAK, p); } }
    private static final class NativeTrapdoor extends TrapDoorBlock { NativeTrapdoor(BlockBehaviour.Properties p) { super(BlockSetType.OAK, p); } }
    private static final class NativeButton extends ButtonBlock { NativeButton(BlockBehaviour.Properties p) { super(BlockSetType.OAK, 30, p); } }
    private static final class NativePressurePlate extends PressurePlateBlock { NativePressurePlate(BlockBehaviour.Properties p) { super(BlockSetType.OAK, p); } }
    private static final class NativeBars extends IronBarsBlock { NativeBars(BlockBehaviour.Properties p) { super(p); } }
    private static final class NativeRail extends RailBlock { NativeRail(BlockBehaviour.Properties p) { super(p); } }
    private static final class NativeSand extends FallingBlock {
        NativeSand(BlockBehaviour.Properties p) { super(p); }
        @Override public int getDustColor(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) { return 0x8C765C; }
    }
}
