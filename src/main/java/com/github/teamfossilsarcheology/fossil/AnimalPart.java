package com.github.teamfossilsarcheology.fossil;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;

/** Ephemeral server-owned targets; damage is forwarded to the living parent exactly once. */
public final class AnimalPart extends Entity {
    public static final EntityType<AnimalPart> TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, ModContent.id("animal_part"),
            EntityType.Builder.<AnimalPart>of(AnimalPart::new, MobCategory.MISC).sized(1, 1).noSave().clientTrackingRange(10).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ModContent.id("animal_part"))));
    private static final EntityDataAccessor<Integer> PARENT = SynchedEntityData.defineId(AnimalPart.class, EntityDataSerializers.INT);
    private NativeAnimal parent;
    private final boolean front;
    public AnimalPart(EntityType<? extends AnimalPart> type, Level level) { super(type, level); front = false; }
    private AnimalPart(NativeAnimal parent, boolean front) { super(TYPE, parent.level()); this.parent = parent; this.front = front; entityData.set(PARENT, parent.getId()); }
    public static void initialize() {}
    public static AnimalPart create(NativeAnimal parent, boolean front) {
        var part = new AnimalPart(parent, front); part.follow(); ((ServerLevel)parent.level()).addFreshEntity(part); return part;
    }
    private NativeAnimal parent() { if (parent == null && level().getEntity(entityData.get(PARENT)) instanceof NativeAnimal animal) parent = animal; return parent; }
    private void follow() {
        var animal = parent(); if (animal == null) return;
        var offset = animal.getLookAngle().multiply(1, 0, 1).normalize().scale(animal.getBbWidth() * (front ? .65 : -.65));
        setPos(animal.getX() + offset.x, animal.getY() + animal.getBbHeight() * (front ? .65 : .35), animal.getZ() + offset.z);
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide()) { var animal = parent(); if (animal == null || !animal.isAlive() || animal.isRemoved()) { discard(); return; } follow(); }
    }
    @Override public boolean isPickable() { return parent() != null && parent().isAlive(); }
    @Override public net.minecraft.world.InteractionResult interact(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.Vec3 hit) {
        var animal = parent();
        if (animal == null) return net.minecraft.world.InteractionResult.PASS;
        var stack = player.getItemInHand(hand);
        var result = animal.mobInteract(player, hand);
        return result.consumesAction() ? result : stack.getItem().interactLivingEntity(stack, player, animal, hand);
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { var animal = parent(); return animal != null && animal.hurtServer(level, source, damage); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(PARENT, -1); }
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
    @Override protected void readAdditionalSaveData(ValueInput input) {}
}
