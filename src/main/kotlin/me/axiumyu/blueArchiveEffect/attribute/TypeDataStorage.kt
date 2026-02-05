package me.axiumyu.blueArchiveEffect.attribute

import io.papermc.paper.registry.RegistryAccess.registryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.AttributeKeys
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.NAMESPACE_KEY
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.Util.nullIf
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionType

data class Attack(
    val atk: AttackType,
    val def: DefenseType
)

object TypeDataStorage {

    @JvmField
    val keyAttack = NamespacedKey(NAMESPACE_KEY, "attack")

    @JvmField
    val keyDefense = NamespacedKey(NAMESPACE_KEY, "defense")

    @JvmField
    val keyTypeModifiers = ((AttackType.entries + DefenseType.entries) as List<Type>)
        .filter { it != AttackType.NORMAL_A && it != DefenseType.NORMAL_D }
        .map {
            NamespacedKey(NAMESPACE_KEY, it.id.lowercase())
        }

    /*
     * 获取攻击类型,实体类型直接调用，不需要手动判断武器类型
     */
    @JvmStatic
    var PersistentDataHolder.atkType: AttackType
        get() {
            val weapon = if (this is LivingEntity) {
                this.equipment?.itemInMainHand?.persistentDataContainer[keyAttack, PersistentDataType.STRING]
            } else null

            val weaponType = AttackType.fromId(weapon).nullIf(AttackType.NORMAL_A)

            return weaponType ?: AttackType.fromId(persistentDataContainer[keyAttack, PersistentDataType.STRING])
            ?: AttackType.NORMAL_A
        }
        set(value) {
            if (value == AttackType.NORMAL_A){
                persistentDataContainer.remove(keyAttack)
            } else {
                persistentDataContainer[keyAttack, PersistentDataType.STRING] = value.id
            }

            if (this is ItemMeta) {
                setEnchant(this, value.nullIf(AttackType.NORMAL_A))
            }
        }

    /*
     * 获取防御类型,实体类型直接调用，不需要手动判断胸甲类型
     */
    @JvmStatic
    var PersistentDataHolder.defType: DefenseType
        get() {
            val armor = if (this is LivingEntity) {
                this.equipment?.chestplate?.persistentDataContainer[keyDefense, PersistentDataType.STRING]
            } else null
            val armorType = DefenseType.fromId(armor).nullIf(DefenseType.NORMAL_D)
            return armorType ?: DefenseType.fromId(persistentDataContainer[keyDefense, PersistentDataType.STRING])
            ?: DefenseType.NORMAL_D
        }
        set(value) {
            if (value == DefenseType.NORMAL_D){
                persistentDataContainer.remove(keyDefense)
            } else{
                persistentDataContainer[keyDefense, PersistentDataType.STRING] = value.id
            }

            if (this is ItemMeta) {
                setEnchant(this, value.nullIf(DefenseType.NORMAL_D))
            }
        }

    @JvmStatic
    fun PersistentDataHolder.modifier(type: Type): Double {
        val typeKey = keyTypeModifiers.findLast {
            it.value() == type.id.lowercase()
        } ?: return 0.0
        return persistentDataContainer[typeKey, PersistentDataType.DOUBLE] ?: 0.0
    }

    @JvmStatic
    fun PersistentDataHolder.modifier(type: Type, value: Double) {
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
     * 统一使用数据包附魔代替lore显示
     * 为了代码整洁，你可以将此逻辑移回 AttributeDataService
     */
    @JvmStatic
    fun setEnchant(meta: ItemMeta, type: Type?) {
        val enchantments = meta.enchants
        if (type == null) {
            val types = enchantments.filter { it.key.key.key == "battr" }
            if (types.isEmpty()) return
            types.forEach { meta.removeEnchant(it.key) }
            return
        }
        if (enchantments.isEmpty()) {
            val ench = registryAccess().getRegistry(RegistryKey.ENCHANTMENT)[NamespacedKey(
                "battr",
                if (type is AttackType) "weapons/${type.id}" else "armors/${type.id}"
            )] ?: throw IllegalStateException("No BA Enchantments found")
            meta.addEnchant(ench, 1, true)
        }
    }

    @JvmStatic
    fun isTypeCore(item: ItemStack): Boolean {
        val isCore = item.persistentDataContainer[NamespacedKey("overenchant", "item_type"), PersistentDataType.STRING]
        val level = item.persistentDataContainer[NamespacedKey("overenchant", "rune_level"), PersistentDataType.INTEGER]
        val enchants = item.enchantments.count()
        return isCore == "RUNE" && level == 3 && enchants <= 1
    }

    @JvmStatic
    fun isRightPotion(item: ItemStack): Boolean {
        val potion = item.itemMeta as? PotionMeta ?: return false
        return potion.basePotionType == PotionType.LONG_TURTLE_MASTER || potion.basePotionType == PotionType.STRONG_TURTLE_MASTER
    }

    @JvmStatic
    fun createCore(from: ItemStack, type: Type?, modifier: (ItemStack.() -> Any)? = null): ItemStack {
        val new = from.clone()
        new.editMeta {
            it.atkType = AttackType.NORMAL_A
            it.defType = DefenseType.NORMAL_D
            it.setMaxStackSize(1)
            it.setRarity(ItemRarity.RARE)
            it.setEnchantmentGlintOverride(true)
            it.customName(mm.deserialize("<!i><gray>属性核心"))
        }
        if (type == null) {
            new.editMeta {
                val lore = listOf(
                    mm.deserialize("<!i><aqua>----------------"),
                    mm.deserialize("<!i><aqua>尚无属性")
                )
                it.lore(lore)
            }
        } else {

            val id = type.id
            val isWeapon = type is AttackType
//            if (isWeapon) new.itemMeta.atkType = type else new.itemMeta.defType = type as DefenseType
//            getServer().sendMessage(mm.deserialize("set type to ${type.displayName}"))
            if (new.enchantments.isEmpty()) {
                val ench = registryAccess().getRegistry(RegistryKey.ENCHANTMENT)[NamespacedKey(
                    "battr",
                    if (isWeapon) "weapons/$id" else "armors/$id"
                )] ?: throw IllegalStateException("No BA Enchantments found")
                new.addUnsafeEnchantment(ench, 1)
            }
            new.editMeta {
                val lore = listOf(
                    mm.deserialize("<!i><gray>----------------"),
                    mm.deserialize("<!i><aqua>属性：<${type.color.asHexString()}>${type.displayName}"),
                    mm.deserialize("<!i>充能条：0/100"),
                    mm.deserialize("<!i><gray>□□□□□□□□□□□□□□□□□□□□")
                )
                it.lore(lore)
            }
        }

        modifier?.invoke(new)
        return new
    }
}