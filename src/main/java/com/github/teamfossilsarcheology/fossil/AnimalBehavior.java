package com.github.teamfossilsarcheology.fossil;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** Small shared instincts; diet, size, movement and temperament come from species data. */
final class AnimalBehavior {
    private final NativeAnimal animal;
    private LivingEntity pursuit, ignoredTarget;
    private int pursuitStarted, lastVisible, ignoreUntil;
    private boolean hunting, directedMovement;

    AnimalBehavior(NativeAnimal animal) { this.animal = animal; }

    private boolean validTarget(LivingEntity target) {
        if (target == null || target == animal || !target.isAlive() || target.isRemoved()) return false;
        if (target instanceof Player player && (animal.ownedBy(player) || player.isCreative() || player.isSpectator())) return false;
        if (target instanceof OwnableEntity pet && pet.getOwnerReference() != null
                && pet.getOwnerReference().getUUID().equals(animal.ownerId())) return false;
        if (target instanceof NativeAnimal other && (other.isDisplay()
                || animal.ownerId() != null && animal.ownerId().equals(other.ownerId()))) return false;
        return !animal.species().aquatic() || target.isInWater();
    }

    boolean canHunt(LivingEntity target) {
        if (animal.isDisplay() || animal.staying() || animal.isBaby() || animal.isInLove()
                || animal.followingOwner() || animal.directedTarget() != null
                || !validTarget(target) || temporarilyIgnored(target)) return false;
        if (target instanceof Player)
            return !animal.domesticated() && animal.species().aggressive() && animal.distanceToSqr(target) <= 144;
        if (!animal.species().carnivore() || animal.hunger() >= 95) return false;
        if (target instanceof NativeAnimal other && (other.getType() == animal.getType() || other.domesticated())) return false;
        if (target instanceof OwnableEntity pet && pet.getOwnerReference() != null) return false;
        boolean fish = target instanceof AbstractFish || target instanceof NativeAnimal other && other.species().amphibious();
        boolean fishOnly = animal.species().diet().equals("PISCIVORE");
        if (fishOnly ? !fish
                : !(target instanceof Animal) && !(animal.species().diet().contains("PISCI") && fish)) return false;
        return target.getMaxHealth() <= animal.getMaxHealth() * (fishOnly ? .75 : 1.25)
                && target.getBbWidth() <= animal.getBbWidth() * (fishOnly ? .9 : 1.3);
    }

    boolean canKeepAttacking(LivingEntity target) {
        if (hunting && target instanceof Player && (animal.domesticated() || !animal.species().aggressive())) return false;
        if (hunting && !(target instanceof Player) && !canHunt(target)) return false;
        return !animal.staying() && !animal.isBaby() && !animal.isInLove() && validTarget(target)
                && target == pursuit && animal.distanceToSqr(target) <= 1024
                && animal.tickCount - pursuitStarted < 400 && animal.tickCount - lastVisible < 80;
    }

    boolean canWander() { return !directedMovement && !animal.staying() && !animal.isInLove(); }
    private boolean temporarilyIgnored(LivingEntity target) { return target == ignoredTarget && animal.tickCount < ignoreUntil; }
    private boolean fightsBack() {
        return !animal.isBaby() && animal.getHealth() > animal.getMaxHealth() * .25
                && (animal.species().carnivore() || animal.species().defensive());
    }

    void update() {
        directedMovement = false;
        if (animal.staying()) { stopPursuit(false); animal.getNavigation().stop(); animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET); return; }
        if (animal.isInLove()) { if (pursuit != null) stopPursuit(false); return; }
        LivingEntity current = animal.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (current != null) {
            if (animal.hasLineOfSight(current)) lastVisible = animal.tickCount;
            if (!canKeepAttacking(current)) stopPursuit(true);
        } else if (pursuit != null) stopPursuit(true);

