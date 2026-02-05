package me.axiumyu.blueArchiveEffect.listener

import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.createCore
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.isRightPotion
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.isTypeCore
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent

// Tested
object CreateCore : Listener {
    @EventHandler
    fun onCreate(event : PrepareAnvilEvent){
        val input = event.inventory.firstItem ?: return
        if (!isTypeCore(input)) return
        if (input.amount > 1) {
            event.result = null
            return
        }

        val second = event.inventory.secondItem ?: return
        if (isRightPotion(second)) return

        event.view.repairCost = 10
        event.result = createCore(input, null)
    }
}