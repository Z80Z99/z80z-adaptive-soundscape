package cn.blockforge.generated.adaptivesoundscape.client.gui.hub;

import cn.blockforge.generated.adaptivesoundscape.client.CustomMusicPack;
import cn.blockforge.generated.adaptivesoundscape.client.MusicController;
import cn.blockforge.generated.adaptivesoundscape.client.gui.Ui;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.List;

final class OverviewSection extends Section {
    private int rulesCardX;
    private int rulesCardY;
    private int cardW;
    private int cardH = 30;
    private int libraryCardX;
    private int libraryCardY;
    private boolean rulesCardHover;
    private boolean libraryCardHover;
    private boolean cardsFit = true;

    @Override
    void build() {
        MusicConfig config = ConfigManager.get();
        int w = hub.contentW();
        // 游标式布局：每行按 flow 实际高度（感知换行）推进，杜绝换行后与后续元素重叠
        int y = hub.contentY() + 64;

        Button mod = Ui.toggle(I18n.get("z80z_adaptive_soundscape.ui.toggle.mod"), () -> config.enabled, () -> {
            config.enabled = !config.enabled;
            ConfigManager.save();
            MusicController.INSTANCE.forceRefresh();
        });
        mod.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.toggle.mod.tip")));
        Button vanilla = Ui.toggle(I18n.get("z80z_adaptive_soundscape.ui.toggle.vanilla"), () -> config.muteVanillaMusic, () -> {
            config.muteVanillaMusic = !config.muteVanillaMusic;
            ConfigManager.save();
            MusicController.INSTANCE.forceRefresh();
        });
        vanilla.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.toggle.vanilla.tip")));
        Button glow = Ui.toggle(I18n.get("z80z_adaptive_soundscape.ui.toggle.glow"), () -> config.visualGlow, () -> {
            config.visualGlow = !config.visualGlow;
            ConfigManager.save();
        });
        glow.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.toggle.glow.tip")));
        Button notes = Ui.toggle(I18n.get("z80z_adaptive_soundscape.ui.toggle.notes"), () -> config.noteParticles, () -> {
            config.noteParticles = !config.noteParticles;
            ConfigManager.save();
        });
        notes.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.toggle.notes.tip")));
        int[] toggleWidths = {74, 92, 74, 74};
        List<int[]> toggleFlow = Ui.flow(w, 4, 22, toggleWidths);
        place(y, toggleFlow, mod, vanilla, glow, notes);
        y += Ui.flowHeight(w, 4, 22, toggleWidths) + 8;

        Button combat = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.preset.combat"), b -> preset("combat"))
                .bounds(0, 0, 10, 20).build();
        combat.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.preset.combat.tip")));
        Button balanced = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.preset.balanced"), b -> preset("balanced"))
                .bounds(0, 0, 10, 20).build();
        balanced.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.preset.balanced.tip")));
        Button relaxed = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.preset.relaxed"), b -> preset("relaxed"))
                .bounds(0, 0, 10, 20).build();
        relaxed.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.preset.relaxed.tip")));
        Button scan = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.rescan"), b -> scan())
                .bounds(0, 0, 10, 20).build();
        scan.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.rescan.tip")));
        int[] presetWidths = {50, 50, 50, 84};
        List<int[]> presetFlow = Ui.flow(w, 4, 22, presetWidths);
        place(y, presetFlow, combat, balanced, relaxed, scan);
        y += Ui.flowHeight(w, 4, 22, presetWidths) + 12;

        cardW = (w - 10) / 2;
        rulesCardX = hub.contentX();
        libraryCardX = rulesCardX + cardW + 10;
        rulesCardY = libraryCardY = y;
        cardsFit = rulesCardY + cardH <= hub.contentY() + hub.contentH() - 4;
    }

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = hub.contentX();
        int y = hub.contentY();
        int w = hub.contentW();
        MusicController controller = MusicController.INSTANCE;

