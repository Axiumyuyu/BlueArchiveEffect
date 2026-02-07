package me.axiumyu.blueArchiveEffect.effect

import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.plugin
import me.axiumyu.blueArchiveEffect.effect.event.EffectEndEvent
import me.axiumyu.blueArchiveEffect.effect.event.EffectStartEvent
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.scheduler.BukkitTask

/**
 * 管理自定义状态效果的添加、移除、BossBar 和定时任务。
 * 不使用每 tick 轮询，而是通过 runTaskLater 在效果结束时触发。
 */
object EffectManager : Listener {

    /** 用于插件向任何 LivingEntity 应用效果的入口 */
    fun applyTo(entity: LivingEntity, effect: CustomStatusEffect) = applyEffect(entity, effect)

    private data class ActiveEffect(
        val effect: CustomStatusEffect,
        val task: BukkitTask,
        val bossBar: BossBar
    )

    private val activeEffects: MutableMap<LivingEntity, ActiveEffect> = HashMap()

    /**
     * 对实体应用自定义状态效果。
     * 若实体已有同类型效果，则刷新持续时间（取较长者）。
     */
    fun applyEffect(entity: LivingEntity, effect: CustomStatusEffect) {
        val existing = activeEffects[entity]
        if (existing != null && existing.effect.type == effect.type) {
            if (effect.duration <= existing.effect.duration) return
            removeEffectInternal(entity, EffectEndEvent.EndReason.MANUAL)
        }

        val bossBar = createBossBar(effect)
        updateBossBarViewers(bossBar)

        val task = Bukkit.getScheduler().runTaskLater(plugin, Runnable{
            removeEffect(entity, effect.type, EffectEndEvent.EndReason.EXPIRED)
        }, effect.duration.toLong())

        activeEffects[entity] = ActiveEffect(effect, task, bossBar)
        Bukkit.getPluginManager().callEvent(EffectStartEvent(entity, effect))
    }

    /**
     * 移除实体的指定类型自定义状态效果。
     */
    fun removeEffect(entity: LivingEntity, type: NamespacedKey, reason: EffectEndEvent.EndReason = EffectEndEvent.EndReason.MANUAL) {
        val active = activeEffects[entity] ?: return
        if (active.effect.type != type) return
        removeEffectInternal(entity, reason)
    }

    /**
     * 检查实体是否拥有指定类型的自定义状态效果。
     */
    fun hasEffect(entity: LivingEntity, type: NamespacedKey): Boolean =
        activeEffects[entity]?.effect?.type == type

    /**
     * 获取实体当前的自定义状态效果（指定类型），若无则返回 null。
     */
    fun getEffect(entity: LivingEntity, type: NamespacedKey): CustomStatusEffect? =
        activeEffects[entity]?.effect?.takeIf { it.type == type }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        removeEffectInternal(entity, EffectEndEvent.EndReason.ENTITY_DEATH)
    }

    private fun removeEffectInternal(entity: LivingEntity, reason: EffectEndEvent.EndReason) {
        val active = activeEffects.remove(entity) ?: return
        active.task.cancel()
        active.bossBar.removeAll()
        Bukkit.getPluginManager().callEvent(EffectEndEvent(entity, active.effect, reason))
        updateAllBossBarViewers()
    }

    private fun createBossBar(effect: CustomStatusEffect): BossBar {
        val bar = Bukkit.createBossBar(
            effect.getDisplayText(),
            BarColor.GREEN,
            BarStyle.SEGMENTED_10
        )
        bar.progress = 1.0
        return bar
    }

    /** 当前持有该效果的玩家集合（用于 BossBar 可见性） */
    private fun playersWithEffect(): Set<Player> =
        activeEffects.keys.filterIsInstance<Player>().toSet()

    private fun updateBossBarViewers(bar: BossBar) {
        bar.removeAll()
        playersWithEffect().forEach { bar.addPlayer(it) }
    }

    private fun updateAllBossBarViewers() {
        val viewers = playersWithEffect()
        activeEffects.values.forEach { active ->
            active.bossBar.removeAll()
            viewers.forEach { active.bossBar.addPlayer(it) }
        }
    }
}
