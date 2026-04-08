package com.twomdtln.client

import net.minecraft.client.MinecraftClient

object ConduitClientFeatures {
    private var pendingLightmapRefresh = false

    fun initialize() {
        ConduitConfig.load()
        pendingLightmapRefresh = ConduitConfig.fullBrightEnabled
    }

    fun isFullBrightEnabled(): Boolean = ConduitConfig.fullBrightEnabled

    fun isEspEnabled(): Boolean = ConduitConfig.espEnabled

    fun isShowFpsEnabled(): Boolean = ConduitConfig.showFpsEnabled

    fun isShowPingEnabled(): Boolean = false

    fun isAutoTotemEnabled(): Boolean = ConduitConfig.autoTotemEnabled

    fun isAutoTotemInstantEnabled(): Boolean = ConduitConfig.autoTotemInstantEnabled

    fun isFreecamEnabled(): Boolean = ConduitFreecam.isEnabled()

    fun toggleFullBright(client: MinecraftClient): Boolean {
        ConduitConfig.setFullBrightEnabled(!ConduitConfig.fullBrightEnabled)
        pendingLightmapRefresh = true
        refreshLightmapIfReady(client)
        return ConduitConfig.fullBrightEnabled
    }

    fun toggleEsp(): Boolean {
        ConduitConfig.setEspEnabled(!ConduitConfig.espEnabled)
        return ConduitConfig.espEnabled
    }

    fun toggleShowFps(): Boolean {
        ConduitConfig.setShowFpsEnabled(!ConduitConfig.showFpsEnabled)
        return ConduitConfig.showFpsEnabled
    }

    fun toggleShowPing(): Boolean {
        ConduitConfig.setShowPingEnabled(false)
        return false
    }

    fun toggleAutoTotem(): Boolean {
        ConduitConfig.setAutoTotemEnabled(!ConduitConfig.autoTotemEnabled)
        if (!ConduitConfig.autoTotemEnabled) {
            ConduitAutoTotem.reset()
        }
        return ConduitConfig.autoTotemEnabled
    }

    fun toggleAutoTotemInstant(): Boolean {
        ConduitConfig.setAutoTotemInstantEnabled(!ConduitConfig.autoTotemInstantEnabled)
        ConduitAutoTotem.reset()
        return ConduitConfig.autoTotemInstantEnabled
    }

    fun toggleFreecam(client: MinecraftClient): Boolean {
        val enabled = ConduitFreecam.toggle(client)
        ConduitConfig.setFreecamEnabled(enabled)
        return enabled
    }

    fun setFreecamKeyCode(keyCode: Int) {
        ConduitConfig.setFreecamKeyCode(keyCode)
        ConduitClientClient.updateFreecamKeyBinding(keyCode)
    }

    fun tick(client: MinecraftClient) {
        refreshLightmapIfReady(client)
        ConduitAutoTotem.tick(client)
        if (ConduitConfig.freecamEnabled && !ConduitFreecam.isEnabled()) {
            ConduitFreecam.enable(client)
        }
        if (!ConduitConfig.freecamEnabled && ConduitFreecam.isEnabled()) {
            ConduitFreecam.disable(client)
        }
        ConduitFreecam.tick(client)
    }

    private fun refreshLightmapIfReady(client: MinecraftClient) {
        if (!pendingLightmapRefresh || client.world == null || client.gameRenderer == null) {
            return
        }

        client.gameRenderer.lightmapTextureManager.tick()
        pendingLightmapRefresh = false
    }
}
