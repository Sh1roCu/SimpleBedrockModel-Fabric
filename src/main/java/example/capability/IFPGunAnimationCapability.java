package example.capability;

import example.animation.FPGunAnimationInstance;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public interface IFPGunAnimationCapability {
    FPGunAnimationInstance getAnimationInstance();
}
