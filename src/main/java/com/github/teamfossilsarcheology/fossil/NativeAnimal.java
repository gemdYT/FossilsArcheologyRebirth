package com.github.teamfossilsarcheology.fossil;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Native shared movement, combat and breeding; species keep their own assets and profiles. */
public class NativeAnimal extends Animal implements GeoEntity, NativeAgeLock, Bucketable, PlayerRideableJumping, net.tslat.smartbrainlib.api.SmartBrainOwner<NativeAnimal> {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> HUNGER = net.minecraft.network.syncher.SynchedEntityData.defineId(NativeAnimal.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> MOOD = net.minecraft.network.syncher.SynchedEntityData.defineId(NativeAnimal.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> SADDLED = net.minecraft.network.syncher.SynchedEntityData.defineId(NativeAnimal.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private java.util.UUID owner;
    private int command, trust, directedTicks;
    private net.minecraft.core.BlockPos directedTo;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DISPLAY_POSE = net.minecraft.network.syncher.SynchedEntityData.defineId(NativeAnimal.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DISPLAY_BONES = net.minecraft.network.syncher.SynchedEntityData.defineId(NativeAnimal.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final java.util.List<String> BONE_GROUPS = java.util.List.of("arm", "foot", "leg", "ribcage", "skull", "tail", "unique", "vertebrae");
    private final java.util.Set<String> assembledBones = new java.util.LinkedHashSet<>();
    private AnimalPart frontPart, rearPart;
    private final AnimalBehavior behavior = new AnimalBehavior(this);
    public boolean isDisplay() { return NativeAnimals.DISPLAY_PROFILES.containsKey(getType()); }
    public boolean assemble(ItemStack stack, Player player) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!id.startsWith("bone_") || !id.endsWith("_" + species().name()) || assembledBones.contains(id)) return false;
        if (!isDisplay() || NativeItems.get(id) != stack.getItem() || domesticated() && !ownedBy(player)) return false;
        if (!level().isClientSide()) { assembledBones.add(id); syncDisplayBones(); if (owner == null) owner = player.getUUID(); if (!player.getAbilities().instabuild) stack.shrink(1); }
        return true;
    }
    private void syncDisplayBones() {
        int mask = 0;
        for (int group = 0; group < BONE_GROUPS.size(); group++) if (assembledBones.contains("bone_" + BONE_GROUPS.get(group) + "_" + species().name())) mask |= 1 << group;
        entityData.set(DISPLAY_BONES, mask);
    }
    public int displayBones() { return entityData.get(DISPLAY_BONES); }
    public String displayProgress() { return assembledBones.size() + " bone groups • pose " + (entityData.get(DISPLAY_POSE) + 1); }
    private boolean agingStopped, wildSpawn;
    private boolean fromBucket, riderJump;
    @Override public boolean fromBucket() { return fromBucket; }
    @Override public void setFromBucket(boolean value) { fromBucket = value; if (value) setPersistenceRequired(); }
    @Override public ItemStack getBucketItemStack() { return new ItemStack(NativeItems.get("bucket_item_" + species().name())); }
    @Override public SoundEvent getPickupSound() { return net.minecraft.sounds.SoundEvents.BUCKET_FILL_FISH; }
    @Override public boolean canBePickedUpWithBucket(ItemStack stack) { return !isDisplay() && NativeItems.get("bucket_item_" + species().name()) != null && (isBaby() || getBbWidth() <= 2) && stack.is(Items.WATER_BUCKET); }
    @Override public void saveToBucketTag(ItemStack stack) {
        var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level().registryAccess());
        addAdditionalSaveData(output);
        net.minecraft.world.item.component.CustomData.set(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA, stack, output.buildResult());
        if (hasCustomName()) stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, getCustomName());
    }
    @Override public void loadFromBucketTag(net.minecraft.nbt.CompoundTag tag) { readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level().registryAccess(), tag)); }
    @Override public void setAgingStopped(boolean stopped) { agingStopped = stopped; }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(HUNGER, species().carnivore() && !isDisplay() ? 70 : 100); builder.define(MOOD, 70); builder.define(SADDLED, false); builder.define(DISPLAY_POSE, 0); builder.define(DISPLAY_BONES, 0);
    }
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output); output.putBoolean("AgingStopped", agingStopped);
        output.putBoolean("FromBucket", fromBucket);
        output.putBoolean("WildSpawn", wildSpawn);
        if (owner != null) output.putString("Owner", owner.toString());
        output.putInt("Hunger", hunger()); output.putInt("Mood", mood()); output.putInt("Command", command);
        output.putInt("Trust", trust); output.putBoolean("Saddled", entityData.get(SADDLED));
        output.putString("DisplayBones", String.join(",", assembledBones)); output.putInt("DisplayPose", entityData.get(DISPLAY_POSE));
    }
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input); agingStopped = input.getBooleanOr("AgingStopped", false);
        fromBucket = input.getBooleanOr("FromBucket", false);
        wildSpawn = input.getBooleanOr("WildSpawn", false);
        try { owner = java.util.UUID.fromString(input.getStringOr("Owner", "")); } catch (IllegalArgumentException ignored) { owner = null; }
        entityData.set(HUNGER, Math.clamp(input.getIntOr("Hunger", 100), 0, 100));
        entityData.set(MOOD, Math.clamp(input.getIntOr("Mood", 70), 0, 100));
        entityData.set(SADDLED, input.getBooleanOr("Saddled", false)); command = Math.clamp(input.getIntOr("Command", 0), 0, 2); trust = Math.clamp(input.getIntOr("Trust", 0), 0, 10);
        assembledBones.clear(); for (String bone : input.getStringOr("DisplayBones", "").split(",")) if (bone.startsWith("bone_") && bone.endsWith("_" + species().name()) && NativeItems.get(bone) != null) assembledBones.add(bone);
        entityData.set(DISPLAY_POSE, Math.clamp(input.getIntOr("DisplayPose", 0), 0, 2));
        syncDisplayBones();
    }
    public int hunger() { return entityData.get(HUNGER); }
    public int mood() { return entityData.get(MOOD); }
    public boolean ownedBy(Player player) { return owner != null && owner.equals(player.getUUID()); }
    public boolean domesticated() { return owner != null; }
    public boolean staying() { return command == 2 || isVehicle(); }
    public String commandName() { return new String[]{"Wander", "Follow", "Stay"}[command]; }
    public void feed() { entityData.set(HUNGER, Math.min(100, hunger() + 25)); entityData.set(MOOD, Math.min(100, mood() + 8)); heal(3); }
    public void enrich() { entityData.set(MOOD, Math.min(100, mood() + 20)); }
    public void direct(Player player, net.minecraft.core.BlockPos pos) { if (ownedBy(player)) { directedTo = pos.immutable(); directedTicks = 600; command = 0; } }
    @Override public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if ((!domesticated() || ownedBy(player)) && canBePickedUpWithBucket(held)) {
            var pickup = Bucketable.bucketMobPickup(player, hand, this);
            if (pickup.isPresent()) return pickup.get();
        }
        if (isDisplay()) {
            if (!domesticated() || ownedBy(player)) {
                if (assemble(held, player)) return net.minecraft.world.InteractionResult.SUCCESS;
                if (held.isEmpty()) { if (!level().isClientSide()) entityData.set(DISPLAY_POSE, (entityData.get(DISPLAY_POSE) + 1) % 3); return net.minecraft.world.InteractionResult.SUCCESS; }
            }
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (isFood(held) && (!domesticated() || hunger() < 90 || getHealth() < getMaxHealth())) {
            if (!level().isClientSide()) {
                feed();
                if (!domesticated() && ++trust >= (isBaby() ? 2 : species().aggressive() ? 8 : 4)) {
                    owner = player.getUUID(); command = 1; setPersistenceRequired(); level().broadcastEntityEvent(this, (byte) 7);
                }
                if (!player.getAbilities().instabuild) held.shrink(1);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (ownedBy(player) && (held.is(NativeItems.get("whip")) || held.is(NativeItems.get("skull_stick")) || player.isShiftKeyDown() && held.isEmpty())) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server) { command = (command + 1) % 3; clearIntent(); server.sendSystemMessage(net.minecraft.network.chat.Component.literal(commandName()), true); }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (ownedBy(player) && held.is(Items.SADDLE) && !isBaby() && species().width() * species().scale() >= 1 && !species().flying()) {
            if (!level().isClientSide() && !entityData.get(SADDLED)) { entityData.set(SADDLED, true); if (!player.getAbilities().instabuild) held.shrink(1); }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (ownedBy(player) && held.isEmpty() && entityData.get(SADDLED) && !isBaby()) {
            if (!level().isClientSide()) { clearIntent(); player.startRiding(this); }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
    @Override public LivingEntity getControllingPassenger() { return entityData.get(SADDLED) && getFirstPassenger() instanceof Player player ? player : null; }
    @Override protected Vec3 getRiddenInput(Player player, Vec3 input) { return new Vec3(player.xxa * .5, 0, player.zza); }
    @Override protected float getRiddenSpeed(Player player) { return (float) getAttributeValue(Attributes.MOVEMENT_SPEED); }
    @Override protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input); setYRot(player.getYRot()); setXRot(player.getXRot() * .5f); yBodyRot = getYRot(); yHeadRot = getYRot();
        if (riderJump) { if (isInWater()) setDeltaMovement(getDeltaMovement().add(0, .3, 0)); else if (onGround()) jumpFromGround(); riderJump = false; }
    }
    @Override public void onPlayerJump(int strength) { riderJump = strength >= 0 && canJump(); }
    @Override public boolean canJump() { return entityData.get(SADDLED) && !isBaby() && !isDisplay(); }
    @Override public void handleStartJump(int strength) { onPlayerJump(strength); }
    @Override public void handleStopJump() {}
    private void clearIntent() { directedTo = null; directedTicks = 0; getNavigation().stop(); getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET); getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET); }
    @Override public java.util.List<? extends net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor<?>> getSensors(NativeAnimal animal) { return isDisplay() ? java.util.List.of() : AnimalBrain.sensors(); }
    @Override public java.util.List<? extends net.minecraft.world.entity.ai.behavior.BehaviorControl<?>> getAlwaysRunningBehaviours(NativeAnimal animal) { return isDisplay() ? java.util.List.of() : AnimalBrain.core(); }
    @Override public java.util.List<? extends net.minecraft.world.entity.ai.behavior.BehaviorControl<?>> getIdleBehaviours(NativeAnimal animal) { return isDisplay() ? java.util.List.of() : AnimalBrain.idle(this); }
    @Override public java.util.List<? extends net.minecraft.world.entity.ai.behavior.BehaviorControl<?>> getFightingBehaviours(NativeAnimal animal) { return isDisplay() ? java.util.List.of() : AnimalBrain.fight(this); }
    java.util.UUID ownerId() { return owner; }
    Player ownerPlayer() { return owner == null ? null : level().getPlayerByUUID(owner); }
    boolean followingOwner() { return command == 1; }
    net.minecraft.core.BlockPos directedTarget() { return directedTicks > 0 ? directedTo : null; }
    public boolean canHunt(LivingEntity target) { return behavior.canHunt(target); }
    public boolean canKeepAttacking(LivingEntity target) { return behavior.canKeepAttacking(target); }
    public boolean canWander() { return behavior.canWander(); }
    public void updateIntent() { behavior.update(); }
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public NativeAnimal(EntityType<? extends NativeAnimal> type, Level level) {
        super(type, level);
        if (isDisplay()) { setNoAi(true); setNoGravity(true); setSilent(true); setPersistenceRequired(); }
        if (species().aquatic()) {
            moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, 1, .1f, false);
            setPathfindingMalus(PathType.WATER, 0);
        } else if (species().flying()) {
            moveControl = new FlyingMoveControl<>(this, 10, true);
            setPathfindingMalus(PathType.WATER, -1);
        } else if (species().amphibious()) {
            setPathfindingMalus(PathType.WATER, 0);
        }
    }
    public AnimalSpecies species() { return NativeAnimals.profile(getType()); }
    public static AttributeSupplier.Builder attributes(AnimalSpecies species) {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, species.health())
                .add(Attributes.MOVEMENT_SPEED, species.speed()).add(Attributes.ATTACK_DAMAGE, species.damage())
                .add(Attributes.ARMOR, species.armor()).add(Attributes.ATTACK_KNOCKBACK, species.knockback()).add(Attributes.FLYING_SPEED, species.speed()).add(Attributes.FOLLOW_RANGE, 24);
    }
    @Override protected PathNavigation createNavigation(Level level) {
        if (species().aquatic()) return new WaterBoundPathNavigation(this, level);
        if (species().flying()) return new FlyingPathNavigation(this, level);
        return super.createNavigation(level);
    }
    @Override protected void registerGoals() {
        if (isDisplay()) return;
        if (!species().aquatic()) goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new BreedGoal(this, 1));
    }
    @Override public boolean isFood(ItemStack stack) {
        String food = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean omnivore = species().diet().equals("OMNIVORE");
        if ((species().carnivore() || omnivore) && food.startsWith("meat_")) return true;
        if (!species().carnivore() && (food.startsWith("berry_") || food.equals("fern_seed"))) return true;
        if ((species().diet().contains("PISCI") || omnivore) && (stack.is(Items.COD) || stack.is(Items.SALMON))) return true;
        if (species().carnivore() || omnivore) {
            if (stack.is(Items.BEEF) || stack.is(Items.PORKCHOP) || stack.is(Items.CHICKEN) || stack.is(Items.MUTTON)) return !species().diet().equals("PISCIVORE") || omnivore;
            if (!omnivore) return false;
        }
        return stack.is(Items.WHEAT) || stack.is(Items.WHEAT_SEEDS) || stack.is(Items.APPLE) || stack.is(Items.CARROT);
    }
    @Override public boolean canMate(Animal other) { return !isDisplay() && hunger() >= 40 && mood() >= 20 && other.getType() == getType() && (!(other instanceof NativeAnimal animal) || animal.hunger() >= 40 && animal.mood() >= 20) && super.canMate(other); }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return getType().create(level, EntitySpawnReason.BREEDING) instanceof AgeableMob child ? child : null;
    }
    @Override public boolean canBreatheUnderwater() { return species().amphibious(); }
    @Override public float getWalkTargetValue(net.minecraft.core.BlockPos pos, net.minecraft.world.level.LevelReader level) {
        if (species().aquatic()) return level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER) ? 10 : -1;
        return super.getWalkTargetValue(pos, level);
    }
    @Override public boolean checkSpawnObstruction(net.minecraft.world.level.LevelReader level) {
        return species().aquatic() ? level.isUnobstructed(this) : super.checkSpawnObstruction(level);
    }
    @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData group) {
        wildSpawn = reason == EntitySpawnReason.NATURAL && NativeAquaticSpawns.SPECIES.contains(species().name());
        return super.finalizeSpawn(level, difficulty, reason, group);
    }
    @Override public boolean removeWhenFarAway(double distance) {
        return wildSpawn && !fromBucket && !domesticated() && !hasCustomName();
    }
    @Override protected SoundEvent getAmbientSound() { return NativeAnimals.sound(species(), "ambient"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return NativeAnimals.sound(species(), "hurt"); }
    @Override protected SoundEvent getDeathSound() { return NativeAnimals.sound(species(), "death"); }
    @Override public void travel(Vec3 input) {
        if (species().amphibious() && isInWater()) {
            moveRelative(.03f, input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(.9));
        } else super.travel(input);
    }
    @Override public void aiStep() {
        super.aiStep();
        if (isDisplay()) return;
        if (level() instanceof ServerLevel server) {
            if (!isBaby() && getBbWidth() > 2) {
                if (frontPart == null || frontPart.isRemoved()) frontPart = AnimalPart.create(this, true);
                if (rearPart == null || rearPart.isRemoved()) rearPart = AnimalPart.create(this, false);
            }
            if (directedTicks > 0) directedTicks--;
            if (!isBaby() && hunger() >= 60 && tickCount % 6000 == 0) {
                var egg = NativeItems.get("egg_" + species().name());
                if (egg != null) spawnAtLocation(server, new ItemStack(egg));
            }
            if (tickCount % ModConfig.careIntervalTicks == 0) {
                entityData.set(HUNGER, Math.max(0, hunger() - 2)); entityData.set(MOOD, Math.max(0, mood() - 1));
                if (hunger() == 0) hurtServer(server, damageSources().starve(), 1);
                else if (hunger() > 80) heal(1);
            }
        }
        if (agingStopped && isBaby()) setAge(-24000);
        if (species().flying()) setNoGravity(!onGround());
        if (species().movement().equals("WALK_AND_GLIDE") && !onGround() && getDeltaMovement().y < 0) setDeltaMovement(getDeltaMovement().multiply(1, .6, 1));
        if (species().aquatic() && !isInWater() && onGround()) {
            setDeltaMovement(getDeltaMovement().add((random.nextDouble() - .5) * .2, .25, (random.nextDouble() - .5) * .2));
            if (tickCount % 20 == 0 && level() instanceof ServerLevel server) hurtServer(server, damageSources().dryOut(), 1);
        }
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        if (isDisplay()) {
            controllers.add(new AnimationController<NativeAnimal>("display", 0, state -> state.setAndContinue(RawAnimation.begin().thenLoop("museum.pose" + entityData.get(DISPLAY_POSE)))));
            return;
        }
        AnimalSpecies species = species();
        RawAnimation idle = RawAnimation.begin().thenLoop(species.idle());
        RawAnimation walking = RawAnimation.begin().thenLoop(species.walk());
        RawAnimation swimming = RawAnimation.begin().thenLoop(species.swim());
        RawAnimation flying = RawAnimation.begin().thenLoop(species.fly());
        RawAnimation attack = RawAnimation.begin().thenPlay(species.attack());
        controllers.add(new AnimationController<NativeAnimal>("movement", 5, state -> state.setAndContinue(
                isSwinging() ? attack : isInWater() && species.amphibious() ? swimming : species.flying() && !onGround() ? flying : state.isMoving() ? walking : idle)));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT);
        if (hit && target instanceof LivingEntity prey && species().poisonTicks() > 0) prey.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, species().poisonTicks()), this);
        if (hit && target instanceof LivingEntity prey && !prey.isAlive() && species().carnivore()) feed();
        return hit;
    }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!isDisplay()) return super.hurtServer(level, source, amount);
        if (source.getEntity() instanceof Player player && (!domesticated() || ownedBy(player)) && !isRemoved()) {
            for (String bone : assembledBones) spawnAtLocation(level, new ItemStack(NativeItems.get(bone)));
            assembledBones.clear(); discard(); return true;
        }
        return false;
    }
}
