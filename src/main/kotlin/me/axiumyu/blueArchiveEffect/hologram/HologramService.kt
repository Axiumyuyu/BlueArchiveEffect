package me.axiumyu.blueArchiveEffect.hologram

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class HologramService: BukkitRunnable() {

    // 存储每个玩家当前看到的虚假实体信息: Player UUID -> (Host Entity ID -> FakeEntityData)
    private val activeDisplays = ConcurrentHashMap<UUID, MutableMap<Int, FakeDisplay>>()

    private data class FakeDisplay(
        val entityId: Int,
        val uuid: UUID,
        var lastText: String,
        var lastLocation: org.bukkit.Location
    )

    override fun run() {
        for (player in Bukkit.getOnlinePlayers()) {
            updateForPlayer(player)
        }
    }

    override fun cancel() {
        clearAll()
        super.cancel()
    }

    private fun updateForPlayer(player: Player) {
        val playerUUID = player.uniqueId
        val playerDisplays = activeDisplays.getOrPut(playerUUID) { mutableMapOf() }

        // 射线追踪玩家看向的实体 (距离设为 20 格)
        val result = player.world.rayTraceEntities(
            player.eyeLocation,
            player.eyeLocation.direction,
            32.0
        ) { it is LivingEntity && it != player }

        val targetEntity = result?.hitEntity as? LivingEntity
        val targetId = targetEntity?.entityId ?: -1

        // 1. 处理不再看向的实体或不满足条件的实体
        val toRemove = playerDisplays.keys.filter { it != targetId }.toMutableList()

        if (targetEntity != null && !shouldShow(targetEntity)) {
            toRemove.add(targetId)
        }

        for (id in toRemove) {
            val display = playerDisplays.remove(id) ?: continue
            removeFakeEntity(player, display.entityId)
        }

        // 2. 处理当前看向的实体
        if (targetEntity != null && shouldShow(targetEntity)) {
            val info = getInfo(targetEntity)
            val spawnLoc = targetEntity.eyeLocation.add(0.0, 0.5, 0.0)

            val existing = playerDisplays[targetId]
            if (existing == null) {
                // 创建新的虚假实体
                val fakeId = SpigotReflectionUtil.generateEntityId()
                val fakeUUID = UUID.randomUUID()
                val display = FakeDisplay(fakeId, fakeUUID, info, spawnLoc)

                spawnFakeTextDisplay(player, display)
                playerDisplays[targetId] = display
            } else {
                // 更新位置
                if (existing.lastLocation.distanceSquared(spawnLoc) > 0.001) {
                    teleportFakeEntity(player, existing.entityId, spawnLoc)
                    existing.lastLocation = spawnLoc
                }

                // 更新文本
                if (existing.lastText != info) {
                    updateText(player, existing.entityId, info)
                    existing.lastText = info
                }
            }
        }
    }

    private fun spawnFakeTextDisplay(player: Player, display: FakeDisplay) {
        val loc = SpigotConversionUtil.fromBukkitLocation(display.lastLocation)

        // 1. 发送生成包
        val spawnPacket = WrapperPlayServerSpawnEntity(
            display.entityId,
            display.uuid,
            EntityTypes.TEXT_DISPLAY,
            loc,
            0f,
            0,
            null
        )

        // 2. 发送元数据包设置初始状态
        val metadataPacket = createMetadataPacket(display.entityId, display.lastText)

        val user = PacketEvents.getAPI().playerManager.getUser(player)
        user.sendPacket(spawnPacket)
        user.sendPacket(metadataPacket)
    }

    private fun updateText(player: Player, entityId: Int, text: String) {
        val packet = createMetadataPacket(entityId, text)
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private fun teleportFakeEntity(player: Player, entityId: Int, location: org.bukkit.Location) {
        val loc = SpigotConversionUtil.fromBukkitLocation(location)
        val packet = WrapperPlayServerEntityTeleport(entityId, loc, true)
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private fun removeFakeEntity(player: Player, entityId: Int) {
        val packet = WrapperPlayServerDestroyEntities(entityId)
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private fun createMetadataPacket(entityId: Int, text: String): WrapperPlayServerEntityMetadata {
        val component = mm.deserialize(text)

        val data = listOf(
            EntityData(23, EntityDataTypes.ADV_COMPONENT, component), // Text
            EntityData(15, EntityDataTypes.BYTE, 3.toByte()),// Billboard: Center
            EntityData(25, EntityDataTypes.INT, 0),
            EntityData(27, EntityDataTypes.BYTE, 3.toByte())
        )


        return WrapperPlayServerEntityMetadata(entityId, data)
    }

    /**
     * 清理所有虚假实体（插件关闭时调用）
     */
    fun clearAll() {
        activeDisplays.forEach { (playerUUID, displays) ->
            val player = Bukkit.getPlayer(playerUUID) ?: return@forEach
            displays.values.forEach { removeFakeEntity(player, it.entityId) }
        }
        activeDisplays.clear()
    }

    private fun shouldShow(entity: LivingEntity): Boolean {
        return entity.atkType != AttackType.NORMAL_A || entity.defType != DefenseType.NORMAL_D || entity.hasPotionEffect(PotionEffectType.INVISIBILITY)
    }

    private fun getInfo(entity: LivingEntity): String {
        val atk = entity.atkType
        val def = entity.defType
        return "<${atk.color.asHexString()}>${atk.displayName}<gray> | <${def.color.asHexString()}>${def.displayName}"
    }
}