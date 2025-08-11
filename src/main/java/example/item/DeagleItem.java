package example.item;

import example.animation.DeagleAnimationGraph;
import example.animation.FPGunAnimationInstance;
import example.animation.GunAnimationGraph;
import example.capability.ModCapability;
import example.client.render.item.DeagleWithoutLevelRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class DeagleItem extends Item implements GunItem {
    public DeagleItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        entity.getCapability(ModCapability.FPGUN_ANIMATION_CAPABILITY).ifPresent(capability -> {
            capability.getAnimationInstance().trigger();
        });
        return true;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        return true;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new DeagleWithoutLevelRenderer();
            }
        });
    }

    @Override
    public GunAnimationGraph getAnimationGraph(FPGunAnimationInstance animationInstance) {
        return new DeagleAnimationGraph(animationInstance);
    }

    @Override
    public boolean hasMagInstalled(ItemStack itemStack) {
        CompoundTag nbt = itemStack.getOrCreateTag();
        if (nbt.contains("HasMagInstalled")) {
            return nbt.getBoolean("HasMagInstalled");
        }
        return false;
    }

    @Override
    public int getAmmoInMag(ItemStack itemStack) {
        CompoundTag nbt = itemStack.getOrCreateTag();
        if (nbt.contains("AmmoInMag")) {
            return nbt.getInt("AmmoInMag");
        }
        return 0;
    }

    @Override
    public int getAmmoInGun(ItemStack itemStack) {
        CompoundTag nbt = itemStack.getOrCreateTag();
        if (nbt.contains("AmmoInGun")) {
            return nbt.getInt("AmmoInGun");
        }
        return 0;
    }

    @Override
    public void setMagInstalled(ItemStack itemStack, boolean installed) {
        CompoundTag nbt = itemStack.getOrCreateTag();
        nbt.putBoolean("HasMagInstalled", installed);
    }

    @Override
    public void setAmmoInMag(ItemStack itemStack, int ammo) {
        CompoundTag nbt = itemStack.getOrCreateTag();
        nbt.putInt("AmmoInMag", ammo);
    }

    @Override
    public void setAmmoInGun(ItemStack itemStack, int ammo) {
        CompoundTag nbt = itemStack.getOrCreateTag();
        nbt.putInt("AmmoInGun", ammo);
    }
}
