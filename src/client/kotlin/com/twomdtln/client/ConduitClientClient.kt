package com.twomdtln.client

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding.Category
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW

object ConduitClientClient : ClientModInitializer {
	private lateinit var openMenuKeyBinding: KeyBinding
	private lateinit var freecamKeyBinding: KeyBinding
    private val conduitCategory = Category.create(Identifier.of("conduit_client", "shortcuts"))

	override fun onInitializeClient() {
        ConduitClientFeatures.initialize()
        ConduitEspRenderer.initialize()
        ConduitHudRenderer.initialize()

		openMenuKeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.conduit-client.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                conduitCategory
            )
        )

		freecamKeyBinding = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "key.conduit-client.toggle_freecam",
                InputUtil.Type.KEYSYM,
                ConduitConfig.freecamKeyCode,
                conduitCategory
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            ConduitClientFeatures.tick(client)
            ConduitEspRenderer.tick(client)

            while (openMenuKeyBinding.wasPressed()) {
                toggleMenu(client)
            }

            while (freecamKeyBinding.wasPressed()) {
                ConduitClientFeatures.toggleFreecam(client)
            }
        })
	}

    fun getFreecamKeyBinding(): KeyBinding = freecamKeyBinding

    fun updateFreecamKeyBinding(keyCode: Int) {
        freecamKeyBinding.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(keyCode))
        KeyBinding.updateKeysByCode()
    }

    private fun toggleMenu(client: MinecraftClient) {
        val currentScreen = client.currentScreen

        when {
            currentScreen is ConduitMenuScreen -> client.setScreen(null)
            currentScreen == null -> client.setScreen(ConduitMenuScreen())
        }
    }
}
