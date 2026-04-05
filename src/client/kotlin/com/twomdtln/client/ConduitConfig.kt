package com.twomdtln.client

import net.fabricmc.loader.api.FabricLoader
import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedHashMap
import java.util.Properties

object ConduitConfig {
    private val defaultEspProfiles = linkedMapOf(
        "Base Hunting" to mutableListOf(
            "minecraft:chest",
            "minecraft:trapped_chest",
            "minecraft:ender_chest",
            "minecraft:barrel",
            "minecraft:shulker_box",
            "minecraft:bed",
            "minecraft:crafting_table",
            "minecraft:furnace",
            "minecraft:blast_furnace",
            "minecraft:smoker",
            "minecraft:anvil",
            "minecraft:enchanting_table",
            "minecraft:respawn_anchor"
        ),
        "Ores" to mutableListOf("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:ancient_debris"),
        "Storage" to mutableListOf("minecraft:chest", "minecraft:barrel", "minecraft:shulker_box")
    )
    private val configPath: Path = FabricLoader.getInstance().configDir.resolve("conduit-client.properties")

    var fullBrightEnabled: Boolean = false
        private set

    var espEnabled: Boolean = false
        private set

    var showFpsEnabled: Boolean = false
        private set

    var showPingEnabled: Boolean = false
        private set

    var selectedEspProfile: String = "Base Hunting"
        private set

    private val espProfiles: LinkedHashMap<String, MutableList<String>> = LinkedHashMap()

    fun load() {
        espProfiles.clear()
        defaultEspProfiles.forEach { (name, blocks) -> espProfiles[name] = blocks.toMutableList() }
        selectedEspProfile = espProfiles.keys.first()

        if (!Files.exists(configPath)) {
            save()
            return
        }

        val properties = Properties()
        Files.newBufferedReader(configPath).use { reader: Reader ->
            properties.load(reader)
        }

        fullBrightEnabled = properties.getProperty("full_bright_enabled", "false").toBoolean()
        espEnabled = properties.getProperty("esp_enabled", "false").toBoolean()
        showFpsEnabled = properties.getProperty("show_fps_enabled", "false").toBoolean()
        showPingEnabled = properties.getProperty("show_ping_enabled", "false").toBoolean()

        val configuredProfiles = properties.getProperty("esp_profiles")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        if (configuredProfiles.isNotEmpty()) {
            espProfiles.clear()
            configuredProfiles.forEach { profile ->
                val savedBlocks = properties.getProperty(profileKey(profile))
                    ?.split("|")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.map(EspBlockCatalog::normalizeForStorage)
                    ?.distinct()
                    ?.toMutableList()
                    ?: mutableListOf()

                espProfiles[profile] = savedBlocks
            }
        }

        if (espProfiles.isEmpty()) {
            defaultEspProfiles.forEach { (name, blocks) -> espProfiles[name] = blocks.toMutableList() }
        }

        selectedEspProfile = properties.getProperty("esp_selected_profile", espProfiles.keys.first())
            .takeIf { espProfiles.containsKey(it) }
            ?: espProfiles.keys.first()
    }

    fun setFullBrightEnabled(enabled: Boolean) {
        fullBrightEnabled = enabled
        save()
    }

    fun setEspEnabled(enabled: Boolean) {
        espEnabled = enabled
        save()
    }

    fun setShowFpsEnabled(enabled: Boolean) {
        showFpsEnabled = enabled
        save()
    }

    fun setShowPingEnabled(enabled: Boolean) {
        showPingEnabled = enabled
        save()
    }

    fun cycleEspProfile(): String {
        val profiles = espProfiles.keys.toList()
        val currentIndex = profiles.indexOf(selectedEspProfile).coerceAtLeast(0)
        selectedEspProfile = profiles[(currentIndex + 1) % profiles.size]
        save()
        return selectedEspProfile
    }

    fun setSelectedEspProfile(profile: String) {
        if (!espProfiles.containsKey(profile)) {
            return
        }

        selectedEspProfile = profile
        save()
    }

    fun getEspProfiles(): List<String> = espProfiles.keys.toList()

    fun getEspBlocks(profile: String = selectedEspProfile): List<String> {
        return espProfiles[profile]?.toList() ?: emptyList()
    }

    fun addEspBlock(blockId: String, profile: String = selectedEspProfile): Boolean {
        val blocks = espProfiles.getOrPut(profile) { mutableListOf() }
        val normalizedBlockId = EspBlockCatalog.normalizeForStorage(blockId)

        if (blocks.contains(normalizedBlockId)) {
            return false
        }

        blocks += normalizedBlockId
        save()
        return true
    }

    fun removeEspBlock(blockId: String, profile: String = selectedEspProfile): Boolean {
        val removed = espProfiles[profile]?.remove(blockId) == true

        if (removed) {
            save()
        }

        return removed
    }

    private fun save() {
        Files.createDirectories(configPath.parent)

        val properties = Properties().apply {
            setProperty("full_bright_enabled", fullBrightEnabled.toString())
            setProperty("esp_enabled", espEnabled.toString())
            setProperty("show_fps_enabled", showFpsEnabled.toString())
            setProperty("show_ping_enabled", showPingEnabled.toString())
            setProperty("esp_selected_profile", selectedEspProfile)
            setProperty("esp_profiles", espProfiles.keys.joinToString(","))

            espProfiles.forEach { (profile, blocks) ->
                setProperty(profileKey(profile), blocks.joinToString("|"))
            }
        }

        Files.newBufferedWriter(configPath).use { writer: Writer ->
            properties.store(writer, "Conduit Client")
        }
    }

    private fun profileKey(profile: String): String {
        return "esp_profile.${profile.lowercase().replace(" ", "_")}"
    }
}
