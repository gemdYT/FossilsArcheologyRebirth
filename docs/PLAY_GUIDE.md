# Fossils and Archeology Rebirth — Fabric 26.3 play guide

Version **0.0.1** targets Minecraft **26.3**, Java **25**, and Fabric Loader **0.19.5 or newer**. This starts Rebirth's own release version sequence. An earlier Fabric development test world can be used; back it up first. Original mod saves are not supported migration inputs. Rebirth project credit: **gemdYT**; see CREDITS.md for attribution.

## Installation

Extract `fossils-archeology-rebirth-fabric-26.3-0.0.1-install-kit.zip` and copy its **six** `mods/*.jar` files into a Fabric 26.3 instance. Remove the previous fossil mod jar when upgrading. The standalone mod jar is `fossils-archeology-rebirth-fabric-26.3-0.0.1.jar`; it requires the five external libraries when installed separately. Restart Minecraft after replacing the jar so mod names and tooltip labels reload. The kit includes the mod, Fabric API 0.161.0+26.3, GeckoLib 5.5.7, TerraBlender 26.3.0.0.9, SmartBrainLib 2.0.2 and Structure Pool API 1.3.0+26.3-fabric. Team Reborn Energy 5.0.0 is bundled.

For development, double-click `launch-game.cmd` or run `.\gradlew.bat runClient`. The creative Fossils and Archeology Rebirth tab contains the content. This is the first Rebirth release; broad balance and modpack testing remain open.

## Survival and machines

Mine fossil deposits with a pickaxe, or sift sand/gravel. Biological finds produce animal DNA; plant finds produce fossil seeds/saplings that need another analysis. Relics yield artifacts, pottery, figurines and exploration items. Craft Bio-Goo from rotten flesh.

| Machine | Input | Second slot | Output |
| --- | --- | --- | --- |
| Analyzer | Biological fossils, species bones, frozen meat | Empty | Animal DNA |
| Analyzer | Plant fossils, fossil seeds/saplings | Empty | Plant finds, then living plants |
| Analyzer | Relic scrap | Empty | Artifacts and archaeological finds |
| Culture Vat | DNA | Bio-Goo | Matching egg/embryo |
| Sifter | Sand, red sand, gravel | Empty | Fossils and relics |
| Worktable | Broken weapons/helmet | Iron ingot | Ancient equipment |
| Worktable | Damaged pottery/destroyed figurines | Pottery shard | Restored artifact; another pass produces pristine artifacts |
| Feeder | Suitable animal food | More food | Feeds nearby animals within eight blocks |

Right-click opens any of the five machine interfaces. Shift-click transfers items. Processing pauses when the output cannot accept its result. Recipes define duration; the dinopedia shows available processing information.

For deliberate hostile experiments, culture rotten flesh with Bio-Goo to obtain a Failuresaurus spawn egg. Craft eight tar fossils with Bio-Goo for a Tar Slime spawn egg. These modern recipes make both creatures available in survival without random failures destroying a normal DNA culture.

Hoppers insert input from above, fuel from the sides and extract finished output below. Feeder slots both accept food. Transactional item transfer and Energy API insertion are exposed to other Fabric mods. Optional power speeds processing; normal ingredient fuel is still needed.

Place a cultured egg/embryo to incubate for about 15 seconds. Keep the space above clear; aquatic species need water above. Blocked eggs retry. Inventories, incubation species and in-progress ingredients/results persist. Breaking a busy machine returns its ingredients.

## Animals and exhibits

The port registers 64 prehistoric species using shared profiles, original assets and available animations/sounds. Feed suitable food to tame: two juvenile feeds, four non-aggressive adult feeds or eight aggressive adult feeds. Quaggas retain Minecraft's horse behavior.

Four surviving aquatic species now spawn naturally in water: alligator gar in swamps and mangrove swamps, sturgeon in rivers, and coelacanth/nautilus in oceans. They share Minecraft's fish population limit and require water around their spawn position below sea level. Wild fish can despawn; revived, bred, tamed, named or bucket-captured animals are retained. Extinct species still come from fossil incubation. Datapacks can extend the `fossil:spawns/<species>` biome tags.

Use a whip, skull stick, or sneak with an empty hand to cycle your animal's Wander/Follow/Stay command. A laser pointer directs owned animals toward a clicked block; the magic conch recalls owned aquatic animals. Saddle eligible non-flying adults and interact with an empty hand to ride. The jump key hops on land or rises in water. Mounted animals stop autonomous pursuit and roaming so those goals do not compete with rider input.

