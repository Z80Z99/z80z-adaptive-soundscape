package cn.blockforge.generated.adaptivesoundscape.client.gui.hub;

import cn.blockforge.generated.adaptivesoundscape.client.CustomMusicPack;
import cn.blockforge.generated.adaptivesoundscape.client.MusicController;
import cn.blockforge.generated.adaptivesoundscape.client.PreviewPlayer;
import cn.blockforge.generated.adaptivesoundscape.client.gui.Ui;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import cn.blockforge.generated.adaptivesoundscape.config.MusicConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.function.Consumer;

/**
 * 配置中枢：单界面 + 左侧导航 + 内容区原位切换，取代原先的多个独立界面。
 */
public final class HubScreen extends Screen {
    public enum SectionId {OVERVIEW, RULES, LIBRARY, SETTINGS, JSON, EDITOR}

    private static final String[] NAV_KEYS = {
            "z80z_adaptive_soundscape.ui.nav.overview",
            "z80z_adaptive_soundscape.ui.nav.rules",
            "z80z_adaptive_soundscape.ui.nav.library",
            "z80z_adaptive_soundscape.ui.nav.settings",
            "z80z_adaptive_soundscape.ui.nav.json"
    };

    private final Screen parent;
    private final EnumMap<SectionId, Section> sections = new EnumMap<>(SectionId.class);
    private SectionId current = SectionId.OVERVIEW;
    private Consumer<String> libraryPicker;
    private MusicConfig.Rule editingRule;
    private String status = "";
    private boolean statusError;
    private int navX = 6;
    private int navY = 28;
    private int navW = 62;
    private int contentX;
    private int contentY = 28;
    private int contentW;
    private int contentH;

    public HubScreen(Screen parent) {
        super(Component.literal("情境感知背景音乐"));
        this.parent = parent;
    }

    // ===== 对节开放的 API =====

    public void addWidget(AbstractWidget widget) {
        addRenderableWidget(widget);
    }

    public void setStatus(String text, boolean error) {
        status = text;
        statusError = error;
    }

    public Minecraft mc() {
        return minecraft;
    }

    public Font font() {
        return font;
    }

    public int contentX() {
        return contentX;
    }

    public int contentY() {
        return contentY;
    }

    public int contentW() {
        return contentW;
    }

    public int contentH() {
        return contentH;
    }

    public SectionId currentSection() {
        return current;
    }

    public MusicConfig.Rule editingRule() {
        return editingRule;
    }

    public boolean isPicking() {
        return libraryPicker != null;
    }

    /** 延迟一帧重建（按钮回调中切换控件时避免并发修改）。 */
    public void rebuild() {
        minecraft.tell(this::rebuildWidgets);
    }

    public void switchTo(SectionId target) {
        if (target == current) return;
        if (current == SectionId.LIBRARY || current == SectionId.EDITOR) PreviewPlayer.stop();
        if (current == SectionId.LIBRARY) CustomMusicPack.saveLibraryVolumes();
        current = target;
        rebuild();
    }

    public void openEditor(MusicConfig.Rule rule) {
        PreviewPlayer.stop();
        editingRule = rule;
        current = SectionId.EDITOR;
        rebuild();
    }

    public void exitEditor() {
        PreviewPlayer.stop();
        editingRule = null;
        current = SectionId.RULES;
        rebuild();
    }

    public void openLibraryPicker(Consumer<String> picker) {
        PreviewPlayer.stop();
        libraryPicker = picker;
        current = SectionId.LIBRARY;
        rebuild();
    }

    public void pickAndReturn(String trackId) {
        Consumer<String> picker = libraryPicker;
        libraryPicker = null;
        if (picker != null) picker.accept(trackId);
        current = SectionId.EDITOR;
        rebuild();
    }

    public void cancelPicker() {
        libraryPicker = null;
        current = SectionId.EDITOR;
        rebuild();
    }

    // ===== 生命周期 =====

