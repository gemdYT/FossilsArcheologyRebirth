package com.github.teamfossilsarcheology.fossil;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Persistent native inventories, processing and species incubation for the five machines. */
public final class MachineBlockEntity extends BaseContainerBlockEntity implements ExtendedMenuProvider<String>, WorldlyContainer {
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private ItemStack pending = ItemStack.EMPTY, consumed = ItemStack.EMPTY, consumedFuel = ItemStack.EMPTY;
    private int remaining, duration;
    private boolean eggStarted;
    private String species = "dodo";
    public final ContainerData data = new ContainerData() {
        public int get(int index) { return switch (index) { case 0 -> remaining; case 1 -> duration; default -> (int)energy.amount; }; }
        public void set(int index, int value) { if (index == 0) remaining = value; else if (index == 1) duration = value; else energy.amount = value; }
        public int getCount() { return 3; }
    };
    public MachineBlockEntity(BlockPos pos, BlockState state) { super(ModContent.MACHINE, pos, state); }
    public String kind() { return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath(); }
    public void setSpecies(String value) { if (NativeItems.animalType(value) != null) { species = value; setChanged(); } }
    public void interact(Player player, ItemStack held) {
        if (getBlockState().is(ModContent.DODO_EGG)) {
            if (player instanceof ServerPlayer server) server.sendSystemMessage(Component.literal(species.replace('_', ' ') + " • " + Math.max(1, remaining / 20) + "s incubation; aquatic species require water above"), true);
        } else player.openMenu(this);
    }
    @Override public String getScreenOpeningData(ServerPlayer player) { return kind(); }
    @Override protected Component getDefaultName() { return Component.translatable("block.fossil." + kind()); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override public int getContainerSize() { return 3; }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new NativeMachineMenu(kind(), id, inventory, this, data); }
    @Override public boolean canPlaceItem(int index, ItemStack stack) {
        if (stack.isEmpty() || index < 0 || index > 1) return false;
        if (kind().equals("feeder")) return ProcessingRecipes.animalFood(stack);
        return index == 0 ? ProcessingRecipes.find(kind(), stack) != null : ProcessingRecipes.isFuel(kind(), stack);
    }

