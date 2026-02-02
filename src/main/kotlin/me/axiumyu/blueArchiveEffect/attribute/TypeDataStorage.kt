package me.axiumyu.blueArchiveEffect.attribute

import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.NAMESPACE_KEY
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.Util.toPlainText
import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

object TypeDataStorage {
    @JvmField
    val keyAttack = NamespacedKey(NAMESPACE_KEY, "attack")

    @JvmField
    val keyDefense = NamespacedKey(NAMESPACE_KEY, "defense")

    @JvmField
    val keyTypeModifiers = AttackType.entries
        .filter { it != AttackType.NORMAL_A }
        .map {
            NamespacedKey(NAMESPACE_KEY, it.id.lowercase())
        }

    @JvmStatic
    var PersistentDataHolder.atkType: AttackType
        get() = AttackType.fromId(persistentDataContainer[keyAttack, PersistentDataType.STRING]) ?: AttackType.NORMAL_A
        set(value) {
            persistentDataContainer.apply {
                this[keyAttack, PersistentDataType.STRING] = value.id
            }
            if (this is ItemMeta) {
                updateItemLore(this, value, null)
            }
        }

    @JvmStatic
    var PersistentDataHolder.defType: DefenseType
        get() = DefenseType.fromId(persistentDataContainer[keyDefense, PersistentDataType.STRING])
            ?: DefenseType.NORMAL_D
        set(value) {
            persistentDataContainer.apply {
                this[keyDefense, PersistentDataType.STRING] = value.id
            }
            if (this is ItemMeta) {
                updateItemLore(this, null, value)
            }
        }

    @JvmStatic
    fun PersistentDataHolder.atkModifier(type: AttackType): Double {
        val typeKey = keyTypeModifiers.findLast {
            it.value() == type.id.lowercase()
        } ?: return 0.0
        return persistentDataContainer[typeKey, PersistentDataType.DOUBLE] ?: 0.0
    }

    @JvmStatic
    fun PersistentDataHolder.defModifier(type: DefenseType): Double {
        val typeKey = keyTypeModifiers.findLast {
            it.value() == type.id.lowercase()
        } ?: return 0.0
        return persistentDataContainer[typeKey, PersistentDataType.DOUBLE] ?: 0.0
    }

    @JvmStatic
    fun PersistentDataHolder.atkModifier(type: AttackType, value: Double) {
        val typeKey = keyTypeModifiers.findLast {
            it.value() == type.id.lowercase()
        } ?: return
        if (value == 0.0) {
            persistentDataContainer.remove(typeKey)
        } else {
            persistentDataContainer[typeKey, PersistentDataType.DOUBLE] = value
        }
    }

    @JvmStatic
    fun PersistentDataHolder.defModifier(type: DefenseType, value: Double) {
        val typeKey = keyTypeModifiers.findLast {
            it.value() == type.id.lowercase()
        } ?: return
        if (value == 0.0) {
            persistentDataContainer.remove(typeKey)
        } else {
            persistentDataContainer[typeKey, PersistentDataType.DOUBLE] = value
        }
    }

    /**
     * 简单的 Lore 更新逻辑
     * 为了代码整洁，你可以将此逻辑移回 AttributeDataService
     */
    @JvmStatic
    fun updateItemLore(meta: ItemMeta, atk: AttackType?, def: DefenseType?) {
        // 如果传入 null，说明不修改该项，尝试从 PDC 读取当前值
        val currentAtk = atk ?: meta.atkType
        val currentDef = def ?: meta.defType

        val lore = meta.lore()?: mutableListOf()
        val newLore = lore.filter {
            val str = it.toPlainText()
            !str.contains("攻击属性") && !str.contains("防御属性")
        }.toMutableList()

        if (currentAtk != AttackType.NORMAL_A) {
            newLore.add(mm.deserialize("<!i><gray>攻击属性: <${currentAtk.color.asHexString()}>${currentAtk.displayName}"))
        }
        if (currentDef != DefenseType.NORMAL_D) {
            newLore.add(mm.deserialize("<!i><gray>防御属性: <${currentDef.color.asHexString()}>${currentDef.displayName}"))
        }

        meta.lore(newLore)
    }
}