package cn.blockforge.generated.adaptivesoundscape.client.gui.hub;

import cn.blockforge.generated.adaptivesoundscape.client.MusicController;
import cn.blockforge.generated.adaptivesoundscape.client.gui.Ui;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.List;

final class JsonSection extends Section {
    private MultiLineEditBox editor;

    @Override
    void build() {
        int x = hub.contentX();
        int w = hub.contentW();
        int buttonRow = hub.contentY() + hub.contentH() - 22;
        editor = new MultiLineEditBox(hub.font(), x, hub.contentY() + 2, w, buttonRow - hub.contentY() - 10,
                Component.translatable("z80z_adaptive_soundscape.ui.json.editor"),
                Component.translatable("z80z_adaptive_soundscape.ui.json.hint"));
        editor.setCharacterLimit(1_000_000);
        editor.setValue(ConfigManager.exportJson());
        hub.addWidget(editor);

        Button save = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.json.save"), b -> save())
                .bounds(0, 0, 10, 20).build();
        save.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.json.save.tip")));
        Button reload = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.json.reload"), b -> reload())
                .bounds(0, 0, 10, 20).build();
        reload.setTooltip(Tooltip.create(Component.translatable("z80z_adaptive_soundscape.ui.json.reload.tip")));
        List<int[]> flow = Ui.flow(w, 4, 20, 100, 100);
        int index = 0;
        for (int[] position : flow) {
            Button button = index++ == 0 ? save : reload;
            button.setX(x + position[0]);
            button.setY(buttonRow + position[1]);
            button.setWidth(position[2]);
            hub.addWidget(button);
        }
    }

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(hub.font(), I18n.get("z80z_adaptive_soundscape.ui.json.header"),
                hub.contentX(), hub.contentY() - 10, Ui.MUTED_COLOR, false);
    }

    @Override
    void tick() {
        if (editor != null) editor.tick();
    }

    private void save() {
        String result = ConfigManager.importJson(editor.getValue());
        if (!ConfigManager.lastImportFailed()) MusicController.INSTANCE.forceRefresh();
        hub.setStatus(result, ConfigManager.lastImportFailed());
    }

    private void reload() {
        ConfigManager.load();
        editor.setValue(ConfigManager.exportJson());
        String error = ConfigManager.lastError();
        hub.setStatus(error.isBlank()
                ? I18n.get("message.z80z_adaptive_soundscape.json.reloaded")
                : I18n.get("message.z80z_adaptive_soundscape.json.reload_failed", error), !error.isBlank());
        MusicController.INSTANCE.forceRefresh();
    }
}
