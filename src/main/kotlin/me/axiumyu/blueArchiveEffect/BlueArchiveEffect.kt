package me.axiumyu.blueArchiveEffect

import io.papermc.paper.potion.PotionMix
import me.axiumyu.blueArchiveEffect.attribute.effect.ModifierListener
import me.axiumyu.blueArchiveEffect.comand.ModifierHelper
import me.axiumyu.blueArchiveEffect.comand.TypeHelper
import me.axiumyu.blueArchiveEffect.config.Config
import me.axiumyu.blueArchiveEffect.hologram.HologramService
import me.axiumyu.blueArchiveEffect.listener.*
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.plugin.java.JavaPlugin

class BlueArchiveEffect : JavaPlugin() {

    companion object {
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
        server.potionBrewer.addPotionMix(
            PotionMix(
                NamespacedKey(plugin, "dummy_exp_brew"),
                ItemStack.of(Material.POTION), // 占位结果，反正会被 BrewEvent 拦截
                RecipeChoice.MaterialChoice(Material.POTION), // 底部
                RecipeChoice.MaterialChoice(Material.BEACON)
            )
        )
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
            ModifierListener,
            NaturalItemType
        ).forEach {
            server.pluginManager.registerEvents(it, this)
        }
    }

    override fun onDisable() {
        updateTask.cancel()
    }
}
