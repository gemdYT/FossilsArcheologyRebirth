# Library decision record

The selected 26.3 libraries are now integrated: Fabric API, GeckoLib, TerraBlender, SmartBrainLib and Structure Pool API, with Team Reborn Energy bundled. See [architecture and exact pins](ARCHITECTURE.md).

SmartBrainLib handles shared animal sensors/behaviors; native horse behavior remains for Quagga. Structure Pool API injects the ten village houses and caps their occurrence. TerraBlender handles the volcano region/surface rules. Native data handles the actual trees, vegetation, ores, ruins and dimensions.

Minecraft/Fabric APIs cover saves, care, ownership, incubation, menus, dinopedia, item transfer, portals and configuration. The small internal damage-target implementation replaces the unavailable historical multipart dependency. No additional library was needed for these systems.

JEI/Jade integrations remain optional enhancements. The native dinopedia provides machine information without requiring either. No extra GUI/configuration, component, dimension or animation framework is included.

The two JSON audit files preserve the October 2, 2026 version research and original candidate comparisons. They are historical selection records; current runtime pins are in gradle.properties. Beta labels were not used to reject the selected releases. Compatibility still needs testing in the intended modpack.
