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
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.RayTraceResult
import java.util.*


/**
 * 每个玩家当前正在显示的虚假 Text Display
 */
private data class DisplayState(
    val hostEntityId: Int,
    val displayEntityId: Int
)


class EntityInfoDisplayTask(
    private val maxDistance: Double = 10.0
) : BukkitRunnable() {


    // player UUID -> 当前显示状态
    private val activeDisplays = mutableMapOf<UUID, DisplayState>()


    override fun run() {
        for (player in Bukkit.getOnlinePlayers()) {
            handlePlayer(player)
        }
    }


    private fun handlePlayer(player: Player) {
        val target = getLookAtLivingEntity(player, maxDistance)
        val state = activeDisplays[player.uniqueId]


        if (target != null && shouldShow(target)) {
// 需要显示
            if (state == null || state.hostEntityId != target.entityId) {
// 切换目标 or 第一次显示
                state?.let { destroyDisplay(player, it) }
                spawnDisplay(player, target)
            } else {
// 已存在，更新位置
                teleportDisplay(player, state.displayEntityId, target)
            }
        } else {
// 不需要显示
            if (state != null) {
                destroyDisplay(player, state)
            }
        }
    }


    /**
     * 获取玩家视线内的 LivingEntity
     */
    private fun getLookAtLivingEntity(player: Player, distance: Double): LivingEntity? {
        val eye = player.eyeLocation
        val direction = eye.direction.normalize()


        val result: RayTraceResult? = player.world.rayTraceEntities(
            eye,
            direction,
            distance
        ) { it is LivingEntity }


        return result?.hitEntity as? LivingEntity
    }


    /**
     * 生成虚假的 Text Display
     */
    private fun spawnDisplay(player: Player, host: LivingEntity) {
        val displayEntityId = nextEntityId()


        val location = host.eyeLocation.clone().add(0.0, host.height + 0.4, 0.0)


        val spawn = WrapperPlayServerSpawnEntity(
            displayEntityId,
            UUID.randomUUID(),
            EntityTypes.TEXT_DISPLAY,
            SpigotConversionUtil.fromBukkitLocation(location), 0f, 0, null
        )


        val atk = host.atkType
        val def = host.defType
        val textComponent =
            mm.deserialize("<${atk.color.asHexString()}>${atk.displayName} <gray> | <${def.color.asHexString()}>${def.displayName}")

        val metadata = listOf(
            EntityData(23, EntityDataTypes.ADV_COMPONENT, textComponent), // Text
            EntityData(15, EntityDataTypes.BYTE, 3.toByte()),// Billboard: Center
            EntityData(25, EntityDataTypes.INT, 0),
            EntityData(27, EntityDataTypes.BYTE, 3.toByte())
        )
        val metaPacket = WrapperPlayServerEntityMetadata(displayEntityId, metadata)

        send(player, spawn)
        send(player, metaPacket)


        activeDisplays[player.uniqueId] = DisplayState(
            hostEntityId = host.entityId,
            displayEntityId = displayEntityId
        )
    }


    /**
     * 更新 Text Display 位置（跟随实体）
     */
    private fun teleportDisplay(player: Player, displayEntityId: Int, host: LivingEntity) {
        val loc = host.eyeLocation.clone().add(0.0, host.height + 0.4, 0.0)


        val teleport = WrapperPlayServerEntityTeleport(
            displayEntityId,
            SpigotConversionUtil.fromBukkitLocation(loc),
            false
        )
        send(player, teleport)
    }


    /**
     * 销毁虚假实体
     */
    private fun destroyDisplay(player: Player, state: DisplayState) {
        val destroy = WrapperPlayServerDestroyEntities(state.displayEntityId)
        send(player, destroy)
        activeDisplays.remove(player.uniqueId)
    }


    private fun send(player: Player, packet: Any) {
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private fun shouldShow(entity: LivingEntity): Boolean {
        return entity.atkType != AttackType.NORMAL_A || entity.defType != DefenseType.NORMAL_D
    }

    companion object {
        // 非线程安全但在主线程运行 OK
        private var ENTITY_ID = 2_000_000
        private fun nextEntityId(): Int = ENTITY_ID++
    }
}