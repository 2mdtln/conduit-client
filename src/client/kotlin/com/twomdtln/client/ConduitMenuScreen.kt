package com.twomdtln.client

import com.twomdtln.client.ui.ConduitIconButton
import com.twomdtln.client.ui.ConduitTextButton
import com.twomdtln.client.ui.ConduitToggleWidget
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import kotlin.math.roundToInt

class ConduitMenuScreen(private val parent: Screen? = null) : Screen(Text.literal("Conduit Client")) {
    private lateinit var fullBrightToggle: ConduitToggleWidget
    private lateinit var espToggle: ConduitToggleWidget
    private lateinit var fpsToggle: ConduitToggleWidget
    private lateinit var pingToggle: ConduitToggleWidget
    private lateinit var espProfileButton: ConduitTextButton
    private lateinit var espSettingsButton: ConduitIconButton
    private var featureScroll = 0

    private val modVersion = FabricLoader.getInstance()
        .getModContainer("conduit-client")
        .map { it.metadata.version.friendlyString }
        .orElse("dev")

    override fun init() {
        fullBrightToggle = addDrawableChild(
            ConduitToggleWidget(0, 0, 58, 18, { ConduitClientFeatures.isFullBrightEnabled() }) {
                ConduitClientFeatures.toggleFullBright(client!!)
            }
        )

        espToggle = addDrawableChild(
            ConduitToggleWidget(0, 0, 58, 18, { ConduitClientFeatures.isEspEnabled() }) {
                ConduitClientFeatures.toggleEsp()
            }
        )

        fpsToggle = addDrawableChild(
            ConduitToggleWidget(0, 0, 58, 18, { ConduitClientFeatures.isShowFpsEnabled() }) {
                ConduitClientFeatures.toggleShowFps()
            }
        )

        pingToggle = addDrawableChild(
            ConduitToggleWidget(0, 0, 58, 18, { false }) {}
        )

        espProfileButton = addDrawableChild(
            ConduitTextButton(0, 0, 138, 20, Text.literal(ConduitConfig.selectedEspProfile)) {
                ConduitConfig.cycleEspProfile()
                refreshEspProfileText()
            }
        )

        addDrawableChild(
            ConduitIconButton(panelLeft() + panelWidth() - 30, panelTop() + 10, 18, Text.literal("X")) { close() }
        )

        espSettingsButton = addDrawableChild(
            ConduitIconButton(0, 0, 16, Text.literal("⚙")) {
                client?.setScreen(EspSettingsScreen(this))
            }
        )

        updateFeatureLayout()
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderOverlayBackground(context)
        updateFeatureLayout()

        val panelLeft = panelLeft()
        val panelTop = panelTop()
        val panelWidth = panelWidth()
        val panelHeight = panelHeight()
        val contentLeft = panelLeft + 24
        val viewportTop = panelTop + TOP_BAR_HEIGHT
        val viewportBottom = panelTop + panelHeight - 28

        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xD1110B1D.toInt())
        context.fillGradient(panelLeft, panelTop, panelLeft + panelWidth, panelTop + TOP_BAR_HEIGHT, 0xFF2C1344.toInt(), 0xFF1A102A.toInt())
        context.drawStrokedRectangle(panelLeft, panelTop, panelWidth, panelHeight, 0xBF9C74FF.toInt())
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelTop + 14, 0xFFF8F0FF.toInt())

        renderFeatureCard(context, contentLeft, viewportTop - featureScroll, "ESP", span = 2)
        renderFeatureCard(
            context,
            contentLeft + (SQUARE_SIZE + SQUARE_GAP) * 2,
            viewportTop - featureScroll,
            "Full Bright"
        )
        renderFeatureCard(
            context,
            contentLeft + (SQUARE_SIZE + SQUARE_GAP) * 3,
            viewportTop - featureScroll,
            "Show FPS"
        )
        renderFeatureCard(
            context,
            contentLeft,
            viewportTop - featureScroll + SQUARE_SIZE + SQUARE_GAP,
            "Show Ping",
            subtitle = "WIP"
        )

        context.drawTextWithShadow(
            textRenderer,
            Text.literal("Conduit Client v$modVersion"),
            panelLeft + panelWidth - 146,
            panelTop + panelHeight - 16,
            0xFF9D7CC5.toInt()
        )

        super.render(context, mouseX, mouseY, deltaTicks)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val maxScroll = maxFeatureScroll()
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }

        featureScroll = (featureScroll - (verticalAmount * 18.0).roundToInt()).coerceIn(0, maxScroll)
        updateFeatureLayout()
        return true
    }

    override fun shouldPause(): Boolean = false

    override fun shouldCloseOnEsc(): Boolean = true

    override fun tick() {
        refreshEspProfileText()
        updateFeatureLayout()
    }

    override fun close() {
        client?.setScreen(parent)
    }

    private fun updateFeatureLayout() {
        val panelLeft = panelLeft()
        val panelTop = panelTop()
        val contentLeft = panelLeft + 24
        val viewportTop = panelTop + TOP_BAR_HEIGHT
        val viewportBottom = panelTop + panelHeight() - 28

        val espY = viewportTop - featureScroll
        val secondRowY = viewportTop - featureScroll + SQUARE_SIZE + SQUARE_GAP
        val espCardWidth = SQUARE_SIZE * 2 + SQUARE_GAP

        espToggle.x = contentLeft + espCardWidth - 74
        espToggle.y = espY + SQUARE_SIZE - 25
        espProfileButton.x = contentLeft + 12
        espProfileButton.y = espY + 32
        espSettingsButton.x = contentLeft + espCardWidth - 26
        espSettingsButton.y = espY + 12

        val fullBrightX = contentLeft + espCardWidth + SQUARE_GAP
        val showFpsX = fullBrightX + SQUARE_SIZE + SQUARE_GAP

        fullBrightToggle.x = fullBrightX + SQUARE_SIZE - 74
        fullBrightToggle.y = espY + SQUARE_SIZE - 25
        fpsToggle.x = showFpsX + SQUARE_SIZE - 74
        fpsToggle.y = espY + SQUARE_SIZE - 25
        pingToggle.x = contentLeft + SQUARE_SIZE - 74
        pingToggle.y = secondRowY + SQUARE_SIZE - 25

        setFeatureVisibility(espToggle, espY, SQUARE_SIZE, viewportTop, viewportBottom)
        setFeatureVisibility(espProfileButton, espY, SQUARE_SIZE, viewportTop, viewportBottom)
        setFeatureVisibility(espSettingsButton, espY, SQUARE_SIZE, viewportTop, viewportBottom)
        setFeatureVisibility(fullBrightToggle, espY, SQUARE_SIZE, viewportTop, viewportBottom)
        setFeatureVisibility(fpsToggle, espY, SQUARE_SIZE, viewportTop, viewportBottom)
        setFeatureVisibility(pingToggle, secondRowY, SQUARE_SIZE, viewportTop, viewportBottom)
        pingToggle.active = false
    }

    private fun setFeatureVisibility(widget: net.minecraft.client.gui.widget.ClickableWidget, y: Int, height: Int, viewportTop: Int, viewportBottom: Int) {
        val visible = y + height >= viewportTop && y <= viewportBottom
        widget.visible = visible
        widget.active = visible
    }

    private fun renderFeatureCard(
        context: DrawContext,
        x: Int,
        y: Int,
        title: String,
        span: Int = 1,
        subtitle: String? = null
    ) {
        val viewportTop = panelTop() + TOP_BAR_HEIGHT
        val viewportBottom = panelTop() + panelHeight() - 28

        if (y + SQUARE_SIZE < viewportTop || y > viewportBottom) {
            return
        }

        val width = SQUARE_SIZE * span + SQUARE_GAP * (span - 1)
        context.fill(x, y, x + width, y + SQUARE_SIZE, 0x4A26173F)
        context.drawStrokedRectangle(x, y, width, SQUARE_SIZE, 0x8A714CB8.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal(title), x + 16, y + 16, 0xFFFFFFFF.toInt())
        if (subtitle != null) {
            context.drawTextWithShadow(textRenderer, Text.literal(subtitle), x + 16, y + 32, 0xFFB58AE9.toInt())
        }
    }

    private fun renderOverlayBackground(context: DrawContext) {
        try {
            context.applyBlur()
        } catch (_: IllegalStateException) {
        }

        context.fillGradient(0, 0, width, height, 0x40101824, 0x7A080B10)
    }

    private fun refreshEspProfileText() {
        espProfileButton.message = Text.literal(ConduitConfig.selectedEspProfile)
    }

    private fun panelWidth(): Int = 48 + SQUARE_SIZE * 4 + SQUARE_GAP * 3

    private fun panelHeight(): Int = TOP_BAR_HEIGHT + SQUARE_SIZE * 2 + SQUARE_GAP + 32

    private fun panelLeft(): Int = width / 2 - panelWidth() / 2

    private fun panelTop(): Int = height / 2 - panelHeight() / 2

    private fun maxFeatureScroll(): Int {
        val viewportHeight = panelHeight() - TOP_BAR_HEIGHT - 28
        return (contentHeight() - viewportHeight).coerceAtLeast(0)
    }

    private fun contentHeight(): Int {
        return SQUARE_SIZE * 2 + SQUARE_GAP
    }

    companion object {
        private const val SQUARE_SIZE = 86
        private const val SQUARE_GAP = 12
        private const val TOP_BAR_HEIGHT = 36
    }
}