Hunger and mood affect healing, breeding and starvation. Feeders and enclosure toys help care for animals. Land movement uses native navigation. Flight and swimming use persistent 3D destinations, smooth acceleration, limited turn rates and body-sized obstacle probes. Amphibious animals swim in water and resume native walking on land. Flyers take off, cruise, periodically descend to rest, and use their wing animation with gentle banking and pitch. Give large swimmers deep, broad water and flyers open airspace; tiny tanks and low ceilings limit what they can do. Species profiles separately tune walking, swimming, flight, cruise and pursuit speed. These shared behaviors still need species-by-species playtesting.

| Animal type | Basic behavior |
| --- | --- |
| Carnivores | Hungry adults choose visible prey of suitable size, chase, attack and gain food from kills. They avoid their own species and owned pets. |
| Fish eaters | Hunt smaller fish, including vanilla fish. Aquatic hunters pursue targets in water; mixed fish/meat diets also accept land prey. |
| Timid herbivores, passive animals and juveniles | Flee nearby hunting predators and attackers. Juveniles follow nearby adults of their species. |
| Defensive herbivores | Stand their ground against attackers and close hunting predators while healthy. Low-health animals retreat. |
| Aggressive adults | Can target Survival/Adventure players within twelve blocks; Creative/Spectator players and owners are ignored. |
| Owned predators and defensive animals | Assist nearby owners in combat. Follow and pointer commands suppress opportunistic hunting; Stay prevents combat. |

Defensive profiles are ankylosaurus, brachiosaurus, diplodocus, elasmotherium, mammoth, pachycephalosaurus, pachyrhinosaurus, stegosaurus, therizinosaurus and triceratops. Other adults gather loosely with their own species and approach suitable held food. Omnivores accept plant food, fish and meat. Existing venom, knockback and species attack intervals remain active.

New predators start at 70 hunger so they can demonstrate hunting immediately. They stop looking for meals at 95 hunger; existing animals retain saved hunger. Chases end after twenty seconds, four seconds without sight, or a target escaping beyond thirty-two blocks. An abandoned target gets a short retry delay.

Use chicken essence to accelerate juvenile growth; stunted essence pauses it. A water bucket captures eligible small aquatic animals and preserves their care/age state.

Use a species skull on the ground to place an exhibit. Add matching arm, foot, leg, rib, skull, tail, unique and vertebra bones. Duplicate groups are rejected. Empty-hand interaction cycles three poses; attacking your exhibit returns its assembled bones. Displayed anatomy is grouped approximately across different models.

Use the dinopedia on animals/exhibits or machines for details, or in the air for the guide. Pages are navigable. Historical species descriptions currently use the included English text.

## Exploration

Fossils, amber, prehistoric trees/plants and volcanic terrain generate in new chunks. Original ruins/temples and ten village buildings use native templates. Archaeologist and paleontologist villagers have five trade levels.

Find or craft an Anu statue and use an ancient key to enter the lair. Use Easy, Normal or Hard difficulty for the encounter. Defeat Anu, then use a scarab gem on a block inside the lair to enter the treasury. The home portal returns you to your recorded origin. The encounter uses shared native combat with reinforcement and blast phases.

## Testing and simplified behavior

Native stairs, doors, beds, spears, armor, jukebox songs and barrel inventories replace much of the old custom code. Furniture and some special blocks have simpler geometry/animation. Tar is a non-flowing slowing pool. Plants use shared growth rather than the historical ecology simulation. Large animals have additional server-owned damage targets; rider positioning and anatomy remain approximate.

The focused client check covers actual land wandering, swimming, flight, owner following, Stay, autonomous land/fish hunting and eating, proactive fleeing, defensive herbivore retaliation, owner combat assistance, conspecific protection and escaped-prey abandonment. It additionally checks grounded Quetzalcoatlus takeoff and client flight-animation progression, sustained aquatic cruising, T. rex/Megalodon hunting and aggression toward Survival players, amphibious swimming/land pursuit, and rider priority over autonomous goals. It also covers native aquatic spawning in controlled swamp/river/ocean habitats, a normal generated world, ownership/save state, skeleton assembly, capture, native loot, template inventories, villager trades, machine processing/screens/automation, volcano placement and the Anu reward gate/portal return. It does not establish every species' combat, rendering or riding quality, long-session stability, distribution balance, or compatibility with every modpack.

A separate dedicated-server session still needs testing after the server operator accepts Minecraft's EULA. No acceptance is written automatically.

Build/check: `.\gradlew.bat build`. Client smoke: `.\gradlew.bat runClientGameTest`. Data audit: `python tools/audit_content.py`. The audit checks references and acquisition sources, not full survival reachability or balance.
