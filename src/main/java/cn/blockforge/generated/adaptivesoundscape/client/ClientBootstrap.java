package cn.blockforge.generated.adaptivesoundscape.client;

import cn.blockforge.generated.adaptivesoundscape.GeneratedMod;
import cn.blockforge.generated.adaptivesoundscape.client.gui.hub.HubScreen;
import cn.blockforge.generated.adaptivesoundscape.config.ConfigManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public final class ClientBootstrap {
    private static final KeyMapping OPEN_CONFIG = new KeyMapping(
            "key.z80z_adaptive_soundscape.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.z80z_adaptive_soundscape"
    );

    private ClientBootstrap() {
    }

    public static void initialize() {
        ConfigManager.migrateLegacyDirectory();
        ConfigManager.load();
        CustomMusicPack.scan();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientBootstrap::registerKeys);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientBootstrap::registerOverlays);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(CustomMusicPack::onAddPackFinders);
        MinecraftForge.EVENT_BUS.addListener(ClientBootstrap::clientTick);
        MinecraftForge.registerConfigScreen((minecraft, parent) -> new HubScreen(parent));
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("music_transition", MusicOverlay::render);
        event.registerAboveAll("music_notice", MusicOverlay::renderNotice);
    }

    private static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        DragDropImporter.tryRegister();
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_CONFIG.consumeClick()) minecraft.setScreen(new HubScreen(minecraft.screen));
        PreviewPlayer.tick(minecraft);
        MusicController.INSTANCE.tick(minecraft);
        MusicController.INSTANCE.tickVisual();
    }
}
