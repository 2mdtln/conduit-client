package com.twomdtln.client

import net.minecraft.client.MinecraftClient
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.Items
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import kotlin.random.Random

object ConduitAutoTotem {
    private const val MIN_HUMAN_DELAY_MS = 1_000L
    private const val MAX_HUMAN_DELAY_MS = 5_000L

    private var scheduledEquipAtMs: Long? = null

    fun tick(client: MinecraftClient) {
        if (!ConduitConfig.autoTotemEnabled) {
            reset()
            return
        }

        val player = client.player ?: run {
            reset()
            return
        }

        if (client.interactionManager == null || client.world == null) {
            reset()
            return
        }

        if (!player.offHandStack.isEmpty) {
            reset()
            return
        }

        val handler = player.currentScreenHandler ?: run {
            reset()
            return
        }

        if (client.currentScreen != null || !handler.cursorStack.isEmpty) {
            return
        }

        val totemSlot = findTotemSlot(handler.slots, player.inventory) ?: run {
            reset()
            return
        }
        val offhandSlot = findOffhandSlot(handler.slots, player.inventory) ?: run {
            reset()
            return
        }

        if (ConduitConfig.autoTotemInstantEnabled) {
            scheduledEquipAtMs = null
            moveTotemToOffhand(client, handler.syncId, totemSlot, offhandSlot)
            return
        }

        val now = System.currentTimeMillis()
        val scheduledAt = scheduledEquipAtMs ?: scheduleHumanDelay(now)
        if (now >= scheduledAt) {
            moveTotemToOffhand(client, handler.syncId, totemSlot, offhandSlot)
            scheduledEquipAtMs = null
        }
    }

    private fun scheduleHumanDelay(now: Long): Long {
        val scheduledAt = now + Random.nextLong(MIN_HUMAN_DELAY_MS, MAX_HUMAN_DELAY_MS + 1)
        scheduledEquipAtMs = scheduledAt
        return scheduledAt
    }

    private fun moveTotemToOffhand(client: MinecraftClient, syncId: Int, sourceSlot: Slot, offhandSlot: Slot) {
        val interactionManager = client.interactionManager ?: return
        interactionManager.clickSlot(syncId, sourceSlot.id, 0, SlotActionType.PICKUP, client.player)
        interactionManager.clickSlot(syncId, offhandSlot.id, 0, SlotActionType.PICKUP, client.player)

        val handler = client.player?.currentScreenHandler ?: return
        if (!handler.cursorStack.isEmpty) {
            interactionManager.clickSlot(syncId, sourceSlot.id, 0, SlotActionType.PICKUP, client.player)
        }
    }

    private fun findTotemSlot(slots: List<Slot>, inventory: PlayerInventory): Slot? {
        return slots.firstOrNull { slot ->
            slot.inventory === inventory &&
                slot.index != PlayerInventory.OFF_HAND_SLOT &&
                slot.hasStack() &&
                slot.stack.isOf(Items.TOTEM_OF_UNDYING)
        }
    }

    private fun findOffhandSlot(slots: List<Slot>, inventory: PlayerInventory): Slot? {
        return slots.firstOrNull { slot ->
            slot.inventory === inventory && slot.index == PlayerInventory.OFF_HAND_SLOT
        }
    }

    fun reset() {
        scheduledEquipAtMs = null
    }
}
