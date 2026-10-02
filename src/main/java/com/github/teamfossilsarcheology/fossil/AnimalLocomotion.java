package com.github.teamfossilsarcheology.fossil;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Inertial air/water steering with body-sized collision probes and persistent cruise legs. */
final class AnimalLocomotion extends MoveControl<NativeAnimal> {
    private int restingUntil, nextProbe;
    private Vec3 origin, avoidance;
    private boolean landing;

    AnimalLocomotion(NativeAnimal animal) { super(animal); }
    static boolean spatial(NativeAnimal animal) { return animal.species().flying() || animal.species().aquatic() || animal.species().amphibious() && animal.isInWater(); }

    void cruise() {
        if (mob.tickCount < restingUntil || mob.staying()) return;
        var target = mob.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElse(null);
        if (target != null && mob.position().distanceToSqr(target.getTarget().currentPosition()) > 4 && !mob.horizontalCollision) return;
        if (origin == null || mob.position().distanceToSqr(origin) > 6400) origin = mob.position();
        landing = mob.species().flying() && mob.tickCount > 100 && mob.tickCount % 600 > 440;
        for (int attempt = 0; attempt < 12; attempt++) {
            double heading = Math.toRadians(mob.getYRot() + (mob.getRandom().nextDouble() - .5) * (attempt < 6 ? 100 : 360));
            double distance = attempt < 6 ? Math.max(mob.getBbWidth() * 2, 6) + mob.getRandom().nextDouble() * 10
                    : Math.max(mob.getBbWidth(), 2) + mob.getRandom().nextDouble() * 3;
            Vec3 candidate = mob.position().add(-Math.sin(heading) * distance, (mob.getRandom().nextDouble() - .5) * 6, Math.cos(heading) * distance);
            if (mob.species().flying()) {
                int surface = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(candidate.x), Mth.floor(candidate.z));
                double altitude = landing ? surface + .05 : Math.max(surface + 4, Math.min(candidate.y, surface + mob.species().flightHeight()));
                candidate = new Vec3(candidate.x, altitude, candidate.z);
            }
            if (candidate.distanceToSqr(origin) > 3600) candidate = origin.add(0, mob.species().flying() ? 5 : 0, 0);
            if (mob.species().amphibious() && !water(candidate)) continue;
            if (clear(mob.position(), candidate)) {
                mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(candidate, mob.species().cruiseSpeed(), 1));
                return;
            }
        }
        // Smaller legs let confined fish turn away from walls rather than circle one point.
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override public void tick() {
        mob.setXxa(0); mob.setYya(0); mob.setZza(0);
        if (mob.getControllingPassenger() != null) {
            mob.setNoGravity(mob.isInWater()); return;
        }
        if (mob.species().amphibious() && !mob.species().aquatic() && !mob.isInWater()) {
            mob.setNoGravity(false); super.tick(); return;
        }
        if (mob.species().aquatic() && !mob.isInWater()) { mob.setNoGravity(false); return; }
        var attack = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        var walk = mob.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElse(null);
        Vec3 goal = attack != null ? new Vec3(attack.getX(), attack.getY() + Math.max(0, (attack.getBbHeight() - mob.getBbHeight()) * .5), attack.getZ())
                : walk == null ? null : walk.getTarget().currentPosition();
        if (mob.staying() || mob.tickCount < restingUntil || goal == null) {
            mob.setSpeed(0);
            mob.setDeltaMovement(mob.getDeltaMovement().scale(mob.species().flying() ? .9 : .75));
            // A stopped flyer descends to a perch instead of hovering forever.
            mob.setNoGravity(mob.species().aquatic() && mob.isInWater());
            mob.setFlyingState(mob.species().flying() && !mob.onGround());
            return;
        }
        double distance = mob.position().distanceTo(goal);
        if (attack == null && distance < Math.max(1, mob.getBbWidth() * .35)) {
            mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            if (landing && mob.species().flying()) { restingUntil = mob.tickCount + 100; landing = false; }
            return;
        }
        mob.setNoGravity(true);
        mob.setFlyingState(mob.species().flying());
        Vec3 direction = goal.subtract(mob.position()).normalize();
        if (mob.tickCount >= nextProbe || mob.horizontalCollision || mob.verticalCollision) {
            nextProbe = mob.tickCount + 5;
            avoidance = null;
            double ahead = Math.max(2, mob.getBbWidth());
            if (!clear(mob.position(), mob.position().add(direction.scale(ahead)))) {
                for (double turn : new double[]{45, -45, 90, -90, 150, -150}) {
                    double angle = Math.atan2(direction.z, direction.x) + Math.toRadians(turn);
                    Vec3 alternative = new Vec3(Math.cos(angle), mob.species().flying() ? .4 : direction.y * .5, Math.sin(angle)).normalize();
                    if (clear(mob.position(), mob.position().add(alternative.scale(ahead)))) { avoidance = alternative; break; }
                }
                if (avoidance == null) { mob.setDeltaMovement(mob.getDeltaMovement().scale(.4)); mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET); return; }
            }
        }
        if (avoidance != null) direction = avoidance;
        float heading = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90;
        float turn = Mth.clamp(Mth.wrapDegrees(heading - mob.getYRot()), -mob.species().turnRate(), mob.species().turnRate());
        mob.setYRot(mob.getYRot() + turn);
        mob.yBodyRot = mob.getYRot(); mob.yHeadRot = mob.getYRot();
        float pitch = (float) (-Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG);
        mob.setXRot(Mth.lerp(.1f, mob.getXRot(), Mth.clamp(pitch, -35, 35)));
        double modifier = attack != null ? mob.species().chaseSpeed() : walk.getSpeedModifier();
        double speed = (mob.species().flying() ? mob.getAttributeValue(Attributes.FLYING_SPEED) : mob.species().swimSpeed()) * modifier;
        double alignment = Math.max(.15, Math.cos(Math.toRadians(Mth.wrapDegrees(heading - mob.getYRot()))));
        speed *= alignment;
        if (attack != null && mob.isWithinMeleeAttackRange(attack)) speed *= .25;
        double yaw = Math.toRadians(mob.getYRot());
        Vec3 desired = new Vec3(-Math.sin(yaw) * speed, Mth.clamp(direction.y * speed, -speed * .6, speed * .6), Math.cos(yaw) * speed);
        mob.setDeltaMovement(mob.getDeltaMovement().lerp(desired, mob.species().flying() ? .08 : .16));
        mob.setSpeed((float) speed);
    }

    private boolean clear(Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(1, Mth.ceil(delta.length() / 1.5));
        for (int step = 1; step <= steps; step++) {
            Vec3 at = start.add(delta.scale((double) step / steps));
            var box = mob.getBoundingBox().move(at.subtract(mob.position())).deflate(.05);
            if (mob.level().getBlockCollisions(mob, box).iterator().hasNext()) return false;
            if (mob.species().aquatic()) {
                for (double x : new double[]{box.minX, box.maxX}) for (double z : new double[]{box.minZ, box.maxZ}) {
                    if (!water(new Vec3(x, box.minY, z)) || !water(new Vec3(x, box.maxY, z))) return false;
                }
            } else if (mob.level().getFluidState(BlockPos.containing(at)).is(FluidTags.WATER)) return false;
        }
        return true;
    }
    private boolean water(Vec3 pos) { return mob.level().getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER); }
}
