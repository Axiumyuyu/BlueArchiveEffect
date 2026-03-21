package me.axiumyu.blueArchiveEffect.attribute

import io.papermc.paper.registry.RegistryAccess.registryAccess
import io.papermc.paper.registry.RegistryKey
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.NAMESPACE_KEY
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.Util.nullIf
import me.axiumyu.blueArchiveEffect.listener.DATAPACK_NAMESPACE
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

    private const val WEIGHT_HELMET = 5
    private const val WEIGHT_CHESTPLATE = 8
    private const val WEIGHT_LEGGINGS = 7
    private const val WEIGHT_BOOTS = 4
    private const val DOMINANCE_THRESHOLD = 12
    val keyAttack = NamespacedKey(NAMESPACE_KEY, "attack")

    val keyDefense = NamespacedKey(NAMESPACE_KEY, "defense")

    val keyAtkModifier = NamespacedKey(NAMESPACE_KEY, "atk_modifier")

    val keyDefModifier = NamespacedKey(NAMESPACE_KEY, "def_modifier")

    /*
     * 获取攻击类型,实体类型直接调用，不需要手动判断武器类型
     */
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
            if (value == AttackType.NORMAL_A) {
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
    var PersistentDataHolder.defType: DefenseType
        get() {
            // 第一层解析：有装备的生物
            if (this is LivingEntity) {
                val equipment = this.equipment
                if (equipment != null) {
                    // 用于累加各防御属性的权重
                    val weights = mutableMapOf<DefenseType, Int>()

                    // 局部辅助函数：提取护甲属性并累加权重
                    fun addWeight(item: ItemStack?, weight: Int) {
                        if (item == null || item.type.isAir) return
                        val type = getArmorDefType(item)
                        // NORMAL_D 代表无属性，不参与主导权争夺
                        if (type != DefenseType.NORMAL_D) {
                            weights[type] = weights.getOrDefault(type, 0) + weight
                        }
                    }

                    addWeight(equipment.helmet, WEIGHT_HELMET)
                    addWeight(equipment.chestplate, WEIGHT_CHESTPLATE)
                    addWeight(equipment.leggings, WEIGHT_LEGGINGS)
                    addWeight(equipment.boots, WEIGHT_BOOTS)

                    // 寻找总权重严格大于 12 的主导属性 (超过50%)
                    val dominantType = weights.entries.find { it.value > DOMINANCE_THRESHOLD }?.key
                    if (dominantType != null) {
                        return dominantType // 护甲共鸣成功，法则覆盖实体自身
                    }
                }
            }

            // 第二层解析：无装备，或护甲混搭导致法则失效，读取自身底色
            val baseTypeId = this.persistentDataContainer[keyDefense, PersistentDataType.STRING]
            return DefenseType.fromId(baseTypeId) ?: DefenseType.NORMAL_D
        }
        set(value) {
            if (value == DefenseType.NORMAL_D) {
                persistentDataContainer.remove(keyDefense)
            } else {
                persistentDataContainer[keyDefense, PersistentDataType.STRING] = value.id
            }

            if (this is ItemMeta) {
                setEnchant(this, value.nullIf(DefenseType.NORMAL_D))
            }
        }

    /*
 * 解析单件护甲的防御属性：PDC 烙印优先，原生材质垫底
 */
    private fun getArmorDefType(item: ItemStack): DefenseType {
        val meta = item.itemMeta ?: return DefenseType.NORMAL_D

        val pdcId = meta.persistentDataContainer[keyDefense, PersistentDataType.STRING]
        val pdcType = DefenseType.fromId(pdcId)
            return pdcType ?: DefenseType.NORMAL_D
    }

    fun PersistentDataHolder.modifier(isAtk: Boolean): Double {
        val typeKey = if (isAtk) keyAtkModifier else keyDefModifier
        return persistentDataContainer[typeKey, PersistentDataType.DOUBLE] ?: 0.0
    }

    fun PersistentDataHolder.modifier(isAtk: Boolean, value: Double) {
        val typeKey = if (isAtk) keyAtkModifier else keyDefModifier
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
    fun setEnchant(meta: ItemMeta, type: Type?) {
        val enchantments = meta.enchants
        val types = enchantments.filter { it.key.key.namespace == DATAPACK_NAMESPACE }
        types.forEach { meta.removeEnchant(it.key) }
        type ?: return
        val ench = registryAccess().getRegistry(RegistryKey.ENCHANTMENT)[NamespacedKey(
            DATAPACK_NAMESPACE,
            if (type is AttackType) "weapons/${type.id}" else "armors/${type.id}"
        )] ?: throw IllegalStateException("No BA Enchantments found")
        meta.addEnchant(ench, 1, true)
    }

    fun isTypeCore(item: ItemStack): kotlin.Boolean {
        val isCore = item.persistentDataContainer[NamespacedKey("overenchant", "item_type"), PersistentDataType.STRING]
        val level = item.persistentDataContainer[NamespacedKey("overenchant", "rune_level"), PersistentDataType.INTEGER]
        val enchants = item.enchantments.count()
        return isCore == "RUNE" && level == 3 && enchants <= 1
    }

    fun isRightPotion(item: ItemStack): kotlin.Boolean {
        val potion = item.itemMeta as? PotionMeta ?: return false
        return potion.basePotionType == PotionType.LONG_TURTLE_MASTER || potion.basePotionType == PotionType.STRONG_TURTLE_MASTER
    }

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
                    DATAPACK_NAMESPACE,
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