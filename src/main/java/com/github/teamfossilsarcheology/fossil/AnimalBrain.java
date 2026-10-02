package com.github.teamfossilsarcheology.fossil;

import java.util.List;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.base.ExtendedBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.*;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.attack.AnimatableMeleeAttack;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/** Library integration only; species, commands and care remain native mod-owned data. */
public final class AnimalBrain {
    private AnimalBrain() {}
    // The 2.0.2 CustomBehaviour constructor reads an uninitialized instance field.
    // A small native intent task keeps its requirements available during construction.
    private static final class Intent extends ExtendedBehaviour<NativeAnimal> {
        @Override public java.util.Set<net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition<?, ?>> getMemoryRequirements() { return java.util.Set.of(); }
        @Override protected void start(NativeAnimal animal) { animal.updateIntent(); }
    }
    // In SBL 2.0.2 start() clears WALK_TARGET, but shouldKeepRunning() requires it.
    // Preserve the selected destination so the new path survives its first tick.
    private static final class MoveToTarget extends MoveToWalkTarget<NativeAnimal> {
        @Override protected void start(NativeAnimal animal) {
            var destination = animal.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
            super.start(animal);
            destination.ifPresent(target -> animal.getBrain().setMemory(MemoryModuleType.WALK_TARGET, target));
        }
    }
    public static List<? extends ExtendedSensor<?>> sensors() { return List.of(new NearbyLivingEntitySensor<NativeAnimal>().setRadius(24).scanRate(20)); }
    public static List<? extends BehaviorControl<?>> core() {
        return List.of(new Intent().cooldownFor(10),
                new LookAtTarget<NativeAnimal>(), new MoveToTarget().startCondition(a -> !a.staying() && !a.isInLove()));
    }
    public static List<? extends BehaviorControl<?>> idle(NativeAnimal animal) {
        var random = animal.species().aquatic() ? new SetRandomSwimTarget<NativeAnimal>() : animal.species().flying() ? new SetRandomFlyTarget<NativeAnimal>() : new SetRandomWalkTarget<NativeAnimal>();
        return List.of(random.startCondition(NativeAnimal::canWander).cooldownFor(80));
    }
    public static List<? extends BehaviorControl<?>> fight(NativeAnimal animal) {
        return List.of(new InvalidateAttackTarget<NativeAnimal>().invalidateIf((a, target) -> !a.canKeepAttacking(target)),
                new SetWalkTargetToAttackTarget<NativeAnimal>(), new AnimatableMeleeAttack<NativeAnimal>(animal.species().attackDelay()).attackInterval(animal.species().attackInterval()));
    }
}
