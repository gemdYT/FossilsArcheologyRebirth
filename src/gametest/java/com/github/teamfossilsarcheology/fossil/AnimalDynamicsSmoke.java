package com.github.teamfossilsarcheology.fossil;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.Vec3;

/** Regressions missed by the small-animal smoke: takeoff, sustained motion and giant hunters. */
final class AnimalDynamicsSmoke {
    private final List<NativeAnimal> animals = new ArrayList<>();
    private final List<Vec3> last = new ArrayList<>();
    private final double[] travel = new double[5];
    private NativeAnimal rex, shark, amphibian;
    private Mob cow, fish, shoreFish, shoreCow;
    private double takeoffHeight;
    private int movingSwimSamples, flyingSamples;
    int flyerId;
    private double animationTime = -1;

    void prepareAnimation(MinecraftServer server) {
        var flyer = animals.get(2);
        flyer.setPos(64, 116, -58); flyer.setDeltaMovement(Vec3.ZERO); flyer.setYRot(180);
        flyer.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new net.minecraft.world.entity.ai.memory.WalkTarget(new Vec3(64, 116, -74), .8f, 1));
        server.getPlayerList().getPlayers().getFirst().teleportTo(server.overworld(), 64, 114, -44, java.util.Set.of(), 180, 0, true);
    }
    void verifyAnimation(net.minecraft.client.Minecraft client) {
        var flyer = (NativeAnimal) client.level.getEntity(flyerId);
        if (flyer == null || !flyer.flyingNow()) throw new AssertionError("Flight state did not sync to client");
        var controller = flyer.getAnimatableInstanceCache().getManagerForId(flyerId).getAnimationControllers().get("movement");
        var clip = controller.getCurrentRawAnimation();
        if (clip == null || clip.getAnimationStages().stream().noneMatch(stage -> stage.animationName().equals("fly")))
            throw new AssertionError("Client renderer did not select flight animation: " + clip);
        double time = controller.getCurrentTimelineTime();
        if (animationTime >= 0 && time <= animationTime) throw new AssertionError("Flight animation timeline did not advance: " + animationTime + " -> " + time);
        animationTime = time;
        System.out.println("FOSSIL FLIGHT ANIMATION: client wing animation timeline=" + time);
    }
    void prepareAggression(MinecraftServer server, boolean aquatic) {
        var player = server.getPlayerList().getPlayers().getFirst();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.teleportTo(server.overworld(), aquatic ? 104 : 48, aquatic ? 115 : 110, aquatic ? -25 : -3, java.util.Set.of(), 180, 0, true);
        var predator = aquatic ? shark : rex;
        predator.setPos(aquatic ? 94 : 40, aquatic ? 115 : 110, aquatic ? -25 : -3);
        predator.getBrain().setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, server.overworld().getGameTime() - 400);
    }
    void verifyAggression(MinecraftServer server, boolean aquatic) {
        var player = server.getPlayerList().getPlayers().getFirst();
        var predator = aquatic ? shark : rex;
        if (predator.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) != player)
            throw new AssertionError("Wild adult did not threaten Survival player: " + predator.species().name()
                    + " alive=" + predator.isAlive() + " distance=" + predator.distanceTo(player) + " canHunt=" + predator.canHunt(player)
                    + " target=" + predator.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET) + " attacker=" + predator.getLastHurtByMob()
                    + " hunger=" + predator.hunger() + " ticks=" + predator.tickCount + " player=" + player.position()
                    + " oldPath=" + predator.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)
                    + " nearby=" + predator.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES));
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        System.out.println("FOSSIL AGGRESSION: " + predator.species().name() + " acquired Survival player");
    }
    void prepareShore(MinecraftServer server) {
        amphibian.setPos(123, 110, 1); amphibian.setDeltaMovement(Vec3.ZERO);
        shoreCow = EntityTypes.COW.create(server.overworld(), EntitySpawnReason.COMMAND);
        shoreCow.setPos(130, 110, 1); shoreCow.setNoAi(true); server.overworld().addFreshEntity(shoreCow);
        amphibian.hurtServer(server.overworld(), server.overworld().damageSources().mobAttack(shoreCow), 1);
    }
    void verifyShore() {
        if (shoreCow.getHealth() == shoreCow.getMaxHealth()) throw new AssertionError("Amphibious hunter did not resume land pursuit after swimming");
        System.out.println("FOSSIL AMPHIBIOUS: Sarcosuchus resumed land pursuit after swimming");
    }
    void prepareRiding(MinecraftServer server) {
        var player = server.getPlayerList().getPlayers().getFirst();
        player.teleportTo(server.overworld(), amphibian.getX(), amphibian.getY() + 1, amphibian.getZ(), java.util.Set.of(), 180, 0, true);
        player.setItemSlot(EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF, 16));
        for (int feed = 0; feed < 8; feed++) amphibian.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        player.setItemSlot(EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE));
        amphibian.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        player.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
        amphibian.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (amphibian.getControllingPassenger() != player) throw new AssertionError("Saddled amphibian could not be ridden");
    }
    void verifyRiding(MinecraftServer server) {
        if (amphibian.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) || amphibian.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET))
            throw new AssertionError("Animal AI competed with rider input");
        server.getPlayerList().getPlayers().getFirst().stopRiding();
        System.out.println("FOSSIL RIDING: rider control takes priority over autonomous movement/combat");
    }

    void setup(MinecraftServer server) {
        var level = server.overworld();
        for (var pos : BlockPos.betweenClosed(36, 109, -74, 132, 132, 4))
            level.setBlock(pos, pos.getY() == 109 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (var pos : BlockPos.betweenClosed(84, 110, -42, 132, 122, -6))
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
        rex = spawn(server, "tyrannosaurus", new Vec3(42, 110, -20));
        cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
        cow.setPos(56, 110, -20); cow.setNoAi(true); level.addFreshEntity(cow);
        shark = spawn(server, "megalodon", new Vec3(94, 114, -25));
        fish = EntityTypes.COD.create(level, EntitySpawnReason.COMMAND);
        fish.setPos(111, 114, -25); fish.setNoAi(true); level.addFreshEntity(fish);
        var flyer = spawn(server, "quetzalcoatlus", new Vec3(64, 110, -58));
        flyerId = flyer.getId();
        var swimmer = spawn(server, "nautilus", new Vec3(112, 113, -12));
        amphibian = spawn(server, "sarcosuchus", new Vec3(124, 114, -36));
        shoreFish = EntityTypes.COD.create(level, EntitySpawnReason.COMMAND);
        shoreFish.setPos(124, 114, -23); shoreFish.setNoAi(true); level.addFreshEntity(shoreFish);
        animals.addAll(List.of(rex, shark, flyer, swimmer, amphibian));
        for (var animal : animals) last.add(animal.position());
        server.getPlayerList().getPlayers().getFirst().teleportTo(level, 64, 114, -44, java.util.Set.of(), 180, 0, true);
    }
    private NativeAnimal spawn(MinecraftServer server, String name, Vec3 pos) {
        var animal = NativeAnimals.TYPES.get(name).create(server.overworld(), EntitySpawnReason.COMMAND);
        animal.setPos(pos); server.overworld().addFreshEntity(animal); return animal;
    }
    void sample() {
        for (int i = 0; i < animals.size(); i++) {
            Vec3 at = animals.get(i).position(); travel[i] += at.distanceTo(last.get(i)); last.set(i, at);
        }
        var flyer = animals.get(2);
        takeoffHeight = Math.max(takeoffHeight, flyer.getY() - 110);
        if (flyer.flyingNow() && flyer.movementAnimation().equals("fly")) flyingSamples++;
        if (animals.get(3).getDeltaMovement().horizontalDistance() > .025) movingSwimSamples++;
    }
    void verify() {
        if (cow.isAlive()) throw new AssertionError("Giant land predator did not hunt naturally: target=" + rex.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET) + " travel=" + travel[0]);
        if (fish.isAlive()) throw new AssertionError("Giant aquatic predator did not hunt naturally: target=" + shark.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET) + " travel=" + travel[1]);
        if (takeoffHeight < 3 || flyingSamples < 4 || travel[2] < 12) throw new AssertionError("Grounded flyer did not take off and sustain flight: rise=" + takeoffHeight + " samples=" + flyingSamples + " travel=" + travel[2]);
        if (movingSwimSamples < 10 || travel[3] < 6) throw new AssertionError("Swimmer did not cruise continuously: samples=" + movingSwimSamples + " travel=" + travel[3] + " ticks=" + animals.get(3).tickCount + " alive=" + animals.get(3).isAlive() + " walk=" + animals.get(3).getBrain().getMemory(MemoryModuleType.WALK_TARGET));
        if (shoreFish.isAlive() || travel[4] < 6) throw new AssertionError("Amphibious hunter did not swim and hunt: travel=" + travel[4]);
        System.out.println("FOSSIL DYNAMICS: large land/water hunting, grounded takeoff and sustained cruising passed; travel=" + java.util.Arrays.toString(travel));
    }
    void cleanup() { for (var animal : animals) animal.discard(); cow.discard(); fish.discard(); shoreFish.discard(); if (shoreCow != null) shoreCow.discard(); }
}
