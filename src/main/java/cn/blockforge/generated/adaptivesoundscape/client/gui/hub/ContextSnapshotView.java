package cn.blockforge.generated.adaptivesoundscape.client.gui.hub;

import cn.blockforge.generated.adaptivesoundscape.client.ContextSnapshot;
import cn.blockforge.generated.adaptivesoundscape.client.MusicController;
import net.minecraft.client.resources.language.I18n;

/** 概览节使用的实时情境只读视图（未在世界内时显示占位）。 */
final class ContextSnapshotView {
    private ContextSnapshotView() {
    }

    static ContextSnapshotView capture() {
        return new ContextSnapshotView();
    }

    private ContextSnapshot snapshot() {
        return MusicController.INSTANCE.snapshot();
    }

    String state() {
        ContextSnapshot snapshot = snapshot();
        if (snapshot == null) return I18n.get("z80z_adaptive_soundscape.ui.ctx.not_in_world");
        return switch (snapshot.state()) {
            case EXPLORE -> I18n.get("z80z_adaptive_soundscape.ui.ctx.explore");
            case ALERT -> I18n.get("z80z_adaptive_soundscape.ui.ctx.alert");
            case COMBAT -> I18n.get("z80z_adaptive_soundscape.ui.ctx.combat");
            default -> "-";
        };
    }

    String enemies() {
        ContextSnapshot snapshot = snapshot();
        return snapshot == null ? "-" : String.valueOf(snapshot.hostileCount());
    }

    String boss() {
        ContextSnapshot snapshot = snapshot();
        if (snapshot == null) return "-";
        return switch (snapshot.bossStage()) {
            case NEAR -> I18n.get("z80z_adaptive_soundscape.ui.ctx.boss_near");
            case ENGAGED -> I18n.get("z80z_adaptive_soundscape.ui.ctx.boss_engaged");
            case PHASE_TWO -> I18n.get("z80z_adaptive_soundscape.ui.ctx.boss_phase_two");
            case NONE -> I18n.get("z80z_adaptive_soundscape.ui.ctx.boss_none");
            default -> "-";
        };
    }
}
