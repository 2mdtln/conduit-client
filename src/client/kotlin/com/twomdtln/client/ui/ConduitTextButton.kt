package com.twomdtln.client.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.Text

class ConduitTextButton(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    message: Text,
    private val onPress: () -> Unit
) : ClickableWidget(x, y, width, height, message) {
    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val hovered = isHovered
        val enabled = active

        val background = when {
            !enabled -> 0xFF221A2B.toInt()
            hovered -> 0xFF5A2A89.toInt()
            else -> 0xFF301B46.toInt()
        }

        val border = when {
            !enabled -> 0xFF4E415C.toInt()
            hovered -> 0xFFE0CCFF.toInt()
            else -> 0xFF8D6DB3.toInt()
        }

        val textColor = if (enabled) 0xFFF7EFFF.toInt() else 0xFF9A8AAC.toInt()

        context.fill(x, y, x + width, y + height, background)
        context.drawStrokedRectangle(x, y, width, height, border)
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            message,
            x + width / 2,
            y + (height - 8) / 2,
            textColor
        )
    }

    override fun onClick(click: net.minecraft.client.gui.Click, doubleClick: Boolean) {
        if (!active) {
            return
        }

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
