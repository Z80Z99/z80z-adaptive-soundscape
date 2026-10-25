package cn.blockforge.generated.adaptivesoundscape.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public final class Ui {
    public static final int PANEL_BG = 0xC00E1424;
    public static final int PANEL_BORDER = 0xFF6A58B8;
    public static final int TITLE_COLOR = 0xFFE9D8FF;
    public static final int TEXT_COLOR = 0xFFE8E8F0;
    public static final int MUTED_COLOR = 0xFFB8B8D8;
    public static final int ACCENT_COLOR = 0xFFB79CFF;
    public static final int STATUS_INFO = 0xFFFFD978;
    public static final int STATUS_ERROR = 0xFFFF7777;
    public static final int NAV_BG = 0xE60C0A18;
    public static final int NAV_BORDER = 0xFF3A3070;
    public static final int CARD_BG = 0xB0161330;
    public static final int CARD_BORDER = 0xFF4A3E86;
    public static final int OK_COLOR = 0xFF8CE8A0;

    private Ui() {
    }

    public static int contentWidth(int screenWidth, int desired) {
        return Math.max(120, Math.min(desired, screenWidth - 16));
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        graphics.fill(x, y, x + width, y + 1, PANEL_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        graphics.fill(x, y, x + 1, y + height, PANEL_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
    }

    public static void title(GuiGraphics graphics, Font font, String text, int centerX, int y) {
        graphics.drawCenteredString(font, text, centerX, y, TITLE_COLOR);
    }

    public static String trunc(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
    }

    public static void drawStatus(GuiGraphics graphics, Font font, String text, boolean error, int centerX, int y) {
        if (text == null || text.isBlank()) return;
        graphics.drawCenteredString(font, text, centerX, y, error ? STATUS_ERROR : STATUS_INFO);
    }

    /**
     * 流式布局：给定每项宽度，返回每项 {x, y, w}（相对坐标），行内居中、超宽自动换行。
     */
    public static List<int[]> flow(int maxWidth, int gap, int rowHeight, int... widths) {
        List<int[]> placed = new ArrayList<>();
        int index = 0;
        int y = 0;
        while (index < widths.length) {
            int rowWidth = 0;
            int count = 0;
            while (index + count < widths.length
                    && (count == 0 || rowWidth + gap + widths[index + count] <= maxWidth)) {
                rowWidth += (count == 0 ? 0 : gap) + widths[index + count];
                count++;
            }
            int x = (maxWidth - rowWidth) / 2;
            for (int i = 0; i < count; i++) {
                placed.add(new int[]{x, y, widths[index + i]});
                x += widths[index + i] + gap;
            }
            index += count;
            y += rowHeight;
        }
        return placed;
    }

    public static Button toggle(String name, BooleanSupplier state, Runnable onToggle) {
        return Button.builder(toggleText(name, state.getAsBoolean()), button -> {
            onToggle.run();
            button.setMessage(toggleText(name, state.getAsBoolean()));
        }).bounds(0, 0, 10, 20).build();
    }

    public static Component toggleText(String name, boolean enabled) {
        return Component.literal(name + "："
                + net.minecraft.client.resources.language.I18n.get(enabled
                ? "z80z_adaptive_soundscape.ui.common.on"
                : "z80z_adaptive_soundscape.ui.common.off"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    public static void navColumn(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, NAV_BG);
        graphics.fill(x + width, y, x + width + 1, y + height, NAV_BORDER);
    }

    public static void accentBar(GuiGraphics graphics, int x, int y, int height) {
        graphics.fill(x, y, x + 2, y + height, ACCENT_COLOR);
    }

    public static void card(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, CARD_BG);
        graphics.fill(x, y, x + width, y + 1, CARD_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, CARD_BORDER);
        graphics.fill(x, y, x + 1, y + height, CARD_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, CARD_BORDER);
    }

    /** 小徽标：带底色的短文本，返回其宽度便于排布。 */
    public static int badge(GuiGraphics graphics, Font font, String text, int x, int y, int background) {
        int width = font.width(text) + 6;
        graphics.fill(x, y, x + width, y + 10, background);
        graphics.drawString(font, text, x + 3, y + 1, TEXT_COLOR, false);
        return width;
    }

    public static Predicate<String> numberFilter() {
        return value -> value.isEmpty() || value.matches("-?\\d{0,9}(\\.\\d{0,4})?");
    }

    public static void scrollbar(GuiGraphics graphics, int x, int y, int height, int total, int visible, int scroll) {
        if (total <= visible || height <= 4) return;
        graphics.fill(x, y, x + 3, y + height, 0x30FFFFFF);
        int thumbHeight = Math.max(10, height * visible / total);
        int thumbY = y + (height - thumbHeight) * scroll / Math.max(1, total - visible);
        graphics.fill(x, thumbY, x + 3, thumbY + thumbHeight, ACCENT_COLOR);
    }
}
