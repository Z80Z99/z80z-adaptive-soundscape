package cn.blockforge.generated.adaptivesoundscape.client.gui.hub;

import cn.blockforge.generated.adaptivesoundscape.client.MusicController;
import cn.blockforge.generated.adaptivesoundscape.client.gui.Ui;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class SettingsSection extends Section {
    private static final String[] LABEL_KEYS = {
            "z80z_adaptive_soundscape.ui.set.scan_interval",
            "z80z_adaptive_soundscape.ui.set.combat_hold",
            "z80z_adaptive_soundscape.ui.set.fade_ticks",
            "z80z_adaptive_soundscape.ui.set.hostile_radius",
            "z80z_adaptive_soundscape.ui.set.boss_radius",
            "z80z_adaptive_soundscape.ui.set.boss_health",
            "z80z_adaptive_soundscape.ui.set.preview_volume"
    };
    private static final String[] TOOLTIP_KEYS = {
            "z80z_adaptive_soundscape.ui.set.scan_interval.tip",
            "z80z_adaptive_soundscape.ui.set.combat_hold.tip",
            "z80z_adaptive_soundscape.ui.set.fade_ticks.tip",
            "z80z_adaptive_soundscape.ui.set.hostile_radius.tip",
            "z80z_adaptive_soundscape.ui.set.boss_radius.tip",
            "z80z_adaptive_soundscape.ui.set.boss_health.tip",
            "z80z_adaptive_soundscape.ui.set.preview_volume.tip"
    };

    private final String[] drafts = new String[8];
    private boolean loaded;
    private final List<Row> rows = new ArrayList<>();
    private int scroll;
    private int maxScroll;

    @Override
    void build() {
        rows.clear();
        int x = hub.contentX();
        int w = hub.contentW();
        if (!loaded) {
            MusicConfig config = ConfigManager.get();
            drafts[0] = String.valueOf(config.scanIntervalTicks);
            drafts[1] = String.valueOf(config.combatHoldTicks);
            drafts[2] = String.valueOf(config.fadeTicks);
            drafts[3] = String.valueOf(config.hostileRadius);
            drafts[4] = String.valueOf(config.bossRadius);
            drafts[5] = String.valueOf(config.bossHealthThreshold);
            drafts[6] = String.valueOf(config.previewVolume);
            drafts[7] = String.join(", ", config.bossEntities);
            loaded = true;
        }

        boolean twoColumns = w >= 340;
        int cellWidth = twoColumns ? (w - 10) / 2 : w;
        int y = hub.contentY() + 2;
        int column = 0;
        for (int i = 0; i < 8; i++) {
            boolean fullWidth = i == 7;
            Row row = new Row(i, fullWidth);
            EditBox box = new EditBox(hub.font(), 0, 0, 10, 20,
                    Component.translatable(labelKey(i)));
            box.setMaxLength(fullWidth ? 1024 : 16);
            if (!fullWidth) box.setFilter(Ui.numberFilter());
            box.setValue(drafts[i]);
            final int index = i;
            box.setResponder(value -> drafts[index] = value);
            box.moveCursorToStart();
            box.setTooltip(Tooltip.create(Component.translatable(i == 7
                    ? "z80z_adaptive_soundscape.ui.set.boss_entities.tip"
                    : TOOLTIP_KEYS[i])));
            row.box = box;
            if (fullWidth || !twoColumns) {
                row.x = 0;
                row.y = y;
                y += 32;
                column = 0;
            } else {
                row.x = column * (cellWidth + 10);
                row.y = y;
                if (column == 0) {
                    column = 1;
                } else {
                    column = 0;
                    y += 32;
                }
            }
            box.setX(x + row.x);
            box.setY(row.y + 10);
            box.setWidth(fullWidth || !twoColumns ? w : cellWidth);
            hub.addWidget(box);
            rows.add(row);
        }

        int buttonRow = hub.contentY() + hub.contentH() - 22;
        Button reset = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.set.reset"), b -> resetDefaults())
                .bounds(0, 0, 10, 20).build();
        reset.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.set.reset.tip")));
        Button save = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.set.save"), b -> save())
                .bounds(0, 0, 10, 20).build();
        save.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.set.save.tip")));
        List<int[]> flow = Ui.flow(w, 4, 20, 84, 60);
        int index = 0;
        for (int[] position : flow) {
            Button button = index++ == 0 ? reset : save;
            button.setX(x + position[0]);
            button.setY(buttonRow + position[1]);
            button.setWidth(position[2]);
            hub.addWidget(button);
        }

        int contentBottomLimit = buttonRow - 8;
        int contentHeight = rows.isEmpty() ? 0 : rows.get(rows.size() - 1).y + 30 - hub.contentY();
        maxScroll = Math.max(0, contentHeight - (contentBottomLimit - hub.contentY()));
        scroll = Math.min(scroll, maxScroll);
        applyScroll(contentBottomLimit);
    }

    private void applyScroll(int limit) {
        for (Row row : rows) {
            int labelY = row.y - scroll;
            int boxY = row.y + 10 - scroll;
            row.box.setY(boxY);
            row.box.visible = labelY >= hub.contentY() - 2 && boxY + 20 <= limit + 2;
        }
    }

    @Override
    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0 && mouseX >= hub.contentX() && mouseX <= hub.contentX() + hub.contentW()) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(delta)));
            applyScroll(hub.contentY() + hub.contentH() - 30);
            return true;
        }
        return false;
    }

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        for (Row row : rows) {
            int labelY = row.y - scroll;
            if (labelY < hub.contentY() - 2 || labelY > hub.contentY() + hub.contentH() - 30) continue;
            graphics.drawString(hub.font(), I18n.get(labelKey(row.index)),
                    hub.contentX() + row.x, labelY, Ui.MUTED_COLOR, false);
        }
        if (maxScroll > 0) {
            Ui.scrollbar(graphics, hub.contentX() + hub.contentW() - 5, hub.contentY(),
                    hub.contentH() - 30, rows.size() + 1, Math.max(1, (hub.contentH() - 30) / 32), scroll / 32);
        }
    }

    private String labelKey(int index) {
        return index == 7 ? "z80z_adaptive_soundscape.ui.set.boss_entities" : LABEL_KEYS[index];
    }

    private void save() {
        MusicConfig config = ConfigManager.get();
        try {
            config.scanIntervalTicks = parse(0);
            config.combatHoldTicks = parse(1);
            config.fadeTicks = parse(2);
            config.hostileRadius = parseDouble(3);
            config.bossRadius = parseDouble(4);
            config.bossHealthThreshold = parseDouble(5);
            config.previewVolume = (float) Math.max(0.0, Math.min(2.0, parseDouble(6)));
            config.bossEntities = splitList(drafts[7]);
            ConfigManager.save();
            MusicController.INSTANCE.forceRefresh();
            hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.set.saved"), false);
        } catch (IllegalArgumentException exception) {
            hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.set.bad_number", exception.getMessage()), true);
        }
    }

    private void resetDefaults() {
        MusicConfig defaults = new MusicConfig();
        drafts[0] = String.valueOf(defaults.scanIntervalTicks);
        drafts[1] = String.valueOf(defaults.combatHoldTicks);
        drafts[2] = String.valueOf(defaults.fadeTicks);
        drafts[3] = String.valueOf(defaults.hostileRadius);
        drafts[4] = String.valueOf(defaults.bossRadius);
        drafts[5] = String.valueOf(defaults.bossHealthThreshold);
        drafts[6] = String.valueOf(defaults.previewVolume);
        drafts[7] = String.join(", ", defaults.bossEntities);
        hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.set.defaults_filled"), false);
        hub.rebuild();
    }

    private int parse(int index) {
        try {
            return Integer.parseInt(drafts[index].trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(I18n.get(labelKey(index)));
        }
    }

    private double parseDouble(int index) {
        try {
            return Double.parseDouble(drafts[index].trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(I18n.get(labelKey(index)));
        }
    }

    private static List<String> splitList(String value) {
        List<String> result = new ArrayList<>();
        Arrays.stream(value.split(",")).map(String::trim).filter(part -> !part.isBlank()).forEach(result::add);
        return result;
    }

    private static final class Row {
        final int index;
        boolean fullWidth;
        EditBox box;
        int x;
        int y;

        Row(int index, boolean fullWidth) {
            this.index = index;
            this.fullWidth = fullWidth;
        }
    }
}
