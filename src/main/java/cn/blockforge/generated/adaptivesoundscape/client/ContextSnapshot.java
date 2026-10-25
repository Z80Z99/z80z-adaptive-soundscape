package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public record ContextSnapshot(
        MusicConfig.State state,
        int hostileCount,
        int engagedCount,
        MusicConfig.BossStage bossStage,
        ResourceLocation dimension,
        ResourceLocation biomeId,
        Holder<Biome> biome,
        boolean night
) {
}
