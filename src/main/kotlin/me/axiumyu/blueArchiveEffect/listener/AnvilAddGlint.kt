package me.axiumyu.blueArchiveEffect.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.inventory.PrepareAnvilEvent

object AnvilAddGlint : Listener {

    @EventHandler
    fun onAnvil(event: PrepareAnvilEvent){
        val result = event.result ?: return

        if (result.enchantments.any { it.key.key.namespace != DATAPACK_NAMESPACE }){
            result.editMeta { it.setEnchantmentGlintOverride(true) }
        }
    }

    @EventHandler
    fun onEnchantTable(event: EnchantItemEvent){
        val result = event.item.clone()

        if (result.enchantments.all { it.key.key.namespace == DATAPACK_NAMESPACE }){
            result.editMeta { it.setEnchantmentGlintOverride(true) }
            event.item = result
        }
    }
}