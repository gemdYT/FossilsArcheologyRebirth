# Fabric 26.3 architecture

This checkout contains one active Fabric source tree. Forge, Architectury, previous-version Java sources and migration generators have been removed. Original content assets, structure templates, language text, contributor attribution and licensing are retained. Minecraft 26.3 is the verified target; future 26.3+ releases will require their own dependency/build checks.

## Runtime libraries

| Library | Pinned version | Responsibility |
| --- | --- | --- |
| Fabric API | 0.161.0+26.3 | Events, networking, attachments, item transfer and lookup |
| GeckoLib | 5.5.7 | Shared animal/exhibit rendering and animation |
| TerraBlender | 26.3.0.0.9 | Weighted volcano biome region and surface rules |
| SmartBrainLib | 2.0.2 | Shared sensors, movement, targeting and timed behaviors |
| Structure Pool API | 1.3.0+26.3-fabric | Ten village-building injections with spawn limits |
| Team Reborn Energy | 5.0.0, bundled | Transactional machine power interoperability |

The first five libraries are separate runtime jars. Fabric Loader is the platform. Gradle properties pin immutable Modrinth artifact IDs where applicable. No second animation, component, GUI, dimension, multipart or configuration framework is needed.

## Data and code ownership

- `assets/fossil/species.json` defines the 64 species' dimensions, rendering, diet, movement and attack parameters. Walking/swimming/flight base speeds, cruise/chase modifiers, turn rates, flight height and slow/fast animation clips are explicit profile fields. Its optional `defensive` boolean selects herbivores that fight back; other non-predators flee. `NativeAnimal` owns persistent care, ownership, commands, riding, capture and exhibit state. `AnimalBrain` contains SmartBrainLib composition; `NativeQuagga` reuses Minecraft horse behavior.
- `AnimalBehavior` owns per-animal prey selection, threat avoidance, owner assistance, food temptation, loose grouping and pursuit limits. Diet and size select prey; aquatic movement limits combat to water. It writes standard brain memories. SmartBrainLib/native navigation execute ground movement and timed attacks; air/water steering reads those same destinations. New pursuits reset stale failed-path memory so SmartBrainLib does not immediately abandon a fresh target after an earlier unreachable roaming destination. Pursuit state is transient; saved hunger and ownership retain their existing format.
- `AnimalLocomotion` maintains air/water cruise legs and steers with inertia, limited turning and body-sized collision probes. It uses native move control on land for amphibious species. Flight state is synced for client wing selection; banking/pitch are smoothed visual offsets layered onto existing animation. Grounded flyers can take off without requiring a pre-existing airborne navigation path.
- `AnimalPart` adds two damage targets for eligible large adults and forwards hits to the parent. These are transient entities; the parent recreates them.
- `AnimalBrain.MoveToTarget` preserves `WALK_TARGET` when SmartBrainLib 2.0.2 starts navigation; the library otherwise clears the memory required by its own continuation check, cancelling paths immediately. Remove this local workaround only after an updated library passes the movement checks. Brain ticking is provided by the library's mob mixin.
- `NativeAquaticSpawns` adds four living species through Fabric biome modifications and datapack biome tags, using the vanilla fish category and water placement. `SpawnPlacementsInvoker` exposes the private native registration method. Aquatic obstruction checks permit water while still rejecting overlapping blocks.
- `assets/fossil/blocks.json` and `items.json` describe the catalogs. Native classes cover wood/stone families, beds, storage and equipment. Small shared classes implement tar, decoration/plants, toys and utility interactions.
- `MachineBlockEntity` owns inventories, selected outputs, consumed ingredients, timers, incubation and power. All five menus/screens share implementations. `ProcessingRecipes` reloads `data/fossil/fossil_processing/*.json` and validates identifiers; content balance belongs in those files.
- `NativeWorldGen` isolates TerraBlender and Structure Pool API integration. Ores, trees, vegetation, ruins and temples otherwise use native data. `VolcanoFeature` is the small custom cone feature.
- `NativePortals` owns the two fixed dimensions, saved return position, arena creation and defeat gate through persistent Fabric attachments. `NativeAnu` owns encounter phases.
- `DinopediaPayload` sends bounded read-only server information to the paginated native screen. Client rendering/screens are confined to the client package and entrypoint.
- Configuration is a small JSON file using the game's Gson. Incubation and care intervals are configurable; processing durations and rewards are reloadable data.

Minecraft 26.3 uses singular loot `condition` and `modifier` keys with `type` discriminators. Old `conditions/functions/function` keys must not be reintroduced. Native structures live in `data/fossil/structure`; features in `worldgen/feature`.

## Maintenance workflow

1. Edit a catalog/profile, processing recipe or native data file.
2. Run `python tools/generate_progression.py` when rebuilding shared survival recipes, tags or loot. It reads only active catalogs. Animal base loot regeneration is optional; run `generate_animal_loot.py` before progression generation to restore supplementary fur/rewards.
3. Run `python tools/audit_content.py` to check item references, loot schema, template references, declared acquisition sources, motion parameters and animation references. Its report is `build/content-audit.json`; acquisition references do not prove a playable survival graph.
4. Run `.\gradlew.bat build` for compilation, resources, config persistence checks and the jar.
5. Run `.\gradlew.bat runClientGameTest` after runtime changes. This focused check uses representative animals and leaves broad species testing to players.
6. Run `python tools/package_build.py` after a successful build to package the mod, five pinned external jars, SHA-256 manifest, guide and attribution.

Original assets remain under their stated license; source cleanup does not change their rights. See README and ORIGINAL_CONTRIBUTORS.txt. The dependency audit JSON records the research used for this port; it is historical evidence, not an automatic updater.

Broad animal balancing, complete furniture animation fidelity, optional recipe/overlay integrations, modpack interaction and full dedicated-server gameplay are open validation/enhancement areas. Their absence does not require retaining the old source or loaders.
