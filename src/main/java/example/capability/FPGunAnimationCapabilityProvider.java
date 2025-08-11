package example.capability;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FPGunAnimationCapabilityProvider implements ICapabilityProvider {
    private IFPGunAnimationCapability capability;
    private final Player player;

    public FPGunAnimationCapabilityProvider(Player player) {
        this.player = player;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ModCapability.FPGUN_ANIMATION_CAPABILITY ? LazyOptional.of(this::getOrCreateCapability).cast() : LazyOptional.empty();
    }

    @NotNull
    IFPGunAnimationCapability getOrCreateCapability() {
        if (capability == null) {
            this.capability = new FPGunAnimationCapability(player);
        }
        return this.capability;
    }
}
