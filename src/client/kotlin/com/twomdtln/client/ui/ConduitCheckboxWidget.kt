package com.twomdtln.client.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.sound.SoundManager
import net.minecraft.text.Text

class ConduitCheckboxWidget(
    x: Int,
    y: Int,
    size: Int,
    message: Text,
    private val checkedProvider: () -> Boolean,
    private val onPress: () -> Unit
) : ClickableWidget(x, y, size, size, message) {
    override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val checked = checkedProvider()
        val hovered = isHovered

        val background = when {
            !active -> 0xFF211A28.toInt()
            checked && hovered -> 0xFF6D39AF.toInt()
            checked -> 0xFF552C8B.toInt()
            hovered -> 0xFF3A294D.toInt()
            else -> 0xFF251A32.toInt()
        }
        val border = when {
            !active -> 0xFF52455F.toInt()
            checked -> 0xFFD8C2FF.toInt()
            else -> 0xFF6F5A86.toInt()
        }

        context.fill(x, y, x + width, y + height, background)
        context.drawStrokedRectangle(x, y, width, height, border)

        if (checked) {
            context.drawCenteredTextWithShadow(
                MinecraftClient.getInstance().textRenderer,
                Text.literal("x"),
                x + width / 2,
                y + (height - 8) / 2,
                0xFFF7F0FF.toInt()
            )
        }
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
