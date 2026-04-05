package com.twomdtln.client

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.DrawStyle
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.debug.gizmo.GizmoDrawing

object ConduitEspRenderer {
    private const val SCAN_INTERVAL_TICKS = 20
    private const val MAX_RENDERED_BLOCKS = 64
    private const val MAX_MATCHES_TO_SORT = 192
    private const val CORNER_SIZE = 0.22
    private const val ESP_COLOR = 0xFF9C74FF.toInt()

    private var tickCounter = 0
    private var targets: List<BlockPos> = emptyList()

    fun initialize() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(WorldRenderEvents.DebugRender { context ->
            render(context)
        })
    }

    fun tick(client: MinecraftClient) {
        if (!ConduitConfig.espEnabled || client.world == null || client.player == null) {
            targets = emptyList()
            tickCounter = 0
            return
        }

        tickCounter++

        if (tickCounter < SCAN_INTERVAL_TICKS) {
            return
        }

        tickCounter = 0
        updateTargets(client)
    }

    private fun updateTargets(client: MinecraftClient) {
        val world = client.world ?: return
        val player = client.player ?: return
        val trackedBlocks = resolveTrackedBlocks()

        if (trackedBlocks.isEmpty()) {
            targets = emptyList()
            return
        }

        val center = player.blockPos
        val centerChunkX = center.x shr 4
        val centerChunkZ = center.z shr 4
        val chunkRadius = client.options.getClampedViewDistance()
        val found = mutableListOf<Pair<BlockPos, Double>>()

        outer@ for (chunkX in centerChunkX - chunkRadius..centerChunkX + chunkRadius) {
            for (chunkZ in centerChunkZ - chunkRadius..centerChunkZ + chunkRadius) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue
                }

                val startX = chunkX shl 4
                val startZ = chunkZ shl 4

                for (y in world.bottomY..world.topYInclusive) {
                    for (localX in 0 until 16) {
                        for (localZ in 0 until 16) {
                            val pos = BlockPos(startX + localX, y, startZ + localZ)
                            val block = world.getBlockState(pos).block

                            if (!trackedBlocks.contains(block)) {
                                continue
                            }

                            val immutable = pos.toImmutable()
                            val distance = immutable.getSquaredDistance(center)
                            found += immutable to distance

                            if (found.size >= MAX_MATCHES_TO_SORT) {
                                break@outer
                            }
                        }
                    }
                }
            }
        }

        targets = found
            .sortedBy { it.second }
            .take(MAX_RENDERED_BLOCKS)
            .map { it.first }
    }

    private fun render(context: WorldRenderContext) {
        if (!ConduitConfig.espEnabled || targets.isEmpty()) {
            return
        }

        MinecraftClient.getInstance().newGizmoScope().use {
            targets.forEach { pos ->
                drawCornerBox(Box(pos).expand(0.01))
            }
        }
    }

    private fun drawCornerBox(box: Box) {
        val x = box.getLengthX() * CORNER_SIZE
        val y = box.getLengthY() * CORNER_SIZE
        val z = box.getLengthZ() * CORNER_SIZE

        drawCorner(Vec3d(box.minX, box.minY, box.minZ), x, y, z, 1.0, 1.0, 1.0)
        drawCorner(Vec3d(box.maxX, box.minY, box.minZ), x, y, z, -1.0, 1.0, 1.0)
        drawCorner(Vec3d(box.minX, box.maxY, box.minZ), x, y, z, 1.0, -1.0, 1.0)
        drawCorner(Vec3d(box.maxX, box.maxY, box.minZ), x, y, z, -1.0, -1.0, 1.0)
        drawCorner(Vec3d(box.minX, box.minY, box.maxZ), x, y, z, 1.0, 1.0, -1.0)
        drawCorner(Vec3d(box.maxX, box.minY, box.maxZ), x, y, z, -1.0, 1.0, -1.0)
        drawCorner(Vec3d(box.minX, box.maxY, box.maxZ), x, y, z, 1.0, -1.0, -1.0)
        drawCorner(Vec3d(box.maxX, box.maxY, box.maxZ), x, y, z, -1.0, -1.0, -1.0)
    }

    private fun drawCorner(origin: Vec3d, x: Double, y: Double, z: Double, xDir: Double, yDir: Double, zDir: Double) {
        drawLine(origin, origin.add(x * xDir, 0.0, 0.0))
        drawLine(origin, origin.add(0.0, y * yDir, 0.0))
        drawLine(origin, origin.add(0.0, 0.0, z * zDir))
    }

    private fun drawLine(start: Vec3d, end: Vec3d) {
        GizmoDrawing.line(start, end, ESP_COLOR, 2.0f).ignoreOcclusion()
    }

    private fun resolveTrackedBlocks(): Set<Block> {
        return ConduitConfig.getEspBlocks()
            .mapNotNull { Identifier.tryParse(it) }
            .mapNotNull { Registries.BLOCK.getOptionalValue(it).orElse(null) }
            .toSet()
    }

    private fun BlockPos.getSquaredDistance(other: BlockPos): Double {
        val dx = (x - other.x).toDouble()
        val dy = (y - other.y).toDouble()
        val dz = (z - other.z).toDouble()
        return dx * dx + dy * dy + dz * dz
    }
}
