package com.twomdtln.client

import com.twomdtln.client.ui.ConduitIconButton
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class EspSettingsScreen(private val parent: Screen? = null) : Screen(Text.literal("ESP Settings")) {
    private lateinit var blockInput: TextFieldWidget
    private lateinit var addButton: ButtonWidget
    private val removeButtons = mutableListOf<ConduitIconButton>()
    private var scrollOffset = 0
    private var suggestions: List<String> = emptyList()

    override fun init() {
        val panelWidth = 360
        val panelHeight = 244
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2

        blockInput = TextFieldWidget(textRenderer, panelLeft + 20, panelTop + 52, 220, 18, Text.literal("Block"))
        blockInput.setMaxLength(64)
        blockInput.setChangedListener { updateSuggestion(it) }
        blockInput.setPlaceholder(Text.literal("minecraft:diamond_ore"))
        addDrawableChild(blockInput)

        addButton = addDrawableChild(
            ButtonWidget.builder(Text.literal("Add")) { addSuggestedBlock() }
                .dimensions(panelLeft + 248, panelTop + 51, 56, 20)
                .build()
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
        val panelHeight = 244
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2
        val profile = ConduitConfig.selectedEspProfile
        val blocks = ConduitConfig.getEspBlocks(profile)

        context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xDE120B20.toInt())
        context.fillGradient(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 36, 0xFF2C1344.toInt(), 0xFF1A102A.toInt())
        context.drawStrokedRectangle(panelLeft, panelTop, panelWidth, panelHeight, 0xBF9C74FF.toInt())
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelTop + 12, 0xFFF8F0FF.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Profile: $profile"), panelLeft + 20, panelTop + 28, 0xFFD8C6F1.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Suggestions"), panelLeft + 20, panelTop + 76, 0xFFB884E0.toInt())
        context.drawTextWithShadow(textRenderer, Text.literal("Tracked Blocks"), panelLeft + 20, panelTop + 146, 0xFFB884E0.toInt())

        if (suggestions.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal("No block match"), panelLeft + 20, panelTop + 94, 0xFFBFAFD0.toInt())
        }

        suggestions.take(SUGGESTION_ROWS).forEachIndexed { index, blockId ->
            val rowY = panelTop + 92 + index * 16
            context.fill(panelLeft + 16, rowY - 2, panelLeft + 308, rowY + 13, 0x2AFFFFFF)
            context.drawItem(resolveBlockStack(blockId), panelLeft + 20, rowY - 2)
            context.drawTextWithShadow(textRenderer, Text.literal(blockId), panelLeft + 40, rowY, 0xFFE5DAF4.toInt())
        }

        if (blocks.isEmpty()) {
            context.drawTextWithShadow(textRenderer, Text.literal("No blocks tracked in this profile."), panelLeft + 20, panelTop + 168, 0xFFBFAFD0.toInt())
        }

        blocks.drop(scrollOffset).take(VISIBLE_ROWS).forEachIndexed { index, blockId ->
            val rowY = panelTop + 164 + index * 18
            context.drawItem(resolveBlockStack(blockId), panelLeft + 20, rowY - 2)
            context.drawTextWithShadow(textRenderer, Text.literal(blockId), panelLeft + 40, rowY, 0xFFF1EAFF.toInt())
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
        suggestions = resolveBlockSuggestions(input)
        addButton.active = suggestions.isNotEmpty()
    }

    private fun refreshRemoveButtons() {
        removeButtons.forEach(::remove)
        removeButtons.clear()

        val blocks = ConduitConfig.getEspBlocks()
        val visibleBlocks = blocks.drop(scrollOffset).take(VISIBLE_ROWS)

        val panelWidth = 360
        val panelHeight = 244
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2

        visibleBlocks.forEachIndexed { index, blockId ->
            val rowY = panelTop + 164 + index * 18

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
        return resolveBlockSuggestions(input).firstOrNull()
    }

    private fun resolveBlockSuggestions(input: String): List<String> {
        val query = input.trim().lowercase()
        val allIds = Registries.BLOCK.ids.map { it.toString() }.sorted()

        if (query.isEmpty()) {
            return allIds.take(SUGGESTION_ROWS)
        }

        val startsWith = allIds.filter { it.startsWith(query) }
        val contains = allIds.filter { !it.startsWith(query) && it.contains(query) }
        return (startsWith + contains).take(SUGGESTION_ROWS)
    }

    private fun resolveBlockStack(blockId: String): ItemStack {
        val id = Identifier.tryParse(blockId) ?: return ItemStack.EMPTY
        val block = Registries.BLOCK.getOptionalValue(id).orElse(null) ?: return ItemStack.EMPTY
        return ItemStack(block)
    }

    private fun clickSuggestion(mouseX: Double, mouseY: Double): Boolean {
        val panelWidth = 360
        val panelHeight = 244
        val panelLeft = width / 2 - panelWidth / 2
        val panelTop = height / 2 - panelHeight / 2

        suggestions.take(SUGGESTION_ROWS).forEachIndexed { index, blockId ->
            val rowY = panelTop + 92 + index * 16

            if (mouseX in (panelLeft + 16).toDouble()..(panelLeft + 308).toDouble() &&
                mouseY in (rowY - 2).toDouble()..(rowY + 13).toDouble()
            ) {
                blockInput.setText(blockId)
                updateSuggestion(blockId)
                return true
            }
        }

        return false
    }

    companion object {
        private const val VISIBLE_ROWS = 4
        private const val SUGGESTION_ROWS = 4
    }
}
