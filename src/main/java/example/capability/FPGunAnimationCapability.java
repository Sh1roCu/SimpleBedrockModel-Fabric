package example.capability;

import example.animation.FPGunAnimationInstance;
import net.minecraft.world.entity.player.Player;

public class FPGunAnimationCapability implements IFPGunAnimationCapability{
    private final FPGunAnimationInstance animationInstance;

    public FPGunAnimationCapability(Player player) {
        this.animationInstance = new FPGunAnimationInstance(player);
    }

    @Override
    public FPGunAnimationInstance getAnimationInstance() {
        return animationInstance;
    }
}
