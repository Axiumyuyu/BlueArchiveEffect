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
import org.bukkit.Bukkit.getOnlinePlayers
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object HologramService {

    // 存储玩家正在看到的虚假实体 ID: PlayerUUID -> FakeEntityID
    private val playerViewingMap = ConcurrentHashMap<UUID, Int>()

    // 存储虚假实体对应的宿主: FakeEntityID -> HostLivingEntity
    private val fakeToHostMap = ConcurrentHashMap<Int, LivingEntity>()



    fun updateHolograms() {
        for (player in getOnlinePlayers()) {
            val target = rayTrace(player)
            val currentFakeId = playerViewingMap[player.uniqueId]

            if (target == null || !shouldShow(target)) {
                if (currentFakeId != null) removeHologram(player, currentFakeId)
                continue
            }

            if (currentFakeId == null) {
                createHologram(player, target)
            } else {
                // 如果换了目标，先删旧的
                if (fakeToHostMap[currentFakeId] != target) {
                    removeHologram(player, currentFakeId)
                    createHologram(player, target)
                } else {
                    moveHologram(player, currentFakeId, target)
                }
            }
        }
    }

    private fun createHologram(player: Player, host: LivingEntity) {
        val fakeId = 2000000 + Random.nextInt(1000000) // 安全范围内的虚假ID
        val uuid = UUID.randomUUID()
        val loc = host.eyeLocation.add(0.0, 0.6, 0.0)

        // 1. 生成实体包
        val spawnPacket = WrapperPlayServerSpawnEntity(
            fakeId, uuid, EntityTypes.TEXT_DISPLAY,
            SpigotConversionUtil.fromBukkitLocation(loc), 0f, 0, null
        )

        // 2. 元数据包 (核心：定义文本和外观)
        val atk = host.atkType
        val def = host.defType
        val textComponent =
            mm.deserialize("<${atk.color.asHexString()}>${atk.displayName} <gray> | <${def.color.asHexString()}>${def.displayName}")

        val metadata = mutableListOf<EntityData<*>>()
        // Index 23: Text (Component) - 1.21.4 对应位
        metadata.add(EntityData(23, EntityDataTypes.ADV_COMPONENT, textComponent))
        // Index 15: Billboard (设置为 3: CENTER, 随玩家旋转)
        metadata.add(EntityData(15, EntityDataTypes.BYTE, 3.toByte()))
        // Index 25: Background Color (设置为 0: 完全透明)
        metadata.add(EntityData(25, EntityDataTypes.INT, 0))
        // Index 27: Text Shadow (Boolean)
        metadata.add(EntityData(27, EntityDataTypes.BOOLEAN, true))

        val metaPacket = WrapperPlayServerEntityMetadata(fakeId, metadata)

        // 发送

        PacketEvents.getAPI().playerManager.run {
            sendPacket(player, spawnPacket)
            sendPacket(player, metaPacket)
        }

        playerViewingMap[player.uniqueId] = fakeId
        fakeToHostMap[fakeId] = host
    }

    private fun moveHologram(player: Player, fakeId: Int, host: LivingEntity) {
        val loc = host.eyeLocation.add(0.0, 0.6, 0.0)
        val teleportPacket = WrapperPlayServerEntityTeleport(
            fakeId, SpigotConversionUtil.fromBukkitLocation(loc), false
        )
        PacketEvents.getAPI().playerManager.sendPacket(player, teleportPacket)
    }

    private fun removeHologram(player: Player, fakeId: Int) {
        val destroyPacket = WrapperPlayServerDestroyEntities(fakeId)
        PacketEvents.getAPI().playerManager.sendPacket(player, destroyPacket)
        playerViewingMap.remove(player.uniqueId)
        fakeToHostMap.remove(fakeId)
    }

    private fun rayTrace(player: Player): LivingEntity? {
        val result = player.rayTraceEntities(10, false) ?: return null
        return result.hitEntity as? LivingEntity
    }

    private fun shouldShow(entity: LivingEntity): Boolean {
        // 只有非常规属性才显示，减少视觉干扰
        return entity.atkType != AttackType.NORMAL_A || entity.defType != DefenseType.NORMAL_D
    }
}