        List<LivingEntity> nearby = animal.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).orElse(List.of());
        LivingEntity attacker = animal.getLastHurtByMob();
        if (validTarget(attacker) && !temporarilyIgnored(attacker) && animal.distanceToSqr(attacker) < 576
                && animal.tickCount - animal.getLastHurtByMobTimestamp() < 200) {
            if (fightsBack()) attack(attacker, false); else flee(attacker);
            return;
        }

        Player owner = animal.ownerPlayer();
        if (owner != null && fightsBack() && animal.distanceToSqr(owner) < 256) {
            LivingEntity danger = owner.getLastHurtByMob();
            if (owner.tickCount - owner.getLastHurtByMobTimestamp() >= 200) danger = null;
            if (danger == null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100) danger = owner.getLastHurtMob();
            if (validTarget(danger) && !temporarilyIgnored(danger) && animal.distanceToSqr(danger) < 576 && animal.hasLineOfSight(danger)) {
                attack(danger, false); return;
            }
        }

        LivingEntity threat = nearby.stream().filter(e -> e instanceof NativeAnimal predator
                && predator.canHunt(animal) && animal.distanceToSqr(e) < 64 && animal.hasLineOfSight(e))
                .min(java.util.Comparator.comparingDouble(animal::distanceToSqr)).orElse(null);
        if (threat != null && !temporarilyIgnored(threat)) {
            if (animal.species().defensive() && fightsBack()) attack(threat, false); else flee(threat);
            return;
        }
        if (pursuit != null) return;

        if (animal.directedTarget() != null) { walk(Vec3.atBottomCenterOf(animal.directedTarget()), 1.1f); return; }
        if (animal.followingOwner() && owner != null && animal.distanceToSqr(owner) > 9) { approach(owner, 1.1f); return; }

        LivingEntity foodHolder = nearby.stream().filter(e -> e instanceof Player p
                && (animal.isFood(p.getMainHandItem()) || animal.isFood(p.getOffhandItem())) && animal.distanceToSqr(p) < 100)
                .min(java.util.Comparator.comparingDouble(animal::distanceToSqr)).orElse(null);
        if (foodHolder != null) {
            if (animal.distanceToSqr(foodHolder) > 4) approach(foodHolder, 1);
            else { look(foodHolder); directedMovement = true; animal.getNavigation().stop(); animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET); }
            return;
        }

        LivingEntity prey = nearby.stream().filter(e -> canHunt(e) && animal.hasLineOfSight(e))
                .min(java.util.Comparator.comparingDouble(animal::distanceToSqr)).orElse(null);
        if (prey != null) { attack(prey, true); return; }

        LivingEntity herd = nearby.stream().filter(e -> e.getType() == animal.getType() && e instanceof Animal a && !a.isBaby())
                .min(java.util.Comparator.comparingDouble(animal::distanceToSqr)).orElse(null);
        if (herd != null && animal.distanceToSqr(herd) > (animal.isBaby() ? 9 : 100)) approach(herd, 1);
    }

    private void attack(LivingEntity target, boolean forFood) {
        if (pursuit != target) { pursuit = target; pursuitStarted = animal.tickCount; lastVisible = animal.tickCount; }
        if (animal.hasLineOfSight(target)) lastVisible = animal.tickCount;
        hunting = forFood;
        animal.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        look(target);
        directedMovement = true;
    }
    private void stopPursuit(boolean ignore) {
        if (ignore && pursuit != null) { ignoredTarget = pursuit; ignoreUntil = animal.tickCount + 120; }
        pursuit = null;
        animal.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        animal.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        animal.getNavigation().stop();
    }
    private void flee(LivingEntity threat) {
        if (pursuit != null) stopPursuit(false);
        Vec3 away = DefaultRandomPos.getPosAway(animal, 12, 5, threat.position());
        if (away == null) away = animal.position().add(animal.position().subtract(threat.position()).normalize().scale(8));
        walk(away, 1.4f);
    }
    private void approach(LivingEntity target, float speed) { look(target); walk(target.position(), speed); }
    private void look(LivingEntity target) { animal.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true)); }
    private void walk(Vec3 position, float speed) {
        directedMovement = true;
        animal.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(position, speed, 2));
    }
}
