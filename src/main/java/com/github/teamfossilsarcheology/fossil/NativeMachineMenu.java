package com.github.teamfossilsarcheology.fossil;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import java.util.*;

public final class NativeMachineMenu extends AbstractContainerMenu {
    public static final Map<String, ExtendedMenuType<NativeMachineMenu, String>> TYPES = new LinkedHashMap<>();
    public static void initialize() {
        for (String kind : List.of("analyzer", "culture_vat", "sifter", "worktable", "feeder"))
            TYPES.put(kind, Registry.register(BuiltInRegistries.MENU, ModContent.id(kind),
                    new ExtendedMenuType<>((id, inventory, data) -> new NativeMachineMenu(kind, id, inventory, new SimpleContainer(3), new SimpleContainerData(3)), ByteBufCodecs.STRING_UTF8)));
    }
    public final String kind;
    private final Container container;
    private final ContainerData data;
    private final int machineSlots;
    public NativeMachineMenu(String kind, int id, Inventory inventory, Container container, ContainerData data) {
        super(TYPES.get(kind), id);
        this.kind = kind; this.container = container; this.data = data;
        machineSlots = kind.equals("feeder") ? 2 : 3;
        addSlot(new Slot(container, 0, 42, 36));
        addSlot(new Slot(container, 1, 78, 36));
        if (machineSlots == 3) addSlot(new Slot(container, 2, 132, 36) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }
    public int progress() { return data.get(1) == 0 ? 0 : Math.clamp(100 * (data.get(1) - data.get(0)) / data.get(1), 0, 100); }
    public boolean processing() { return data.get(0) > 0; }
    public int energy() { return data.get(2); }
    @Override public boolean stillValid(Player player) { return container.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            var recipe = ProcessingRecipes.find(kind, container.getItem(0));
            int target = recipe != null && recipe.fuel() != null && recipe.fueledBy(stack) || ProcessingRecipes.isFuel(kind, stack) ? 1 : 0;
            if (!container.canPlaceItem(target, stack)) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
