package cn.blockforge.generated.adaptivesoundscape.client.gui.hub;

import cn.blockforge.generated.adaptivesoundscape.client.CustomMusicPack;
import cn.blockforge.generated.adaptivesoundscape.client.MusicController;
import cn.blockforge.generated.adaptivesoundscape.client.gui.Ui;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class RulesSection extends Section {
    private static final int ROW_HEIGHT = 20;

    private int selected = -1;
    private int dragging = -1;
    private boolean dragMoved;
    private int scroll;
    private int lastClickIndex = -1;
    private long lastClickTime;
    private int listTop;
    private int listBottom;
    private int visibleRows = 1;
    private int hoveredRow = -1;

    @Override
    void build() {
        int x = hub.contentX();
        int w = hub.contentW();
        int bottomRow = hub.contentY() + hub.contentH() - 22;
        listTop = hub.contentY() + 2;
        listBottom = bottomRow - 6;

        Button add = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.rules.add"), b -> addRule())
                .bounds(0, 0, 10, 20).build();
        add.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.rules.add.tip")));
        Button edit = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.rules.edit"), b -> editSelected())
                .bounds(0, 0, 10, 20).build();
        edit.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.rules.edit.tip")));
        Button copy = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.rules.copy"), b -> copySelected())
                .bounds(0, 0, 10, 20).build();
        Button delete = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.rules.delete"), b -> deleteSelected())
                .bounds(0, 0, 10, 20).build();
        delete.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.rules.delete.tip")));
        Button up = Button.builder(Component.literal("↑"), b -> move(-1)).bounds(0, 0, 10, 20).build();
        Button down = Button.builder(Component.literal("↓"), b -> move(1)).bounds(0, 0, 10, 20).build();
        List<int[]> flow = Ui.flow(w, 4, 22, 50, 50, 50, 50, 26, 26);
        int index = 0;
        for (int[] position : flow) {
            Button button = switch (index++) {
                case 0 -> add;
                case 1 -> edit;
                case 2 -> copy;
                case 3 -> delete;
                case 4 -> up;
                default -> down;
            };
            button.setX(x + position[0]);
            button.setY(bottomRow + position[1]);
            button.setWidth(position[2]);
            hub.addWidget(button);
        }
    }

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        List<MusicConfig.Rule> rules = ConfigManager.get().rules;
        int x = hub.contentX();
        int w = hub.contentW();
        visibleRows = Math.max(1, (listBottom - listTop - 4) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rules.size() - visibleRows)));
        Ui.panel(graphics, x, listTop - 2, w, listBottom - listTop + 4);
        hoveredRow = -1;
        int rowX = x + 4;
        int rowW = w - 12;
        int nameW = Math.max(60, rowW * 2 / 5);
        for (int row = 0; row < visibleRows && row + scroll < rules.size(); row++) {
            int index = row + scroll;
            MusicConfig.Rule rule = rules.get(index);
            int y = listTop + 2 + row * ROW_HEIGHT;
            int fill = index == selected ? 0xAA604090
                    : index == dragging ? 0x80483878
                    : (row % 2 == 0 ? 0x55303048 : 0x55202038);
            graphics.fill(rowX, y, rowX + rowW, y + 18, fill);
            if (mouseX >= rowX && mouseX < rowX + rowW && mouseY >= y && mouseY < y + 18) hoveredRow = index;
            String dot = rule.enabled ? "●" : "○";
            graphics.drawString(hub.font(), Ui.trunc(hub.font(), dot + " " + rule.name, nameW), rowX + 6, y + 5,
                    rule.enabled ? 0xFFFFFFFF : 0xFF888888, false);
            graphics.drawString(hub.font(), Ui.trunc(hub.font(), summary(rule), rowW - nameW - 14),
                    rowX + nameW + 8, y + 5, Ui.MUTED_COLOR, false);
        }
        if (rules.isEmpty()) {
            graphics.drawCenteredString(hub.font(),
                    I18n.get("z80z_adaptive_soundscape.ui.rules.empty"), x + w / 2, listTop + 10, 0xFFAAAAAA);
        }
        Ui.scrollbar(graphics, x + w - 7, listTop, listBottom - listTop, rules.size(), visibleRows, scroll);
    }

    @Override
    void renderLate(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredRow < 0 || dragging >= 0) return;
        List<MusicConfig.Rule> rules = ConfigManager.get().rules;
        if (hoveredRow >= rules.size()) return;
        graphics.renderComponentTooltip(hub.font(), tooltipLines(rules.get(hoveredRow)), mouseX, mouseY);
    }

    @Override
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rowX = hub.contentX() + 4;
        int rowW = hub.contentW() - 12;
        if (button == 0 && mouseX >= rowX && mouseX < rowX + rowW && mouseY >= listTop && mouseY < listBottom) {
            int index = scroll + ((int) mouseY - listTop - 2) / ROW_HEIGHT;
            if (index >= 0 && index < ConfigManager.get().rules.size()) {
                if (Screen.hasShiftDown()) {
                    MusicConfig.Rule rule = ConfigManager.get().rules.get(index);
                    rule.enabled = !rule.enabled;
                    ConfigManager.save();
                    MusicController.INSTANCE.forceRefresh();
                } else {
                    long now = System.currentTimeMillis();
                    if (index == lastClickIndex && now - lastClickTime < 300L) {
                        hub.openEditor(ConfigManager.get().rules.get(index));
                        return true;
                    }
                    lastClickIndex = index;
                    lastClickTime = now;
                }
                selected = index;
                dragging = index;
                return true;
            }
        }
        return false;
    }

    @Override
    boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging >= 0) {
            int target = scroll + ((int) mouseY - listTop - 2) / ROW_HEIGHT;
            List<MusicConfig.Rule> rules = ConfigManager.get().rules;
            target = Math.max(0, Math.min(rules.size() - 1, target));
            if (target != dragging) {
                MusicConfig.Rule moved = rules.remove(dragging);
                rules.add(target, moved);
                dragging = target;
                selected = target;
                dragMoved = true;
            }
            return true;
        }
        return false;
    }

    @Override
    boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging >= 0) {
            dragging = -1;
            if (dragMoved) {
                dragMoved = false;
                ConfigManager.save();
                MusicController.INSTANCE.forceRefresh();
            }
            return true;
        }
        return false;
    }

    @Override
    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= hub.contentX() && mouseX <= hub.contentX() + hub.contentW()
                && mouseY >= listTop && mouseY <= listBottom) {
            scroll = Math.max(0, scroll - (int) Math.signum(delta));
            return true;
        }
        return false;
    }

    private void addRule() {
        MusicConfig.Rule rule = new MusicConfig.Rule();
        rule.id = "custom_" + (ConfigManager.get().rules.size() + 1);
        rule.tracks.add(new MusicConfig.Track());
        ConfigManager.get().rules.add(0, rule);
        ConfigManager.save();
        hub.openEditor(rule);
    }

    private void editSelected() {
        if (validSelection()) hub.openEditor(ConfigManager.get().rules.get(selected));
    }

    private void copySelected() {
        if (!validSelection()) return;
        ConfigManager.get().rules.add(selected + 1, ConfigManager.get().rules.get(selected).copy());
        selected++;
        ConfigManager.save();
        MusicController.INSTANCE.forceRefresh();
    }

    private void deleteSelected() {
        if (!validSelection()) return;
        ConfigManager.get().rules.remove(selected);
        selected = Math.min(selected, ConfigManager.get().rules.size() - 1);
        ConfigManager.save();
        MusicController.INSTANCE.forceRefresh();
    }

    private void move(int direction) {
        if (!validSelection()) return;
        int target = selected + direction;
        if (target < 0 || target >= ConfigManager.get().rules.size()) return;
        MusicConfig.Rule rule = ConfigManager.get().rules.remove(selected);
        ConfigManager.get().rules.add(target, rule);
        selected = target;
        ConfigManager.save();
        MusicController.INSTANCE.forceRefresh();
    }

    private boolean validSelection() {
        return selected >= 0 && selected < ConfigManager.get().rules.size();
    }

    private static String summary(MusicConfig.Rule rule) {
        StringBuilder text = new StringBuilder("P").append(rule.priority);
        if (rule.state != MusicConfig.State.ANY) text.append(" · ").append(I18n.get(stateKey(rule.state)));
        if (rule.minEnemies > 0 || rule.maxEnemies >= 0) {
            text.append(" · ").append(I18n.get("z80z_adaptive_soundscape.ui.term.enemies",
                    rule.minEnemies, rule.maxEnemies < 0 ? "+" : "-" + rule.maxEnemies));
        }
        if (rule.bossStage != MusicConfig.BossStage.ANY) text.append(" · ").append(I18n.get(bossKey(rule.bossStage)));
        if (rule.time != MusicConfig.Time.ANY) text.append(" · ").append(I18n.get(timeKey(rule.time)));
        if (!rule.dimensions.isEmpty()) text.append(" · ").append(I18n.get("z80z_adaptive_soundscape.ui.term.dims",
                rule.dimensions.size()));
        if (!rule.biomeIds.isEmpty() || !rule.biomeTags.isEmpty()) {
            text.append(" · ").append(I18n.get("z80z_adaptive_soundscape.ui.term.biomes"));
        }
        text.append(" · ").append(I18n.get("z80z_adaptive_soundscape.ui.term.tracks", rule.tracks.size()));
        return text.toString();
    }

    private static List<Component> tooltipLines(MusicConfig.Rule rule) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal((rule.enabled ? "● " : "○ ") + rule.name)
                .withStyle(rule.enabled ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        lines.add(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.tip.rule_id",
                rule.id, rule.priority)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.tip.rule_cond",
                I18n.get(stateKey(rule.state)), I18n.get(timeKey(rule.time)), I18n.get(bossKey(rule.bossStage))))
                .withStyle(ChatFormatting.GRAY));
        if (rule.minEnemies > 0 || rule.maxEnemies >= 0) {
            lines.add(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.term.enemies",
                    rule.minEnemies, rule.maxEnemies < 0 ? "+" : "-" + rule.maxEnemies))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!rule.dimensions.isEmpty()) {
            lines.add(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.tip.dims",
                    String.join(", ", rule.dimensions))).withStyle(ChatFormatting.GRAY));
        }
        if (!rule.biomeIds.isEmpty() || !rule.biomeTags.isEmpty()) {
            List<String> biomes = new ArrayList<>(rule.biomeIds);
            biomes.addAll(rule.biomeTags);
            lines.add(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.tip.biomes",
                    String.join(", ", biomes))).withStyle(ChatFormatting.GRAY));
        }
        int totalWeight = 0;
        for (MusicConfig.Track track : rule.tracks) totalWeight += Math.max(1, track.weight);
        lines.add(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.tip.tracks",
                rule.tracks.size(), totalWeight)).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < Math.min(3, rule.tracks.size()); i++) {
            MusicConfig.Track track = rule.tracks.get(i);
            float effective = CustomMusicPack.resolveTrackVolume(track.sound, track.volume);
            lines.add(Component.literal("  ♪ " + track.sound + " ×" + Math.max(1, track.weight)
                    + " · " + Math.round(effective * 100.0F) + "%"
                    + (track.volume >= 0.0F ? I18n.get("z80z_adaptive_soundscape.ui.term.independent") : ""))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        lines.add(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.rules.row_hint"))
                .withStyle(ChatFormatting.DARK_GRAY));
        return lines;
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
