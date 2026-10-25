package cn.blockforge.generated.adaptivesoundscape;

import cn.blockforge.generated.adaptivesoundscape.client.ClientBootstrap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "z80z_adaptive_soundscape";

    public GeneratedMod() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientBootstrap::initialize);
    }
}
