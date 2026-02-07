package me.axiumyu.blueArchiveEffect.effect.event

import me.axiumyu.blueArchiveEffect.effect.CustomStatusEffect
import org.bukkit.entity.LivingEntity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 当自定义状态效果结束时触发。
 * 在效果因持续时间到期或实体死亡而移除时调用。
 */
class EffectEndEvent(
    val entity: LivingEntity,
    val effect: CustomStatusEffect,
    val reason: EndReason
) : Event() {

    enum class EndReason {
        /** 持续时间到期 */
        EXPIRED,
        /** 实体死亡 */
        ENTITY_DEATH,
        /** 被手动移除 */
        MANUAL
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }

    override fun getHandlers(): HandlerList = handlerList
}