    private boolean start(ItemStack input) {
        if (remaining > 0 || !pending.isEmpty() || !items.get(2).isEmpty()) return false;
        var recipe = ProcessingRecipes.find(kind(), input);
        if (recipe == null) return false;
        pending = recipe.output(level == null ? net.minecraft.util.RandomSource.create(0) : level.getRandom());
        consumed = input.copyWithCount(1); duration = recipe.ticks();
        remaining = duration; updateActive(true); setChanged(); return true;
    }
    private boolean fueled() { var recipe = ProcessingRecipes.find(kind(), items.get(0)); return recipe != null && recipe.fuel() != null; }
    private boolean validFuel() { var recipe = ProcessingRecipes.find(kind(), items.get(0)); return recipe != null && recipe.fueledBy(items.get(1)); }
    public final team.reborn.energy.api.base.SimpleEnergyStorage energy = new team.reborn.energy.api.base.SimpleEnergyStorage(10000, 128, 0) {
        @Override protected void onFinalCommit() { setChanged(); }
    };
    @Override public int[] getSlotsForFace(Direction side) { return kind().equals("feeder") ? new int[]{0, 1} : side == Direction.DOWN ? new int[]{2} : side == Direction.UP ? new int[]{0} : new int[]{1}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot < 2 && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 2; }
    public static void tick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        if (state.is(ModContent.DODO_EGG)) { machine.incubate((ServerLevel) level, pos); return; }
        if (machine.kind().equals("feeder")) {
            if (level.getGameTime() % 40 == 0) machine.feed((ServerLevel) level);
            return;
        }
        if (machine.remaining > 0) {
            machine.remaining--;
            if (machine.energy.amount >= 2 && machine.remaining > 0) { machine.energy.amount -= 2; machine.remaining--; }
            if (machine.remaining == 0) {
                machine.items.set(2, machine.pending); machine.pending = ItemStack.EMPTY;
                machine.consumed = machine.consumedFuel = ItemStack.EMPTY;
                machine.updateActive(false);
            }
            machine.setChanged();
        } else if (machine.validFuel() && machine.start(machine.items.get(0))) {
            boolean fuel = machine.fueled();
            machine.items.get(0).shrink(1);
            if (fuel) { machine.consumedFuel = machine.items.get(1).copyWithCount(1); machine.items.get(1).shrink(1); }
            machine.setChanged();
        }
    }
    private void feed(ServerLevel level) {
        for (Animal animal : level.getEntitiesOfClass(Animal.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(8), a -> a.isBaby() || a.getHealth() < a.getMaxHealth() || a instanceof NativeAnimal n && n.hunger() < 90)) {
            for (int slot = 0; slot < 2; slot++) {
                ItemStack food = items.get(slot);
                if (!food.isEmpty() && animal.isFood(food)) {
                    if (animal instanceof NativeAnimal prehistoric) prehistoric.feed(); else animal.heal(4);
                    if (animal.isBaby()) animal.ageUp(30);
                    food.shrink(1); setChanged(); return;
                }
            }
        }
    }
    private void incubate(ServerLevel level, BlockPos pos) {
        if (!eggStarted) { remaining = ModConfig.eggHatchTicks; eggStarted = true; setChanged(); }
        if (remaining > 0) { remaining--; setChanged(); return; }
        var type = NativeItems.animalType(species);
        if (type == null) type = ModContent.DODO;
        var profile = NativeAnimals.PROFILES.get(type);
        if (profile != null && profile.aquatic() && !level.getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.WATER)) { remaining = 20; return; }
        Mob animal = type.create(level, EntitySpawnReason.TRIGGERED);
        if (animal == null) return;
        if (animal instanceof AgeableMob baby) baby.setBaby(true);
        animal.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        if (level.noCollision(animal) && level.addFreshEntity(animal)) {
            level.removeBlock(pos, false);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, animal.getX(), animal.getY() + .5, animal.getZ(), 15, .3, .3, .3, .05);
        } else remaining = 20;
    }
    private void updateActive(boolean active) {
        if (level != null && getBlockState().hasProperty(MachineBlock.ACTIVE))
            level.setBlock(worldPosition, getBlockState().setValue(MachineBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output); ContainerHelper.saveAllItems(output, items);
        output.store("Pending", ItemStack.OPTIONAL_CODEC, pending);
        output.store("Consumed", ItemStack.OPTIONAL_CODEC, consumed);
        output.store("ConsumedFuel", ItemStack.OPTIONAL_CODEC, consumedFuel);
        output.putLong("Energy", energy.amount); output.putInt("Remaining", remaining); output.putInt("Duration", duration);
        output.putBoolean("EggStarted", eggStarted); output.putString("Species", species);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input); ContainerHelper.loadAllItems(input, items);
        pending = input.read("Pending", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        consumed = input.read("Consumed", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        consumedFuel = input.read("ConsumedFuel", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        remaining = Math.max(0, input.getIntOr("Remaining", 0)); duration = Math.max(remaining, input.getIntOr("Duration", 0));
        eggStarted = input.getBooleanOr("EggStarted", false); species = input.getStringOr("Species", "dodo");
        energy.amount = Math.clamp(input.getLongOr("Energy", 0), 0, energy.getCapacity());
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            Containers.dropContents(level, pos, this);
            if (remaining > 0) { Block.popResource(level, pos, consumed); Block.popResource(level, pos, consumedFuel); }
            clearContent(); pending = consumed = consumedFuel = ItemStack.EMPTY;
        }
        super.preRemoveSideEffects(pos, state);
    }
}

