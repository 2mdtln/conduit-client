package com.twomdtln.client

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text
import com.twomdtln.client.ui.ConduitIconButton
import com.twomdtln.client.ui.ConduitToggleWidget

class ConduitMenuScreen(private val parent: Screen? = null) : Screen(Text.literal("Conduit Client")) {
    private var fullBrightToggle: ConduitToggleWidget? = null
    private var espToggle: ConduitToggleWidget? = null
    private var espProfileButton: ButtonWidget? = null

    private val modVersion = FabricLoader.getInstance()
        .getModContainer("conduit-client")
        .map { it.metadata.version.friendlyString }
        .orElse("dev")

    override fun init() {
        val panelWidth = 320
        val panelHeight = 226
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2
        val tileSize = 118
        val tileGap = 18
        val tileTop = panelTop + 68
        val fullBrightTileLeft = panelLeft + 24
        val espTileLeft = fullBrightTileLeft + tileSize + tileGap

        fullBrightToggle = addDrawableChild(
            ConduitToggleWidget(
                fullBrightTileLeft + tileSize / 2 - 29,
                tileTop + 66,
                58,
                18,
                { ConduitClientFeatures.isFullBrightEnabled() }
            ) {
                ConduitClientFeatures.toggleFullBright(client!!)
            }
        )

        espToggle = addDrawableChild(
            ConduitToggleWidget(
                espTileLeft + tileSize / 2 - 29,
                tileTop + 72,
                58,
                18,
                { ConduitClientFeatures.isEspEnabled() }
            ) {
                ConduitClientFeatures.toggleEsp()
            }
        )

        espProfileButton = addDrawableChild(
            ButtonWidget.builder(Text.literal(ConduitConfig.selectedEspProfile)) {
                ConduitConfig.cycleEspProfile()
                refreshEspProfileText()
            }
                .dimensions(espTileLeft + 22, tileTop + 42, tileSize - 44, 16)
                .build()
        )

        addDrawableChild(
            ConduitIconButton(panelLeft + panelWidth - 30, panelTop + 10, 18, Text.literal("X")) { close() }
        )

        addDrawableChild(
            ConduitIconButton(espTileLeft + tileSize - 22, tileTop + 8, 14, Text.literal("⚙")) {
                client?.setScreen(EspSettingsScreen(this))
            }
        )
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderOverlayBackground(context)

        val panelWidth = 320
        val panelHeight = 226
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2
        val tileSize = 118
        val tileGap = 18
        val tileTop = panelTop + 68
        val fullBrightTileLeft = panelLeft + 24
        val espTileLeft = fullBrightTileLeft + tileSize + tileGap
        val footerY = panelTop + panelHeight - 16

        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xD1110B1D.toInt())
        context.fillGradient(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 44, 0xFF2C1344.toInt(), 0xFF1A102A.toInt())
        context.fill(fullBrightTileLeft, tileTop, fullBrightTileLeft + tileSize, tileTop + tileSize, 0x4A26173F)
        context.fill(espTileLeft, tileTop, espTileLeft + tileSize, tileTop + tileSize, 0x4A26173F)
        context.drawStrokedRectangle(panelLeft, panelTop, panelWidth, panelHeight, 0xBF9C74FF.toInt())
        context.drawStrokedRectangle(fullBrightTileLeft, tileTop, tileSize, tileSize, 0x8A714CB8.toInt())
        context.drawStrokedRectangle(espTileLeft, tileTop, tileSize, tileSize, 0x8A714CB8.toInt())
        context.drawHorizontalLine(panelLeft + 1, panelLeft + panelWidth - 2, panelTop + 44, 0xCC5D3191.toInt())
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelTop + 14, 0xFFF8F0FF.toInt())
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Full Bright"), fullBrightTileLeft + tileSize / 2, tileTop + 26, 0xFFFFFFFF.toInt())
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("ESP"), espTileLeft + tileSize / 2, tileTop + 18, 0xFFFFFFFF.toInt())
        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Conduit Client v$modVersion"),
            panelLeft + panelWidth - 146,
            footerY,
            0xFF9D7CC5.toInt()
        )

        super.render(context, mouseX, mouseY, deltaTicks)
    }

    override fun shouldPause(): Boolean = false

    override fun shouldCloseOnEsc(): Boolean = true

    override fun tick() {
        refreshEspProfileText()
    }

    override fun close() {
        client?.setScreen(parent)
    }

    private fun renderOverlayBackground(context: DrawContext) {
        try {
            context.applyBlur()
        } catch (_: IllegalStateException) {
            // Another screen hook or mod already consumed the frame blur.
        }

        context.fillGradient(0, 0, width, height, 0x40101824, 0x7A080B10)
    }

    private fun refreshEspProfileText() {
        espProfileButton?.message = Text.literal(ConduitConfig.selectedEspProfile)
    }
}
