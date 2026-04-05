package com.twomdtln.client

import com.twomdtln.client.ui.ConduitIconButton
import com.twomdtln.client.ui.ConduitTextButton
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class EspSettingsScreen(private val parent: Screen? = null) : Screen(Text.literal("ESP Settings")) {
    private lateinit var blockInput: TextFieldWidget
    private lateinit var addButton: ConduitTextButton
    private lateinit var profileButton: ConduitTextButton
    private val removeButtons = mutableListOf<ConduitIconButton>()
    private var scrollOffset = 0
    private var suggestions: List<String> = emptyList()

    override fun init() {
        val panelWidth = 360
        val panelHeight = 274
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2

        blockInput = TextFieldWidget(textRenderer, panelLeft + 20, panelTop + 52, 220, 18, Text.literal("Block"))
        blockInput.setMaxLength(64)
        blockInput.setChangedListener { updateSuggestion(it) }
        blockInput.setPlaceholder(Text.literal("minecraft:diamond_ore"))
        addDrawableChild(blockInput)

        addButton = addDrawableChild(
            ConduitTextButton(panelLeft + 248, panelTop + 51, 72, 20, Text.literal("Add")) {
                addSuggestedBlock()
            }
        )

        profileButton = addDrawableChild(
            ConduitTextButton(panelLeft + 78, panelTop + 24, 152, 18, Text.literal(ConduitConfig.selectedEspProfile)) {
                ConduitConfig.cycleEspProfile()
                scrollOffset = 0
                refreshRemoveButtons()
                updateSuggestion(blockInput.text)
                refreshProfileText()
            }
        )

        addDrawableChild(
            ConduitIconButton(panelLeft + panelWidth - 30, panelTop + 10, 18, Text.literal("X")) { close() }
        )

        refreshRemoveButtons()
        updateSuggestion("")
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        renderBackground(context, mouseX, mouseY, deltaTicks)

        val panelWidth = 360
        val panelHeight = 274
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2
        val profile = ConduitConfig.selectedEspProfile
        val blocks = ConduitConfig.getEspBlocks(profile)
        val showSuggestions = blockInput.text.isNotBlank() && suggestions.isNotEmpty()

        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xDE120B20.toInt())
        context.fillGradient(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 36, 0xFF2C1344.toInt(), 0xFF1A102A.toInt())
        context.drawStrokedRectangle(panelLeft, panelTop, panelWidth, panelHeight, 0xBF9C74FF.toInt())
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelTop + 12, 0xFFF8F0FF.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Profile"), panelLeft + 20, panelTop + 29, 0xFFD8C6F1.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Tracked Blocks"), panelLeft + 20, panelTop + 88, 0xFFB884E0.toInt())

        if (blocks.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal("No blocks tracked in this profile."), panelLeft + 20, panelTop + 106, 0xFFBFAFD0.toInt())
        }

        blocks.drop(scrollOffset).take(VISIBLE_ROWS).forEachIndexed { index, blockId ->
            val rowY = panelTop + 104 + index * 18
            context.drawItem(EspBlockCatalog.getDisplayStack(blockId), panelLeft + 20, rowY - 2)
            context.drawTextWithShadow(textRenderer, Text.literal(EspBlockCatalog.getDisplayLabel(blockId)), panelLeft + 40, rowY, 0xFFF1EAFF.toInt())
        }

        if (blocks.size > VISIBLE_ROWS) {
            val end = (scrollOffset + VISIBLE_ROWS).coerceAtMost(blocks.size)
            context.drawTextWithShadow(
                textRenderer,
                Text.literal("${scrollOffset + 1}-$end / ${blocks.size}"),
                panelLeft + 20,
                panelTop + panelHeight - 18,
                0xFFBFAFD0.toInt()
            )
        }

        if (showSuggestions) {
            val dropdownTop = panelTop + 74
            val dropdownHeight = suggestions.take(SUGGESTION_ROWS).size * 18 + 6
            context.fill(panelLeft + 16, dropdownTop, panelLeft + 332, dropdownTop + dropdownHeight, 0xF0181027.toInt())
            context.drawStrokedRectangle(panelLeft + 16, dropdownTop, 316, dropdownHeight, 0x9E714CB8.toInt())

            suggestions.take(SUGGESTION_ROWS).forEachIndexed { index, blockId ->
                val rowY = dropdownTop + 4 + index * 18
                context.drawItem(EspBlockCatalog.getDisplayStack(blockId), panelLeft + 22, rowY - 2)
                context.drawTextWithShadow(textRenderer, Text.literal(EspBlockCatalog.getDisplayLabel(blockId)), panelLeft + 42, rowY, 0xFFE5DAF4.toInt())
            }
        } else if (blockInput.text.isNotBlank()) {
            context.drawTextWithShadow(textRenderer, Text.literal("No block match"), panelLeft + 20, panelTop + 74, 0xFFBFAFD0.toInt())
        }

        super.render(context, mouseX, mouseY, deltaTicks)
    }

    override fun shouldPause(): Boolean = false

    override fun shouldCloseOnEsc(): Boolean = true

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val maxOffset = (ConduitConfig.getEspBlocks().size - VISIBLE_ROWS).coerceAtLeast(0)
        scrollOffset = (scrollOffset - verticalAmount.toInt()).coerceIn(0, maxOffset)
        refreshRemoveButtons()
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.button() == 0 && clickSuggestion(click.x(), click.y())) {
            return true
        }

        return super.mouseClicked(click, doubled)
    }

    override fun close() {
        client?.setScreen(parent)
    }

    private fun addSuggestedBlock() {
        val blockId = resolveBlockId(blockInput.text) ?: suggestions.firstOrNull() ?: return

        if (ConduitConfig.addEspBlock(blockId)) {
            blockInput.setText("")
            scrollOffset = 0
            refreshRemoveButtons()
            updateSuggestion("")
        }
    }

    private fun updateSuggestion(input: String) {
        suggestions = EspBlockCatalog.resolveSuggestions(input, SUGGESTION_ROWS)
        addButton.active = suggestions.isNotEmpty()
    }

    private fun refreshRemoveButtons() {
        removeButtons.forEach(::remove)
        removeButtons.clear()

        val blocks = ConduitConfig.getEspBlocks()
        val visibleBlocks = blocks.drop(scrollOffset).take(VISIBLE_ROWS)

        val panelWidth = 360
        val panelHeight = 274
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2

        visibleBlocks.forEachIndexed { index, blockId ->
            val rowY = panelTop + 104 + index * 18

            removeButtons += addDrawableChild(
                ConduitIconButton(panelLeft + 314, rowY - 2, 14, Text.literal("X")) {
                    ConduitConfig.removeEspBlock(blockId)
                    val maxOffset = (ConduitConfig.getEspBlocks().size - VISIBLE_ROWS).coerceAtLeast(0)
                    scrollOffset = scrollOffset.coerceIn(0, maxOffset)
                    refreshRemoveButtons()
                    updateSuggestion(blockInput.text)
                }
            )
        }
    }

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        try {
            context.applyBlur()
        } catch (_: IllegalStateException) {
        }

        context.fillGradient(0, 0, width, height, 0x40101824, 0x7A080B10)
    }

    private fun resolveBlockId(input: String): String? {
        return EspBlockCatalog.resolveSuggestions(input, 1).firstOrNull()
    }

    private fun clickSuggestion(mouseX: Double, mouseY: Double): Boolean {
        if (blockInput.text.isBlank()) {
            return false
        }

        val panelWidth = 360
        val panelHeight = 274
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2

        suggestions.take(SUGGESTION_ROWS).forEachIndexed { index, blockId ->
            val rowY = panelTop + 78 + index * 18

            if (mouseX in (panelLeft + 16).toDouble()..(panelLeft + 332).toDouble() &&
                mouseY in (rowY - 2).toDouble()..(rowY + 13).toDouble()
            ) {
                blockInput.setText(blockId)
                updateSuggestion(blockId)
                return true
            }
        }

        return false
    }

    private fun refreshProfileText() {
        profileButton.message = Text.literal(ConduitConfig.selectedEspProfile)
    }

    companion object {
        private const val VISIBLE_ROWS = 8
        private const val SUGGESTION_ROWS = 4
    }
}
