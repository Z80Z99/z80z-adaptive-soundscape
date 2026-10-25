package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ContextDetector {
    private int holdTicks;

    public ContextSnapshot detect(Minecraft minecraft, MusicConfig config) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return null;
        double scanRadius = Math.max(config.hostileRadius, config.bossRadius);
        AABB area = player.getBoundingBox().inflate(scanRadius);
        List<LivingEntity> nearby = minecraft.level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class), area,
                entity -> entity != player && entity.isAlive()
        );
        int hostileCount = 0;
        int engagedCount = 0;
        LivingEntity closestBoss = null;
        double closestBossDistance = Double.MAX_VALUE;
        Set<String> bossIds = new HashSet<>(config.bossEntities);

        for (LivingEntity entity : nearby) {
            double distance = player.distanceToSqr(entity);
            boolean hostile = entity instanceof Enemy;
            boolean targetingPlayer = entity instanceof Mob mob && mob.getTarget() == player;
            boolean playerTargeting = entity.getLastHurtByMob() == player;
            boolean recentlyHurtBy = player.getLastHurtByMob() == entity;
            if ((hostile || targetingPlayer || playerTargeting || recentlyHurtBy)
                    && distance <= config.hostileRadius * config.hostileRadius) {
                hostileCount++;
                if (targetingPlayer || playerTargeting || recentlyHurtBy) engagedCount++;
            }
            String entityId = EntityType.getKey(entity.getType()).toString();
            boolean boss = bossIds.contains(entityId)
                    || (entity instanceof Enemy && entity.getMaxHealth() >= config.bossHealthThreshold);
            if (boss && distance <= config.bossRadius * config.bossRadius && distance < closestBossDistance) {
                closestBoss = entity;
                closestBossDistance = distance;
            }
        }

        MusicConfig.BossStage bossStage = MusicConfig.BossStage.NONE;
        if (closestBoss != null) {
            boolean engaged = (closestBoss instanceof Mob mob && mob.getTarget() == player)
                    || closestBoss.getLastHurtByMob() == player
                    || player.getLastHurtByMob() == closestBoss;
            if (engaged) {
                bossStage = closestBoss.getHealth() <= closestBoss.getMaxHealth() * 0.5F
                        ? MusicConfig.BossStage.PHASE_TWO : MusicConfig.BossStage.ENGAGED;
                engagedCount = Math.max(1, engagedCount);
            } else {
                bossStage = MusicConfig.BossStage.NEAR;
            }
        }

        MusicConfig.State immediate;
        if (engagedCount > 0) {
            immediate = MusicConfig.State.COMBAT;
            holdTicks = config.combatHoldTicks;
        } else if (hostileCount > 0) {
            immediate = MusicConfig.State.ALERT;
            holdTicks = Math.max(holdTicks, config.combatHoldTicks / 2);
        } else if (holdTicks > 0) {
            holdTicks -= config.scanIntervalTicks;
            immediate = MusicConfig.State.ALERT;
        } else {
            immediate = MusicConfig.State.EXPLORE;
        }

        Holder<Biome> biome = minecraft.level.getBiomeManager().getBiome(player.blockPosition());
        ResourceLocation biomeId = biome.unwrapKey()
                .map(key -> key.location())
                .orElse(new ResourceLocation("minecraft", "unknown"));
        return new ContextSnapshot(
                immediate,
                hostileCount,
                engagedCount,
                bossStage,
                minecraft.level.dimension().location(),
                biomeId,
                biome,
                minecraft.level.isNight()
        );
    }
}
