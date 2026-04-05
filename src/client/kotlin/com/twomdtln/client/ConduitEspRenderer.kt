package com.twomdtln.client

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.block.Block
import net.minecraft.client.MinecraftClient
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.chunk.WorldChunk
import net.minecraft.util.math.Vec3d
import net.minecraft.world.debug.gizmo.GizmoDrawing
import java.util.ArrayDeque
import java.util.HashSet

object ConduitEspRenderer {
    private const val CHUNKS_PER_TICK = 3
    private const val CACHE_REFRESH_INTERVAL_TICKS = 120
    private const val MAX_RENDERED_BLOCKS = 64
    private const val MAX_MATCHES_TO_SORT = 256
    private const val MAX_MATCHES_PER_CHUNK = 24
    private const val CORNER_SIZE = 0.16
    private const val ESP_COLOR = 0xFF9C74FF.toInt()

    private val cachedChunkMatches = linkedMapOf<Long, List<BlockPos>>()
    private val pendingChunkScans = ArrayDeque<Long>()
    private val pendingChunkSet = HashSet<Long>()
    private var targets: List<BlockPos> = emptyList()
    private var maintenanceCounter = 0
    private var lastProfileFingerprint = ""
    private var lastWorldIdentity = 0
    private var lastCenterChunkX = Int.MIN_VALUE
    private var lastCenterChunkZ = Int.MIN_VALUE
    private var targetsDirty = true
    private var targetRefreshCooldown = 0
    private var trackedBlocks = emptySet<Block>()
    private var trackedStaticBlocks = emptySet<Block>()

    data class TargetSummary(
        val count: Int,
        val stack: ItemStack,
    )

