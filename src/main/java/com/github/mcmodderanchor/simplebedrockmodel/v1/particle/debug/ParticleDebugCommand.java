package com.github.mcmodderanchor.simplebedrockmodel.v1.particle.debug;

import com.github.mcmodderanchor.simplebedrockmodel.SimpleBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.resource.ParticleDefinitionLoader;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.WorldEmitterManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod.EventBusSubscriber(modid = SimpleBedrockModel.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ParticleDebugCommand {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                literal("sbm")
                    .then(literal("particle")
                    .then(literal("spawn")
                    .then(argument("effect", ResourceLocationArgument.id())
                        .suggests(ParticleDebugCommand::suggestParticleEffects)
                    .then(argument("pos", Vec3Argument.vec3(false))
                        .executes(ParticleDebugCommand::spawnEmitter)
                    .then(argument("velocity", Vec3Argument.vec3(false))
                        .executes(ParticleDebugCommand::spawnEmitter))))))
        );
    }

    private static CompletableFuture<Suggestions> suggestParticleEffects(CommandContext<?> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                ParticleDefinitionLoader.getInstance().getAllDefinitions().keySet(),
                builder
        );
    }

    private static int spawnEmitter(CommandContext<CommandSourceStack> context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            sendMessage(Component.literal("[SBM] 当前没有可用的客户端世界"), true);
            return 0;
        }

        ResourceLocation effectId = ResourceLocationArgument.getId(context, "effect");
        if (effectId == null) {
            sendMessage(Component.literal("[SBM] 无效的粒子效果 ID: " + effectId), true);
            return 0;
        }

        ParticleEffectDefinition definition = ParticleDefinitionLoader.getInstance().getDefinition(effectId);
        if (definition == null) {
            sendMessage(Component.literal("[SBM] 找不到粒子效果定义: " + effectId), true);
            return 0;
        }

        Vec3 pos = Vec3Argument.getVec3(context, "pos");
        Vec3 velocity = context.getNodes().stream().anyMatch(node -> "velocity".equals(node.getNode().getName()))
                ? Vec3Argument.getVec3(context, "velocity")
                : Vec3.ZERO;

        WorldEmitterManager.getInstance().addEmitter(mc.level, pos, velocity, definition);
        sendMessage(Component.literal("[SBM] 已生成粒子发射器: " + effectId + " @ "
                + formatVec3(pos) + " vel=" + formatVec3(velocity)), false);
        return 1;
    }

    private static String formatVec3(Vec3 vec) {
        return String.format(Locale.ROOT, "%.3f, %.3f, %.3f", vec.x, vec.y, vec.z);
    }

    private static void sendMessage(Component message, boolean overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(message, overlay);
        }
    }
}
