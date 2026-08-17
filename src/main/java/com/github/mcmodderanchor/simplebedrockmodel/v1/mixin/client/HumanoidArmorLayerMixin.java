package com.github.mcmodderanchor.simplebedrockmodel.v1.mixin.client;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.ICustomArmorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.armortrim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public HumanoidArmorLayerMixin(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Inject(
            method = "renderModel",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sbm$renderMultiBufferArmor(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                            ArmorItem armorItem, A model, boolean withGlint,
                                            float red, float green, float blue, String armorResource,
                                            CallbackInfo ci) {
        if (model instanceof ICustomArmorRenderer renderer) {
            renderer.renderArmorToBuffer(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY, red, green, blue, 1.0F);
            ci.cancel();
        }
    }

    @Inject(
            method = "renderTrim",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sbm$skipVanillaTrim(ArmorMaterial armorMaterial, PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, ArmorTrim trim, A model, boolean innerTexture,
                                     CallbackInfo ci) {
        if (model instanceof ICustomArmorRenderer renderer) {
            renderer.renderArmorTrimToBuffer(armorMaterial, poseStack, bufferSource, packedLight, trim, innerTexture);
            if (!renderer.shouldRenderVanillaTrim()) {
                ci.cancel();
            }
        }
    }

    @Inject(
            method = "renderGlint",
            at = @At("HEAD"),
            cancellable = true
    )
    private void sbm$skipVanillaGlint(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                      A model, CallbackInfo ci) {
        if (model instanceof ICustomArmorRenderer renderer) {
            renderer.renderArmorGlintToBuffer(poseStack, bufferSource, packedLight);
            if (!renderer.shouldRenderVanillaGlint()) {
                ci.cancel();
            }
        }
    }
}
