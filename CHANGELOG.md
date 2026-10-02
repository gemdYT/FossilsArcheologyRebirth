## 9.3.4.0-dev.8 � Rebirth branding cleanup

- Removed remaining old project branding from documentation, credits labels and build coordinates.
- Culture-vat help and the dinopedia describe cultured eggs/embryos consistently; incubation code uses the same terminology.
- Rebuilt the install kit with Rebirth metadata. Restart Minecraft after updating to reload tooltip branding.

## 9.3.4.0-dev.7 � Animal locomotion and predator fixes

- Corrected movement/attack animation assignments, including Quetzalcoatlus flight and trilobite locomotion. Added separate movement and triggered attack controllers.
- Added persistent air/water cruising with gradual acceleration, bounded turning, body-sized obstacle probes, flight takeoff/resting, and client banking/pitch.
- Added species-specific walking, swimming, flight, cruise and chase speed profiles; refresh saved animals' movement attributes when loading.
- Amphibious animals now use water steering while swimming and native amphibious navigation on land.
- Rider input takes priority over autonomous movement/combat; swimming mounts use species speed and bounded acceleration.
- Reset stale failed-path state when starting a hunt so an earlier unreachable wandering destination cannot cancel a fresh combat target.
- Expanded wild predator aggression and retained diet, hunger, ownership, juvenile and command protections.
- Added focused checks for large-predator hunts, Survival-player aggression, takeoff, rendered wing animation progression, sustained swimming and amphibious transitions. Profile audits now check speeds and animation references.
- Runtime dependencies are unchanged; metadata templates now parse as valid JSON during Loom configuration.

## 9.3.4.0-dev.6 — Rebirth name and credits

- Renamed the mod, creative tab, guide and jar to Fossils and Archeology Rebirth.
- Added gemdYT to the mod authors and project credits while preserving original attribution.

## 9.3.4.0-dev.5 — Basic species instincts

- Added autonomous diet/size-based hunting, vanilla fish prey and food gained from kills. New predators start hungry enough to hunt; satiated animals stop hunting.
- Added proactive fleeing for timid animals and juveniles, plus ten defensive herbivore profiles.
- Added owner combat assistance, pet/conspecific protections and command priority over opportunistic hunting.
- Added pursuit time, distance and visibility limits; retained species attack timing, venom and knockback, and trigger attack animations on hits.
- Mixed diets and omnivores accept appropriate fish, meat and plant foods.
- Separated shared instincts from entity persistence and library composition; no additional runtime dependencies.

## 9.3.4.0-dev.4 — Animal movement and wild aquatics

- Fixed shared navigation cancelling its destination immediately, leaving spawned animals stationary.
- Added natural alligator gar in swamps, sturgeon in rivers, and coelacanth/nautilus in oceans, with native fish limits and water placement.
- Added focused runtime checks for real land/water/air movement, owner Follow/Stay, predator pursuit/damage and native aquatic spawning.
- Runtime dependencies are unchanged.

## 9.3.4.0-dev.3 — Fabric 26.3 development port

- One native Fabric source tree; removed Forge, Architectury and old-version source support.
- Registered 64 prehistoric species, 250 additional blocks and 879 catalog items.
- Shared animal brains, care, ownership, commands, riding, capture, venom/knockback and large-animal damage targets.
- Museum bone assembly and three display poses, plus a paginated dinopedia.
- Five native machine interfaces, 191 reloadable processing recipes, restoration, hopper/item-transfer automation and optional power.
- Native fossil/amber deposits, prehistoric vegetation, volcano biome/cones, ruins, temples, village buildings and two professions.
- Native Anu encounter, defeat-gated treasury and saved portal return.
- Converted loot to the 26.3 schema and added survival recipes, tool tags and excavation enchantments.
- Added deliberate Failuresaurus cultures and Tar Slime crafting for survival access.
- Build and representative normal-world client checks passed. Broad species/balance/modpack and dedicated-server gameplay testing remain open; see docs/PLAY_GUIDE.md.

## Historical upstream release notes

### Added
- Missing recipe for Tyrannosaurus tooth dagger
- Analyzing frozen meat or tar fossils can now return turtle dna
- Reduced dna rates when analyzing ink sac, string or chicken eggs
- Fuel duration field to culture vat and worktable recipes

### Fixed
- Deinonychus model jaw clipping
- World not loading if an entity has multiple variants of the same type
- Fossil entity not syncing the correct model and age on load
- Particles for various blocks
- Small texture issue in sifter menu
- Anu Boss music sometimes playing multiple times

### Mod Compatibility
- Fixed JEI display for recipes with "nothing" output
- Added support for EMI
- Added more items to fabric community tags
- Fixed Farmers Delight Roast Chicken not being considered meat