        Ui.card(graphics, x, y, w, 36);
        String playing = controller.currentRuleName();
        if (playing.isBlank()) {
            graphics.drawCenteredString(hub.font(),
                    I18n.get("z80z_adaptive_soundscape.ui.overview.not_playing"), x + w / 2, y + 6, Ui.MUTED_COLOR);
        } else {
            graphics.drawString(hub.font(), "♫ " + playing, x + 8, y + 5, Ui.TITLE_COLOR, false);
            graphics.drawString(hub.font(), Ui.trunc(hub.font(), controller.currentTrack(), w - 20),
                    x + 8, y + 19, Ui.MUTED_COLOR, false);
        }

        ContextSnapshotView snapshot = ContextSnapshotView.capture();
        int badgeY = y + 44;
        int bx = x;
        bx += badge(graphics, "z80z_adaptive_soundscape.ui.ctx.state", snapshot.state(), bx, badgeY) + 4;
        bx += badge(graphics, "z80z_adaptive_soundscape.ui.ctx.enemies", snapshot.enemies(), bx, badgeY) + 4;
        badge(graphics, "z80z_adaptive_soundscape.ui.ctx.boss", snapshot.boss(), bx, badgeY);

        if (cardsFit) {
            renderStatCard(graphics, mouseX, mouseY, rulesCardX, rulesCardY, true);
            renderStatCard(graphics, mouseX, mouseY, libraryCardX, libraryCardY, false);
        } else {
            rulesCardHover = false;
            libraryCardHover = false;
        }
    }

    private int badge(GuiGraphics graphics, String labelKey, String value, int x, int y) {
        return Ui.badge(graphics, hub.font(), I18n.get(labelKey) + " " + value, x, y, 0xFF4A3E86);
    }

    private void renderStatCard(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean rules) {
        String title = I18n.get(rules ? "z80z_adaptive_soundscape.ui.card.rules" : "z80z_adaptive_soundscape.ui.card.library");
        String value = rules ? rulesSummary() : librarySummary();
        boolean hover = mouseX >= x && mouseX < x + cardW && mouseY >= y && mouseY < y + cardH;
        if (rules) rulesCardHover = hover;
        else libraryCardHover = hover;
        Ui.card(graphics, x, y, cardW, cardH);
        if (hover) graphics.fill(x, y, x + cardW, y + cardH, 0x30FFFFFF);
        graphics.drawString(hub.font(), title, x + 8, y + 4, Ui.MUTED_COLOR, false);
        graphics.drawString(hub.font(), value, x + 8, y + 16, Ui.TEXT_COLOR, false);
    }

    private String rulesSummary() {
        List<MusicConfig.Rule> rules = ConfigManager.get().rules;
        long enabled = rules.stream().filter(rule -> rule.enabled).count();
        return I18n.get("z80z_adaptive_soundscape.ui.card.rules.summary", rules.size(), enabled);
    }

    private String librarySummary() {
        return I18n.get("z80z_adaptive_soundscape.ui.card.library.summary",
                CustomMusicPack.libraryEntries().size());
    }

    @Override
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && rulesCardHover) {
            hub.switchTo(HubScreen.SectionId.RULES);
            return true;
        }
        if (button == 0 && libraryCardHover) {
            if (hub.isPicking()) hub.cancelPicker();
            hub.switchTo(HubScreen.SectionId.LIBRARY);
            return true;
        }
        return false;
    }

    private void place(int baseY, List<int[]> positions, Button... buttons) {
        for (int i = 0; i < buttons.length; i++) {
            int[] position = positions.get(i);
            buttons[i].setX(hub.contentX() + position[0]);
            buttons[i].setY(baseY + position[1]);
            buttons[i].setWidth(position[2]);
            hub.addWidget(buttons[i]);
        }
    }

    private void preset(String preset) {
        ConfigManager.applyPreset(preset);
        MusicController.INSTANCE.forceRefresh();
        hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.preset.loaded",
                I18n.get("z80z_adaptive_soundscape.ui.preset." + preset)), false);
    }

    private void scan() {
        CustomMusicPack.scan();
        String message = CustomMusicPack.lastScanMessage();
        if (CustomMusicPack.consumeOggReloadRequired()) {
            message += I18n.get("message.z80z_adaptive_soundscape.scan.reloading");
            hub.mc().reloadResourcePacks().thenRun(MusicController.INSTANCE::forceRefresh);
        } else {
            MusicController.INSTANCE.forceRefresh();
        }
        hub.setStatus(message, false);
    }
}
