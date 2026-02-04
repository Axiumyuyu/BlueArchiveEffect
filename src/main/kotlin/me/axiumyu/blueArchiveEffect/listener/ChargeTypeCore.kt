package me.axiumyu.blueArchiveEffect.listener

import me.axiumyu.blueArchiveEffect.LoreProgressBar
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.createCore
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.isTypeCore
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.persistence.PersistentDataType

object ChargeTypeCore : Listener {
    val keyCharge = NamespacedKey("battr", "charge")

    @EventHandler
    fun onPlayerAttack(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        val victim = event.entity

        // 玩家手持属性核心攻击某防御类型的实体，充能手中物品
        if (damager is Player) {
            val item = damager.inventory.itemInMainHand
            if (!isTypeCore(item)) return

            val defType = victim.defType
            if (defType == DefenseType.NORMAL_D) return

            // 原有物品类型
            val itemType = item.itemMeta.defType

            //不一致则重置，由于是clone，需要清理pdc键
            if (itemType != defType) {
                damager.inventory.setItemInMainHand(createCore(item, defType) {
                    editPersistentDataContainer {
                        it.remove(keyCharge)
                    }
                })
            } else {
                // 充能并写入持久化保存
                val progress = item.persistentDataContainer[keyCharge, PersistentDataType.INTEGER] ?: 0
                val chargeBar = LoreProgressBar(100, numberLinePrefix = "充能条：")
                val charge = event.finalDamage.toInt() / 2
                val newItem = chargeBar.updateLore(item, progress + charge)
                item.editPersistentDataContainer {
                    it[keyCharge, PersistentDataType.INTEGER] = (progress + charge).coerceIn(0, 100)
                }
                damager.inventory.setItemInMainHand(newItem)
            }
        }
        // 同上
        if (victim is Player) {
            val item = victim.inventory.helmet ?: return
            if (!isTypeCore(item)) return

            val hitType = damager.atkType
            if (hitType == AttackType.NORMAL_A) return

            val itemType = item.itemMeta.atkType

            if (itemType != hitType) {
                victim.inventory.helmet = createCore(item, hitType) {
                    editPersistentDataContainer {
                        it.remove(keyCharge)
                    }
                }
            } else {
                val progress = item.persistentDataContainer[keyCharge, PersistentDataType.INTEGER] ?: 0
                val chargeBar = LoreProgressBar(100, numberLinePrefix = "充能条：")
                val charge = event.finalDamage.toInt() / 2
                val newItem = chargeBar.updateLore(item, progress + charge)
                item.editPersistentDataContainer {
                    it[keyCharge, PersistentDataType.INTEGER] = (progress + charge).coerceIn(0, 100)
                }
                victim.inventory.helmet = newItem
            }
        }

    }
}