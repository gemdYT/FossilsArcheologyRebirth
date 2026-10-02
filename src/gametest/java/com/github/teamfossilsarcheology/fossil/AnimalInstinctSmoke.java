package com.github.teamfossilsarcheology.fossil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

/** Representative encounters: autonomous selection, diet, fleeing, defense and pursuit cleanup. */
final class AnimalInstinctSmoke {
    private NativeAnimal hunter, meal, spare, fishHunter, timid, threat, defender;
    private Mob fish, attacker;
    private double fleeingDistance;
    private float healthAfterOwnerHit;

    private NativeAnimal spawn(MinecraftServer server, String name, double x, double y, double z) {
        var animal = NativeAnimals.TYPES.get(name).create(server.overworld(), EntitySpawnReason.COMMAND);
        animal.setPos(x, y, z); server.overworld().addFreshEntity(animal); return animal;
    }
    void beginHunting(MinecraftServer server) {
        hunter = spawn(server, "allosaurus", -18, 110, -30);
        meal = spawn(server, "dodo", -10, 110, -30); meal.setNoAi(true);
        spare = spawn(server, "dodo", -1, 110, -30); spare.setNoAi(true);
        fishHunter = spawn(server, "alligator_gar", 18, 112, -28);
        fish = EntityTypes.COD.create(server.overworld(), EntitySpawnReason.COMMAND);
        fish.setPos(24, 112, -28); fish.setNoAi(true); server.overworld().addFreshEntity(fish);
    }
    void verifyHunting() {
        if (meal.isAlive() || hunter.hunger() < 95) throw new AssertionError("Land predator did not find, kill and eat prey on its own");
        if (spare.getHealth() != spare.getMaxHealth()) throw new AssertionError("Sated predator kept hunting");
        if (fish.isAlive() || fishHunter.hunger() < 95) throw new AssertionError("Piscivore did not find and eat vanilla fish");
        hunter.discard(); meal.discard(); spare.discard(); fishHunter.discard(); fish.discard();
        System.out.println("FOSSIL INSTINCTS: autonomous land/fish hunting and eating passed");
    }
    void beginDefense(MinecraftServer server) {
        timid = spawn(server, "dodo", -18, 110, -30);
        threat = spawn(server, "allosaurus", -11, 110, -30); threat.setNoAi(true);
        fleeingDistance = timid.distanceTo(threat);
        defender = spawn(server, "triceratops", -4, 110, -16);
        attacker = EntityTypes.ZOMBIE.create(server.overworld(), EntitySpawnReason.COMMAND);
        attacker.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        attacker.setPos(4, 110, -16); attacker.setNoAi(true); server.overworld().addFreshEntity(attacker);
        defender.hurtServer(server.overworld(), server.overworld().damageSources().mobAttack(attacker), 1);
    }
    void verifyDefense() {
        if (!timid.isAlive() || timid.distanceTo(threat) < fleeingDistance + 2) throw new AssertionError("Timid animal did not flee a nearby predator");
        if (attacker.getHealth() == attacker.getMaxHealth()) throw new AssertionError("Defensive herbivore did not fight its attacker");
        timid.discard(); threat.discard(); defender.discard(); attacker.discard();
        System.out.println("FOSSIL INSTINCTS: proactive fleeing and herbivore defense passed");
    }
    void beginOwnerDefense(MinecraftServer server) {
        var player = server.getPlayerList().getPlayers().getFirst();
        player.teleportTo(server.overworld(), -8, 110, -30, java.util.Set.of(), 180, 20, true);
        hunter = spawn(server, "allosaurus", -16, 110, -30);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BEEF, 8));
        for (int i = 0; i < 4; i++) hunter.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (!hunter.ownedBy(player)) throw new AssertionError("Owner-defense taming failed");
        spare = spawn(server, "dodo", -10, 110, -30); spare.setNoAi(true);
        attacker = EntityTypes.ZOMBIE.create(server.overworld(), EntitySpawnReason.COMMAND);
        attacker.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        attacker.setPos(-7, 110, -25); attacker.setNoAi(true); server.overworld().addFreshEntity(attacker);
        player.attack(attacker);
        healthAfterOwnerHit = attacker.getHealth();
    }
    void verifyOwnerDefense() {
        if (attacker.getHealth() >= healthAfterOwnerHit) throw new AssertionError("Owned predator did not assist its owner's attack");
        if (spare.getHealth() != spare.getMaxHealth()) throw new AssertionError("Following pet attacked unrelated prey");
        hunter.discard(); spare.discard(); attacker.discard();
        System.out.println("FOSSIL INSTINCTS: owner assistance and Follow restraint passed");
    }
    void beginPursuit(MinecraftServer server) {
        hunter = spawn(server, "velociraptor", -18, 110, -30);
        meal = spawn(server, "dodo", -6, 110, -30); meal.setNoAi(true);
        spare = spawn(server, "velociraptor", -20, 110, -28); spare.setNoAi(true);
    }
    void escape() {
        if (hunter.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) != meal)
            throw new AssertionError("Small predator did not choose prey over its own species");
        meal.setPos(new Vec3(100, 110, -30));
    }
    void verifyPursuit() {
        if (hunter.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) throw new AssertionError("Predator kept chasing escaped prey");
        if (spare.getHealth() != spare.getMaxHealth()) throw new AssertionError("Predator attacked its own species");
        hunter.discard(); meal.discard(); spare.discard();
        System.out.println("FOSSIL INSTINCTS: prey selection, conspecific safety and pursuit abandonment passed");
    }
}
