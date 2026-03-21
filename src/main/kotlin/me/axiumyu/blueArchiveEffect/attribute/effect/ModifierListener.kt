package me.axiumyu.blueArchiveEffect.attribute.effect
import me.axiumyu.blueArchiveEffect.BlueArchiveEffect.Companion.mm
import me.axiumyu.blueArchiveEffect.attribute.TypeDataStorage.modifier
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.inventory.BrewEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffectType

object ModifierListener : Listener {

    private val specialPotionTypes = setOf(
        PotionEffectType.WIND_CHARGED,
        PotionEffectType.WEAVING,
        PotionEffectType.OOZING,
        PotionEffectType.INFESTED
    )

    // ---------------------------------------------------------
    // 1. 动态酿造拦截：处理经验池注入与蛛眼反转
    // ---------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBrew(event: BrewEvent) {
        val inventory = event.contents
        val ingredient = inventory.ingredient ?: return

        // 情景 A：使用【经验池】将四种基础药水转化为【攻击共鸣药水】
        if (isExperiencePool(ingredient)) {
            val xpValue = getXpFromPool(ingredient)
            val potency = xpValue * 0.01 // 假设 1级 = 1%加成 (0.01)

            var isBrewed = false
            for (i in 0..2) {
                val item = inventory.getItem(i) ?: continue
                if (isBaseSpecialPotion(item)) {
                    val meta = item.itemMeta as? PotionMeta ?: continue

                    // 写入攻击加成
                    meta.modifier(isAtk = true, value = potency)
                    meta.lore(listOf(
                        mm.deserialize("<green>⚔ 共鸣药水 - <red>攻击</red></green>"),
                        mm.deserialize("<red>⚔ 能力: ${potency * 100}%</red>")
                    ))
                    item.itemMeta = meta
                    isBrewed = true
                }
            }

            if (isBrewed) {
                event.isCancelled = true // 拦截原版结果，手动结算
                ingredient.amount -= 1   // 连同容器一起消耗
            }
        }
        // 情景 B：使用【发酵蛛眼】将攻击药水反转为【防御共鸣药水】
        else if (ingredient.type == Material.FERMENTED_SPIDER_EYE) {
            var isBrewed = false
            for (i in 0..2) {
                val item = inventory.getItem(i) ?: continue
                val atkMod = item.itemMeta?.modifier(isAtk = true) ?: 0.0

                // 只有带有攻击极化的药水才能被反转
                if (atkMod > 0.0) {
                    val meta = item.itemMeta as? PotionMeta ?: continue

                    meta.modifier(isAtk = true, value = 0.0) // 抹除攻击
                    meta.modifier(isAtk = false, value = atkMod) // 转换为防御
                    meta.lore(listOf(
                        mm.deserialize("<green>⛨ 共鸣药水 - <aqua>防御</aqua></green>"),
                        mm.deserialize("<aqua>⛨ 能力: ${atkMod * 100}%</aqua>")
                    ))
                    item.itemMeta = meta
                    isBrewed = true
                }
            }

            if (isBrewed) {
                event.isCancelled = true
                ingredient.amount -= 1
            }
        }
    }

    // ---------------------------------------------------------
    // 2. 饮用药水：将物品 PDC 转录给玩家
    // ---------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onConsume(event: PlayerItemConsumeEvent) {
        val item = event.item
        if (item.type != Material.POTION && item.type != Material.SPLASH_POTION) return
        val meta = item.itemMeta ?: return

        val atkMod = meta.modifier(isAtk = true)
        val defMod = meta.modifier(isAtk = false)

        if (atkMod > 0 || defMod > 0) {
            val player = event.player
            if (atkMod > 0) player.modifier(isAtk = true, value = atkMod)
            if (defMod > 0) player.modifier(isAtk = false, value = defMod)

            player.sendMessage(mm.deserialize("<gold>你获得了属性特效加成!</gold>"))
        }
    }

    // ---------------------------------------------------------
    // 3. 状态结束：白嫖原版时间轴，自动剥离加成
    // ---------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEffectEnd(event: EntityPotionEffectEvent) {
        val player = event.entity as? Player ?: return
        val action = event.action

        // 仅拦截效果被清除(喝牛奶/死亡)或自然过期
        if (action == EntityPotionEffectEvent.Action.CLEARED ||
            action == EntityPotionEffectEvent.Action.REMOVED ) {

            val effectType = event.oldEffect?.type ?: return

            // 检查失去的是否为那四种载体药水
            if (effectType in specialPotionTypes) {
                // 如果玩家身上有修改器，则清零
                if (player.modifier(isAtk = true) > 0 || player.modifier(isAtk = false) > 0) {
                    player.modifier(isAtk = true, value = 0.0)
                    player.modifier(isAtk = false, value = 0.0)
                    player.sendMessage(mm.deserialize("<gray>法则共鸣已消散...</gray>"))
                }
            }
        }
    }

    // --- 辅助判定方法 (需根据你的实际物品实现补充) ---
    private fun isExperiencePool(item: ItemStack): Boolean {
        // 检查是否为你的经验池物品
        return item.itemMeta?.persistentDataContainer?.get(NamespacedKey("specialitem", "special_item_id"),
            PersistentDataType.STRING) == "experience_storge"
    }

    private fun getXpFromPool(item: ItemStack): Double {
        // 读取经验池内的经验值
        val exp = item.itemMeta?.persistentDataContainer?.get(NamespacedKey("specialitem", "exp"), PersistentDataType.DOUBLE) ?: 0.0
        return exp * 0.25
    }

    private fun isBaseSpecialPotion(item: ItemStack): Boolean {
        if (item.type != Material.POTION) return false
        val meta = item.itemMeta as? PotionMeta ?: return false
        val effect = meta.basePotionType?.potionEffects?.first()?.type ?: return false
        return  specialPotionTypes.contains(effect)
    }
}