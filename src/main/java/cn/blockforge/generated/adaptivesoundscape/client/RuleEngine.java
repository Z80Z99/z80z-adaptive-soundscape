package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.List;
import java.util.Random;

public final class RuleEngine {
    private final Random random = new Random();

    public MusicConfig.Rule selectRule(MusicConfig config, ContextSnapshot context) {
        MusicConfig.Rule selected = null;
        for (MusicConfig.Rule rule : config.rules) {
            if (!matches(rule, context)) continue;
            if (selected == null || rule.priority > selected.priority) selected = rule;
        }
        return selected;
    }

    public MusicConfig.Track selectTrack(MusicConfig.Rule rule, String previousSound) {
        if (rule == null || rule.tracks.isEmpty()) return null;
        MusicConfig.Track picked = roll(rule);
        if (previousSound != null && picked.sound.equals(previousSound) && rule.tracks.size() > 1) {
            MusicConfig.Track rerolled = roll(rule);
            if (!rerolled.sound.equals(previousSound)) return rerolled;
        }
        return picked;
    }

    private MusicConfig.Track roll(MusicConfig.Rule rule) {
        int total = 0;
        for (MusicConfig.Track track : rule.tracks) total += Math.max(1, track.weight);
        int roll = random.nextInt(Math.max(1, total));
        for (MusicConfig.Track track : rule.tracks) {
            roll -= Math.max(1, track.weight);
            if (roll < 0) return track;
        }
        return rule.tracks.get(0);
    }

    private boolean matches(MusicConfig.Rule rule, ContextSnapshot context) {
        if (!rule.enabled || rule.tracks.isEmpty()) return false;
        if (rule.state != MusicConfig.State.ANY && rule.state != context.state()) return false;
        if (context.hostileCount() < rule.minEnemies) return false;
        if (rule.maxEnemies >= 0 && context.hostileCount() > rule.maxEnemies) return false;
        if (rule.bossStage != MusicConfig.BossStage.ANY && rule.bossStage != context.bossStage()) return false;
        if (rule.time == MusicConfig.Time.DAY && context.night()) return false;
        if (rule.time == MusicConfig.Time.NIGHT && !context.night()) return false;
        if (!matchesAny(rule.dimensions, context.dimension())) return false;
        if (!rule.biomeIds.isEmpty() || !rule.biomeTags.isEmpty()) {
            boolean biomeMatch = matchesAny(rule.biomeIds, context.biomeId());
            for (String value : rule.biomeTags) {
                ResourceLocation id = ResourceLocation.tryParse(value);
                if (id != null && context.biome().is(TagKey.create(Registries.BIOME, id))) {
                    biomeMatch = true;
                    break;
                }
            }
            if (!biomeMatch) return false;
        }
        return true;
    }

    private boolean matchesAny(List<String> values, ResourceLocation actual) {
        if (values.isEmpty()) return true;
        for (String value : values) {
            ResourceLocation expected = ResourceLocation.tryParse(value);
            if (actual.equals(expected)) return true;
        }
        return false;
    }
}
