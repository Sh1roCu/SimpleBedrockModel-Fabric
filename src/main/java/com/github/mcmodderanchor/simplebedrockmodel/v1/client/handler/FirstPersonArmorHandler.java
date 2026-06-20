package com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler;

import cn.sh1rocu.simplebedrockmodel.api.event.RenderArmEvent;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.IFPArmorHandRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockBone;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.impl.client.rendering.ArmorRendererRegistryImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * 通用的第一人称盔甲手臂渲染处理器。
 * 监听 RenderArmEvent，在玩家手臂上叠加渲染 Bedrock 盔甲模型的手臂部分。
 */
@Environment(EnvType.CLIENT)
public class FirstPersonArmorHandler {

    private static HumanoidModel<?> defaultModel;

    private static HumanoidModel<?> getDefaultModel() {
        if (defaultModel == null) {
            defaultModel = new HumanoidModel<>(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)
            );
        }
        return defaultModel;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        HumanoidArm arm = event.getArm();

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestStack.isEmpty()) return;

        var ext = ArmorRendererRegistryImpl.get(chestStack.getItem());
        var model = ext == null ? getDefaultModel() : ext;
        if (!(model instanceof IFPArmorHandRenderer armorRenderer)) return;

        armorRenderer.renderFirstPersonArmorArm(player, arm, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    // 取得骨骼除了自身变换以外的全局变换矩阵
    public static Matrix4f getGlobalTransform(@NotNull BedrockBone targetBone) {
        Matrix4f matrix = new Matrix4f();

        for (BedrockBone bone = targetBone.parent; bone != null; bone = bone.parent) {
            matrix.scaleLocal(bone.xScale, bone.yScale, bone.zScale);
            matrix.rotateLocal(bone.rotation);
            matrix.translateLocal(bone.x / 16.0F, bone.y / 16.0F, bone.z / 16.0F);
        }

        return matrix;
    }
}
