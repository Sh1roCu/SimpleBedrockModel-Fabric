package com.github.mcmodderanchor.simplebedrockmodel.v1.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public final class RenderHelper {
    public static void blit(PoseStack poseStack, float x, float y, float uOffset, float vOffset, float pWidth, float height, float textureWidth, float textureHeight) {
        blit(poseStack, x, y, pWidth, height, uOffset, vOffset, pWidth, height, textureWidth, textureHeight);
    }

    private static void blit(PoseStack poseStack, float x, float y, float pWidth, float height, float uOffset, float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        innerBlit(poseStack, x, x + pWidth, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth, textureHeight);
    }

    private static void innerBlit(PoseStack poseStack, float x1, float x2, float y1, float y2, float blitOffset, float uWidth, float vHeight, float uOffset, float vOffset, float textureWidth, float textureHeight) {
        innerBlit(poseStack.last().pose(), x1, x2, y1, y2, blitOffset, (uOffset + 0.0F) / textureWidth, (uOffset + uWidth) / textureWidth, (vOffset + 0.0F) / textureHeight, (vOffset + vHeight) / textureHeight);
    }

    private static void innerBlit(Matrix4f matrix, float x1, float x2, float y1, float y2, float blitOffset, float minU, float maxU, float minV, float maxV) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix, x1, y2, blitOffset).uv(minU, maxV).endVertex();
        bufferbuilder.vertex(matrix, x2, y2, blitOffset).uv(maxU, maxV).endVertex();
        bufferbuilder.vertex(matrix, x2, y1, blitOffset).uv(maxU, minV).endVertex();
        bufferbuilder.vertex(matrix, x1, y1, blitOffset).uv(minU, minV).endVertex();
        BufferUploader.draw(bufferbuilder.end());
    }

//    public static void enableItemEntityStencilTest() {
//        RenderSystem.assertOnRenderThread();
//        if (OptifineCompat.isOptifineInstalled()) {
//            // 以下代码用于应对 使用 optifine 的场景
//            int depthTextureId = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
//            int stencilTextureId = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
//            if (depthTextureId != GL30.GL_NONE && stencilTextureId == GL30.GL_NONE) {
//                GL30.glBindTexture(GL30.GL_TEXTURE_2D, depthTextureId);
//                int dataType = GL30.glGetTexLevelParameteri(GL30.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_DEPTH_TYPE);
//                if (dataType == GL30.GL_UNSIGNED_NORMALIZED) {
//                    int width = GL30.glGetTexLevelParameteri(GL30.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_WIDTH);
//                    int height = GL30.glGetTexLevelParameteri(GL30.GL_TEXTURE_2D, 0, GL30.GL_TEXTURE_HEIGHT);
//                    GlStateManager._texImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, width, height, 0, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, null);
//                    GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, 3553, depthTextureId, 0);
//                }
//            }
//        } else {
//            Minecraft.getInstance().getMainRenderTarget().enableStencil();
//        }
//        GL11.glEnable(GL11.GL_STENCIL_TEST);
//    }

    public static void disableItemEntityStencilTest() {
        RenderSystem.assertOnRenderThread();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void renderFirstPersonArm(LocalPlayer player, HumanoidArm hand, PoseStack matrixStack, int combinedLight) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher renderManager = mc.getEntityRenderDispatcher();
        PlayerRenderer renderer = (PlayerRenderer) renderManager.getRenderer(player);
        MultiBufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        int oldId = RenderSystem.getShaderTexture(0);
        RenderSystem.setShaderTexture(0, player.getSkinTextureLocation());

        if (hand == HumanoidArm.RIGHT) {
            renderRightHand(matrixStack, buffer, combinedLight, player, renderer);
        } else {
            renderLeftHand(matrixStack, buffer, combinedLight, player, renderer);
        }
        RenderSystem.setShaderTexture(0, oldId);
    }

    // 原版的方法只重置y轴且会被pose影响，故将实现复制出来
    @SuppressWarnings("UnstableApiUsage")
    public static void renderRightHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer, PlayerRenderer pRenderer) {
        if(!net.minecraftforge.client.ForgeHooksClient.renderSpecificFirstPersonArm(pPoseStack, pBuffer, pCombinedLight, pPlayer, HumanoidArm.RIGHT))
            renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, (pRenderer.getModel()).rightArm, (pRenderer.getModel()).rightSleeve, pRenderer);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void renderLeftHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer, PlayerRenderer pRenderer) {
        if(!net.minecraftforge.client.ForgeHooksClient.renderSpecificFirstPersonArm(pPoseStack, pBuffer, pCombinedLight, pPlayer, HumanoidArm.LEFT))
            renderHand(pPoseStack, pBuffer, pCombinedLight, pPlayer, (pRenderer.getModel()).leftArm, (pRenderer.getModel()).leftSleeve, pRenderer);
    }

    // todo 需要测试实现
    public static void renderHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, AbstractClientPlayer pPlayer, ModelPart pRendererArm, ModelPart pRendererArmwear, PlayerRenderer pRenderer) {
        PlayerModel<AbstractClientPlayer> playermodel = pRenderer.getModel();
        playermodel.attackTime = 0.0F;
        playermodel.crouching = false;
        playermodel.swimAmount = 0.0F;
        pRendererArm.xRot = 0.0F;
        pRendererArm.yRot = 0.0F;
        pRendererArm.zRot = 0.0F;
        pRendererArm.render(pPoseStack, pBuffer.getBuffer(RenderType.entitySolid(pPlayer.getSkinTextureLocation())), pCombinedLight, OverlayTexture.NO_OVERLAY);
        pRendererArmwear.xRot = 0.0F;
        pRendererArmwear.yRot = 0.0F;
        pRendererArmwear.zRot = 0.0F;
        pRendererArmwear.render(pPoseStack, pBuffer.getBuffer(RenderType.entityTranslucent(pPlayer.getSkinTextureLocation())), pCombinedLight, OverlayTexture.NO_OVERLAY);
    }
}
