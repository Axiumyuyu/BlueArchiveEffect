package me.axiumyu.blueArchiveEffect.effect

import me.axiumyu.blueArchiveEffect.BlueArchiveEffect
import org.bukkit.NamespacedKey

/**
 * 预定义的自定义状态效果类型。
 * 插件可通过 [EffectManager.applyTo] 应用这些效果。
 */
object EffectTypes {
    /** 示例自定义效果类型 */
    val CUSTOM = key("custom")

    private fun key(name: String): NamespacedKey =
        NamespacedKey(BlueArchiveEffect.NAMESPACE_KEY, name)
}
