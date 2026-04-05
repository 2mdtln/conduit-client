package com.twomdtln.client

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object ConduitHudRenderer {
    fun initialize() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("conduit-client", "overlay")) { context, _ ->
            val client = MinecraftClient.getInstance()
            var nextHudY = 6

            if (ConduitConfig.showFpsEnabled) {
                context.drawTextWithShadow(
                    client.textRenderer,
                    Text.literal("${client.currentFps} FPS"),
                    6,
                    nextHudY,
                    0xFFF5EDFF.toInt()
                )
                nextHudY += 18
            }

            if (ConduitConfig.showPingEnabled) {
                val ping = resolvePing(client)
                context.drawTextWithShadow(
                    client.textRenderer,
                    Text.literal("$ping ms"),
                    6,
                    nextHudY,
                    0xFFF5EDFF.toInt()
                )
                nextHudY += 18
            }

            renderAimedEspBlock(context, client, nextHudY)
            renderEspSummary(context, client)
        }
    }

    private fun renderAimedEspBlock(context: DrawContext, client: MinecraftClient, topY: Int) {
        if (!ConduitConfig.espEnabled || client.world == null || client.player == null) {
            return
        }

        val pos = ConduitEspRenderer.getAimedTarget(client) ?: return
        val state = client.world?.getBlockState(pos) ?: return
        val block = state.block
        val stack = resolveBlockStack(block)
        val name = stack.name.copy().append(Text.literal("  ${pos.x} ${pos.y} ${pos.z}"))
        val boxWidth = maxOf(150, client.textRenderer.getWidth(name) + 34)
        val boxHeight = 22
        val x = 8
        val y = topY

        context.fill(x, y, x + boxWidth, y + boxHeight, 0xD2140D22.toInt())
        context.drawStrokedRectangle(x, y, boxWidth, boxHeight, 0xAA8D61D9.toInt())
        context.drawItem(stack, x + 4, y + 3)
        context.drawTextWithShadow(client.textRenderer, name, x + 24, y + 7, 0xFFF6EEFF.toInt())
    }

    private fun resolvePing(client: MinecraftClient): Int {
        val uuid = client.player?.uuid ?: return 0
        return client.networkHandler?.getPlayerListEntry(uuid)?.latency ?: 0
    }

    private fun renderEspSummary(context: DrawContext, client: MinecraftClient) {
        val summaries = ConduitEspRenderer.getTargetSummaries(client)

        if (summaries.isEmpty()) {
            return
        }

        val rowHeight = 18
        val padding = 6
        val maxWidth = summaries.maxOf { summary ->
            client.textRenderer.getWidth(Text.literal("${summary.count}x ").append(summary.stack.name)) + 30
        }
        val totalHeight = summaries.size * rowHeight + 4
        val x = 8
        val y = client.window.scaledHeight - totalHeight - padding

        context.fill(x, y, x + maxWidth, y + totalHeight, 0xB8140D22.toInt())
        context.drawStrokedRectangle(x, y, maxWidth, totalHeight, 0x8A8D61D9.toInt())

        summaries.forEachIndexed { index, summary ->
            val rowY = y + 4 + index * rowHeight
            val text = Text.literal("${summary.count}x ").append(summary.stack.name)
            context.drawItem(summary.stack, x + 4, rowY - 1)
            context.drawTextWithShadow(client.textRenderer, text, x + 24, rowY + 3, 0xFFF6EEFF.toInt())
        }
    }

    private fun resolveBlockStack(block: Block): ItemStack {
        return ItemStack(block)
    }
}
