package example.capability;

import example.animation.FPGunAnimationInstance;
import net.minecraft.world.entity.player.Player;

public class FPGunAnimationCapability implements IFPGunAnimationCapability{
    private final FPGunAnimationInstance animationInstance;
    private int lastSelected = -1;

    public FPGunAnimationCapability(Player player) {
        this.animationInstance = new FPGunAnimationInstance(player);
    }

    @Override
    public FPGunAnimationInstance getAnimationInstance() {
        return animationInstance;
    }

    public int getLastSelected() {
        return lastSelected;
    }

    public void setLastSelected(int lastSelected) {
        this.lastSelected = lastSelected;
    }
}
