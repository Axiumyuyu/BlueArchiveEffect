package me.axiumyu.blueArchiveEffect.listener

import io.papermc.paper.datacomponent.DataComponentTypes
import me.axiumyu.blueArchiveEffect.Util.nullIf
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.isTypeCore
import me.axiumyu.blueArchiveEffect.listener.ChargeTypeCore.keyCharge
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.persistence.PersistentDataType
import kotlin.math.roundToInt

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
        val result = equipment.clone()
        result.editMeta {
            val rate = if (it.atkType != AttackType.NORMAL_A || it.defType != DefenseType.NORMAL_D) {
                0.75
            } else 0.9
            val maxDamage = result.getData(DataComponentTypes.MAX_DAMAGE)
            if (maxDamage != null) {
                if (!result.hasData(DataComponentTypes.DAMAGE)) {
                    // 增加1点damage,用于查看耐久最大值的变化
                    result.setData(DataComponentTypes.DAMAGE, 1)
                }
                result.setData(DataComponentTypes.MAX_DAMAGE, (maxDamage * rate).roundToInt())
            }
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