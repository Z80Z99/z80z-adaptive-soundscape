package cn.blockforge.generated.adaptivesoundscape.client.gui.hub;

import cn.blockforge.generated.adaptivesoundscape.client.CustomMusicPack;
import cn.blockforge.generated.adaptivesoundscape.client.MusicController;
import cn.blockforge.generated.adaptivesoundscape.client.PreviewPlayer;
import cn.blockforge.generated.adaptivesoundscape.client.gui.Ui;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

final class RuleEditorSection extends Section {
    private enum Tab {BASIC, CONDITION, TRACKS}

    private static final int TRACK_ROW_HEIGHT = 16;

    private final List<Label> labels = new ArrayList<>();
    private Tab tab = Tab.BASIC;
    private MusicConfig.Rule initializedRule;
    private String idText;
    private String nameText;
    private String priorityText;
    private String minText;
    private String maxText;
    private String dimensionText;
    private String biomeText;
    private String tagText;
    private String soundDraft;
    private String weightDraft;
    private String volumeDraft;
    private int selectedTrack;
    private int trackScroll;
    private EditBox soundBox;
    private EditBox weightBox;
    private EditBox volumeBox;
    private int trackListTop;
    private int trackListBottom;
    private int visibleTrackRows = 3;
    private int hintY;
    private boolean hintVisible = true;

    @Override
    void build() {
        MusicConfig.Rule rule = hub.editingRule();
        if (rule == null) {
            hub.switchTo(HubScreen.SectionId.RULES);
            return;
        }
        if (initializedRule != rule) {
            loadDrafts(rule);
            initializedRule = rule;
        }
        labels.clear();
        int y = hub.contentY();
        int w = hub.contentW();

        Button basic = tabButton("z80z_adaptive_soundscape.ui.tab.basic", Tab.BASIC);
        Button condition = tabButton("z80z_adaptive_soundscape.ui.tab.condition", Tab.CONDITION);
        Button tracks = tabButton("z80z_adaptive_soundscape.ui.tab.tracks", Tab.TRACKS);
        List<int[]> tabs = Ui.flow(w, 4, 20, 64, 64, 64);
        place(y, tabs, basic, condition, tracks);

        int bottom = hub.contentY() + hub.contentH();
        int saveRow = bottom - 24;
        Button save = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.editor.save"), b -> save())
                .bounds(0, 0, 10, 20).build();
        save.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.editor.save.tip")));
        Button cancel = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.editor.cancel"), b -> hub.exitEditor())
                .bounds(0, 0, 10, 20).build();
        cancel.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.editor.cancel.tip")));
        List<int[]> bottomFlow = Ui.flow(w, 4, 20, 60, 60);
        place(saveRow, bottomFlow, save, cancel);
        hintY = saveRow - 13;

        switch (tab) {
            case BASIC -> buildBasic();
            case CONDITION -> buildCondition();
            case TRACKS -> buildTracks();
        }
    }

    private void loadDrafts(MusicConfig.Rule rule) {
        idText = rule.id;
        nameText = rule.name;
        priorityText = String.valueOf(rule.priority);
        minText = String.valueOf(rule.minEnemies);
        maxText = String.valueOf(rule.maxEnemies);
        dimensionText = String.join(", ", rule.dimensions);
        biomeText = String.join(", ", rule.biomeIds);
        tagText = String.join(", ", rule.biomeTags);
        MusicConfig.Track track = currentTrack();
        soundDraft = track.sound;
        weightDraft = String.valueOf(track.weight);
        volumeDraft = track.volume < 0.0F ? "" : String.valueOf(track.volume);
        selectedTrack = 0;
        trackScroll = 0;
        tab = Tab.BASIC;
    }

