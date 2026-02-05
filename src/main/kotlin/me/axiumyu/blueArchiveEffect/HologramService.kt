package me.axiumyu.blueArchiveEffect

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class HologramService {

    // 存储玩家当前的观察状态
    // Key: PlayerUUID
    // Value: ViewingData (包含虚假实体ID 和 宿主实体ID)
    private val viewMap = ConcurrentHashMap<UUID, ViewingData>()

    data class ViewingData(val fakeEntityId: Int, val hostEntityId: Int)

    fun updateHolograms() {
        for (player in Bukkit.getOnlinePlayers()) {
            val rayTraceTarget = rayTrace(player)
            val currentView = viewMap[player.uniqueId]

            if (currentView != null) {
                // === 玩家当前正在看某个全息图 ===

                // 获取宿主实体（通过ID获取，避免对象引用失效）
                // 注意：在主线程运行此逻辑是安全的，如果是异步需注意 Bukkit.getEntity 线程安全
                // 但 Entity.getEntityId 是安全的，我们可以尝试先从当前世界查找
                val hostEntity = getEntityById(player.world, currentView.hostEntityId)

                if (hostEntity != null && hostEntity.isValid && !hostEntity.isDead) {
                    // 宿主还活着，检查是否需要切换目标

                    if (rayTraceTarget != null && rayTraceTarget.entityId != currentView.hostEntityId) {
                        // 1. 玩家把准星移到了另一个新怪物身上 -> 立即切换
                        removeHologram(player, currentView.fakeEntityId)
                        if (shouldShow(rayTraceTarget)) {
                            createHologram(player, rayTraceTarget)
                        }
                    } else if (rayTraceTarget == null && !isLookingAt(player, hostEntity)) {
                        // 2. 射线没打中，且视线偏离了宿主太多 -> 销毁
                        removeHologram(player, currentView.fakeEntityId)
                    } else {
                        // 3. 射线打中了同一个怪 OR 射线虽然没打中但还在视线范围内（吸附） -> 更新位置
                        moveHologram(player, currentView.fakeEntityId, hostEntity)
                    }
                } else {
                    // 宿主死了或消失了 -> 销毁
                    removeHologram(player, currentView.fakeEntityId)
                }
            } else {
                // === 玩家当前没看任何东西 ===
                if (rayTraceTarget != null && shouldShow(rayTraceTarget)) {
                    createHologram(player, rayTraceTarget)
                }
            }
        }
    }

    private fun createHologram(player: Player, host: LivingEntity) {
        val fakeId = 2000000 + Random.nextInt(1000000)
        val uuid = UUID.randomUUID()
        val loc = host.eyeLocation.add(0.0, 0.6, 0.0)

        // 1. Spawn
        val spawnPacket = WrapperPlayServerSpawnEntity(
            fakeId, uuid, EntityTypes.TEXT_DISPLAY,
            SpigotConversionUtil.fromBukkitLocation(loc), 0f, 0, null
        )

        // 2. Metadata
        val atk = host.atkType
        val def = host.defType
        val textComponent =
            mm.deserialize("<${atk.color.asHexString()}>${atk.displayName} <gray>/ <${def.color.asHexString()}>${def.displayName}")

        val metadata = listOf(
            EntityData(23, EntityDataTypes.ADV_COMPONENT, textComponent), // Text
            EntityData(15, EntityDataTypes.BYTE, 3.toByte()),// Billboard: Center
            EntityData(25, EntityDataTypes.INT, 0),
            EntityData(27, EntityDataTypes.BYTE, 3.toByte())
        )

        // [关键修正] Flags: Shadow(1) | SeeThrough(2) | DefaultBackground(4)
        // 这里只开启阴影(1)，类型必须是 Byte


        val metaPacket = WrapperPlayServerEntityMetadata(fakeId, metadata)

        val pm = PacketEvents.getAPI().playerManager
        pm.sendPacket(player, spawnPacket)
        pm.sendPacket(player, metaPacket)

        viewMap[player.uniqueId] = ViewingData(fakeId, host.entityId)
    }

    private fun moveHologram(player: Player, fakeId: Int, host: LivingEntity) {
        val loc = host.eyeLocation.add(0.0, 0.6, 0.0)
        // 确保 teleportPacket 位置更新
        val teleportPacket = WrapperPlayServerEntityTeleport(
            fakeId, SpigotConversionUtil.fromBukkitLocation(loc), false
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, teleportPacket)
    }

    private fun removeHologram(player: Player, fakeId: Int) {
        val destroyPacket = WrapperPlayServerDestroyEntities(fakeId)
        PacketEvents.getAPI().playerManager.sendPacket(player, destroyPacket)
        viewMap.remove(player.uniqueId)
    }

    // --- 辅助逻辑 ---

    private fun rayTrace(player: Player): LivingEntity? {
        // 射线检测 10 格
        val result = player.rayTraceEntities(10, false) ?: return null
        return result.hitEntity as? LivingEntity
    }

    /**
     * 宽松的视线检测（吸附逻辑）
     * 检查玩家视线方向与"玩家到怪物向量"的夹角
     */
    private fun isLookingAt(player: Player, target: LivingEntity): Boolean {
        val eye = player.eyeLocation
        val toEntity = target.eyeLocation.toVector().subtract(eye.toVector())

        // 距离太远直接放弃
        if (toEntity.lengthSquared() > 100) return false // > 10 blocks

        val direction = eye.direction
        val dot = direction.normalize().dot(toEntity.normalize())

        // dot 越接近 1，说明视线越对准怪物
        // 0.96 大约是 15度角，允许一定的误差，防止稍微抖动就消失
        return dot > 0.96
    }

    private fun shouldShow(entity: LivingEntity): Boolean {
        return entity.atkType != AttackType.NORMAL_A || entity.defType != DefenseType.NORMAL_D
    }

    // 简单的 Helper，从 World 获取 Entity (Paper Api 优化过，性能尚可)
    private fun getEntityById(world: org.bukkit.World, id: Int): LivingEntity? {
        return world.entities.firstOrNull { it.entityId == id } as? LivingEntity
    }
}