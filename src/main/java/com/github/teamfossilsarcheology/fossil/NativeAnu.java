package com.github.teamfossilsarcheology.fossil;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.level.Level;

/** Three-phase native encounter: melee, reinforcements, then a visible shockwave. */
public final class NativeAnu extends PiglinBrute {
    private final ServerBossEvent bar = new ServerBossEvent(getUUID(), Component.literal("Anu"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private int abilityTicks;
    public NativeAnu(EntityType<? extends PiglinBrute> type, Level level) { super(type, level); setImmuneToZombification(true); setPersistenceRequired(); }
    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bar.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bar.removePlayer(player); }
    @Override protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        float health = getHealth() / getMaxHealth();
        bar.setProgress(health); bar.setName(Component.literal(health > .66f ? "Anu — Guardian" : health > .33f ? "Anu — Commander" : "Anu — Wrath"));
        if (getTarget() == null) return;
        abilityTicks++;
        if (health <= .66f && abilityTicks % 300 == 0 && level.getEntitiesOfClass(PiglinBrute.class, getBoundingBox().inflate(24)).size() < 6) {
            for (int side : new int[]{-1, 1}) {
                var sentry = NativeHostiles.SENTRY.create(level, EntitySpawnReason.TRIGGERED);
                if (sentry != null) { sentry.setPos(getX() + side * 3, getY(), getZ()); if (level.noCollision(sentry)) { sentry.setTarget(getTarget()); level.addFreshEntity(sentry); } }
            }
        }
        if (health <= .33f && abilityTicks % 100 == 80) level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, getX(), getY() + .2, getZ(), 45, 4, .1, 4, .01);
        if (health <= .33f && abilityTicks % 100 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, getX(), getY() + 1, getZ(), 8, 3, .2, 3, 0);
            for (var player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(5), p -> !p.isCreative() && !p.isSpectator())) {
                player.hurtServer(level, damageSources().mobAttack(this), 6);
                player.push(player.getX() - getX(), .5, player.getZ() - getZ());
            }
        }
    }
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) { super.addAdditionalSaveData(output); output.putInt("AbilityTicks", abilityTicks); }
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) { super.readAdditionalSaveData(input); abilityTicks = Math.max(0, input.getIntOr("AbilityTicks", 0)); }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source) { super.die(source); if (level() instanceof ServerLevel level) NativePortals.defeated(level); bar.removeAllPlayers(); }
}
