package cn.blockforge.generated.adaptivesoundscape.client.gui.hub;

import cn.blockforge.generated.adaptivesoundscape.client.CustomMusicPack;
import cn.blockforge.generated.adaptivesoundscape.client.MusicController;
import cn.blockforge.generated.adaptivesoundscape.client.MusicOverlay;
import cn.blockforge.generated.adaptivesoundscape.client.PreviewPlayer;
import cn.blockforge.generated.adaptivesoundscape.client.gui.Ui;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.List;

final class LibrarySection extends Section {
    private static final int ROW_HEIGHT = 18;

    private int selected = -1;
    private int scroll;
    private int listTop;
    private int listBottom;
    private int visibleRows = 1;
    private TotalVolumeSlider totalSlider;

    @Override
    void build() {
        int x = hub.contentX();
        int w = hub.contentW();
        int buttonRow = hub.contentY() + hub.contentH() - 22;
        int slider2 = hub.contentY() + hub.contentH() - 46;
        int slider1 = hub.contentY() + hub.contentH() - 70;
        listTop = hub.contentY() + 2;
        listBottom = slider1 - 8;

        Button preview = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.lib.preview"), b -> previewSelected())
                .bounds(0, 0, 10, 20).build();
        preview.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.lib.preview.tip")));
        Button use = null;
        if (hub.isPicking()) {
            use = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.lib.use"), b -> useSelected())
                    .bounds(0, 0, 10, 20).build();
            use.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.lib.use.tip")));
        }
        Button delete = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.lib.delete"), b -> deleteSelected())
                .bounds(0, 0, 10, 20).build();
        delete.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.lib.delete.tip")));
        Button refresh = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.lib.refresh"), b -> refresh())
                .bounds(0, 0, 10, 20).build();
        refresh.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.lib.refresh.tip")));
        Button back = null;
        if (hub.isPicking()) {
            back = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.lib.cancel_pick"), b -> hub.cancelPicker())
                    .bounds(0, 0, 10, 20).build();
        }
        List<int[]> flow = use == null
                ? Ui.flow(w, 4, 22, 60, 50, 50, 60)
                : Ui.flow(w, 4, 22, 60, 60, 50, 50, 74);
        int index = 0;
        for (int[] position : flow) {
            Button button = switch (index) {
                case 0 -> preview;
                case 1 -> use != null ? use : delete;
                case 2 -> use != null ? delete : refresh;
                case 3 -> use != null ? refresh : back;
                default -> back;
            };
            if (button == null) break;
            button.setX(x + position[0]);
            button.setY(buttonRow + position[1]);
            button.setWidth(position[2]);
            hub.addWidget(button);
            index++;
        }

        totalSlider = new TotalVolumeSlider(x + w / 2 - 80, slider2, 160);
        totalSlider.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.lib.total_volume.tip")));
        hub.addWidget(totalSlider);
        PreviewVolumeSlider previewSlider = new PreviewVolumeSlider(x + w / 2 - 80, slider1, 160);
        previewSlider.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.lib.preview_volume.tip")));
        hub.addWidget(previewSlider);
    }

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        List<CustomMusicPack.Entry> entries = CustomMusicPack.libraryEntries();
        int x = hub.contentX();
        int w = hub.contentW();
        graphics.drawCenteredString(hub.font(), Ui.trunc(hub.font(),
                I18n.get("z80z_adaptive_soundscape.ui.lib.subtitle", entries.size()), w), x + w / 2,
                listTop - 10, Ui.MUTED_COLOR);
        visibleRows = Math.max(1, (listBottom - listTop - 4) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - visibleRows)));
        Ui.panel(graphics, x, listTop, w, listBottom - listTop + 2);
        int rowX = x + 4;
        int rowW = w - 12;
        for (int row = 0; row < visibleRows && row + scroll < entries.size(); row++) {
            int entryIndex = row + scroll;
            CustomMusicPack.Entry entry = entries.get(entryIndex);
            int y = listTop + 2 + row * ROW_HEIGHT;
            int fill = entryIndex == selected ? 0xAA604090 : (row % 2 == 0 ? 0x55303048 : 0x55202038);
            graphics.fill(rowX, y, rowX + rowW, y + 16, fill);
            float volume = CustomMusicPack.libraryVolume(entry.trackId());
            String info = entry.format()
                    + (volume != 1.0F ? " · " + Math.round(volume * 100.0F) + "%" : "")
                    + " · " + formatSize(entry.sizeBytes());
            String name = entry.fileName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
            graphics.drawString(hub.font(),
                    Ui.trunc(hub.font(), (entryIndex + 1) + ". " + name, rowW - hub.font().width(info) - 22),
                    rowX + 6, y + 4, entryIndex == selected ? 0xFFFFFFFF : Ui.TEXT_COLOR, false);
            graphics.drawString(hub.font(), info, rowX + rowW - 6 - hub.font().width(info), y + 4,
                    Ui.MUTED_COLOR, false);
        }
        if (entries.isEmpty()) {
            graphics.drawCenteredString(hub.font(),
                    I18n.get("z80z_adaptive_soundscape.ui.lib.empty"), x + w / 2, listTop + 12, 0xFFAAAAAA);
        }
        Ui.scrollbar(graphics, x + w - 7, listTop, listBottom - listTop, entries.size(), visibleRows, scroll);
    }

    @Override
    boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rowX = hub.contentX() + 4;
        int rowW = hub.contentW() - 12;
        if (button == 0 && mouseX >= rowX && mouseX < rowX + rowW && mouseY >= listTop && mouseY < listBottom) {
            int index = scroll + ((int) mouseY - listTop - 2) / ROW_HEIGHT;
            if (index >= 0 && index < CustomMusicPack.libraryEntries().size()) {
                selected = index;
                if (totalSlider != null) totalSlider.syncSelection();
                return true;
            }
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

    private CustomMusicPack.Entry selectedEntry() {
        List<CustomMusicPack.Entry> entries = CustomMusicPack.libraryEntries();
        if (selected < 0 || selected >= entries.size()) return null;
        return entries.get(selected);
    }

    private void previewSelected() {
        CustomMusicPack.Entry entry = selectedEntry();
        if (entry == null) {
            hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.lib.select_first"), true);
            return;
        }
        PreviewPlayer.play(hub.mc(), entry.trackId(), CustomMusicPack.libraryVolume(entry.trackId()));
        hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.lib.previewing", entry.fileName()), false);
    }

    private void useSelected() {
        CustomMusicPack.Entry entry = selectedEntry();
        if (entry == null) {
            hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.lib.select_first"), true);
            return;
        }
        CustomMusicPack.saveLibraryVolumes();
        hub.pickAndReturn(entry.trackId());
    }

    private void deleteSelected() {
        CustomMusicPack.Entry entry = selectedEntry();
        if (entry == null) {
            hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.lib.select_first"), true);
            return;
        }
        int references = CustomMusicPack.countReferences(entry.trackId());
        String fileName = entry.fileName();
        hub.mc().setScreen(new ConfirmScreen(result -> {
            if (result) {
                boolean deleted = CustomMusicPack.removeTrack(entry.trackId());
                if (deleted) {
                    MusicOverlay.announce(I18n.get("message.z80z_adaptive_soundscape.lib.deleted", fileName));
                    hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.lib.deleted_status", fileName), false);
                    selected = Math.min(selected, CustomMusicPack.libraryEntries().size() - 1);
                    if (totalSlider != null) totalSlider.syncSelection();
                    MusicController.INSTANCE.forceRefresh();
                } else {
                    hub.setStatus(I18n.get("message.z80z_adaptive_soundscape.lib.delete_failed"), true);
                }
            }
            hub.mc().setScreen(hub);
        }, Component.translatable("z80z_adaptive_soundscape.ui.lib.confirm_title"),
                Component.translatable("z80z_adaptive_soundscape.ui.lib.confirm_message", fileName, references),
                Component.translatable("z80z_adaptive_soundscape.ui.lib.confirm_yes"),
                Component.translatable("z80z_adaptive_soundscape.ui.lib.confirm_no")));
    }

    private void refresh() {
        CustomMusicPack.scan();
        selected = Math.min(selected, CustomMusicPack.libraryEntries().size() - 1);
        if (totalSlider != null) totalSlider.syncSelection();
        hub.setStatus(CustomMusicPack.lastScanMessage(), false);
        MusicController.INSTANCE.forceRefresh();
    }

    private final class PreviewVolumeSlider extends AbstractSliderButton {
        PreviewVolumeSlider(int x, int y, int width) {
            super(x, y, width, 20, Component.empty(),
                    Math.max(0.0, Math.min(1.0, ConfigManager.get().previewVolume / 2.0)));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.lib.preview_volume",
                    Math.round(ConfigManager.get().previewVolume * 100.0F))));
        }

        @Override
        protected void applyValue() {
            ConfigManager.get().previewVolume = (float) (value * 2.0);
            updateMessage();
        }
    }

    private final class TotalVolumeSlider extends AbstractSliderButton {
        TotalVolumeSlider(int x, int y, int width) {
            super(x, y, width, 20, Component.empty(), 0.5);
            syncSelection();
        }

        void syncSelection() {
            boolean hasSelection = selectedEntry() != null;
            active = hasSelection;
            if (hasSelection) {
                float volume = CustomMusicPack.libraryVolume(selectedEntry().trackId());
                value = Math.max(0.0, Math.min(1.0, volume / 2.0));
            }
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            CustomMusicPack.Entry entry = selectedEntry();
            if (entry == null) {
                setMessage(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.lib.total_volume.none")));
                return;
            }
            setMessage(Component.literal(I18n.get("z80z_adaptive_soundscape.ui.lib.total_volume",
                    Math.round(CustomMusicPack.libraryVolume(entry.trackId()) * 100.0F))));
        }

        @Override
        protected void applyValue() {
            CustomMusicPack.Entry entry = selectedEntry();
            if (entry == null) return;
            CustomMusicPack.setLibraryVolume(entry.trackId(), (float) (value * 2.0));
            updateMessage();
        }
    }

    private static String formatSize(long bytes) {
        if (bytes >= 1024 * 1024) return String.format("%.1fMB", bytes / 1048576.0);
        return Math.max(1, bytes / 1024) + "KB";
    }
}
