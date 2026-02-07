package me.axiumyu.blueArchiveEffect

import me.axiumyu.blueArchiveEffect.attribute.effect.VanillaEffects
import me.axiumyu.blueArchiveEffect.comand.ModifierHelper
import me.axiumyu.blueArchiveEffect.comand.TypeHelper
import me.axiumyu.blueArchiveEffect.config.Config
import me.axiumyu.blueArchiveEffect.hologram.HologramService
import me.axiumyu.blueArchiveEffect.listener.ChargeTypeCore
import me.axiumyu.blueArchiveEffect.listener.CreateCore
import me.axiumyu.blueArchiveEffect.listener.DamageModifier
import me.axiumyu.blueArchiveEffect.listener.ItemForgeType
import me.axiumyu.blueArchiveEffect.listener.MobSpawnType
import me.axiumyu.blueArchiveEffect.effect.EffectManager
import me.axiumyu.blueArchiveEffect.listener.RemoveType
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.plugin.java.JavaPlugin
import kotlin.getValue

class BlueArchiveEffect : JavaPlugin() {

    companion object{
        @JvmField
        val mm = MiniMessage.miniMessage()

        val plugin by lazy(LazyThreadSafetyMode.NONE) { getPlugin(BlueArchiveEffect::class.java) }

        const val NAMESPACE_KEY = "ba_attr"

        val updateTask by lazy(LazyThreadSafetyMode.NONE) { HologramService() }

    }
    override fun onLoad() {
        super.onLoad()

        TypeHelper.register()
        ModifierHelper.register()
    }

    override fun onEnable() {
        saveDefaultConfig()
        Config.loadConfig()

        updateTask.runTaskTimer(this, 0L, 1L)

        listOf(
            DamageModifier,
            MobSpawnType,
            CreateCore,
            ChargeTypeCore,
            ItemForgeType,
            RemoveType,
            EffectManager,
            VanillaEffects
        ).forEach {
            server.pluginManager.registerEvents(it, this)
        }
    }

    override fun onDisable() {
        updateTask.cancel()
    }
}
