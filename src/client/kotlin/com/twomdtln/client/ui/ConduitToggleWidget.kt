package com.twomdtln.client.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.Text

class ConduitToggleWidget(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    private val stateProvider: () -> Boolean,
    private val onToggle: () -> Unit
) : ClickableWidget(x, y, width, height, Text.empty()) {
    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        if (!active) {
            val knobSize = height - 6
            val knobY = y + 3

            context.fill(x, y, x + width, y + height, 0xFF1F1A27.toInt())
            context.drawStrokedRectangle(x, y, width, height, 0xFF52455F.toInt())
            context.fill(x + 3, knobY, x + 3 + knobSize, knobY + knobSize, 0xFF8A7D98.toInt())
            context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                Text.literal("WIP"),
                x + width / 2,
                y + (height - 8) / 2 + 1,
                0xFFD1C4DE.toInt()
            )
            return
        }

        val enabled = stateProvider()
        val hovered = isHovered

        val background = when {
            enabled && hovered -> 0xFF8F58F8.toInt()
            enabled -> 0xFF7440DA.toInt()
            hovered -> 0xFF3A294D.toInt()
            else -> 0xFF251A32.toInt()
        }

        val border = if (enabled) 0xFFD8C2FF.toInt() else 0xFF6F5A86.toInt()
        val knobColor = 0xFFF7F0FF.toInt()
        val knobSize = height - 6
        val knobX = if (enabled) x + width - knobSize - 3 else x + 3
        val knobY = y + 3
        val label = if (enabled) Text.literal("ON") else Text.literal("OFF")
        val labelColor = if (enabled) 0xFFF9F4FF.toInt() else 0xFFB8A7CC.toInt()

        context.fill(x, y, x + width, y + height, background)
        context.drawStrokedRectangle(x, y, width, height, border)
        context.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, knobColor)
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            label,
            x + width / 2,
            y + (height - 8) / 2 + 1,
            labelColor
        )
    }

    override fun onClick(click: net.minecraft.client.gui.Click, doubleClick: Boolean) {
        onToggle()
        playDownSound(MinecraftClient.getInstance().soundManager)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun playDownSound(soundManager: SoundManager) {
        ClickableWidget.playClickSound(soundManager)
    }
}
