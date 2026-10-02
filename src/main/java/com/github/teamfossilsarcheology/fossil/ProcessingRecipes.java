package com.github.teamfossilsarcheology.fossil;

import com.google.gson.Gson;
import java.util.*;
import net.fabricmc.fabric.api.resource.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;

/** Small reloadable machine recipe format; inputs/results are ordinary registry identifiers. */
public final class ProcessingRecipes implements SimpleSynchronousResourceReloadListener {
    public record Result(String item, int count, int weight) {}
    public record Recipe(String machine, List<String> inputs, String fuel, int ticks, List<Result> results) {
        public boolean matches(ItemStack stack) { return inputs.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()); }
        public boolean fueledBy(ItemStack stack) { return fuel == null || stack.is(item(fuel)); }
        public ItemStack output(net.minecraft.util.RandomSource random) {
            int choice = random.nextInt(results.stream().mapToInt(Result::weight).sum());
            for (Result result : results) { choice -= result.weight(); if (choice < 0) return new ItemStack(item(result.item()), result.count()); }
            throw new IllegalStateException("Empty processing result");
        }
    }
    private static volatile List<Recipe> recipes = List.of();
    private static Item item(String id) { return BuiltInRegistries.ITEM.getValue(Identifier.parse(id)); }
    public static Recipe find(String machine, ItemStack input) { return recipes.stream().filter(r -> r.machine().equals(machine) && r.matches(input)).findFirst().orElse(null); }
    public static boolean isFuel(String machine, ItemStack stack) { return recipes.stream().anyMatch(r -> r.machine().equals(machine) && r.fuel() != null && r.fueledBy(stack)); }
    public static boolean animalFood(ItemStack stack) { return stack.has(net.minecraft.core.component.DataComponents.FOOD) || stack.is(Items.WHEAT) || stack.is(Items.WHEAT_SEEDS) || stack.is(NativeItems.get("fern_seed")); }
    public static String guide() {
        return "Fossils and Archeology Rebirth\n\nMine fossil-bearing stone with a pickaxe, or sift sand and gravel. Analyze fossils for DNA; culture DNA with Bio-Goo, then place the cultured egg or embryo. Aquatic eggs require water above them.\n\nAnalyze plant fossils twice: recover a fossil seed or sapling, then extract its living counterpart. Plant and use bonemeal to grow it.\n\nFeed juveniles twice to tame them. Adults need four feeds, or eight for predators. The whip, skull stick, or crouching with an empty hand cycles Wander / Follow / Stay. Saddle a large adult and mount with an empty hand; use jump to hop or rise in water.\n\nFood and feeders restore hunger and health. Place toys and interact with them to restore mood. Fed, content adults breed with their usual food. Water buckets capture small aquatic animals or supported juveniles, preserving care and ownership.\n\nPlace a skull on the ground to start a museum skeleton. Add matching bone groups, then use an empty hand to change pose. Its owner can break it to recover the bones.\n\nAnalyze relic scrap, then repair artifacts with iron or pottery shards at the worktable. Find ancient ruins, temples and dig sites. Use an ancient key on an Anu statue to enter his arena; defeat him and use his scarab gem inside the lair to enter the treasury. Home portals return you to your starting point.\n\nUse the dinopedia on an animal for its care and history, or on a machine for recipes. Hoppers insert from above and extract below; fuel enters from the side. Optional energy speeds processors up without being required.\n\nCredits\nRebirth project: gemdYT.\nOriginal mod and assets: TeamFossilsArcheology and the original mod community.";
    }
    public static String describe(String machine) {
        if (machine.equals("feeder")) return "Feeder\n\nPut food in either slot. The feeder cares for animals within eight blocks every two seconds. Food must match the animal's diet.\n\n" + guide();
        StringBuilder text = new StringBuilder(machine.replace('_', ' ') + " recipes\n\n");
        for (Recipe recipe : recipes) if (recipe.machine().equals(machine)) {
            text.append(recipe.inputs().getFirst().replace("fossil:", "").replace("minecraft:", ""));
            if (recipe.inputs().size() > 1) text.append(" (+").append(recipe.inputs().size() - 1).append(" alternatives)");
            if (recipe.fuel() != null) text.append(" + ").append(recipe.fuel().replace("fossil:", "").replace("minecraft:", ""));
            text.append(" → ");
            if (recipe.results().size() > 4) text.append("one of ").append(recipe.results().size()).append(" discoveries");
            else text.append(String.join(" / ", recipe.results().stream().map(r -> item(r.item()).getName(new ItemStack(item(r.item()))).getString()).toList()));
            text.append(" ( ").append(recipe.ticks() / 20f).append("s )\n\n");
        }
        return text.toString();
    }
    public static void initialize() { ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new ProcessingRecipes()); }
    @Override public Identifier getFabricId() { return ModContent.id("processing"); }
    @Override public void onResourceManagerReload(ResourceManager manager) {
        List<Recipe> loaded = new ArrayList<>();
        manager.listResources("fossil_processing", id -> id.getPath().endsWith(".json")).entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (var reader = entry.getValue().openAsReader()) {
                Recipe recipe = new Gson().fromJson(reader, Recipe.class);
                if (!NativeMachineMenu.TYPES.containsKey(recipe.machine()) || recipe.ticks() < 1 || recipe.ticks() > 12000 || recipe.inputs().isEmpty() || recipe.results().isEmpty()) throw new IllegalArgumentException("Invalid machine, duration or inputs");
                for (String id : recipe.inputs()) validate(id);
                if (recipe.fuel() != null) validate(recipe.fuel());
                int weight = 0;
                for (Result result : recipe.results()) { validate(result.item()); if (result.count() < 1 || result.count() > 64 || result.weight() < 1 || result.weight() > 10000) throw new IllegalArgumentException("Invalid output"); weight = Math.addExact(weight, result.weight()); }
                loaded.add(recipe);
            } catch (Exception e) { throw new IllegalStateException("Invalid fossil processing recipe " + entry.getKey(), e); }
        });
        recipes = List.copyOf(loaded);
    }
    private static void validate(String value) { if (!BuiltInRegistries.ITEM.containsKey(Identifier.parse(value))) throw new IllegalArgumentException("Unknown item " + value); }
}
