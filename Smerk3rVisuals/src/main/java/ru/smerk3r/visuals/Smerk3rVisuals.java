package ru.smerk3r.visuals;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

public final class Smerk3rVisuals implements ClientModInitializer {
    public static final String MOD_ID = "smerk3rvisuals";

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"));

    private KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.smerk3rvisuals.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new VisualsScreen());
                } else if (client.screen instanceof VisualsScreen) {
                    client.setScreen(null);
                }
            }
        });
    }
}
