package com.github.teamfossilsarcheology.fossil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/** A bounded volcanic cone, with a contained lava crater and smoke vents. */
public record VolcanoFeature(int radius, int height) implements Feature {
    public static final MapCodec<VolcanoFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(4, 12).fieldOf("radius").forGetter(VolcanoFeature::radius),
            Codec.intRange(6, 24).fieldOf("height").forGetter(VolcanoFeature::height)).apply(i, VolcanoFeature::new));
    @Override public MapCodec<? extends Feature> codec() { return CODEC; }
    @Override public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int base = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ());
        if (base + height + 2 >= level.getMaxY()) return false;
        var rock = NativeBlocks.BLOCKS.get("volcanic_rock").defaultBlockState();
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            double distance = Math.sqrt(x * x + z * z);
            if (distance > radius) continue;
            int rise = distance <= 2 ? height - 2 : distance <= 3 ? height : Math.max(1, (int)Math.round(height * (1 - (distance - 2) / (radius - 1))));
            int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + x, origin.getZ() + z);
            for (int y = Math.min(base, ground) - 1; y <= base + rise; y++) setBlock(level, new BlockPos(origin.getX() + x, y, origin.getZ() + z), rock);
            if (distance <= 2) {
                for (int y = base + height - 1; y <= base + height + 2; y++) setBlock(level, new BlockPos(origin.getX() + x, y, origin.getZ() + z), Blocks.AIR.defaultBlockState());
                setBlock(level, new BlockPos(origin.getX() + x, base + height - 1, origin.getZ() + z), Blocks.LAVA.defaultBlockState());
            } else if (distance >= radius - 2 && random.nextInt(10) == 0) {
                setBlock(level, new BlockPos(origin.getX() + x, base + rise + 1, origin.getZ() + z), NativeBlocks.BLOCKS.get("ash_vent").defaultBlockState());
            }
        }
        return true;
    }
}
