package com.twomdtln.client

import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object EspBlockCatalog {
    private val dyePrefixes = listOf(
        "white_",
        "light_gray_",
        "gray_",
        "black_",
        "brown_",
        "red_",
        "orange_",
        "yellow_",
        "lime_",
        "green_",
        "cyan_",
        "light_blue_",
        "blue_",
        "purple_",
        "magenta_",
        "pink_"
    )

    private val suggestionEntries by lazy {
        Registries.BLOCK.ids
            .map { it.toString() }
            .groupBy(::normalizeForStorage)
            .map { (canonicalId, aliases) -> SuggestionEntry(canonicalId, aliases.toSet()) }
            .sortedBy { it.id }
    }

    fun normalizeForStorage(blockId: String): String {
        val id = Identifier.tryParse(blockId) ?: return blockId
        val remainder = stripDyePrefix(id.path) ?: id.path

        if (!isColorFamily(id.namespace, remainder)) {
            return blockId
        }

        return Identifier.of(id.namespace, remainder).toString()
    }

    fun resolveTrackedBlocks(storedIds: List<String>): Set<Block> {
        return storedIds
            .asSequence()
            .map(::normalizeForStorage)
            .flatMap { expandBlockIds(it).asSequence() }
            .mapNotNull { Identifier.tryParse(it) }
            .mapNotNull { Registries.BLOCK.getOptionalValue(it).orElse(null) }
            .toSet()
    }

    fun resolveSuggestions(input: String, limit: Int): List<String> {
        val query = input.trim().lowercase()

        return if (query.isEmpty()) {
            suggestionEntries.take(limit).map { it.id }
        } else {
            val startsWith = suggestionEntries.filter { it.matchesPrefix(query) }
            val contains = suggestionEntries.filter { !it.matchesPrefix(query) && it.matchesContains(query) }
            (startsWith + contains).take(limit).map { it.id }
        }
    }

    fun getDisplayLabel(blockId: String): String {
        val normalized = normalizeForStorage(blockId)
        return if (isColorFamilyId(normalized)) {
            "$normalized (all colors)"
        } else {
            normalized
        }
    }

    fun getDisplayStack(blockId: String): ItemStack {
        val normalized = normalizeForStorage(blockId)
        val id = Identifier.tryParse(normalized) ?: return ItemStack.EMPTY
        val displayId = preferredDisplayId(id)
        val block = Registries.BLOCK.getOptionalValue(displayId).orElse(null) ?: return ItemStack.EMPTY
        return ItemStack(block)
    }

    fun getStorageIdForBlock(block: Block): String {
        return normalizeForStorage(Registries.BLOCK.getId(block).toString())
    }

    private fun expandBlockIds(blockId: String): List<String> {
        val normalized = normalizeForStorage(blockId)
        val id = Identifier.tryParse(normalized) ?: return emptyList()

        if (!isColorFamily(id.namespace, id.path)) {
            return listOf(normalized)
        }

        val expanded = mutableListOf<String>()
        val baseId = Identifier.of(id.namespace, id.path)

        if (Registries.BLOCK.containsId(baseId)) {
            expanded += baseId.toString()
        }

        dyePrefixes.forEach { prefix ->
            val coloredId = Identifier.of(id.namespace, prefix + id.path)

            if (Registries.BLOCK.containsId(coloredId)) {
                expanded += coloredId.toString()
            }
        }

        return expanded
    }

    private fun preferredDisplayId(id: Identifier): Identifier {
        if (!isColorFamily(id.namespace, id.path)) {
            return id
        }

        val blackVariant = Identifier.of(id.namespace, "black_${id.path}")
        if (Registries.BLOCK.containsId(blackVariant)) {
            return blackVariant
        }

        if (Registries.BLOCK.containsId(id)) {
            return id
        }

        return dyePrefixes
            .asSequence()
            .map { Identifier.of(id.namespace, it + id.path) }
            .firstOrNull { Registries.BLOCK.containsId(it) }
            ?: id
    }

    private fun isColorFamilyId(blockId: String): Boolean {
        val id = Identifier.tryParse(blockId) ?: return false
        return isColorFamily(id.namespace, id.path)
    }

    private fun isColorFamily(namespace: String, basePath: String): Boolean {
        val variantCount = dyePrefixes.count { prefix ->
            Registries.BLOCK.containsId(Identifier.of(namespace, prefix + basePath))
        }

        return variantCount >= 2
    }

    private fun stripDyePrefix(path: String): String? {
        return dyePrefixes.firstOrNull { path.startsWith(it) }
            ?.let { prefix -> path.removePrefix(prefix) }
    }

    private data class SuggestionEntry(
        val id: String,
        val aliases: Set<String>,
    ) {
        fun matchesPrefix(query: String): Boolean {
            return id.contains(query) && (id.startsWith(query) || aliases.any { it.startsWith(query) })
        }

        fun matchesContains(query: String): Boolean {
            return id.contains(query) || aliases.any { it.contains(query) }
        }
    }
}
