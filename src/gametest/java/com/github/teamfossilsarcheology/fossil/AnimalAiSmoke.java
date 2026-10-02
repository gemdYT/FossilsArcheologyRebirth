package com.github.teamfossilsarcheology.fossil;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Exercise real server ticks and navigation, including skeleton-first brain initialization. */
final class AnimalAiSmoke {
    private record Sample(NativeAnimal animal, Vec3 origin) {}
    private final List<Sample> animals = new ArrayList<>();
    private final double[] movement = new double[4];
    private NativeAnimal follower, predator, prey;
    private double followingDistance;
    private Vec3 stayPosition;

    void setup(MinecraftServer server) {
        var level = server.overworld();
        for (var pos : BlockPos.betweenClosed(-24, 109, -44, 30, 125, -10))
            level.setBlock(pos, pos.getY() == 109 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (var pos : BlockPos.betweenClosed(12, 110, -36, 28, 115, -20))
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        // A display must not poison the memory schema of subsequently created live animals.
        var display = NativeAnimals.DISPLAYS.get("dodo").create(level, EntitySpawnReason.COMMAND);
        display.discard();
        String[] names = {"dodo", "triceratops", "coelacanth", "pteranodon"};
        Vec3[] positions = {new Vec3(-16, 110, -30), new Vec3(-4, 110, -30), new Vec3(20, 112, -28), new Vec3(0, 120, -24)};
        for (int i = 0; i < names.length; i++) {
            var animal = NativeAnimals.TYPES.get(names[i]).create(level, EntitySpawnReason.COMMAND);
            animal.setPos(positions[i]);
            level.addFreshEntity(animal);
            animals.add(new Sample(animal, positions[i]));
            System.out.println("FOSSIL AI START " + names[i] + " noAi=" + animal.isNoAi() + " pathMemory=" + animal.getBrain().checkMemory(MemoryModuleType.PATH, net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED));
        }
    }

    void sample() {
        for (int i = 0; i < animals.size(); i++) {
            var sample = animals.get(i);
            double distance = sample.animal.position().subtract(sample.origin).horizontalDistanceSqr();
            movement[i] = Math.max(movement[i], distance);
        }
    }

    void verify() {
        for (int i = 0; i < animals.size(); i++) {
            var animal = animals.get(i).animal;
            System.out.println("FOSSIL AI MOVEMENT " + animal.species().name() + " distance=" + Math.sqrt(movement[i]) + " active=" + animal.getBrain().getActiveActivities() + " running=" + animal.getBrain().getRunningBehaviors());
            if (!animal.isAlive() || movement[i] < 1) throw new AssertionError("Spawned animal did not navigate: " + animal.species().name());
            if (i > 0) animal.discard();
        }
    }

    void beginFollowing(MinecraftServer server) {
        var player = server.getPlayerList().getPlayers().getFirst();
        follower = animals.getFirst().animal;
        follower.setPos(-20, 110, -30);
        player.teleportTo(server.overworld(), -8, 110, -30, java.util.Set.of(), 180, 20, true);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT, 8));
        for (int i = 0; i < 4; i++) follower.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
        if (!follower.ownedBy(player)) throw new AssertionError("Follow test taming failed");
        followingDistance = follower.distanceTo(player);
    }

    void verifyFollowingAndStay(MinecraftServer server) {
        var player = server.getPlayerList().getPlayers().getFirst();
        if (follower.distanceTo(player) >= followingDistance - 2) throw new AssertionError("Owned animal did not follow its owner");
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(NativeItems.get("whip")));
        follower.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!follower.staying()) throw new AssertionError("Stay command failed");
    }

    void recordStay() { stayPosition = follower.position(); }
    void verifyStay() {
        if (follower.position().subtract(stayPosition).horizontalDistanceSqr() > .02) throw new AssertionError("Stay command did not stop movement");
        follower.discard();
    }

    void beginCombat(MinecraftServer server) {
        var level = server.overworld();
        predator = NativeAnimals.TYPES.get("allosaurus").create(level, EntitySpawnReason.COMMAND);
        prey = NativeAnimals.TYPES.get("dodo").create(level, EntitySpawnReason.COMMAND);
        predator.setPos(-6, 110, -16); prey.setPos(6, 110, -16); prey.setNoAi(true);
        level.addFreshEntity(predator); level.addFreshEntity(prey);
        predator.hurtServer(level, level.damageSources().mobAttack(prey), 1);
    }

    void verifyCombat() {
        if (prey.getHealth() == prey.getMaxHealth()) throw new AssertionError("Predator did not chase and attack its attacker");
        predator.discard(); prey.discard();
        System.out.println("FOSSIL AI: wandering, swimming, flying, owner following, Stay and predator pursuit/damage passed");
    }
}