    fun initialize() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(WorldRenderEvents.DebugRender { context ->
            render(context)
        })
    }

    fun tick(client: MinecraftClient) {
        if (!ConduitConfig.espEnabled || client.world == null || client.player == null) {
            clear()
            return
        }

        val world = client.world ?: return
        val center = client.player?.blockPos ?: return
        val centerChunkX = center.x shr 4
        val centerChunkZ = center.z shr 4
        val fingerprint = ConduitConfig.selectedEspProfile + "|" + ConduitConfig.getEspBlocks().joinToString(",")
        val worldIdentity = System.identityHashCode(world)

        if (fingerprint != lastProfileFingerprint || worldIdentity != lastWorldIdentity) {
            clear()
            lastProfileFingerprint = fingerprint
            lastWorldIdentity = worldIdentity
            trackedBlocks = resolveTrackedBlocks()
            trackedStaticBlocks = trackedBlocks.filterTo(linkedSetOf()) { !it.defaultState.hasBlockEntity() }
            enqueueVisibleChunks(client, centerChunkX, centerChunkZ, force = true)
        }

        maintenanceCounter++
        targetRefreshCooldown++

        if (centerChunkX != lastCenterChunkX || centerChunkZ != lastCenterChunkZ || maintenanceCounter >= CACHE_REFRESH_INTERVAL_TICKS) {
            lastCenterChunkX = centerChunkX
            lastCenterChunkZ = centerChunkZ
            maintenanceCounter = 0
            pruneInvisibleChunks(centerChunkX, centerChunkZ, client.options.getClampedViewDistance())
            enqueueVisibleChunks(client, centerChunkX, centerChunkZ, force = false)
            targetsDirty = true
        }

        repeat(CHUNKS_PER_TICK) {
            val chunkKey = if (pendingChunkScans.isEmpty()) {
                return@repeat
            } else {
                pendingChunkScans.removeFirst()
            }
            pendingChunkSet.remove(chunkKey)
            scanChunk(client, unpackChunkX(chunkKey), unpackChunkZ(chunkKey))
        }

        if (targetsDirty && (pendingChunkScans.isEmpty() || targetRefreshCooldown >= 4)) {
            rebuildTargets(center)
            targetsDirty = false
            targetRefreshCooldown = 0
        }
    }

    private fun scanChunk(client: MinecraftClient, chunkX: Int, chunkZ: Int) {
        val world = client.world ?: return

        if (trackedBlocks.isEmpty() || !world.isChunkLoaded(chunkX, chunkZ)) {
            cachedChunkMatches.remove(chunkKey(chunkX, chunkZ))
            targetsDirty = true
            return
        }

        val chunk = world.chunkManager.getWorldChunk(chunkX, chunkZ, false) ?: return
        val matches = mutableListOf<BlockPos>()
        val seen = HashSet<Long>()

        chunk.blockEntities.keys.forEach { pos ->
            if (trackedBlocks.contains(world.getBlockState(pos).block) && seen.add(pos.asLong())) {
                matches += pos.toImmutable()
            }
        }

        if (matches.size < MAX_MATCHES_PER_CHUNK && trackedStaticBlocks.isNotEmpty()) {
            scanChunkBlocks(chunk, trackedStaticBlocks, matches, seen)
        }

        cachedChunkMatches[chunkKey(chunkX, chunkZ)] = matches
        targetsDirty = true
    }

    private fun scanChunkBlocks(chunk: WorldChunk, trackedBlocks: Set<Block>, matches: MutableList<BlockPos>, seen: MutableSet<Long>) {
        val chunkX = chunk.pos.x
        val chunkZ = chunk.pos.z
        val startX = chunkX shl 4
        val startZ = chunkZ shl 4
        val world = chunk.world

        outer@ for (y in world.bottomY..world.topYInclusive) {
            for (localX in 0 until 16) {
                for (localZ in 0 until 16) {
                    val pos = BlockPos(startX + localX, y, startZ + localZ)
                    val block = chunk.getBlockState(pos).block

                    if (!trackedBlocks.contains(block) || !seen.add(pos.asLong())) {
                        continue
                    }

                    matches += pos.toImmutable()

                    if (matches.size >= MAX_MATCHES_PER_CHUNK) {
                        break@outer
                    }
                }
            }
        }
    }

    private fun rebuildTargets(center: BlockPos) {
        targets = cachedChunkMatches.values
            .asSequence()
            .flatten()
            .map { it to it.getSquaredDistance(center) }
            .sortedBy { it.second }
            .take(MAX_MATCHES_TO_SORT)
            .take(MAX_RENDERED_BLOCKS)
            .map { it.first }
            .toList()
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

    fun getAimedTarget(client: MinecraftClient): BlockPos? {
        if (!ConduitConfig.espEnabled || targets.isEmpty() || client.player == null) {
            return null
        }

        val player = client.player ?: return null
        val cameraPos = player.getCameraPosVec(1.0f)
        val look = player.rotationVecClient.normalize()
        var bestTarget: BlockPos? = null
        var bestScore = Double.NEGATIVE_INFINITY

        targets.forEach { pos ->
            val center = Vec3d.ofCenter(pos)
            val toTarget = center.subtract(cameraPos)
            val distanceSq = toTarget.lengthSquared()

            if (distanceSq <= 0.0001) {
                return@forEach
            }

            val distance = kotlin.math.sqrt(distanceSq)

            val direction = toTarget.normalize()
            val alignment = look.dotProduct(direction)
            val requiredAlignment = requiredAimAlignment(distance)

            if (alignment < requiredAlignment) {
                return@forEach
            }

            val score = alignment - (distanceSq * 0.0000025)

            if (score > bestScore) {
                bestScore = score
                bestTarget = pos
            }
        }

        return bestTarget
    }

    fun onBlockUpdate(pos: BlockPos) {
        if (!ConduitConfig.espEnabled) {
            return
        }

        val client = MinecraftClient.getInstance()
        val world = client.world ?: return
        val chunkX = pos.x shr 4
        val chunkZ = pos.z shr 4
        val key = chunkKey(chunkX, chunkZ)

        cachedChunkMatches.remove(key)
        pendingChunkSet.remove(key)
        pendingChunkScans.remove(key)

        if (world.isChunkLoaded(chunkX, chunkZ)) {
            pendingChunkScans.addFirst(key)
            pendingChunkSet.add(key)
        }

        targetsDirty = true
        targetRefreshCooldown = 4
    }

    fun getTargetSummaries(client: MinecraftClient): List<TargetSummary> {
        if (!ConduitConfig.espEnabled || targets.isEmpty() || client.world == null) {
            return emptyList()
        }

        val counts = linkedMapOf<String, Int>()

        targets.forEach { pos ->
            val blockId = client.world?.getBlockState(pos)?.block?.let(EspBlockCatalog::getStorageIdForBlock) ?: return@forEach
            counts[blockId] = (counts[blockId] ?: 0) + 1
        }

        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(6)
            .map { (blockId, count) ->
                TargetSummary(
                    count = count,
                    stack = EspBlockCatalog.getDisplayStack(blockId)
                )
            }
    }

    private fun requiredAimAlignment(distance: Double): Double {
        return when {
            distance >= 96.0 -> 0.9925
            distance >= 64.0 -> 0.988
            distance >= 40.0 -> 0.98
            distance >= 24.0 -> 0.965
            else -> 0.93
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
        GizmoDrawing.line(start, end, ESP_COLOR, 1.5f).ignoreOcclusion()
    }

    private fun resolveTrackedBlocks(): Set<Block> {
        return EspBlockCatalog.resolveTrackedBlocks(ConduitConfig.getEspBlocks())
    }

    private fun enqueueVisibleChunks(client: MinecraftClient, centerChunkX: Int, centerChunkZ: Int, force: Boolean) {
        val world = client.world ?: return
        val chunkRadius = client.options.getClampedViewDistance()
        val queuedChunks = mutableListOf<Long>()

        for (chunkX in centerChunkX - chunkRadius..centerChunkX + chunkRadius) {
            for (chunkZ in centerChunkZ - chunkRadius..centerChunkZ + chunkRadius) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    cachedChunkMatches.remove(chunkKey(chunkX, chunkZ))
                    continue
                }

                val key = chunkKey(chunkX, chunkZ)

                if (!force && (cachedChunkMatches.containsKey(key) || pendingChunkSet.contains(key))) {
                    continue
                }

                queuedChunks += key
            }
        }

        queuedChunks
            .sortedBy { chunkDistanceSq(centerChunkX, centerChunkZ, unpackChunkX(it), unpackChunkZ(it)) }
            .forEach { key ->
                pendingChunkScans += key
                pendingChunkSet += key
            }
    }

    private fun pruneInvisibleChunks(centerChunkX: Int, centerChunkZ: Int, chunkRadius: Int) {
        val iterator = cachedChunkMatches.keys.iterator()

        while (iterator.hasNext()) {
            val key = iterator.next()
            val chunkX = unpackChunkX(key)
            val chunkZ = unpackChunkZ(key)

            if (kotlin.math.abs(chunkX - centerChunkX) > chunkRadius || kotlin.math.abs(chunkZ - centerChunkZ) > chunkRadius) {
                iterator.remove()
                pendingChunkSet.remove(key)
                pendingChunkScans.remove(key)
            }
        }
    }

    private fun clear() {
        cachedChunkMatches.clear()
        pendingChunkScans.clear()
        pendingChunkSet.clear()
        targets = emptyList()
        maintenanceCounter = 0
        lastProfileFingerprint = ""
        lastWorldIdentity = 0
        lastCenterChunkX = Int.MIN_VALUE
        lastCenterChunkZ = Int.MIN_VALUE
        targetsDirty = true
        targetRefreshCooldown = 0
        trackedBlocks = emptySet()
        trackedStaticBlocks = emptySet()
    }

    private fun chunkKey(chunkX: Int, chunkZ: Int): Long {
        return (chunkX.toLong() shl 32) xor (chunkZ.toLong() and 0xffffffffL)
    }

    private fun unpackChunkX(key: Long): Int = (key shr 32).toInt()

    private fun unpackChunkZ(key: Long): Int = key.toInt()

    private fun chunkDistanceSq(centerChunkX: Int, centerChunkZ: Int, chunkX: Int, chunkZ: Int): Int {
        val dx = chunkX - centerChunkX
        val dz = chunkZ - centerChunkZ
        return dx * dx + dz * dz
    }

    private fun BlockPos.getSquaredDistance(other: BlockPos): Double {
        val dx = (x - other.x).toDouble()
        val dy = (y - other.y).toDouble()
        val dz = (z - other.z).toDouble()
        return dx * dx + dy * dy + dz * dz
    }
}
