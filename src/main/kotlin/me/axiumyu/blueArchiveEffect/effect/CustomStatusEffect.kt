package me.axiumyu.blueArchiveEffect.effect

import org.bukkit.NamespacedKey

/**
 * 自定义状态效果的数据类。
 * 不使用每 tick 调用，而是通过定时任务和自定义事件驱动。
 *
 * @param type 效果类型的 NamespacedKey
 * @param duration 持续时间（游戏 tick，20 tick = 1 秒）
 * @param amplifier 效果等级（0-based）
 * @param displayName BossBar 显示的名称，若为 null 则使用 type 的 key
 */
data class CustomStatusEffect(
    val type: NamespacedKey,
    val duration: Int,
    val amplifier: Int = 0,
    val displayName: String? = null
) {
    /** BossBar 显示的文本 */
    fun getDisplayText(): String = displayName ?: type.key
}
