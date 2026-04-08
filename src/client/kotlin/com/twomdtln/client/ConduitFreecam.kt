package com.twomdtln.client

import com.mojang.authlib.GameProfile
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import java.util.UUID

object ConduitFreecam {
    private const val CAMERA_ENTITY_ID = -41_337
    private const val GHOST_ENTITY_ID = -41_338
    private const val BASE_SPEED = 0.7
    private const val FAST_SPEED = 1.2

    private var enabled = false
    private var anchorPos: Vec3d? = null
    private var anchorYaw = 0f
    private var anchorPitch = 0f
    private var cameraEntity: OtherClientPlayerEntity? = null
    private var ghostEntity: OtherClientPlayerEntity? = null

    fun isEnabled(): Boolean = enabled

    fun toggle(client: MinecraftClient): Boolean {
        return if (enabled) {
            disable(client)
            false
        } else {
            enable(client)
        }
    }

    fun enable(client: MinecraftClient): Boolean {
        val player = client.player ?: return false
        val world = client.world ?: return false

        if (enabled) {
            return true
        }

        anchorPos = Vec3d(player.x, player.y, player.z)
        anchorYaw = player.yaw
        anchorPitch = player.pitch

        val profile = GameProfile(UUID.randomUUID(), "${player.gameProfile.name}_freecam")
        val freecam = OtherClientPlayerEntity(world, profile)
        freecam.setId(CAMERA_ENTITY_ID)
        freecam.copyPositionAndRotation(player)
        freecam.noClip = true
        freecam.setNoGravity(true)
        freecam.setVelocity(Vec3d.ZERO)
        world.addEntity(freecam)
        client.setCameraEntity(freecam)

        val ghost = OtherClientPlayerEntity(world, player.gameProfile)
        ghost.setId(GHOST_ENTITY_ID)
        ghost.copyPositionAndRotation(player)
        ghost.headYaw = player.headYaw
        ghost.bodyYaw = player.bodyYaw
        ghost.noClip = false
        ghost.setNoGravity(true)
        ghost.setVelocity(Vec3d.ZERO)
        world.addEntity(ghost)

        cameraEntity = freecam
        ghostEntity = ghost
        enabled = true
        return true
    }

    fun disable(client: MinecraftClient) {
        if (!enabled) {
            cleanup(client)
            return
        }

        val player = client.player
        val anchor = anchorPos
        if (player != null && anchor != null) {
            player.refreshPositionAndAngles(anchor, anchorYaw, anchorPitch)
            player.setVelocity(Vec3d.ZERO)
        }

        cleanup(client)
    }

    fun tick(client: MinecraftClient) {
        if (!enabled) {
            return
        }

        val player = client.player
        val world = client.world
        val freecam = cameraEntity
        val ghost = ghostEntity
        val anchor = anchorPos
        if (player == null || world == null || freecam == null || ghost == null || anchor == null) {
            disable(client)
            return
        }

        if (client.cameraEntity !== freecam) {
            client.setCameraEntity(freecam)
        }

        player.refreshPositionAndAngles(anchor, player.yaw, player.pitch)
        player.setVelocity(Vec3d.ZERO)
        player.fallDistance = 0.0

        ghost.refreshPositionAndAngles(anchor, anchorYaw, anchorPitch)
        ghost.headYaw = anchorYaw
        ghost.bodyYaw = anchorYaw
        ghost.setVelocity(Vec3d.ZERO)

        freecam.setYaw(player.yaw)
        freecam.setPitch(player.pitch)
        freecam.headYaw = player.headYaw
        freecam.bodyYaw = player.bodyYaw

        val direction = resolveMovement(client, freecam)
        if (direction != Vec3d.ZERO) {
            freecam.setVelocity(direction)
            freecam.setPosition(freecam.x + direction.x, freecam.y + direction.y, freecam.z + direction.z)
        } else {
            freecam.setVelocity(Vec3d.ZERO)
        }
    }

    private fun resolveMovement(client: MinecraftClient, camera: OtherClientPlayerEntity): Vec3d {
        val options = client.options
        val speed = if (options.sprintKey.isPressed) FAST_SPEED else BASE_SPEED

        val forward = camera.rotationVector.normalize()
        val horizontalForward = Vec3d(forward.x, 0.0, forward.z).normalize()
        val right = horizontalForward.crossProduct(Vec3d(0.0, 1.0, 0.0)).normalize()

        var movement = Vec3d.ZERO
        if (options.forwardKey.isPressed) {
            movement = movement.add(horizontalForward)
        }
        if (options.backKey.isPressed) {
            movement = movement.subtract(horizontalForward)
        }
        if (options.rightKey.isPressed) {
            movement = movement.add(right)
        }
        if (options.leftKey.isPressed) {
            movement = movement.subtract(right)
        }
        if (options.jumpKey.isPressed) {
            movement = movement.add(0.0, 1.0, 0.0)
        }
        if (options.sneakKey.isPressed) {
            movement = movement.add(0.0, -1.0, 0.0)
        }

        return if (movement == Vec3d.ZERO) {
            Vec3d.ZERO
        } else {
            movement.normalize().multiply(speed)
        }
    }

    private fun cleanup(client: MinecraftClient) {
        val player = client.player
        if (player != null) {
            client.setCameraEntity(player)
        }

        val freecam = cameraEntity
        if (freecam != null) {
            client.world?.removeEntity(freecam.id, Entity.RemovalReason.DISCARDED)
        }

        val ghost = ghostEntity
        if (ghost != null) {
            client.world?.removeEntity(ghost.id, Entity.RemovalReason.DISCARDED)
        }

        cameraEntity = null
        ghostEntity = null
        anchorPos = null
        enabled = false
    }
}
