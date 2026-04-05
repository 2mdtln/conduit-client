package com.twomdtln.client.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.Text

class ConduitIconButton(
    x: Int,
    y: Int,
    size: Int,
    private val label: Text,
    private val onPress: () -> Unit
) : ClickableWidget(x, y, size, size, label) {
    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val hovered = isHovered
        val background = if (hovered) 0xFF5A2A89.toInt() else 0xFF301B46.toInt()
        val border = if (hovered) 0xFFE0CCFF.toInt() else 0xFF8D6DB3.toInt()

        context.fill(x, y, x + width, y + height, background)
        context.drawStrokedRectangle(x, y, width, height, border)
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            label,
            x + width / 2,
            y + (height - 8) / 2,
            0xFFF7EFFF.toInt()
        )
    }

    override fun onClick(click: net.minecraft.client.gui.Click, doubleClick: Boolean) {
        onPress()
        playDownSound(MinecraftClient.getInstance().soundManager)
    }

    override fun appendClickableNarrations(builder: NarrationMessageBuilder) {
        appendDefaultNarrations(builder)
    }

    override fun playDownSound(soundManager: SoundManager) {
        ClickableWidget.playClickSound(soundManager)
    }
}
