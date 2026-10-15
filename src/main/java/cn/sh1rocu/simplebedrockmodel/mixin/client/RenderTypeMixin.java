package cn.sh1rocu.simplebedrockmodel.mixin.client;

import cn.sh1rocu.simplebedrockmodel.api.mixin.RenderTypeExtension;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderType.class)
public class RenderTypeMixin implements RenderTypeExtension {
    @Shadow
    @Final
    private static ImmutableList<RenderType> CHUNK_BUFFER_LAYERS;
    @Unique
    public int sbm$chunkLayerId = -1;

    @Override
    public int sbm$getChunkLayerId() {
        return sbm$chunkLayerId;
    }

    @Override
    public void sbm$setChunkLayerId(int chunkLayerId) {
        sbm$chunkLayerId = chunkLayerId;
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void sbm$clinit(CallbackInfo ci) {
        int i = 0;
        for (var layer : CHUNK_BUFFER_LAYERS)
            ((RenderTypeExtension) layer).sbm$setChunkLayerId(i++);
    }
}