    @Override
    protected void init() {
        contentX = navX + navW + 8;
        contentW = Math.max(120, width - contentX - 8);
        contentH = Math.max(60, height - contentY - 34);

        SectionId[] order = {SectionId.OVERVIEW, SectionId.RULES, SectionId.LIBRARY, SectionId.SETTINGS,
                SectionId.JSON};
        for (int i = 0; i < order.length; i++) {
            SectionId id = order[i];
            boolean active = id == current || (id == SectionId.RULES && current == SectionId.EDITOR)
                    || (id == SectionId.LIBRARY && current == SectionId.LIBRARY && libraryPicker != null);
            Button button = Button.builder(
                            Component.translatable(NAV_KEYS[i]).withStyle(active ? ChatFormatting.AQUA : ChatFormatting.GRAY),
                            click -> {
                                if (id == SectionId.RULES && current == SectionId.EDITOR) {
                                    exitEditor();
                                } else if (libraryPicker != null && id != SectionId.LIBRARY) {
                                    cancelPicker();
                                } else {
                                    switchTo(id);
                                }
                            })
                    .bounds(navX, navY + i * 24, navW, 20).build();
            if (active) button.active = false;
            addRenderableWidget(button);
        }
        Button done = Button.builder(Component.translatable("z80z_adaptive_soundscape.ui.nav.done"), button -> onClose())
                .bounds(navX, height - 26, navW, 20).build();
        addRenderableWidget(done);

        section(current).build();
    }

    @Override
    public void tick() {
        section(current).tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderHeader(graphics);
        Ui.navColumn(graphics, navX - 4, navY - 6, navW + 8, height - navY - 2);
        int navIndex = current == SectionId.EDITOR ? 1 : current == SectionId.LIBRARY ? 2
                : current == SectionId.SETTINGS ? 3 : current == SectionId.JSON ? 4 : 0;
        Ui.accentBar(graphics, navX - 4, navY + navIndex * 24, 20);
        section(current).render(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        section(current).renderLate(graphics, mouseX, mouseY);
        Ui.drawStatus(graphics, font, status, statusError, contentX + contentW / 2, height - 22);
    }

    private void renderHeader(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("z80z_adaptive_soundscape.ui.title"),
                navX - 4, 8, Ui.TITLE_COLOR, false);
        MusicConfig config = ConfigManager.get();
        String playing = MusicController.INSTANCE.currentRuleName();
        String right = net.minecraft.client.resources.language.I18n.get(config.enabled
                        ? "z80z_adaptive_soundscape.ui.status.running"
                        : "z80z_adaptive_soundscape.ui.status.stopped")
                + (playing.isBlank() ? "" : "  ♫ " + playing);
        int x = width - font.width(right) - 8;
        graphics.drawString(font, right, x, 8, config.enabled ? Ui.OK_COLOR : 0xFF999999, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (section(current).mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (section(current).mouseDragged(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (section(current).mouseReleased(mouseX, mouseY, button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (section(current).mouseScrolled(mouseX, mouseY, delta)) return true;
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (libraryPicker != null) {
            cancelPicker();
            return;
        }
        if (current == SectionId.EDITOR) {
            exitEditor();
            return;
        }
        PreviewPlayer.stop();
        ConfigManager.save();
        CustomMusicPack.saveLibraryVolumes();
        if (CustomMusicPack.consumeOggReloadRequired()) {
            minecraft.reloadResourcePacks();
            MusicController.INSTANCE.forceRefresh();
        }
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Section section(SectionId id) {
        return sections.computeIfAbsent(id, key -> {
            Section created = switch (key) {
                case OVERVIEW -> new OverviewSection();
                case RULES -> new RulesSection();
                case EDITOR -> new RuleEditorSection();
                case LIBRARY -> new LibrarySection();
                case SETTINGS -> new SettingsSection();
                case JSON -> new JsonSection();
            };
            created.attach(this);
            return created;
        });
    }
}
