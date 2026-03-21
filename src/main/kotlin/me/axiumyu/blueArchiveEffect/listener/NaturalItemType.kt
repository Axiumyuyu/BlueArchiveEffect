package me.axiumyu.blueArchiveEffect.listener

import com.destroystokyo.paper.MaterialSetTag
import me.axiumyu.blueArchiveEffect.attribute.AttackType
import me.axiumyu.blueArchiveEffect.attribute.DefenseType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.atkType
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.defType
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag.ITEMS_SPEARS
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack

const val DATAPACK_NAMESPACE = "battr"

object NaturalItemType: Listener {

    // ---------------------------------------------------------
    // 构建极速检索的 MaterialSetTag (在类加载时初始化，单次开销)
    // ---------------------------------------------------------

    private val PIERCING_WEAPONS = MaterialSetTag(NamespacedKey(DATAPACK_NAMESPACE,"piercing_weapon"))

    // 防御端标签 (结合 Paper 自带的 MaterialTags.ARMOR 过滤，避免误判)
    private val LIGHT_ARMOR = MaterialSetTag(NamespacedKey(DATAPACK_NAMESPACE,"light_armor"))

    private val HEAVY_ARMOR = MaterialSetTag(NamespacedKey(DATAPACK_NAMESPACE,"heavy_armor"))
    private val COMPOSITE_ARMOR = MaterialSetTag(NamespacedKey(DATAPACK_NAMESPACE,"composite_armor"))

    private val SPECIAL_ARMOR = MaterialSetTag(NamespacedKey(DATAPACK_NAMESPACE,"special_armor"))


    // ---------------------------------------------------------
    // 核心处理逻辑
    // ---------------------------------------------------------
    private fun processItem(item: ItemStack?) {
        item ?: return
        val meta = item.itemMeta ?: return
        var modified = false

        // 1. 武器攻击属性初始化 (直接通过 getter 判断是否为默认值)
        if (meta.atkType == AttackType.NORMAL_A) {
            val defaultAtk = getDefaultAttackType(item.type)
            if (defaultAtk != null) {
                meta.atkType = defaultAtk // 直接走你写的 setter
                modified = true
            }
        }

        // 2. 护甲防御属性初始化
        if (meta.defType == DefenseType.NORMAL_D) {
            val defaultDef = getDefaultDefenseType(item.type)
            if (defaultDef != null && defaultDef != DefenseType.NORMAL_D) {
                meta.defType = defaultDef // 直接走你写的 setter
                modified = true
            }
        }

        // 修改后回写
        if (modified) {
            item.itemMeta = meta
        }
    }

    private fun getDefaultAttackType(material: Material): AttackType? = when {
        ITEMS_SPEARS.isTagged(material) -> AttackType.BURST
        MaterialSetTag.ITEMS_AXES.isTagged(material) || material == Material.MACE -> AttackType.DECOMPOSE
        PIERCING_WEAPONS.isTagged(material) -> AttackType.PIERCING
        else -> null
    }

    private fun getDefaultDefenseType(material: Material): DefenseType? = when {
        LIGHT_ARMOR.isTagged(material) -> DefenseType.LIGHT
        HEAVY_ARMOR.isTagged(material) -> DefenseType.HEAVY
        COMPOSITE_ARMOR.isTagged(material) -> DefenseType.COMPOSITE
        SPECIAL_ARMOR.isTagged(material) -> DefenseType.SPECIAL
        else -> null
    }

    // ---------------------------------------------------------
    // 事件拦截器
    // ---------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPickup(event: EntityPickupItemEvent) = processItem(event.item.itemStack)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onCraft(event: CraftItemEvent) = processItem(event.currentItem)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        processItem(event.currentItem)
        processItem(event.cursor)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.inventory.contents.forEach { processItem(it) }
    }
}