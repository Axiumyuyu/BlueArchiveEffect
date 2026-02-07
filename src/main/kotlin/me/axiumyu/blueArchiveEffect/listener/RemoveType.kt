package me.axiumyu.blueArchiveEffect.listener

import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.createCore
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.isTypeCore
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareGrindstoneEvent

object RemoveType: Listener {

    @EventHandler
    fun onRemove(event: PrepareGrindstoneEvent) {
        val upItem = event.inventory.upperItem ?: return
        val loItem = event.inventory.lowerItem

        if (loItem != null) return

        if (!isTypeCore(upItem)) return
        event.result = createCore(upItem, null)
    }
}