package me.axiumyu.blueArchiveEffect.listener

import me.axiumyu.blueArchiveEffect.SmartLoreBar.updateSmartLore
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.Type
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.createCore
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.isTypeCore
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

// Tested
object ChargeTypeCore : Listener {

    @JvmField
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

            processItem(item, defType, false, damager, event.finalDamage.toInt())

        }
        // 同上
        if (victim is Player) {
            val item = victim.inventory.helmet ?: return
            if (!isTypeCore(item)) return

            val atkType = damager.atkType
            if (atkType == AttackType.NORMAL_A) return

            processItem(item, atkType, true, victim, event.finalDamage.toInt())
        }

    }

    fun processItem(item: ItemStack, type: Type,isATK: Boolean, processer : Player, finalDamage: Int) {
        val newItem = item.clone()
        val itemType = if (isATK) item.itemMeta.atkType else item.itemMeta.defType
        if (itemType != type) {
            val newItem = createCore(newItem, type) {
                editPersistentDataContainer {
                    it.remove(keyCharge)
                }
                editMeta {
                    it.removeEnchantments()
                    if (isATK) {
                        it.atkType = type as AttackType
                    } else {
                        it.defType = type as DefenseType
                    }
                }
            }
            if (isATK) {
                processer.inventory.helmet = newItem
            } else {
                processer.inventory.setItemInMainHand(newItem)
            }
        } else {
            val progress = item.persistentDataContainer[keyCharge, PersistentDataType.INTEGER] ?: 0
            val charge = finalDamage / 2
            val newItem = newItem.updateSmartLore(
                "充能条：",
                progress + charge,
                100,
                filledColor = "<${type.color.asHexString()}>"
            )
            newItem.editPersistentDataContainer {
                it[keyCharge, PersistentDataType.INTEGER] = (progress + charge).coerceIn(0, 100)
            }
            if (isATK) {
                processer.inventory.helmet = newItem
            } else {
                processer.inventory.setItemInMainHand(newItem)
            }
        }

    }
}