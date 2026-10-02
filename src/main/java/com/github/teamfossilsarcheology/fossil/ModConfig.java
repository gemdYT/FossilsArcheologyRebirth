package com.github.teamfossilsarcheology.fossil;

import com.github.teamfossilsarcheology.fossil.config.ConfigEntry;
import com.github.teamfossilsarcheology.fossil.config.JsonConfig;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;

public final class ModConfig {
    @ConfigEntry(min = 20, max = 24000) public static int careIntervalTicks = 1200;
    @ConfigEntry(min = 20, max = 12000) public static int eggHatchTicks = 300;

    public static void load() {
        JsonConfig config = new JsonConfig(FabricLoader.getInstance().getConfigDir().resolve("fossil.json"), ModConfig.class);
        try { config.load(); config.save(); }
        catch (IOException exception) { throw new IllegalStateException("Cannot load fossil.json; original file preserved", exception); }
    }
}
