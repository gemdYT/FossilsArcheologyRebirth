package com.github.teamfossilsarcheology.fossil.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.github.teamfossilsarcheology.fossil.ModContent;
import com.github.teamfossilsarcheology.fossil.NativeAnimals;
import com.github.teamfossilsarcheology.fossil.NativeAnimal;
import com.github.teamfossilsarcheology.fossil.AnimalSpecies;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.animal.equine.BabyHorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.resources.Identifier;

public final class FossilClient implements ClientModInitializer {
    private static final com.geckolib.constant.dataticket.DataTicket<Integer> DISPLAY_BONES = com.geckolib.constant.dataticket.DataTicket.create("fossil:display_bones", Integer.class);
    private static final com.geckolib.constant.dataticket.DataTicket<float[]> FLIGHT_ATTITUDE = com.geckolib.constant.dataticket.DataTicket.create("fossil:flight_attitude", float[].class);
    @Override public void onInitializeClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.github.teamfossilsarcheology.fossil.DinopediaPayload.TYPE, (payload, context) -> context.client().gui.setScreen(new DinopediaScreen(payload.text())));
        EntityRendererRegistry.register(com.github.teamfossilsarcheology.fossil.AnimalPart.TYPE, net.minecraft.client.renderer.entity.NoopRenderer::new);
        com.github.teamfossilsarcheology.fossil.NativeMachineMenu.TYPES.values().forEach(type -> net.minecraft.client.gui.screens.MenuScreens.register(type, NativeMachineScreen::new));
        NativeAnimals.TYPES.forEach((name, type) -> EntityRendererRegistry.register(type,
                context -> new AnimalRenderer(context, NativeAnimals.profile(type))));
        NativeAnimals.DISPLAYS.forEach((name, type) -> EntityRendererRegistry.register(type, context -> new AnimalRenderer(context, NativeAnimals.profile(type), true)));
        EntityRendererRegistry.register(NativeAnimals.QUAGGA, QuaggaRenderer::new);
        EntityRendererRegistry.register(com.github.teamfossilsarcheology.fossil.NativeHostiles.ANU, context -> new NativeBruteRenderer(context, "anu_boss"));
        EntityRendererRegistry.register(com.github.teamfossilsarcheology.fossil.NativeHostiles.SENTRY, context -> new NativeBruteRenderer(context, "sentry_piglin"));
        EntityRendererRegistry.register(com.github.teamfossilsarcheology.fossil.NativeHostiles.TAR_SLIME, context -> new net.minecraft.client.renderer.entity.SlimeRenderer(context) {
            @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.SlimeRenderState state) { return ModContent.id("textures/entity/tar_slime.png"); }
        });
        EntityRendererRegistry.register(com.github.teamfossilsarcheology.fossil.NativeHostiles.FAILURE, context -> new GeoEntityRenderer<>(context, new GeoModel<com.github.teamfossilsarcheology.fossil.NativeFailure>() {
            @Override public Identifier getModelResource(GeoRenderState state) { return ModContent.id("entity/failuresaurus_theropod"); }
            @Override public Identifier getTextureResource(GeoRenderState state) { return ModContent.id("textures/entity/failuresaurus/failuresaurus_theropod.png"); }
            @Override public Identifier getAnimationResource(com.github.teamfossilsarcheology.fossil.NativeFailure entity) { return ModContent.id("failuresaurus_theropod"); }
        }));
    }
    private static final class NativeBruteRenderer extends net.minecraft.client.renderer.entity.PiglinRenderer {
        private final String name;
        NativeBruteRenderer(EntityRendererProvider.Context context, String name) { super(context, ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BABY, ModelLayers.PIGLIN_BRUTE_ARMOR, ModelLayers.PIGLIN_BABY_ARMOR); this.name = name; }
        @Override public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.PiglinRenderState state) { return ModContent.id("textures/entity/" + name + ".png"); }
    }

    private static final class AnimalRenderer extends GeoEntityRenderer<NativeAnimal, LivingEntityRenderState> {
        AnimalRenderer(EntityRendererProvider.Context context, AnimalSpecies species) {
            this(context, species, false);
        }
        AnimalRenderer(EntityRendererProvider.Context context, AnimalSpecies species, boolean display) {
            super(context, new AnimalModel(species, display));
            withScale(species.scale());
            if (!display && species.flying()) withRenderLayer(new com.geckolib.renderer.layer.GeoRenderLayer<NativeAnimal, Void, LivingEntityRenderState>(this) {
                @Override public void preRender(com.geckolib.renderer.base.RenderPassInfo<LivingEntityRenderState> pass, net.minecraft.client.renderer.SubmitNodeCollector collector) {
                    float[] tilt = pass.getOrDefaultGeckolibData(FLIGHT_ATTITUDE, new float[]{0, 0});
                    pass.addBoneUpdater((info, snapshots) -> {
                        for (var root : info.model().topLevelBones()) {
                            var pose = snapshots.get(root);
                            pose.setRotX(pose.getRotX() + tilt[0]).setRotZ(pose.getRotZ() + tilt[1]);
                        }
                    });
                }
            });
            if (display) withRenderLayer(new com.geckolib.renderer.layer.GeoRenderLayer<NativeAnimal, Void, LivingEntityRenderState>(this) {
                @Override public void preRender(com.geckolib.renderer.base.RenderPassInfo<LivingEntityRenderState> pass, net.minecraft.client.renderer.SubmitNodeCollector collector) {
                    int mask = pass.getOrDefaultGeckolibData(DISPLAY_BONES, 0);
                    pass.addBoneUpdater((info, snapshots) -> hideMissingBones(info.model().topLevelBones(), snapshots, mask));
                }
            });
        }
    }
    private static void hideMissingBones(com.geckolib.cache.model.GeoBone[] bones, com.geckolib.renderer.base.BoneSnapshots snapshots, int mask) {
        for (var bone : bones) {
            String name = bone.name().toLowerCase(java.util.Locale.ROOT);
            int group = name.contains("head") || name.contains("jaw") || name.contains("horn") ? 4
                    : name.contains("tail") ? 5 : name.contains("foot") || name.contains("toe") || name.contains("claw") ? 1
                    : name.contains("arm") || name.contains("wing") || name.contains("flipper") ? 0
                    : name.contains("leg") || name.contains("thigh") ? 2 : name.contains("neck") || name.contains("spine") ? 7
                    : name.contains("body") || name.contains("chest") || name.contains("torso") ? 3 : 6;
            snapshots.get(bone).skipRender((mask & (1 << group)) == 0).skipChildrenRender(false);
            hideMissingBones(bone.children(), snapshots, mask);
        }
    }
    private static final class AnimalModel extends GeoModel<NativeAnimal> {
        private final AnimalSpecies species;
        private final boolean display;
        AnimalModel(AnimalSpecies species, boolean display) { this.species = species; this.display = display; }
        @Override public void addAdditionalStateData(NativeAnimal animal, Object relatedObject, GeoRenderState state) {
            if (display) state.addGeckolibData(DISPLAY_BONES, animal.displayBones());
            else if (species.flying()) state.addGeckolibData(FLIGHT_ATTITUDE, new float[]{animal.flightPitch() * net.minecraft.util.Mth.DEG_TO_RAD, animal.flightBank() * net.minecraft.util.Mth.DEG_TO_RAD});
        }
        @Override public Identifier getModelResource(GeoRenderState state) { return ModContent.id("entity/" + species.model()); }
        @Override public Identifier getTextureResource(GeoRenderState state) {
            if (display) return ModContent.id("textures/entity/" + species.name() + "/" + species.name() + "_skeleton.png");
            return ModContent.id(state instanceof LivingEntityRenderState living && living.isBaby ? species.babyTexture() : species.texture());
        }
        @Override public Identifier getAnimationResource(NativeAnimal entity) { return ModContent.id(species.model()); }
    }
    private static final class QuaggaRenderer extends AbstractHorseRenderer<Horse, HorseRenderState, HorseModel> {
        QuaggaRenderer(EntityRendererProvider.Context context) {
            super(context, new HorseModel(context.bakeLayer(ModelLayers.HORSE)), new BabyHorseModel(context.bakeLayer(ModelLayers.HORSE_BABY)));
        }
        @Override public Identifier getTextureLocation(HorseRenderState state) { return ModContent.id("textures/entity/quagga/quagga_saddled.png"); }
        @Override public HorseRenderState createRenderState() { return new HorseRenderState(); }
    }

}