    private Button tabButton(String labelKey, Tab target) {
        boolean active = tab == target;
        Component message = Component.translatable(labelKey)
                .withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY);
        Button button = Button.builder(message, click -> {
            if (tab == target) return;
            commitTrackEditor();
            tab = target;
            hub.rebuild();
        }).bounds(0, 0, 10, 20).build();
        if (active) button.active = false;
        return button;
    }

    // ===== 基础页 =====

    private void buildBasic() {
        int w = hub.contentW();
        // 页签条占 [contentY, +20]，字段从 +32 起（标签在 +22，与页签完全脱离）
        int y = hub.contentY() + 32;
        int halfWidth = (w - 10) / 2;
        EditBox id = field(idText, value -> idText = value, "z80z_adaptive_soundscape.ui.f.id",
                "z80z_adaptive_soundscape.ui.f.id.tip");
        EditBox name = field(nameText, value -> nameText = value, "z80z_adaptive_soundscape.ui.f.name",
                "z80z_adaptive_soundscape.ui.f.name.tip");
        List<int[]> row1 = Ui.flow(w, 4, 22, halfWidth, halfWidth);
        label("z80z_adaptive_soundscape.ui.f.id", row1.get(0)[0], y - 10);
        label("z80z_adaptive_soundscape.ui.f.name", row1.get(1)[0], y - 10);
        place(y, row1, id, name);
        y += Ui.flowHeight(w, 4, 22, halfWidth, halfWidth) + 10;

        List<int[]> row2 = Ui.flow(w, 4, 22, 64, 120, 100, 80);
        EditBox priority = field(priorityText, value -> priorityText = value, "z80z_adaptive_soundscape.ui.f.priority",
                "z80z_adaptive_soundscape.ui.f.priority.tip");
        priority.setFilter(Ui.numberFilter());
        MusicConfig.Rule rule = hub.editingRule();
        Button state = cycleButton("z80z_adaptive_soundscape.ui.f.state", stateKey(rule.state), () -> {
            rule.state = cycle(rule.state, MusicConfig.State.values());
            return I18n.get(stateKey(rule.state));
        }, "z80z_adaptive_soundscape.ui.f.state.tip");
        Button time = cycleButton("z80z_adaptive_soundscape.ui.f.time", timeKey(rule.time), () -> {
            rule.time = cycle(rule.time, MusicConfig.Time.values());
            return I18n.get(timeKey(rule.time));
        }, "z80z_adaptive_soundscape.ui.f.time.tip");
        Button enabled = Ui.toggle(I18n.get("z80z_adaptive_soundscape.ui.f.enabled"), () -> rule.enabled,
                () -> rule.enabled = !rule.enabled);
        label("z80z_adaptive_soundscape.ui.f.priority", row2.get(0)[0], y - 10);
        place(y, row2, priority, state, time, enabled);
        clampHint(y + Ui.flowHeight(w, 4, 22, 64, 120, 100, 80));
    }

    // ===== 条件页 =====

    private void buildCondition() {
        int w = hub.contentW();
        int y = hub.contentY() + 32;
        List<int[]> row1 = Ui.flow(w, 4, 22, 64, 74, 140);
        EditBox min = field(minText, value -> minText = value, "z80z_adaptive_soundscape.ui.f.min_enemies",
                "z80z_adaptive_soundscape.ui.f.min_enemies.tip");
        min.setFilter(Ui.numberFilter());
        EditBox max = field(maxText, value -> maxText = value, "z80z_adaptive_soundscape.ui.f.max_enemies",
                "z80z_adaptive_soundscape.ui.f.max_enemies.tip");
        max.setFilter(Ui.numberFilter());
        MusicConfig.Rule rule = hub.editingRule();
        Button boss = cycleButton("z80z_adaptive_soundscape.ui.f.boss", bossKey(rule.bossStage), () -> {
            rule.bossStage = cycle(rule.bossStage, MusicConfig.BossStage.values());
            return I18n.get(bossKey(rule.bossStage));
        }, "z80z_adaptive_soundscape.ui.f.boss.tip");
        label("z80z_adaptive_soundscape.ui.f.min_enemies", row1.get(0)[0], y - 10);
        label("z80z_adaptive_soundscape.ui.f.max_enemies", row1.get(1)[0], y - 10);
        place(y, row1, min, max, boss);
        y += Ui.flowHeight(w, 4, 22, 64, 74, 140) + 10;

        int pitch = 30;
        EditBox dimensions = field(dimensionText, value -> dimensionText = value, "z80z_adaptive_soundscape.ui.f.dims",
                "z80z_adaptive_soundscape.ui.f.dims.tip");
        label("z80z_adaptive_soundscape.ui.f.dims", 0, y - 10);
        place(y, single(w), dimensions);
        y += pitch;

        EditBox biomeIds = field(biomeText, value -> biomeText = value, "z80z_adaptive_soundscape.ui.f.biome_ids",
                "z80z_adaptive_soundscape.ui.f.biome_ids.tip");
        label("z80z_adaptive_soundscape.ui.f.biome_ids", 0, y - 10);
        place(y, single(w), biomeIds);
        y += pitch;

        EditBox biomeTags = field(tagText, value -> tagText = value, "z80z_adaptive_soundscape.ui.f.biome_tags",
                "z80z_adaptive_soundscape.ui.f.biome_tags.tip");
        label("z80z_adaptive_soundscape.ui.f.biome_tags", 0, y - 10);
        place(y, single(w), biomeTags);
        clampHint(y + 20);
    }

    // ===== 曲目页（自底向上逐级降级，永不越界） =====

    private void buildTracks() {
        int w = hub.contentW();
        int contentBottom = hub.contentY() + hub.contentH();
        int contentTop = hub.contentY() + 32;
        int[] opsWidths = {118, 54, 54, 54, 26, 26};
        int opsH = Ui.flowHeight(w, 4, 22, opsWidths);

        // 三档间距逐级收紧；每档先尝试带提示行再尝试去掉；列表最小高度 28（末档 24）
        int[] gapTier = {8, 6, 4};
        int soundBlock = 30;   // 标签 10 + 输入框 20
        int propsBlock = 30;
        int saveBlock = 24;
        int hintBlock = 17;
        boolean placed = false;
        hintVisible = false;
        for (int tier = 0; tier < 3 && !placed; tier++) {
            int minList = tier < 2 ? 28 : 24;
            for (boolean withHint : new boolean[]{true, false}) {
                int total = 32 + minList + gapTier[tier] + soundBlock + gapTier[tier]
                        + propsBlock + gapTier[tier] + opsH + (withHint ? hintBlock : 4) + saveBlock;
                if (total > hub.contentH()) continue;
                hintVisible = withHint;
                int opsTop = contentBottom - saveBlock - (withHint ? hintBlock : 4) - opsH;
                int propsY = opsTop - gapTier[tier] - propsBlock;
                int soundY = propsY - gapTier[tier] - soundBlock;
                int listBottom = soundY - gapTier[tier];
                placeTracksContent(contentTop, listBottom, minList, soundY, propsY, opsTop, opsH, opsWidths);
                placed = true;
                break;
            }
        }
        if (!placed) {
            // 极端兜底：最小列表 + 最紧间距，无提示
            int opsTop = contentBottom - saveBlock - 4 - opsH;
            int propsY = opsTop - 2 - propsBlock;
            int soundY = propsY - 2 - soundBlock;
            hintVisible = false;
            placeTracksContent(contentTop, soundY - 2, 24, soundY, propsY, opsTop, opsH, opsWidths);
        }
    }

    private void placeTracksContent(int contentTop, int listBottom, int minList, int soundY, int propsY,
                                    int opsTop, int opsH, int[] opsWidths) {
        int w = hub.contentW();
        int listH = Math.max(minList, Math.min(72, listBottom - contentTop));
        trackListTop = contentTop;
        trackListBottom = contentTop + listH;
        visibleTrackRows = Math.max(1, (listH - 2) / TRACK_ROW_HEIGHT);

        soundBox = field(soundDraft, value -> soundDraft = value, "z80z_adaptive_soundscape.ui.f.sound",
                "z80z_adaptive_soundscape.ui.f.sound.tip");
        label("z80z_adaptive_soundscape.ui.f.sound", 0, soundY - 10);
        place(soundY, single(w), soundBox);

        List<int[]> row = Ui.flow(w, 4, 22, 50, 50, 78, 54, 40);
        weightBox = field(weightDraft, value -> weightDraft = value, "z80z_adaptive_soundscape.ui.f.weight",
                "z80z_adaptive_soundscape.ui.f.weight.tip");
        weightBox.setFilter(Ui.numberFilter());
        volumeBox = field(volumeDraft, value -> volumeDraft = value, "z80z_adaptive_soundscape.ui.f.volume",
                "z80z_adaptive_soundscape.ui.f.volume.tip");
        volumeBox.setFilter(Ui.numberFilter());
        volumeBox.setHint(Component.translatable("z80z_adaptive_soundscape.ui.f.volume.hint"));
        Button loop = Ui.toggle(I18n.get("z80z_adaptive_soundscape.ui.f.loop"), () -> currentTrack().loop,
                () -> currentTrack().loop = !currentTrack().loop);
        loop.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.f.loop.tip")));
        Button preview = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.editor.preview"), b -> previewTrack())
                .bounds(0, 0, 10, 20).build();
        preview.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.editor.preview.tip")));
        Button follow = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.f.follow"), b -> {
            volumeDraft = "";
            if (volumeBox != null) volumeBox.setValue("");
            commitTrackEditor();
        }).bounds(0, 0, 10, 20).build();
        follow.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.f.follow.tip")));
        label("z80z_adaptive_soundscape.ui.f.weight", row.get(0)[0], propsY - 10);
        label("z80z_adaptive_soundscape.ui.f.volume", row.get(1)[0], propsY - 10);
        place(propsY, row, weightBox, volumeBox, loop, preview, follow);

        Button choose = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.editor.choose"), b -> chooseFromLibrary())
                .bounds(0, 0, 10, 20).build();
        choose.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.editor.choose.tip")));
        Button add = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.editor.add_track"), b -> addTrack())
                .bounds(0, 0, 10, 20).build();
        Button copy = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.editor.copy_track"), b -> copyTrack())
                .bounds(0, 0, 10, 20).build();
        Button remove = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.editor.remove_track"), b -> removeTrack())
                .bounds(0, 0, 10, 20).build();
        Button up = Button.builder(Component.literal("↑"), b -> moveTrack(-1)).bounds(0, 0, 10, 20).build();
        Button down = Button.builder(Component.literal("↓"), b -> moveTrack(1)).bounds(0, 0, 10, 20).build();
        List<int[]> ops = Ui.flow(w, 4, 22, opsWidths);
        place(opsTop, ops, choose, add, copy, remove, up, down);
    }

    /** 基础/条件页用：提示行仅在最后元素下方仍有空间时显示。 */
    private void clampHint(int lastElementBottom) {
        hintVisible = lastElementBottom + 6 <= hintY;
    }

    // ===== 渲染 =====

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hub.editingRule() == null) return;
        for (Label label : labels) {
            graphics.drawString(hub.font(), I18n.get(label.key()), hub.contentX() + label.x(), label.y(),
                    Ui.MUTED_COLOR, false);
        }
        if (tab == Tab.TRACKS) renderTrackList(graphics);
    }

    private void label(String key, int x, int y) {
        labels.add(new Label(key, x, y));
    }

    private record Label(String key, int x, int y) {
    }

    private void renderTrackList(GuiGraphics graphics) {
        MusicConfig.Rule rule = hub.editingRule();
        if (rule == null) return;
        List<MusicConfig.Track> tracks = rule.tracks;
        trackScroll = Math.max(0, Math.min(trackScroll, Math.max(0, tracks.size() - visibleTrackRows)));
        int x = hub.contentX();
        int w = hub.contentW();
        Ui.panel(graphics, x, trackListTop - 2, w, trackListBottom - trackListTop + 4);
        int rowX = x + 4;
        int rowW = w - 12;
        for (int row = 0; row < visibleTrackRows && row + trackScroll < tracks.size(); row++) {
            int index = row + trackScroll;
            MusicConfig.Track track = tracks.get(index);
            int y = trackListTop + 1 + row * TRACK_ROW_HEIGHT;
            int fill = index == selectedTrack ? 0xAA604090 : (row % 2 == 0 ? 0x55303048 : 0x55202038);
            graphics.fill(rowX, y, rowX + rowW, y + 15, fill);
            float effective = CustomMusicPack.resolveTrackVolume(track.sound, track.volume);
            String source = track.volume >= 0.0F ? I18n.get("z80z_adaptive_soundscape.ui.term.independent")
                    : (isLibrarySound(track.sound) ? I18n.get("z80z_adaptive_soundscape.ui.term.follow_lib") : "");
            String info = "×" + Math.max(1, track.weight) + " · " + Math.round(effective * 100.0F) + "%" + source
                    + (track.loop ? " · ♩" : "");
            graphics.drawString(hub.font(),
                    Ui.trunc(hub.font(), (index + 1) + ". " + track.sound, rowW - hub.font().width(info) - 20),
                    rowX + 6, y + 4, index == selectedTrack ? 0xFFFFFFFF : Ui.TEXT_COLOR, false);
            graphics.drawString(hub.font(), info, rowX + rowW - 6 - hub.font().width(info), y + 4,
                    Ui.MUTED_COLOR, false);
        }
        Ui.scrollbar(graphics, x + w - 7, trackListTop, trackListBottom - trackListTop,
                tracks.size(), visibleTrackRows, trackScroll);
    }

    @Override
    void renderLate(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!hintVisible) return;
        int w = hub.contentW();
        graphics.drawCenteredString(hub.font(), Ui.trunc(hub.font(), tracksHint(), w),
                hub.contentX() + w / 2, hintY, Ui.MUTED_COLOR);
    }

    private String tracksHint() {
        String baseKey = switch (tab) {
            case BASIC -> "z80z_adaptive_soundscape.ui.hint.basic";
            case CONDITION -> "z80z_adaptive_soundscape.ui.hint.condition";
            case TRACKS -> "z80z_adaptive_soundscape.ui.hint.tracks";
        };
        if (tab == Tab.TRACKS) {
            MusicConfig.Track track = currentTrack();
            if (isLibrarySound(track.sound)) {
                float libraryVolume = CustomMusicPack.libraryVolume(track.sound);
                if (libraryVolume != 1.0F) {
                    return I18n.get("z80z_adaptive_soundscape.ui.hint.tracks_volume",
                            Math.round(libraryVolume * 100.0F));
                }
            }
        }
        return I18n.get(baseKey);
    }

    // ===== 交互 =====

    @Override
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == Tab.TRACKS && button == 0
                && mouseX >= hub.contentX() + 4 && mouseX < hub.contentX() + hub.contentW() - 8
                && mouseY >= trackListTop && mouseY < trackListBottom) {
            int index = trackScroll + ((int) mouseY - trackListTop - 1) / TRACK_ROW_HEIGHT;
            if (hub.editingRule() != null && index >= 0 && index < hub.editingRule().tracks.size()) {
                commitTrackEditor();
                selectTrack(index);
            }
            return true;
        }
        return false;
    }

    @Override
    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == Tab.TRACKS && mouseY >= trackListTop && mouseY <= trackListBottom) {
            trackScroll = Math.max(0, trackScroll - (int) Math.signum(delta));
            return true;
        }
        return false;
    }

    // ===== 操作 =====

    private void previewTrack() {
        commitTrackEditor();
        MusicConfig.Track track = currentTrack();
        float effective = CustomMusicPack.resolveTrackVolume(track.sound, track.volume);
        PreviewPlayer.play(hub.mc(), track.sound, effective);
        hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.editor.previewing", track.sound,
                Math.round(effective * 100.0F),
                I18n.get(track.volume >= 0.0F ? "z80z_adaptive_soundscape.ui.term.independent"
                        : "z80z_adaptive_soundscape.ui.term.follow_lib")), false);
    }

    private void chooseFromLibrary() {
        commitTrackEditor();
        hub.openLibraryPicker(trackId -> {
            soundDraft = trackId;
            if (soundBox != null) soundBox.setValue(trackId);
        });
    }

    private void addTrack() {
        hub.editingRule().tracks.add(new MusicConfig.Track());
        selectTrack(hub.editingRule().tracks.size() - 1);
        hub.rebuild();
    }

    private void copyTrack() {
        commitTrackEditor();
        MusicConfig.Rule rule = hub.editingRule();
        rule.tracks.add(selectedTrack + 1, currentTrack().copy());
        selectTrack(selectedTrack + 1);
        hub.rebuild();
    }

    private void removeTrack() {
        MusicConfig.Rule rule = hub.editingRule();
        if (rule.tracks.size() <= 1) {
            rule.tracks.set(0, new MusicConfig.Track());
        } else {
            rule.tracks.remove(selectedTrack);
        }
        selectTrack(Math.min(selectedTrack, rule.tracks.size() - 1));
        hub.rebuild();
    }

    private void moveTrack(int direction) {
        MusicConfig.Rule rule = hub.editingRule();
        int target = selectedTrack + direction;
        if (target < 0 || target >= rule.tracks.size()) return;
        commitTrackEditor();
        MusicConfig.Track moved = rule.tracks.remove(selectedTrack);
        rule.tracks.add(target, moved);
        selectTrack(target);
        hub.rebuild();
    }

    private void selectTrack(int index) {
        MusicConfig.Rule rule = hub.editingRule();
        selectedTrack = Math.max(0, Math.min(index, rule.tracks.size() - 1));
        MusicConfig.Track track = currentTrack();
        soundDraft = track.sound;
        weightDraft = String.valueOf(track.weight);
        volumeDraft = track.volume < 0.0F ? "" : String.valueOf(track.volume);
        if (soundBox != null) {
            soundBox.setValue(soundDraft);
            weightBox.setValue(weightDraft);
            volumeBox.setValue(volumeDraft);
        }
    }

    private void commitTrackEditor() {
        MusicConfig.Rule rule = hub.editingRule();
        if (rule == null || rule.tracks.isEmpty()) return;
        MusicConfig.Track track = currentTrack();
        if (soundDraft != null) track.sound = soundDraft.trim();
        if (weightDraft != null) {
            try {
                track.weight = Math.max(1, Integer.parseInt(weightDraft.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (volumeDraft != null) {
            if (volumeDraft.trim().isEmpty()) {
                track.volume = MusicConfig.Track.FOLLOW_LIBRARY;
            } else {
                try {
                    track.volume = (float) Math.max(0.0, Math.min(2.0, Double.parseDouble(volumeDraft.trim())));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private void save() {
        try {
            commitTrackEditor();
            MusicConfig.Rule rule = hub.editingRule();
            rule.id = idText.trim();
            rule.name = nameText.trim();
            rule.priority = Integer.parseInt(priorityText.trim());
            rule.minEnemies = Math.max(0, Integer.parseInt(minText.trim()));
            rule.maxEnemies = Integer.parseInt(maxText.trim());
            rule.dimensions = split(dimensionText);
            rule.biomeIds = split(biomeText);
            rule.biomeTags = split(tagText);
            ConfigManager.save();
            MusicController.INSTANCE.forceRefresh();
            PreviewPlayer.stop();
            hub.exitEditor();
        } catch (NumberFormatException exception) {
            hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.editor.bad_number"), true);
        }
    }

    private MusicConfig.Track currentTrack() {
        MusicConfig.Rule rule = hub.editingRule();
        if (rule == null) return new MusicConfig.Track();
        if (rule.tracks.isEmpty()) rule.tracks.add(new MusicConfig.Track());
        if (selectedTrack >= rule.tracks.size()) selectedTrack = rule.tracks.size() - 1;
        if (selectedTrack < 0) selectedTrack = 0;
        return rule.tracks.get(selectedTrack);
    }

    private static boolean isLibrarySound(String soundId) {
        return soundId != null && (soundId.startsWith(CustomMusicPack.NAMESPACE + ":")
                || soundId.startsWith(CustomMusicPack.LIB_NAMESPACE + ":"));
    }

    // ===== 控件工厂 =====

    private EditBox field(String value, Consumer<String> responder, String labelKey, String tooltipKey) {
        EditBox box = new EditBox(hub.font(), 0, 0, 10, 20, Component.translatable(labelKey));
        box.setMaxLength(512);
        box.setValue(value);
        box.setResponder(responder);
        box.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        box.moveCursorToStart();
        return box;
    }

    private Button cycleButton(String nameKey, String currentValueKey, java.util.function.Supplier<String> next,
                               String tooltipKey) {
        Button button = Button.builder(
                        Component.literal(I18n.get(nameKey) + I18n.get(currentValueKey)),
                        click -> click.setMessage(Component.literal(I18n.get(nameKey) + next.get())))
                .bounds(0, 0, 10, 20).build();
        button.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        return button;
    }

    private void place(int baseY, List<int[]> positions, AbstractWidget... widgets) {
        for (int i = 0; i < widgets.length; i++) {
            int[] position = positions.get(i);
            widgets[i].setX(hub.contentX() + position[0]);
            widgets[i].setY(baseY + position[1]);
            widgets[i].setWidth(position[2]);
            hub.addWidget(widgets[i]);
        }
    }

    private List<int[]> single(int width) {
        return List.of(new int[]{(hub.contentW() - width) / 2, 0, width});
    }

    private static ArrayList<String> split(String value) {
        ArrayList<String> result = new ArrayList<>();
        Arrays.stream(value.split(",")).map(String::trim).filter(part -> !part.isBlank()).forEach(result::add);
        return result;
    }

    private static <T> T cycle(T current, T[] values) {
        int index = Arrays.asList(values).indexOf(current);
        return values[(index + 1) % values.length];
    }

    private static String stateKey(MusicConfig.State value) {
        return switch (value) {
            case ANY -> "z80z_adaptive_soundscape.ui.state.any";
            case EXPLORE -> "z80z_adaptive_soundscape.ui.state.explore";
            case ALERT -> "z80z_adaptive_soundscape.ui.state.alert";
            case COMBAT -> "z80z_adaptive_soundscape.ui.state.combat";
        };
    }

    private static String bossKey(MusicConfig.BossStage value) {
        return switch (value) {
            case ANY -> "z80z_adaptive_soundscape.ui.boss.any";
            case NONE -> "z80z_adaptive_soundscape.ui.boss.none";
            case NEAR -> "z80z_adaptive_soundscape.ui.boss.near";
            case ENGAGED -> "z80z_adaptive_soundscape.ui.boss.engaged";
            case PHASE_TWO -> "z80z_adaptive_soundscape.ui.boss.phase_two";
        };
    }

    private static String timeKey(MusicConfig.Time value) {
        return switch (value) {
            case ANY -> "z80z_adaptive_soundscape.ui.time.any";
            case DAY -> "z80z_adaptive_soundscape.ui.time.day";
            case NIGHT -> "z80z_adaptive_soundscape.ui.time.night";
        };
    }
}
