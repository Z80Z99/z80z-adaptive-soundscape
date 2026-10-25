package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.client.gui.hub.HubScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.lwjgl.system.MemoryUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 窗口文件拖入导入：任意时刻把 .ogg/.mp3/.wav 拖到游戏窗口即入音乐库；
 * 其他文件委托给原版回调（保留拖 zip 加载资源包行为）。
 */
public final class DragDropImporter {
    private static boolean registered;
    private static GLFWDropCallbackI previous;

    private DragDropImporter() {
    }

    public static void tryRegister() {
        if (registered) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) return;
        long window = minecraft.getWindow().getWindow();
        previous = GLFW.glfwSetDropCallback(window, DragDropImporter::onDrop);
        registered = true;
    }

    private static void onDrop(long window, int count, long names) {
        List<Path> files = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long pointer = MemoryUtil.memGetAddress(names + (long) i * Long.BYTES);
            String path = MemoryUtil.memUTF8(pointer);
            if (path != null) files.add(Paths.get(path));
        }
        List<Path> audio = new ArrayList<>();
        boolean otherFiles = false;
        for (Path file : files) {
            if (CustomMusicPack.isSupportedFileName(file.getFileName().toString())) {
                audio.add(file);
            } else {
                otherFiles = true;
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!audio.isEmpty()) {
            String result = CustomMusicPack.importFiles(audio);
            MusicOverlay.announce(result);
            // 配置界面打开时由界面在退出时统一重载，避免当场打断声音
            boolean hubOpen = minecraft != null && minecraft.screen instanceof HubScreen;
            if (!hubOpen && minecraft != null && CustomMusicPack.consumeOggReloadRequired()) {
                minecraft.reloadResourcePacks();
            }
            MusicController.INSTANCE.forceRefresh();
        } else if (otherFiles) {
            MusicOverlay.announce(net.minecraft.client.resources.language.I18n
                    .get("message.z80z_adaptive_soundscape.drop.unsupported"));
        }
        if (audio.isEmpty() && previous != null) {
            previous.invoke(window, count, names);
        }
    }
}
