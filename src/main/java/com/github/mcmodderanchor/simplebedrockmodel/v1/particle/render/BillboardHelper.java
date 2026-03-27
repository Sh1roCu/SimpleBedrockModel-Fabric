package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render;

import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.component.ParticleAppearanceBillboard;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Billboard quad 构建工具。
 */
@OnlyIn(Dist.CLIENT)
public final class BillboardHelper {

    private BillboardHelper() {}

    public static void renderBillboard(ParticleInstance particle, PoseStack poseStack,
                                        VertexConsumer consumer, int light,
                                        ParticleAppearanceBillboard.FaceCameraMode mode,
                                        Matrix4f emitterTransform,
                                        boolean localPos, boolean localRot,
                                        Matrix4f worldToView) {
        Matrix4f pose = poseStack.last().pose();

        // 构建最终的变换矩阵：
        // 局部空间粒子 → pose × emitterTransform（跟随发射器）
        // 世界空间粒子 → worldToView（世界坐标 → 视图空间）
        Matrix4f effectivePose;
        if (!particle.worldSpace && localPos) {
            effectivePose = new Matrix4f(pose).mul(emitterTransform);
        } else if (particle.worldSpace) {
            effectivePose = worldToView;
        } else {
            effectivePose = pose;
        }

        // 用 effectivePose 把粒子坐标变换到视图空间
        Vector4f viewPos = effectivePose.transform(new Vector4f(particle.x, particle.y, particle.z, 1));

        // 使用发射时的骨骼缩放快照计算粒子大小
        float scale = particle.spawnScale;

        float hw = particle.width * scale;
        float hh = particle.height * scale;

        // 确定速度方向使用的变换矩阵
        // 如果 rotation 也是局部空间的，速度需要经过发射器变换
        // 世界空间粒子的速度已在世界空间中，需要用 worldToView 转到视图空间
        float velX = particle.vx, velY = particle.vy, velZ = particle.vz;
        Matrix4f velPose;
        if (particle.worldSpace) {
            velPose = worldToView;
        } else if (localRot) {
            velPose = effectivePose;
        } else {
            velPose = pose;
        }

        // 计算 billboard 的两个轴向量（视图空间中）
        Vector3f axisX = new Vector3f(1, 0, 0);
        Vector3f axisY = new Vector3f(0, 1, 0);
        applyBillboardAxes(axisX, axisY, mode, velX, velY, velZ, velPose);

        // 应用粒子自旋旋转
        if (particle.rotation != 0) {
            float rad = (float) Math.toRadians(particle.rotation);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            float ax = axisX.x, ay = axisX.y, az = axisX.z;
            float bx = axisY.x, by = axisY.y, bz = axisY.z;
            axisX.set(ax * cos + bx * sin, ay * cos + by * sin, az * cos + bz * sin);
            axisY.set(-ax * sin + bx * cos, -ay * sin + by * cos, -az * sin + bz * cos);
        }

        // 直接在视图空间中构建 4 个顶点
        float cx = viewPos.x, cy = viewPos.y, cz = viewPos.z;

        // 使用 identity pose，顶点已经在视图空间中
        Matrix4f identity = new Matrix4f();
        Matrix3f normalId = new Matrix3f();

        vertex(consumer, identity, normalId,
                cx - axisX.x * hw - axisY.x * hh, cy - axisX.y * hw - axisY.y * hh, cz - axisX.z * hw - axisY.z * hh,
                particle.u0, particle.v1, particle, light);
        vertex(consumer, identity, normalId,
                cx - axisX.x * hw + axisY.x * hh, cy - axisX.y * hw + axisY.y * hh, cz - axisX.z * hw + axisY.z * hh,
                particle.u0, particle.v0, particle, light);
        vertex(consumer, identity, normalId,
                cx + axisX.x * hw + axisY.x * hh, cy + axisX.y * hw + axisY.y * hh, cz + axisX.z * hw + axisY.z * hh,
                particle.u1, particle.v0, particle, light);
        vertex(consumer, identity, normalId,
                cx + axisX.x * hw - axisY.x * hh, cy + axisX.y * hw - axisY.y * hh, cz + axisX.z * hw - axisY.z * hh,
                particle.u1, particle.v1, particle, light);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                                float x, float y, float z,
                                float u, float v,
                                ParticleInstance particle, int light) {
        consumer.vertex(pose, x, y, z)
                .color(particle.r, particle.g, particle.b, particle.a)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }

    /**
     * 根据朝向模式计算 billboard 的 X/Y 轴方向（视图空间中的单位向量）。
     * axisX 对应 quad 的宽度方向，axisY 对应高度方向。
     *
     * @param velX/velY/velZ 粒子速度（在粒子自身的坐标空间中）
     * @param velPose 用于将速度变换到视图空间的矩阵
     */
    private static void applyBillboardAxes(Vector3f axisX, Vector3f axisY,
                                            ParticleAppearanceBillboard.FaceCameraMode mode,
                                            float velX, float velY, float velZ,
                                            Matrix4f velPose) {
        switch (mode) {
            case ROTATE_XYZ, LOOKAT_XYZ -> {
                axisX.set(1, 0, 0);
                axisY.set(0, 1, 0);
            }
            case ROTATE_Y, LOOKAT_Y -> {
                Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                float pitch = (float) Math.toRadians(camera.getXRot());
                axisX.set(1, 0, 0);
                axisY.set(0, (float) Math.cos(pitch), (float) -Math.sin(pitch));
            }
            case LOOKAT_DIRECTION -> {
                Vector3f viewVel = transformDirection(velPose, velX, velY, velZ);
                float velLen = viewVel.length();
                if (velLen > 0.0001f) {
                    Vector3f dir = new Vector3f(viewVel).normalize();
                    Vector3f forward = new Vector3f(0, 0, 1);
                    Vector3f up = new Vector3f(forward).cross(dir);
                    float upLen = up.length();
                    if (upLen > 0.0001f) {
                        up.normalize();
                        axisX.set(dir);
                        axisY.set(up);
                    }
                }
            }
            case DIRECTION_X, DIRECTION_Y, DIRECTION_Z -> {
                Vector3f viewVel = transformDirection(velPose, velX, velY, velZ);
                float sLen = viewVel.x * viewVel.x + viewVel.y * viewVel.y;
                if (sLen > 0.0001f) {
                    float angle = (float) Math.atan2(viewVel.y, viewVel.x);
                    float cos = (float) Math.cos(angle);
                    float sin = (float) Math.sin(angle);
                    axisX.set(cos, sin, 0);
                    axisY.set(-sin, cos, 0);
                }
            }
            default -> {
                // EMITTER_TRANSFORM_* 等模式：保持默认
            }
        }
    }

    /**
     * 用矩阵的 3x3 部分（含缩放+旋转）变换一个方向向量。
     */
    private static Vector3f transformDirection(Matrix4f pose, float x, float y, float z) {
        return new Vector3f(
                pose.m00() * x + pose.m10() * y + pose.m20() * z,
                pose.m01() * x + pose.m11() * y + pose.m21() * z,
                pose.m02() * x + pose.m12() * y + pose.m22() * z
        );
    }
}
