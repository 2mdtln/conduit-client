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

    fun tick(client: MinecraftClient) {
        refreshLightmapIfReady(client)
    }

    private fun refreshLightmapIfReady(client: MinecraftClient) {
        if (!pendingLightmapRefresh || client.world == null || client.gameRenderer == null) {
            return
        }

        client.gameRenderer.lightmapTextureManager.tick()
        pendingLightmapRefresh = false
    }
}
