package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class MusicOverlay {
    private static String announcement = "";
    private static int announceTicks;

    private MusicOverlay() {
    }

    public static void announce(String message) {
        announcement = message;
        announceTicks = 100;
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        MusicController controller = MusicController.INSTANCE;
        int ticks = controller.visualTicks();
        if (ticks <= 0 || !ConfigManager.get().visualGlow || Minecraft.getInstance().options.hideGui) return;
        float progress = ticks / 60.0F;
        int alpha = Math.max(0, Math.min(100, (int) (Math.sin(progress * Math.PI) * 100)));
        int color = alpha << 24 | 0x8060FF;
        int edge = 8;
        graphics.fillGradient(0, 0, width, edge, color, 0x008060FF);
        graphics.fillGradient(0, height - edge, width, height, 0x008060FF, color);
        graphics.fill(0, 0, edge, height, color);
        graphics.fill(width - edge, 0, width, height, color);
        if (!controller.currentRuleName().isBlank()) {
            int textAlpha = Math.max(40, Math.min(255, ticks * 6));
            int textColor = textAlpha << 24 | 0xE8DCFF;
            graphics.drawCenteredString(Minecraft.getInstance().font,
                    "♫ " + controller.currentRuleName(), width / 2, height - 58, textColor);
        }
    }

    public static void renderNotice(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        renderAnnouncement(graphics, width, height);
    }

    public static void renderAnnouncement(GuiGraphics graphics, int width, int height) {
        if (announceTicks <= 0 || Minecraft.getInstance().options.hideGui) return;
        announceTicks--;
        float progress = announceTicks / 100.0F;
        int alpha = Math.max(0, Math.min(220, (int) (Math.sin(progress * Math.PI) * 220)));
        graphics.drawCenteredString(Minecraft.getInstance().font, announcement, width / 2, height - 72,
                (alpha << 24) | 0xB79CFF);
    }
}
