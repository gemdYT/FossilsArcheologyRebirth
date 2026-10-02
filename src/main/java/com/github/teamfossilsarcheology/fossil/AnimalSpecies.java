package com.github.teamfossilsarcheology.fossil;

/** Data-driven dimensions, assets, diet and combat tuning shared by every species. */
public record AnimalSpecies(String name, String movement, String diet, boolean aggressive,
        float width, float height, float scale, double health, double speed, double damage, double armor,
        String model, String texture, String babyTexture, String idle, String walk, String swim, String fly, String attack,
        int attackDelay, int attackInterval, double knockback, int poisonTicks, boolean defensive) {
    public boolean aquatic() { return movement.equals("AQUATIC"); }
    public boolean amphibious() { return aquatic() || movement.equals("SEMI_AQUATIC"); }
    public boolean flying() { return movement.equals("FLIGHT"); }
    public boolean carnivore() { return diet.contains("CARNIVORE") || diet.equals("PISCIVORE"); }
}
