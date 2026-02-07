package me.axiumyu.blueArchiveEffect.effect.event

import me.axiumyu.blueArchiveEffect.effect.CustomStatusEffect
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 当自定义状态效果开始时触发。
 * 在效果应用到实体并启动定时任务后调用。
 */
class EffectStartEvent(
    val entity: LivingEntity,
    val effect: CustomStatusEffect
) : Event() {

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }

    override fun getHandlers(): HandlerList = handlerList
}
