package com.github.tartaricacid.simplebedrockmodel.client.bedrock.model;

import com.github.tartaricacid.simplebedrockmodel.client.bedrock.pojo.*;
import com.github.tartaricacid.simplebedrockmodel.client.compat.sodium.SodiumBedrockCubeBox;
import com.github.tartaricacid.simplebedrockmodel.client.compat.sodium.SodiumBedrockCubePerFace;
import com.github.tartaricacid.simplebedrockmodel.client.compat.sodium.SodiumCompat;
import com.google.common.collect.Collections2;
import com.maydaymemory.mae.basic.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class BedrockModel implements Skeleton {
    private static final Vector3f NORMAL_SCALE = new Vector3f(1, 1, 1);
    /**
     * 储存 name -> bone 的映射关系
     */
    protected final HashMap<String, BedrockBone> boneMap = new HashMap<>();
    /**
     * 储存 index -> bone 的映射关系
     */
    protected final ArrayList<BedrockBone> boneIndex = new ArrayList<>();
    /**
     * 顶层 bone，没有对应的 name 和 index
     */
    protected final BedrockBone root = new BedrockBone();
    /**
     * 模型的 AABB
     */
    protected AABB renderBoundingBox;
    /**
     * 模型默认的 Pose
     */
    private final Pose bindingPose;

    public BedrockModel(BedrockModelPOJO pojo) {
        if (BedrockVersion.isLegacyVersion(pojo)) {
            loadLegacyModel(pojo);
        }
        if (BedrockVersion.isNewVersion(pojo)) {
            loadNewModel(pojo);
        }
        bindingPose = initializeBindingPose();
    }

    protected Pose initializeBindingPose() {
        PoseBuilder poseBuilder = new ArrayPoseBuilder();
        BoneTransformFactory transformFactory = new ZYXBoneTransformFactory();
        for (int i = 0; i < boneIndex.size(); i++) {
            BedrockBone part = boneIndex.get(i);
            BoneTransform boneTransform = transformFactory.createBoneTransform(
                    i,
                    new Vector3f(part.x, part.y, part.z),
                    new Vector3f(part.xRot, part.yRot, part.zRot),
                    NORMAL_SCALE
            );
            poseBuilder.addBoneTransform(boneTransform);
        }
        return poseBuilder.toPose();
    }

    protected void loadNewModel(BedrockModelPOJO pojo) {
        assert pojo.getGeometryModelNew() != null;
        pojo.getGeometryModelNew().deco();

        Description description = pojo.getGeometryModelNew().getDescription();
        // 材质的长度、宽度
        int texWidth = description.getTextureWidth();
        int texHeight = description.getTextureHeight();

        float[] offset = description.getVisibleBoundsOffset();
        float offsetX = offset[0];
        float offsetY = offset[1];
        float offsetZ = offset[2];
        float width = description.getVisibleBoundsWidth() / 2.0f;
        float height = description.getVisibleBoundsHeight() / 2.0f;
        renderBoundingBox = new AABB(offsetX - width, offsetY - height, offsetZ - width, offsetX + width, offsetY + height, offsetZ + width);

        BonesItem[] bones = pojo.getGeometryModelNew().getBones();
        initialWithBoneItems(bones,texWidth, texHeight);
    }

    protected void loadLegacyModel(BedrockModelPOJO pojo) {
        assert pojo.getGeometryModelLegacy() != null;
        pojo.getGeometryModelLegacy().deco();

        // 材质的长度、宽度
        int texWidth = pojo.getGeometryModelLegacy().getTextureWidth();
        int texHeight = pojo.getGeometryModelLegacy().getTextureHeight();

        float[] offset = pojo.getGeometryModelLegacy().getVisibleBoundsOffset();
        float offsetX = offset[0];
        float offsetY = offset[1];
        float offsetZ = offset[2];
        float width = pojo.getGeometryModelLegacy().getVisibleBoundsWidth() / 2.0f;
        float height = pojo.getGeometryModelLegacy().getVisibleBoundsHeight() / 2.0f;
        renderBoundingBox = new AABB(offsetX - width, offsetY - height, offsetZ - width, offsetX + width, offsetY + height, offsetZ + width);

        BonesItem[] bones = pojo.getGeometryModelLegacy().getBones();
        initialWithBoneItems(bones, texWidth, texHeight);
    }

    protected BedrockCube createCubeBox(float texOffX, float texOffY, float x, float y, float z, float width, float height, float depth,
                                        float delta, boolean mirror, float texWidth, float texHeight) {
        if (SodiumCompat.isSodiumInstalled()) {
            return new SodiumBedrockCubeBox(texOffX, texOffY, x, y, z, width, height, depth, delta, mirror, texWidth, texHeight);
        }
        return new BedrockCubeBox(texOffX, texOffY, x, y, z, width, height, depth, delta, mirror, texWidth, texHeight);
    }

    protected BedrockCube createCubePerFace(float x, float y, float z, float width, float height, float depth, float delta,
                                            float texWidth, float texHeight, FaceUVsItem faces) {
        if (SodiumCompat.isSodiumInstalled()) {
            return new SodiumBedrockCubePerFace(x, y, z, width, height, depth, delta, texWidth, texHeight, faces);
        }
        return new BedrockCubePerFace(x, y, z, width, height, depth, delta, texWidth, texHeight, faces);
    }

    @ParametersAreNonnullByDefault
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        root.render(poseStack, buffer, packedLight, packedOverlay);
    }

    public AABB getRenderBoundingBox() {
        return renderBoundingBox;
    }

    public HashMap<String, BedrockBone> getBoneMap() {
        return boneMap;
    }

    public ArrayList<BedrockBone> getBoneIndexes() {
        return boneIndex;
    }

    private void convertPivot(BedrockBone root) {
        // 后序遍历，子节点计算完后计算当前节点
        for (BedrockBone child : root.getChildren()) {
            convertPivot(child);
        }
        if (root.parent != null) {
            root.x = root.x - root.parent.x;
            root.y = root.y - root.parent.y;
            root.z = root.z - root.parent.z;
        }
    }

    private void initialWithBoneItems(BonesItem[] bones, int texWidth, int texHeight) {
        // 建立 name -> bone 和 index -> bone 的映射，对 BedrockPart 实例进行第一遍初始化
        for (BonesItem bone : bones) {
            BedrockBone part = new BedrockBone();
            float[] pivot = bone.getPivot();
            float[] rotation = bone.getRotation();
            // 这里先简单的将左手系坐标转换为右手系坐标，待父子关系建立后再转换为相对坐标
            if (pivot != null) {
                part.x = -pivot[0];
                part.y =  pivot[1];
                part.z =  pivot[2];
            }
            if (rotation != null) {
                part.xRot = (float) -Math.toRadians(rotation[0]);
                part.yRot = (float) -Math.toRadians(rotation[1]);
                part.zRot = (float)  Math.toRadians(rotation[2]);
            }
            part.mirror = bone.isMirror();
            part.index = boneIndex.size();

            boneIndex.add(part);
            boneMap.put(bone.getName(), part);
        }
        // 建立父子关系，塞入 cubes（因为 cube 的 origin 需要依赖绝对坐标的 pivot 计算，因此必须排在计算相对 pivot 之前）
        for (BonesItem bone : bones) {
            BedrockBone part = boneMap.get(bone.getName());
            // 父骨骼的名称，可能为空
            @Nullable String parentName = bone.getParent();
            if (parentName != null) {
                part.parent = boneMap.get(parentName);
                Objects.requireNonNull(part.parent);
                part.parent.addChild(part);
            } else {
                part.parent = root;
                root.addChild(part);
            }
            // 塞入 cubes
            if (bone.getCubes() != null) {
                for (CubesItem cube : bone.getCubes()) {
                    float[] uv = cube.getUv();
                    @Nullable FaceUVsItem faceUv = cube.getFaceUv();
                    float[] size = cube.getSize();
                    float[] origin = cube.getOrigin();
                    @Nullable float[] cubeRotation = cube.getRotation();
                    @Nullable float[] cubePivot = cube.getPivot();
                    boolean mirror = cube.isMirror();
                    float inflate = cube.getInflate();
                    // 先将 origin 的 x 轴坐标处理一下，先加上 size.x 再镜像。
                    origin[0] = -(origin[0] + size[0]);
                    // 初步处理 cubeRotation 和 cubePivot，其中 cubePivot 是从左手系转换为右手系的绝对坐标
                    if (cubeRotation != null) {
                        cubeRotation[0] = (float) -Math.toRadians(cubeRotation[0]);
                        cubeRotation[1] = (float) -Math.toRadians(cubeRotation[1]);
                        cubeRotation[2] = (float)  Math.toRadians(cubeRotation[2]);
                    }
                    if (cubePivot != null) {
                        cubePivot[0] = -cubePivot[0];
                    }
                    // 根据情况建立好 BedrockCube 实例
                    BedrockCube cubeInstance;
                    float originX = cubePivot == null ? origin[0] - part.x : origin[0] - cubePivot[0];
                    float originY = cubePivot == null ? origin[1] - part.y : origin[1] - cubePivot[1];
                    float originZ = cubePivot == null ? origin[2] - part.z : origin[2] - cubePivot[2];
                    if (faceUv == null) {
                        cubeInstance = createCubeBox(
                                uv[0], uv[1],
                                originX, originY, originZ,
                                size[0], size[1], size[2],
                                inflate, mirror, texWidth, texHeight
                        );
                    } else {
                        cubeInstance = createCubePerFace(
                                originX, originY, originZ,
                                size[0], size[1], size[2],
                                inflate, texWidth, texHeight, faceUv
                        );
                    }
                    if (cubeRotation == null || cubePivot == null) {
                        // 普通 cube 直接放入 cubes
                        part.cubes.add(cubeInstance);
                    } else {
                        // 带有 pivot 和 rotation 的需要套一层 BedrockPart
                        BedrockBone cubeRenderer = new BedrockBone();
                        cubeRenderer.x = cubePivot[0];
                        cubeRenderer.y = cubePivot[1];
                        cubeRenderer.z = cubePivot[2];
                        cubeRenderer.xRot = cubeRotation[0];
                        cubeRenderer.yRot = cubeRotation[1];
                        cubeRenderer.zRot = cubeRotation[2];
                        cubeRenderer.cubes.add(cubeInstance);
                        // 添加进父骨骼中
                        cubeRenderer.parent = part;
                        part.addChild(cubeRenderer);
                    }
                }
            }
        }
        // 将所有相对 pivot 转换为绝对 pivot，使用 DFS 实现
        convertPivot(root);
    }

    @Override
    public Collection<Integer> getChildren(int i) {
        BedrockBone part = boneIndex.get(i);
        return Collections2.transform(part.getChildren(), val -> val.index);
    }

    @Override
    public int getFather(int i) {
        BedrockBone part = boneIndex.get(i);
        if (part.parent == null) {
            return -1;
        }
        return part.parent.index;
    }

    @Override
    public void applyPose(Pose pose) {
        for (BoneTransform boneTransform : pose.getBoneTransforms()) {
            BedrockBone part = boneIndex.get(boneTransform.boneIndex());
            Vector3fc translation = boneTransform.translation();
            Vector3fc rotation = boneTransform.rotation().asEulerAngle();
            Vector3fc scale = boneTransform.scale();
            part.x = translation.x();
            part.y = translation.y();
            part.z = translation.z();
            part.xRot = rotation.x();
            part.yRot = rotation.y();
            part.zRot = rotation.z();
            part.xScale = scale.x();
            part.yScale = scale.y();
            part.zScale = scale.z();
        }
    }

    @Override
    public Pose getBindPose() {
        return bindingPose;
    }
}
