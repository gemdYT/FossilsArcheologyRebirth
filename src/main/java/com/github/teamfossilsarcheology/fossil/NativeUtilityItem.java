package com.github.teamfossilsarcheology.fossil;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;

public final class NativeUtilityItem extends Item {
    private final String name;
    public NativeUtilityItem(String name, Properties properties) { super(properties); this.name = name; }
    @Override public InteractionResult use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        if (name.equals("dinopedia") || name.equals("stone_tablet")) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server) net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(server, new DinopediaPayload(ProcessingRecipes.guide()));
            return InteractionResult.SUCCESS;
        }
        if (name.equals("ancient_clock")) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server) server.sendSystemMessage(Component.literal("World time: " + level.getGameTime()), true);
            return InteractionResult.SUCCESS;
        }
        if (name.equals("magic_conch")) {
            if (!level.isClientSide()) for (var animal : level.getEntitiesOfClass(NativeAnimal.class, player.getBoundingBox().inflate(24), a -> a.species().amphibious() && !a.isDisplay())) animal.direct(player, player.blockPosition());
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (name.equals("dinopedia") && context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MachineBlockEntity machine) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new DinopediaPayload(ProcessingRecipes.describe(machine.kind()))); return InteractionResult.SUCCESS;
        }
        if (name.startsWith("bone_skull_")) {
            var type = NativeAnimals.DISPLAYS.get(name.substring("bone_skull_".length()));
            if (type != null && context.getLevel() instanceof net.minecraft.server.level.ServerLevel level && context.getPlayer() != null) {
                var skeleton = type.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
                var pos = context.getClickedPos().relative(context.getClickedFace());
                if (skeleton != null) {
                    skeleton.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5); skeleton.setYRot(context.getPlayer().getYRot());
                    if (level.noCollision(skeleton) && level.addFreshEntity(skeleton)) { skeleton.assemble(context.getItemInHand(), context.getPlayer()); return InteractionResult.SUCCESS; }
                }
            }
        }
        if (name.equals("ancient_key") && context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
            var block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
            if (block == NativeBlocks.BLOCKS.get("anu_statue") || block == NativeBlocks.BLOCKS.get("anu_portal")) {
                NativePortals.enter(player, false); return InteractionResult.SUCCESS;
            }
        }
        if (name.equals("scarab_gem") && context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player && player.level().dimension().equals(NativePortals.LAIR)) {
            NativePortals.enter(player, true); return InteractionResult.SUCCESS;
        }
        if (name.equals("laser_pointer") || name.equals("laser_pointer_active")) {
            if (context.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
                var pos = context.getClickedPos().relative(context.getClickedFace());
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.getX() + .5, pos.getY() + .2, pos.getZ() + .5, 5, .1, .1, .1, .01);
                if (context.getPlayer() != null) for (var animal : level.getEntitiesOfClass(NativeAnimal.class, new net.minecraft.world.phys.AABB(pos).inflate(16), a -> !a.isDisplay())) animal.direct(context.getPlayer(), pos);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (name.equals("dinopedia")) {
            if (player instanceof net.minecraft.server.level.ServerPlayer server) {
                String details = target instanceof NativeAnimal animal ? animal.isDisplay() ? animal.displayProgress() : " · Hunger " + animal.hunger() + "/100 · Mood " + animal.mood() + "/100 · " + animal.commandName() + " · Diet " + animal.species().diet() + " · " + (animal.ownedBy(player) ? "Your animal" : animal.domesticated() ? "Owned" : "Wild") : "";
                String description = target instanceof NativeAnimal animal ? DinopediaPayload.description(animal.species().name()) : "";
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(server, new DinopediaPayload(target.getName().getString() + "\nHealth " + Math.round(target.getHealth()) + "/" + Math.round(target.getMaxHealth()) + (target instanceof Animal animal && animal.isBaby() ? " · Juvenile" : " · Adult") + details + "\n\n" + description));
            }
            return InteractionResult.SUCCESS;
        }
        if (name.equals("essence_chicken") && target instanceof Animal animal && animal.isBaby()) {
            if (!player.level().isClientSide()) { if (target instanceof NativeAgeLock lock) lock.setAgingStopped(false); animal.ageUp(1200); if (!player.getAbilities().instabuild) stack.shrink(1); }
            return InteractionResult.SUCCESS;
        }
        if (name.equals("essence_stunted") && target instanceof Animal animal && animal.isBaby() && target instanceof NativeAgeLock lock) {
            if (!player.level().isClientSide()) { lock.setAgingStopped(true); if (!player.getAbilities().instabuild) stack.shrink(1); }
            return InteractionResult.SUCCESS;
        }
        return super.interactLivingEntity(stack, player, target, hand);
    }
}

