package me.axiumyu.blueArchiveEffect.listener

import me.axiumyu.blueArchiveEffect.Util.nullIf
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.Type
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.isTypeCore
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.keyAttack
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.keyDefense
import me.axiumyu.blueArchiveEffect.listener.ChargeTypeCore.keyCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.persistence.PersistentDataType

// Tested
object ItemForgeType : Listener {

    @EventHandler
    fun onItemForge(event: PrepareAnvilEvent) {
        val equipment = event.inventory.firstItem ?: return
        val typeCore = event.inventory.secondItem ?: return

        if (!isTypeCore(typeCore)) return
        if (typeCore.persistentDataContainer[keyCharge, PersistentDataType.INTEGER] != 100) {
            event.result = null
            return
        }
        val atkType = typeCore.itemMeta.atkType.nullIf(AttackType.NORMAL_A)
        val defType = typeCore.itemMeta.defType.nullIf(DefenseType.NORMAL_D)
        val  result = equipment.clone()
        result.editMeta {
            it.atkType = AttackType.NORMAL_A
            it.defType = DefenseType.NORMAL_D
            if (defType != null) {
                it.defType = defType
            } else if (atkType != null) {
                it.atkType = atkType
            }
        }
        event.result = result
        event.view.repairCost = 10
    }
}