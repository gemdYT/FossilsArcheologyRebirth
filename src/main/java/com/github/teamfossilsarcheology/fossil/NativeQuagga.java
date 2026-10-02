package com.github.teamfossilsarcheology.fossil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.level.Level;

/** Preserve native horse taming, riding, saddling and breeding for the extinct quagga. */
public final class NativeQuagga extends Horse implements NativeAgeLock {
    private boolean agingStopped;
    @Override public void setAgingStopped(boolean stopped) { agingStopped = stopped; }
    @Override public void aiStep() { super.aiStep(); if (agingStopped && isBaby()) setAge(-24000); }
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) { super.addAdditionalSaveData(output); output.putBoolean("AgingStopped", agingStopped); }
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) { super.readAdditionalSaveData(input); agingStopped = input.getBooleanOr("AgingStopped", false); }
    public NativeQuagga(EntityType<? extends NativeQuagga> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return Horse.createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 26).add(Attributes.MOVEMENT_SPEED, .225);
    }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return NativeAnimals.QUAGGA.create(level, EntitySpawnReason.BREEDING);
    }
}
