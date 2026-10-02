package com.github.teamfossilsarcheology.fossil;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Short native content boot check; broad species/gameplay testing is left to the player. */
public final class ContentSmokeClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().adjustSettings(settings -> {
            var presets = settings.getSettings().worldgenLoadContext().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET);
            settings.setWorldType(new net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.WorldTypeEntry(presets.getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)));
            settings.setGenerateStructures(true);
        }).create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("difficulty normal");
            world.getServer().runCommand("tp @a 0 101 5 180 20");
            context.waitTicks(5);
            world.getServer().runOnServer(mc -> {
                if (NativeAnimals.PROFILES.size() != 64 || NativeBlocks.BLOCKS.size() != 250) throw new AssertionError("Incomplete content registries");
                for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) mc.overworld().setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                int x = -3;
                for (String name : java.util.List.of("calamites_planks", "calamites_stairs", "calamites_slab", "calamites_fence", "ancient_chest", "amber_block")) {
                    mc.overworld().setBlock(new BlockPos(x++, 100, 0), NativeBlocks.BLOCKS.get(name).defaultBlockState(), Block.UPDATE_ALL);
                }
            });
            var ai = new AnimalAiSmoke();
            world.getServer().runOnServer(ai::setup);
            for (int sample = 0; sample < 20; sample++) {
                context.waitTicks(20);
                world.getServer().runOnServer(mc -> ai.sample());
            }
            world.getServer().runOnServer(mc -> ai.verify());
            world.getServer().runOnServer(ai::beginFollowing);
            context.waitTicks(120);
            world.getServer().runOnServer(ai::verifyFollowingAndStay);
            context.waitTicks(20);
            world.getServer().runOnServer(mc -> ai.recordStay());
            context.waitTicks(40);
            world.getServer().runOnServer(mc -> ai.verifyStay());
            world.getServer().runOnServer(ai::beginCombat);
            context.waitTicks(160);
            world.getServer().runOnServer(mc -> ai.verifyCombat());
            var instincts = new AnimalInstinctSmoke();
            world.getServer().runOnServer(instincts::beginHunting);
            context.waitTicks(220);
            world.getServer().runOnServer(mc -> instincts.verifyHunting());
            world.getServer().runOnServer(instincts::beginDefense);
            context.waitTicks(180);
            world.getServer().runOnServer(mc -> instincts.verifyDefense());
            world.getServer().runOnServer(instincts::beginOwnerDefense);
            context.waitTicks(160);
            world.getServer().runOnServer(mc -> instincts.verifyOwnerDefense());
            world.getServer().runOnServer(instincts::beginPursuit);
            context.waitTicks(30);
            world.getServer().runOnServer(mc -> instincts.escape());
            context.waitTicks(30);
            world.getServer().runOnServer(mc -> instincts.verifyPursuit());
            world.getServer().runCommand("tp @a 0 101 5 180 20");
            context.waitTicks(5);
            var wildAquatics = new AquaticSpawnSmoke();
            world.getServer().runCommand("setworldspawn -128 100 -128");
            world.getServer().runOnServer(wildAquatics::setup);
            for (String biome : java.util.List.of("swamp", "river", "ocean")) {
                world.getServer().runCommand(wildAquatics.biomeCommand(biome));
                context.waitTicks(2);
                world.getServer().runOnServer(mc -> wildAquatics.verifyNaturalSpawning(mc, biome));
            }
            world.getServer().runCommand("tp @a 0 101 5 180 20");
            context.waitTicks(5);
            world.getServer().runOnServer(mc -> {
                var level = mc.overworld(); var player = mc.getPlayerList().getPlayers().getFirst();
                ProgressionSmoke.verify(level);
                for (String species : java.util.List.of("allosaurus", "triceratops")) {
                    var animal = NativeAnimals.TYPES.get(species).create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    if (!(animal.getBrain() instanceof net.tslat.smartbrainlib.api.internal.SmartBrain<?>)) throw new AssertionError("Animal does not use the selected brain library");
                    animal.setPos(species.equals("allosaurus") ? 3 : -3, 100, -3); level.addFreshEntity(animal);
                    var food = species.equals("allosaurus") ? net.minecraft.world.item.Items.BEEF : net.minecraft.world.item.Items.WHEAT;
                    player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(food, 16));
                    for (int feed = 0; feed < 8; feed++) animal.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                    if (!animal.ownedBy(player)) throw new AssertionError("Taming failed: " + species);
                    player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(NativeItems.get("whip")));
                    animal.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                    if (!animal.commandName().equals("Stay")) throw new AssertionError("Owner command failed");
                    var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
                    animal.saveWithoutId(output);
                    var restored = NativeAnimals.TYPES.get(species).create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
                    restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
                    if (!restored.ownedBy(player) || !restored.commandName().equals("Stay") || restored.hunger() != animal.hunger()) throw new AssertionError("Animal ownership/care save failed");
                }
                var display = NativeAnimals.DISPLAYS.get("tyrannosaurus").create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                display.setPos(0, 100, -4); level.addFreshEntity(display);
                for (var entry : NativeItems.ITEMS.entrySet()) if (entry.getKey().startsWith("bone_") && entry.getKey().endsWith("_tyrannosaurus")) display.assemble(new net.minecraft.world.item.ItemStack(entry.getValue()), player);
                if (!display.displayProgress().startsWith("8 bone groups")) throw new AssertionError("Skeleton assembly failed");
                if (display.displayBones() != 255 || display.assemble(new net.minecraft.world.item.ItemStack(NativeItems.get("bone_skull_tyrannosaurus")), player)) throw new AssertionError("Skeleton accepted duplicate bones");
                var fish = NativeAnimals.TYPES.get("coelacanth").create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                fish.setBaby(true); fish.setCustomName(net.minecraft.network.chat.Component.literal("Museum fish"));
                var bucket = fish.getBucketItemStack(); fish.saveToBucketTag(bucket);
                var restoredFish = NativeAnimals.TYPES.get("coelacanth").create(level, net.minecraft.world.entity.EntitySpawnReason.BUCKET);
                restoredFish.loadFromBucketTag(bucket.get(net.minecraft.core.component.DataComponents.BUCKET_ENTITY_DATA).copyTag());
                if (!restoredFish.isBaby() || !fish.canBePickedUpWithBucket(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WATER_BUCKET))) throw new AssertionError("Aquatic capture lost animal state");
                var templates = mc.getStructureTemplateManager();
                for (var id : templates.listTemplates().filter(id -> id.getNamespace().equals("fossil")).toList()) {
                    if (templates.get(id).orElseThrow().getSize().getY() == 0) throw new AssertionError("Empty structure template: " + id);
                }
                for (String role : java.util.List.of("paleontologist", "archeologist")) {
                    var villager = (net.minecraft.world.entity.npc.villager.Villager) net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("villager")).create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION, ModContent.id(role))));
                    if (villager.getOffers().isEmpty()) throw new AssertionError("Villager has no trades: " + role);
                }
                level.setBlock(new BlockPos(0, 100, 2), ModContent.ANALYZER.defaultBlockState(), Block.UPDATE_ALL);
                var machine = (MachineBlockEntity) level.getBlockEntity(new BlockPos(0, 100, 2));
                var storage = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.find(level, machine.getBlockPos(), net.minecraft.core.Direction.UP);
                if (storage == null || machine.getSlotsForFace(net.minecraft.core.Direction.DOWN)[0] != 2) throw new AssertionError("Machine automation not exposed");
                try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                    machine.energy.insert(100, transaction); transaction.commit();
                }
                if (machine.energy.amount != 100) throw new AssertionError("Machine energy transfer failed");
                if (mc.getLevel(NativePortals.LAIR) == null || mc.getLevel(NativePortals.TREASURE) == null) throw new AssertionError("Mod dimensions missing");
                if (!NativePortals.enter(player, false)) throw new AssertionError("Lair portal failed");
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                if (NativePortals.enter(player, true)) throw new AssertionError("Treasury accessible before defeating Anu");
            });
            context.waitTicks(20);
            world.getServer().runOnServer(mc -> {
                var player = mc.getPlayerList().getPlayers().getFirst();
                var lair = mc.getLevel(NativePortals.LAIR);
                var boss = lair.getEntitiesOfClass(NativeAnu.class, new net.minecraft.world.phys.AABB(-20, 0, -20, 20, 20, 20)).getFirst();
                boss.hurtServer(lair, lair.damageSources().genericKill(), Float.MAX_VALUE);
            });
            context.waitTicks(2);
            world.getServer().runOnServer(mc -> {
                var player = mc.getPlayerList().getPlayers().getFirst(); var lair = mc.getLevel(NativePortals.LAIR);
                if (lair.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(-20, 0, -20, 20, 20, 20), i -> i.getItem().is(NativeItems.get("scarab_gem"))).isEmpty()) throw new AssertionError("Anu reward missing");
                if (!NativePortals.enter(player, true) || !NativePortals.home(player)) throw new AssertionError("Treasure unlock or portal return failed");
                if (player.level() != mc.overworld() || Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot() - 180)) > 1 || Math.abs(player.getXRot() - 20) > 1)
                    throw new AssertionError("Portal return lost dimension or viewing direction: " + player.level().dimension() + " yaw=" + player.getYRot() + " pitch=" + player.getXRot());
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            });
            context.waitTicks(15);
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);
            context.takeScreenshot("fossil-care-and-skeletons");
            context.takeScreenshot("fossil-content-smoke");
            world.getServer().runOnServer(mc -> {
                var player = mc.getPlayerList().getPlayers().getFirst();
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new DinopediaPayload(ProcessingRecipes.guide()));
            });
            context.waitTicks(5);
            context.runOnClient(mc -> {
                if (!(mc.gui.screen() instanceof com.github.teamfossilsarcheology.fossil.client.DinopediaScreen)) throw new AssertionError("Dinopedia networking/screen failed");
            });
            context.takeScreenshot("fossil-dinopedia");
            context.runOnClient(mc -> mc.gui.setScreen(null));
            for (String kind : NativeMachineMenu.TYPES.keySet()) {
                world.getServer().runOnServer(mc -> {
                    var player = mc.getPlayerList().getPlayers().getFirst();
                    player.closeContainer();
                    var pos = new BlockPos(0, 100, 2);
                    var block = kind.equals("analyzer") ? ModContent.ANALYZER : kind.equals("culture_vat") ? ModContent.CULTURE_VAT : NativeBlocks.BLOCKS.get(kind);
                    mc.overworld().setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
                    var machine = (MachineBlockEntity) mc.overworld().getBlockEntity(pos);
                    if (machine == null) throw new AssertionError("Missing block entity: " + kind);
                    if (kind.equals("culture_vat")) {
                        machine.setItem(0, new net.minecraft.world.item.ItemStack(NativeItems.get("allosaurus_dna")));
                        machine.setItem(1, new net.minecraft.world.item.ItemStack(ModContent.BIO_GOO));
                    }
                    machine.interact(player, net.minecraft.world.item.ItemStack.EMPTY);
                });
                context.waitTicks(10);
                context.runOnClient(mc -> {
                    if (!(mc.gui.screen() instanceof com.github.teamfossilsarcheology.fossil.client.NativeMachineScreen screen) || !screen.getMenu().kind.equals(kind)) throw new AssertionError("Machine screen did not open: " + kind);
                });
                context.takeScreenshot("fossil-machine-" + kind);
            }
            world.getServer().runOnServer(mc -> mc.getPlayerList().getPlayers().getFirst().closeContainer());
            world.getServer().runOnServer(mc -> {
                for (String kind : java.util.List.of("analyzer", "culture_vat", "sifter", "worktable")) {
                    var pos = new BlockPos(0, 100, 2);
                    var block = kind.equals("analyzer") ? ModContent.ANALYZER : kind.equals("culture_vat") ? ModContent.CULTURE_VAT : NativeBlocks.BLOCKS.get(kind);
                    mc.overworld().setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
                    var machine = (MachineBlockEntity) mc.overworld().getBlockEntity(pos);
                    var input = kind.equals("analyzer") ? ModContent.BIO_FOSSIL : kind.equals("culture_vat") ? NativeItems.get("allosaurus_dna") : kind.equals("sifter") ? net.minecraft.world.item.Items.GRAVEL : NativeItems.get("broken_sword");
                    machine.setItem(0, new net.minecraft.world.item.ItemStack(input, 2));
                    machine.setItem(1, new net.minecraft.world.item.ItemStack(kind.equals("worktable") ? net.minecraft.world.item.Items.IRON_INGOT : ModContent.BIO_GOO, 2));
                    MachineBlockEntity.tick(mc.overworld(), pos, machine.getBlockState(), machine);
                    var saved = machine.saveWithFullMetadata(mc.overworld().registryAccess());
                    machine = (MachineBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, machine.getBlockState(), saved, mc.overworld().registryAccess());
                    if (machine == null || !machine.getItem(2).isEmpty()) throw new AssertionError("Pending output escaped save storage");
                    mc.overworld().setBlockEntity(machine);
                    for (int i = 0; i < 200; i++) MachineBlockEntity.tick(mc.overworld(), pos, machine.getBlockState(), machine);
                    var output = machine.getItem(2);
                    if (output.isEmpty() || machine.getItem(0).getCount() != 1) throw new AssertionError("Machine processing/consumption failed: " + kind);
                    if (kind.equals("culture_vat") && (!output.is(NativeItems.REVIVAL_ITEMS.get("allosaurus")) || machine.getItem(1).getCount() != 1)) throw new AssertionError("Species revival or vat fuel failed");
                    if (kind.equals("worktable") && !output.is(NativeItems.get("ancient_sword"))) throw new AssertionError("Artifact restoration failed");
                }
            });
            System.out.println("FOSSIL CONTENT: 64 animals and 250 additional catalog blocks booted successfully");
            System.out.println("FOSSIL MACHINES: all five native inventory interfaces opened successfully; items=" + NativeItems.ITEMS.size());
            System.out.println("FOSSIL PROCESSING: analyzer, species culture, sifter and artifact restoration outputs/consumption passed");
            System.out.println("FOSSIL SYSTEMS: representative brain AI, taming, commands, care saves, skeleton assembly, automation, energy and portal return passed");
        }
    }
}


