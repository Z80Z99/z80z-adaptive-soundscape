package cn.blockforge.generated.adaptivesoundscape.config;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public final class MusicConfig {
    public int schemaVersion = 1;
    public boolean enabled = true;
    public boolean muteVanillaMusic = true;
    public boolean visualGlow = true;
    public boolean noteParticles = true;
    public int scanIntervalTicks = 10;
    public int combatHoldTicks = 100;
    public int fadeTicks = 40;
    public float previewVolume = 1.0F;
    public double hostileRadius = 16.0;
    public double bossRadius = 30.0;
    public double bossHealthThreshold = 80.0;
    public List<String> bossEntities = new ArrayList<>();
    public List<Rule> rules = new ArrayList<>();

    public enum State {
        ANY,
        EXPLORE,
        ALERT,
        COMBAT
    }

    public enum Time {
        ANY,
        DAY,
        NIGHT
    }

    public enum BossStage {
        ANY,
        NONE,
        NEAR,
        ENGAGED,
        PHASE_TWO
    }

    public static final class Rule {
        public String id = "new_rule";
        public String name = "新规则";
        public boolean enabled = true;
        public int priority = 0;
        public State state = State.ANY;
        public int minEnemies = 0;
        public int maxEnemies = -1;
        public BossStage bossStage = BossStage.ANY;
        public Time time = Time.ANY;
        public List<String> dimensions = new ArrayList<>();
        public List<String> biomeIds = new ArrayList<>();
        public List<String> biomeTags = new ArrayList<>();
        public List<Track> tracks = new ArrayList<>();

        public Rule copy() {
            Rule copy = new Rule();
            copy.id = id + "_copy";
            copy.name = name + "（副本）";
            copy.enabled = enabled;
            copy.priority = priority;
            copy.state = state;
            copy.minEnemies = minEnemies;
            copy.maxEnemies = maxEnemies;
            copy.bossStage = bossStage;
            copy.time = time;
            copy.dimensions = new ArrayList<>(dimensions);
            copy.biomeIds = new ArrayList<>(biomeIds);
            copy.biomeTags = new ArrayList<>(biomeTags);
            for (Track track : tracks) copy.tracks.add(track.copy());
            return copy;
        }
    }

    public static final class Track {
        /** 音量小于 0 表示跟随音乐库总音量；≥0 为本规则内独立覆盖（0.0~2.0）。 */
        public static final float FOLLOW_LIBRARY = -1.0F;

        public String sound = "minecraft:music.game";
        public int weight = 1;
        public float volume = FOLLOW_LIBRARY;
        public boolean loop = true;

        public Track() {
        }

        public Track(String sound, int weight, float volume, boolean loop) {
            this.sound = sound;
            this.weight = weight;
            this.volume = volume;
            this.loop = loop;
        }

        public Track copy() {
            return new Track(sound, weight, volume, loop);
        }
    }
}
